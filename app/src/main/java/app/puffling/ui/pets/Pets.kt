package app.puffling.ui.pets

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import app.puffling.ui.components.Icons
import app.puffling.ui.components.Icon
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.puffling.container
import app.puffling.data.Pet
import app.puffling.domain.Element
import app.puffling.domain.Engine
import app.puffling.domain.Form
import app.puffling.domain.Pets
import app.puffling.domain.Stage
import app.puffling.domain.Tally
import app.puffling.domain.elMap
import app.puffling.domain.count
import app.puffling.domain.fmt
import app.puffling.domain.fmt1
import app.puffling.domain.fmtDayYear
import app.puffling.domain.nat
import app.puffling.domain.sp
import app.puffling.domain.stage
import app.puffling.domain.tr
import app.puffling.ui.Data
import app.puffling.ui.Nav
import app.puffling.ui.Screen
import app.puffling.ui.art.Art
import app.puffling.ui.art.Creature
import app.puffling.ui.art.PetView
import app.puffling.ui.art.Pose
import app.puffling.ui.components.Btn
import app.puffling.ui.components.Card
import app.puffling.ui.components.Field
import app.puffling.ui.components.Ghost
import app.puffling.ui.components.Meter
import app.puffling.ui.components.Page
import app.puffling.ui.components.Section
import app.puffling.ui.components.Segments
import app.puffling.ui.components.card
import app.puffling.ui.share.CardKind
import app.puffling.ui.share.ShareReq
import app.puffling.ui.theme.C
import app.puffling.ui.theme.T

@Composable
fun PetsScreen(d: Data, nav: Nav) {
    val c = LocalContext.current.container
    var tab by remember { mutableStateOf(0) }
    var pick by remember { mutableStateOf<Int?>(null) }
    val tally = remember(d.sessions, d.bells, d.s) { Tally(d.sessions, d.bells, d.s, d.entries) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 30.dp)) {
        Row(Modifier.padding(top = 14.dp, start = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(tr("Питомцы", "Pets"), style = T.title)
                Text(tr("${d.pets.size} из ${Pets.all.size} найдено", "${d.pets.size} of ${Pets.all.size} found"), style = T.small)
            }
            Box(Modifier.size(44.dp).clip(CircleShape).clickable { nav.share = ShareReq(if (tab == 0) CardKind.HOUSE else CardKind.BESTIARY) },
                contentAlignment = Alignment.Center) { Icon(Icons.SHARE, C.ink, 22.dp) }
        }
        app.puffling.ui.components.Hint("pets", d.s.seen, Icons.PAW, tr("Ваши питомцы", "Your pets"), tr("Ведущий дышит с вами и ест всё спокойствие, двое рядом – по половине, и способности всех троих складываются. Нажмите на питомца, чтобы поменять команду. Новые яйца находятся по тому, как вы практикуете, – кого и как искать, написано в бестиарии.",
            "The leader breathes with you and eats all the calm, the two beside it half each, and their abilities add up. Tap a pet to change the team. New eggs turn up from how you practise – the bestiary tells who and how."), Modifier.padding(top = 10.dp))
        Segments(listOf(tr("Мои", "Mine"), tr("Бестиарий", "Bestiary")), tab, Modifier.padding(top = 10.dp)) { tab = it }
        if (tab == 0) {
            TeamStage(d, nav) { pick = it }
            val home = d.pets.filter { it.slot !in 0..2 && it.hatched > 0 }.sortedByDescending { it.growth }
            val eggs = d.pets.filter { it.slot !in 0..2 && it.hatched == 0L }.sortedByDescending { it.growth }
            if (home.isNotEmpty()) {
                Section(icon = Icons.HOME, text = tr("Дома", "At home") + " · ${home.size}")
                home.chunked(2).forEach { row ->
                    Row(Modifier.padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEach { p -> PetTile(p, Modifier.weight(1f)) { nav.push(Screen.PetInfo(p.id)) } }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
            if (eggs.isNotEmpty()) {
                Section(icon = Icons.EGG, text = tr("Яйца", "Eggs") + " · ${eggs.size}")
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    eggs.forEach { p -> Column(Modifier.width(96.dp).card(16.dp).clickable { nav.push(Screen.PetInfo(p.id)) }.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        PetView(p, 72.dp)
                        Meter((p.growth / Pets.HATCH).toFloat(), Modifier.padding(top = 4.dp), h = 5.dp)
                        Text("${((p.growth / Pets.HATCH) * 100).toInt().coerceAtMost(99)}%", style = T.tiny)
                    } }
                }
                Text(tr("Дома яйца греются на четверти спокойствия команды. В команде вылупятся быстрее.", "Eggs at home warm on a quarter of the team's calm; on the team they hatch faster."), style = T.tiny, modifier = Modifier.padding(top = 6.dp))
            }
            if (home.isEmpty() && eggs.isEmpty()) Text(tr("Новые яйца найдутся по тому, как вы практикуете. Загляните в бестиарий – там написано, кого и как искать.",
                "New eggs turn up from the way you practise. The bestiary tells who and how."), style = T.small, modifier = Modifier.padding(top = 14.dp))
        } else Bestiary(d, tally, nav)
    }
    pick?.let { sl -> SlotPicker(sl, d, { p -> c.write { setSlot(p, sl) }; pick = null }) { pick = null } }
}

/** The team standing together on a soft stage: the leader in the middle, the companions either side. */
@Composable
private fun TeamStage(d: Data, nav: Nav, onEmpty: (Int) -> Unit) {
    val leadColor = d.leader?.let { Color(it.sp().color) } ?: C.acc
    Column(Modifier.padding(top = 14.dp).fillMaxWidth().clip(RoundedCornerShape(24.dp))
        .background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(leadColor.copy(alpha = .22f), C.card))).padding(14.dp)) {
        Text(tr("Команда", "The team"), style = T.h3)
        Text(tr("Ведущий дышит с вами, двое рядом – за ним", "The leader breathes with you, the two beside follow"), style = T.small)
        Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.Bottom) {
            listOf(1, 0, 2).forEach { slot -> val p = d.pets.firstOrNull { it.slot == slot }
                val sz = if (slot == 0) 128.dp else 92.dp
                Column(Modifier.weight(if (slot == 0) 1.3f else 1f).clip(RoundedCornerShape(16.dp)).clickable { if (p != null) nav.push(Screen.PetInfo(p.id)) else onEmpty(slot) }.padding(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    if (p != null) { PetView(p, sz, Pose.IDLE); Text(p.name, style = T.bodyB, maxLines = 1); Text(p.stage().label, style = T.tiny) }
                    else { Box(Modifier.size(sz * .8f).clip(CircleShape).background(C.card2), contentAlignment = Alignment.Center) { Text("+", style = T.title.copy(color = C.mute)) }
                        Text(tr("свободно", "free"), style = T.small) }
                }
            }
        }
        val bonuses = d.team.mapNotNull { it.sp().likes?.label?.lowercase() }.distinct()
        if (d.team.isNotEmpty()) Text(tr("Команда любит: ", "The team loves: ") + (bonuses.ifEmpty { listOf(tr("особые способности", "special abilities")) }).joinToString(", "),
            style = T.small.copy(color = C.ink), modifier = Modifier.padding(top = 8.dp))
    }
}

/** A pet resting at home, as a card in the grid. */
@Composable
private fun PetTile(p: Pet, modifier: Modifier, onClick: () -> Unit) {
    Column(modifier.card(18.dp).clickable(onClick = onClick).padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color(p.sp().color).copy(alpha = .14f)), contentAlignment = Alignment.Center) { PetView(p, 104.dp) }
        Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(p.name, style = T.bodyB, maxLines = 1)
            if (p.shiny) Text(" ★", style = T.smallB.copy(color = C.gold))
        }
        Text("${p.sp().name} · ${p.stage().label}", style = T.tiny, maxLines = 1)
        Meter(Stage.progress(p), Modifier.padding(top = 6.dp), h = 5.dp)
    }
}

/** All twenty, in tiles of the same size: found ones alive, the rest as shadows with how to find them. */
@Composable
private fun Bestiary(d: Data, tally: Tally, nav: Nav) {
    Spacer(Modifier.height(12.dp))
    Pets.all.chunked(2).forEach { row ->
        Row(Modifier.padding(bottom = 10.dp).height(238.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            row.forEach { sp -> val owned = d.pets.firstOrNull { it.species == sp.id }
                Column(Modifier.weight(1f).fillMaxHeight().card(18.dp).clickable { if (owned != null) nav.push(Screen.PetInfo(owned.id)) }.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.fillMaxWidth().height(100.dp).clip(RoundedCornerShape(14.dp)).background(if (owned != null) Color(sp.color).copy(alpha = .14f) else C.card2),
                        contentAlignment = Alignment.Center) { if (owned != null) PetView(owned, 96.dp) else Silhouette(sp.id) }
                    Text(if (owned != null) sp.name else "???", style = T.bodyB, modifier = Modifier.padding(top = 6.dp), maxLines = 1)
                    Text(if (sp.legendary) tr("легенда", "legend") else if (owned != null) owned.stage().label else tr("не найден", "not found"),
                        style = T.tiny.copy(color = if (sp.legendary) C.gold else C.mute))
                    Spacer(Modifier.weight(1f))
                    if (owned == null) { val (f, txt) = Engine.near(sp.id, tally)
                        Text(sp.find, style = T.tiny, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
                        Meter(f, Modifier.padding(top = 5.dp), h = 5.dp); Text(txt, style = T.tiny, maxLines = 1) }
                    else { Text(tr("найден ${fmtDayYear(owned.found)}", "found ${fmtDayYear(owned.found)}"), style = T.tiny, modifier = Modifier.padding(top = 6.dp))
                        Meter(Stage.progress(owned), Modifier.padding(top = 5.dp), h = 5.dp, color = C.ok) }
                }
            }
            if (row.size == 1) Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun SlotPicker(slot: Int, d: Data, onPick: (Pet) -> Unit, onClose: () -> Unit) {
    app.puffling.ui.components.Sheet(onClose) {
        Text(if (slot == 0) tr("Кто поведёт команду?", "Who leads the team?") else tr("Кто встанет рядом?", "Who stands beside?"), style = T.h2)
        Spacer(Modifier.height(10.dp))
        val free = d.pets.filter { it.slot !in 0..2 }.sortedWith(compareBy({ it.hatched == 0L }, { -it.growth }))
        if (free.isEmpty()) Text(tr("Все питомцы уже в команде", "Every pet is already on the team"), style = T.body)
        free.chunked(3).forEach { row -> Row(Modifier.padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            row.forEach { p -> Column(Modifier.weight(1f).card(16.dp).clickable { onPick(p) }.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                PetView(p, 72.dp); Text(p.name, style = T.smallB, maxLines = 1); Text(p.sp().name, style = T.tiny, maxLines = 1) } }
            repeat(3 - row.size) { Spacer(Modifier.weight(1f)) } } }
    }
}

@Composable
private fun Silhouette(species: String) {
    val img = Art.pet(LocalContext.current, species, 2)
    if (img != null) androidx.compose.foundation.Image(img, null, Modifier.size(84.dp).alpha(.45f), colorFilter = ColorFilter.tint(C.mute))
    else Text("?", style = T.title.copy(color = C.mute))
}

@Composable
fun PetPage(id: Long, d: Data, nav: Nav) {
    val c = LocalContext.current.container
    val p = d.pets.firstOrNull { it.id == id } ?: run { if (d.pets.isNotEmpty()) nav.pop(); return }
    val st = p.stage(); val sp = p.sp()
    val col = Color(sp.color).let { if (it.luminance() < .3f) C.acc else it }
    var rename by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf(p.name) }
    var pose by remember { mutableStateOf(Pose.IDLE) }
    Page("", { nav.pop() }) {
        app.puffling.ui.components.Hint("pet", d.s.seen, app.puffling.ui.components.Icons.PAW, tr("Питомец", "Your pet"), tr("Нажмите на него – покажет позы. Внизу выберите, где он: ведёт команду, рядом или отдыхает дома.", "Tap it to see its poses. Below, choose its place: leading, beside, or resting at home."), Modifier.padding(bottom = 10.dp))
        // the hero: the pet big, its name and kind under it, on its own colour
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(col.copy(alpha = .28f), col.copy(alpha = .06f))))
            .clickable { if (st != Stage.EGG) pose = Pose.entries[(pose.ordinal + 1) % Pose.entries.size] }.padding(top = 10.dp, bottom = 18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            if (st == Stage.EGG) PetView(p, 190.dp) else Creature(p.species, st, pose, 210.dp, p.shiny)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(p.name, style = T.title)
                if (p.shiny) Text("  ★", style = T.h3.copy(color = C.gold))
                Box(Modifier.padding(start = 4.dp).size(34.dp).clip(CircleShape).clickable { rename = true }, contentAlignment = Alignment.Center) { Icon(Icons.EDIT, C.mute, 16.dp) }
            }
            Text(sp.name + (if (p.shiny) tr(" · редкий окрас", " · rare colouring") else ""), style = T.body.copy(color = C.mute))
            if (st != Stage.EGG) Text(tr("нажмите – покажет, что умеет", "tap – it shows what it can do"), style = T.tiny, modifier = Modifier.padding(top = 4.dp))
        }
        // growth as four steps
        Column(Modifier.padding(top = 12.dp).fillMaxWidth().card(20.dp).padding(16.dp)) {
            if (st == Stage.EGG) {
                Text(tr("Яйцо", "An egg"), style = T.h3)
                Meter((p.growth / Pets.HATCH).toFloat(), Modifier.padding(top = 8.dp), color = col, h = 8.dp)
                Text(tr("Вылупится от практик. В команде – быстрее.", "Practice hatches it. On the team – faster."), style = T.small, modifier = Modifier.padding(top = 8.dp))
            } else {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    listOf(Stage.BABY, Stage.TEEN, Stage.ADULT, Stage.MASTER).forEach { s -> val reached = st >= s
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(Modifier.size(if (s == st) 16.dp else 12.dp).clip(CircleShape).background(if (reached) col else C.track))
                            Text(s.label, style = (if (s == st) T.smallB else T.tiny).copy(color = if (reached) C.ink else C.mute), modifier = Modifier.padding(top = 4.dp)) } }
                }
                if (st != Stage.MASTER) Meter(Stage.progress(p), Modifier.padding(top = 12.dp), color = col, h = 8.dp)
                Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(count(p.sessions, "практика вместе", "практики вместе", "практик вместе", "practice together", "practices together"), style = T.small, modifier = Modifier.weight(1f))
                    if (p.belly >= 1) Text(tr("+${fmt1(p.belly)} этой ночью", "+${fmt1(p.belly)} tonight"), style = T.smallB.copy(color = col))
                }
            }
        }
        InfoCard(Icons.SPARK, col, tr("Способность", "Ability"), sp.likes?.label, sp.ability)
        InfoCard(Icons.HEART, C.bad, tr("Характер", "Nature"), p.nat().label, p.nat().about)
        Form.of(p.form)?.let { InfoCard(Icons.LEAF, C.ok, tr("Путь", "Way"), it.label, it.about) }
        val els = p.elMap().filterValues { it > 0 }
        if (els.isNotEmpty()) Column(Modifier.padding(top = 10.dp).fillMaxWidth().card(20.dp).padding(16.dp)) {
            Text(tr("На чём рос", "What it grew on"), style = T.h3)
            val total = els.values.sum()
            els.entries.sortedByDescending { it.value }.take(4).forEach { (k, v) -> val el = Element.of(k)
                Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(el.label, style = T.body, modifier = Modifier.width(96.dp)); Meter((v / total).toFloat(), Modifier.weight(1f), color = Color(el.color), h = 8.dp)
                    Text("${(v / total * 100).toInt()}%", style = T.smallB, modifier = Modifier.width(48.dp), textAlign = TextAlign.End) } }
            if (st < Stage.ADULT) Text(tr("Повзрослев, станет: ${Form.from(els).label.lowercase()}", "When grown, it becomes: ${Form.from(els).label.lowercase()}"), style = T.small, modifier = Modifier.padding(top = 10.dp))
        }
        Text(tr("МЕСТО", "PLACE"), style = T.sec, modifier = Modifier.padding(top = 18.dp, bottom = 8.dp))
        Segments(listOf(tr("Ведущий", "Leader"), tr("Слева", "Left"), tr("Справа", "Right"), tr("Дома", "Home")), when (p.slot) { 0 -> 0; 1 -> 1; 2 -> 2; else -> 3 }) { i ->
            c.write { setSlot(p, listOf(0, 1, 2, -1)[i]) } }
        if (st != Stage.EGG) Btn(tr("Поделиться открыткой", "Share a card"), Modifier.fillMaxWidth().padding(top = 16.dp)) { nav.share = ShareReq(CardKind.PET, p.id) }
        Text(tr("Найден ${fmtDayYear(p.found)}", "Found ${fmtDayYear(p.found)}") + (if (p.hatched > 0) tr(", вылупился ${fmtDayYear(p.hatched)}", ", hatched ${fmtDayYear(p.hatched)}") else ""),
            style = T.tiny, modifier = Modifier.fillMaxWidth().padding(top = 12.dp), textAlign = TextAlign.Center)
    }
    if (rename) app.puffling.ui.components.Popup({ rename = false }) {
        Text(tr("Как зовут?", "What's its name?"), style = T.h2)
        Field(name, { name = it }, tr("Имя", "Name"), Modifier.fillMaxWidth().padding(top = 10.dp))
        Btn(tr("Сохранить", "Save"), Modifier.fillMaxWidth().padding(top = 12.dp), enabled = name.isNotBlank()) { c.write { dao.update(p.copy(name = name.trim())) }; rename = false }
    }
}

/** One fact about a pet: an icon, what it is, its name in bold and a plain line of what it means. */
@Composable
private fun InfoCard(icon: Icons, tint: Color, label: String, value: String?, about: String) {
    Row(Modifier.padding(top = 10.dp).fillMaxWidth().card(20.dp).padding(16.dp), verticalAlignment = Alignment.Top) {
        app.puffling.ui.components.Badge(icon, tint, 40.dp)
        Column(Modifier.weight(1f).padding(start = 14.dp)) {
            Text(label.uppercase(), style = T.sec)
            if (value != null) Text(value, style = T.h3, modifier = Modifier.padding(top = 2.dp))
            Text(about, style = T.body, modifier = Modifier.padding(top = 2.dp))
        }
    }
}
