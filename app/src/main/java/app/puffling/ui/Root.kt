package app.puffling.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.puffling.container
import app.puffling.data.Bell
import app.puffling.data.Bottle
import app.puffling.data.OwnAff
import app.puffling.data.OwnPractice
import app.puffling.data.Pet
import app.puffling.data.Session
import app.puffling.data.Settings
import app.puffling.domain.Outcome
import app.puffling.domain.Practices
import app.puffling.domain.dayOf
import app.puffling.domain.toPractice
import app.puffling.domain.today
import app.puffling.domain.tr
import app.puffling.domain.weekStart
import app.puffling.ui.components.Icon
import app.puffling.ui.components.Icons
import app.puffling.ui.home.AffirmationsPage
import app.puffling.ui.home.AquariumPage
import app.puffling.ui.home.HomeScreen
import app.puffling.ui.home.Inbox
import app.puffling.ui.home.SettingsPage
import app.puffling.ui.home.ShopPage
import app.puffling.ui.pets.PetPage
import app.puffling.ui.pets.PetsScreen
import app.puffling.ui.practice.BoltPage
import app.puffling.ui.practice.CoursePage
import app.puffling.ui.practice.LibraryScreen
import app.puffling.ui.practice.OwnPage
import app.puffling.ui.practice.QuickSheet
import app.puffling.ui.practice.ResultPage
import app.puffling.ui.practice.SessionScreen
import app.puffling.ui.practice.StartPage
import app.puffling.ui.share.CardKind
import app.puffling.ui.share.ShareReq
import app.puffling.ui.share.ShareSheet
import app.puffling.ui.stats.SessionPage
import app.puffling.ui.stats.StatsScreen
import app.puffling.ui.theme.C
import app.puffling.ui.theme.T
import kotlinx.coroutines.delay

/** Where a launch opens: a bell from its notification; in demo builds also a tab or a page by intent. */
object Start { var diaryTab = 0; var tab = 0; var open: String? = null; var bell by androidx.compose.runtime.mutableLongStateOf(0L) }

/** A page over the tabs. */
sealed class Screen {
    /** The practice page before it starts; [course] «basics:3» when it is a course day; [bell] the bell it answers. */
    data class Begin(val practice: String, val course: String = "", val bell: Long = 0) : Screen()
    data object Playing : Screen()
    data class Result(val id: Long, val outcome: Outcome) : Screen()
    data class SessionView(val id: Long) : Screen()
    data class PetInfo(val id: Long) : Screen()
    data object Shop : Screen()
    data object Settings : Screen()
    data class OwnEdit(val id: Long, val kind: String = "b") : Screen()
    data object Affirmations : Screen()
    data class Course(val id: String) : Screen()
    data object Aquarium : Screen()
    data object Bolt : Screen()
    /** A diary entry: 0 for a new one, opened on a family of feelings. */
    data class Entry(val id: Long, val mood: String = "") : Screen()
}

class Nav {
    var tab by mutableIntStateOf(0)
    val stack = mutableStateListOf<Screen>()
    var share by mutableStateOf<ShareReq?>(null)
    var quick by mutableStateOf(false)
    fun push(s: Screen) { stack.add(s) }
    fun pop() { if (stack.isNotEmpty()) stack.removeAt(stack.lastIndex) }
    fun replace(s: Screen) { pop(); push(s) }
}

/** Everything the screens read, collected once. */
class Data(val s: Settings, val sessions: List<Session>, val pets: List<Pet>, val own: List<OwnPractice>, val affs: List<OwnAff>, val bottles: List<Bottle>, val bells: List<Bell>,
           val entries: List<app.puffling.data.Entry> = emptyList()) {
    val team = pets.filter { it.slot in 0..2 }.sortedBy { it.slot }
    val leader = team.firstOrNull { it.slot == 0 }
    val real = sessions.filter { it.practice != "bell" && it.practice != "bolt" }
    val monday = weekStart(today())
    val week = sessions.filter { dayOf(it.start) >= monday }
    val weekMinutes = week.filter { it.practice != "bolt" }.sumOf { it.secs } / 60.0
    val tide = (weekMinutes / s.weekGoal.coerceAtLeast(1)).toFloat().coerceIn(0f, 1f)
    val ashore = bottles.filter { !it.opened && it.back <= today() }
    val bellsToday = bells.filter { dayOf(it.at) == today() }
    val first = sessions.minOfOrNull { dayOf(it.start) } ?: today()
}

@Composable
fun Root(s: Settings) {
    val ctx = LocalContext.current
    val c = ctx.container
    val sessions by c.dao.sessions().collectAsState(initial = emptyList())
    val pets by c.dao.pets().collectAsState(initial = emptyList())
    val own by c.dao.own().collectAsState(initial = emptyList())
    val affs by c.dao.affs().collectAsState(initial = emptyList())
    val bottles by c.dao.bottles().collectAsState(initial = emptyList())
    val bells by c.dao.bells().collectAsState(initial = emptyList())
    val entries by c.dao.entries().collectAsState(initial = emptyList())
    LaunchedEffect(own) { Practices.own = own.filter { !it.archived }.map { it.toPractice() } }
    app.puffling.domain.Gender.fem = s.fem
    val data = remember(s, sessions, pets, own, affs, bottles, bells, entries) { Data(s, sessions, pets, own, affs, bottles, bells, entries) }
    val nav = remember { Nav().apply { tab = Start.tab } }
    var note by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) { c.news.collect { note = it; delay(2600); note = null } }
    LaunchedEffect(Unit) { c.tick() }
    LaunchedEffect(Unit) { if (!app.puffling.BuildConfig.SHOTS) while (true) { c.refreshOutside(); delay(30 * 60_000L) } }
    // a session still playing (the app was closed and opened again) comes back to the front
    LaunchedEffect(Unit) { if (app.puffling.work.Player.plan != null && nav.stack.none { it is Screen.Playing }) nav.push(Screen.Playing) }
    LaunchedEffect(Start.bell) {
        if (Start.bell != 0L) { val id = Start.bell; Start.bell = 0
            if (app.puffling.work.Player.plan == null) { nav.stack.clear(); app.puffling.ui.practice.launch(ctx, data, nav, "bell", bell = id) } }
    }
    LaunchedEffect(Unit) {
        val o = Start.open
        when {
            o == null -> {}
            o == "shop" -> nav.push(Screen.Shop); o == "settings" -> nav.push(Screen.Settings); o == "aquarium" -> nav.push(Screen.Aquarium)
            o == "affirmations" -> nav.push(Screen.Affirmations); o == "bolt" -> nav.push(Screen.Bolt)
            o == "quick" -> nav.quick = true
            o == "summary" -> Start.diaryTab = 1
            o.startsWith("entry:") -> nav.push(Screen.Entry(o.drop(6).toLongOrNull() ?: 0, if (o.drop(6).toLongOrNull() == null) o.drop(6) else ""))
            o.startsWith("begin:") -> nav.push(Screen.Begin(o.drop(6)))
            o.startsWith("course:") -> nav.push(Screen.Course(o.drop(7)))
            o.startsWith("pet:") -> nav.push(Screen.PetInfo(o.drop(4).toLong()))
            o.startsWith("session:") -> nav.push(Screen.SessionView(o.drop(8).toLong()))
            o.startsWith("own:") -> nav.push(Screen.OwnEdit(0, o.drop(4)))
            o.startsWith("share:") -> nav.share = ShareReq(CardKind.valueOf(o.drop(6).substringBefore(':')), o.drop(6).substringAfter(':', "0").toLong())
        }
        Start.open = null
    }

    Box(Modifier.fillMaxSize().background(C.bg)) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f).statusBarsPadding()) {
                when (nav.tab) {
                    0 -> HomeScreen(data, nav)
                    1 -> LibraryScreen(data, nav)
                    2 -> PetsScreen(data, nav)
                    else -> app.puffling.ui.diary.DiaryScreen(data, nav)
                }
            }
            Dock(nav.tab, { nav.tab = it }) { nav.quick = true }
        }
        for (scr in nav.stack) Box(Modifier.fillMaxSize()) {
            when (scr) {
                is Screen.Begin -> StartPage(scr, data, nav)
                Screen.Playing -> SessionScreen(data, nav)
                is Screen.Result -> ResultPage(scr, data, nav)
                is Screen.SessionView -> SessionPage(scr.id, data, nav)
                is Screen.PetInfo -> PetPage(scr.id, data, nav)
                Screen.Shop -> ShopPage(data, nav)
                Screen.Settings -> SettingsPage(data, nav)
                is Screen.OwnEdit -> OwnPage(scr.id, scr.kind, data, nav)
                Screen.Affirmations -> AffirmationsPage(data, nav)
                is Screen.Course -> CoursePage(scr.id, data, nav)
                Screen.Aquarium -> AquariumPage(data, nav)
                Screen.Bolt -> BoltPage(data, nav)
                is Screen.Entry -> app.puffling.ui.diary.EntryPage(scr, data, nav)
            }
        }
        if (nav.stack.isEmpty()) Inbox(data, nav)
        if (nav.quick) QuickSheet(data, nav)
        nav.share?.let { ShareSheet(it, data) { nav.share = null } }
        AnimatedVisibility(note != null, Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 12.dp, start = 18.dp, end = 18.dp), enter = fadeIn(), exit = fadeOut()) {
            Text(note ?: "", style = T.bodyB.copy(color = C.bg), modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(C.ink).padding(horizontal = 16.dp, vertical = 11.dp))
        }
    }
}

@Composable
private fun Dock(sel: Int, onSel: (Int) -> Unit, onGo: () -> Unit) {
    val tabs = listOf(tr("Чайная", "Tea house") to Icons.HOME, tr("Практики", "Practices") to Icons.LOTUS, tr("Питомцы", "Pets") to Icons.PAW, tr("Дневник", "Diary") to Icons.BOOK)
    Row(Modifier.fillMaxWidth().shadow(12.dp, ambientColor = C.shadow, spotColor = C.shadow).background(C.nav).navigationBarsPadding().padding(top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically) {
        tabs.forEachIndexed { i, (label, ic) ->
            if (i == 2) Box(Modifier.weight(1.1f), contentAlignment = Alignment.Center) {
                Box(Modifier.size(56.dp).clip(CircleShape).background(C.nav).padding(3.dp).clip(CircleShape).background(C.acc).clickable(onClick = onGo),
                    contentAlignment = Alignment.Center) { Icon(Icons.WIND, C.accInk, 28.dp) }
            }
            val col = if (i == sel) C.acc else C.mute
            Column(Modifier.weight(1f).clickable(remember { MutableInteractionSource() }, null) { onSel(i) }, horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(ic, col, 24.dp)
                Text(label, style = T.tiny.copy(color = col), modifier = Modifier.padding(top = 2.dp))
            }
        }
    }
}
