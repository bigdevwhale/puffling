package app.puffling.work

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/**
 * Every sound is made here, in code: the bells, the soft breath cues and the backgrounds.
 * Nothing is recorded; a bell is a few decaying partials, the sea is filtered noise that swells and falls.
 */
object Synth {
    const val RATE = 22050
    private val cache = HashMap<String, ShortArray>()

    /** One partial: frequency ratio, loudness, decay seconds, detune for a slow beat. */
    private class P(val r: Double, val a: Double, val d: Double, val beat: Double = 0.0)

    private val BELLS = mapOf(
        "bowl" to (196.0 to listOf(P(1.0, 1.0, 9.0, .7), P(2.76, .55, 6.0, 1.1), P(5.4, .26, 3.5), P(8.93, .12, 2.2))),
        "bell" to (392.0 to listOf(P(.5, .5, 7.0), P(1.0, 1.0, 5.0, .4), P(1.19, .45, 4.0), P(1.5, .35, 3.0), P(2.0, .3, 2.5), P(2.52, .15, 1.6), P(3.01, .1, 1.2))),
        "chime" to (1568.0 to listOf(P(1.0, 1.0, 4.5, 4.5), P(2.32, .25, 2.0), P(3.9, .12, 1.0))),
        "gong" to (98.0 to listOf(P(1.0, 1.0, 10.0, .3), P(1.52, .7, 8.0, .5), P(2.11, .45, 6.0), P(2.9, .3, 4.0), P(3.63, .2, 3.0), P(5.1, .12, 2.0))),
    )

    @Synchronized fun bell(kind: String): ShortArray = cache.getOrPut("bell:$kind") {
        val (f0, parts) = BELLS[kind] ?: BELLS.getValue("bowl")
        val secs = if (kind == "chime") 5.0 else if (kind == "gong") 10.0 else 8.0
        val n = (RATE * secs).toInt(); val out = DoubleArray(n)
        for (p in parts) {
            val f = f0 * p.r; val w = 2 * PI * f / RATE; val w2 = 2 * PI * (f + p.beat) / RATE
            for (i in 0 until n) {
                val t = i.toDouble() / RATE
                val env = exp(-t * 3.0 / p.d) * (if (t < .006) t / .006 else 1.0)
                out[i] += p.a * env * (if (p.beat > 0) .6 * sin(w * i) + .4 * sin(w2 * i) else sin(w * i))
            }
        }
        if (kind == "gong") { val r = Random(7); var lp = 0.0
            for (i in 0 until (RATE * .4).toInt()) { lp += (r.nextDouble(-1.0, 1.0) - lp) * .08; out[i] += lp * 1.5 * exp(-i.toDouble() / RATE * 8) } }
        pcm(out, .8)
    }

    /** A soft tone for a turn of the breath: rising on the in-breath, falling on the out-breath, a tick at a hold. */
    @Synchronized fun cue(kind: Char): ShortArray = cache.getOrPut("cue:$kind") {
        val secs = if (kind == 'h' || kind == 'e') .25 else .9
        val n = (RATE * secs).toInt(); val out = DoubleArray(n)
        val (a, b) = when (kind) { 'i', 't' -> 261.6 to 329.6; 'o' -> 329.6 to 220.0; else -> 392.0 to 392.0 }
        var ph = 0.0
        for (i in 0 until n) { val t = i.toDouble() / n
            val f = a + (b - a) * t; ph += 2 * PI * f / RATE
            val env = sin(PI * t).let { it * it }
            out[i] = env * (sin(ph) + .25 * sin(2 * ph)) }
        pcm(out, .35)
    }

    /**
     * A breath (from SwissZen): white noise through a band-pass filter whose centre sweeps up on an
     * in-breath and down on an out-breath, as long as the phase.
     */
    @Synchronized fun breath(inhale: Boolean, seconds: Double): ShortArray = cache.getOrPut("br:$inhale:${(seconds * 10).toInt()}") {
        val n = (seconds * RATE).toInt().coerceAtLeast(1)
        val out = ShortArray(n)
        val rnd = Random(42)
        val f0 = if (inhale) 360.0 else 1250.0; val f1 = if (inhale) 1250.0 else 320.0
        val peakAt = if (inhale) .6 else .18; val peak = if (inhale) .11 else .08
        var x1 = 0.0; var x2 = 0.0; var y1 = 0.0; var y2 = 0.0
        var b0 = 0.0; var b2 = 0.0; var a1 = 0.0; var a2 = 0.0
        for (i in 0 until n) {
            val p = i.toDouble() / n
            if (i % 64 == 0) {
                val f = f0 * exp(kotlin.math.ln(f1 / f0) * p)
                val w = 2 * PI * f / RATE
                val alpha = sin(w) / (2 * .8); val a0 = 1 + alpha
                b0 = alpha / a0; b2 = -alpha / a0; a1 = -2 * kotlin.math.cos(w) / a0; a2 = (1 - alpha) / a0
            }
            val x = rnd.nextDouble() * 2 - 1
            val y = b0 * x + b2 * x2 - a1 * y1 - a2 * y2
            x2 = x1; x1 = x; y2 = y1; y1 = y
            val env = if (p < peakAt) p / peakAt else 1 - (p - peakAt) / (1 - peakAt)
            out[i] = (y * env * env * peak * 4.0 * Short.MAX_VALUE).toInt().coerceIn(-32767, 32767).toShort()
        }
        out
    }

    private fun pcm(d: DoubleArray, peak: Double): ShortArray {
        val m = d.maxOf { kotlin.math.abs(it) }.coerceAtLeast(1e-9)
        return ShortArray(d.size) { (d[it] / m * peak * Short.MAX_VALUE).toInt().toShort() }
    }

    private fun track(samples: Int, static: Boolean): AudioTrack = AudioTrack.Builder()
        .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
        .setAudioFormat(AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(RATE).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
        .setBufferSizeInBytes(samples * 2).setTransferMode(if (static) AudioTrack.MODE_STATIC else AudioTrack.MODE_STREAM).build()

    /** Plays a short sound once; the track frees itself when done. */
    fun play(pcm: ShortArray, volume: Float) {
        Thread {
            runCatching {
                val t = track(pcm.size, true)
                t.write(pcm, 0, pcm.size); t.setVolume(volume.coerceIn(0f, 1f)); t.play()
                Thread.sleep(pcm.size * 1000L / RATE + 200)
                t.release()
            }
        }.start()
    }

    fun ring(kind: String, volume: Float) { Thread { runCatching { play(bell(kind), volume) } }.start() }
    fun warm() { Thread { runCatching { bell("bowl"); cue('i'); cue('o'); cue('h') } }.start() }
}

/**
 * The background, made sample by sample: sea, rain, wind, fire. When [breath] is 0..1 the sea swells
 * with it (the pet's breath), otherwise it keeps its own slow waves.
 */
class Ambience(private val kind: String) {
    @Volatile var volume = .5f
    @Volatile var breath = -1f
    @Volatile private var on = false
    private var thread: Thread? = null

    fun start() {
        if (on || kind == "none") return
        on = true
        thread = Thread {
            val buf = ShortArray(Synth.RATE / 10)
            val t = runCatching { AudioTrack.Builder()
                .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
                .setAudioFormat(AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(Synth.RATE).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                .setBufferSizeInBytes(buf.size * 8).setTransferMode(AudioTrack.MODE_STREAM).build() }.getOrNull() ?: return@Thread
            t.play()
            val r = Random(System.nanoTime())
            var lp = 0.0; var lp2 = 0.0; var hp = 0.0; var brown = 0.0
            var i = 0L; var swell = .5; var fade = 0.0
            var crack = 0.0
            while (on) {
                for (k in buf.indices) {
                    val time = i.toDouble() / Synth.RATE
                    val white = r.nextDouble(-1.0, 1.0)
                    brown = (brown + white * .02).coerceIn(-1.0, 1.0) * .998
                    val s = when (kind) {
                        "sea" -> {
                            val target = if (breath >= 0) .25 + .75 * breath else .3 + .7 * (.5 + .5 * sin(2 * PI * time / 9.5 + sin(time / 23.0))).let { it * it }
                            swell += (target - swell) * .00012
                            val cut = .02 + .1 * swell
                            lp += (white - lp) * cut; lp2 += (lp - lp2) * cut
                            (lp2 * 3.2 + brown * .6) * (.25 + .75 * swell)
                        }
                        "rain" -> {
                            lp += (white - lp) * .5; hp = white - lp
                            var v = hp * .35 + brown * .25
                            if (r.nextDouble() < .0009) crack = r.nextDouble(.3, .8)
                            if (crack > .01) { v += crack * r.nextDouble(-1.0, 1.0); crack *= .92 }
                            v
                        }
                        "wind" -> {
                            val cut = .01 + .05 * (.5 + .5 * sin(2 * PI * time / 13.0 + sin(time / 5.0)))
                            lp += (white - lp) * cut; lp2 += (lp - lp2) * cut
                            lp2 * 4.0
                        }
                        "fire" -> {
                            var v = brown * 1.1
                            if (r.nextDouble() < .0016) crack = r.nextDouble(.4, 1.0)
                            if (crack > .01) { v += crack * white * .9; crack *= .8 }
                            lp += (v - lp) * .35; lp
                        }
                        else -> 0.0
                    }
                    fade = (fade + 1.0 / (Synth.RATE * 2.5)).coerceAtMost(1.0)
                    buf[k] = (s.coerceIn(-1.0, 1.0) * fade * Short.MAX_VALUE * .5).toInt().toShort()
                    i++
                }
                t.setVolume(volume.coerceIn(0f, 1f))
                t.write(buf, 0, buf.size)
            }
            runCatching { t.pause(); t.flush(); t.release() }
        }.apply { isDaemon = true; start() }
    }

    fun stop() { on = false; thread = null }
}
