package app.puffling.work

import android.content.Context
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.puffling.domain.Kind
import app.puffling.domain.Ph
import app.puffling.domain.Phase
import app.puffling.domain.Practice
import app.puffling.domain.WhPhase
import app.puffling.domain.WimHofEngine
import app.puffling.domain.WimHofLevel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min

/** What a session is to play: the practice and its options. */
data class Plan(
    val p: Practice, val minutes: Int, val rounds: Int, val breaths: Int,
    val sound: String, val bellKind: String, val volume: Float, val cues: Boolean, val haptics: Boolean,
    val belly: Boolean, val course: String = "", val before: Int = 0, val bell: Long = 0,
    val cycles: Int = 0,             // a fixed number of breaths instead of minutes (the bell: three)
    val level: WimHofLevel? = null, val inhale: Double = WimHofEngine.DEFAULT_INHALE, val exhale: Double = WimHofEngine.DEFAULT_EXHALE,
)

/**
 * The clock of a session and everything that follows from it: the breath phase, the step, the bells.
 * It lives outside the screens, so it keeps time (and rings) with the screen off; a foreground
 * service keeps the app awake meanwhile.
 */
object Player {
    var plan by mutableStateOf<Plan?>(null); private set
    /** Seconds of practice so far (pauses not counted). */
    var t by mutableFloatStateOf(0f); private set
    var paused by mutableStateOf(false)
    var done by mutableStateOf(false); private set

    // breath
    var phase by mutableStateOf<Phase?>(null); private set
    var phaseLeft by mutableFloatStateOf(0f); private set
    /** How full the lungs are, 0..1: what the pet's body follows. */
    var lungs by mutableFloatStateOf(0f); private set
    var cycles by mutableIntStateOf(0); private set

    // Wim Hof: the engine from SwissZen; [frame] ticks so the screen redraws with it
    var wh: WimHofEngine? = null; private set
    var frame by mutableIntStateOf(0); private set
    val holds = mutableStateListOf<Int>()
    val round get() = (wh?.round ?: 1) - 1
    val holdT get() = if (wh?.phase == WhPhase.HOLD) wh!!.t.toFloat() else 0f

    // steps and cards
    var step by mutableIntStateOf(0); private set
    var stepT by mutableFloatStateOf(0f); private set
    val notes = mutableStateListOf<String>()
    var said by mutableIntStateOf(0)
    var card by mutableIntStateOf(0)
    val noted = HashMap<String, Int>()
    var notedN by mutableIntStateOf(0)

    var ending by mutableLongStateOf(0L)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var job: Job? = null
    private var amb: Ambience? = null
    private var app: Context? = null
    private var cycleStart = 0f
    private var phaseIdx = 0
    private var phaseStart = 0f
    private var lastBell = 0

    fun start(ctx: Context, pl: Plan) {
        stop(ctx)
        app = ctx.applicationContext
        plan = pl; t = 0f; paused = false; done = false
        phase = null; phaseLeft = 0f; lungs = 0f; cycles = 0; cycleStart = 0f; phaseIdx = -1; phaseStart = 0f
        holds.clear(); wh = if (pl.p.kind == Kind.WIMHOF) WimHofEngine(pl.level ?: WimHofLevel.BEGINNER, pl.rounds, pl.inhale, pl.exhale) else null
        step = 0; stepT = 0f; notes.clear(); said = 0; card = 0; noted.clear(); notedN = 0; lastBell = 0
        Synth.warm(); if (pl.cues && pl.p.kind == Kind.BREATH) Thread { pl.p.pattern.forEach { runCatching { Synth.breath(it.ph != Ph.OUT, it.secs) } } }.start()
        amb = Ambience(pl.sound).apply { volume = pl.volume * .55f; start() }
        if (pl.belly) BreathSensor.start(ctx)
        val k = pl.p.kind
        if (k == Kind.SIT || k == Kind.GUIDED) Synth.ring(pl.bellKind, pl.volume)
        if (k != Kind.AFFIRM) SessionService.start(ctx, pl.p.name)
        var last = SystemClock.elapsedRealtime()
        job = scope.launch {
            while (!done) {
                delay(33)
                val now = SystemClock.elapsedRealtime(); val dt = (now - last) / 1000f; last = now
                if (paused) continue
                t += dt
                tick(dt)
                amb?.breath = if (plan?.p?.kind == Kind.BREATH || plan?.p?.kind == Kind.WIMHOF) lungs else -1f
            }
        }
    }

    private fun tick(dt: Float) {
        val pl = plan ?: return
        when (pl.p.kind) {
            Kind.BREATH -> breathTick(pl)
            Kind.WIMHOF -> wimTick(pl, dt)
            Kind.SIT -> {
                if (pl.belly) lungs = BreathSensor.level
                else lungs = (.5f - .5f * cos(2 * PI.toFloat() * t / 11f))
                val left = pl.minutes * 60 - t
                if (pl.p.bell > 0) { val n = (t / (pl.p.bell * 60)).toInt(); if (n > lastBell && left > 20) { lastBell = n; Synth.ring(pl.bellKind, pl.volume * .7f) } }
                if (left <= 0) finish(true)
            }
            Kind.GUIDED -> {
                stepT += dt
                lungs = if (pl.belly) BreathSensor.level else (.5f - .5f * cos(2 * PI.toFloat() * t / 10f))
                val st = pl.p.steps.getOrNull(step)
                if (st == null) finish(true) else if (stepT >= st.secs) next()
            }
            Kind.AFFIRM -> { lungs = .5f - .5f * cos(2 * PI.toFloat() * t / 8f) }
        }
    }

    private fun breathTick(pl: Plan) {
        val pat = pl.p.pattern; if (pat.isEmpty()) return
        fun len(i: Int): Float {
            val ph = pat[i]
            val extra = if (ph.ph == Ph.OUT && pl.p.ramp > 0) min(ph.secs, pl.p.ramp * (t / 60.0)) else 0.0
            return (ph.secs + extra).toFloat()
        }
        if (phaseIdx < 0) { phaseIdx = 0; phaseStart = 0f; curLen = len(0); enter(pl, pat[0]) }
        while (t - phaseStart >= len(phaseIdx)) {
            phaseStart += len(phaseIdx)
            phaseIdx++
            if (phaseIdx >= pat.size) {
                phaseIdx = 0; cycles++
                if (if (pl.cycles > 0) cycles >= pl.cycles else t >= pl.minutes * 60f) { finish(true); return }
            }
            curLen = len(phaseIdx); enter(pl, pat[phaseIdx])
        }
        val ph = pat[phaseIdx]; val l = len(phaseIdx); val f = ((t - phaseStart) / l).coerceIn(0f, 1f)
        phaseLeft = l - (t - phaseStart)
        val ease = .5f - .5f * cos(PI.toFloat() * f)
        val topNext = pat.getOrNull(phaseIdx + 1)?.ph == Ph.TOP
        lungs = when (ph.ph) {
            Ph.IN -> (if (topNext) .82f else 1f) * ease
            Ph.TOP -> .82f + .18f * ease
            Ph.HOLD -> 1f
            Ph.OUT -> 1f - ease
            Ph.EMPTY -> 0f
        }
    }

    private var curLen = 4f
    private fun enter(pl: Plan, ph: Phase) {
        phase = ph
        if (pl.cues) when (ph.ph) {
            Ph.IN, Ph.TOP, Ph.OUT -> Synth.play(Synth.breath(ph.ph != Ph.OUT, curLen.toDouble()), pl.volume)
            else -> Synth.play(Synth.cue('h'), pl.volume * .5f)
        }
        if (pl.haptics) buzz(if (ph.ph == Ph.HOLD || ph.ph == Ph.EMPTY) 12 else 25)
    }

    private fun wimTick(pl: Plan, dt: Float) {
        val e = wh ?: return
        for (ph in e.tick(dt.toDouble())) {
            if (pl.cues) when (ph) {
                WhPhase.IN -> Synth.play(Synth.breath(true, e.inhale), pl.volume)
                WhPhase.OUT -> Synth.play(Synth.breath(false, e.exhale), pl.volume)
                WhPhase.LAST_IN, WhPhase.REC_IN -> Synth.play(Synth.breath(true, WimHofEngine.LAST_IN), pl.volume)
                WhPhase.LAST_OUT -> Synth.play(Synth.breath(false, WimHofEngine.LAST_OUT), pl.volume)
                WhPhase.REC_OUT -> Synth.play(Synth.breath(false, WimHofEngine.REC_OUT), pl.volume)
                WhPhase.HOLD -> Synth.ring("chime", pl.volume * .6f)
                else -> {}
            }
            if (pl.haptics && ph != WhPhase.IN && ph != WhPhase.OUT) buzz(30)
            if (ph == WhPhase.DONE) { finish(true); return }
        }
        lungs = e.lungs(); frame++
    }

    /** «Breathe in»: the hold ends, the recovery breath follows. */
    fun endHold() {
        val e = wh ?: return
        val held = e.t
        if (e.breatheIn()) { holds += held.toInt(); frame++; plan?.let { if (it.cues) Synth.play(Synth.breath(true, WimHofEngine.REC_IN), it.volume); if (it.haptics) buzz(30) } }
    }

    /** The next step of a guided practice (by the clock or a tap). */
    fun next() {
        val pl = plan ?: return
        step++; stepT = 0f
        if (step >= pl.p.steps.size) finish(true) else { Synth.ring("chime", pl.volume * .35f); if (pl.haptics) buzz(15) }
    }

    fun note(k: String) { noted[k] = (noted[k] ?: 0) + 1; notedN++ }

    /** Ends the session: [natural] when it ran its course, false when ended early. */
    fun finish(natural: Boolean) {
        if (done) return
        done = true
        val pl = plan
        if (pl != null && natural && pl.p.kind != Kind.AFFIRM) Synth.ring(pl.bellKind, pl.volume)
        amb?.stop(); amb = null
        job?.cancel()
        app?.let { SessionService.stop(it) }
        if (pl?.belly == true) BreathSensor.stop()
        ending = System.currentTimeMillis()
    }

    /** Forgets the session (after it was saved or thrown away). */
    fun stop(ctx: Context) {
        job?.cancel(); amb?.stop(); amb = null
        if (plan != null) SessionService.stop(ctx)
        BreathSensor.stop()
        plan = null; done = false
    }

    fun setVolume(v: Float) { amb?.volume = v * .55f; plan = plan?.copy(volume = v) }

    private fun buzz(ms: Long) {
        val v = app?.getSystemService(Vibrator::class.java) ?: return
        runCatching { v.vibrate(VibrationEffect.createOneShot(ms, 60)) }
    }
}
