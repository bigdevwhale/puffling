package app.puffling.ui.practice

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.puffling.container
import app.puffling.data.OwnPractice
import app.puffling.domain.Affirmations
import app.puffling.domain.Course
import app.puffling.domain.Courses
import app.puffling.domain.Element
import app.puffling.domain.Kind
import app.puffling.domain.Ph
import app.puffling.domain.Phase
import app.puffling.domain.Pets
import app.puffling.domain.Practice
import app.puffling.domain.Practices
import app.puffling.domain.Stage
import app.puffling.domain.clock
import app.puffling.domain.count
import app.puffling.domain.dayOf
import app.puffling.domain.fmt1
import app.puffling.domain.fmtDayShort
import app.puffling.domain.mins
import app.puffling.domain.today
import app.puffling.domain.tr
import app.puffling.ui.Data
import app.puffling.ui.Nav
import app.puffling.ui.Screen
import app.puffling.ui.components.Btn
import app.puffling.ui.components.Chip
import app.puffling.ui.components.Field
import app.puffling.ui.components.Ghost
import app.puffling.ui.components.Hint
import app.puffling.ui.components.Icon
import app.puffling.ui.components.Icons
import app.puffling.ui.components.Line
import app.puffling.ui.components.Page
import app.puffling.ui.components.Section
import app.puffling.ui.components.Sheet
import app.puffling.ui.components.card
import app.puffling.ui.theme.C
import app.puffling.ui.theme.T
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

fun kindIcon(k: Kind) = when (k) { Kind.BREATH -> Icons.WIND; Kind.WIMHOF -> Icons.DROP; Kind.SIT -> Icons.LOTUS; Kind.GUIDED -> Icons.LEAF; Kind.AFFIRM -> Icons.HEART }
fun elIcon(e: Element) = when (e) { Element.AIR -> Icons.WIND; Element.DEEP -> Icons.DROP; Element.STILL -> Icons.LOTUS; Element.EARTH -> Icons.LEAF; Element.HEART -> Icons.HEART; Element.LIGHT -> Icons.SUN }
fun lengthOf(p: Practice) = when (p.kind) { Kind.GUIDED -> mins(p.guidedSecs); Kind.WIMHOF -> count(p.rounds, "раунд", "раунда", "раундов", "round", "rounds"); Kind.AFFIRM -> tr("5 фраз", "5 phrases"); else -> tr("${p.minutes} мин", "${p.minutes} min") }

/** The family's face on the practices tab: the pet that loves it. */
private fun familyPet(e: Element) = when (e) { Element.AIR -> "puffin"; Element.DEEP -> "puffer"; Element.STILL -> "cat"; Element.EARTH -> "heron"; Element.HEART -> "panda"; Element.LIGHT -> "moth" }
private fun familyLine(e: Element) = when (e) {
    Element.AIR -> tr("дыхание по счёту", "breathing to a count"); Element.DEEP -> tr("Вим Хоф и задержки", "Wim Hof and holds")
    Element.STILL -> tr("тихое сидение", "sitting in silence"); Element.EARTH -> tr("тело и шаги", "body and steps")
    Element.HEART -> tr("тёплые слова", "warm words"); Element.LIGHT -> tr("внимание", "attention") }

/**
 * Six families, not twenty-odd practices: the tab shows the families as cards, and a family opens
 * into a plain list of names and lengths.
 */
@Composable
fun LibraryScreen(d: Data, nav: Nav) {
    var make by remember { mutableStateOf(false) }
    var open by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf<String?>(null) }
    val own = Practices.own
    if (open != null) {
        androidx.activity.compose.BackHandler { open = null }
        val el = Element.entries.firstOrNull { it.id == open }
        val list = if (el == null) own else (Practices.builtin + own).filter { it.el == el }
        val col = el?.let { Color(it.color) } ?: C.acc
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(44.dp).clip(CircleShape).clickable { open = null }, contentAlignment = Alignment.Center) { Icon(Icons.BACK, C.ink, 22.dp) }
                Text(el?.label ?: tr("Свои", "Own"), style = T.title, modifier = Modifier.weight(1f))
                if (el != null) app.puffling.ui.art.Creature(familyPet(el), Stage.ADULT, app.puffling.ui.art.Pose.PRACTICE, 56.dp, glow = false)
            }
            app.puffling.ui.components.Hint("family", d.s.seen, app.puffling.ui.components.Icons.LIST, tr("Выберите практику", "Pick a practice"), tr("Нажмите на название – откроется страница с длительностью и звуком, оттуда и начнёте.", "Tap a name to open its page with length and sound, and start from there."), Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
            Column(Modifier.padding(horizontal = 16.dp).padding(top = 6.dp).fillMaxWidth().card(20.dp)) {
                list.forEachIndexed { i, p ->
                    if (i > 0) Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(1.dp).background(C.line))
                    Row(Modifier.fillMaxWidth().clickable { nav.push(Screen.Begin(p.id)) }.padding(horizontal = 16.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(p.name, style = T.body, modifier = Modifier.weight(1f))
                        Text(lengthOf(p), style = T.small)
                        Icon(Icons.NEXT, C.line, 16.dp, Modifier.padding(start = 6.dp))
                    }
                }
            }
            if (el == null || own.isEmpty()) Text(tr("+ Собрать свою", "+ Make your own"), style = T.smallB.copy(color = col), modifier = Modifier.padding(start = 20.dp, top = 14.dp).clickable { make = true })
        }
    } else Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 24.dp)) {
        Row(Modifier.fillMaxWidth().padding(start = 4.dp, top = 6.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(tr("Практики", "Practices"), style = T.title, modifier = Modifier.weight(1f))
            Row(Modifier.clip(RoundedCornerShape(50)).background(C.accSoft).clickable { make = true }.padding(horizontal = 12.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.PLUS, C.acc, 16.dp); Text(tr(" Своя", " Own"), style = T.smallB.copy(color = C.acc)) }
        }
        app.puffling.ui.components.Hint("library", d.s.seen, app.puffling.ui.components.Icons.LOTUS, tr("Шесть стихий", "Six families"), tr("Практики собраны по стихиям – нажмите на карточку. Питомец на ней любит эту стихию и ест за неё больше. Ниже – курсы на неделю и больше.", "Practices are grouped into families – tap a card. The pet on it loves that family and eats more for it. Below are courses of a week and more."), Modifier.padding(bottom = 10.dp))
        Element.entries.chunked(2).forEach { row -> Row(Modifier.padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            row.forEach { el -> val n = (Practices.builtin + own).count { it.el == el }; val col = Color(el.color)
                Box(Modifier.weight(1f).height(124.dp).clip(RoundedCornerShape(22.dp)).background(col.copy(alpha = if (C.dark) .22f else .14f)).clickable { open = el.id }) {
                    Column(Modifier.padding(14.dp)) {
                        Text(el.label, style = T.h3)
                        Text(familyLine(el), style = T.tiny, maxLines = 1)
                    }
                    Text("$n", style = T.small.copy(color = col), modifier = Modifier.align(Alignment.BottomStart).padding(14.dp))
                    app.puffling.ui.art.Creature(familyPet(el), Stage.ADULT, app.puffling.ui.art.Pose.IDLE, 78.dp, glow = false, modifier = Modifier.align(Alignment.BottomEnd).padding(end = 4.dp))
                }
            }
        } }
        if (own.isNotEmpty()) Row(Modifier.fillMaxWidth().card(18.dp).clickable { open = "own" }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(tr("Свои практики", "Your own practices"), style = T.bodyB, modifier = Modifier.weight(1f)); Text("${own.size}", style = T.small); Icon(Icons.NEXT, C.line, 16.dp, Modifier.padding(start = 6.dp)) }
        Text(tr("КУРСЫ", "COURSES"), style = T.sec, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp, start = 4.dp))
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Courses.all.forEach { co -> CourseTile(co, d) { nav.push(Screen.Course(co.id)) } }
        }
        Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ToolTile(Icons.TIMER, Color(Element.DEEP.color), tr("Тест", "Test"), Modifier.weight(1f)) { nav.push(Screen.Bolt) }
            ToolTile(Icons.FISH, C.acc, tr("Аквариум", "Aquarium"), Modifier.weight(1f)) { nav.push(Screen.Aquarium) }
            ToolTile(Icons.HEART, Color(Element.HEART.color), tr("Мои слова", "My words"), Modifier.weight(1f)) { nav.push(Screen.Affirmations) }
        }
    }
    if (make) Sheet({ make = false }) {
        Text(tr("Какую практику собрать?", "What kind of practice?"), style = T.h2)
        Spacer(Modifier.height(10.dp))
        listOf(Kind.BREATH to tr("фазы и секунды", "phases and seconds"), Kind.SIT to tr("со своими колокольчиками", "with your own bells"),
            Kind.GUIDED to tr("шаги своими словами", "steps in your own words"), Kind.AFFIRM to tr("колода фраз", "a deck of phrases"), Kind.WIMHOF to tr("свои раунды", "your own rounds")).forEach { (k, l) ->
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable { make = false; nav.push(Screen.OwnEdit(0, k.code)) }.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                app.puffling.ui.components.Badge(kindIcon(k), C.acc); Spacer(Modifier.width(12.dp))
                Text(k.label, style = T.bodyB, modifier = Modifier.weight(1f)); Text(l, style = T.small)
            }
        }
    }
}

/** A practice as a quiet tile: the family's colour and icon, the name, how long. Nothing else. */
@Composable
fun PracticeTile(p: Practice, modifier: Modifier, onClick: () -> Unit) {
    val col = Color(p.el.color)
    Column(modifier.heightIn(min = 108.dp).fillMaxHeight().clip(RoundedCornerShape(18.dp)).background(col.copy(alpha = if (C.dark) .2f else .12f)).clickable(onClick = onClick).padding(12.dp)) {
        Box(Modifier.size(32.dp).clip(CircleShape).background(col.copy(alpha = .22f)), contentAlignment = Alignment.Center) { Icon(kindIcon(p.kind), col, 17.dp) }
        Spacer(Modifier.weight(1f).heightIn(min = 14.dp))
        Text(p.name, style = T.bodyB, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(lengthOf(p), style = T.tiny)
    }
}

@Composable
private fun ToolTile(icon: Icons, tint: Color, title: String, modifier: Modifier, onClick: () -> Unit) {
    Column(modifier.card(16.dp).clickable(onClick = onClick).padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, tint, 22.dp); Text(title, style = T.tiny.copy(color = C.ink), modifier = Modifier.padding(top = 6.dp), maxLines = 1)
    }
}

@Composable
private fun CourseTile(co: Course, d: Data, onClick: () -> Unit) {
    val on = d.s.course == co.id
    Column(Modifier.width(150.dp).card(18.dp, if (on) Color(co.color).copy(alpha = .16f) else null).clickable(onClick = onClick).padding(12.dp)) {
        Icon(Icons.BOOK, Color(co.color), 18.dp)
        Text(co.name, style = T.bodyB, modifier = Modifier.padding(top = 6.dp), maxLines = 2, minLines = 2, overflow = TextOverflow.Ellipsis)
        Text(if (on) tr("Идёт: ${d.s.courseDay} из ${co.days.size}", "On: ${d.s.courseDay} of ${co.days.size}") else count(co.days.size, "день", "дня", "дней", "day", "days"), style = T.small.copy(color = if (on) Color(co.color) else C.mute))
    }
}

/** The round button: what is needed right now. */
@Composable
fun QuickSheet(d: Data, nav: Nav) {
    val ctx = LocalContext.current
    fun go(id: String) { nav.quick = false; nav.push(Screen.Begin(id)) }
    Sheet({ nav.quick = false }) {
        Text(tr("Что сейчас нужно?", "What do you need right now?"), style = T.h2)
        Spacer(Modifier.height(12.dp))
        val co = Courses.byId[d.s.course]
        if (co != null && d.s.courseDay < co.days.size && d.s.courseLast != today()) {
            val p = Practices.of(co.days[d.s.courseDay])
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color(co.color).copy(alpha = .15f)).clickable { nav.quick = false; nav.push(Screen.Begin(p.id, "${co.id}:${d.s.courseDay}")) }.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.BOOK, Color(co.color), 22.dp); Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) { Text(tr("День ${d.s.courseDay + 1} курса", "Course day ${d.s.courseDay + 1}"), style = T.bodyB); Text(p.name, style = T.small) }
                Icon(Icons.PLAY, Color(co.color), 18.dp)
            }
            Spacer(Modifier.height(10.dp))
        }
        val opts = listOf(
            Triple(tr("Успокоиться", "Calm down"), "sigh", Icons.WIND), Triple(tr("Собраться", "Steady myself"), "box", Icons.TARGET),
            Triple(tr("Взбодриться", "Wake up"), if (d.real.any { it.kind == Kind.WIMHOF.code }) "wimhof" else "bellows", Icons.SUN), Triple(tr("Уснуть", "Fall asleep"), "478", Icons.MOON),
            Triple(tr("Посидеть", "Sit quietly"), "sit", Icons.LOTUS), Triple(tr("Вернуться в тело", "Back to the body"), "ground", Icons.LEAF),
        )
        opts.chunked(2).forEach { row -> Row(Modifier.padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            row.forEach { (l, id, ic) -> val p = Practices.of(id)
                Column(Modifier.weight(1f).card(16.dp).clickable { go(id) }.padding(14.dp)) {
                    Icon(ic, Color(p.el.color), 22.dp); Text(l, style = T.bodyB, modifier = Modifier.padding(top = 6.dp)); Text(p.name, style = T.small, maxLines = 1, overflow = TextOverflow.Ellipsis) } }
        } }
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(C.card2).clickable { nav.quick = false; nav.push(Screen.Entry(0)) }.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.BOOK, C.acc, 20.dp); Spacer(Modifier.width(12.dp)); Text(tr("Записать, что чувствую", "Write down what I feel"), style = T.body, modifier = Modifier.weight(1f)) }
        Spacer(Modifier.height(10.dp))
        d.real.firstOrNull()?.let { last -> Practices.find(last.practice)?.takeIf { it.id != "sos" }?.let { p ->
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(C.card2).clickable { go(p.id) }.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.SWAP, C.mute, 20.dp); Spacer(Modifier.width(12.dp)); Text(tr("Как в прошлый раз: ", "Same as last time: ") + p.name.lowercase(), style = T.body, modifier = Modifier.weight(1f)) } } }
        Spacer(Modifier.height(10.dp))
        Btn(tr("Штормит – помогите", "Rough sea – help"), Modifier.fillMaxWidth(), color = C.bad) { nav.quick = false; launch(ctx, d, nav, Practices.SOS.id) }
    }
}

@Composable
fun CoursePage(id: String, d: Data, nav: Nav) {
    val co = Courses.byId[id] ?: run { nav.pop(); return }
    val c = LocalContext.current.container
    val on = d.s.course == id
    Page(co.name, { nav.pop() }) {
        app.puffling.ui.components.Hint("course", d.s.seen, app.puffling.ui.components.Icons.BOOK, tr("Курс", "A course"), tr("Одна практика в день по порядку. Пропущенный день не сгорает – курс подождёт.", "One practice a day in order. A missed day is not lost – the course waits."), Modifier.padding(bottom = 10.dp))
        Text(co.about, style = T.body)
        Text(tr("Один день – одна практика. Пропустили день – курс просто подождёт.", "One day, one practice. Missed a day – the course just waits."), style = T.small, modifier = Modifier.padding(top = 6.dp))
        Section(count(co.days.size, "день", "дня", "дней", "day", "days"))
        Column(Modifier.fillMaxWidth().card(18.dp)) {
            co.days.forEachIndexed { i, pid -> val p = Practices.of(pid)
                val done = on && i < d.s.courseDay; val now = on && i == d.s.courseDay
                Row(Modifier.fillMaxWidth().clickable { nav.push(Screen.Begin(p.id, if (now && d.s.courseLast != today()) "${co.id}:$i" else "")) }.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(30.dp).clip(CircleShape).background(if (done) Color(co.color) else if (now) Color(co.color).copy(alpha = .2f) else C.card2), contentAlignment = Alignment.Center) {
                        if (done) Icon(Icons.CHECK, Color.White, 15.dp) else Text("${i + 1}", style = T.smallB) }
                    Column(Modifier.weight(1f).padding(horizontal = 12.dp)) { Text(p.name, style = T.bodyB); Text(lengthOf(p), style = T.small) }
                    if (now) Text(if (d.s.courseLast == today()) tr("завтра", "tomorrow") else tr("сегодня", "today"), style = T.smallB.copy(color = Color(co.color)))
                }
            }
        }
        Spacer(Modifier.height(18.dp))
        if (!on) Btn(if (d.s.course.isEmpty()) tr("Начать курс", "Take this course") else tr("Сменить курс на этот", "Switch to this course"), Modifier.fillMaxWidth(), color = Color(co.color)) {
            c.write { settings.update { it.copy(course = id, courseDay = 0, courseLast = 0) } } }
        else Ghost(tr("Оставить курс", "Leave the course"), Modifier.fillMaxWidth()) { c.write { settings.update { it.copy(course = "", courseDay = 0) } } }
    }
}

/** Building an own practice. */
@Composable
fun OwnPage(id: Long, kind0: String, d: Data, nav: Nav) {
    val c = LocalContext.current.container
    val old = d.own.firstOrNull { it.id == id }
    var name by remember { mutableStateOf(old?.name ?: "") }
    val kind = Kind.of(old?.kind ?: kind0)
    var el by remember { mutableStateOf(Element.of(old?.el ?: when (kind) { Kind.SIT -> "still"; Kind.GUIDED -> "earth"; Kind.AFFIRM -> "heart"; Kind.WIMHOF -> "deep"; else -> "air" })) }
    var about by remember { mutableStateOf(old?.about ?: "") }
    var minutes by remember { mutableIntStateOf(old?.minutes ?: 5) }
    val phases = remember { mutableStateListOf<Phase>().apply { addAll(Practices.parse(old?.pattern ?: "i4,h2,o6")) } }
    var ramp by remember { mutableStateOf((old?.ramp ?: 0.0) > 0) }
    val steps = remember { mutableStateListOf<Pair<String, Int>>().apply {
        addAll((old?.steps ?: "").lines().filter { it.isNotBlank() }.map { l -> l.substringAfter('|').trim() to (l.substringBefore('|').trim().toIntOrNull() ?: 60) }); if (isEmpty()) add("" to 60) } }
    var bell by remember { mutableIntStateOf(old?.bell ?: 0) }
    var rounds by remember { mutableIntStateOf(old?.rounds ?: 3) }
    var breaths by remember { mutableIntStateOf(old?.breaths ?: 30) }
    var hold by remember { mutableIntStateOf(old?.hold ?: 60) }
    var deck by remember { mutableStateOf(old?.deck ?: "own") }
    val ok = name.isNotBlank() && when (kind) { Kind.BREATH -> phases.any { it.ph == Ph.IN } && phases.any { it.ph == Ph.OUT }; Kind.GUIDED -> steps.any { it.first.isNotBlank() }; else -> true }
    Page(if (old == null) tr("Своя практика", "Your own practice") else old.name, { nav.pop() }) {
        app.puffling.ui.components.Hint("own", d.s.seen, app.puffling.ui.components.Icons.EDIT, tr("Своя практика", "Your own practice"), tr("Дайте название и соберите её: фазы дыхания, колокольчики или шаги своими словами. Она появится в своей стихии.", "Name it and build it: breath phases, bells or steps in your own words. It shows up in its family."), Modifier.padding(bottom = 10.dp))
        Text(kind.label, style = T.sec.copy(color = C.acc))
        Field(name, { name = it }, tr("Название", "Name"), Modifier.fillMaxWidth().padding(top = 8.dp), style = T.h3)
        Field(about, { about = it }, tr("Пара слов о ней (необязательно)", "A few words about it (optional)"), Modifier.fillMaxWidth().padding(top = 8.dp), single = false)
        Section(tr("Стихия", "Family"))
        FlowChips(Element.entries.map { it.id to it.label }, el.id) { el = Element.of(it) }
        when (kind) {
            Kind.BREATH -> {
                Section(tr("Один круг дыхания", "One round of breath"))
                phases.forEachIndexed { i, ph ->
                    Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Row(Modifier.weight(1f).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf(Ph.IN, Ph.HOLD, Ph.OUT, Ph.EMPTY, Ph.TOP).forEach { p -> Chip(p.label, ph.ph == p, color = phColor(p)) { phases[i] = ph.copy(ph = p) } } }
                        Stepper(ph.secs, { phases[i] = ph.copy(secs = it.coerceIn(.5, 30.0)) })
                        Box(Modifier.size(32.dp).clip(CircleShape).clickable { if (phases.size > 1) phases.removeAt(i) }, contentAlignment = Alignment.Center) { Icon(Icons.CLOSE, C.mute, 16.dp) }
                    }
                }
                Text(tr("+ фаза", "+ phase"), style = T.smallB.copy(color = C.acc), modifier = Modifier.clickable { phases += Phase(Ph.OUT, 4.0) }.padding(vertical = 6.dp))
                val cyc = phases.sumOf { it.secs }
                Text(tr("Круг ${fmt1(cyc)} с · ${fmt1(60 / cyc.coerceAtLeast(.5))} вдохов в минуту", "A round of ${fmt1(cyc)} s · ${fmt1(60 / cyc.coerceAtLeast(.5))} breaths a minute"), style = T.small)
                Row(Modifier.padding(top = 8.dp).clickable { ramp = !ramp }, verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(22.dp).clip(CircleShape).background(if (ramp) C.acc else C.track), contentAlignment = Alignment.Center) { if (ramp) Icon(Icons.CHECK, Color.White, 13.dp) }
                    Text(tr("  Выдох понемногу удлиняется", "  The out-breath slowly grows"), style = T.body) }
                MinutesRow(minutes) { minutes = it }
            }
            Kind.SIT -> { MinutesRow(minutes) { minutes = it }
                Section(tr("Колокольчик внутри", "A bell during it"))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { listOf(0, 1, 2, 3, 5, 10).forEach { b -> Chip(if (b == 0) tr("нет", "none") else tr("каждые $b", "every $b"), bell == b) { bell = b } } } }
            Kind.GUIDED -> {
                Section(tr("Шаги", "Steps"))
                steps.forEachIndexed { i, (t, s) ->
                    Column(Modifier.fillMaxWidth().padding(bottom = 10.dp).card(14.dp).padding(10.dp)) {
                        Field(t, { steps[i] = it to s }, tr("Что делать на этом шаге", "What to do in this step"), Modifier.fillMaxWidth(), single = false)
                        Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(tr("секунд: ", "seconds: "), style = T.small); Stepper(s.toDouble(), { steps[i] = t to it.toInt().coerceIn(5, 1800) }, step = 10.0)
                            Spacer(Modifier.weight(1f)); if (steps.size > 1) Text(tr("убрать", "remove"), style = T.smallB.copy(color = C.bad), modifier = Modifier.clickable { steps.removeAt(i) }) }
                    }
                }
                Text(tr("+ шаг", "+ step"), style = T.smallB.copy(color = C.acc), modifier = Modifier.clickable { steps += "" to 60 }.padding(vertical = 6.dp))
                Text(tr("Всего: ", "In all: ") + mins(steps.sumOf { it.second }), style = T.small)
            }
            Kind.AFFIRM -> { Section(tr("Колода", "Deck"))
                FlowChips(listOf("own" to tr("Мои слова", "My words"), "fav" to tr("Любимые", "Favourites"), "mix" to tr("Микс дня", "Today's mix")) + Affirmations.themes.map { it.id to it.name }, deck) { deck = it }
                Text(tr("Свои слова добавляются в «Мои слова».", "Your own words are added under «My words»."), style = T.small.copy(color = C.acc), modifier = Modifier.padding(top = 8.dp).clickable { nav.push(Screen.Affirmations) }) }
            Kind.WIMHOF -> { Section(tr("Раунды", "Rounds")); Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { (1..6).forEach { n -> Chip("$n", rounds == n) { rounds = n } } }
                Section(tr("Вдохов в раунде", "Breaths a round")); Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { listOf(15, 20, 25, 30, 35, 40).forEach { n -> Chip("$n", breaths == n) { breaths = n } } }
                Section(tr("Цель задержки в первом раунде", "First round's hold target"))
                Row(verticalAlignment = Alignment.CenterVertically) { Stepper(hold.toDouble(), { hold = it.toInt().coerceIn(15, 300) }, step = 15.0); Text("  " + clock(hold), style = T.body) }
                Text(tr("Дальше каждый раунд на 30 секунд дольше.", "Each next round is 30 seconds longer."), style = T.small) }
        }
        Spacer(Modifier.height(20.dp))
        Btn(tr("Сохранить", "Save"), Modifier.fillMaxWidth(), enabled = ok) {
            val p = OwnPractice(id = old?.id ?: 0, name = name.trim(), kind = kind.code, el = el.id, about = about.trim(), minutes = minutes, pattern = Practices.code(phases),
                ramp = if (ramp) .5 else 0.0, steps = steps.filter { it.first.isNotBlank() }.joinToString("\n") { "${it.second}|${it.first.trim().replace('\n', ' ')}" },
                rounds = rounds, breaths = breaths, bell = bell, hold = hold, deck = deck, created = old?.created ?: 0)
            c.write { saveOwn(p); say(tr("Практика сохранена", "Practice saved")) }; nav.pop()
        }
        if (old != null) Text(tr("Убрать практику", "Remove the practice"), style = T.smallB.copy(color = C.bad), modifier = Modifier.padding(top = 16.dp).clickable {
            c.write { dao.update(old.copy(archived = true)); loadOwn() }; nav.stack.removeAll { it is Screen.OwnEdit || (it is Screen.Begin && it.practice == "u${old.id}") } })
    }
}

@Composable
private fun MinutesRow(m: Int, on: (Int) -> Unit) {
    Section(tr("Минут обычно", "Minutes usually"))
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) { listOf(1, 2, 3, 5, 10, 15, 20, 30).forEach { v -> Chip("$v", m == v, Modifier.weight(1f)) { on(v) } } }
}

@Composable
private fun Stepper(v: Double, on: (Double) -> Unit, step: Double = .5) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(30.dp).clip(CircleShape).background(C.card2).clickable { on(v - step) }, contentAlignment = Alignment.Center) { Text("−", style = T.bodyB) }
        Text(fmt1(v), style = T.numS, modifier = Modifier.width(40.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Box(Modifier.size(30.dp).clip(CircleShape).background(C.card2).clickable { on(v + step) }, contentAlignment = Alignment.Center) { Text("+", style = T.bodyB) }
    }
}

/** The control pause: breathe out normally, hold until the first wish to breathe. */
@Composable
fun BoltPage(d: Data, nav: Nav) {
    val c = LocalContext.current.container
    var state by remember { mutableIntStateOf(0) }  // 0 ready, 1 holding, 2 result
    var start by remember { mutableStateOf(0L) }
    var secs by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(state) { while (state == 1) { secs = ((System.currentTimeMillis() - start) / 1000).toInt(); delay(100) } }
    val past = d.sessions.filter { it.practice == "bolt" }.sortedBy { it.start }
    Page(tr("Тест задержки", "Breath-hold test"), { nav.pop() }) {
        app.puffling.ui.components.Hint("bolt", d.s.seen, app.puffling.ui.components.Icons.TIMER, tr("Тест задержки", "Breath-hold test"), tr("Раз в неделю, в покое. Не терпите – останавливайтесь при первом желании вдохнуть.", "Once a week, at rest. Do not push – stop at the first wish to breathe."), Modifier.padding(bottom = 10.dp))
        Text(tr("Контрольная пауза: сколько секунд вы спокойно обходитесь без вдоха после обычного выдоха. Чем дольше, тем ровнее дыхание в жизни. Делайте утром или в покое, раз в неделю – и смотрите, как растёт.",
            "The control pause: how many seconds you calmly go without breathing after a normal breath out. The longer, the calmer your everyday breathing. Do it in the morning or at rest, once a week – and watch it grow."), style = T.body)
        Column(Modifier.padding(top = 18.dp).fillMaxWidth().card(22.dp).padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            when (state) {
                0 -> { Text(tr("Обычный вдох. Обычный выдох. Зажмите нос – и нажмите «Старт».", "A normal breath in. A normal breath out. Pinch your nose – and tap «Start»."), style = T.bodyB, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    Btn(tr("Старт", "Start"), Modifier.padding(top = 16.dp).fillMaxWidth()) { start = System.currentTimeMillis(); secs = 0; state = 1 } }
                1 -> { Text("$secs", style = T.big.copy(fontSize = androidx.compose.ui.unit.TextUnit(64f, androidx.compose.ui.unit.TextUnitType.Sp)))
                    Text(tr("Нажмите при первом желании вдохнуть – не терпите", "Tap at the first wish to breathe – don't push"), style = T.small, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    Btn(tr("Стоп", "Stop"), Modifier.padding(top = 16.dp).fillMaxWidth(), color = C.ok) { secs = ((System.currentTimeMillis() - start) / 1000).toInt(); state = 2; scope.launch { c.bolt(secs) } } }
                else -> { Text(tr("$secs с", "$secs s"), style = T.big)
                    Text(boltWord(secs), style = T.body, textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.padding(top = 6.dp))
                    Ghost(tr("Ещё раз через неделю", "Again in a week"), Modifier.padding(top = 16.dp).fillMaxWidth()) { nav.pop() } }
            }
        }
        if (past.size >= 2) { Section(tr("Как менялось", "Over time")); Line(past.map { dayOf(it.start).toDouble() to (it.holdList().firstOrNull() ?: it.secs).toDouble() }) }
        if (past.isNotEmpty()) { Section(tr("Записи", "Records"))
            past.reversed().take(10).forEach { s -> Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) { Text(fmtDayShort(dayOf(s.start)), style = T.body, modifier = Modifier.weight(1f)); Text(tr("${s.holdList().firstOrNull() ?: s.secs} с", "${s.holdList().firstOrNull() ?: s.secs} s"), style = T.numS) } } }
    }
}


fun boltWord(s: Int) = when {
    s < 10 -> tr("Дыхание пока частое и поверхностное. Медленные практики с длинным выдохом помогут.", "Your breathing is still quick and shallow. Slow practices with a long exhale will help.")
    s < 20 -> tr("Есть куда расти. Ровное дыхание каждый день – и через месяц будет заметно.", "Room to grow. Coherent breathing every day – and in a month it will show.")
    s < 30 -> tr("Неплохо: дыхание спокойное.", "Not bad: your breathing is calm.")
    s < 40 -> tr("Хорошо! Дыхание ровное и экономное.", "Good! Your breathing is even and economical.")
    else -> tr("Отлично – дыхание очень спокойное.", "Excellent – very calm breathing.")
}
