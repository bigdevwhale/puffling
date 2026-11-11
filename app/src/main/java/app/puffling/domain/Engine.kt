package app.puffling.domain

import app.puffling.data.Bell
import app.puffling.data.Pet
import app.puffling.data.Session
import app.puffling.data.Settings
import kotlin.math.max
import kotlin.math.min

/** Practices that are not practices: the bell's breaths and the hold test. */
fun Session.isPractice() = practice != "bell" && practice != "bolt"
fun Session.min() = secs / 60.0
fun Session.prac(): Practice? = Practices.find(practice)

/** Sums over sessions that decide which eggs are found. */
class Tally(val sessions: List<Session>, val bells: List<Bell>, val s: Settings, val entries: List<app.puffling.data.Entry> = emptyList()) {
    val real = sessions.filter { it.isPractice() }
    private fun secsOf(f: (Practice) -> Boolean) = real.filter { it.prac()?.let(f) == true }.sumOf { it.secs }
    private fun countOf(f: (Practice) -> Boolean) = real.count { it.prac()?.let(f) == true }
    private fun countId(id: String) = real.count { it.practice == id }
    val sitSecs = real.filter { it.kind == Kind.SIT.code }.sumOf { it.secs }
    val gratitude = countId("gratitude")
    val slowSecs = secsOf { it.slow }
    val maxHold = sessions.flatMap { it.holdList() }.maxOrNull() ?: 0
    val wimhof = real.count { it.kind == Kind.WIMHOF.code }
    val coherentSecs = real.filter { it.practice == "coherent" }.sumOf { it.secs }
    val late9 = real.count { hourOf(it.start) >= 21 }
    val late10 = real.count { val h = hourOf(it.start); h >= 22 || h < 3 }
    val walks = countId("walk")
    val candles = countId("candle")
    val sos = countId("sos")
    val days = real.map { dayOf(it.start) }.toSortedSet()
    val maxGap = days.zipWithNext { a, b -> b - a }.maxOrNull() ?: 0
    val longest = real.maxOfOrNull { it.secs } ?: 0
    val bellsAnswered = bells.count { it.answered > 0 }
    val said = sessions.sumOf { it.said }
    val longExhale = countOf { it.longExhale }
    val distinct = real.map { it.practice }.filter { it != "sos" }.toSet().size
    val boxes = countOf { it.box }
    val totalSecs = real.sumOf { it.secs }
    val els = real.map { it.el }.toSet()
    val namedTwo = entries.count { it.feelingIds().size >= 2 }
    val words = entries.sumOf { Diary.words(it.text) }
    val entryDays = entries.map { dayOf(it.at) }.toSet().size
}

/** Weeks and the tide: the minutes of each week against the goal. */
class Tides(sessions: List<Session>, val goal: Int) {
    val minutes: Map<Long, Double> = sessions.filter { it.practice != "bolt" }.groupBy { weekStart(dayOf(it.start)) }.mapValues { e -> e.value.sumOf { it.secs } / 60.0 }
    fun of(week: Long) = minutes[week] ?: 0.0
    fun met(week: Long) = of(week) >= goal
    /** Weeks in a row at high tide, this week counted once it's reached. A missed week is just a low tide: the streak starts again, nothing is lost. */
    val streak: Int = run { var w = weekStart(today()); var n = 0; if (met(w)) n++; w -= 7; while (met(w)) { n++; w -= 7 }; n }
    val best: Int = run {
        if (minutes.isEmpty()) return@run 0
        var w = minutes.keys.min(); val end = weekStart(today()); var run = 0; var best = 0
        while (w <= end) { if (met(w)) { run++; best = max(best, run) } else if (w != end) run = 0; w += 7 }
        best
    }
}

/** A thing found on the sea floor: deeper holds find more. [secs] is the hold that reaches it. */
data class Find(val id: String, val ru: String, val en: String, val secs: Int) {
    val name get() = tr(ru, en)
    val metres get() = depthOf(secs)
}

/** How deep a hold of [secs] takes the pet: half a metre a second. */
fun depthOf(secs: Int) = secs / 2

object Deep {
    val all = listOf(
        Find("star", "Морская звезда", "Starfish", 15), Find("clown", "Рыба-клоун", "Clownfish", 25), Find("horse", "Морской конёк", "Seahorse", 35),
        Find("urchin", "Морской ёж", "Sea urchin", 45), Find("kelp", "Ламинария с улиткой", "Kelp and a snail", 55), Find("turtle", "Морская черепаха", "Sea turtle", 65),
        Find("ray", "Скат", "Eagle ray", 75), Find("pearl", "Жемчужина", "A pearl", 85), Find("bell", "Корабельный колокол", "A ship's bell", 95),
        Find("octopus", "Осьминог в бутылке", "Octopus in a bottle", 105), Find("lantern", "Рыба-фонарь", "Lanternfish", 115), Find("ghost", "Призрачная медуза", "Ghost jellyfish", 130),
        Find("angler", "Удильщик", "Anglerfish", 145), Find("squid", "Гигантский кальмар", "Giant squid", 160), Find("statue", "Каменная голова", "Stone head", 180),
        Find("crystal", "Кристалл бездны", "Abyss crystal", 210),
    )
    val byId = all.associateBy { it.id }
    fun reached(secs: Int) = all.filter { secs >= it.secs }
}

/** What a finished session gave. */
data class Outcome(
    val food: Double, val shells: Int, val eggs: List<String>, val hatched: List<Pet>, val quests: List<Quest>,
    val shares: Map<Long, Double>, val bonus: List<String>, val finds: List<String>, val depth: Int, val first: Boolean,
    val bottles: Int, val course: Boolean,
)

object Engine {
    fun team(pets: List<Pet>) = pets.filter { it.slot in 0..2 }.sortedBy { it.slot }

    /**
     * Feeds the team for session [x]. [before] are the sessions finished before it.
     * Returns the outcome (without eggs, quests and finds, which the app adds) and the pets as they are now.
     */
    fun feed(x: Session, before: List<Session>, pets: List<Pet>, s: Settings): Pair<Outcome, List<Pet>> {
        val team = team(pets)
        val species = team.map { it.species }.toSet()
        val pr = x.prac()
        val hour = hourOf(x.start)
        val lastDay = before.filter { it.isPractice() }.maxOfOrNull { dayOf(it.start) }
        val gap = if (lastDay == null) 0L else dayOf(x.start) - lastDay
        val bell = x.practice == "bell"
        val minutes = x.secs / 60.0
        val bonus = ArrayList<String>()
        var base = when {
            bell -> 1.0
            x.practice == "bolt" -> .5
            x.kind == Kind.WIMHOF.code -> minutes * 1.2
            else -> max(.5, minutes)
        }
        val holdsOverMin = x.holdList().filter { it > 60 }.sumOf { it - 60 } / 60.0
        var m = 1.0
        for (p in team) if (p.sp().likes?.id == x.el && !bell) m += .5
        fun add(id: String, ok: Boolean, k: Double, ru: String, en: String) { if (id in species && ok) { m += k; bonus += tr(ru, en) } }
        add("sloth", pr?.slow == true, 1.0, "Ленивец: медленное дыхание, ×2", "Sloth: slow breathing, ×2")
        add("jelly", x.practice == "coherent", 1.0, "Медуза: ровное дыхание, ×2", "Jellyfish: coherent breathing, ×2")
        add("seal", pr?.box == true, 1.0, "Тюлень: квадрат, ×2", "Seal: box breathing, ×2")
        add("tortoise", pr?.longExhale == true, .5, "Черепаха: длинный выдох, ×1,5", "Tortoise: long exhale, ×1.5")
        add("macaque", x.kind == Kind.WIMHOF.code && hour < 10, .5, "Макака: утренний Вим Хоф", "Snow monkey: morning Wim Hof")
        add("owl", hour >= 20 && !bell, .5, "Сова: вечерняя практика, ×1,5", "Owl: an evening practice, ×1.5")
        add("hermit", x.practice == "sos", 1.0, "Отшельник: после бури – вдвое", "Hermit crab: after the storm – double")
        add("axolotl", gap >= 7 && !bell, 2.0, "Аксолотль: возвращение через $gap дн., ×3", "Axolotl: back after $gap days, ×3")
        add("whale", x.secs >= 1200, 1.0, "Китёнок: долгая практика, ×2", "Whale calf: a long practice, ×2")
        add("capybara", bell, 1.0, "Капибара: колокольчик, ×2", "Capybara: a bell, ×2")
        if ("dragon" in species) m += .3
        if ("puffer" in species && holdsOverMin > 0) { base += holdsOverMin; bonus += tr("Рыба-ёж: долгие задержки", "Pufferfish: long holds") }
        var total = base * m
        if ("frog" in species && x.said > 0) { total += x.said; bonus += tr("Лягушка: +${x.said} за слова вслух", "Frog: +${x.said} for words aloud") }

        // shells
        val seen = before.map { it.practice }.toSet()
        val first = x.isPractice() && x.practice !in seen && before.isNotEmpty()
        var shells = when { bell -> 1; x.practice == "bolt" -> 1; else -> 3 + min(12, (minutes / 2).toInt()) }
        if (bell && "capybara" in species) shells += 1
        if (first && "fox" in species) { shells += 5; bonus += tr("Лисёнок: новая практика, +5 ракушек", "Fox kit: a new practice, +5 shells") }

        val shares = HashMap<Long, Double>()
        val hatched = ArrayList<Pet>()
        val tides = Tides(before, s.weekGoal)
        val prevMet = tides.met(weekStart(dayOf(x.start)) - 7)
        val now = pets.map { p ->
            if (p.slot !in 0..2) {
                if (p.hatched != 0L) return@map p
                val g = p.growth + total * .25; val h = if (g >= Pets.HATCH) dayOf(x.start) else 0L
                return@map p.copy(growth = g, hatched = h).also { if (h > 0) hatched += it }
            }
            val nat = p.nat()
            var share = if (p.slot == 0) 1.0 else if (nat == Nature.SOCIAL) .75 else .5
            var eat = total * share
            if (nat == Nature.EARLY && hour < 9 && !bell) eat *= 1.3
            if (nat == Nature.PATIENT && x.secs >= 900) eat *= 1.3
            if (nat == Nature.STEADY && prevMet) eat *= 1.15
            if (nat == Nature.CHATTY) eat += x.said * share
            shares[p.id] = eat
            val els = p.elMap().toMutableMap(); els[x.el] = (els[x.el] ?: 0.0) + eat
            if (p.hatched == 0L) {
                val g = p.growth + eat; val h = if (g >= Pets.HATCH) dayOf(x.start) else 0L
                p.copy(growth = g, hatched = h, els = elString(els), sessions = p.sessions + 1).also { if (h > 0) hatched += it }
            } else p.copy(belly = p.belly + eat, els = elString(els), sessions = p.sessions + 1)
        }
        val depth = depthOf(x.holdList().maxOrNull() ?: 0)
        return Outcome(total, shells, emptyList(), hatched, emptyList(), shares, bonus, emptyList(), depth, first, 0, false) to now
    }

    /**
     * Feeds the team for a diary entry: a little calm for naming feelings, more for words.
     * Returns the calm eaten, the shells, the bonuses and the pets as they are now.
     */
    fun feedEntry(e: app.puffling.data.Entry, before: List<app.puffling.data.Entry>, pets: List<Pet>): Triple<Double, List<String>, List<Pet>> {
        val team = team(pets); val species = team.map { it.species }.toSet()
        val words = Diary.words(e.text)
        var food = 2.0 + e.feelingIds().size * .5 + min(4.0, words / 40.0)
        val bonus = ArrayList<String>()
        if ("chameleon" in species && e.feelingIds().size >= 2) { food *= 2; bonus += tr("Хамелеон: несколько чувств, ×2", "Chameleon: several feelings, ×2") }
        if ("raven" in species && words >= 50) { food += words / 50; bonus += tr("Воронёнок: +${words / 50} за слова", "Raven chick: +${words / 50} for words") }
        if ("snail" in species && before.any { dayOf(it.at) == dayOf(e.at) - 1 }) { food *= 1.5; bonus += tr("Улитка: запись и вчера, +50%", "Snail: an entry yesterday too, +50%") }
        if ("dragon" in species) food *= 1.3
        val now = pets.map { p ->
            if (p.slot !in 0..2) return@map if (p.hatched == 0L) p.copy(growth = p.growth + food * .25).let { if (it.growth >= Pets.HATCH) it.copy(hatched = dayOf(e.at)) else it } else p
            val share = if (p.slot == 0) 1.0 else if (p.nat() == Nature.SOCIAL) .75 else .5
            val eat = food * share
            val els = p.elMap().toMutableMap(); els["heart"] = (els["heart"] ?: 0.0) + eat
            if (p.hatched == 0L) p.copy(growth = p.growth + eat, els = elString(els)).let { if (it.growth >= Pets.HATCH) it.copy(hatched = dayOf(e.at)) else it }
            else p.copy(belly = p.belly + eat, els = elString(els))
        }
        return Triple(food, bonus, now)
    }

    /** Eggs whose conditions [t] now meets and that aren't owned yet. */
    fun finds(t: Tally, owned: Set<String>): List<String> {
        val out = ArrayList<String>()
        fun f(id: String, ok: Boolean) { if (ok && id !in owned) out += id }
        f("cat", t.sitSecs >= 3 * 3600)
        f("panda", t.gratitude >= 10)
        f("sloth", t.slowSecs >= 2 * 3600)
        f("puffer", t.maxHold >= 90)
        f("macaque", t.wimhof >= 10)
        f("jelly", t.coherentSecs >= 3600)
        f("owl", t.late9 >= 10)
        f("koala", t.late10 >= 7)
        f("heron", t.walks >= 5)
        f("moth", t.candles >= 5)
        f("hermit", t.sos >= 1)
        f("axolotl", t.maxGap >= 14)
        f("whale", t.longest >= 1800)
        f("capybara", t.bellsAnswered >= 40)
        f("frog", t.said >= 50)
        f("tortoise", t.longExhale >= 15)
        f("fox", t.distinct >= 12)
        f("seal", t.boxes >= 15)
        f("chameleon", t.namedTwo >= 15)
        f("raven", t.words >= 1500)
        f("snail", t.entryDays >= 20)
        f("dragon", t.totalSecs >= 50 * 3600 && t.els.size >= Element.entries.size)
        return out
    }

    /** How near an egg is, 0..1, and the words for it. */
    fun near(id: String, t: Tally): Pair<Float, String> {
        fun r(v: Double, of: Double, txt: String) = (v / of).toFloat().coerceIn(0f, 1f) to txt
        fun h(sec: Int) = hours(sec.toLong())
        return when (id) {
            "cat" -> r(t.sitSecs.toDouble(), 3 * 3600.0, "${h(t.sitSecs)} / ${tr("3 ч", "3 h")}")
            "panda" -> r(t.gratitude.toDouble(), 10.0, "${t.gratitude} / 10")
            "sloth" -> r(t.slowSecs.toDouble(), 7200.0, "${h(t.slowSecs)} / ${tr("2 ч", "2 h")}")
            "puffer" -> r(t.maxHold.toDouble(), 90.0, "${clock(t.maxHold)} / 1:30")
            "macaque" -> r(t.wimhof.toDouble(), 10.0, "${t.wimhof} / 10")
            "jelly" -> r(t.coherentSecs.toDouble(), 3600.0, "${dur(t.coherentSecs)} / ${tr("1 ч", "1 h")}")
            "owl" -> r(t.late9.toDouble(), 10.0, "${t.late9} / 10")
            "koala" -> r(t.late10.toDouble(), 7.0, "${t.late10} / 7")
            "heron" -> r(t.walks.toDouble(), 5.0, "${t.walks} / 5")
            "moth" -> r(t.candles.toDouble(), 5.0, "${t.candles} / 5")
            "hermit" -> r(t.sos.toDouble(), 1.0, "${t.sos} / 1")
            "axolotl" -> r(t.maxGap.toDouble(), 14.0, tr("перерыв ${t.maxGap} / 14 дн.", "break ${t.maxGap} / 14 days"))
            "whale" -> r(t.longest.toDouble(), 1800.0, "${dur(t.longest)} / ${tr("30 мин", "30 min")}")
            "capybara" -> r(t.bellsAnswered.toDouble(), 40.0, "${t.bellsAnswered} / 40")
            "frog" -> r(t.said.toDouble(), 50.0, "${t.said} / 50")
            "tortoise" -> r(t.longExhale.toDouble(), 15.0, "${t.longExhale} / 15")
            "fox" -> r(t.distinct.toDouble(), 12.0, "${t.distinct} / 12")
            "seal" -> r(t.boxes.toDouble(), 15.0, "${t.boxes} / 15")
            "chameleon" -> r(t.namedTwo.toDouble(), 15.0, "${t.namedTwo} / 15")
            "raven" -> r(t.words.toDouble(), 1500.0, "${fmt(t.words)} / ${fmt(1500)}")
            "snail" -> r(t.entryDays.toDouble(), 20.0, "${t.entryDays} / 20")
            "dragon" -> r(min(t.totalSecs / (50 * 3600.0), t.els.size / 6.0), 1.0, "${hours(t.totalSecs.toLong())} / ${tr("50 ч", "50 h")} · ${t.els.size} / 6")
            else -> 0f to ""
        }
    }

    /**
     * The nights from the day after [from] to yesterday: everything in the belly turns into growth while the pets sleep.
     * The koala and the dreamers grow more after an evening practice. Returns the pets and news of new stages.
     */
    fun digest(pets: List<Pet>, from: Long, until: Long, sessions: List<Session>): Pair<List<Pet>, List<String>> {
        if (from >= until - 1) return pets to emptyList()
        val news = ArrayList<String>()
        val koala = pets.any { it.species == "koala" && it.slot in 0..2 && it.hatched > 0 }
        val evenings = sessions.filter { it.isPractice() && hourOf(it.start) >= 18 }.map { dayOf(it.start) }.toSet()
        val out = pets.map { p0 ->
            if (p0.hatched == 0L || p0.belly <= 0) return@map p0
            var p = p0; val before = p.stage()
            var d = from + 1
            while (d < until && p.belly > 0) {
                var grow = p.belly
                if (koala && d in evenings) grow *= 1.25
                if (p.nat() == Nature.DREAMY) grow *= 1.2
                p = p.copy(growth = p.growth + grow, belly = 0.0)
                d++
            }
            val after = p.stage()
            if (after != before) {
                news += "stage:${p.id}:${after.ordinal}"
                if (after >= Stage.ADULT && p.form.isEmpty()) p = p.copy(form = Form.from(p.elMap()).id)
            }
            p
        }
        return out to news
    }
}
