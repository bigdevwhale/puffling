package app.puffling.domain

import app.puffling.data.Entry

/** A family of feelings, with its colour; [words] are the feelings in it. */
data class Mood(val id: String, val ru: String, val en: String, val color: Long, val words: List<Feeling>) {
    val name get() = tr(ru, en)
}

data class Feeling(val id: String, val ru: String, val en: String, val mood: String) {
    val name get() = tr(ru, en)
}

/** What a diary entry was about. */
data class Tag(val id: String, val ru: String, val en: String) { val name get() = tr(ru, en) }

object Diary {
    private fun m(id: String, ru: String, en: String, color: Long, vararg w: Pair<String, String>) =
        Mood(id, ru, en, color, w.map { (r, e) -> Feeling("$id:${e.lowercase().replace(' ', '_')}", r, e, id) })

    val moods = listOf(
        m("joy", "Радость", "Joy", 0xFFE8B23A, "радость" to "Joy", "благодарность" to "Gratitude", "гордость" to "Pride", "нежность" to "Tenderness",
            "восторг" to "Delight", "надежда" to "Hope", "вдохновение" to "Inspiration"),
        m("calm", "Спокойствие", "Calm", 0xFF5E9C76, "спокойствие" to "Calm", "умиротворение" to "Peace", "уверенность" to "Confidence",
            "облегчение" to "Relief", "интерес" to "Interest", "тепло" to "Warmth"),
        m("sad", "Грусть", "Sadness", 0xFF5B86C4, "грусть" to "Sadness", "одиночество" to "Loneliness", "тоска" to "Longing",
            "разочарование" to "Disappointment", "сожаление" to "Regret", "растроганность" to "Being moved"),
        m("fear", "Тревога", "Anxiety", 0xFF8C7BB8, "тревога" to "Anxiety", "страх" to "Fear", "беспокойство" to "Worry",
            "растерянность" to "Confusion", "неловкость" to "Awkwardness", "стыд" to "Shame"),
        m("anger", "Злость", "Anger", 0xFFD9606A, "злость" to "Anger", "раздражение" to "Irritation", "обида" to "Hurt",
            "зависть" to "Envy", "бессилие" to "Helplessness", "нетерпение" to "Impatience"),
        m("tired", "Усталость", "Tiredness", 0xFF8E8A84, "усталость" to "Tiredness", "скука" to "Boredom", "апатия" to "Apathy",
            "опустошённость" to "Emptiness", "сонливость" to "Sleepiness", "перегруз" to "Overload"),
    )
    val byMood = moods.associateBy { it.id }
    val feelings = moods.flatMap { it.words }.associateBy { it.id }
    fun feeling(id: String) = feelings[id]
    fun moodOf(id: String) = byMood[id.substringBefore(':')] ?: moods[1]

    val tags = listOf(
        Tag("sleep", "Сон", "Sleep"), Tag("work", "Работа", "Work"), Tag("people", "Близкие", "People"), Tag("body", "Тело", "Body"),
        Tag("weather", "Погода", "Weather"), Tag("food", "Еда", "Food"), Tag("move", "Движение", "Movement"), Tag("nature", "Природа", "Nature"),
        Tag("alone", "Одиночество", "Being alone"), Tag("money", "Деньги", "Money"), Tag("study", "Учёба", "Study"), Tag("home", "Дом", "Home"),
        Tag("screen", "Экран", "Screens"), Tag("news", "Новости", "News"),
    )
    val byTag = tags.associateBy { it.id }

    /** A question to start from, different each day. */
    fun prompt(day: Long): String {
        val l = listOf(
            tr("Что сегодня было самым живым?", "What felt most alive today?"), tr("Что вы чувствуете прямо сейчас – в теле?", "What do you feel right now – in your body?"),
            tr("Что сегодня забрало силы, а что дало?", "What drained you today, and what gave you strength?"), tr("О чём хочется не забыть?", "What do you want to remember?"),
            tr("Что бы вы сказали другу на своём месте?", "What would you tell a friend in your place?"), tr("Что сегодня получилось, пусть даже мелочь?", "What went right today, even a small thing?"),
            tr("Что сейчас тревожит – одним предложением?", "What worries you now – in one sentence?"), tr("Кого сегодня хочется поблагодарить?", "Who would you like to thank today?"),
            tr("Чего вам сейчас не хватает?", "What are you missing right now?"), tr("Какая мысль сегодня возвращалась чаще всего?", "Which thought kept coming back today?"),
        )
        return l[Math.floorMod(hash("prompt$day"), l.size)]
    }

    fun words(text: String) = text.split(Regex("\\s+")).count { it.any(Char::isLetter) }
}

fun Entry.feelingIds(): List<String> = emotions.split(',').filter { it.isNotBlank() }
fun Entry.tagIds(): List<String> = tags.split(',').filter { it.isNotBlank() }
/** The family most of the entry's feelings belong to. */
fun Entry.mainMood(): Mood? = feelingIds().groupingBy { it.substringBefore(':') }.eachCount().maxByOrNull { it.value }?.key?.let { Diary.byMood[it] }
