package app.puffling.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.puffling.container
import app.puffling.domain.Courses
import app.puffling.domain.Lang
import app.puffling.domain.Pets
import app.puffling.domain.Stage
import app.puffling.domain.today
import app.puffling.domain.tr
import app.puffling.ui.art.Creature
import app.puffling.ui.art.EggView
import app.puffling.ui.art.Pose
import app.puffling.ui.components.Btn
import app.puffling.ui.components.Chip
import app.puffling.ui.components.Segments
import app.puffling.ui.theme.C
import app.puffling.ui.theme.T
import app.puffling.work.Bells
import kotlinx.coroutines.launch

/** What the user comes for: each suggests a course. */
private val GOALS = listOf(
    Triple("calm", "Спокойнее реагировать", "React more calmly") to "basics",
    Triple("anxiety", "Меньше тревоги", "Less anxiety") to "calm",
    Triple("sleep", "Лучше спать", "Sleep better") to "sleep",
    Triple("energy", "Бодрость и холод", "Energy and cold") to "wimhof",
    Triple("focus", "Ясная голова", "A clear head") to "silence",
    Triple("kind", "Добрее к себе", "Kinder to myself") to "kind",
)

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun Onboarding() {
    val c = LocalContext.current.container
    var page by remember { mutableIntStateOf(0) }
    var goals by remember { mutableStateOf(setOf<String>()) }
    var egg by remember { mutableStateOf("cat") }
    var goal by remember { mutableIntStateOf(60) }
    var bells by remember { mutableStateOf(true) }
    var fem by remember { mutableStateOf(false) }
    var course by remember { mutableStateOf("basics") }
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { finish(c, goals, egg, goal, bells, fem, course) }
    Column(Modifier.fillMaxSize().background(C.bg).statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.padding(bottom = 18.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            (0..3).forEach { Box(Modifier.size(if (it == page) 22.dp else 8.dp, 8.dp).clip(RoundedCornerShape(50)).background(if (it <= page) C.acc else C.line)) } }
        AnimatedContent(page, label = "page") { pg ->
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                when (pg) {
                    0 -> {
                        Creature("puffin", Stage.ADULT, Pose.PRACTICE, 220.dp)
                        Text(tr("Привет! Я тупичок", "Hi! I'm a puffling"), style = T.title, textAlign = TextAlign.Center)
                        Text(tr("Я живу в маленькой чайной на морской скале. Когда вы дышите, я дышу вместе с вами: раздуваюсь на вдохе и сдуваюсь на выдохе.\n\nПрактики – дыхание, тишина, Вим Хоф, колокольчик, добрые слова – кормят меня спокойствием, а расту я ночью, пока сплю.",
                            "I live in a little tea house on a sea cliff. When you breathe, I breathe with you: puffing up on the in-breath and settling on the out-breath.\n\nPractices – breathing, silence, Wim Hof, the bell, kind words – feed me calm, and I grow at night while I sleep."),
                            style = T.body, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 12.dp))
                    }
                    1 -> {
                        Text(tr("Зачем вы здесь?", "What brings you here?"), style = T.title, textAlign = TextAlign.Center)
                        Text(tr("Можно выбрать несколько. Подберу первый курс – его всегда можно сменить.", "Pick any. I'll suggest a first course – you can always change it."), style = T.small, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp, bottom = 14.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            GOALS.forEach { (g, co) -> val on = g.first in goals
                                Chip(tr(g.second, g.third), on) { goals = if (on) goals - g.first else goals + g.first; if (!on) course = co } } }
                        val co = Courses.byId[course]
                        if (co != null) Column(Modifier.padding(top = 20.dp).fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Color(co.color).copy(alpha = .14f)).padding(16.dp)) {
                            Text(tr("Первый курс: ${co.name}", "First course: ${co.name}"), style = T.bodyB)
                            Text(co.about, style = T.small)
                            Text(tr("Без курса", "No course"), style = T.smallB.copy(color = C.mute), modifier = Modifier.padding(top = 8.dp).clickable { course = "" })
                        } else Text(tr("Без курса – просто практики по настроению.", "No course – just practices by mood."), style = T.small, modifier = Modifier.padding(top = 20.dp))
                    }
                    2 -> {
                        Text(tr("Кто будет рядом?", "Who'll keep you company?"), style = T.title, textAlign = TextAlign.Center)
                        Text(tr("Возьмите одно яйцо – оно вылупится после первых практик. Остальных найдёте по тому, как практикуете.", "Take one egg – it hatches after your first practices. You'll find the rest by how you practise."), style = T.small, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp, bottom = 14.dp))
                        Pets.starters.forEach { sp -> val on = egg == sp.id
                            Row(Modifier.padding(bottom = 10.dp).fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(if (on) C.accSoft else C.card)
                                .border(if (on) 2.dp else 0.dp, if (on) C.acc else Color.Transparent, RoundedCornerShape(18.dp)).clickable { egg = sp.id }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                EggView(sp.id, 64.dp, if (on) .6f else 0f)
                                Column(Modifier.weight(1f).padding(start = 12.dp)) { Text(sp.name, style = T.bodyB); Text(sp.ability, style = T.small) }
                            } }
                    }
                    else -> {
                        Text(tr("Ваш ритм", "Your rhythm"), style = T.title, textAlign = TextAlign.Center)
                        Text(tr("Сколько минут в неделю – тогда море в окне поднимется до прилива. Каждый день не обязательно.", "Minutes a week to bring the sea in the window to high tide. Not every day is needed."), style = T.small, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp, bottom = 12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { listOf(30, 45, 60, 90, 120).forEach { n -> Chip("$n", goal == n) { goal = n } } }
                        Text(tr("Колокольчик внимания", "The mindfulness bell"), style = T.h3, modifier = Modifier.padding(top = 22.dp))
                        Text(tr("Четыре раза в день, с 9 до 21, в случайный момент тихо зазвонит: остановитесь на три вдоха. Настраивается.", "Four times a day, 9 to 21, at a random moment it rings softly: stop for three breaths. You can tune it."), style = T.small, textAlign = TextAlign.Center, modifier = Modifier.padding(vertical = 8.dp))
                        Segments(listOf(tr("пусть звонит", "let it ring"), tr("не надо", "no thanks")), if (bells) 0 else 1) { bells = it == 0 }
                        if (Lang.ru) {
                            Text("Как к вам обращаться", style = T.h3, modifier = Modifier.padding(top = 22.dp))
                            Text("Чтобы аффирмации звучали от вашего лица: «я благодарен» или «я благодарна»", style = T.small, textAlign = TextAlign.Center, modifier = Modifier.padding(vertical = 8.dp))
                            Segments(listOf("в мужском роде", "в женском роде"), if (fem) 1 else 0) { fem = it == 1 }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(26.dp))
        Btn(if (page < 3) tr("Дальше", "Next") else tr("Начать", "Begin"), Modifier.fillMaxWidth()) {
            if (page < 3) page++
            else if (bells && Build.VERSION.SDK_INT >= 33) ask.launch(Manifest.permission.POST_NOTIFICATIONS)
            else finish(c, goals, egg, goal, bells, fem, course)
        }
        if (page > 0) Text(tr("Назад", "Back"), style = T.smallB.copy(color = C.mute), modifier = Modifier.padding(top = 14.dp).clickable { page-- })
    }
}

private fun finish(c: app.puffling.AppContainer, goals: Set<String>, egg: String, goal: Int, bells: Boolean, fem: Boolean, course: String) {
    c.scope.launch {
        if (c.dao.petList().isEmpty()) {
            c.dao.insert(c.newPet("puffin", 0, hatched = today()).copy(shiny = false))
            c.dao.insert(c.newPet(egg, 1))
        }
        app.puffling.domain.Gender.fem = fem
        c.settings.update { it.copy(onboarded = true, goals = goals, weekGoal = goal, bells = bells, fem = fem, course = course, courseDay = 0, digested = today() - 1, shells = 20) }
        Bells.schedule(c.app, c.settings.now())
    }
}
