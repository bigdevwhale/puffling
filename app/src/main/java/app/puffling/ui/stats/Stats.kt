package app.puffling.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.mutableIntStateOf
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
import app.puffling.domain.stage
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.unit.sp
import app.puffling.container
import app.puffling.data.Session
import app.puffling.domain.Deep
import app.puffling.domain.Element
import app.puffling.domain.Span
import app.puffling.domain.Stats
import app.puffling.domain.Tides
import app.puffling.domain.clock
import app.puffling.domain.count
import app.puffling.domain.dayOf
import app.puffling.domain.depthOf
import app.puffling.domain.fmt
import app.puffling.domain.fmt1
import app.puffling.domain.fmtDayShort
import app.puffling.domain.fmtDayYear
import app.puffling.domain.hours
import app.puffling.domain.feelingIds
import app.puffling.domain.hourOf
import app.puffling.domain.mins
import app.puffling.domain.relDay
import app.puffling.domain.today
import app.puffling.domain.tr
import app.puffling.ui.Data
import app.puffling.ui.Nav
import app.puffling.ui.Screen
import app.puffling.ui.components.Bars
import app.puffling.ui.components.Btn
import app.puffling.ui.components.Empty
import app.puffling.ui.components.Fold
import app.puffling.ui.components.Ghost
import app.puffling.ui.components.Heat
import app.puffling.ui.components.Hint
import app.puffling.ui.components.Icon
import app.puffling.ui.components.Icons
import app.puffling.ui.components.Line
import app.puffling.ui.components.Meter
import app.puffling.ui.components.Page
import app.puffling.ui.components.Popup
import app.puffling.ui.components.Section
import app.puffling.ui.components.Segments
import app.puffling.ui.components.TileGrid
import app.puffling.ui.components.card
import app.puffling.ui.practice.elIcon
import app.puffling.ui.practice.feelColor
import app.puffling.ui.practice.feelWord
import app.puffling.ui.share.CardKind
import app.puffling.ui.share.ShareReq
import app.puffling.ui.theme.C
import app.puffling.ui.theme.T

@Composable
fun StatsScreen(d: Data, nav: Nav, header: Boolean = true) {
    var kind by androidx.compose.runtime.saveable.rememberSaveable { mutableIntStateOf(Span.WEEK) }
    var span by remember(kind) { mutableStateOf(Span.of(kind)) }
    val st = remember(d.sessions, d.bells, span) { Stats(d.sessions, d.bells, span) }
    val tides = remember(d.sessions, d.s.weekGoal) { Tides(d.sessions, d.s.weekGoal) }
    var all by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 30.dp)) {
        if (header) Text(tr("Летопись", "Journal"), style = T.title, modifier = Modifier.padding(top = 6.dp, start = 4.dp))
        Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Segments(listOf(tr("Неделя", "Week"), tr("Месяц", "Month"), tr("Год", "Year")), kind, Modifier.weight(1f)) { kind = it }
            Box(Modifier.padding(start = 6.dp).size(40.dp).clip(CircleShape).clickable { nav.share = ShareReq(CardKind.WEEK) }, contentAlignment = Alignment.Center) { Icon(Icons.SHARE, C.mute, 20.dp) }
        }
        app.puffling.ui.components.Hint("stats", d.s.seen, app.puffling.ui.components.Icons.STATS, tr("Итоги", "Summary"), tr("Листайте стрелками недели, месяцы и годы. Отмечайте чувства в дневнике – появится карта настроения.", "Arrows move through weeks, months and years. Note feelings in the diary and a mood map appears."), Modifier.padding(top = 10.dp))
        // the period: the number, one line, the bars
        Column(Modifier.padding(top = 12.dp).fillMaxWidth().card(22.dp).padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(32.dp).clip(CircleShape).clickable { span = span.prev() }, contentAlignment = Alignment.Center) { Icon(Icons.BACK, C.mute, 18.dp) }
                Text(span.label(), style = T.smallB.copy(color = C.mute), modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                Box(Modifier.size(32.dp).clip(CircleShape).clickable(enabled = !span.current) { span = span.next() }, contentAlignment = Alignment.Center) { Icon(Icons.NEXT, if (span.current) C.line else C.mute, 18.dp) }
            }
            Text(mins(st.secs), style = T.big, modifier = Modifier.fillMaxWidth().padding(top = 6.dp), textAlign = TextAlign.Center)
            Text(count(st.count, "практика", "практики", "практик", "practice", "practices") + " · " + count(st.days.size, "день", "дня", "дней", "day", "days"),
                style = T.small, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            Spacer(Modifier.height(14.dp))
            Bars(st.bars(d.first), color = C.acc, h = 100.dp)
        }
        // facts at a glance
        val fav = st.byPractice.firstOrNull()?.let { (id, n) -> (app.puffling.domain.Practices.find(id)?.name ?: st.real.first { it.practice == id }.name) to n }
        val total = d.sessions.filter { it.practice != "bolt" }.sumOf { it.secs }
        Spacer(Modifier.height(12.dp))
        TileGrid(listOf(
            Triple(count(tides.streak, "прилив", "прилива", "приливов", "tide", "tides"), tr("подряд · лучшее ${tides.best}", "in a row · best ${tides.best}"), null),
            Triple(if (st.longest > 0) mins(st.longest) else "–", tr("самая долгая практика", "the longest practice"), null),
            Triple(fav?.first ?: "–", if (fav != null) tr("любимая · ×${fav.second}", "favourite · ×${fav.second}") else tr("любимая", "favourite"), null),
            Triple(if (st.bellsRung.isEmpty()) "–" else "${st.bellsAnswered * 100 / st.bellsRung.size}%", tr("колокольчиков отвечено", "bells answered"), null),
            Triple("${st.shells}", tr("ракушек за период", "shells this period"), null),
            Triple(hours(total.toLong()), tr("всего вместе", "together in all"), null),
        ), icons = listOf(Icons.WAVE to C.acc, Icons.CLOCK to C.ok, Icons.STAR to C.gold, Icons.BELL to C.gold, Icons.SHELL to C.gold, Icons.LOTUS to C.ok))
        app.puffling.ui.diary.FeelingsCard(d, span)
        MoodMap(d, span)
        if (st.rated.isNotEmpty()) Card(tr("Что помогает", "What helps"), tr("тише в ${st.calmer} из ${st.rated.size}", "calmer ${st.calmer} of ${st.rated.size}")) {
            Text(tr("После практики в среднем на ${fmt1(st.drop)} ступени спокойнее", "After a practice ${fmt1(st.drop)} steps calmer on average"), style = T.small)
            st.helps.take(3).forEach { h -> Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(h.name, style = T.body, modifier = Modifier.weight(1f)); Text((if (h.drop >= 0) "−" else "+") + fmt1(kotlin.math.abs(h.drop)), style = T.bodyB.copy(color = if (h.drop > 0) C.ok else C.mute)) } }
        }
        if (st.byEl.isNotEmpty()) Card(tr("Стихии", "Families"), null) {
            val sum = st.byEl.values.sum().coerceAtLeast(1)
            Row(Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(50))) {
                Element.entries.filter { (st.byEl[it] ?: 0) > 0 }.forEach { el -> Box(Modifier.weight((st.byEl[el] ?: 0).toFloat()).fillMaxSize().background(Color(el.color))) } }
            androidx.compose.foundation.layout.FlowRow(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Element.entries.filter { (st.byEl[it] ?: 0) > 0 }.sortedByDescending { st.byEl[it] }.forEach { el -> Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(Color(el.color))); Text(" ${el.label} ${(st.byEl[el] ?: 0) * 100 / sum}%", style = T.small) } } }
        }
        if (st.holds.isNotEmpty()) Card(tr("Задержки", "Holds"), tr("лучшая ${clock(st.bestHold)} · ${depthOf(st.bestHold)} м", "best ${clock(st.bestHold)} · ${depthOf(st.bestHold)} m")) {
            Line(st.holds.map { it.first.toDouble() to it.second.toDouble() }, color = Color(Element.DEEP.color), h = 110.dp)
            Text(tr("В аквариуме ${d.s.finds.size} из ${Deep.all.size}", "In the aquarium ${d.s.finds.size} of ${Deep.all.size}"), style = T.smallB.copy(color = C.acc), modifier = Modifier.padding(top = 8.dp).clickable { nav.push(Screen.Aquarium) })
        }
        if (st.bolts.isNotEmpty()) Card(tr("Тест задержки", "Breath-hold test"), tr("последний ${st.bolts.last().second} с", "last ${st.bolts.last().second} s")) {
            if (st.bolts.size >= 2) Line(st.bolts.map { it.first.toDouble() to it.second.toDouble() }, color = C.ok, h = 100.dp)
            Text(tr("Пройти ещё раз", "Take it again"), style = T.smallB.copy(color = C.acc), modifier = Modifier.padding(top = 8.dp).clickable { nav.push(Screen.Bolt) })
        }
        if (st.bpms.isNotEmpty()) Card(tr("Вдохов в минуту", "Breaths a minute"), fmt1(st.bpms.last().second)) {
            Line(st.bpms.map { it.first.toDouble() to it.second }, color = C.acc, h = 100.dp)
            Text(tr("Телефон на животе. Чем реже, тем спокойнее.", "The phone on your belly. Fewer is calmer."), style = T.small, modifier = Modifier.padding(top = 6.dp))
        }
        if (st.count > 0) Card(tr("Когда вы практикуете", "When you practise"), null) {
            Bars((0 until 24).map { h -> (if (h % 6 == 0) "$h" else "") to st.hours[h] / 60.0 }, color = C.gold, h = 70.dp)
            val wd = IntArray(7).also { a -> st.real.forEach { a[(java.time.LocalDate.ofEpochDay(dayOf(it.start)).dayOfWeek.value - 1)] += it.secs } }
            Bars((0 until 7).map { app.puffling.domain.dayShort(it) to wd[it] / 60.0 }, color = C.ok, h = 70.dp, modifier = Modifier.padding(top = 14.dp))
        }
        if (st.gratitude.isNotEmpty()) Card(tr("Хорошие вещи", "Good things"), "${st.gratitude.size}") {
            st.gratitude.reversed().take(4).forEach { (day, l) -> Row(Modifier.padding(vertical = 3.dp)) {
                Text(fmtDayShort(day), style = T.tiny, modifier = Modifier.width(56.dp)); Text(l, style = T.body.copy(fontStyle = FontStyle.Italic)) } }
        }
        val list = st.inSpan.filter { it.practice != "bell" }
        if (list.isNotEmpty()) {
            Text(tr("ПРАКТИКИ", "PRACTICES"), style = T.sec, modifier = Modifier.padding(top = 18.dp, bottom = 8.dp, start = 4.dp))
            Column(Modifier.fillMaxWidth().card(22.dp)) {
                list.take(if (all) 80 else 5).forEachIndexed { i, x -> if (i > 0) Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(1.dp).background(C.line))
                    SessionRow(x) { nav.push(Screen.SessionView(x.id)) } }
            }
            if (list.size > 5 && !all) Text(tr("Все · ${list.size}", "All · ${list.size}"), style = T.smallB.copy(color = C.acc), modifier = Modifier.padding(top = 10.dp, start = 4.dp).clickable { all = true })
        } else Empty(tr("В этот период тихо. Самое время подышать.", "Quiet in this period. A good time to breathe."))
    }
}

/** A titled card with a small value on the right. */
@Composable
private fun Card(title: String, value: String?, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Column(Modifier.padding(top = 12.dp).fillMaxWidth().card(22.dp).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 10.dp)) {
            Text(title, style = T.h3, modifier = Modifier.weight(1f)); if (value != null) Text(value, style = T.small) }
        content()
    }
}

/** Each day of the span as a dot in the colour of what was felt most; grey when nothing was written. */
@Composable
private fun MoodMap(d: Data, span: Span) {
    val list = d.entries.filter { dayOf(it.at) in span.from..span.to }
    if (list.isEmpty() || span.kind == Span.YEAR) return
    val byDay = list.groupBy { dayOf(it.at) }
    Card(tr("Карта настроения", "Mood map"), null) {
        val days = (span.from..span.to).toList()
        days.chunked(7).forEach { week -> Row(Modifier.fillMaxWidth().padding(bottom = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            week.forEach { day -> val m = byDay[day]?.flatMap { it.feelingIds() }?.groupingBy { it.substringBefore(':') }?.eachCount()?.maxByOrNull { it.value }?.key
                Box(Modifier.size(30.dp).clip(CircleShape).background(m?.let { Color(app.puffling.domain.Diary.byMood[it]!!.color) } ?: C.track), contentAlignment = Alignment.Center) {
                    Text("${java.time.LocalDate.ofEpochDay(day).dayOfMonth}", style = T.tiny.copy(color = if (m != null) Color.White else C.mute)) } }
            repeat(7 - week.size) { Spacer(Modifier.size(30.dp)) } } }
    }
}

/** An entry: the name and when on the left, how long on the right. */
@Composable
private fun SessionRow(x: Session, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(Color(Element.of(x.el).color)))
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(if (x.practice == "bolt") tr("Тест задержки", "Breath-hold test") else x.name, style = T.body, maxLines = 1)
            Text(relDay(dayOf(x.start)), style = T.tiny)
        }
        Text(if (x.practice == "bolt") tr("${x.holdList().firstOrNull() ?: x.secs} с", "${x.holdList().firstOrNull() ?: x.secs} s") else mins(x.secs), style = T.small)
    }
}

@Composable
fun SessionPage(id: Long, d: Data, nav: Nav) {
    val c = LocalContext.current.container
    val x = d.sessions.firstOrNull { it.id == id } ?: run { if (d.sessions.isNotEmpty()) nav.pop(); return }
    var del by remember { mutableStateOf(false) }
    val el = Element.of(x.el)
    val col = Color(el.color)
    val bolt = x.practice == "bolt"
    val pet = d.pets.firstOrNull { it.species == x.leader } ?: d.leader
    Page("", { nav.pop() }, action = { Box(Modifier.size(44.dp).clip(CircleShape).clickable { nav.share = ShareReq(CardKind.SESSION, x.id) }, contentAlignment = Alignment.Center) { Icon(Icons.SHARE, C.ink, 20.dp) } }) {
        app.puffling.ui.components.Hint("session", d.s.seen, Icons.BOOK, tr("Одна практика", "One practice"), tr("Всё, что она дала: спокойствие команде, ракушки, задержки и ваши слова. «Ещё раз» запустит её снова.",
            "Everything it gave: calm for the team, shells, holds and your words. «Again» starts it once more."), Modifier.padding(bottom = 10.dp))
        // the hero: who practised, what and when
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(col.copy(alpha = .32f), col.copy(alpha = .06f))))
            .padding(vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            if (pet != null) app.puffling.ui.art.Creature(pet.species, maxOf(pet.stage(), app.puffling.domain.Stage.BABY), app.puffling.ui.art.Pose.PRACTICE, 150.dp, pet.shiny)
            Text(if (bolt) tr("Тест задержки", "Breath-hold test") else x.name, style = T.title.copy(fontSize = 24.sp), textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 16.dp))
            Text(el.label + " · " + fmtDayYear(dayOf(x.start)) + ", " + String.format("%d:%02d", hourOf(x.start), java.time.Instant.ofEpochMilli(x.start).atZone(app.puffling.domain.zone).minute),
                style = T.small.copy(color = col), modifier = Modifier.padding(top = 2.dp))
        }
        // three numbers
        Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Big(if (bolt) tr("${x.holdList().firstOrNull() ?: x.secs} с", "${x.holdList().firstOrNull() ?: x.secs} s") else mins(x.secs), if (bolt) tr("без вдоха", "without a breath") else tr("тишины", "of quiet"), C.acc, Modifier.weight(1f))
            Big("+" + fmt1(x.food), tr("спокойствия", "calm"), C.ok, Modifier.weight(1f))
            Big("+${x.shells}", tr("ракушек", "shells"), C.gold, Modifier.weight(1f))
        }
        if (x.before > 0 && x.after > 0) Column(Modifier.padding(top = 12.dp).fillMaxWidth().card(22.dp).padding(16.dp)) {
            Text(tr("Как было – как стало", "Before – after"), style = T.h3)
            Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                FeelWave(x.before, Modifier.weight(1f)); Icon(Icons.NEXT, C.mute, 20.dp, Modifier.padding(horizontal = 6.dp)); FeelWave(x.after, Modifier.weight(1f))
            }
            Text(when { x.after < x.before -> tr("Море стало тише на ${x.before - x.after}", "The sea grew calmer by ${x.before - x.after}"); x.after == x.before -> tr("Ровно, как и было", "Steady, as it was")
                else -> tr("Бывает и так – это тоже важно заметить", "That happens too – it matters to notice") }, style = T.small, modifier = Modifier.padding(top = 10.dp))
        }
        val holds = x.holdList()
        if (holds.isNotEmpty() && !bolt) Column(Modifier.padding(top = 12.dp).fillMaxWidth().clip(RoundedCornerShape(22.dp))
            .background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color(0xFF3F8FC0), Color(0xFF0B2236)))).padding(16.dp)) {
            Text(tr("Нырки", "Dives"), style = T.h3.copy(color = Color.White))
            Row(Modifier.fillMaxWidth().height(150.dp).padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                val mx = maxOf(120, holds.max())
                holds.forEachIndexed { i, h -> Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.fillMaxWidth().height((110f * h / mx).dp.coerceAtLeast(6.dp)).clip(RoundedCornerShape(bottomStart = 10.dp, bottomEnd = 10.dp)).background(Color(0x66FFFFFF)))
                    Text("${depthOf(h)} " + tr("м", "m"), style = T.smallB.copy(color = Color.White), modifier = Modifier.padding(top = 4.dp))
                    Text(clock(h), style = T.tiny.copy(color = Color.White.copy(alpha = .7f))) } }
            }
            val seen = Deep.reached(holds.max())
            if (seen.isNotEmpty()) {
                Text(tr("Видели на глубине", "Seen in the deep"), style = T.smallB.copy(color = Color.White.copy(alpha = .8f)), modifier = Modifier.padding(top = 8.dp))
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    seen.reversed().forEach { f -> app.puffling.ui.art.Picture("find_${f.id}", 52.dp) } }
            }
        }
        if (x.bpm > 0) Row(Modifier.padding(top = 12.dp).fillMaxWidth().card(22.dp).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            app.puffling.ui.components.Badge(Icons.WAVE, C.acc, 40.dp)
            Column(Modifier.padding(start = 12.dp)) { Text(tr("${fmt1(x.bpm)} вдохов в минуту", "${fmt1(x.bpm)} breaths a minute"), style = T.h3); Text(tr("телефон на животе", "the phone on your belly"), style = T.small) }
        }
        if (x.said > 0) Row(Modifier.padding(top = 12.dp).fillMaxWidth().card(22.dp).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            app.puffling.ui.components.Badge(Icons.BOTTLE, C.acc, 40.dp)
            Column(Modifier.padding(start = 12.dp)) { Text(tr("${x.said} бутылок в море", "${x.said} bottles at sea"), style = T.h3); Text(tr("сказано вслух – однажды вернутся", "said aloud – one day they'll come back"), style = T.small) }
        }
        if (x.noted.isNotBlank()) Column(Modifier.padding(top = 12.dp).fillMaxWidth().card(22.dp).padding(16.dp)) {
            Text(tr("Куда уходило внимание", "Where attention went"), style = T.h3)
            val parts = x.noted.split(',').mapNotNull { val p = it.split(':'); p.getOrNull(1)?.toIntOrNull()?.let { n -> p[0] to n } }
            val tot = parts.sumOf { it.second }.coerceAtLeast(1)
            parts.forEach { (k, n) -> Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(when (k) { "think" -> tr("думаю", "thinking"); "hear" -> tr("слышу", "hearing"); "feel" -> tr("чувствую", "feeling"); else -> tr("тело", "body") }, style = T.body, modifier = Modifier.width(96.dp))
                Meter(n.toFloat() / tot, Modifier.weight(1f), color = col, h = 8.dp); Text("  ×$n", style = T.small) } }
        }
        if (x.notes.isNotBlank()) x.notes.lines().forEach { l ->
            Row(Modifier.padding(top = 10.dp).fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(col.copy(alpha = .1f)).padding(16.dp)) {
                Text("«", style = T.title.copy(color = col)); Text(l, style = T.body.copy(fontStyle = FontStyle.Italic), modifier = Modifier.padding(start = 6.dp, top = 6.dp)) }
        }
        Spacer(Modifier.height(18.dp))
        if (!bolt && app.puffling.domain.Practices.find(x.practice) != null) Btn(tr("Ещё раз", "Again"), Modifier.fillMaxWidth(), color = col) { nav.replace(Screen.Begin(x.practice)) }
        Text(tr("Удалить запись", "Delete the entry"), style = T.smallB.copy(color = C.bad), modifier = Modifier.fillMaxWidth().padding(top = 16.dp).clickable { del = true }, textAlign = TextAlign.Center)
    }
    if (del) Popup({ del = false }) {
        Text(tr("Удалить запись?", "Delete the entry?"), style = T.h2)
        Text(tr("Питомцы останутся такими, какие есть, а запись исчезнет из летописи.", "The pets won't give back what they ate – but the journal will forget it."), style = T.small, modifier = Modifier.padding(top = 6.dp))
        Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Ghost(tr("Оставить", "Keep"), Modifier.weight(1f)) { del = false }
            Btn(tr("Удалить", "Delete"), Modifier.weight(1f), color = C.bad) { del = false; c.write { dao.deleteSession(x.id) }; nav.pop() }
        }
    }
}

@Composable
private fun Big(value: String, label: String, tint: Color, modifier: Modifier) {
    Column(modifier.card(20.dp).padding(vertical = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = T.num.copy(color = tint), maxLines = 1); Text(label, style = T.tiny, maxLines = 1)
    }
}

/** How tense, as the sea: a flat line is calm, tall waves a storm. */
@Composable
private fun FeelWave(n: Int, modifier: Modifier) {
    Column(modifier.clip(RoundedCornerShape(16.dp)).background(feelColor(n).copy(alpha = .14f)).padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        androidx.compose.foundation.Canvas(Modifier.fillMaxWidth().height(34.dp)) {
            val p = androidx.compose.ui.graphics.Path(); val amp = size.height * (.06f + .09f * n); p.moveTo(0f, size.height / 2); var xx = 0f
            while (xx <= size.width) { xx += 2f; p.lineTo(xx, size.height / 2 + amp * kotlin.math.sin(xx / size.width * 6.28f * (1 + n * .4f))) }
            drawPath(p, feelColor(n), style = androidx.compose.ui.graphics.drawscope.Stroke(3.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round))
        }
        Text(feelWord(n), style = T.smallB.copy(color = feelColor(n)), modifier = Modifier.padding(top = 4.dp))
    }
}
