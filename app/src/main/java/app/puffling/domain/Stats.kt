package app.puffling.domain

import app.puffling.data.Bell
import app.puffling.data.Session
import java.time.LocalDate

/** A stretch of days: a week, a month, a year or all time. */
data class Span(val kind: Int, val from: Long, val to: Long) {
    fun label(): String = when (kind) {
        WEEK -> "${fmtDayShort(from)} – ${fmtDayShort(to)}"
        MONTH -> LocalDate.ofEpochDay(from).let { "${monthName(it.monthValue).cap()} ${it.year}" }
        YEAR -> LocalDate.ofEpochDay(from).year.toString()
        else -> tr("Всё время", "All time")
    }
    fun prev(): Span = shift(-1)
    fun next(): Span = shift(1)
    private fun shift(k: Int): Span {
        val d = LocalDate.ofEpochDay(from)
        return when (kind) {
            WEEK -> of(WEEK, from + 7 * k)
            MONTH -> of(MONTH, d.plusMonths(k.toLong()).toEpochDay())
            YEAR -> of(YEAR, d.plusYears(k.toLong()).toEpochDay())
            else -> this
        }
    }
    val current get() = today() in from..to
    /** The parts the span is shown in: days of a week, weeks… no – days of a month, months of a year, years of all time. */
    fun parts(first: Long): List<Pair<String, LongRange>> = when (kind) {
        WEEK -> (0 until 7).map { dayShort(it) to (from + it)..(from + it) }
        MONTH -> (from..to).map { "${LocalDate.ofEpochDay(it).dayOfMonth}" to it..it }
        YEAR -> (1..12).map { m -> val a = LocalDate.ofEpochDay(from).withMonth(m); monthShort(m) to a.toEpochDay()..a.plusMonths(1).minusDays(1).toEpochDay() }
        else -> { val y0 = LocalDate.ofEpochDay(minOf(first, today())).year; (y0..LocalDate.now().year).map { y -> "$y" to LocalDate.of(y, 1, 1).toEpochDay()..LocalDate.of(y, 12, 31).toEpochDay() } }
    }

    companion object {
        const val WEEK = 0; const val MONTH = 1; const val YEAR = 2; const val ALL = 3
        fun of(kind: Int, day: Long = today()): Span {
            val d = LocalDate.ofEpochDay(day)
            return when (kind) {
                WEEK -> weekStart(day).let { Span(WEEK, it, it + 6) }
                MONTH -> Span(MONTH, d.withDayOfMonth(1).toEpochDay(), d.withDayOfMonth(d.lengthOfMonth()).toEpochDay())
                YEAR -> Span(YEAR, d.withDayOfYear(1).toEpochDay(), d.withDayOfYear(d.lengthOfYear()).toEpochDay())
                else -> Span(ALL, 0, today())
            }
        }
    }
}

/** What helped: a practice, how many times it was rated, how much calmer it left you on average (points of 1..5). */
data class Help(val practice: String, val name: String, val n: Int, val drop: Double)

class Stats(all: List<Session>, bells: List<Bell>, val span: Span) {
    val inSpan = all.filter { dayOf(it.start) in span.from..span.to }
    val real = inSpan.filter { it.isPractice() }
    val count = real.size
    val secs = inSpan.filter { it.practice != "bolt" }.sumOf { it.secs }
    val minutes = secs / 60
    val days = real.map { dayOf(it.start) }.toSet()
    val byEl: Map<Element, Int> = real.groupBy { Element.of(it.el) }.mapValues { e -> e.value.sumOf { it.secs } }
    val byPractice: List<Pair<String, Int>> = real.groupBy { it.practice }.map { (k, v) -> k to v.size }.sortedByDescending { it.second }
    val longest = real.maxOfOrNull { it.secs } ?: 0
    val rated = real.filter { it.before > 0 && it.after > 0 }
    val drop = if (rated.isEmpty()) 0.0 else rated.map { it.before - it.after }.average()
    val calmer = rated.count { it.after < it.before }
    val helps: List<Help> = rated.groupBy { it.practice }.filter { it.value.size >= 2 }
        .map { (k, v) -> Help(k, Practices.find(k)?.name ?: v.first().name, v.size, v.map { it.before - it.after }.average()) }.sortedByDescending { it.drop }
    val holds = inSpan.filter { it.kind == Kind.WIMHOF.code }.sortedBy { it.start }.map { it.start to (it.holdList().maxOrNull() ?: 0) }
    val bestHold = inSpan.filter { it.practice != "bolt" }.flatMap { it.holdList() }.maxOrNull() ?: 0
    val bolts = all.filter { it.practice == "bolt" }.sortedBy { it.start }.map { it.start to (it.holdList().firstOrNull() ?: it.secs) }
    val bpms = inSpan.filter { it.bpm > 0 }.sortedBy { it.start }.map { it.start to it.bpm }
    val hours: IntArray = IntArray(24).also { h -> real.forEach { h[hourOf(it.start)] += it.secs } }
    val bellsRung = bells.filter { dayOf(it.at) in span.from..span.to }
    val bellsAnswered = bellsRung.count { it.answered > 0 }
    val said = inSpan.sumOf { it.said }
    val shells = inSpan.sumOf { it.shells }
    val gratitude = inSpan.filter { it.notes.isNotBlank() }.flatMap { s -> s.notes.lines().map { dayOf(s.start) to it } }

    fun bars(first: Long): List<Pair<String, Double>> = span.parts(first).map { (l, r) -> l to inSpan.filter { dayOf(it.start) in r && it.practice != "bolt" }.sumOf { it.secs } / 60.0 }
}

/** «12 мин», «1 ч 20 мин» */
fun mins(secs: Int): String { val m = (secs + 30) / 60; return if (m < 60) tr("$m мин", "$m min") else if (m % 60 == 0) tr("${m / 60} ч", "${m / 60} h") else tr("${m / 60} ч ${m % 60} мин", "${m / 60} h ${m % 60} min") }
