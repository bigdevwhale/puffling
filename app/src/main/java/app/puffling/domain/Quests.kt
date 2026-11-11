package app.puffling.domain

import app.puffling.data.Bell
import app.puffling.data.Session

/** One week's sessions and bells, for quests. */
class Week(val sessions: List<Session>, val bells: List<Bell>, val before: List<Session>, val monday: Long, val entries: Int = 0) {
    val real = sessions.filter { it.isPractice() }
    val minutes = sessions.filter { it.practice != "bolt" }.sumOf { it.secs } / 60.0
}

/** A weekly quest: [progress] is measured on the week, [need] reached means done. */
data class Quest(val id: String, val ru: String, val en: String, val need: Int, val shells: Int, val progress: (Week) -> Int) {
    val name get() = tr(ru, en)
}

object Quests {
    val all = listOf(
        Quest("breath5", "5 дыхательных практик", "5 breathing practices", 5, 15) { w -> w.real.count { it.el == Element.AIR.id } },
        Quest("sit3", "Посидеть в тишине трижды", "Sit in silence three times", 3, 15) { w -> w.real.count { it.kind == Kind.SIT.code } },
        Quest("bells8", "Ответить на 8 колокольчиков", "Answer 8 bells", 8, 15) { w -> w.bells.count { it.answered > 0 } },
        Quest("long15", "Практика от 15 минут", "A practice of 15 minutes", 1, 12) { w -> w.real.count { it.secs >= 900 } },
        Quest("evening3", "Три вечерние практики", "Three evening practices", 3, 12) { w -> w.real.count { hourOf(it.start) >= 19 } },
        Quest("morning3", "Три утренние практики", "Three morning practices", 3, 12) { w -> w.real.count { hourOf(it.start) < 10 } },
        Quest("new1", "Попробовать новую практику", "Try a practice you haven't done", 1, 15) { w -> val seen = w.before.map { it.practice }.toSet(); w.real.map { it.practice }.distinct().count { it !in seen } },
        Quest("said10", "Сказать вслух 10 аффирмаций", "Say 10 affirmations aloud", 10, 12) { w -> w.sessions.sumOf { it.said } },
        Quest("hold90", "Задержка дольше 90 секунд", "A hold of over 90 seconds", 1, 15) { w -> w.sessions.count { s -> s.holdList().any { it >= 90 } } },
        Quest("days5", "Практика в пять разных дней", "Practise on five different days", 5, 20) { w -> w.real.map { dayOf(it.start) }.distinct().size },
        Quest("thanks3", "Три благодарности", "Three gratitude practices", 3, 12) { w -> w.real.count { it.practice == "gratitude" } },
        Quest("body2", "Две телесные практики", "Two body practices", 2, 10) { w -> w.real.count { it.el == Element.EARTH.id } },
        Quest("light2", "Две практики внимания", "Two attention practices", 2, 10) { w -> w.real.count { it.el == Element.LIGHT.id } },
        Quest("min45", "45 минут за неделю", "45 minutes this week", 45, 15) { w -> w.minutes.toInt() },
        Quest("diary3", "Три записи в дневнике", "Three diary entries", 3, 12) { w -> w.entries },
        Quest("elements4", "Четыре разные стихии", "Four different families", 4, 18) { w -> w.real.map { it.el }.distinct().size },
    )
    val byId = all.associateBy { it.id }
    /** Three quests a week, the same all week. */
    fun deal(monday: Long): List<String> = all.sortedBy { hash("q$monday${it.id}") }.take(3).map { it.id }
}

/** A thing for the tea house. Wall things hang ([y] is their centre), floor things stand ([y] is their foot). */
data class Item(val id: String, val ru: String, val en: String, val price: Int, val x: Float, val y: Float, val w: Float, val wall: Boolean = false) {
    val name get() = tr(ru, en)
}

object Shop {
    val all = listOf(
        Item("cushion", "Подушка для медитации", "Meditation cushion", 20, .5f, .97f, .2f),
        Item("bowl", "Поющая чаша", "Singing bowl", 30, .7f, .95f, .1f),
        Item("incense", "Благовония", "Incense", 25, .28f, .92f, .07f),
        Item("plant", "Папоротник", "Fern", 35, .07f, .96f, .14f),
        Item("bonsai", "Бонсай", "Bonsai", 60, .5f, .62f, .13f),
        Item("lantern", "Бумажный фонарь", "Paper lantern", 50, .93f, .95f, .1f),
        Item("chime", "Музыка ветра", "Wind chime", 40, .2f, .2f, .07f, wall = true),
        Item("scroll", "Свиток с волной", "Wave scroll", 55, .82f, .38f, .11f, wall = true),
        Item("tea", "Чайный столик", "Tea table", 70, .3f, .99f, .16f),
        Item("candles", "Свечи", "Candles", 30, .64f, .62f, .07f),
        Item("shelf", "Полка с ракушками", "Shell shelf", 45, .16f, .45f, .14f, wall = true),
        Item("garden", "Сад камней", "Rock garden", 65, .36f, .62f, .12f),
        Item("fishbowl", "Аквариум с рыбкой", "Fishbowl", 80, .83f, .7f, .1f),
        Item("gong", "Гонг", "Gong", 120, .84f, .96f, .16f),
        Item("telescope", "Подзорная труба", "Telescope", 100, .14f, .97f, .12f),
        Item("blanket", "Корзина с пледом", "Blanket basket", 40, .68f, 1f, .11f),
    )
    val byId = all.associateBy { it.id }
}

/** A course: a practice for each day, in order; a missed day just waits. */
data class Course(val id: String, val ru: String, val en: String, val aboutRu: String, val aboutEn: String, val days: List<String>, val color: Long) {
    val name get() = tr(ru, en)
    val about get() = tr(aboutRu, aboutEn)
}

object Courses {
    val all = listOf(
        Course("basics", "Неделя дыхания", "A week of breathing", "Семь коротких практик, чтобы подружиться с дыханием.", "Seven short practices to make friends with your breath.",
            listOf("triangle", "sigh", "box", "exhale", "coherent", "humming", "478"), 0xFF5FA8C9),
        Course("calm", "Когда тревожно", "When anxious", "Когда внутри неспокойно: выдохи, заземление, доброта к себе.", "When it's restless inside: long breaths out, grounding, kindness to yourself.",
            listOf("sigh", "ground", "exhale", "scan", "metta", "coherent", "sky"), 0xFFD9707A),
        Course("sleep", "Неделя сна", "A week of sleep", "Вечерние практики, после которых легче уснуть.", "Evening practices that make sleep come easier.",
            listOf("478", "scan", "exhale", "gratitude", "humming", "scan", "478"), 0xFF6A6FB0),
        Course("wimhof", "Вим Хоф, три недели", "Wim Hof, three weeks", "От мягких раундов к полным. Каждый второй день – отдых и ровное дыхание.", "From gentle rounds to full ones. Every other day rest and coherent breathing.",
            listOf("wimhof_soft", "coherent", "wimhof_soft", "box", "wimhof_soft", "coherent", "wimhof", "coherent", "wimhof", "box", "wimhof", "coherent", "wimhof", "sky",
                "wimhof", "coherent", "wimhof", "box", "wimhof", "coherent", "wimhof"), 0xFF2F6E9E),
        Course("silence", "Десять дней тишины", "Ten days of silence", "Понемногу удлинять тихое сидение и учиться замечать.", "Lengthen silent sitting little by little and learn to notice.",
            listOf("count", "sound", "sit", "noting", "candle", "sit", "sky", "noting", "walk", "sit"), 0xFF8C7BB8),
        Course("kind", "Тёплая неделя", "A warm week", "Благодарность, доброта и тёплые слова – себе и другим.", "Gratitude, kindness and warm words – to yourself and others.",
            listOf("gratitude", "affirm", "metta", "gratitude", "tea", "affirm", "metta"), 0xFFE0A63A),
    )
    val byId = all.associateBy { it.id }
}
