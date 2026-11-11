package app.puffling

import android.app.Application
import android.content.Context
import app.puffling.data.Bottle
import app.puffling.data.Db
import app.puffling.data.Outside
import app.puffling.data.OutsideSource
import app.puffling.data.OwnPractice
import app.puffling.data.Pet
import app.puffling.data.Session
import app.puffling.data.SettingsStore
import app.puffling.domain.Courses
import app.puffling.domain.Diary
import app.puffling.domain.Deep
import app.puffling.domain.Engine
import app.puffling.domain.Gender
import app.puffling.domain.Kind
import app.puffling.domain.Lang
import app.puffling.domain.Nature
import app.puffling.domain.Outcome
import app.puffling.domain.Pets
import app.puffling.domain.Practices
import app.puffling.domain.Quests
import app.puffling.domain.Shop
import app.puffling.domain.Tally
import app.puffling.domain.Week
import app.puffling.domain.dayOf
import app.puffling.domain.today
import app.puffling.domain.toPractice
import app.puffling.domain.weekStart
import app.puffling.work.Bells
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.random.Random

/** What a session left behind, from the player, to be saved. */
data class Done(
    val practice: String, val start: Long, val secs: Int, val rounds: Int = 0, val holds: List<Int> = emptyList(), val bpm: Double = 0.0,
    val before: Int = 0, val after: Int = 0, val notes: List<String> = emptyList(), val said: List<String> = emptyList(),
    val noted: Map<String, Int> = emptyMap(), val course: String = "", val bell: Long = 0,
)

class AppContainer(context: Context) {
    val app: Context = context.applicationContext
    val db = Db.create(app)
    val dao = db.dao()
    val settings = SettingsStore(app)
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    val news = MutableSharedFlow<String>(extraBufferCapacity = 8)
    private val lock = Mutex()

    fun write(block: suspend AppContainer.() -> Unit) { scope.launch { block() } }
    fun say(s: String) { news.tryEmit(s) }

    suspend fun loadOwn() { Practices.own = dao.ownList().filter { !it.archived }.map { it.toPractice() } }

    /**
     * Saves a finished session and works out what it gave: food for the team, shells, eggs found,
     * sea finds, quests, bottles sent, a course day. Returns its id and the outcome.
     */
    suspend fun finish(d: Done): Pair<Long, Outcome> = lock.withLock {
        val p = Practices.of(d.practice)
        val s = settings.now()
        val before = dao.sessionList()
        val x0 = Session(start = d.start, secs = d.secs.coerceAtLeast(1), practice = d.practice, kind = p.kind.code, el = p.el.id, rounds = d.rounds,
            holds = d.holds.joinToString(","), bpm = d.bpm, before = d.before, after = d.after, notes = d.notes.filter { it.isNotBlank() }.joinToString("\n"),
            said = d.said.size, noted = d.noted.entries.joinToString(",") { "${it.key}:${it.value}" }, course = d.course, name = p.name)
        val pets = dao.petList()
        val (out0, fed) = Engine.feed(x0, before, pets, s)
        dao.updatePets(fed)
        val leader = Engine.team(fed).firstOrNull()?.species.orEmpty()
        val bells = dao.bellList()
        if (d.bell > 0) bells.firstOrNull { it.id == d.bell }?.let { dao.update(it.copy(answered = System.currentTimeMillis())) }
        // sea finds from the deepest hold
        val newFinds = Deep.reached(d.holds.maxOrNull() ?: 0).map { it.id }.filter { it !in s.finds }
        // course day
        val courseDay = d.course.isNotEmpty() && s.course == d.course.substringBefore(':') && s.courseLast != today()
        var shells = out0.shells + (if (courseDay) 5 else 0) + newFinds.size * 3
        val x = x0.copy(food = out0.food, leader = leader, depth = out0.depth, shells = shells)
        val id = dao.insert(x)
        val all = before + x.copy(id = id)
        // eggs
        val bellsNow = dao.bellList()
        val entries = dao.entryList()
        val eggs = Engine.finds(Tally(all, bellsNow, s, entries), fed.map { it.species }.toSet())
        val freeSlots = (0..2).filter { sl -> fed.none { it.slot == sl } }.toMutableList()
        for (e in eggs) dao.insert(newPet(e, freeSlots.removeFirstOrNull() ?: -1))
        // bottles: every phrase said aloud sails off and washes back in two to six weeks
        if (d.said.isNotEmpty()) dao.insertBottles(d.said.map { t -> Bottle(text = t, sent = today(), back = today() + 10 + Random.nextInt(s.bottleWait * 2)) })
        // quests
        val monday = weekStart(today())
        val wk = Week(all.filter { dayOf(it.start) >= monday }, bellsNow.filter { dayOf(it.at) >= monday }, all.filter { dayOf(it.start) < monday }, monday, entries.count { dayOf(it.at) >= monday })
        val questIds = if (s.questWeek == monday) s.quests else Quests.deal(monday)
        val doneBefore = if (s.questWeek == monday) s.questsDone else emptySet()
        val newQuests = questIds.mapNotNull { Quests.byId[it] }.filter { it.id !in doneBefore && it.progress(wk) >= it.need }
        shells += newQuests.sumOf { it.shells }
        dao.update(x.copy(id = id, shells = shells))
        val courseDone = courseDay && (s.courseDay + 1) >= (Courses.byId[s.course]?.days?.size ?: 0)
        settings.update { st -> st.copy(shells = st.shells + shells, questWeek = monday, quests = questIds, questsDone = doneBefore + newQuests.map { q -> q.id },
            finds = st.finds + newFinds,
            courseDay = if (courseDay) st.courseDay + 1 else st.courseDay, courseLast = if (courseDay) today() else st.courseLast,
            inbox = st.inbox + eggs.map { e -> "egg:$e" } + out0.hatched.map { h -> "hatch:${h.id}" } + (if (courseDone) listOf("course:${st.course}") else emptyList())) }
        id to out0.copy(shells = shells, eggs = eggs, quests = newQuests, finds = newFinds, bottles = d.said.size, course = courseDay)
    }

    /** What a diary entry gave. */
    data class EntryOut(val food: Double, val shells: Int, val bonus: List<String>, val eggs: List<String>, val quests: List<app.puffling.domain.Quest>)

    /**
     * Saves a diary entry. A new one feeds the team (a little for each feeling named, more for words), brings shells,
     * and may turn up an egg or finish a quest; an edited one just changes its words.
     */
    suspend fun saveEntry(e: app.puffling.data.Entry): EntryOut? = lock.withLock {
        if (e.id != 0L) { dao.update(e); return null }
        val before = dao.entryList()
        val (food, bonus, fed) = Engine.feedEntry(e, before, dao.petList())
        dao.updatePets(fed)
        val s = settings.now()
        var shells = 2 + (if (Diary.words(e.text) >= 30) 1 else 0)
        val id = dao.insert(e.copy(food = food, shells = shells))
        val entries = before + e.copy(id = id)
        val sessions = dao.sessionList(); val bells = dao.bellList()
        val eggs = Engine.finds(Tally(sessions, bells, s, entries), fed.map { it.species }.toSet())
        val freeSlots = (0..2).filter { sl -> fed.none { it.slot == sl } }.toMutableList()
        for (x in eggs) dao.insert(newPet(x, freeSlots.removeFirstOrNull() ?: -1))
        val monday = weekStart(today())
        val wk = Week(sessions.filter { dayOf(it.start) >= monday }, bells.filter { dayOf(it.at) >= monday }, sessions.filter { dayOf(it.start) < monday }, monday, entries.count { dayOf(it.at) >= monday })
        val questIds = if (s.questWeek == monday) s.quests else Quests.deal(monday)
        val doneBefore = if (s.questWeek == monday) s.questsDone else emptySet()
        val newQuests = questIds.mapNotNull { Quests.byId[it] }.filter { it.id !in doneBefore && it.progress(wk) >= it.need }
        shells += newQuests.sumOf { it.shells }
        settings.update { st -> st.copy(shells = st.shells + shells, questWeek = monday, quests = questIds, questsDone = doneBefore + newQuests.map { q -> q.id },
            inbox = st.inbox + eggs.map { x -> "egg:$x" }) }
        EntryOut(food, shells, bonus, eggs, newQuests)
    }

    /** Daily housekeeping: the pets grow in their sleep, this week's quests are dealt, the next bell is set. */
    suspend fun tick() = lock.withLock {
        val s = settings.now(); val t = today()
        val from = if (s.digested == 0L) t - 1 else s.digested
        if (from < t - 1) {
            val (pets, news) = Engine.digest(dao.petList(), from, t, dao.sessionList())
            dao.updatePets(pets)
            settings.update { it.copy(digested = t - 1, inbox = it.inbox + news) }
        } else if (s.digested == 0L) settings.update { it.copy(digested = t - 1) }
        val week = weekStart(t)
        if (settings.now().questWeek != week) settings.update { it.copy(questWeek = week, quests = Quests.deal(week), questsDone = emptySet()) }
        Bells.schedule(app, settings.now())
    }

    fun newPet(species: String, slot: Int, hatched: Long = 0, nature: Nature? = null): Pet {
        val sp = Pets.of(species); val names = if (Lang.ru) sp.names else sp.namesEn
        return Pet(species = species, name = names.random(), nature = (nature ?: Nature.entries.random()).id, found = today(), hatched = hatched,
            slot = slot, shiny = Random.nextInt(16) == 0)
    }

    suspend fun setSlot(p: Pet, slot: Int) {
        val pets = dao.petList()
        val other = pets.firstOrNull { it.slot == slot && it.id != p.id }
        if (other != null) dao.update(other.copy(slot = p.slot))
        dao.update(p.copy(slot = slot))
    }

    suspend fun buy(id: String): Boolean {
        val it = Shop.byId[id] ?: return false; val s = settings.now()
        if (s.shells < it.price || id in s.items) return false
        settings.update { st -> st.copy(shells = st.shells - it.price, items = st.items + id) }
        return true
    }

    suspend fun saveOwn(p: OwnPractice): String {
        val id = if (p.id == 0L) dao.insert(p.copy(created = System.currentTimeMillis())) else { dao.update(p); p.id }
        loadOwn(); return "u$id"
    }

    suspend fun bolt(secs: Int) {
        finish(Done("bolt", System.currentTimeMillis() - secs * 1000L, secs, holds = listOf(secs)))
    }

    /** Fetches the weather for the window when it is older than half an hour; guesses the town from the time zone once. */
    suspend fun refreshOutside(force: Boolean = false) = kotlinx.coroutines.withContext(Dispatchers.IO) { runCatching {
        var s = settings.now()
        if (s.lat.isNaN() && !s.guessed) {
            val t = OutsideSource.guess(Lang.ru)
            settings.update { it.copy(guessed = true, town = t?.name ?: it.town, lat = t?.lat ?: it.lat, lon = t?.lon ?: it.lon) }
            s = settings.now()
        }
        if (s.lat.isNaN()) return@runCatching
        val old = Outside.of(s.outside)
        if (!force && old != null && System.currentTimeMillis() - old.at < 30 * 60_000L) return@runCatching
        val o = OutsideSource.fetch(s.lat, s.lon)
        settings.update { it.copy(outside = o.json()) }
    } }
}

class PufflingApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        Lang.ru = resources.configuration.locales[0].language == "ru"
        container = AppContainer(this)
        container.scope.launch(Dispatchers.Default) { container.loadOwn(); Gender.fem = container.settings.now().fem }
    }
}

val Context.container: AppContainer get() = (applicationContext as PufflingApp).container

/** The kind of a practice id, for the few places that only have the id. */
fun kindOf(id: String): Kind = Practices.find(id)?.kind ?: Kind.BREATH
