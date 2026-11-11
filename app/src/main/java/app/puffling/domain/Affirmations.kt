package app.puffling.domain

/** A theme of affirmations; [lines] are pairs of Russian and English. */
data class Theme(val id: String, val ru: String, val en: String, val lines: List<Pair<String, String>>) {
    val name get() = tr(ru, en)
}

data class Aff(val id: String, val text: String, val theme: String, val own: Boolean = false)

object Affirmations {
    private fun t(id: String, ru: String, en: String, vararg l: Pair<String, String>) = Theme(id, ru, en, l.toList())

    val themes = listOf(
        t("calm", "Спокойствие", "Calm",
            "Я дышу медленно, и мне становится спокойнее." to "I breathe slowly, and I grow calmer.",
            "Мне не нужно решать всё прямо сейчас." to "I don't need to solve everything right now.",
            "Это чувство пройдёт, как проходит погода." to "This feeling will pass, the way weather passes.",
            "Я в безопасности в этот момент." to "I am safe in this moment.",
            "Я могу остановиться и сделать вдох." to "I can stop and take a breath.",
            "Тишина внутри меня никуда не делась." to "The quiet inside me is still there.",
            "Я отпускаю то, что не могу изменить." to "I let go of what I can't change.",
            "Мои плечи опускаются, челюсть мягкая." to "My shoulders drop, my jaw is soft.",
            "Я выбираю медленный темп." to "I choose a slow pace.",
            "Вокруг может быть шумно, а во мне тихо." to "It can be loud around me and quiet within.",
            "Мне хватает времени на главное." to "I have enough time for what matters.",
            "Каждый выдох уносит лишнее." to "Every out-breath carries something away.",
            "Я как море в штиль: глубокое и ровное." to "I am like the sea in a calm: deep and still.",
            "Я замечаю тревогу и не верю каждому её слову." to "I notice my worry and don't believe all it says.",
            "Сейчас достаточно просто быть." to "Right now it is enough simply to be."),
        t("self", "Принятие себя", "Self-acceptance",
            "Я {достаточно хорош|достаточно хороша} {такой|такая}, как есть." to "I am good enough as I am.",
            "Я отношусь к себе так же бережно, как к другу." to "I treat myself as kindly as I treat a friend.",
            "Мои ошибки – часть пути, а не приговор." to "My mistakes are part of the way, not a verdict.",
            "Я имею право отдыхать." to "I have the right to rest.",
            "Мне не нужно заслуживать любовь." to "I don't need to earn love.",
            "Я принимаю своё тело и благодарю его." to "I accept my body and thank it.",
            "Я учусь, и это нормально – не уметь сразу." to "I am learning, and it's fine not to know at once.",
            "Мои чувства имеют значение." to "My feelings matter.",
            "Я могу говорить «нет» и оставаться хорошим человеком." to "I can say «no» and still be a good person.",
            "Я прощаю себя за вчерашнее." to "I forgive myself for yesterday.",
            "Я – не мои мысли, я {тот|та}, кто их замечает." to "I am not my thoughts, I am the one who notices them.",
            "Мне не нужно быть идеальным, чтобы быть {ценным|ценной}." to "I don't have to be perfect to be worth something.",
            "Я на своей стороне." to "I am on my own side.",
            "Во мне много света, даже когда его не видно." to "There is a lot of light in me, even when it can't be seen.",
            "Я разрешаю себе быть {разным|разной}." to "I allow myself to be all kinds of things."),
        t("strength", "Сила", "Strength",
            "Я {справлялся|справлялась} раньше и справлюсь снова." to "I have coped before and will cope again.",
            "Я сильнее, чем мне кажется." to "I am stronger than I think.",
            "Трудное делает меня глубже." to "Hard things make me deeper.",
            "Я делаю один шаг, и этого достаточно." to "I take one step, and that is enough.",
            "Я могу бояться и всё равно действовать." to "I can be afraid and act anyway.",
            "У меня есть всё, чтобы пройти этот день." to "I have all I need to get through this day.",
            "Я выдерживаю волну и остаюсь на ногах." to "I ride the wave and stay on my feet.",
            "Мои корни глубоко, ветер меня не вырвет." to "My roots are deep, the wind won't pull me up.",
            "Я доверяю себе." to "I trust myself.",
            "Каждая трудность чему-то учит." to "Every difficulty teaches something.",
            "Я выбираю смелость в малом." to "I choose courage in small things.",
            "Мне по силам просить о помощи." to "I'm strong enough to ask for help.",
            "Я иду своим шагом." to "I walk at my own pace.",
            "Я не сдаюсь, я отдыхаю и иду дальше." to "I don't give up, I rest and go on.",
            "Внутри меня есть тихая опора." to "There is a quiet support inside me."),
        t("thanks", "Благодарность", "Gratitude",
            "Я {благодарен|благодарна} за этот вдох." to "I am grateful for this breath.",
            "Вокруг меня больше хорошего, чем я замечаю." to "There is more good around me than I notice.",
            "Я замечаю маленькие радости." to "I notice small joys.",
            "Спасибо моему телу за то, что оно несёт меня." to "Thank you to my body for carrying me.",
            "Я {благодарен|благодарна} людям, которые рядом." to "I am grateful for the people near me.",
            "Сегодняшний день уже что-то мне подарил." to "Today has already given me something.",
            "У меня есть дом, тепло и чашка чая." to "I have a home, warmth and a cup of tea.",
            "Я ценю то, что у меня есть, прямо сейчас." to "I value what I have, right now.",
            "Даже в трудный день найдётся за что сказать спасибо." to "Even on a hard day there is something to say thank you for.",
            "Я благодарю себя за то, что стараюсь." to "I thank myself for trying.",
            "Свет из окна, голос друга, вкусная еда – всё это подарки." to "Light at the window, a friend's voice, good food – all gifts.",
            "Благодарность делает меня богаче." to "Gratitude makes me richer.",
            "Я замечаю доброту других." to "I notice the kindness of others.",
            "Мне повезло проснуться сегодня." to "I'm lucky to have woken up today.",
            "Я {благодарен|благодарна} за уроки, даже трудные." to "I am grateful for lessons, even the hard ones."),
        t("sleep", "Сон", "Sleep",
            "День закончился, и я могу его отпустить." to "The day is over, and I can let it go.",
            "Мне не нужно больше ничего делать сегодня." to "I don't need to do anything more today.",
            "Моё тело тяжелеет и расслабляется." to "My body grows heavy and relaxed.",
            "Мысли подождут до утра." to "My thoughts can wait till morning.",
            "Я заслуживаю глубокого отдыха." to "I deserve deep rest.",
            "С каждым выдохом я погружаюсь глубже." to "With every out-breath I sink deeper.",
            "Я в тепле и в безопасности." to "I am warm and safe.",
            "Завтра будет новым." to "Tomorrow will be new.",
            "Я {сделал|сделала} сегодня достаточно." to "I did enough today.",
            "Ночь бережёт меня." to "The night keeps me safe.",
            "Мне легко засыпать." to "Falling asleep is easy for me.",
            "Я отпускаю этот день с благодарностью." to "I let this day go with thanks.",
            "Темнота мягкая и спокойная." to "The dark is soft and calm.",
            "Моё дыхание медленное, как волны." to "My breath is slow, like waves.",
            "Сон восстанавливает меня." to "Sleep restores me."),
        t("morning", "Утро", "Morning",
            "Сегодня я начинаю с чистого листа." to "Today I start with a clean page.",
            "У меня есть силы на этот день." to "I have strength for this day.",
            "Я выбираю, каким будет моё утро." to "I choose what my morning will be like.",
            "Я встречаю день с любопытством." to "I meet the day with curiosity.",
            "Сегодня найдётся повод улыбнуться." to "Today there will be a reason to smile.",
            "Я делаю главное первым." to "I do what matters first.",
            "Мне не нужно спешить, чтобы успеть." to "I don't need to hurry to get things done.",
            "Я замечу сегодня что-то красивое." to "I'll notice something beautiful today.",
            "Я буду {добр|добра} к себе и к другим." to "I'll be kind to myself and to others.",
            "Каждое утро – новая попытка." to "Every morning is a new try.",
            "Я просыпаюсь, и это уже подарок." to "I wake up, and that's already a gift.",
            "Сегодня я сделаю один маленький шаг к мечте." to "Today I'll take one small step towards a dream.",
            "Я выбираю спокойный темп." to "I choose a calm pace.",
            "Я {открыт|открыта} хорошему." to "I am open to good things.",
            "Этот день мой." to "This day is mine."),
        t("love", "Близкие", "People",
            "Я умею любить и принимать любовь." to "I can love and accept love.",
            "Мне есть кому позвонить." to "There is someone I can call.",
            "Я слушаю, чтобы понять, а не чтобы ответить." to "I listen to understand, not to answer.",
            "Я отпускаю старые обиды." to "I let old hurts go.",
            "Мои границы помогают моим отношениям." to "My boundaries help my relationships.",
            "Я {окружён|окружена} людьми, которым не всё равно." to "I am surrounded by people who care.",
            "Я дарю тепло, и оно возвращается." to "I give warmth, and it comes back.",
            "Я могу быть рядом, даже когда молчу." to "I can be there even when I'm silent.",
            "Я прощаю, чтобы быть {свободным|свободной}." to "I forgive so that I can be free.",
            "Мне не нужно нравиться всем." to "I don't need everyone to like me.",
            "Я ценю людей такими, какие они есть." to "I value people as they are.",
            "В моей жизни есть место новым друзьям." to "There is room in my life for new friends.",
            "Я говорю о своих чувствах честно и мягко." to "I speak about my feelings honestly and gently.",
            "Я {достоин|достойна} хороших отношений." to "I deserve good relationships.",
            "Любовь начинается с меня." to "Love starts with me."),
        t("focus", "Ясность", "Clarity",
            "Я делаю одно дело за раз." to "I do one thing at a time.",
            "Моё внимание здесь, в этой минуте." to "My attention is here, in this minute.",
            "Я замечаю, когда ум уходит, и возвращаю его." to "I notice when my mind drifts and bring it back.",
            "Мне ясно, что для меня важно." to "I am clear about what matters to me.",
            "Я отпускаю лишнее и оставляю главное." to "I let go of the extra and keep the essential.",
            "Мой ум как чистая вода." to "My mind is like clear water.",
            "Я принимаю решения спокойно." to "I make decisions calmly.",
            "Я могу не отвечать сразу." to "I don't have to answer straight away.",
            "Мне достаточно знать следующий шаг." to "It's enough to know the next step.",
            "Я выбираю, на что тратить внимание." to "I choose what to spend my attention on.",
            "Тишина помогает мне думать." to "Quiet helps me think.",
            "Я замечаю, а не оцениваю." to "I notice rather than judge.",
            "Моё дыхание – мой якорь." to "My breath is my anchor.",
            "Я здесь и сейчас." to "I am here and now.",
            "Я делаю медленно, и поэтому хорошо." to "I do it slowly, and so I do it well."),
    )
    val byId = themes.associateBy { it.id }

    fun builtin(): List<Aff> = themes.flatMap { th -> th.lines.mapIndexed { i, l -> Aff("${th.id}:$i", tr(l.first, l.second), th.id) } }
    fun of(id: String, own: List<app.puffling.data.OwnAff>): Aff? =
        if (id.startsWith("u")) own.firstOrNull { "u${it.id}" == id }?.let { Aff(id, it.text, "own", true) }
        else { val th = byId[id.substringBefore(':')]; val l = th?.lines?.getOrNull(id.substringAfter(':').toIntOrNull() ?: -1); if (l == null) null else Aff(id, tr(l.first, l.second), th.id) }

    /**
     * The cards for an affirmation session: a [deck] theme, «own», «fav» or «mix» (the day's mix:
     * one each from three themes, plus a favourite or own one). Stable for the day.
     */
    fun deal(deck: String, own: List<app.puffling.data.OwnAff>, favs: Set<String>, day: Long, n: Int = 5): List<Aff> {
        val ownA = own.map { Aff("u${it.id}", it.text, "own", true) }
        val all = builtin() + ownA
        val r = Rand(hash("aff$day$deck"))
        fun pick(l: List<Aff>, k: Int) = l.sortedBy { hash(it.id + day) }.take(k)
        return when (deck) {
            "own" -> pick(ownA, n)
            "fav" -> pick(all.filter { it.id in favs }, n)
            "mix", "" -> { val ths = themes.sortedBy { hash(it.id + day) }.take(3)
                val l = ths.map { th -> val i = r.int(th.lines.size); Aff("${th.id}:$i", tr(th.lines[i].first, th.lines[i].second), th.id) }
                val extra = pick(all.filter { it.id in favs } + ownA, 2)
                (l + extra).distinctBy { it.id }.take(n) }
            else -> pick(all.filter { it.theme == deck }, n)
        }
    }

    /** The affirmation of the day for the home screen. */
    fun ofDay(day: Long, own: List<app.puffling.data.OwnAff>, favs: Set<String>): Aff {
        val all = builtin() + own.map { Aff("u${it.id}", it.text, "own", true) }
        val pool = all.filter { it.id in favs || it.own }.takeIf { it.size >= 3 && day % 2 == 0L } ?: all
        return pool[Math.floorMod(hash("day$day"), pool.size)]
    }

    /** How alike what was said is to the phrase: the share of the phrase's words heard, 0..1. */
    fun heard(phrase: String, said: String): Float {
        fun words(s: String) = s.lowercase().replace('ё', 'е').split(Regex("[^\\p{L}\\p{N}]+")).filter { it.length > 1 }
        val want = words(phrase); if (want.isEmpty()) return 1f
        val got = words(said).toSet()
        return want.count { w -> got.any { g -> g == w || (w.length > 4 && g.length > 4 && g.take(w.length - 2) == w.take(w.length - 2)) } }.toFloat() / want.size
    }
}
