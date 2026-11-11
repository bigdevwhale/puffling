package app.puffling.ui.practice

import android.content.Context
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
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
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.puffling.Done
import app.puffling.container
import app.puffling.domain.Affirmations
import app.puffling.domain.Aff
import app.puffling.domain.Courses
import app.puffling.domain.Deep
import app.puffling.domain.Element
import app.puffling.domain.Kind
import app.puffling.domain.Ph
import app.puffling.domain.Pets
import app.puffling.domain.Practice
import app.puffling.domain.Practices
import app.puffling.domain.Stage
import app.puffling.domain.WhPhase
import app.puffling.domain.WimHofEngine
import app.puffling.domain.WimHofLevel
import app.puffling.domain.clock
import app.puffling.domain.count
import app.puffling.domain.depthOf
import app.puffling.domain.fmt1
import app.puffling.domain.mins
import app.puffling.domain.stage
import app.puffling.domain.today
import app.puffling.domain.tr
import app.puffling.ui.Data
import app.puffling.ui.Nav
import app.puffling.ui.Screen
import app.puffling.ui.art.Art
import app.puffling.ui.art.Creature
import app.puffling.ui.art.Flame
import app.puffling.ui.art.Pose
import app.puffling.ui.art.breathRings
import app.puffling.ui.art.deepWater
import app.puffling.ui.art.loop
import app.puffling.ui.art.sprite
import app.puffling.ui.components.Btn
import app.puffling.ui.components.Chip
import app.puffling.ui.components.Field
import app.puffling.ui.components.Ghost
import app.puffling.ui.components.Icon
import app.puffling.ui.components.Icons
import app.puffling.ui.components.Page
import app.puffling.ui.components.Popup
import app.puffling.ui.components.Section
import app.puffling.ui.components.card
import app.puffling.ui.theme.C
import app.puffling.ui.theme.T
import app.puffling.work.BreathSensor
import app.puffling.work.Plan
import app.puffling.work.Player
import kotlinx.coroutines.launch

/** Options of the start page that a launch passes on. */
data class Opts(val minutes: Int? = null, val rounds: Int? = null, val breaths: Int? = null, val sound: String? = null, val bellKind: String? = null,
                val belly: Boolean? = null, val deck: String? = null, val before: Int = 0, val cycles: Int = 0, val level: String? = null, val inhale: Double? = null, val exhale: Double? = null)

/** The Wim Hof level a practice plays: its own preset, or the one chosen. */
fun levelOf(p: Practice, chosen: String): WimHofLevel = if (p.level == "cus") WimHofLevel.custom(p.rounds, p.breaths, p.hold) else WimHofLevel.byId(p.level.ifEmpty { chosen })

/** The words for how tense it feels, 1 calm .. 5 storm. */
fun feelWord(n: Int) = when (n) { 1 -> tr("Штиль", "Calm"); 2 -> tr("Лёгкий ветер", "A breeze"); 3 -> tr("Волнение", "Choppy"); 4 -> tr("Шторм", "Rough"); else -> tr("Буря", "Storm") }
fun feelColor(n: Int) = when (n) { 1 -> Color(0xFF5E9C76); 2 -> Color(0xFF7DB0A0); 3 -> Color(0xFFE0A63A); 4 -> Color(0xFFE07A4A); else -> Color(0xFFD9606A) }

/** Starts a practice with the settings' defaults and [o], and opens the session screen. */
fun launch(ctx: Context, d: Data, nav: Nav, id: String, course: String = "", bell: Long = 0, o: Opts = Opts()) {
    val p0 = Practices.of(id)
    val p = if (p0.kind == Kind.AFFIRM && o.deck != null) p0.copy(deck = o.deck) else p0
    val lv = if (p.kind == Kind.WIMHOF) levelOf(p, o.level ?: d.s.whLevel) else null
    val pl = Plan(p, o.minutes ?: p.minutes, o.rounds ?: lv?.defaultRounds ?: p.rounds, o.breaths ?: p.breaths, o.sound ?: (if (id == "bell") "none" else d.s.sound), o.bellKind ?: d.s.bellKind,
        d.s.volume, d.s.cues, d.s.haptics, o.belly ?: false, course, o.before, bell, cycles = if (id == "bell") 3 else o.cycles,
        level = lv, inhale = o.inhale ?: d.s.whIn, exhale = o.exhale ?: d.s.whOut)
    Player.start(ctx, pl)
    nav.push(Screen.Playing)
}

/** The page before a practice: the pet and the name, a short line about it, a few options, «Begin» always in reach. */
@Composable
fun StartPage(b: Screen.Begin, d: Data, nav: Nav) {
    val ctx = LocalContext.current
    val p = Practices.find(b.practice) ?: run { nav.pop(); return }
    var minutes by remember { mutableIntStateOf(p.minutes) }
    var rounds by remember { mutableIntStateOf(p.rounds) }
    var breaths by remember { mutableIntStateOf(p.breaths) }
    var sound by remember { mutableStateOf(d.s.sound) }
    var bellKind by remember { mutableStateOf(d.s.bellKind) }
    var belly by remember { mutableStateOf(d.s.belly) }
    var deck by remember { mutableStateOf(p.deck.ifEmpty { "mix" }) }
    var before by remember { mutableIntStateOf(0) }
    var level by remember { mutableStateOf(d.s.whLevel) }
    var inhale by remember { mutableStateOf(d.s.whIn) }
    var exhale by remember { mutableStateOf(d.s.whOut) }
    var more by remember { mutableStateOf(false) }
    var safety by remember { mutableStateOf(false) }
    val lv = if (p.kind == Kind.WIMHOF) levelOf(p, level) else null
    LaunchedEffect(level) { if (lv != null) rounds = lv.defaultRounds }
    val course = b.course.substringBefore(':').let { Courses.byId[it] }
    val col = Color(p.el.color)
    androidx.activity.compose.BackHandler { nav.pop() }
    Column(Modifier.fillMaxSize().background(C.bg).clickable(remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, null) {}.statusBarsPadding().navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(CircleShape).clickable { nav.pop() }, contentAlignment = Alignment.Center) { Icon(Icons.BACK, C.ink, 22.dp) }
            Spacer(Modifier.weight(1f))
            if (p.own) Box(Modifier.size(44.dp).clip(CircleShape).clickable { nav.push(Screen.OwnEdit(p.id.drop(1).toLong())) }, contentAlignment = Alignment.Center) { Icon(Icons.EDIT, C.ink, 20.dp) }
        }
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            app.puffling.ui.components.Hint("begin", d.s.seen, app.puffling.ui.components.Icons.PLAY, tr("Перед началом", "Before you begin"), tr("Выберите длительность и фон. «Как сейчас внутри» – по желанию: потом «Итоги» покажут, что помогает именно вам.", "Pick the length and the sound. How it feels inside is optional: later the summary shows what helps you."), Modifier.padding(bottom = 10.dp))
            Box(Modifier.size(148.dp).clip(CircleShape).background(Brush.radialGradient(listOf(col.copy(alpha = .3f), col.copy(alpha = .06f)))), contentAlignment = Alignment.Center) {
                d.leader?.let { lead -> Creature(lead.species, maxOf(lead.stage(), Stage.BABY), Pose.PRACTICE, 136.dp, lead.shiny, glow = false) } }
            Text(p.name, style = T.title.copy(fontSize = 24.sp), textAlign = TextAlign.Center, modifier = Modifier.padding(top = 10.dp))
            val meta = listOfNotNull(p.el.label, when (p.kind) { Kind.BREATH, Kind.SIT -> null; else -> lengthOf(p) },
                if (p.kind == Kind.BREATH) p.pattern.take(6).joinToString("-") { fmt1(it.secs) } else null)
            Text(meta.joinToString(" · "), style = T.small.copy(color = col), modifier = Modifier.padding(top = 2.dp))
            if (course != null) Text(tr("Курс «${course.name}», день ${(b.course.substringAfter(':').toIntOrNull() ?: 0) + 1}", "Course «${course.name}», day ${(b.course.substringAfter(':').toIntOrNull() ?: 0) + 1}"),
                style = T.smallB.copy(color = Color(course.color)), modifier = Modifier.padding(top = 4.dp))
            if (p.about.isNotBlank()) Text(p.about, style = T.body.copy(color = C.mute), textAlign = TextAlign.Center, maxLines = if (more) 20 else 2, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 10.dp).clickable { more = !more })
            if (p.kind == Kind.WIMHOF) Row(Modifier.padding(top = 10.dp).clip(RoundedCornerShape(50)).background(C.goldSoft).clickable { safety = !safety }.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.DROP, C.gold, 14.dp); Text(tr("  Только сидя или лёжа", "  Only sitting or lying down"), style = T.smallB.copy(color = C.gold)) }
            if (safety) Text(tr("Никогда в воде, в ванне и за рулём. Головокружение и покалывание – нормально, но не терпите через силу. При болезнях сердца, эпилепсии и беременности – не надо.",
                "Never in water, in the bath or at the wheel. Light-headedness and tingling are normal, but don't push through. Not with heart conditions, epilepsy or in pregnancy."),
                style = T.small, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp))
            Spacer(Modifier.height(16.dp))
            // the options, one quiet card
            Column(Modifier.fillMaxWidth().card(18.dp).padding(horizontal = 14.dp, vertical = 6.dp)) {
                when (p.kind) {
                    Kind.BREATH, Kind.SIT -> OptRow(tr("Минут", "Minutes")) { ChipsScroll(listOf(1, 2, 3, 5, 10, 15, 20, 30).map { "$it" to "$it" }, "$minutes") { minutes = it.toInt() } }
                    Kind.WIMHOF -> if (lv != null) {
                        if (p.level.isEmpty()) OptRow(tr("Уровень", "Level")) { ChipsScroll(WimHofLevel.ALL.map { it.id to it.name }, level) { level = it } }
                        if (lv.maxRounds > lv.defaultRounds) OptRow(tr("Раунды", "Rounds")) { ChipsScroll((lv.defaultRounds..lv.maxRounds).map { "$it" to "$it" }, "$rounds") { rounds = it.toInt() } }
                        val rr = rounds.coerceIn(1, lv.breaths.size)
                        OptRow(tr("Вдохи", "Breaths")) { Text(tr("${lv.breathsLabel} вдохов · задержки ", "${lv.breathsLabel} breaths · holds ") + lv.holds.take(rr).joinToString("→") { clock(it) }, style = T.small.copy(color = C.ink)) }
                        OptRow(tr("Темп", "Pace")) { Row(verticalAlignment = Alignment.CenterVertically) { TempoStep(inhale) { inhale = it }; Text(" / ", style = T.small); TempoStep(exhale) { exhale = it } } }
                        OptRow(tr("Всего", "In all")) { Text(tr("~${(lv.estimateSeconds(rr, inhale, exhale) / 60).toInt()} мин", "~${(lv.estimateSeconds(rr, inhale, exhale) / 60).toInt()} min"), style = T.small.copy(color = C.ink)) }
                    }
                    Kind.AFFIRM -> OptRow(tr("Слова", "Words")) {
                        ChipsScroll(listOf("mix" to tr("Микс дня", "Today's mix")) + (if (d.s.favs.isNotEmpty()) listOf("fav" to tr("Любимые", "Favourites")) else emptyList()) +
                            (if (d.affs.isNotEmpty()) listOf("own" to tr("Свои", "My own")) else emptyList()) + Affirmations.themes.map { it.id to it.name }, deck) { deck = it } }
                    else -> {}
                }
                if (p.kind != Kind.AFFIRM) OptRow(tr("Фон", "Sound")) {
                    ChipsScroll(listOf("sea" to tr("Море", "Sea"), "rain" to tr("Дождь", "Rain"), "wind" to tr("Ветер", "Wind"), "fire" to tr("Костёр", "Fire"), "none" to tr("Тишина", "Silence")), sound) { sound = it } }
                if (p.kind == Kind.SIT || p.kind == Kind.GUIDED) OptRow(tr("Колокол", "Bell")) {
                    ChipsScroll(listOf("bowl" to tr("Чаша", "Bowl"), "bell" to tr("Колокол", "Bell"), "chime" to tr("Тингша", "Tingsha"), "gong" to tr("Гонг", "Gong")), bellKind) { bellKind = it; app.puffling.work.Synth.ring(it, d.s.volume) } }
                if ((p.kind == Kind.BREATH || p.kind == Kind.SIT) && BreathSensor.available(ctx)) OptRow(tr("На живот", "On belly")) {
                    Row(Modifier.fillMaxWidth().clickable { belly = !belly }, verticalAlignment = Alignment.CenterVertically) {
                        Text(tr("телефон чувствует ваше дыхание", "the phone feels your breath"), style = T.small, modifier = Modifier.weight(1f))
                        androidx.compose.material3.Switch(belly, { belly = it }, colors = androidx.compose.material3.SwitchDefaults.colors(checkedTrackColor = C.acc)) } }
            }
            if (d.s.askFeel && b.practice != "bolt") {
                Text(tr("Как сейчас внутри?", "How is it inside right now?"), style = T.smallB.copy(color = C.mute), modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
                FeelRow(before) { before = if (before == it) 0 else it }
            }
            Spacer(Modifier.height(16.dp))
        }
        Btn(tr("Начать", "Begin"), Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp), color = col) {
            nav.pop()
            launch(ctx, d, nav, p.id, b.course, b.bell, Opts(minutes, rounds, breaths, sound, bellKind, belly, deck, before, level = level, inhale = inhale, exhale = exhale))
            val keepLevel = p.level.isEmpty() && p.kind == Kind.WIMHOF
            ctx.container.write { settings.update { it.copy(belly = belly, sound = sound, bellKind = bellKind, whLevel = if (keepLevel) level else it.whLevel, whIn = inhale, whOut = exhale) } }
        }
    }
}

/** One option: a short label on the left, its control on the right. */
@Composable
private fun OptRow(label: String, content: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = T.small, modifier = Modifier.width(76.dp))
        Box(Modifier.weight(1f)) { content() }
    }
}

@Composable
private fun ChipsScroll(items: List<Pair<String, String>>, sel: String, onSel: (String) -> Unit) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items.forEach { (k, l) -> Chip(l, k == sel) { onSel(k) } }
    }
}

@Composable
private fun TempoStep(v: Double, on: (Double) -> Unit) {
    fun step(k: Double) = (Math.round((v + k) * 10) / 10.0).coerceIn(.5, 4.0)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(30.dp).clip(CircleShape).background(C.card2).clickable { on(step(-.1)) }, contentAlignment = Alignment.Center) { Text("−", style = T.bodyB) }
        Text(tr("${fmt1(v)} с", "${fmt1(v)} s"), style = T.numS, modifier = Modifier.width(56.dp), textAlign = TextAlign.Center)
        Box(Modifier.size(30.dp).clip(CircleShape).background(C.card2).clickable { on(step(.1)) }, contentAlignment = Alignment.Center) { Text("+", style = T.bodyB) }
    }
}

@Composable
fun FlowChips(items: List<Pair<String, String>>, sel: String, onSel: (String) -> Unit) {
    androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items.forEach { (k, l) -> Chip(l, k == sel) { onSel(k) } }
    }
}

@Composable
fun FeelRow(sel: Int, light: Boolean = false, onSel: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        (1..5).forEach { n ->
            val on = sel == n
            Column(Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(if (on) feelColor(n) else if (light) Color(0x22FFFFFF) else C.card2).clickable { onSel(n) }.padding(vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Canvas(Modifier.size(28.dp, 16.dp)) { // a little wave: flatter is calmer
                    val p = androidx.compose.ui.graphics.Path(); val amp = size.height * (.05f + .1f * n)
                    p.moveTo(0f, size.height / 2); var x = 0f
                    while (x <= size.width) { x += 1f; p.lineTo(x, size.height / 2 + amp * kotlin.math.sin(x / size.width * 6.28f * (1 + n * .3f))) }
                    drawPath(p, if (on) Color.White else if (light) Color.White.copy(alpha = .7f) else feelColor(n), style = androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx()))
                }
                Text(feelWord(n), style = T.tiny.copy(color = if (on) Color.White else if (light) Color.White.copy(alpha = .85f) else C.ink, fontSize = 10.sp), maxLines = 1, textAlign = TextAlign.Center)
            }
        }
    }
}

fun phColor(p: Ph) = when (p) { Ph.IN, Ph.TOP -> Color(0xFF5FA8C9); Ph.OUT -> Color(0xFF5E9C76); else -> Color(0xFFE0A63A) }

/** The session itself: dark and quiet, the pet in the middle. */
@Composable
fun SessionScreen(d: Data, nav: Nav) {
    val ctx = LocalContext.current
    val c = ctx.container
    val pl = Player.plan
    if (pl == null) { LaunchedEffect(Unit) { nav.pop() }; return }
    val p = pl.p
    var ask by remember { mutableStateOf(false) }
    var after by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    val view = LocalView.current
    DisposableEffect(Unit) { view.keepScreenOn = true; onDispose { view.keepScreenOn = false } }
    BackHandler { if (Player.done) {} else ask = true }
    fun save() {
        val secs = if (Player.done) Player.t.toInt() else Player.t.toInt()
        val holds = Player.holds.toList(); val notes = Player.notes.toList(); val noted = HashMap(Player.noted)
        val said = saidList.toList(); saidList.clear()
        val bpm = if (pl.belly) BreathSensor.average() else 0.0
        Player.stop(ctx)
        if (secs < 10 && p.id != "bell") { nav.pop(); return }
        scope.launch {
            val (id, out) = c.finish(Done(p.id, System.currentTimeMillis() - secs * 1000L, secs, Player.round + 1, holds, bpm, pl.before, after, notes, said, noted, pl.course, pl.bell))
            nav.replace(Screen.Result(id, out))
        }
    }
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF16263A), C.night)))) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(44.dp).clip(CircleShape).clickable { if (!Player.done) ask = true }, contentAlignment = Alignment.Center) { Icon(Icons.CLOSE, Color.White.copy(alpha = .7f), 22.dp) }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(p.name, style = T.bodyB.copy(color = Color.White.copy(alpha = .9f)), maxLines = 1)
                    Text(timeLine(pl), style = T.small.copy(color = Color.White.copy(alpha = .55f)))
                }
                Box(Modifier.size(44.dp).clip(CircleShape).clickable { if (!Player.done) Player.paused = !Player.paused }, contentAlignment = Alignment.Center) {
                    Icon(if (Player.paused) Icons.PLAY else Icons.PAUSE, Color.White.copy(alpha = .7f), 20.dp) }
            }
            Box(Modifier.weight(1f).fillMaxWidth()) {
                if (Player.done) AfterPanel(d, pl, after, { after = it }) { save() }
                else when (p.kind) {
                    Kind.BREATH -> Pacer(d, pl)
                    Kind.WIMHOF -> WimHof(d, pl)
                    Kind.SIT -> Sitting(d, pl)
                    Kind.GUIDED -> Guided(d, pl)
                    Kind.AFFIRM -> Affirm(d, pl)
                }
            }
        }
        if (Player.paused && !Player.done) Box(Modifier.fillMaxSize().background(Color(0xAA000000)).clickable { Player.paused = false }, contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.PLAY, Color.White, 48.dp); Text(tr("Пауза", "Paused"), style = T.h3.copy(color = Color.White)) } }
    }
    if (ask) Popup({ ask = false }) {
        Text(tr("Закончить сейчас?", "Finish now?"), style = T.h2)
        Text(tr("Всё, что уже сделано, засчитается.", "Everything done so far will count."), style = T.small, modifier = Modifier.padding(top = 6.dp))
        Row(Modifier.padding(top = 18.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Ghost(tr("Продолжить", "Keep going"), Modifier.weight(1f)) { ask = false }
            Btn(tr("Закончить", "Finish"), Modifier.weight(1f)) { ask = false; if (Player.wh?.phase == WhPhase.HOLD) Player.endHold(); Player.finish(false) }
        }
        Text(tr("Выйти без сохранения", "Leave without saving"), style = T.smallB.copy(color = C.bad), modifier = Modifier.padding(top = 14.dp).clickable { ask = false; Player.stop(ctx); saidList.clear(); nav.pop() })
    }
}

/** Phrases said aloud this session (kept outside the player: they are only text). */
val saidList = mutableStateListOf<String>()

private fun timeLine(pl: Plan): String {
    val t = Player.t.toInt()
    return when (pl.p.kind) {
        Kind.BREATH -> if (pl.cycles > 0) "${Player.cycles.coerceAtMost(pl.cycles)} / ${pl.cycles}" else "${clock(t)} / ${clock(pl.minutes * 60)}"
        Kind.SIT -> clock((pl.minutes * 60 - t).coerceAtLeast(0))
        Kind.WIMHOF -> tr("Раунд ${Player.round + 1} из ${pl.rounds}", "Round ${Player.round + 1} of ${pl.rounds}")
        Kind.GUIDED -> clock((pl.p.guidedSecs - t).coerceAtLeast(0))
        Kind.AFFIRM -> clock(t)
    }
}

/** The pet as the breath: it fills on the in-breath; the rings widen with it. */
@Composable
private fun Pacer(d: Data, pl: Plan) {
    val lead = d.leader
    val ph = Player.phase
    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val s = minOf(maxWidth, maxHeight) * .62f
        Canvas(Modifier.fillMaxSize()) { breathRings(Player.lungs, phColor(ph?.ph ?: Ph.IN)) }
        if (lead != null) Creature(lead.species, maxOf(lead.stage(), Stage.BABY), Pose.PRACTICE, s, lead.shiny, lungs = Player.lungs, glow = false)
        Column(Modifier.align(Alignment.BottomCenter).padding(bottom = 26.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            AnimatedContent(ph?.ph?.label ?: "", transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "ph") { Text(it, style = T.title.copy(color = Color.White, fontSize = 34.sp)) }
            if (!ph?.note.isNullOrBlank()) Text(ph!!.note, style = T.body.copy(color = Color.White.copy(alpha = .7f)))
            Text("${kotlin.math.ceil(Player.phaseLeft.toDouble()).toInt().coerceAtLeast(1)}", style = T.big.copy(color = Color.White.copy(alpha = .8f)))
            if (pl.belly) BellyLine()
        }
    }
}

/** The breath the phone feels on the belly, as a small line and breaths a minute. */
@Composable
private fun BellyLine() {
    val trace = remember { ArrayList<Float>() }
    trace += BreathSensor.level; if (trace.size > 120) trace.removeAt(0)
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 8.dp)) {
        Canvas(Modifier.width(160.dp).height(28.dp)) {
            val p = androidx.compose.ui.graphics.Path()
            trace.forEachIndexed { i, v -> val x = size.width * i / 119f; val y = size.height * (1 - v); if (i == 0) p.moveTo(x, y) else p.lineTo(x, y) }
            drawPath(p, Color(0xFF8FD0EA), style = androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx()))
        }
        Text(if (BreathSensor.bpm > 0) tr("ваше дыхание · ${fmt1(BreathSensor.bpm.toDouble())} в мин", "your breath · ${fmt1(BreathSensor.bpm.toDouble())} a min") else tr("слушаю ваше дыхание…", "listening to your breath…"),
            style = T.tiny.copy(color = Color.White.copy(alpha = .6f)))
    }
}

/** Wim Hof (the SwissZen engine): the power breaths, the hold as a dive, the recovery breath. */
@Composable
private fun WimHof(d: Data, pl: Plan) {
    val e = Player.wh ?: return
    @Suppress("UNUSED_VARIABLE") val f = Player.frame
    val lead = d.leader
    val ctx = LocalContext.current
    val (big, small) = e.words()
    if (e.phase == WhPhase.HOLD) {
        val t = loop(9000, "deep")
        val secs = e.t.toFloat()
        val near = Deep.all.filter { kotlin.math.abs(secs - it.secs) < 9 }
        BoxWithConstraints(Modifier.fillMaxSize().clickable { Player.endHold() }) {
            val wDp = maxWidth; val hDp = maxHeight
            Canvas(Modifier.fillMaxSize()) {
                deepWater(secs, t)
                for (fd in near) { val img = Art.find(ctx, fd.id) ?: continue
                    val k = (secs - fd.secs + 9) / 18f
                    val y = size.height * (1.1f - k * 1.1f); val x = size.width * (if (fd.secs % 2 == 0) .22f else .78f)
                    sprite(img, x, y, size.width * .24f, alpha = (1f - kotlin.math.abs(k - .5f) * 2f).coerceIn(0f, 1f) * .95f) }
            }
            if (lead != null) Box(Modifier.offset(x = (wDp - wDp * .42f) / 2, y = hDp * .32f)) {
                Creature(lead.species, maxOf(lead.stage(), Stage.BABY), Pose.PRACTICE, wDp * .42f, lead.shiny, lungs = 0f, glow = false) }
            Column(Modifier.align(Alignment.TopCenter).padding(top = 18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(clock(secs.toInt()), style = T.big.copy(color = Color.White, fontSize = 52.sp))
                val target = if (secs > e.holdTarget) tr("цель ${clock(e.holdTarget)} пройдена", "target ${clock(e.holdTarget)} passed") else tr("цель ${clock(e.holdTarget)}", "target ${clock(e.holdTarget)}")
                Text(tr("${depthOf(secs.toInt())} м", "${depthOf(secs.toInt())} m") + " · " + target, style = T.h3.copy(color = Color(0xFF8FD0EA)))
                near.firstOrNull { secs >= it.secs - 1 }?.let { Text(it.name, style = T.small.copy(color = Color.White.copy(alpha = .8f))) }
            }
            Column(Modifier.align(Alignment.BottomCenter).padding(bottom = 26.dp, start = 24.dp, end = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(small, style = T.small.copy(color = Color.White.copy(alpha = .75f)), textAlign = TextAlign.Center)
                Btn(tr("Вдохнуть", "Breathe in"), Modifier.padding(top = 12.dp).width(220.dp), color = Color.White, ink = Color(0xFF16263A)) { Player.endHold() }
            }
        }
        return
    }
    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val s = minOf(maxWidth, maxHeight) * .58f
        val ringCol = if (e.phase == WhPhase.REC_HOLD || e.phase == WhPhase.REC_IN) Color(0xFFE0A63A) else Color(0xFF5FA8C9)
        val ring = e.ring()
        Canvas(Modifier.fillMaxSize()) {
            breathRings(Player.lungs, ringCol)
            val r = size.minDimension * .44f
            val tl = Offset(size.width / 2 - r, size.height / 2 - r); val sz = androidx.compose.ui.geometry.Size(2 * r, 2 * r)
            drawArc(Color.White.copy(alpha = .1f), 0f, 360f, false, tl, sz, style = androidx.compose.ui.graphics.drawscope.Stroke(4.dp.toPx()))
            drawArc(ringCol, -90f, 360f * ring, false, tl, sz, style = androidx.compose.ui.graphics.drawscope.Stroke(4.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round))
        }
        if (lead != null) Creature(lead.species, maxOf(lead.stage(), Stage.BABY), Pose.PRACTICE, s, lead.shiny, lungs = Player.lungs, glow = false)
        Column(Modifier.align(Alignment.TopCenter).padding(top = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            val count = when (e.phase) {
                WhPhase.INTRO -> "${kotlin.math.ceil(WimHofEngine.INTRO - e.t).toInt().coerceAtLeast(1)}"
                WhPhase.IN, WhPhase.OUT -> "${e.breath}"
                WhPhase.LAST_IN, WhPhase.LAST_OUT -> "${e.breathsThisRound}"
                WhPhase.REC_HOLD -> "${kotlin.math.ceil(WimHofEngine.REC_HOLD - e.t).toInt().coerceAtLeast(1)}"
                else -> ""
            }
            Text(count, style = T.big.copy(color = Color.White, fontSize = 52.sp))
            if (e.phase == WhPhase.IN || e.phase == WhPhase.OUT) Text(tr("из ${e.breathsThisRound}", "of ${e.breathsThisRound}"), style = T.small.copy(color = Color.White.copy(alpha = .6f)))
        }
        Column(Modifier.align(Alignment.BottomCenter).padding(bottom = 26.dp, start = 24.dp, end = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            AnimatedContent(big, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "wh") { Text(it, style = T.title.copy(color = Color.White, fontSize = 28.sp), textAlign = TextAlign.Center) }
            if (small.isNotEmpty()) Text(small, style = T.body.copy(color = Color.White.copy(alpha = .7f)), textAlign = TextAlign.Center)
            if (Player.holds.isNotEmpty()) Text(Player.holds.mapIndexed { i, h -> "${i + 1}: ${clock(h)}" }.joinToString(" · "), style = T.tiny.copy(color = Color(0xFF8FD0EA)), modifier = Modifier.padding(top = 8.dp))
        }
    }
}

/** Silent sitting: the pet sits, the time goes round. A tap darkens the screen. */
@Composable
private fun Sitting(d: Data, pl: Plan) {
    var dim by remember { mutableStateOf(false) }
    val lead = d.leader
    BoxWithConstraints(Modifier.fillMaxSize().clickable { dim = !dim }, contentAlignment = Alignment.Center) {
        val s = minOf(maxWidth, maxHeight) * .55f
        val f = (Player.t / (pl.minutes * 60f)).coerceIn(0f, 1f)
        Canvas(Modifier.size(s * 1.5f)) {
            drawCircle(Color.White.copy(alpha = .08f), size.minDimension / 2, style = androidx.compose.ui.graphics.drawscope.Stroke(4.dp.toPx()))
            drawArc(Color(0xFF8C7BB8), -90f, 360f * f, false, style = androidx.compose.ui.graphics.drawscope.Stroke(4.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round))
        }
        if (lead != null) Creature(lead.species, maxOf(lead.stage(), Stage.BABY), Pose.PRACTICE, s, lead.shiny, lungs = Player.lungs, glow = false)
        Column(Modifier.align(Alignment.BottomCenter).padding(bottom = 26.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(tr("Просто сидите и дышите", "Just sit and breathe"), style = T.body.copy(color = Color.White.copy(alpha = .7f)))
            if (pl.p.bell > 0) Text(tr("колокольчик каждые ${pl.p.bell} мин", "a bell every ${pl.p.bell} min"), style = T.tiny.copy(color = Color.White.copy(alpha = .5f)))
            if (pl.belly) BellyLine()
            Text(tr("коснитесь, чтобы погасить экран", "tap to darken the screen"), style = T.tiny.copy(color = Color.White.copy(alpha = .4f)), modifier = Modifier.padding(top = 8.dp))
        }
        if (dim) Box(Modifier.fillMaxSize().background(Color.Black).clickable { dim = false })
    }
}

/** Guided steps: one line at a time, the next one by itself or by a tap. */
@Composable
private fun Guided(d: Data, pl: Plan) {
    val st = pl.p.steps.getOrNull(Player.step) ?: return
    val lead = d.leader
    var line by remember(Player.step) { mutableStateOf("") }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding().padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            pl.p.steps.indices.forEach { i -> Box(Modifier.size(if (i == Player.step) 18.dp else 6.dp, 6.dp).clip(RoundedCornerShape(50)).background(if (i <= Player.step) Color.White.copy(alpha = .8f) else Color.White.copy(alpha = .2f))) }
        }
        Spacer(Modifier.height(18.dp))
        when (pl.p.id) {
            "candle" -> Flame(Modifier.size(170.dp, 230.dp))
            else -> if (lead != null) Creature(lead.species, maxOf(lead.stage(), Stage.BABY), Pose.PRACTICE, 170.dp, lead.shiny, lungs = Player.lungs, glow = false)
        }
        AnimatedContent(st.text, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "step") {
            Text(it, style = T.h2.copy(color = Color.White, fontSize = 22.sp, lineHeight = 30.sp), textAlign = TextAlign.Center, modifier = Modifier.padding(top = 20.dp))
        }
        val left = (st.secs - Player.stepT).toInt().coerceAtLeast(0)
        Box(Modifier.padding(top = 14.dp).width(160.dp).height(3.dp).clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = .15f))) {
            Box(Modifier.fillMaxWidth((Player.stepT / st.secs).coerceIn(0f, 1f)).height(3.dp).background(Color.White.copy(alpha = .6f))) }
        if (st.ask) {
            Field(line, { line = it }, tr("Можно записать…", "You can write it down…"), Modifier.padding(top = 18.dp).fillMaxWidth(), single = false)
            if (line.isNotBlank()) Text(tr("Сохранить и дальше", "Keep it and go on"), style = T.smallB.copy(color = Color(0xFF8FD0EA)), modifier = Modifier.padding(top = 10.dp).clickable {
                Player.notes += line.trim(); line = ""; Player.next() })
        }
        if (pl.p.id == "noting") Row(Modifier.padding(top = 18.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("think" to tr("Думаю", "Thinking"), "hear" to tr("Слышу", "Hearing"), "feel" to tr("Чувствую", "Feeling"), "body" to tr("Тело", "Body")).forEach { (k, l) ->
                val n = Player.notedN.let { Player.noted[k] ?: 0 }
                Column(Modifier.clip(RoundedCornerShape(14.dp)).background(Color.White.copy(alpha = .1f)).clickable { Player.note(k) }.padding(horizontal = 12.dp, vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(l, style = T.smallB.copy(color = Color.White)); Text("$n", style = T.tiny.copy(color = Color.White.copy(alpha = .6f))) }
            }
        }
        Spacer(Modifier.height(18.dp))
        Text(tr("Дальше", "Next") + " · ${clock(left)}", style = T.smallB.copy(color = Color.White.copy(alpha = .6f)), modifier = Modifier.clip(RoundedCornerShape(50)).border(1.dp, Color.White.copy(alpha = .25f), RoundedCornerShape(50))
            .clickable { if (st.ask && line.isNotBlank()) { Player.notes += line.trim(); line = "" }; Player.next() }.padding(horizontal = 18.dp, vertical = 9.dp))
        Spacer(Modifier.height(30.dp))
    }
}

/** Affirmations: a card at a time, said aloud (the phone listens) or just tapped. */
@Composable
private fun Affirm(d: Data, pl: Plan) {
    val ctx = LocalContext.current
    val cards = remember { Affirmations.deal(pl.p.deck.ifEmpty { "mix" }, d.affs, d.s.favs, today()).ifEmpty { Affirmations.deal("mix", d.affs, d.s.favs, today()) } }
    val a: Aff? = cards.getOrNull(Player.card)
    var heard by remember(Player.card) { mutableStateOf<Float?>(null) }
    val canListen = remember { Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).resolveActivity(ctx.packageManager) != null }
    fun said() { a?.let { saidList += it.text; Player.said++ }; next(cards.size) }
    val listen = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        val got = r.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull().orEmpty()
        val k = if (a == null) 0f else Affirmations.heard(a.text, got)
        heard = k
        if (k >= .6f) said()
    }
    if (a == null) { LaunchedEffect(Unit) { Player.finish(true) }; return }
    val lead = d.leader
    Column(Modifier.fillMaxSize().padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        if (lead != null) Creature(lead.species, maxOf(lead.stage(), Stage.BABY), if (Player.said > 0) Pose.JOY else Pose.IDLE, 130.dp, lead.shiny, lungs = Player.lungs, glow = false)
        Text("${Player.card + 1} / ${cards.size}", style = T.small.copy(color = Color.White.copy(alpha = .5f)), modifier = Modifier.padding(top = 10.dp))
        AnimatedContent(a.text, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "aff") {
            Text("«$it»", style = T.h2.copy(color = Color.White, fontSize = 24.sp, lineHeight = 32.sp, fontStyle = FontStyle.Italic), textAlign = TextAlign.Center, modifier = Modifier.padding(vertical = 22.dp))
        }
        heard?.let { if (it < .6f) Text(tr("Не расслышал. Попробуйте ещё раз или нажмите «Сказал{|а}».", "Didn't quite hear it. Try again or tap «Said it»."), style = T.small.copy(color = Color(0xFFFFC98A)), textAlign = TextAlign.Center) }
        Text(tr("Скажите вслух, медленно, как будто это правда. Это правда.", "Say it aloud, slowly, as if it were true. It is."), style = T.small.copy(color = Color.White.copy(alpha = .6f)), textAlign = TextAlign.Center)
        Row(Modifier.padding(top = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (canListen) Btn(tr("Сказать", "Speak"), color = Color.White, ink = Color(0xFF16263A)) {
                runCatching { listen.launch(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    .putExtra(RecognizerIntent.EXTRA_LANGUAGE, if (app.puffling.domain.Lang.ru) "ru-RU" else "en-GB").putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
                    .putExtra(RecognizerIntent.EXTRA_PROMPT, a.text)) } }
            Btn(tr("Сказал{|а}", "Said it"), color = Color(0x33FFFFFF), ink = Color.White) { said() }
        }
        Text(tr("Пропустить", "Skip"), style = T.small.copy(color = Color.White.copy(alpha = .5f)), modifier = Modifier.padding(top = 14.dp).clickable { next(cards.size) })
        if (Player.said > 0) Text(tr("В море ушло бутылок: ${Player.said}", "Bottles sent to sea: ${Player.said}"), style = T.tiny.copy(color = Color(0xFF8FD0EA)), modifier = Modifier.padding(top = 10.dp))
    }
}

private fun next(n: Int) { Player.card++; if (Player.card >= n) Player.finish(true) }

/** After the bell: how it is now, then save. */
@Composable
private fun AfterPanel(d: Data, pl: Plan, after: Int, onAfter: (Int) -> Unit, onSave: () -> Unit) {
    val lead = d.leader
    LaunchedEffect(Unit) { if (!d.s.askFeel || pl.p.id == "bell") onSave() }
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        if (lead != null) Creature(lead.species, maxOf(lead.stage(), Stage.BABY), Pose.JOY, 150.dp, lead.shiny, glow = false)
        Text(tr("Готово", "Done"), style = T.title.copy(color = Color.White), modifier = Modifier.padding(top = 10.dp))
        Text(mins(Player.t.toInt()), style = T.body.copy(color = Color.White.copy(alpha = .6f)))
        if (d.s.askFeel) {
            Text(tr("А как теперь?", "And how is it now?"), style = T.h3.copy(color = Color.White), modifier = Modifier.padding(top = 24.dp, bottom = 10.dp))
            FeelRow(after, light = true) { onAfter(if (after == it) 0 else it) }
        }
        Btn(tr("Сохранить", "Save"), Modifier.padding(top = 24.dp).fillMaxWidth(), color = Color.White, ink = Color(0xFF16263A)) { onSave() }
    }
}

/** What the session gave: food, shells, finds, eggs, quests. */
@Composable
fun ResultPage(r: Screen.Result, d: Data, nav: Nav) {
    val x = d.sessions.firstOrNull { it.id == r.id }
    val o = r.outcome
    val ctx = LocalContext.current
    Page(tr("Итог", "Result"), { nav.pop() }, action = { Box(Modifier.size(44.dp).clip(CircleShape).clickable { nav.share = app.puffling.ui.share.ShareReq(app.puffling.ui.share.CardKind.SESSION, r.id) }, contentAlignment = Alignment.Center) { Icon(Icons.SHARE, C.ink, 20.dp) } }) {
        app.puffling.ui.components.Hint("result", d.s.seen, app.puffling.ui.components.Icons.LEAF, tr("Итог практики", "After a practice"), tr("Питомцы получили своё спокойствие и подрастут ночью. Ракушки тратьте в чайной – нажмите на сцену на главной.", "The pets ate the calm and will grow tonight. Spend shells in the tea house – tap the scene on the home screen."), Modifier.padding(bottom = 10.dp))
        val lead = d.leader
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            if (lead != null) Creature(lead.species, maxOf(lead.stage(), Stage.BABY), Pose.JOY, 150.dp, lead.shiny)
            Text(x?.name ?: "", style = T.h2, textAlign = TextAlign.Center)
            Text(if (x != null) mins(x.secs) else "", style = T.small)
        }
        Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            app.puffling.ui.components.Tile("+" + fmt1(o.food), tr("спокойствия команде", "calm for the team"), Modifier.weight(1f), icon = Icons.LEAF, tint = C.ok)
            app.puffling.ui.components.Tile("+${o.shells}", tr("ракушек", "shells"), Modifier.weight(1f), icon = Icons.SHELL, tint = C.gold)
        }
        if (x != null && x.before > 0 && x.after > 0) Row(Modifier.padding(top = 10.dp).fillMaxWidth().card(16.dp).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(feelWord(x.before), style = T.bodyB.copy(color = feelColor(x.before))); Text("  →  ", style = T.body); Text(feelWord(x.after), style = T.bodyB.copy(color = feelColor(x.after)))
            Spacer(Modifier.weight(1f))
            Text(if (x.after < x.before) tr("стало тише", "calmer") else if (x.after == x.before) tr("ровно", "the same") else tr("бывает и так", "that happens too"), style = T.small)
        }
        if (o.depth > 0) Section(tr("Нырок", "The dive"))
        if (o.depth > 0) Column(Modifier.fillMaxWidth().card(16.dp).padding(14.dp)) {
            Text(tr("Глубже всего: ${o.depth} м", "Deepest: ${o.depth} m"), style = T.bodyB)
            x?.holdList()?.let { Text(it.joinToString(" · ") { h -> clock(h) }, style = T.small) }
            if (o.finds.isNotEmpty()) {
                Text(tr("Новое в аквариуме:", "New in the aquarium:"), style = T.smallB, modifier = Modifier.padding(top = 10.dp))
                androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    o.finds.forEach { id -> Column(horizontalAlignment = Alignment.CenterHorizontally) { app.puffling.ui.art.Picture("find_$id", 64.dp); Text(Deep.byId[id]?.name ?: "", style = T.tiny) } } }
            }
        }
        if (o.eggs.isNotEmpty()) { Section(tr("Найдено яйцо!", "An egg found!"))
            o.eggs.forEach { e -> Row(Modifier.fillMaxWidth().card(16.dp).clickable { nav.tab = 2; nav.stack.clear() }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                app.puffling.ui.art.EggView(e, 56.dp, 0f); Spacer(Modifier.width(12.dp))
                Column { Text(Pets.of(e).name, style = T.bodyB); Text(Pets.of(e).find, style = T.small) } } } }
        if (o.hatched.isNotEmpty()) o.hatched.forEach { h -> Text(tr("${h.name} вылупился!", "${h.name} hatched!"), style = T.h3.copy(color = C.acc), modifier = Modifier.padding(top = 12.dp)) }
        if (o.bonus.isNotEmpty()) { Section(tr("Способности", "Abilities")); o.bonus.forEach { Text("· $it", style = T.body) } }
        if (o.quests.isNotEmpty()) { Section(tr("Задания", "Quests")); o.quests.forEach { q -> Text(tr("✓ ${q.name} · +${q.shells} ракушек", "✓ ${q.name} · +${q.shells} shells"), style = T.body.copy(color = C.ok)) } }
        if (o.course) Text(tr("День курса пройден, +5 ракушек", "Course day done, +5 shells"), style = T.bodyB.copy(color = C.acc), modifier = Modifier.padding(top = 12.dp))
        if (o.bottles > 0) Text(tr("В море ушло бутылок: ${o.bottles}. Однажды их прибьёт обратно.", "Bottles sent to sea: ${o.bottles}. One day they'll wash back."), style = T.small, modifier = Modifier.padding(top = 12.dp))
        if (o.first && Practices.find(x?.practice ?: "") != null) Text(tr("Новая практика в копилке!", "A new practice tried!"), style = T.small.copy(color = C.ok), modifier = Modifier.padding(top = 6.dp))
        Text(tr("Питомцы получили своё спокойствие и подрастут ночью, пока спят.", "The pets ate this calm and will grow tonight while they sleep."), style = T.small, modifier = Modifier.padding(top = 16.dp))
        Btn(tr("Готово", "Done"), Modifier.fillMaxWidth().padding(top = 18.dp)) { nav.pop() }
        if (x?.practice == "bell") LaunchedEffect(Unit) { ctx.container.say(tr("Фонарик зажжён", "A lantern is lit")) }
    }
}
