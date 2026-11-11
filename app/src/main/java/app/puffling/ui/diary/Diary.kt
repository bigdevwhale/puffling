package app.puffling.ui.diary

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.puffling.container
import app.puffling.data.Entry
import app.puffling.domain.Diary
import app.puffling.domain.Span
import app.puffling.domain.dayOf
import app.puffling.domain.feelingIds
import app.puffling.domain.fmt1
import app.puffling.domain.hourOf
import app.puffling.domain.mainMood
import app.puffling.domain.relDay
import app.puffling.domain.tagIds
import app.puffling.domain.today
import app.puffling.domain.tr
import app.puffling.domain.zone
import app.puffling.ui.Data
import app.puffling.ui.Nav
import app.puffling.ui.Screen
import app.puffling.ui.components.Btn
import app.puffling.ui.components.Chip
import app.puffling.ui.components.Field
import app.puffling.ui.components.Ghost
import app.puffling.ui.components.Icon
import app.puffling.ui.components.Icons
import app.puffling.ui.components.Meter
import app.puffling.ui.components.Popup
import app.puffling.ui.components.Segments
import app.puffling.ui.components.card
import app.puffling.ui.stats.StatsScreen
import app.puffling.ui.theme.C
import app.puffling.ui.theme.T
import kotlinx.coroutines.launch

/** The diary tab: entries first, the numbers one tap away. */
@Composable
fun DiaryScreen(d: Data, nav: Nav) {
    var tab by androidx.compose.runtime.saveable.rememberSaveable { mutableIntStateOf(app.puffling.ui.Start.diaryTab) }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 16.dp, top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(tr("Дневник", "Diary"), style = T.title, modifier = Modifier.weight(1f))
            Segments(listOf(tr("Записи", "Entries"), tr("Итоги", "Summary")), tab, Modifier.width(190.dp)) { tab = it }
        }
        Box(Modifier.weight(1f)) { if (tab == 0) Entries(d, nav) else StatsScreen(d, nav, header = false) }
    }
}

/** «How are you?» – six families of feelings – and the entries by day. */
@Composable
private fun Entries(d: Data, nav: Nav) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(top = 12.dp, bottom = 24.dp)) {
        app.puffling.ui.components.Hint("diary", d.s.seen, app.puffling.ui.components.Icons.BOOK, tr("Дневник", "Diary"), tr("Нажмите на цвет – это семья чувств, внутри выберите точное слово. Каждая запись кормит питомцев, а трое новых находятся только через дневник.", "Tap a colour – a family of feelings – then pick the exact word. Every entry feeds the pets, and three new ones are found only through the diary."), Modifier.padding(bottom = 10.dp))
        Column(Modifier.fillMaxWidth().card(22.dp).padding(16.dp)) {
            Text(tr("Как вы сейчас?", "How are you right now?"), style = T.h3)
            Text(Diary.prompt(today()), style = T.small, modifier = Modifier.padding(top = 2.dp))
            Row(Modifier.fillMaxWidth().padding(top = 14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Diary.moods.forEach { m -> Column(Modifier.clip(RoundedCornerShape(14.dp)).clickable { nav.push(Screen.Entry(0, m.id)) }.padding(4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    MoodDot(Color(m.color), 40.dp)
                    Text(m.name, style = T.tiny, modifier = Modifier.padding(top = 4.dp), maxLines = 1) } }
            }
        }
        if (d.entries.isEmpty()) Text(tr("Здесь будут ваши записи: что вы чувствовали, почему и что хотелось сказать. Чем точнее названо чувство, тем легче с ним быть.",
            "Your entries will be here: what you felt, why, and what you wanted to say. The more precisely a feeling is named, the easier it is to be with it."),
            style = T.small, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(24.dp))
        d.entries.groupBy { dayOf(it.at) }.forEach { (day, list) ->
            Text(relDay(day).replaceFirstChar { it.uppercase() }, style = T.sec, modifier = Modifier.padding(top = 18.dp, bottom = 8.dp, start = 4.dp))
            Column(Modifier.fillMaxWidth().card(20.dp)) {
                list.forEachIndexed { i, e -> if (i > 0) Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(1.dp).background(C.line))
                    EntryRow(e) { nav.push(Screen.Entry(e.id)) } }
            }
        }
    }
}

@Composable
fun MoodDot(c: Color, size: androidx.compose.ui.unit.Dp) {
    Box(Modifier.size(size).clip(CircleShape).background(androidx.compose.ui.graphics.Brush.radialGradient(listOf(c.copy(alpha = .55f), c))))
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EntryRow(e: Entry, onClick: () -> Unit) {
    val m = e.mainMood()
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp), verticalAlignment = Alignment.Top) {
        Box(Modifier.padding(top = 2.dp)) { MoodDot(Color(m?.color ?: 0xFF8E8A84), 14.dp) }
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(e.feelingIds().mapNotNull { Diary.feeling(it)?.name }.joinToString(", ").ifEmpty { tr("Без чувства", "No feeling named") }.replaceFirstChar { it.uppercase() },
                    style = T.bodyB, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(String.format("%d:%02d", hourOf(e.at), java.time.Instant.ofEpochMilli(e.at).atZone(zone).minute), style = T.tiny)
            }
            if (e.text.isNotBlank()) Text(e.text, style = T.body.copy(color = C.mute), maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp))
            if (e.tags.isNotBlank()) Text(e.tagIds().mapNotNull { Diary.byTag[it]?.name?.lowercase() }.joinToString(" · "), style = T.tiny, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

/** Writing an entry: the feelings, how strong, what it was about, a few words. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EntryPage(s: Screen.Entry, d: Data, nav: Nav) {
    val c = LocalContext.current.container
    val old = d.entries.firstOrNull { it.id == s.id }
    var feelings by remember { mutableStateOf(old?.feelingIds()?.toSet() ?: emptySet()) }
    var opened by remember { mutableStateOf(setOfNotNull(s.mood.ifEmpty { null }) + (old?.feelingIds()?.map { it.substringBefore(':') } ?: emptyList())) }
    var strength by remember { mutableIntStateOf(old?.strength ?: 3) }
    var tags by remember { mutableStateOf(old?.tagIds()?.toSet() ?: emptySet()) }
    var text by remember { mutableStateOf(old?.text ?: "") }
    var del by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    androidx.activity.compose.BackHandler { nav.pop() }
    Column(Modifier.fillMaxSize().background(C.bg).clickable(remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, null) {}.statusBarsPadding().navigationBarsPadding().imePadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(CircleShape).clickable { nav.pop() }, contentAlignment = Alignment.Center) { Icon(Icons.CLOSE, C.ink, 22.dp) }
            Text(if (old == null) tr("Новая запись", "New entry") else relDay(dayOf(old.at)).replaceFirstChar { it.uppercase() }, style = T.h2, modifier = Modifier.weight(1f))
            if (old != null) Box(Modifier.size(44.dp).clip(CircleShape).clickable { del = true }, contentAlignment = Alignment.Center) { Icon(Icons.TRASH, C.mute, 20.dp) }
        }
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp)) {
            app.puffling.ui.components.Hint("entry", d.s.seen, app.puffling.ui.components.Icons.EDIT, tr("Запись", "An entry"), tr("Можно выбрать несколько чувств из разных семей. Сила, причины и текст – по желанию.", "You can pick several feelings from different families. Strength, causes and text are optional."), Modifier.padding(top = 6.dp))
            Text(tr("Что вы чувствуете?", "What do you feel?"), style = T.h3, modifier = Modifier.padding(top = 6.dp))
            Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Diary.moods.forEach { m -> val on = m.id in opened
                    Column(Modifier.clip(RoundedCornerShape(14.dp)).clickable { opened = if (on) opened - m.id else opened + m.id }.padding(4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.size(42.dp).clip(CircleShape).border(if (on) 3.dp else 0.dp, if (on) C.ink else Color.Transparent, CircleShape).padding(if (on) 4.dp else 0.dp)) { MoodDot(Color(m.color), 42.dp) }
                        Text(m.name, style = T.tiny.copy(color = if (on) C.ink else C.mute), modifier = Modifier.padding(top = 4.dp), maxLines = 1) } }
            }
            Diary.moods.filter { it.id in opened }.forEach { m ->
                FlowRow(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    m.words.forEach { f -> val on = f.id in feelings
                        Chip(f.name, on, color = Color(m.color)) { feelings = if (on) feelings - f.id else feelings + f.id } } }
            }
            if (feelings.isNotEmpty()) {
                Text(tr("Насколько сильно", "How strong"), style = T.h3, modifier = Modifier.padding(top = 20.dp))
                Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val col = Color(Diary.moodOf(feelings.first()).color)
                    (1..5).forEach { n -> Box(Modifier.weight(1f).height(14.dp + (n * 4).dp).clip(RoundedCornerShape(8.dp)).background(if (n <= strength) col else C.track).clickable { strength = n }) }
                }
                Row(Modifier.fillMaxWidth().padding(top = 4.dp)) { Text(tr("едва", "barely"), style = T.tiny, modifier = Modifier.weight(1f)); Text(tr("очень", "very"), style = T.tiny) }
            }
            Text(tr("Что повлияло", "What it's about"), style = T.h3, modifier = Modifier.padding(top = 20.dp))
            FlowRow(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Diary.tags.forEach { t -> val on = t.id in tags; Chip(t.name, on) { tags = if (on) tags - t.id else tags + t.id } } }
            Text(tr("Несколько слов", "A few words"), style = T.h3, modifier = Modifier.padding(top = 20.dp))
            Field(text, { text = it }, Diary.prompt(today()), Modifier.fillMaxWidth().padding(top = 10.dp).height(140.dp), single = false)
            Spacer(Modifier.height(16.dp))
        }
        Btn(tr("Сохранить", "Save"), Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp), enabled = feelings.isNotEmpty() || text.isNotBlank()) {
            val e = (old ?: Entry(at = System.currentTimeMillis())).copy(emotions = feelings.joinToString(","), strength = strength, tags = tags.joinToString(","), text = text.trim())
            scope.launch {
                val out = c.saveEntry(e)
                nav.pop()
                if (out != null) c.say(tr("Записано · +${fmt1(out.food)} спокойствия, +${out.shells} ракушек", "Saved · +${fmt1(out.food)} calm, +${out.shells} shells") +
                    (out.bonus.firstOrNull()?.let { "\n$it" } ?: ""))
            }
        }
    }
    if (del && old != null) Popup({ del = false }) {
        Text(tr("Удалить запись?", "Delete the entry?"), style = T.h2)
        Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Ghost(tr("Оставить", "Keep"), Modifier.weight(1f)) { del = false }
            Btn(tr("Удалить", "Delete"), Modifier.weight(1f), color = C.bad) { del = false; c.write { dao.deleteEntry(old.id) }; nav.pop() }
        }
    }
}

/** Feelings in a span, for the summary: the families as one stripe and the most frequent words. */
@Composable
fun FeelingsCard(d: Data, span: Span) {
    val list = d.entries.filter { dayOf(it.at) in span.from..span.to }
    if (list.isEmpty()) return
    val ids = list.flatMap { it.feelingIds() }
    val byMood = ids.groupingBy { it.substringBefore(':') }.eachCount()
    val total = byMood.values.sum().coerceAtLeast(1)
    Column(Modifier.padding(top = 12.dp).fillMaxWidth().card(22.dp).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(tr("Чувства", "Feelings"), style = T.h3, modifier = Modifier.weight(1f))
            Text(app.puffling.domain.count(list.size, "запись", "записи", "записей", "entry", "entries"), style = T.small)
        }
        Row(Modifier.padding(top = 12.dp).fillMaxWidth().height(12.dp).clip(RoundedCornerShape(50))) {
            Diary.moods.filter { (byMood[it.id] ?: 0) > 0 }.forEach { m -> Box(Modifier.weight((byMood[m.id] ?: 0).toFloat()).fillMaxSize().background(Color(m.color))) }
        }
        ids.groupingBy { it }.eachCount().entries.sortedByDescending { it.value }.take(4).forEach { (id, n) -> val f = Diary.feeling(id) ?: return@forEach
            Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                MoodDot(Color(Diary.moodOf(id).color), 10.dp)
                Text(f.name.replaceFirstChar { it.uppercase() }, style = T.body, modifier = Modifier.padding(start = 10.dp).weight(1f))
                Text("×$n", style = T.small)
            } }
        val tagTop = list.flatMap { it.tagIds() }.groupingBy { it }.eachCount().entries.sortedByDescending { it.value }.take(3)
        if (tagTop.isNotEmpty()) Text(tr("Чаще всего про: ", "Mostly about: ") + tagTop.mapNotNull { Diary.byTag[it.key]?.name?.lowercase() }.joinToString(", "), style = T.small, modifier = Modifier.padding(top = 12.dp))
    }
}
