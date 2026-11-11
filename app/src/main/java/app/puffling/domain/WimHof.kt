package app.puffling.domain

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * A Wim Hof level (taken from SwissZen). The tempo of one power breath is the user's own, the same for every level.
 * [breaths] ramps inside the level's range round by round, [holds] are the per-round hold targets in seconds.
 */
data class WimHofLevel(val id: String, val ru: String, val en: String, val defaultRounds: Int, val maxRounds: Int, val breaths: List<Int>, val breathsLabel: String, val holds: List<Int>) {
    val name get() = tr(ru, en)
    fun estimateSeconds(rounds: Int, inhale: Double, exhale: Double): Double = (0 until rounds).sumOf { r ->
        WimHofEngine.INTRO + breaths[r] * (inhale + exhale) + WimHofEngine.LAST_IN + WimHofEngine.LAST_OUT + holds[r] + WimHofEngine.REC_IN + WimHofEngine.REC_HOLD + WimHofEngine.REC_OUT }

    companion object {
        val BEGINNER = WimHofLevel("beg", "Начинающий", "Beginner", 3, 3, listOf(30, 30, 30), "30", listOf(30, 60, 90))
        val MEDIUM = WimHofLevel("med", "Средний", "Medium", 3, 3, listOf(30, 33, 35), "30–35", listOf(60, 90, 105))
        val ADVANCED = WimHofLevel("adv", "Продвинутый", "Advanced", 4, 4, listOf(35, 37, 38, 40), "35–40", listOf(60, 90, 120, 150))
        val EXPERT = WimHofLevel("exp", "Эксперт", "Expert", 4, 5, listOf(40, 43, 46, 50, 50), "40–50", listOf(90, 120, 150, 180, 180))
        val ALL = listOf(BEGINNER, MEDIUM, ADVANCED, EXPERT)
        fun byId(id: String) = ALL.firstOrNull { it.id == id } ?: BEGINNER

        /** An own preset: the same breaths every round; hold targets start at [hold] and grow 30 s a round for four rounds. */
        fun custom(rounds: Int, breaths: Int, hold: Int = 60): WimHofLevel {
            val r = rounds.coerceIn(1, 6); val b = breaths.coerceIn(10, 60); val h = hold.coerceIn(15, 300)
            return WimHofLevel("cus", "Свой", "Own", r, r, List(r) { b }, "$b", List(r) { h + minOf(it, 4) * 30 })
        }
    }
}

enum class WhPhase { INTRO, IN, OUT, LAST_IN, LAST_OUT, HOLD, REC_IN, REC_HOLD, REC_OUT, DONE }

/**
 * A guided Wim Hof session, advanced by [tick] (from SwissZen).
 * Round: intro → N × (in, out) → last in → let go → hold on empty lungs (open, ended by [breatheIn])
 * → recovery in → 15 s hold → let go → next round or DONE.
 */
class WimHofEngine(val level: WimHofLevel, val rounds: Int = level.defaultRounds, val inhale: Double = DEFAULT_INHALE, val exhale: Double = DEFAULT_EXHALE) {
    var phase = WhPhase.INTRO; private set
    var t = 0.0; private set
    var total = 0.0; private set
    var round = 1; private set
    var breath = 1; private set
    val holds = mutableListOf<Double>()

    val breathsThisRound get() = level.breaths[(round - 1).coerceIn(0, level.breaths.lastIndex)]
    val holdTarget get() = level.holds[(round - 1).coerceIn(0, level.holds.lastIndex)]

    fun duration(p: WhPhase = phase): Double = when (p) {
        WhPhase.INTRO -> INTRO; WhPhase.IN -> inhale; WhPhase.OUT -> exhale; WhPhase.LAST_IN -> LAST_IN; WhPhase.LAST_OUT -> LAST_OUT
        WhPhase.HOLD -> Double.POSITIVE_INFINITY; WhPhase.REC_IN -> REC_IN; WhPhase.REC_HOLD -> REC_HOLD; WhPhase.REC_OUT -> REC_OUT; WhPhase.DONE -> 0.0
    }

    /** Advances time; returns the phases entered during this tick (for sounds and haptics). */
    fun tick(dt: Double): List<WhPhase> {
        if (phase == WhPhase.DONE) return emptyList()
        val entered = mutableListOf<WhPhase>()
        var left = dt
        while (left > 0 && phase != WhPhase.DONE) {
            val remaining = duration() - t
            if (left < remaining) { t += left; total += left; left = 0.0 } else { total += remaining; left -= remaining; advance(); entered += phase }
        }
        return entered
    }

    fun breatheIn(): Boolean { if (phase != WhPhase.HOLD) return false; holds += t; enter(WhPhase.REC_IN); return true }
    fun finish() { enter(WhPhase.DONE) }
    private fun enter(p: WhPhase) { phase = p; t = 0.0 }

    private fun advance() {
        when (phase) {
            WhPhase.INTRO -> { breath = 1; enter(WhPhase.IN) }
            WhPhase.IN -> enter(WhPhase.OUT)
            WhPhase.OUT -> if (breath < breathsThisRound) { breath++; enter(WhPhase.IN) } else enter(WhPhase.LAST_IN)
            WhPhase.LAST_IN -> enter(WhPhase.LAST_OUT)
            WhPhase.LAST_OUT -> enter(WhPhase.HOLD)
            WhPhase.HOLD -> Unit
            WhPhase.REC_IN -> enter(WhPhase.REC_HOLD)
            WhPhase.REC_HOLD -> enter(WhPhase.REC_OUT)
            WhPhase.REC_OUT -> if (round < rounds) { round++; enter(WhPhase.INTRO) } else enter(WhPhase.DONE)
            WhPhase.DONE -> Unit
        }
    }

    private fun p() = if (duration().isInfinite() || duration() == 0.0) 0.0 else (t / duration()).coerceIn(0.0, 1.0)
    private fun ease(x: Double) = .5 - .5 * cos(PI * x)

    /** How full the lungs are, 0 empty .. 1 full: what the pet's body follows. */
    fun lungs(): Float = when (phase) {
        WhPhase.INTRO -> .2 + .03 * sin(total * 2.2)
        WhPhase.IN, WhPhase.LAST_IN -> .2 + .8 * ease(p())
        WhPhase.OUT -> 1 - .8 * ease(p())
        WhPhase.REC_OUT -> 1 - .8 * ease(p())
        WhPhase.LAST_OUT -> 1 - ease(p())
        WhPhase.HOLD -> .0
        WhPhase.REC_IN -> ease(p())
        WhPhase.REC_HOLD -> 1.0
        WhPhase.DONE -> .2
    }.toFloat()

    /** The ring: breaths of the round, the hold against its target, the recovery seconds. */
    fun ring(): Float = when (phase) {
        WhPhase.IN -> (breath - 1 + p() * .5) / breathsThisRound
        WhPhase.OUT -> (breath - .5 + p() * .5) / breathsThisRound
        WhPhase.LAST_IN -> 1.0
        WhPhase.LAST_OUT -> 1 - p()
        WhPhase.HOLD -> (t / holdTarget).coerceAtMost(1.0)
        WhPhase.REC_HOLD -> 1 - p()
        else -> 0.0
    }.toFloat()

    /** The big word of the phase and the small one under it. */
    fun words(): Pair<String, String> = when (phase) {
        WhPhase.INTRO -> if (round == 1) tr("Устройтесь удобно", "Get comfortable") to tr("Сядьте или лягте. Плечи и челюсть мягкие.", "Sit or lie down. Soft shoulders and jaw.")
            else tr("Раунд $round", "Round $round") to tr("Ещё раз, в том же ритме.", "Again, at the same pace.")
        WhPhase.IN -> tr("Глубокий вдох", "Fully in") to tr("Сначала в живот, потом в грудь.", "Belly first, then chest.")
        WhPhase.OUT -> tr("Отпустите", "Let go") to tr("Не выталкивайте воздух – просто отпустите.", "Don't push the air out – just let it go.")
        WhPhase.LAST_IN -> tr("Последний вдох – полный", "Last breath – all the way in") to ""
        WhPhase.LAST_OUT -> tr("И выдохните всё", "And let it all out") to tr("Дальше – задержка на пустых лёгких.", "Then hold on empty lungs.")
        WhPhase.HOLD -> tr("Задержка", "Hold") to (if (t > holdTarget) tr("Цель пройдена. Вдыхайте, когда захочется.", "Past the target. Breathe in when you want to.")
            else tr("Лёгкие пусты, тело мягкое. Вдохните, когда тело попросит.", "Lungs empty, body soft. Breathe in when your body asks."))
        WhPhase.REC_IN -> tr("Восстанавливающий вдох", "Recovery breath") to tr("Вдохните полностью.", "Breathe all the way in.")
        WhPhase.REC_HOLD -> tr("Держите", "Hold it") to tr("Грудь полна. Мягко направьте дыхание вверх.", "Chest full. Gently send the breath upwards.")
        WhPhase.REC_OUT -> tr("Отпустите", "Let go") to (if (round < rounds) tr("Скоро следующий раунд.", "Next round soon.") else tr("Это был последний раунд.", "That was the last round."))
        WhPhase.DONE -> "" to ""
    }

    companion object {
        /** 2.0 s in and 1.5 s out puts thirty breaths at about 1:45, like the guided recordings; changeable by 0.1 s. */
        const val DEFAULT_INHALE = 2.0
        const val DEFAULT_EXHALE = 1.5
        const val INTRO = 3.0
        const val LAST_IN = 2.6
        const val LAST_OUT = 3.4
        const val REC_IN = 2.6
        const val REC_HOLD = 15.0
        const val REC_OUT = 3.0
    }
}
