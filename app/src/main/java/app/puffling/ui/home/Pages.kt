package app.puffling.ui.home

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.puffling.container
import app.puffling.currentLanguage
import app.puffling.data.OutsideSource
import app.puffling.data.OwnAff
import app.puffling.data.Settings
import app.puffling.data.Weather
import app.puffling.domain.Affirmations
import app.puffling.domain.Courses
import app.puffling.domain.Deep
import app.puffling.domain.Form
import app.puffling.domain.Lang
import app.puffling.domain.Pets
import app.puffling.domain.Shop
import app.puffling.domain.Stage
import app.puffling.domain.clock
import app.puffling.domain.count
import app.puffling.domain.dayOf
import app.puffling.domain.fmt
import app.puffling.domain.fmtDay
import app.puffling.domain.nat
import app.puffling.domain.sp
import app.puffling.domain.today
import app.puffling.domain.tr
import app.puffling.domain.weekStart
import app.puffling.setLanguage
import app.puffling.ui.Data
import app.puffling.ui.Nav
import app.puffling.ui.Screen
import app.puffling.ui.art.Creature
import app.puffling.ui.art.EggView
import app.puffling.ui.art.Picture
import app.puffling.ui.art.Pose
import app.puffling.ui.art.Sky
import app.puffling.ui.art.TeaHouse
import app.puffling.ui.components.Badge
import app.puffling.ui.components.Btn
import app.puffling.ui.components.Chip
import app.puffling.ui.components.Empty
import app.puffling.ui.components.Field
import app.puffling.ui.components.Ghost
import app.puffling.ui.components.Hint
import app.puffling.ui.components.Icon
import app.puffling.ui.components.Icons
import app.puffling.ui.components.Page
import app.puffling.ui.components.Popup
import app.puffling.ui.components.Section
import app.puffling.ui.components.Segments
import app.puffling.ui.components.card
import app.puffling.ui.share.CardKind
import app.puffling.ui.share.ShareReq
import app.puffling.ui.theme.C
import app.puffling.ui.theme.T
import app.puffling.work.Bells
import app.puffling.work.Synth

/** News to show once: an egg found, a hatching, a new stage, a finished course. */
@Composable
fun Inbox(d: Data, nav: Nav) {
    val c = LocalContext.current.container
    val msg = d.s.inbox.firstOrNull() ?: return
    val parts = msg.split(':')
    fun close() = c.write { settings.update { s -> s.copy(inbox = s.inbox.drop(1)) } }
    when (parts[0]) {
        "egg" -> { val sp = Pets.of(parts[1])
            Popup(::close) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { EggView(sp.id, 150.dp, .8f) }
                Text(tr("Найдено яйцо!", "An egg!"), style = T.title, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                Text(tr("Его прибило к берегу у чайной. ", "It washed up below the tea house. ") + sp.find, style = T.body, modifier = Modifier.fillMaxWidth().padding(top = 6.dp), textAlign = TextAlign.Center)
                Text(tr("Возьмите его в команду – и оно вылупится от ваших практик.", "Put it on the team and your practice will hatch it."), style = T.small, modifier = Modifier.fillMaxWidth().padding(top = 8.dp), textAlign = TextAlign.Center)
                Spacer(Modifier.height(16.dp)); Btn(tr("Посмотреть", "Have a look"), Modifier.fillMaxWidth()) { close(); nav.tab = 2 }
            } }
        "hatch", "stage" -> { val p = d.pets.firstOrNull { it.id == parts[1].toLongOrNull() }
            if (p == null) { close(); return }
            val st = if (parts[0] == "hatch") Stage.BABY else Stage.entries.getOrElse(parts.getOrNull(2)?.toIntOrNull() ?: 1) { Stage.BABY }
            Popup(::close) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { Creature(p.species, st, Pose.JOY, 170.dp, p.shiny) }
                Text(if (parts[0] == "hatch") tr("Вылупился: ${p.sp().name.lowercase()}!", "A ${p.sp().name.lowercase()} hatched!") else "${p.name}: ${st.label.lowercase()}", style = T.title, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                Text(if (parts[0] == "hatch") tr("Зовут ${p.name}. Характер: ${p.nat().label.lowercase()} – ${p.nat().about}.", "Name: ${p.name}. Nature: ${p.nat().label.lowercase()} – ${p.nat().about}.")
                    else if (st == Stage.ADULT) tr("Вырос за ночь! Его путь: ${Form.of(p.form)?.label ?: ""} – ${Form.of(p.form)?.about ?: ""}.", "Grown overnight! Its way: ${Form.of(p.form)?.label ?: ""} – ${Form.of(p.form)?.about ?: ""}.")
                    else if (st == Stage.MASTER) tr("Мастер: вокруг него теперь мягкий свет.", "A master now, with a soft glow around it.")
                    else tr("Подрос, пока спал.", "Grew in its sleep, on your calm."), style = T.body, modifier = Modifier.fillMaxWidth().padding(top = 6.dp), textAlign = TextAlign.Center)
                if (p.shiny && parts[0] == "hatch") Text(tr("Редкий окрас – такой бывает один раз из шестнадцати!", "A rare colouring – one in sixteen!"), style = T.smallB.copy(color = C.gold), modifier = Modifier.fillMaxWidth().padding(top = 6.dp), textAlign = TextAlign.Center)
                Spacer(Modifier.height(16.dp)); Btn(tr("Ура!", "Hooray!"), Modifier.fillMaxWidth()) { close() }
            } }
        "course" -> { val co = Courses.byId[parts.getOrNull(1) ?: ""]
            Popup(::close) {
                Badge(Icons.BOOK, Color(co?.color ?: 0xFF5FA8C9), 56.dp)
                Text(tr("Курс пройден!", "Course complete!"), style = T.title, modifier = Modifier.padding(top = 10.dp))
                Text(tr("«${co?.name}» – все дни позади. Можно выбрать следующий.", "«${co?.name}» – every day done. You can pick the next one."), style = T.body, modifier = Modifier.padding(top = 6.dp))
                Spacer(Modifier.height(16.dp)); Btn(tr("Хорошо", "Good"), Modifier.fillMaxWidth()) { close(); c.write { settings.update { it.copy(course = "", courseDay = 0) } } }
            } }
        else -> close()
    }
}

@Composable
fun ShopPage(d: Data, nav: Nav) {
    val c = LocalContext.current.container
    var preview by remember { mutableStateOf<String?>(null) }
    Page(tr("Чайная", "The tea house"), { nav.pop() }, action = {
        Row(Modifier.padding(end = 12.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.SHELL, C.gold, 18.dp); Text(" ${fmt(d.s.shells)}", style = T.numS.copy(color = C.gold)) } }) {
        Box(Modifier.card(22.dp)) { TeaHouse(d.team, d.s.items - d.s.hidden, Sky.of(d.s), d.tide, preview = preview) }
        Hint("shop", d.s.seen, Icons.SHELL, tr("Обставьте чайную", "Furnish the tea house"), tr("Ракушки приносят практики, колокольчики, задания и находки на дне. Нажмите на вещь – она появится в комнате на примерку.",
            "Shells come from practices, bells, quests and finds on the sea floor. Tap a thing to try it in the room."), Modifier.padding(vertical = 12.dp))
        if ("shop" in d.s.seen) Spacer(Modifier.height(12.dp))
        Shop.all.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { it ->
                    val owned = it.id in d.s.items; val shown = owned && it.id !in d.s.hidden
                    Column(Modifier.weight(1f).card(18.dp).clickable { preview = it.id }.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Picture("item_${it.id}", 84.dp)
                        Text(it.name, style = T.smallB, textAlign = TextAlign.Center, maxLines = 2)
                        Spacer(Modifier.height(8.dp))
                        if (owned) Ghost(if (shown) tr("Убрать", "Put away") else tr("Поставить", "Put out"), small = true) {
                            c.write { settings.update { s -> s.copy(hidden = if (shown) s.hidden + it.id else s.hidden - it.id) } } }
                        else Btn("${it.price}", enabled = d.s.shells >= it.price, small = true, color = C.gold) { c.write { if (buy(it.id)) say(tr("${it.name} – в чайной!", "${it.name} is in the tea house!")) } }
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun Setting(icon: Icons, tint: Color, title: String, sub: String? = null, content: @Composable () -> Unit = {}) {
    Column(Modifier.padding(bottom = 10.dp).fillMaxWidth().card(20.dp).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Badge(icon, tint, 38.dp)
            Column(Modifier.weight(1f).padding(start = 12.dp)) { Text(title, style = T.bodyB); if (sub != null) Text(sub, style = T.small) }
        }
        Box(Modifier.padding(top = 10.dp)) { content() }
    }
}

/** The StillCraft bot: deep links file a bug or an idea for this app, or follow its releases. */
object Community {
    private const val BOT = "https://t.me/ArcaneWhisperBot?start="
    const val SLUG = "puffling"
    const val BUG = "${BOT}bug_$SLUG"
    const val IDEA = "${BOT}wish_$SLUG"
    const val FOLLOW = "${BOT}f_$SLUG"
    const val CHAT = "https://t.me/+Q4gFEDk8O842OGVi"
    const val SUPPORT = "https://bigdevwhale.github.io/stillcraft/support/"
    const val SITE = "https://bigdevwhale.github.io/stillcraft/"
    const val CODE = "https://github.com/bigdevwhale/puffling"
}

@Composable
private fun LinkRow(icon: Icons, tint: Color, title: String, sub: String?, url: String) {
    val ctx = LocalContext.current
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable {
        runCatching { ctx.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url)).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Badge(icon, tint, 36.dp)
        Column(Modifier.weight(1f).padding(start = 12.dp)) { Text(title, style = T.bodyB); if (sub != null) Text(sub, style = T.small) }
        Icon(Icons.NEXT, C.mute, 18.dp)
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun SettingsPage(d: Data, nav: Nav) {
    val ctx = LocalContext.current
    val c = ctx.container
    val s = d.s
    fun set(f: (Settings) -> Settings) = c.write { settings.update(f); Bells.schedule(app, settings.now()) }
    var tab by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(0) }
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        set { it.copy(bells = true) }; if (!ok) c.say(tr("Без уведомлений колокольчик будет звучать, но без кнопки «Три вдоха»", "Without notifications the bell still rings, but without the «Three breaths» button")) }
    Page(tr("Настройки", "Settings"), { nav.pop() }) {
        app.puffling.ui.components.Hint("settings", d.s.seen, app.puffling.ui.components.Icons.GEAR, tr("Настройки", "Settings"), tr("Четыре вкладки: практика, колокольчик, вид и ещё. Всё сохраняется сразу.", "Four tabs: practice, bell, look and more. Everything saves at once."), Modifier.padding(bottom = 10.dp))
        Row(Modifier.fillMaxWidth().padding(bottom = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(Icons.WIND to tr("Практика", "Practice"), Icons.BELL to tr("Колокольчик", "Bell"), Icons.PALETTE to tr("Вид", "Look"), Icons.DOTS to tr("Ещё", "More"))
                .forEachIndexed { i, (ic, l) -> val on = tab == i
                    Column(Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(if (on) C.acc else C.card).clickable { tab = i }.padding(vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(ic, if (on) C.accInk else C.mute, 22.dp)
                        Text(l, style = T.tiny.copy(color = if (on) C.accInk else C.mute), modifier = Modifier.padding(top = 4.dp), maxLines = 1)
                    } }
        }
        when (tab) {
            0 -> {
                Setting(Icons.WAVE, C.acc, tr("Прилив недели", "The week's high tide"), tr("Сколько минут практики в неделю – тогда море в окне поднимется до краёв. Пропущенные дни не страшны.", "Minutes of practice a week to bring the sea to the brim. Missed days are fine.")) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { listOf(30, 45, 60, 90, 120, 180).forEach { n -> Chip("$n", s.weekGoal == n) { set { it.copy(weekGoal = n) } } } } }
                Setting(Icons.WIND, C.acc, tr("Звуки дыхания", "Breath sounds"), tr("Тихий шорох вдоха и выдоха, чтобы дышать с закрытыми глазами", "A soft whoosh of the breath in and out, to breathe with eyes closed")) {
                    Segments(listOf(tr("включены", "on"), tr("выключены", "off")), if (s.cues) 0 else 1) { i -> set { it.copy(cues = i == 0) } } }
                Setting(Icons.SPARK, C.ok, tr("Вибрация", "Vibration"), tr("Лёгкий толчок на каждой смене дыхания", "A light tap at each turn of the breath")) {
                    Segments(listOf(tr("включена", "on"), tr("выключена", "off")), if (s.haptics) 0 else 1) { i -> set { it.copy(haptics = i == 0) } } }
                Setting(Icons.BELL, C.gold, tr("Громкость", "Volume"), tr("Фона, колоколов и дыхания", "Background, bells and breath")) {
                    var v by remember { mutableStateOf(s.volume) }
                    Slider(v, { v = it }, onValueChangeFinished = { set { it.copy(volume = v) }; Synth.ring(s.bellKind, v) }, colors = SliderDefaults.colors(thumbColor = C.acc, activeTrackColor = C.acc)) }
                Setting(Icons.HEART, C.bad, tr("Спрашивать, как внутри", "Ask how it feels"), tr("До и после практики – так «Летопись» покажет, что помогает именно вам", "Before and after – so the journal can show what helps you")) {
                    Segments(listOf(tr("спрашивать", "ask"), tr("не надо", "don't")), if (s.askFeel) 0 else 1) { i -> set { it.copy(askFeel = i == 0) } } }
                if (Lang.ru) Setting(Icons.EDIT, C.mute, "Как к вам обращаться", "Чтобы аффирмации звучали от вашего лица: «я благодарен» или «я благодарна»") {
                    Segments(listOf("в мужском роде", "в женском роде"), if (s.fem) 1 else 0) { i -> set { it.copy(fem = i == 1) } } }
            }
            1 -> {
                Setting(Icons.BELL, C.gold, tr("Колокольчик внимания", "The mindfulness bell"), tr("Несколько раз в день, в случайный момент, тихо звонит колокольчик. Остановитесь на три вдоха – и на тропе к чайной зажжётся фонарик.",
                    "A few times a day, at a random moment, a bell rings softly. Stop for three breaths – and a lantern lights up on the path to the tea house.")) {
                    Segments(listOf(tr("звонит", "on"), tr("молчит", "off")), if (s.bells) 0 else 1) { i ->
                        if (i == 0 && Build.VERSION.SDK_INT >= 33) ask.launch(Manifest.permission.POST_NOTIFICATIONS) else set { it.copy(bells = i == 0) } } }
                if (s.bells) {
                    Setting(Icons.CLOCK, C.acc, tr("Сколько раз в день", "How many times a day")) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { listOf(2, 3, 4, 6, 8, 10).forEach { n -> Chip("$n", s.bellsPerDay == n) { set { it.copy(bellsPerDay = n) } } } } }
                    Setting(Icons.SUN, C.gold, tr("С какого часа", "From"), "${s.bellFrom}:00") {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { listOf(7, 8, 9, 10, 11, 12).forEach { n -> Chip("$n", s.bellFrom == n) { set { it.copy(bellFrom = n) } } } } }
                    Setting(Icons.MOON, C.acc, tr("До какого часа", "Until"), "${s.bellTo}:00") {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { listOf(17, 18, 19, 20, 21, 22).forEach { n -> Chip("$n", s.bellTo == n) { set { it.copy(bellTo = n) } } } } }
                    Setting(Icons.CAL, C.ok, tr("По каким дням", "Which days")) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) { (1..7).forEach { n -> val on = n in s.bellDays
                            Chip(app.puffling.domain.dayShort(n - 1), on, Modifier.weight(1f)) { set { it.copy(bellDays = if (on) it.bellDays - n else it.bellDays + n) } } } } }
                    Setting(Icons.BELL, C.gold, tr("Голос колокольчика", "The bell's voice")) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("bowl" to tr("Чаша", "Bowl"), "bell" to tr("Колокол", "Bell"), "chime" to tr("Тингша", "Tingsha"), "gong" to tr("Гонг", "Gong")).forEach { (k, l) ->
                                Chip(l, s.bellKind == k) { set { it.copy(bellKind = k) }; Synth.ring(k, s.volume) } } } }
                    Text(tr("Во время звонка и в беззвучном режиме колокольчик молчит. На HyperOS и других «экономных» прошивках разрешите приложению автозапуск, иначе система может его усыплять.",
                        "During a call and in silent mode the bell keeps quiet. On HyperOS and other battery-saving systems allow the app to autostart, or the system may put it to sleep."), style = T.tiny, modifier = Modifier.padding(4.dp))
                }
            }
            2 -> {
                Setting(Icons.PALETTE, C.acc, tr("Тема", "Theme")) {
                    Segments(listOf(tr("светлая", "light"), tr("тёмная", "dark"), tr("как в системе", "system")), when (s.theme) { "light" -> 0; "dark" -> 1; else -> 2 }) { i -> set { it.copy(theme = listOf("light", "dark", "system")[i]) } } }
                Setting(Icons.GLOBE, C.ok, tr("Язык", "Language")) {
                    val lang = currentLanguage()
                    Segments(listOf(tr("системный", "system"), "Русский", "English"), when (lang) { "ru" -> 1; "en" -> 2; else -> 0 }) { i -> setLanguage(listOf("", "ru", "en")[i]) } }
                val o = app.puffling.data.Outside.of(s.outside)
                Setting(Icons.SUN, C.gold, tr("За круглым окном", "Outside the round window"),
                    (if (s.town.isNotEmpty()) s.town else tr("Город не выбран", "No town chosen")) + (o?.let { " · ${weatherWord(it.weather)}" + (if (it.temp.isNaN()) "" else ", ${Math.round(it.temp)}°") } ?: "")) {
                    Column {
                        var q by remember { mutableStateOf("") }
                        var found by remember { mutableStateOf<List<app.puffling.data.Town>>(emptyList()) }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Field(q, { q = it }, tr("Другой город", "Another town"), Modifier.weight(1f))
                            Spacer(Modifier.width(8.dp))
                            Btn(tr("Найти", "Find"), small = true, enabled = q.isNotBlank()) { c.write {
                                found = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { runCatching { OutsideSource.towns(q.trim(), Lang.ru) }.getOrDefault(emptyList()) }
                                if (found.isEmpty()) say(tr("Ничего не нашлось", "Nothing found")) } }
                        }
                        found.forEach { t -> Text("${t.name}, ${t.region}", style = T.body, modifier = Modifier.fillMaxWidth().clickable {
                            c.write { settings.update { it.copy(town = t.name, lat = t.lat, lon = t.lon, guessed = true, outside = "") }; refreshOutside(true) }; found = emptyList(); q = "" }.padding(vertical = 8.dp)) }
                        Text(tr("В окне – время суток и погода там. Отправляются только координаты города – в Open-Meteo или MET Norway.",
                            "The window shows the time of day and the weather there. Only the town's coordinates are sent – to Open-Meteo or MET Norway."), style = T.tiny, modifier = Modifier.padding(top = 6.dp))
                    }
                }
                Setting(Icons.SHELL, C.gold, tr("Вещи в чайной", "Things in the tea house"), tr("Купить, поставить или убрать", "Buy, put out or put away")) {
                    Ghost(tr("Открыть чайную", "Open the tea house"), Modifier.fillMaxWidth(), small = true) { nav.push(Screen.Shop) } }
            }
            else -> {
                Column(Modifier.padding(bottom = 10.dp).fillMaxWidth().clip(RoundedCornerShape(22.dp))
                    .background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(C.acc, Color(0xFF2B6F8E)))).padding(18.dp)) {
                    Text(tr("Поддержать Puffling", "Support Puffling"), style = T.h2.copy(color = Color.White))
                    Text(tr("Приложение бесплатное, без рекламы и аккаунта. Если оно вам помогает, можно поддержать автора – так появятся новые питомцы и практики.",
                        "The app is free, with no ads and no account. If it helps you, you can support the author – that's how new pets and practices appear."),
                        style = T.small.copy(color = Color(0xEEFFFFFF)), modifier = Modifier.padding(top = 4.dp))
                    Btn(tr("💛 Поддержать", "💛 Support"), Modifier.fillMaxWidth().padding(top = 14.dp), color = Color.White, ink = C.acc) {
                        runCatching { ctx.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(Community.SUPPORT)).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)) } }
                }
                Setting(Icons.GLOBE, C.ok, "StillCraft", tr("Puffling – одно из небольших приложений StillCraft", "Puffling is one of the small StillCraft apps")) {
                    Column {
                        LinkRow(Icons.HEART, C.acc, tr("Сообщество в Telegram", "Telegram community"), tr("новости, обсуждения, другие приложения", "news, talk and the other apps"), Community.CHAT)
                        LinkRow(Icons.CLOSE, C.bad, tr("Сообщить об ошибке", "Report a bug"), tr("через бота StillCraft", "through the StillCraft bot"), Community.BUG)
                        LinkRow(Icons.SPARK, C.gold, tr("Предложить идею", "Suggest an idea"), null, Community.IDEA)
                        LinkRow(Icons.STAR, C.ok, tr("Следить за обновлениями", "Follow updates"), tr("бот напишет, когда выйдет новая версия", "the bot writes when a new version is out"), Community.FOLLOW)
                        LinkRow(Icons.HOME, C.acc, tr("Все приложения StillCraft", "All StillCraft apps"), null, Community.SITE)
                        LinkRow(Icons.LIST, C.mute, tr("Исходный код", "Source code"), "GitHub", Community.CODE)
                    }
                }
                Setting(Icons.SPARK, C.acc, tr("Подсказки", "Hints"), tr("Короткие пояснения на каждом экране", "A short note on every screen")) {
                    Ghost(tr("Показать заново", "Show again"), Modifier.fillMaxWidth(), small = true) { c.write { settings.update { it.copy(seen = emptySet()) } }; c.say(tr("Подсказки покажутся снова", "Hints will show again")) } }
                Setting(Icons.HOME, C.ok, tr("О приложении", "About"), tr("Puffling работает без аккаунта. Практики, питомцы и записи хранятся только на телефоне; в интернет уходят лишь координаты города для погоды. Все звуки рождаются прямо в приложении.",
                    "Puffling needs no account. Practices, pets and notes stay on the phone; only the town's coordinates go online, for the weather. Every sound is made right in the app."))
                Text(tr("Погода – Open-Meteo и MET Norway. Дыхание Вима Хофа устроено как в SwissZen.", "Weather: Open-Meteo and MET Norway. Wim Hof breathing works as in SwissZen."), style = T.tiny, modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp))
            }
        }
    }
}

fun weatherWord(w: Weather) = when (w) {
    Weather.CLEAR -> tr("ясно", "clear"); Weather.CLOUDY -> tr("облачно", "cloudy"); Weather.OVERCAST -> tr("пасмурно", "overcast")
    Weather.RAIN -> tr("дождь", "rain"); Weather.SNOW -> tr("снег", "snow"); Weather.FOG -> tr("туман", "fog"); Weather.STORM -> tr("гроза", "storm")
}

/** Own affirmations and favourites. */
@Composable
fun AffirmationsPage(d: Data, nav: Nav) {
    val c = LocalContext.current.container
    var text by remember { mutableStateOf("") }
    var edit by remember { mutableStateOf<OwnAff?>(null) }
    var theme by remember { mutableStateOf("own") }
    Page(tr("Мои слова", "My words"), { nav.pop() }) {
        app.puffling.ui.components.Hint("words", d.s.seen, app.puffling.ui.components.Icons.HEART, tr("Мои слова", "My words"), tr("Сердечко – в любимые. Свои и любимые фразы попадают в аффирмации дня и на главную.", "The heart keeps a phrase. Your own and favourite ones go into the daily affirmations and onto the home screen."), Modifier.padding(bottom = 10.dp))
        Text(tr("Напишите то, что хотите слышать от себя. Лучше в настоящем времени и без «не»: «я справляюсь», а не «я не боюсь».",
            "Write what you want to hear from yourself. Present tense and without «not» works best: «I am coping», not «I am not afraid»."), style = T.small)
        Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Field(text, { text = it }, tr("Я…", "I…"), Modifier.weight(1f), single = false)
            Spacer(Modifier.width(8.dp))
            Btn(tr("Добавить", "Add"), small = true, enabled = text.isNotBlank()) { val t = text.trim(); text = ""; c.write { dao.insert(OwnAff(text = t, created = System.currentTimeMillis())) } }
        }
        Spacer(Modifier.height(10.dp))
        val tabs = listOf("own" to tr("Свои", "Mine"), "fav" to tr("Любимые", "Favourites")) + Affirmations.themes.map { it.id to it.name }
        app.puffling.ui.practice.FlowChips(tabs, theme) { theme = it }
        Spacer(Modifier.height(10.dp))
        val list = when (theme) {
            "own" -> d.affs.map { app.puffling.domain.Aff("u${it.id}", it.text, "own", true) }
            "fav" -> (Affirmations.builtin() + d.affs.map { app.puffling.domain.Aff("u${it.id}", it.text, "own", true) }).filter { it.id in d.s.favs }
            else -> Affirmations.builtin().filter { it.theme == theme }
        }
        if (list.isEmpty()) Empty(if (theme == "own") tr("Пока ни одной – начните с одной фразы.", "None yet – start with one phrase.") else tr("Нажмите сердечко у фразы, чтобы она попала сюда.", "Tap the heart on a phrase to keep it here."))
        Column(Modifier.fillMaxWidth().card(18.dp)) {
            list.forEachIndexed { i, a -> val fav = a.id in d.s.favs
                if (i > 0) Box(Modifier.fillMaxWidth().padding(horizontal = 14.dp).height(1.dp).background(C.line))
                Row(Modifier.fillMaxWidth().clickable(enabled = a.own) { edit = d.affs.firstOrNull { "u${it.id}" == a.id } }.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(a.text, style = T.body.copy(fontStyle = FontStyle.Italic), modifier = Modifier.weight(1f))
                    Box(Modifier.size(36.dp).clip(CircleShape).clickable { c.write { settings.update { it.copy(favs = if (fav) it.favs - a.id else it.favs + a.id) } } }, contentAlignment = Alignment.Center) {
                        Icon(Icons.HEART, if (fav) C.bad else C.line, 19.dp) }
                }
            }
        }
        if (d.affs.isNotEmpty() || d.s.favs.isNotEmpty()) Btn(tr("Сказать вслух", "Say them aloud"), Modifier.fillMaxWidth().padding(top = 16.dp)) { nav.push(Screen.Begin("affirm")) }
        val bottles = d.bottles
        if (bottles.isNotEmpty()) {
            Section(tr("Бутылки в море", "Bottles at sea"), icon = Icons.BOTTLE)
            Text(tr("Отправлено: ${bottles.size}. Вернулось: ${bottles.count { it.back <= today() }}. Следующую прибьёт ${bottles.filter { it.back > today() }.minOfOrNull { it.back }?.let { fmtDay(it) } ?: "когда-нибудь"}.",
                "Sent: ${bottles.size}. Came back: ${bottles.count { it.back <= today() }}. The next one washes up ${bottles.filter { it.back > today() }.minOfOrNull { it.back }?.let { fmtDay(it) } ?: "some day"}."), style = T.small)
        }
    }
    edit?.let { a -> var t by remember(a.id) { mutableStateOf(a.text) }
        Popup({ edit = null }) {
            Field(t, { t = it }, "", Modifier.fillMaxWidth(), single = false)
            Row(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Ghost(tr("Удалить", "Delete"), Modifier.weight(1f), color = C.bad) { c.write { dao.deleteAff(a.id) }; edit = null }
                Btn(tr("Сохранить", "Save"), Modifier.weight(1f), enabled = t.isNotBlank()) { c.write { dao.update(a.copy(text = t.trim())) }; edit = null }
            }
        }
    }
}

/** Everything found on the sea floor during holds. */
@Composable
fun AquariumPage(d: Data, nav: Nav) {
    val best = d.sessions.filter { it.practice != "bolt" }.flatMap { it.holdList() }.maxOrNull() ?: 0
    Page(tr("Аквариум", "Aquarium"), { nav.pop() }, action = { Box(Modifier.size(44.dp).clip(CircleShape).clickable { nav.share = ShareReq(CardKind.AQUARIUM) }, contentAlignment = Alignment.Center) { Icon(Icons.SHARE, C.ink, 20.dp) } }) {
        app.puffling.ui.components.Hint("aquarium", d.s.seen, app.puffling.ui.components.Icons.FISH, tr("Аквариум", "Aquarium"), tr("Чем дольше задержка в Виме Хофе, тем глубже нырок и тем больше находок.", "The longer the hold in Wim Hof, the deeper the dive and the more you find."), Modifier.padding(bottom = 10.dp))
        Text(tr("Пока вы держите дыхание в методе Вима Хофа, питомец ныряет – полметра за секунду. Всё, что он увидел на глубине, живёт здесь.",
            "While you hold your breath in Wim Hof, your pet dives – half a metre a second. Everything it saw in the deep lives here."), style = T.small)
        Text(tr("Найдено ${d.s.finds.size} из ${Deep.all.size} · глубже всего ${app.puffling.domain.depthOf(best)} м", "Found ${d.s.finds.size} of ${Deep.all.size} · deepest ${app.puffling.domain.depthOf(best)} m"), style = T.bodyB, modifier = Modifier.padding(top = 8.dp))
        Spacer(Modifier.height(10.dp))
        Deep.all.chunked(3).forEach { row -> Row(Modifier.padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            row.forEach { f -> val got = f.id in d.s.finds
                Column(Modifier.weight(1f).clip(RoundedCornerShape(18.dp)).background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color(0xFF3F8FC0), Color(0xFF0B2236)).map { if (got) it else it.copy(alpha = .35f) })).padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    Picture("find_${f.id}", 76.dp, dim = !got)
                    Text(if (got) f.name else "???", style = T.smallB.copy(color = Color.White), maxLines = 1, textAlign = TextAlign.Center)
                    Text(tr("${f.metres} м · ${clock(f.secs)}", "${f.metres} m · ${clock(f.secs)}"), style = T.tiny.copy(color = Color.White.copy(alpha = .7f)))
                } }
            repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
        } }
    }
}

