package app.puffling.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

/** Everything that isn't a session or a pet: shells, the tea house, the bells, the week's tide, the course. */
data class Settings(
    val onboarded: Boolean = false,
    val shells: Int = 0,                        // the money
    val items: Set<String> = emptySet(),        // tea house things bought
    val hidden: Set<String> = emptySet(),       // bought things put away
    val weekGoal: Int = 60,                     // minutes a week: the high tide
    val theme: String = "system",
    val fem: Boolean = false,                   // Russian forms: «благодарна»
    val digested: Long = 0,                     // last epoch day the night growth ran for
    val questWeek: Long = 0,
    val quests: List<String> = emptyList(),
    val questsDone: Set<String> = emptySet(),
    val inbox: List<String> = emptyList(),      // news to show once: «egg:owl», «hatch:12», «stage:12:2», «bottle:3», «find:ray»
    val seen: Set<String> = emptySet(),         // hints read
    val town: String = "", val lat: Double = Double.NaN, val lon: Double = Double.NaN,
    val guessed: Boolean = false,
    val outside: String = "",
    val bells: Boolean = false,                 // the mindfulness bell during the day
    val bellsPerDay: Int = 4,
    val bellFrom: Int = 9, val bellTo: Int = 21, // hours
    val bellDays: Set<Int> = (1..7).toSet(),     // ISO weekdays it rings on
    val cues: Boolean = true,                    // soft tones on in- and out-breath
    val haptics: Boolean = true,                 // a tap on each turn of the breath
    val sound: String = "sea",                   // the background: sea, rain, fire, wind, none
    val volume: Float = .6f,
    val bellKind: String = "bowl",               // bowl, bell, chime, gong
    val askFeel: Boolean = true,                 // ask how tense before and after
    val course: String = "",                     // the course taken
    val courseDay: Int = 0,                      // days of it done
    val courseLast: Long = 0,                    // the day the last course day was done
    val favs: Set<String> = emptySet(),          // affirmations kept close
    val finds: Set<String> = emptySet(),         // sea finds from the dives
    val cardBg: Int = 0,
    val goals: Set<String> = emptySet(),
    val belly: Boolean = false,                  // follow the real breath with the phone on the belly
    val bottleWait: Int = 21,                    // average days before a bottle washes back
    val whLevel: String = "beg",                // the Wim Hof level last chosen
    val whIn: Double = 2.0, val whOut: Double = 1.5, // the tempo of one power breath
) {
    fun json(): JSONObject = JSONObject().apply {
        fun arr(c: Collection<Any>) = JSONArray(c.toList())
        put("onboarded", onboarded); put("shells", shells); put("items", arr(items)); put("hidden", arr(hidden)); put("weekGoal", weekGoal); put("theme", theme)
        put("fem", fem); put("digested", digested); put("questWeek", questWeek); put("quests", arr(quests)); put("questsDone", arr(questsDone)); put("inbox", arr(inbox))
        put("seen", arr(seen)); put("town", town); if (!lat.isNaN()) { put("lat", lat); put("lon", lon) }; put("guessed", guessed); put("outside", outside)
        put("bells", bells); put("bellsPerDay", bellsPerDay); put("bellFrom", bellFrom); put("bellTo", bellTo); put("bellDays", arr(bellDays))
        put("cues", cues); put("haptics", haptics); put("sound", sound); put("volume", volume.toDouble()); put("bellKind", bellKind); put("askFeel", askFeel)
        put("course", course); put("courseDay", courseDay); put("courseLast", courseLast); put("favs", arr(favs)); put("finds", arr(finds)); put("cardBg", cardBg)
        put("goals", arr(goals)); put("belly", belly); put("bottleWait", bottleWait); put("whLevel", whLevel); put("whIn", whIn); put("whOut", whOut)
    }

    companion object {
        fun of(j: JSONObject): Settings {
            fun list(k: String) = j.optJSONArray(k)?.let { a -> (0 until a.length()).map { a.getString(it) } } ?: emptyList()
            fun set(k: String) = list(k).toSet()
            val d = Settings()
            return Settings(
                onboarded = j.optBoolean("onboarded"), shells = j.optInt("shells"), items = set("items"), hidden = set("hidden"), weekGoal = j.optInt("weekGoal", d.weekGoal),
                theme = j.optString("theme", d.theme), fem = j.optBoolean("fem"), digested = j.optLong("digested"), questWeek = j.optLong("questWeek"), quests = list("quests"),
                questsDone = set("questsDone"), inbox = list("inbox"), seen = set("seen"), town = j.optString("town"), lat = j.optDouble("lat", Double.NaN), lon = j.optDouble("lon", Double.NaN),
                guessed = j.optBoolean("guessed"), outside = j.optString("outside"), bells = j.optBoolean("bells"), bellsPerDay = j.optInt("bellsPerDay", d.bellsPerDay),
                bellFrom = j.optInt("bellFrom", d.bellFrom), bellTo = j.optInt("bellTo", d.bellTo), bellDays = if (j.has("bellDays")) list("bellDays").mapNotNull { it.toIntOrNull() }.toSet() else d.bellDays,
                cues = j.optBoolean("cues", true), haptics = j.optBoolean("haptics", true), sound = j.optString("sound", d.sound), volume = j.optDouble("volume", .6).toFloat(),
                bellKind = j.optString("bellKind", d.bellKind), askFeel = j.optBoolean("askFeel", true), course = j.optString("course"), courseDay = j.optInt("courseDay"),
                courseLast = j.optLong("courseLast"), favs = set("favs"), finds = set("finds"), cardBg = j.optInt("cardBg"), goals = set("goals"), belly = j.optBoolean("belly"),
                bottleWait = j.optInt("bottleWait", d.bottleWait), whLevel = j.optString("whLevel", d.whLevel), whIn = j.optDouble("whIn", d.whIn), whOut = j.optDouble("whOut", d.whOut))
        }
    }
}

private val Context.store by preferencesDataStore("puffling")
private val KEY = stringPreferencesKey("settings")

class SettingsStore(private val ctx: Context) {
    val settings: Flow<Settings> = ctx.store.data.map { p -> p[KEY]?.let { runCatching { Settings.of(JSONObject(it)) }.getOrNull() } ?: Settings() }
    suspend fun now(): Settings = settings.first()
    suspend fun update(f: (Settings) -> Settings) { ctx.store.edit { p ->
        val cur = p[KEY]?.let { runCatching { Settings.of(JSONObject(it)) }.getOrNull() } ?: Settings()
        p[KEY] = f(cur).json().toString() } }
}
