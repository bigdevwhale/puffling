package app.puffling.domain

import app.puffling.data.OwnPractice

/** The six families of practice. Pets like some of them; a grown pet takes its build from the one it was raised on. */
enum class Element(val id: String, val ru: String, val en: String, val color: Long) {
    AIR("air", "Воздух", "Air", 0xFF5FA8C9),        // breathing patterns
    DEEP("deep", "Глубина", "Deep", 0xFF2F6E9E),     // Wim Hof, holds, energising breath
    STILL("still", "Тишина", "Stillness", 0xFF8C7BB8), // sitting meditation
    EARTH("earth", "Земля", "Earth", 0xFF6E9A5B),    // the body: scan, walking, grounding, tea
    HEART("heart", "Сердце", "Heart", 0xFFD9707A),   // gratitude, kindness, affirmations
    LIGHT("light", "Свет", "Light", 0xFFE0A63A);     // attention: candle, noting, the sky
    val label get() = tr(ru, en)
    companion object { fun of(id: String) = entries.firstOrNull { it.id == id } ?: AIR }
}

/** How a practice is played. */
enum class Kind(val code: String, val ru: String, val en: String) {
    BREATH("b", "Дыхание по счёту", "Paced breathing"),
    WIMHOF("w", "Вдохи и задержка", "Breaths and a hold"),
    SIT("s", "Тихое сидение", "Silent sitting"),
    GUIDED("g", "По шагам", "Step by step"),
    AFFIRM("a", "Аффирмации", "Affirmations");
    val label get() = tr(ru, en)
    companion object { fun of(c: String) = entries.firstOrNull { it.code == c } ?: BREATH }
}

/** One part of a breath: in, hold full, out, hold empty. [top] is a second, short sip of air on top of the first. */
enum class Ph(val code: Char, val ru: String, val en: String) {
    IN('i', "Вдох", "In"), TOP('t', "Ещё вдох", "Top up"), HOLD('h', "Задержка", "Hold"), OUT('o', "Выдох", "Out"), EMPTY('e', "Пауза", "Pause");
    val label get() = tr(ru, en)
    companion object { fun of(c: Char) = entries.firstOrNull { it.code == c } }
}

data class Phase(val ph: Ph, val secs: Double, val noteRu: String = "", val noteEn: String = "") {
    val note get() = tr(noteRu, noteEn)
}

/** A step of a guided practice: what to do and how long. [ask] steps let the user write a line (gratitude). */
data class Step(val ru: String, val en: String, val secs: Int, val ask: Boolean = false) {
    val text get() = tr(ru, en)
}

data class Practice(
    val id: String, val ru: String, val en: String, val kind: Kind, val el: Element,
    val aboutRu: String, val aboutEn: String,
    val minutes: Int = 5,                    // the usual length (breathing and sitting can be changed)
    val pattern: List<Phase> = emptyList(),  // BREATH: one cycle
    val ramp: Double = 0.0,                  // BREATH: seconds added to the out-breath each minute, up to double
    val steps: List<Step> = emptyList(),     // GUIDED
    val rounds: Int = 3, val breaths: Int = 30, // WIMHOF
    val bell: Int = 0,                       // SIT: a soft bell every this many minutes, 0 none
    val deck: String = "",                   // AFFIRM: theme id or «own»/«fav»
    val sleep: Boolean = false,              // good before sleep
    val level: String = "",                 // WIMHOF: a level id, «» = chosen on the start page; own presets carry [hold]
    val hold: Int = 60,                      // WIMHOF own preset: the first round's hold target
    val own: Boolean = false,
) {
    val name get() = tr(ru, en)
    val about get() = tr(aboutRu, aboutEn)
    val cycle get() = pattern.sumOf { it.secs }
    /** Breaths a minute, for breathing patterns. */
    val bpm get() = if (cycle > 0) 60 / cycle else 0.0
    val slow get() = kind == Kind.BREATH && bpm in 0.1..6.5
    val longExhale get() = kind == Kind.BREATH && (pattern.filter { it.ph == Ph.OUT }.sumOf { it.secs }) >= 1.6 * pattern.filter { it.ph == Ph.IN || it.ph == Ph.TOP }.sumOf { it.secs }
    val box get() = kind == Kind.BREATH && pattern.size == 4 && pattern.map { it.secs }.distinct().size == 1
    val guidedSecs get() = steps.sumOf { it.secs }
}

object Practices {
    private fun pat(s: String): List<Phase> = s.split(',').mapNotNull { t -> val p = Ph.of(t.trim().firstOrNull() ?: ' ') ?: return@mapNotNull null
        Phase(p, t.trim().drop(1).replace(',', '.').toDoubleOrNull() ?: 4.0) }
    fun code(l: List<Phase>) = l.joinToString(",") { "${it.ph.code}${fmtSecs(it.secs)}" }
    private fun fmtSecs(v: Double) = if (v % 1.0 == 0.0) v.toInt().toString() else v.toString()
    fun parse(s: String) = pat(s)

    val builtin: List<Practice> = listOf(
        // ---------- air ----------
        Practice("box", "Квадратное дыхание", "Box breathing", Kind.BREATH, Element.AIR,
            "Четыре равные стороны: вдох, задержка, выдох, задержка. Выравнивает и собирает – им пользуются пилоты и спасатели перед трудной минутой.",
            "Four equal sides: in, hold, out, hold. It steadies and gathers you – pilots and rescuers use it before a hard minute.",
            5, pat("i4,h4,o4,e4")),
        Practice("coherent", "Ровное дыхание", "Coherent breathing", Kind.BREATH, Element.AIR,
            "Пять с половиной секунд на вдох и столько же на выдох, без пауз. Около шести вдохов в минуту – ритм, в котором сердце и дыхание идут в ногу.",
            "Five and a half seconds in and the same out, no pauses. About six breaths a minute – the pace at which heart and breath fall into step.",
            10, pat("i5.5,o5.5")),
        Practice("478", "Дыхание 4-7-8", "4-7-8 breathing", Kind.BREATH, Element.AIR,
            "Вдох носом на четыре, задержка на семь, медленный выдох ртом на восемь. Хорошо перед сном: длинный выдох успокаивает.",
            "In through the nose for four, hold for seven, out slowly through the mouth for eight. Good before sleep: a long out-breath calms.",
            4, pat("i4,h7,o8"), sleep = true),
        Practice("sigh", "Вздох облегчения", "Physiological sigh", Kind.BREATH, Element.AIR,
            "Двойной вдох носом – полный и ещё глоток сверху, – потом долгий выдох ртом. Самый быстрый способ сбросить напряжение: хватает пары минут.",
            "A double breath in through the nose – a full one and a small sip on top – then a long sigh out through the mouth. The fastest way to let tension go: a couple of minutes is enough.",
            2, pat("i2.5,t1,o6")),
        Practice("exhale", "Длинный выдох", "Long exhale", Kind.BREATH, Element.AIR,
            "Выдох вдвое длиннее вдоха, и он понемногу растёт. Тело понимает это как «опасности нет».",
            "The out-breath twice as long as the in-breath, growing a little longer. The body reads it as «no danger here».",
            5, pat("i4,o8"), ramp = .5, sleep = true),
        Practice("triangle", "Треугольник", "Triangle breathing", Kind.BREATH, Element.AIR,
            "Вдох, задержка, выдох – по четыре. Проще квадрата, хорошо для начала.",
            "In, hold, out – four each. Simpler than the box, a good start.",
            3, pat("i4,h4,o4")),
        Practice("nadi", "Попеременное дыхание", "Alternate nostril breathing", Kind.BREATH, Element.AIR,
            "Нади шодхана: правым большим пальцем закрывайте правую ноздрю, безымянным – левую. Вдох одной, выдох другой, и так по кругу.",
            "Nadi shodhana: close the right nostril with your right thumb, the left one with your ring finger. In through one side, out through the other, round and round.",
            5, listOf(Phase(Ph.IN, 4.0, "левой ноздрёй", "through the left"), Phase(Ph.HOLD, 2.0), Phase(Ph.OUT, 6.0, "правой", "through the right"),
                Phase(Ph.IN, 4.0, "правой", "through the right"), Phase(Ph.HOLD, 2.0), Phase(Ph.OUT, 6.0, "левой", "through the left"))),
        Practice("humming", "Жужжащий выдох", "Humming breath", Kind.BREATH, Element.AIR,
            "Бхрамари: вдох носом, а выдох – с тихим «ммм», как шмель. Вибрация успокаивает голову изнутри.",
            "Bhramari: breathe in through the nose, and out with a soft «mmm», like a bumblebee. The hum calms the head from within.",
            3, listOf(Phase(Ph.IN, 4.0), Phase(Ph.OUT, 7.0, "ммм…", "mmm…"))),
        // ---------- deep ----------
        Practice("wimhof", "Метод Вима Хофа", "Wim Hof breathing", Kind.WIMHOF, Element.DEEP,
            "Тридцать глубоких вдохов волной, потом выдох и задержка сколько комфортно, потом вдох и пятнадцать секунд под водой. Три раунда. Только сидя или лёжа – никогда в воде и за рулём.",
            "Thirty deep breaths like waves, then let go and hold as long as is comfortable, then breathe in and hold fifteen seconds. Three rounds. Only sitting or lying down – never in water or at the wheel.",
            12, rounds = 3, breaths = 30),
        Practice("wimhof_soft", "Вим Хоф, мягко", "Wim Hof, gently", Kind.WIMHOF, Element.DEEP,
            "Два раунда по двадцать вдохов – чтобы привыкнуть. Та же осторожность: сидя или лёжа.",
            "Two rounds of twenty breaths – to get used to it. The same care: sitting or lying down.",
            6, rounds = 2, breaths = 20, level = "cus", hold = 30),
        Practice("bellows", "Бодрое дыхание", "Bellows breath", Kind.BREATH, Element.DEEP,
            "Быстрые ровные вдохи и выдохи носом, по секунде, а потом спокойная пауза. Вместо кофе. Если закружится голова – дышите обычно.",
            "Quick even breaths in and out through the nose, a second each, then a calm pause. Instead of coffee. If you feel dizzy, breathe normally.",
            3, pat("i1,o1,i1,o1,i1,o1,i1,o1,i1,o1,i3,h5,o5")),
        // ---------- stillness ----------
        Practice("sit", "Тихое сидение", "Silent sitting", Kind.SIT, Element.STILL,
            "Просто сидеть и дышать. Колокольчик в начале и в конце, а если хотите – и посередине. Когда мысль уведёт, мягко возвращайтесь к дыханию.",
            "Just sit and breathe. A bell at the start and the end, and in between if you like. When a thought carries you off, gently come back to the breath.",
            10, bell = 0),
        Practice("count", "Счёт дыханий", "Counting breaths", Kind.SIT, Element.STILL,
            "Считайте выдохи от одного до десяти и снова с начала. Сбились – ничего страшного, начните с единицы. Колокольчик каждые пять минут.",
            "Count the out-breaths from one to ten and start over. Lost count – no matter, start again from one. A bell every five minutes.",
            10, bell = 5),
        Practice("sky", "Ум как небо", "Mind like the sky", Kind.GUIDED, Element.STILL,
            "Мысли – облака, а вы – небо, в котором они плывут.",
            "Thoughts are clouds, and you are the sky they float across.",
            7, steps = listOf(
                Step("Сядьте удобно. Пусть дыхание идёт само.", "Sit comfortably. Let the breath come by itself.", 50),
                Step("Представьте над собой огромное ясное небо.", "Imagine a huge clear sky above you.", 60),
                Step("Когда появляется мысль, увидьте её облаком. Оно приплывает…", "When a thought comes, see it as a cloud. It drifts in…", 70),
                Step("…и уплывает. Не толкайте его и не держите.", "…and drifts away. Don't push it, don't hold it.", 70),
                Step("Облака бывают тёмными и светлыми. Небу всё равно – оно остаётся небом.", "Clouds can be dark or light. The sky doesn't mind – it stays the sky.", 80),
                Step("Побудьте этим небом. Просторным и тихим.", "Be that sky for a while. Wide and quiet.", 90),
                Step("Сделайте глубокий вдох и медленно откройте глаза.", "Take a deep breath and slowly open your eyes.", 20))),
        // ---------- earth ----------
        Practice("scan", "Сканирование тела", "Body scan", Kind.GUIDED, Element.EARTH,
            "Внимание медленно проходит по телу от макушки до пальцев ног и замечает, что там.",
            "Attention travels slowly through the body from the crown to the toes and notices what is there.",
            10, steps = listOf(
                Step("Лягте или сядьте удобно. Закройте глаза, сделайте три медленных выдоха.", "Lie down or sit comfortably. Close your eyes and let out three slow breaths.", 40),
                Step("Макушка и лоб. Есть ли там напряжение? Просто заметьте.", "The crown and the forehead. Is there any tension? Just notice it.", 45),
                Step("Глаза, щёки, челюсть. Пусть челюсть чуть разожмётся.", "Eyes, cheeks, jaw. Let the jaw loosen a little.", 45),
                Step("Шея и плечи. На выдохе плечи опускаются.", "Neck and shoulders. On the out-breath the shoulders drop.", 50),
                Step("Руки до кончиков пальцев. Тепло, тяжесть, покалывание – что угодно.", "Arms down to the fingertips. Warmth, heaviness, tingling – anything.", 50),
                Step("Грудь. Как она поднимается и опускается с дыханием.", "The chest. How it rises and falls with the breath.", 50),
                Step("Живот. Мягкий, тёплый, дышит.", "The belly. Soft, warm, breathing.", 50),
                Step("Спина и поясница. Где тело касается опоры.", "The back and lower back. Where the body meets the support.", 50),
                Step("Бёдра, колени, голени.", "Hips, knees, shins.", 50),
                Step("Стопы и пальцы ног. Самое далёкое место от головы.", "Feet and toes. The furthest place from the head.", 45),
                Step("Теперь всё тело целиком, от макушки до пяток. Оно дышит.", "Now the whole body, crown to heels. It is breathing.", 60),
                Step("Пошевелите пальцами, потянитесь и откройте глаза.", "Wiggle your fingers, stretch and open your eyes.", 20))),
        Practice("walk", "Медленная ходьба", "Walking meditation", Kind.GUIDED, Element.EARTH,
            "Несколько шагов туда и обратно – дома или во дворе. Каждый шаг целиком.",
            "A few steps there and back – at home or outside. Every step in full.",
            8, steps = listOf(
                Step("Встаньте. Почувствуйте, как стопы давят на пол.", "Stand up. Feel your feet pressing into the floor.", 40),
                Step("Начните идти очень медленно. Отрыв пятки, перенос, касание.", "Start walking very slowly. Heel lifts, foot moves, foot lands.", 90),
                Step("Про себя: «поднимаю… несу… ставлю».", "Silently: «lifting… moving… placing».", 90),
                Step("Дойдя до конца, остановитесь, развернитесь, постойте.", "At the end, stop, turn around, stand for a moment.", 60),
                Step("Добавьте дыхание: вдох – шаг, выдох – шаг.", "Add the breath: in – a step, out – a step.", 90),
                Step("Мысль увела – остановитесь и снова почувствуйте стопы.", "A thought carried you off – stop and feel your feet again.", 60),
                Step("Последние шаги. Остановитесь и постойте с закрытыми глазами.", "The last steps. Stop and stand with your eyes closed.", 40))),
        Practice("ground", "Заземление 5-4-3-2-1", "Grounding 5-4-3-2-1", Kind.GUIDED, Element.EARTH,
            "Пять чувств возвращают из мыслей сюда, в комнату. Помогает, когда накрывает тревога.",
            "Five senses bring you back from your thoughts to the room. Helps when anxiety rolls in.",
            4, steps = listOf(
                Step("Сделайте медленный вдох и длинный выдох.", "Take a slow breath in and a long breath out.", 15),
                Step("Найдите глазами пять вещей. Назовите их про себя.", "Find five things with your eyes. Name them silently.", 45),
                Step("Четыре вещи, которых можно коснуться. Коснитесь.", "Four things you can touch. Touch them.", 40),
                Step("Три звука. Даже самые тихие.", "Three sounds. Even the faintest.", 40),
                Step("Два запаха. Если не слышно – вспомните любимые.", "Two smells. If there are none, remember two you love.", 35),
                Step("Один вкус во рту.", "One taste in your mouth.", 25),
                Step("Вы здесь. Ещё один медленный выдох.", "You are here. One more slow breath out.", 15))),
        Practice("tea", "Чашка чая", "A cup of tea", Kind.GUIDED, Element.EARTH,
            "Церемония на одного: заварите чай или налейте воды и выпейте её целиком, ни на что не отвлекаясь.",
            "A ceremony for one: make some tea or pour some water and drink it with your whole attention.",
            6, steps = listOf(
                Step("Возьмите чашку в ладони. Какая она – тёплая, тяжёлая?", "Hold the cup in your hands. How does it feel – warm, heavy?", 40),
                Step("Посмотрите на цвет и на пар.", "Look at the colour and the steam.", 35),
                Step("Вдохните запах. Не спешите.", "Breathe in the smell. No hurry.", 35),
                Step("Первый глоток. Подержите во рту, заметьте вкус.", "The first sip. Hold it in your mouth, notice the taste.", 45),
                Step("Почувствуйте, как тепло идёт вниз.", "Feel the warmth going down.", 40),
                Step("Пейте дальше, глоток за глотком, ничего не делая между ними.", "Keep drinking, sip by sip, doing nothing in between.", 120),
                Step("Последний глоток. Поставьте чашку и посидите минуту.", "The last sip. Put the cup down and sit for a minute.", 45))),
        // ---------- heart ----------
        Practice("gratitude", "Три хорошие вещи", "Three good things", Kind.GUIDED, Element.HEART,
            "Вспомнить три хороших момента дня, даже совсем маленьких, и побыть с каждым. Можно записать – они останутся в летописи.",
            "Recall three good moments of the day, even tiny ones, and stay with each. You can write them down – they stay in your journal.",
            5, steps = listOf(
                Step("Сделайте пару медленных вдохов. Оглянитесь на день.", "Take a couple of slow breaths. Look back over the day.", 25),
                Step("Первая хорошая вещь. Что это было?", "The first good thing. What was it?", 60, ask = true),
                Step("Вторая. Может, совсем мелочь – тёплый свет, вкусный хлеб.", "The second. Maybe a tiny thing – warm light, good bread.", 60, ask = true),
                Step("Третья. Кто или что сделал её возможной?", "The third. Who or what made it possible?", 60, ask = true),
                Step("Побудьте с этим теплом ещё немного.", "Stay with this warmth a little longer.", 40))),
        Practice("metta", "Доброе пожелание", "Loving-kindness", Kind.GUIDED, Element.HEART,
            "Метта: тёплые слова себе, близкому, незнакомцу и всем на свете.",
            "Metta: warm words to yourself, to someone dear, to a stranger and to everyone.",
            8, steps = listOf(
                Step("Положите руку на сердце. Почувствуйте её тепло.", "Put a hand on your heart. Feel its warmth.", 40),
                Step("Себе: «Пусть мне будет спокойно. Пусть я буду {здоров|здорова}. Пусть мне будет легко».", "To yourself: «May I be calm. May I be well. May I be at ease».", 80),
                Step("Вспомните близкого человека. «Пусть тебе будет спокойно…»", "Think of someone dear. «May you be calm…»", 80),
                Step("Кого-то знакомого лишь в лицо – соседа, продавца. Ему то же самое.", "Someone you only know by sight – a neighbour, a cashier. The same for them.", 70),
                Step("Того, с кем сейчас трудно. Насколько получается.", "Someone you find hard right now. As far as you can.", 70),
                Step("Всем живым: «Пусть всем будет спокойно».", "All living beings: «May all be calm».", 60),
                Step("Вернитесь к себе. Как сейчас в груди?", "Come back to yourself. How does your chest feel now?", 30))),
        Practice("affirm", "Аффирмации дня", "Today's affirmations", Kind.AFFIRM, Element.HEART,
            "Несколько добрых фраз вслух. Сказанная фраза уходит в море в бутылке – и однажды её прибьёт обратно.",
            "A few kind phrases out loud. Each one you say goes out to sea in a bottle – and one day it washes back.",
            3, deck = "mix"),
        // ---------- light ----------
        Practice("candle", "Пламя свечи", "Candle gazing", Kind.GUIDED, Element.LIGHT,
            "Тратака: смотреть на огонёк, потом закрыть глаза и смотреть на его след. Учит удерживать внимание.",
            "Trataka: look at a flame, then close your eyes and watch its afterglow. Trains steady attention.",
            5, steps = listOf(
                Step("Зажгите свечу на расстоянии вытянутой руки, на уровне глаз. Или смотрите на огонёк на экране.", "Light a candle at arm's length, at eye level. Or watch the flame on the screen.", 30),
                Step("Смотрите на пламя, стараясь не моргать. Дышите спокойно.", "Gaze at the flame, trying not to blink. Breathe calmly.", 60),
                Step("Закройте глаза. Смотрите на светлый след внутри.", "Close your eyes. Watch the bright trace inside.", 45),
                Step("Откройте глаза и снова смотрите на огонь.", "Open your eyes and look at the flame again.", 60),
                Step("Закройте глаза. След тает – проводите его.", "Close your eyes. The trace fades – see it off.", 45),
                Step("Посидите в темноте за закрытыми веками.", "Sit in the dark behind your closed eyelids.", 40))),
        Practice("noting", "Отмечание", "Noting", Kind.GUIDED, Element.LIGHT,
            "Сидите и мягко отмечайте, куда ушло внимание: «думаю», «слышу», «чувствую». Внизу есть кнопки – нажимайте, если хочется считать.",
            "Sit and gently note where your attention went: «thinking», «hearing», «feeling». There are buttons below – tap them if you like counting.",
            8, steps = listOf(
                Step("Сядьте ровно, дыхание – якорь.", "Sit upright, the breath is your anchor.", 40),
                Step("Внимание ушло? Отметьте куда одним словом и вернитесь к дыханию.", "Attention wandered? Note where in one word and come back to the breath.", 150),
                Step("Отмечайте без оценки. «Думаю» – не ошибка, а просто погода.", "Note without judging. «Thinking» is not a mistake, just weather.", 150),
                Step("Замечайте, как быстро вы замечаете.", "Notice how quickly you notice.", 100),
                Step("Отпустите отметки и просто посидите.", "Let the notes go and simply sit.", 40))),
        Practice("sound", "Слушать звуки", "Listening", Kind.GUIDED, Element.LIGHT,
            "Звуки вокруг как музыка: близкие, далёкие, тишина между ними.",
            "The sounds around you as music: near, far, and the silence between them.",
            5, steps = listOf(
                Step("Закройте глаза. Пусть уши откроются.", "Close your eyes. Let your ears open.", 30),
                Step("Самые близкие звуки: дыхание, одежда, комната.", "The nearest sounds: your breath, clothes, the room.", 60),
                Step("Звуки дальше: за дверью, за окном.", "Sounds further off: behind the door, outside the window.", 60),
                Step("Самый далёкий звук, который удаётся услышать.", "The furthest sound you can hear.", 60),
                Step("Тишина между звуками. Она тоже звучит.", "The silence between sounds. It sounds too.", 60),
                Step("Откройте глаза.", "Open your eyes.", 10))),
    )

    /** The storm button: sighs, the five senses, a question. */
    val SOS = Practice("sos", "Штормит", "Rough sea", Kind.GUIDED, Element.EARTH,
        "Когда накрывает: три вздоха облегчения, потом пять чувств. Две-три минуты.",
        "When it rolls over you: three sighs of relief, then the five senses. Two or three minutes.",
        3, steps = listOf(
            Step("Вы здесь, и это пройдёт. Двойной вдох носом…", "You're here, and this will pass. A double breath in through the nose…", 4),
            Step("…и долгий-долгий выдох ртом.", "…and a long, long breath out through the mouth.", 7),
            Step("Ещё раз: вдох, ещё глоток сверху…", "Again: in, and a small sip on top…", 4),
            Step("…и выдох, как будто сдуваете парус.", "…and out, as if letting the air out of a sail.", 7),
            Step("И третий: вдох-вдох…", "And a third: in-in…", 4),
            Step("…выдох. Плечи опускаются.", "…out. Your shoulders drop.", 8),
            Step("Найдите глазами пять вещей вокруг. Назовите их.", "Find five things around you. Name them.", 30),
            Step("Коснитесь четырёх вещей. Какие они на ощупь?", "Touch four things. How do they feel?", 25),
            Step("Три звука. Два запаха. Один вкус.", "Three sounds. Two smells. One taste.", 30),
            Step("Стопы на полу. Вы здесь. Как сейчас?", "Feet on the floor. You're here. How is it now?", 15)))

    /** The bell's three breaths. */
    val BELL = Practice("bell", "Колокольчик", "The bell", Kind.BREATH, Element.LIGHT,
        "Остановиться и сделать три вдоха.", "Stop and take three breaths.", 1, pat("i4,o6"))

    /** The breath-hold test, kept apart from practices. */
    val BOLT = Practice("bolt", "Тест задержки", "Breath-hold test", Kind.SIT, Element.DEEP,
        "", "", 1)

    @Volatile var own: List<Practice> = emptyList()
    val byId get() = (builtin + SOS + BELL + BOLT).associateBy { it.id } + own.associateBy { it.id }
    fun of(id: String): Practice = byId[id] ?: builtin[0]
    fun find(id: String): Practice? = byId[id]
    val all get() = builtin + own
}

/** A user's practice as the player sees it. Id «u<id>». */
fun OwnPractice.toPractice(): Practice {
    val k = Kind.of(kind)
    return Practice("u$id", name, name, k, Element.of(el), about, about, minutes.coerceAtLeast(1),
        pattern = if (k == Kind.BREATH) Practices.parse(pattern).ifEmpty { Practices.parse("i4,o6") } else emptyList(),
        ramp = ramp,
        steps = if (k == Kind.GUIDED) steps.lines().mapNotNull { l -> val t = l.trim(); if (t.isEmpty()) null
            else { val secs = t.substringBefore('|').trim().toIntOrNull(); if (secs != null && '|' in t) Step(t.substringAfter('|').trim(), t.substringAfter('|').trim(), secs.coerceIn(5, 3600))
                else Step(t, t, 60) } } else emptyList(),
        rounds = rounds.coerceIn(1, 6), breaths = breaths.coerceIn(10, 60), bell = bell, deck = deck, own = true, level = if (k == Kind.WIMHOF) "cus" else "", hold = hold.coerceIn(15, 300))
}
