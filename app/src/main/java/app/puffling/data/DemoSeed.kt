package app.puffling.data

import app.puffling.AppContainer
import app.puffling.Done
import app.puffling.domain.Lang
import app.puffling.domain.Nature
import app.puffling.domain.Practices
import app.puffling.domain.today
import java.time.LocalDate
import java.time.ZoneId
import kotlin.random.Random

/**
 * Four months of practice for screenshots (demo builds only), played through the real engine:
 * the pets eat, eggs turn up, the aquarium fills, bottles sail off and come back.
 */
object DemoSeed {
    suspend fun seedIfEmpty(c: AppContainer) {
        if (c.dao.sessionList().isNotEmpty() || c.settings.now().onboarded) return
        val r = Random(11)
        val days = 120
        val start = today() - days
        c.settings.update { it.copy(onboarded = true, digested = start - 1, bells = true, weekGoal = 60, course = "calm", courseDay = 4, courseLast = today() - 1,
            town = if (Lang.ru) "Мурманск" else "Edinburgh", lat = if (Lang.ru) 68.97 else 55.95, lon = if (Lang.ru) 33.07 else -3.19, guessed = true,
            seen = if (app.puffling.BuildConfig.SHOTS) setOf("home", "library", "family", "pets", "pet", "stats", "shop", "diary", "entry", "begin", "result", "session", "course", "own", "bolt", "words", "aquarium", "settings") else emptySet(), fem = false) }
        c.dao.insert(c.newPet("puffin", 0, hatched = start, nature = Nature.DREAMY).copy(name = if (Lang.ru) "Пых" else "Puff", shiny = false))
        c.dao.insert(c.newPet("cat", 1, nature = Nature.PATIENT).copy(name = if (Lang.ru) "Дымка" else "Misty", shiny = false))
        listOf(if (Lang.ru) "Я справляюсь, шаг за шагом." else "I'm coping, step by step.", if (Lang.ru) "Мне можно отдыхать." else "I'm allowed to rest.").forEach {
            c.dao.insert(OwnAff(text = it, created = System.currentTimeMillis())) }
        c.saveOwn(OwnPractice(name = if (Lang.ru) "Утро у окна" else "Morning at the window", kind = "b", el = "air", minutes = 5, pattern = "i4,h2,o6,e2"))
        val zone = ZoneId.systemDefault()
        fun at(day: Long, h: Int, m: Int) = LocalDate.ofEpochDay(day).atTime(h, m).atZone(zone).toInstant().toEpochMilli()
        val morning = listOf("box", "coherent", "sigh", "u1", "wimhof", "bellows", "triangle")
        val evening = listOf("478", "scan", "exhale", "gratitude", "sit", "humming", "metta", "count")
        val other = listOf("ground", "walk", "tea", "candle", "noting", "sky", "sound", "affirm", "nadi")
        val grat = if (Lang.ru) listOf("Солнце на кухне утром", "Позвонила сестра", "Вкусный хлеб из пекарни", "Дошёл до моря", "Тёплый плед", "Коллега принёс кофе", "Кот уснул на коленях")
            else listOf("Sun in the kitchen this morning", "My sister called", "Good bread from the bakery", "Walked down to the sea", "A warm blanket", "A colleague brought coffee", "The cat fell asleep on my lap")
        for (k in 0 until days) {
            val day = start + k
            val n = when { r.nextFloat() < .18f -> 0; r.nextFloat() < .55f -> 1; else -> 2 }
            for (i in 0 until n) {
                val eve = i == 1 || r.nextFloat() < .35f
                val id = when { eve -> evening.random(r); r.nextFloat() < .25f -> other.random(r); else -> morning.random(r) }
                val p = Practices.find(id) ?: continue
                val mins = when { p.kind.code == "g" -> p.guidedSecs / 60; p.kind.code == "w" -> 10 + r.nextInt(5); p.kind.code == "a" -> 3; else -> listOf(3, 5, 5, 10, 10, 15, 20).random(r) }
                val holds = if (p.kind.code == "w") (0 until 3).map { j -> 40 + k / 2 + j * 15 + r.nextInt(20) } else emptyList()
                val before = if (r.nextFloat() < .8f) 2 + r.nextInt(3) else 0
                val after = if (before > 0) (before - r.nextInt(3)).coerceAtLeast(1) else 0
                val said = if (id == "affirm") listOf("Я выбираю медленный темп.", "Мне хватает времени на главное.").take(1 + r.nextInt(2)) else emptyList()
                val notes = if (id == "gratitude") grat.shuffled(r).take(3) else emptyList()
                c.finish(Done(id, at(day, if (eve) 21 + r.nextInt(2) else 7 + r.nextInt(3), r.nextInt(60)), mins * 60 + r.nextInt(40), holds = holds,
                    bpm = if (r.nextFloat() < .15f) 9.0 - k / 30.0 + r.nextDouble() else 0.0, before = before, after = after, notes = notes, said = said))
            }
            for (b in 0 until 3 + r.nextInt(2)) c.dao.insert(Bell(at = at(day, 10 + b * 3, r.nextInt(60)), answered = if (r.nextFloat() < .7f) at(day, 10 + b * 3, 59) else 0))
            if (k % 7 == 3) c.finish(Done("bolt", at(day, 8, 0), 18 + k / 8, holds = listOf(18 + k / 8 + r.nextInt(4))))
        }
        // the diary: a few evenings a week
        val notes = if (Lang.ru) listOf("Долго не мог уснуть, мысли про работу.", "Гуляли у моря, ветер был тёплый.", "Сорвался на брата, потом извинился.", "Закончил проект, наконец-то!",
            "Устал, но день был хороший.", "Тревожно перед завтрашним разговором.", "Позвонила мама, долго болтали.", "Ничего не хотелось, весь день лежал.", "Первый раз продержался две минуты в задержке.")
            else listOf("Couldn't fall asleep, kept thinking about work.", "Walked by the sea, the wind was warm.", "Snapped at my brother, then apologised.", "Finished the project at last!",
            "Tired, but it was a good day.", "Anxious about tomorrow's talk.", "Mum called, we chatted for ages.", "Didn't feel like anything, lay around all day.", "Held my breath for two minutes for the first time.")
        val feelPool = app.puffling.domain.Diary.moods.flatMap { m -> m.words.take(4).map { it.id } }
        for (k in 0 until days) { if (r.nextFloat() > .45f) continue
            val day = start + k
            val main = app.puffling.domain.Diary.moods[if (r.nextFloat() < .55f) r.nextInt(2) else 2 + r.nextInt(4)]
            val fs = (listOf(main.words.random(r).id) + if (r.nextFloat() < .5f) listOf(feelPool.random(r)) else emptyList()).distinct()
            c.saveEntry(Entry(at = at(day, 21, r.nextInt(50)), emotions = fs.joinToString(","), strength = 1 + r.nextInt(5),
                tags = app.puffling.domain.Diary.tags.shuffled(r).take(1 + r.nextInt(2)).joinToString(",") { it.id }, text = if (r.nextFloat() < .7f) notes.random(r) + " " + notes.random(r) else ""))
        }
        c.dao.insertBottles(listOf(Bottle(text = if (Lang.ru) "Я в безопасности в этот момент." else "I am safe in this moment.", sent = today() - 30, back = today())))
        c.settings.update { it.copy(items = setOf("cushion", "bowl", "bonsai", "plant", "chime", "lantern", "scroll", "candles"), shells = 140, digested = start - 1) }
        c.tick()
        // put the found eggs to work: the team is whoever grew most
        val pets = c.dao.petList()
        pets.filter { it.hatched == 0L }.forEach { c.dao.update(it.copy(growth = 20.0, hatched = today() - 3, belly = 0.0)) }
        val slow = c.dao.petList().firstOrNull { it.species == "sloth" } ?: c.dao.petList().firstOrNull { it.species != "puffin" && it.species != "cat" }
        slow?.let { c.setSlot(it, 2) }
        c.settings.update { it.copy(inbox = emptyList()) }
    }
}
