package app.puffling.domain

import app.puffling.data.Pet

/**
 * A kind of pet. [likes] is the family of practice it eats half as much again of (and shares with the team).
 * [find] says how its egg is found; [starter] eggs can be picked on the first day.
 */
data class Species(
    val id: String, val ru: String, val en: String, val likes: Element?,
    val abilityRu: String, val abilityEn: String, val findRu: String, val findEn: String,
    val starter: Boolean = false, val legendary: Boolean = false, val color: Long,
    val names: List<String>, val namesEn: List<String>,
) {
    val name get() = tr(ru, en)
    val ability get() = tr(abilityRu, abilityEn)
    val find get() = tr(findRu, findEn)
}

object Pets {
    val all = listOf(
        Species("puffin", "Тупичок", "Puffling", Element.AIR, "×1,5 за дыхательные практики", "×1.5 for breathing practices",
            "С него всё начинается", "Where it all begins", color = 0xFF4A4550,
            names = listOf("Пых", "Клювик", "Тупа", "Пуфик"), namesEn = listOf("Puff", "Beaky", "Pip", "Tuffy")),
        Species("cat", "Кошка", "Cat", Element.STILL, "×1,5 за тихое сидение", "×1.5 for silent sitting",
            "Стартовое яйцо. Или 3 часа тихого сидения", "A starter egg. Or 3 hours of silent sitting", true, color = 0xFF8D8A93,
            names = listOf("Мурка", "Буся", "Дымка", "Лапка"), namesEn = listOf("Misty", "Purr", "Smokey", "Mitten")),
        Species("panda", "Красная панда", "Red panda", Element.HEART, "×1,5 за благодарность, метту и аффирмации", "×1.5 for gratitude, kindness and affirmations",
            "Стартовое яйцо. Или 10 благодарностей", "A starter egg. Or 10 gratitude practices", true, color = 0xFFC2552E,
            names = listOf("Рыжик", "Ласка", "Огонёк", "Пончик"), namesEn = listOf("Ginger", "Maple", "Ember", "Bun")),
        Species("sloth", "Ленивец", "Sloth", null, "×2 за медленное дыхание – шесть вдохов в минуту и реже", "×2 for slow breathing – six breaths a minute or fewer",
            "Стартовое яйцо. Или 2 часа медленного дыхания", "A starter egg. Or 2 hours of slow breathing", true, color = 0xFF9B8B72,
            names = listOf("Тихон", "Соня", "Ленни", "Обнимашка"), namesEn = listOf("Lenny", "Snooze", "Mellow", "Hugs")),
        Species("puffer", "Рыба-ёж", "Pufferfish", Element.DEEP, "×1,5 за задержки и Вима Хофа; задержки дольше минуты – вдвое", "×1.5 for holds and Wim Hof; holds over a minute count double",
            "Задержать дыхание на полторы минуты", "Hold your breath for a minute and a half", color = 0xFFE3C46A,
            names = listOf("Шарик", "Пузырь", "Иголка", "Бульк"), namesEn = listOf("Bubbles", "Spike", "Puffy", "Blip")),
        Species("macaque", "Снежная макака", "Snow monkey", Element.DEEP, "×1,5 за Вима Хофа; утром – ×2", "×1.5 for Wim Hof; in the morning ×2",
            "10 сессий Вима Хофа", "10 Wim Hof sessions", color = 0xFFB59A86,
            names = listOf("Юки", "Снежок", "Пар", "Онсэн"), namesEn = listOf("Yuki", "Snowy", "Steam", "Onsen")),
        Species("jelly", "Медуза", "Jellyfish", null, "×2 за ровное дыхание", "×2 for coherent breathing",
            "Час ровного дыхания", "An hour of coherent breathing", color = 0xFFB9A2E0,
            names = listOf("Лиля", "Купол", "Свечение", "Желе"), namesEn = listOf("Lilac", "Dome", "Glow", "Jelly")),
        Species("owl", "Сова", "Owl", null, "×1,5 за практики после восьми вечера", "×1.5 for practices after 8 pm",
            "10 вечерних практик после девяти", "10 evening practices after 9 pm", color = 0xFF8A6440,
            names = listOf("Ухтыш", "Луна", "Полночь", "Филя"), namesEn = listOf("Hoot", "Luna", "Midnight", "Moss")),
        Species("koala", "Коала", "Koala", null, "Команда растёт ночью на четверть быстрее, если вечером была практика", "The team grows a quarter faster at night after an evening practice",
            "7 практик перед сном, после десяти вечера", "7 practices before sleep, after 10 pm", color = 0xFF9AA3AD,
            names = listOf("Соня", "Эвкалипт", "Пижама", "Мишка"), namesEn = listOf("Dozy", "Gum", "Pajama", "Teddy")),
        Species("heron", "Цапля", "Heron", Element.EARTH, "×1,5 за телесные практики: скан, ходьбу, чай", "×1.5 for body practices: the scan, walking, tea",
            "5 медленных прогулок", "5 walking meditations", color = 0xFFA9B4BF,
            names = listOf("Шаг", "Тростинка", "Аист", "Цапа"), namesEn = listOf("Stride", "Reed", "Wader", "Stilts")),
        Species("moth", "Мотылёк", "Moth", Element.LIGHT, "×1,5 за практики внимания: свечу, отмечание, звуки", "×1.5 for attention: the candle, noting, listening",
            "5 практик со свечой", "5 candle practices", color = 0xFFE7DCC0,
            names = listOf("Искорка", "Пыльца", "Свечка", "Мотя"), namesEn = listOf("Flicker", "Dusty", "Wick", "Mothy")),
        Species("hermit", "Рак-отшельник", "Hermit crab", null, "×2 за «Штормит»: после бури команда ест вдвое", "×2 for «Rough sea»: after a storm the team eats double",
            "Нажать «Штормит» в трудную минуту", "Press «Rough sea» in a hard moment", color = 0xFFD9744A,
            names = listOf("Домик", "Ракушка", "Тихоня", "Клешня"), namesEn = listOf("Shelly", "Nook", "Quiet", "Pinch")),
        Species("axolotl", "Аксолотль", "Axolotl", null, "×3 за первую практику после перерыва от недели", "×3 for the first practice after a break of a week",
            "Вернуться после перерыва от двух недель", "Come back after a break of two weeks", color = 0xFFF2A7A0,
            names = listOf("Аксик", "Жабрик", "Улыбка", "Коралл"), namesEn = listOf("Axie", "Frills", "Smiley", "Coral")),
        Species("whale", "Китёнок", "Whale calf", null, "×2 за долгие практики – от двадцати минут", "×2 for long practices – twenty minutes and more",
            "Практика на полчаса без перерыва", "A half-hour practice in one go", color = 0xFF4E7FB5,
            names = listOf("Фонтан", "Синька", "Глубь", "Кит"), namesEn = listOf("Spout", "Blue", "Deep", "Moby")),
        Species("capybara", "Капибара", "Capybara", null, "Колокольчики кормят вдвое и приносят по ракушке", "Bells feed double and bring a shell each",
            "Ответить на 40 колокольчиков", "Answer 40 bells", color = 0xFFA77D55,
            names = listOf("Капа", "Юзу", "Дзен", "Плюх"), namesEn = listOf("Capy", "Yuzu", "Zen", "Splash")),
        Species("frog", "Лягушка", "Frog", null, "+1 за каждую аффирмацию, сказанную вслух", "+1 for each affirmation said out loud",
            "Сказать вслух 50 аффирмаций", "Say 50 affirmations out loud", color = 0xFF6EAE5A,
            names = listOf("Ква", "Кувшинка", "Песня", "Зелёнка"), namesEn = listOf("Ribbit", "Lily", "Song", "Pond")),
        Species("tortoise", "Черепаха", "Tortoise", null, "×1,5 за дыхание с длинным выдохом", "×1.5 for breathing with a long exhale",
            "15 практик с длинным выдохом", "15 practices with a long exhale", color = 0xFF8A8F4A,
            names = listOf("Тортилла", "Панцирь", "Не спеши", "Мудрик"), namesEn = listOf("Shelly", "Steady", "Slowpoke", "Sage")),
        Species("fox", "Лисёнок", "Fox kit", null, "+5 ракушек за каждую новую практику, которую вы пробуете впервые", "+5 shells for every practice you try for the first time",
            "Попробовать 12 разных практик", "Try 12 different practices", color = 0xFFE07A3A,
            names = listOf("Хвостик", "Листик", "Рыжуля", "Нюх"), namesEn = listOf("Tails", "Leaf", "Rusty", "Sniff")),
        Species("seal", "Тюлень", "Seal", null, "×2 за квадратное дыхание", "×2 for box breathing",
            "15 раз подышать квадратом", "Box breathing 15 times", color = 0xFF8C9399,
            names = listOf("Кубик", "Ласт", "Пятнышко", "Нерпа"), namesEn = listOf("Cubey", "Flipper", "Speckle", "Selkie")),
        Species("chameleon", "Хамелеон", "Chameleon", null, "×2 за запись в дневнике, где названо больше одного чувства", "×2 for a diary entry that names more than one feeling",
            "15 записей с двумя чувствами и больше", "15 entries naming two feelings or more", color = 0xFF8FCFAE,
            names = listOf("Радуга", "Перелив", "Хамка", "Пастель"), namesEn = listOf("Rainbow", "Shimmer", "Cammy", "Pastel")),
        Species("raven", "Воронёнок", "Raven chick", null, "+1 за каждые 50 слов в дневнике", "+1 for every 50 words in the diary",
            "Написать в дневнике 1500 слов", "Write 1,500 words in the diary", color = 0xFF3C4258,
            names = listOf("Перо", "Чернилка", "Карл", "Строчка"), namesEn = listOf("Quill", "Inky", "Edgar", "Line")),
        Species("snail", "Улитка", "Snail", null, "+50% за запись, если вчера тоже была запись", "+50% for an entry when there was one yesterday too",
            "Записи в дневнике в 20 разных дней", "Diary entries on 20 different days", color = 0xFFE8A88A,
            names = listOf("Домик", "Тихоход", "Ракушка", "Завиток"), namesEn = listOf("Homey", "Slowpoke", "Swirl", "Curl")),
        Species("dragon", "Туманный дракон", "Mist dragon", null, "×1,3 за всё", "×1.3 for everything",
            "Легенда: 50 часов практик и все шесть стихий", "Legend: 50 hours of practice and all six families", legendary = true, color = 0xFFC9C2E6,
            names = listOf("Туман", "Облачко", "Жемчуг", "Дымка"), namesEn = listOf("Mist", "Cloudy", "Pearl", "Haze")),
    )
    val byId = all.associateBy { it.id }
    fun of(id: String) = byId[id] ?: all[0]
    val starters = all.filter { it.starter }

    /** Calm needed to hatch, then for each stage (about a portion a minute of practice). */
    const val HATCH = 15.0
    val STAGES = doubleArrayOf(HATCH, 140.0, 600.0, 1800.0)
}

enum class Stage(val ru: String, val en: String) {
    EGG("Яйцо", "Egg"), BABY("Птенец", "Baby"), TEEN("Подросток", "Youngster"), ADULT("Взрослый", "Grown"), MASTER("Мастер", "Master");
    val label get() = tr(ru, en)
    companion object {
        fun of(p: Pet): Stage = if (p.hatched == 0L) EGG else when {
            p.growth >= Pets.STAGES[3] -> MASTER; p.growth >= Pets.STAGES[2] -> ADULT; p.growth >= Pets.STAGES[1] -> TEEN; else -> BABY }
        fun progress(p: Pet): Float {
            val s = of(p); if (s == MASTER) return 1f
            val lo = if (s == EGG) 0.0 else Pets.STAGES[s.ordinal - 1]; val hi = Pets.STAGES[s.ordinal]
            return ((p.growth - lo) / (hi - lo)).toFloat().coerceIn(0f, 1f)
        }
    }
}

/** A grown pet's way, fixed by the family of practice it was raised on. */
enum class Form(val id: String, val ru: String, val en: String, val aboutRu: String, val aboutEn: String) {
    WIND("air", "Ветерок", "Breeze", "вырос на дыхании", "raised on breathing"),
    DIVER("deep", "Ныряльщик", "Diver", "вырос на задержках", "raised on holds"),
    MONK("still", "Отшельник", "Hermit", "вырос в тишине", "raised in silence"),
    ROOT("earth", "Корешок", "Rooted", "вырос на земле и теле", "raised on body and earth"),
    WARM("heart", "Тёплый", "Warm-hearted", "вырос на добрых словах", "raised on kind words"),
    LAMP("light", "Фонарщик", "Lamplighter", "вырос на внимании", "raised on attention"),
    WHOLE("all", "Гармоничный", "Balanced", "вырос на всём понемногу", "raised on a bit of everything");
    val label get() = tr(ru, en)
    val about get() = tr(aboutRu, aboutEn)
    companion object {
        fun of(id: String) = entries.firstOrNull { it.id == id }
        fun from(els: Map<String, Double>): Form {
            val total = els.values.sum(); if (total <= 0) return WHOLE
            val top = els.maxBy { it.value }
            return if (top.value / total < .4) WHOLE else entries.firstOrNull { it.id == top.key } ?: WHOLE
        }
    }
}

/** A pet's temper: one small rule of its own. */
enum class Nature(val id: String, val ru: String, val en: String, val aboutRu: String, val aboutEn: String) {
    DREAMY("dreamy", "Мечтатель", "Dreamer", "ночью растёт на 20% быстрее", "grows 20% faster at night"),
    EARLY("early", "Жаворонок", "Early bird", "×1,3 за практики до девяти утра", "×1.3 for practices before 9 am"),
    CHATTY("chatty", "Болтушка", "Chatterbox", "+1 за каждую аффирмацию вслух", "+1 for every affirmation said aloud"),
    PATIENT("patient", "Терпеливый", "Patient", "×1,3 за практики от пятнадцати минут", "×1.3 for practices of fifteen minutes or more"),
    SOCIAL("social", "Компанейский", "Social", "в команде ест три четверти, а не половину", "as a companion eats three quarters, not half"),
    STEADY("steady", "Ровный", "Steady", "+15%, если прошлая неделя дошла до прилива", "+15% when last week reached high tide");
    val label get() = tr(ru, en)
    val about get() = tr(aboutRu, aboutEn)
    companion object { fun of(id: String) = entries.firstOrNull { it.id == id } ?: STEADY }
}

fun Pet.sp() = Pets.of(species)
fun Pet.stage() = Stage.of(this)
fun Pet.nat() = Nature.of(nature)
fun Pet.elMap(): Map<String, Double> = els.split(',').filter { ':' in it }.associate { it.substringBefore(':') to (it.substringAfter(':').toDoubleOrNull() ?: 0.0) }
fun elString(m: Map<String, Double>) = m.entries.joinToString(",") { "${it.key}:${((it.value * 10).toLong() / 10.0)}" }
