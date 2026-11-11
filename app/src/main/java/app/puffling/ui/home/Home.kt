package app.puffling.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.puffling.container
import app.puffling.domain.Affirmations
import app.puffling.domain.Courses
import app.puffling.domain.Pets
import app.puffling.domain.Practice
import app.puffling.domain.Practices
import app.puffling.domain.Quests
import app.puffling.domain.Stage
import app.puffling.domain.Week
import app.puffling.domain.dayOf
import app.puffling.domain.dayShort
import app.puffling.domain.fmt
import app.puffling.domain.fmt1
import app.puffling.domain.stage
import app.puffling.domain.sp
import app.puffling.domain.today
import app.puffling.domain.tr
import app.puffling.ui.Data
import app.puffling.ui.Nav
import app.puffling.ui.Screen
import app.puffling.domain.feelingIds
import app.puffling.domain.mainMood
import app.puffling.ui.art.Pose
import app.puffling.ui.art.Sky
import app.puffling.ui.art.TeaHouse
import app.puffling.ui.components.Btn
import app.puffling.ui.components.Icon
import app.puffling.ui.components.Icons
import app.puffling.ui.components.Meter
import app.puffling.ui.components.Popup
import app.puffling.ui.components.Sheet
import app.puffling.ui.components.card
import app.puffling.ui.practice.launch
import app.puffling.ui.practice.lengthOf
import app.puffling.ui.share.CardKind
import app.puffling.ui.share.ShareReq
import app.puffling.ui.theme.C
import app.puffling.ui.theme.T
import java.time.LocalTime

/**
 * The home screen fits one screen: the tea house with the leader on it, the week's tide, what to do now,
 * the words for today, and three small tiles that open their details.
 */
@Composable
fun HomeScreen(d: Data, nav: Nav) {
    val ctx = LocalContext.current
    val hour = LocalTime.now().hour
    val asleep = hour !in 7..22
    var sheet by remember { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 14.dp).padding(bottom = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth().padding(start = 6.dp, top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Puffling", style = T.title)
                Text(if (d.real.any { dayOf(it.start) == today() }) tr("Сегодня вы уже дышали вместе", "You've breathed together today") else greeting(hour), style = T.small)
            }
            Row(Modifier.clip(RoundedCornerShape(50)).background(C.goldSoft).clickable { nav.push(Screen.Shop) }.padding(horizontal = 12.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.SHELL, C.gold, 18.dp); Spacer(Modifier.width(6.dp)); Text(fmt(d.s.shells), style = T.numS.copy(color = C.gold))
            }
            Box(Modifier.size(44.dp).clip(CircleShape).clickable { nav.push(Screen.Settings) }, contentAlignment = Alignment.Center) { Icon(Icons.GEAR, C.mute, 22.dp) }
        }
        app.puffling.ui.components.Hint("home", d.s.seen, app.puffling.ui.components.Icons.WIND, tr("Ваша чайная на скале", "Your tea house on the cliff"), tr("Круглая кнопка внизу – начать практику. Питомцы едят спокойствие и растут ночью, а море в окне поднимается к приливу с каждой минутой за неделю.", "The round button below starts a practice. Pets eat calm and grow at night, and the sea rises towards high tide with every minute this week."), Modifier.padding(bottom = 10.dp))
        // the scene, with the leader's name and growth over its floor
        Box(Modifier.card(22.dp).clickable { nav.push(Screen.Shop) }) {
            val sky = remember(d.s.outside, d.s.lat, System.currentTimeMillis() / 300_000) { Sky.of(d.s) }
            TeaHouse(d.team, d.s.items - d.s.hidden, sky, d.tide, pose = if (asleep) Pose.SLEEP else Pose.IDLE)
            Row(Modifier.align(Alignment.TopStart).padding(10.dp).clip(RoundedCornerShape(50)).background(Color(0x99203040)).clickable { launch(ctx, d, nav, Practices.SOS.id) }
                .padding(horizontal = 12.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.WAVE, Color.White, 16.dp); Spacer(Modifier.width(6.dp)); Text(tr("Штормит", "Rough sea"), style = T.smallB.copy(color = Color.White))
            }
            Box(Modifier.align(Alignment.TopEnd).padding(10.dp).size(36.dp).clip(CircleShape).background(Color(0x66000000)).clickable { nav.share = ShareReq(CardKind.HOUSE) },
                contentAlignment = Alignment.Center) { Icon(Icons.SHARE, Color.White, 18.dp) }
            d.leader?.let { p -> val st = p.stage()
                Column(Modifier.align(Alignment.TopEnd).padding(top = 10.dp, end = 54.dp).width(132.dp).clip(RoundedCornerShape(14.dp)).background(Color(0xB3182430))
                    .clickable { nav.push(Screen.PetInfo(p.id)) }.padding(horizontal = 10.dp, vertical = 7.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(p.name, style = T.smallB.copy(color = Color.White), modifier = Modifier.weight(1f), maxLines = 1)
                        if (p.belly >= 1) Text("+" + fmt1(p.belly), style = T.tiny.copy(color = Color(0xFF9FD8EE)))
                    }
                    Text(st.label + if (p.belly >= 1) tr(" · вырастет ночью", " · grows tonight") else "", style = T.tiny.copy(color = Color.White.copy(alpha = .7f)), maxLines = 1)
                    Meter(if (st == Stage.EGG) (p.growth / Pets.HATCH).toFloat() else Stage.progress(p), Modifier.padding(top = 4.dp), color = Color(0xFF9FD8EE), h = 4.dp, track = Color(0x33FFFFFF))
                }
            }
        }
        BottleBanner(d)
        TideCard(d)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            NowTile(d, nav, Modifier.weight(1f))
            WordsTile(d, nav, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            val answered = d.bellsToday.count { it.answered > 0 }
            SmallTile(Icons.BELL, C.gold, tr("Колокольчик", "Bell"), if (!d.s.bells) tr("выключен", "off") else if (d.bellsToday.isEmpty()) tr("тихо", "quiet") else "$answered / ${d.bellsToday.size}", Modifier.weight(1f)) { sheet = "bell" }
            val todays = d.entries.firstOrNull { dayOf(it.at) == today() }
            val feel = todays?.feelingIds()?.firstOrNull()?.let { app.puffling.domain.Diary.feeling(it)?.name }
            SmallTile(Icons.BOOK, todays?.mainMood()?.let { Color(it.color) } ?: C.ok, tr("Дневник", "Diary"), feel ?: tr("как вы?", "how are you?"), Modifier.weight(1f)) { nav.push(Screen.Entry(0)) }
            val qs = (if (d.s.questWeek == d.monday) d.s.quests else Quests.deal(d.monday))
            SmallTile(Icons.TARGET, C.acc, tr("Задания", "Quests"), "${qs.count { it in d.s.questsDone && d.s.questWeek == d.monday }} / ${qs.size}", Modifier.weight(1f)) { sheet = "quests" }
        }
    }
    when (sheet) {
        "bell" -> Sheet({ sheet = null }) { BellSheet(d, nav) { sheet = null } }
        "quests" -> Sheet({ sheet = null }) { QuestSheet(d) }
    }
}

private fun greeting(h: Int) = when (h) { in 5..11 -> tr("Доброе утро", "Good morning"); in 12..17 -> tr("Добрый день", "Good afternoon"); in 18..22 -> tr("Добрый вечер", "Good evening"); else -> tr("Доброй ночи", "Good night") }

/** A bottle washed back: your own words from weeks ago. One line until opened. */
@Composable
private fun BottleBanner(d: Data) {
    val b = d.ashore.lastOrNull() ?: return
    val c = LocalContext.current.container
    var open by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(C.accSoft).clickable { open = true }.padding(horizontal = 14.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.BOTTLE, C.acc, 20.dp)
        Text(tr("Бутылку прибило к берегу", "A bottle washed ashore"), style = T.bodyB, modifier = Modifier.weight(1f).padding(horizontal = 10.dp))
        Text(tr("открыть", "open"), style = T.smallB.copy(color = C.acc))
    }
    if (open) Popup({ open = false; c.write { dao.update(b.copy(opened = true)) } }) {
        Text(tr("Вы сказали это ${app.puffling.domain.fmtDay(b.sent)}:", "You said this on ${app.puffling.domain.fmtDay(b.sent)}:"), style = T.small)
        Text("«${b.text}»", style = T.h2.copy(fontStyle = FontStyle.Italic), modifier = Modifier.padding(vertical = 14.dp))
        Btn(tr("Отпустить обратно в море", "Let it back to sea"), Modifier.fillMaxWidth()) { open = false; c.write { dao.update(b.copy(opened = true)) } }
    }
}

/** The week's tide in one card: minutes against the goal and the seven days as a small wave. */
@Composable
private fun TideCard(d: Data) {
    val days = (0..6).map { i -> d.week.filter { dayOf(it.start) == d.monday + i && it.practice != "bolt" }.sumOf { it.secs } / 60 }
    Column(Modifier.fillMaxWidth().card(18.dp).padding(horizontal = 14.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.WAVE, C.acc, 18.dp)
            Text(if (d.tide >= 1f) tr("Прилив!", "High tide!") else tr("Прилив", "Tide"), style = T.bodyB, modifier = Modifier.padding(start = 8.dp).weight(1f))
            Text("${d.weekMinutes.toInt()}", style = T.numS.copy(color = C.acc)); Text(tr(" / ${d.s.weekGoal} мин", " / ${d.s.weekGoal} min"), style = T.small)
        }
        Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.Bottom) {
            val max = maxOf(15, days.maxOrNull() ?: 0)
            days.forEachIndexed { i, m -> val day = d.monday + i
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.fillMaxWidth().height(28.dp), contentAlignment = Alignment.BottomCenter) {
                        Box(Modifier.fillMaxWidth().height((4 + 24f * m / max).dp).clip(RoundedCornerShape(5.dp)).background(if (m > 0) C.acc else if (day == today()) C.accSoft else C.track)) }
                    Text(dayShort(i), style = T.tiny.copy(color = if (day == today()) C.ink else C.mute), modifier = Modifier.padding(top = 3.dp))
                }
            }
        }
    }
}

/** What to do now: today's course day, or a practice for the hour. */
@Composable
private fun NowTile(d: Data, nav: Nav, modifier: Modifier) {
    val co = Courses.byId[d.s.course]
    val courseDay = co != null && d.s.courseDay < co.days.size && d.s.courseLast != today()
    val p: Practice = if (courseDay) Practices.of(co!!.days[d.s.courseDay]) else Practices.of(when (LocalTime.now().hour) { in 5..10 -> "box"; in 11..17 -> "sigh"; in 18..20 -> "coherent"; else -> "478" })
    val col = Color(if (courseDay) co!!.color else p.el.color)
    Column(modifier.height(132.dp).clip(RoundedCornerShape(20.dp)).background(Brush.verticalGradient(listOf(col.copy(alpha = .22f), col.copy(alpha = .08f))))
        .clickable { nav.push(Screen.Begin(p.id, if (courseDay) "${co!!.id}:${d.s.courseDay}" else "")) }.padding(14.dp)) {
        Text(if (courseDay) tr("ДЕНЬ ${d.s.courseDay + 1} КУРСА", "COURSE DAY ${d.s.courseDay + 1}") else tr("СЕЙЧАС", "RIGHT NOW"), style = T.sec.copy(color = col))
        Text(p.name, style = T.h3, modifier = Modifier.padding(top = 4.dp), maxLines = 2, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.weight(1f))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(lengthOf(p), style = T.small, modifier = Modifier.weight(1f))
            Box(Modifier.size(34.dp).clip(CircleShape).background(col), contentAlignment = Alignment.Center) { Icon(Icons.PLAY, Color.White, 15.dp) }
        }
    }
}

/** The words for today, to keep or to say. */
@Composable
private fun WordsTile(d: Data, nav: Nav, modifier: Modifier) {
    val c = LocalContext.current.container
    val a = remember(today(), d.affs, d.s.favs, d.s.fem) { Affirmations.ofDay(today(), d.affs, d.s.favs) }
    val fav = a.id in d.s.favs
    Column(modifier.height(132.dp).card(20.dp).clickable { nav.push(Screen.Begin("affirm")) }.padding(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(tr("СЛОВА ДНЯ", "TODAY'S WORDS"), style = T.sec, modifier = Modifier.weight(1f))
            Box(Modifier.size(26.dp).clip(CircleShape).clickable { c.write { settings.update { it.copy(favs = if (fav) it.favs - a.id else it.favs + a.id) } } }, contentAlignment = Alignment.Center) {
                Icon(Icons.HEART, if (fav) C.bad else C.line, 17.dp) }
        }
        Text(a.text, style = T.body.copy(fontStyle = FontStyle.Italic), modifier = Modifier.padding(top = 4.dp), maxLines = 4, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun SmallTile(icon: Icons, tint: Color, title: String, value: String, modifier: Modifier, onClick: () -> Unit) {
    Column(modifier.card(18.dp).clickable(onClick = onClick).padding(12.dp)) {
        Icon(icon, tint, 20.dp)
        Text(value, style = T.bodyB, modifier = Modifier.padding(top = 6.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(title, style = T.tiny, maxLines = 1)
    }
}

@Composable
private fun ColumnScope.BellSheet(d: Data, nav: Nav, close: () -> Unit) {
    Text(tr("Колокольчик", "The bell"), style = T.h2)
    if (!d.s.bells) {
        Text(tr("Несколько раз в день тихо звонит: остановитесь на три вдоха, и на тропе зажжётся фонарик.", "A few times a day it rings softly: stop for three breaths and a lantern lights up on the path."), style = T.body, modifier = Modifier.padding(top = 6.dp))
        Btn(tr("Включить", "Turn on"), Modifier.fillMaxWidth().padding(top = 14.dp)) { close(); nav.push(Screen.Settings) }
        return
    }
    val next = remember(d.s, d.bells.size) { app.puffling.work.Bells.next(d.s) }
    Text(next?.let { tr("Следующий – около ", "Next – around ") + java.time.format.DateTimeFormatter.ofPattern("H:mm").format(java.time.Instant.ofEpochMilli(it).atZone(java.time.ZoneId.systemDefault())) } ?: "", style = T.small, modifier = Modifier.padding(top = 4.dp))
    if (d.bellsToday.isEmpty()) Text(tr("Сегодня ещё не звонил.", "It hasn't rung today yet."), style = T.body, modifier = Modifier.padding(top = 12.dp))
    else Row(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) { d.bellsToday.sortedBy { it.at }.forEach { Lantern(it.answered > 0, 30.dp) } }
    Text(tr("Горящий фонарик – колокольчик, на который вы ответили.", "A lit lantern is a bell you answered."), style = T.tiny, modifier = Modifier.padding(top = 10.dp))
    Btn(tr("Три вдоха сейчас", "Three breaths now"), Modifier.fillMaxWidth().padding(top = 14.dp)) { close(); nav.push(Screen.Begin("bell")) }
    Text(tr("Настроить", "Settings"), style = T.smallB.copy(color = C.acc), modifier = Modifier.padding(top = 12.dp).clickable { close(); nav.push(Screen.Settings) })
}

@Composable
private fun Lantern(lit: Boolean, w: Dp) {
    Canvas(Modifier.size(w, w * 1.4f)) {
        val x = size.width; val y = size.height
        drawLine(Color(0xFF6B5136), Offset(x / 2, 0f), Offset(x / 2, y * .18f), x * .06f)
        if (lit) drawCircle(Brush.radialGradient(listOf(Color(0x88FFC24D), Color.Transparent), Offset(x / 2, y * .55f), x), x, Offset(x / 2, y * .55f))
        drawRoundRect(if (lit) Color(0xFFFFB648) else Color(0xFFB9B2A6), Offset(x * .15f, y * .18f), androidx.compose.ui.geometry.Size(x * .7f, y * .72f), CornerRadius(x * .3f))
        drawRect(Color(0xFF6B5136), Offset(x * .25f, y * .14f), androidx.compose.ui.geometry.Size(x * .5f, y * .07f))
        drawRect(Color(0xFF6B5136), Offset(x * .25f, y * .88f), androidx.compose.ui.geometry.Size(x * .5f, y * .07f))
    }
}

@Composable
private fun ColumnScope.QuestSheet(d: Data) {
    val mon = d.monday
    val wk = remember(d.sessions, d.bells) { Week(d.week, d.bells.filter { dayOf(it.at) >= mon }, d.sessions.filter { dayOf(it.start) < mon }, mon, d.entries.count { dayOf(it.at) >= mon }) }
    val qs = (if (d.s.questWeek == mon) d.s.quests else Quests.deal(mon)).mapNotNull { Quests.byId[it] }
    Text(tr("Задания недели", "This week's quests"), style = T.h2)
    Text(tr("Меняются каждый понедельник", "They change every Monday"), style = T.small, modifier = Modifier.padding(top = 2.dp, bottom = 8.dp))
    qs.forEach { q ->
        val ok = q.id in d.s.questsDone && d.s.questWeek == mon
        Row(Modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(26.dp).clip(CircleShape).background(if (ok) C.ok else C.card2), contentAlignment = Alignment.Center) { if (ok) Icon(Icons.CHECK, Color.White, 15.dp) }
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(q.name, style = T.body.copy(color = if (ok) C.mute else C.ink))
                if (!ok) Meter((q.progress(wk).toFloat() / q.need).coerceIn(0f, 1f), Modifier.padding(top = 5.dp), h = 5.dp)
            }
            Icon(Icons.SHELL, C.gold, 14.dp); Text(" ${q.shells}", style = T.smallB.copy(color = C.gold))
        }
    }
}
