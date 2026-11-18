package app.puffling.data

import androidx.room.withTransaction
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.io.OutputStream

/**
 * Everything the app keeps, as one JSON file the user owns: the practices done, own practices and affirmations,
 * the bottles at sea, the diary, the bells, the pets and the settings. Puffling keeps no files of its own beside these.
 * Reading a backup replaces what is on the phone.
 */
object Backup {
    /** Raised when the file's layout changes in a way older versions can't read. */
    const val FORMAT = 1
    const val APP = "puffling"
    const val NAME = "puffling.json"
    private const val MAX = 64 * 1024 * 1024

    class Everything(val sessions: List<Session>, val own: List<OwnPractice>, val affs: List<OwnAff>, val bottles: List<Bottle>,
                     val bells: List<Bell>, val pets: List<Pet>, val entries: List<Entry>, val settings: Settings) {
        val minutes get() = sessions.sumOf { it.secs } / 60
    }

    /** The file is not a Puffling backup, or was made by a newer version. */
    class Unreadable(val newer: Boolean) : Exception()

    // ---------- json ----------
    private fun <T> arr(l: List<T>, f: (T) -> JSONObject) = JSONArray().apply { l.forEach { put(f(it)) } }
    private fun <T> list(j: JSONObject, k: String, f: (JSONObject) -> T): List<T> = j.optJSONArray(k)?.let { a -> (0 until a.length()).map { f(a.getJSONObject(it)) } } ?: emptyList()

    private fun Session.json() = JSONObject().put("id", id).put("start", start).put("secs", secs).put("practice", practice).put("kind", kind).put("el", el).put("rounds", rounds)
        .put("holds", holds).put("bpm", bpm).put("before", before).put("after", after).put("notes", notes).put("said", said).put("noted", noted).put("food", food).put("shells", shells)
        .put("leader", leader).put("depth", depth).put("course", course).put("name", name)
    private fun session(j: JSONObject) = Session(j.getLong("id"), j.getLong("start"), j.getInt("secs"), j.getString("practice"), j.optString("kind"), j.optString("el"), j.optInt("rounds"),
        j.optString("holds"), j.optDouble("bpm", 0.0), j.optInt("before"), j.optInt("after"), j.optString("notes"), j.optInt("said"), j.optString("noted"), j.optDouble("food", 0.0),
        j.optInt("shells"), j.optString("leader"), j.optInt("depth"), j.optString("course"), j.optString("name"))

    private fun OwnPractice.json() = JSONObject().put("id", id).put("name", name).put("kind", kind).put("el", el).put("about", about).put("minutes", minutes).put("pattern", pattern)
        .put("ramp", ramp).put("steps", steps).put("rounds", rounds).put("breaths", breaths).put("bell", bell).put("hold", hold).put("deck", deck).put("archived", archived).put("created", created)
    private fun own(j: JSONObject) = OwnPractice(j.getLong("id"), j.getString("name"), j.getString("kind"), j.optString("el"), j.optString("about"), j.optInt("minutes", 5), j.optString("pattern", "i4,o6"),
        j.optDouble("ramp", 0.0), j.optString("steps"), j.optInt("rounds", 3), j.optInt("breaths", 30), j.optInt("bell"), j.optInt("hold", 60), j.optString("deck", "mix"), j.optBoolean("archived"), j.optLong("created"))

    private fun OwnAff.json() = JSONObject().put("id", id).put("text", text).put("created", created)
    private fun aff(j: JSONObject) = OwnAff(j.getLong("id"), j.getString("text"), j.optLong("created"))

    private fun Bottle.json() = JSONObject().put("id", id).put("text", text).put("sent", sent).put("back", back).put("opened", opened)
    private fun bottle(j: JSONObject) = Bottle(j.getLong("id"), j.getString("text"), j.getLong("sent"), j.getLong("back"), j.optBoolean("opened"))

    private fun Bell.json() = JSONObject().put("id", id).put("at", at).put("answered", answered)
    private fun bell(j: JSONObject) = Bell(j.getLong("id"), j.getLong("at"), j.optLong("answered"))

    private fun Pet.json() = JSONObject().put("id", id).put("species", species).put("name", name).put("nature", nature).put("found", found).put("hatched", hatched).put("growth", growth)
        .put("belly", belly).put("form", form).put("els", els).put("slot", slot).put("sessions", sessions).put("shiny", shiny)
    private fun pet(j: JSONObject) = Pet(j.getLong("id"), j.getString("species"), j.optString("name"), j.optString("nature"), j.optLong("found"), j.optLong("hatched"), j.optDouble("growth", 0.0),
        j.optDouble("belly", 0.0), j.optString("form"), j.optString("els"), j.optInt("slot", -1), j.optInt("sessions"), j.optBoolean("shiny"))

    private fun Entry.json() = JSONObject().put("id", id).put("at", at).put("emotions", emotions).put("strength", strength).put("tags", tags).put("text", text).put("food", food).put("shells", shells)
    private fun entry(j: JSONObject) = Entry(j.getLong("id"), j.getLong("at"), j.optString("emotions"), j.optInt("strength", 3), j.optString("tags"), j.optString("text"), j.optDouble("food", 0.0), j.optInt("shells"))

    fun encode(e: Everything, version: String = ""): String = JSONObject().put("app", APP).put("format", FORMAT).put("version", version).put("at", System.currentTimeMillis())
        .put("sessions", arr(e.sessions) { it.json() }).put("own", arr(e.own) { it.json() }).put("affs", arr(e.affs) { it.json() }).put("bottles", arr(e.bottles) { it.json() })
        .put("bells", arr(e.bells) { it.json() }).put("pets", arr(e.pets) { it.json() }).put("entries", arr(e.entries) { it.json() }).put("settings", e.settings.json()).toString()

    /** Reads a whole file; throws [Unreadable] for anything that isn't a Puffling backup this version understands. */
    fun decode(text: String): Everything {
        val j = runCatching { JSONObject(text) }.getOrNull() ?: throw Unreadable(false)
        if (j.optString("app") != APP) throw Unreadable(false)
        if (j.optInt("format", 0) > FORMAT) throw Unreadable(true)
        return runCatching {
            Everything(list(j, "sessions", ::session), list(j, "own", ::own), list(j, "affs", ::aff), list(j, "bottles", ::bottle), list(j, "bells", ::bell), list(j, "pets", ::pet),
                list(j, "entries", ::entry), Settings.of(j.getJSONObject("settings")))
        }.getOrElse { throw Unreadable(false) }
    }

    // ---------- the file ----------
    suspend fun collect(dao: Dao0, store: SettingsStore) =
        Everything(dao.sessionList(), dao.ownList(), dao.affList(), dao.bottleList(), dao.bellList(), dao.petList(), dao.entryList(), store.now())

    /** Writes everything to [out] and closes it. */
    suspend fun write(dao: Dao0, store: SettingsStore, out: OutputStream, version: String) {
        val text = encode(collect(dao, store), version)
        out.use { it.write(text.toByteArray()) }
    }

    /** Reads and checks a backup from [input], closing it; the phone is not touched. Throws [Unreadable] for a wrong file. */
    fun read(input: InputStream): Everything {
        val bytes = input.use { s ->
            val buf = java.io.ByteArrayOutputStream(); val chunk = ByteArray(64 * 1024)
            while (true) { val n = s.read(chunk); if (n < 0) break; buf.write(chunk, 0, n); if (buf.size() > MAX) throw Unreadable(false) }
            buf.toByteArray()
        }
        return decode(bytes.toString(Charsets.UTF_8))
    }

    /** Puts [e] in place of everything on the phone, in one transaction; the settings follow. */
    suspend fun restore(db: Db, store: SettingsStore, e: Everything) {
        val dao = db.dao()
        db.withTransaction {
            dao.clearSessions(); dao.clearOwn(); dao.clearAffs(); dao.clearBottles(); dao.clearBells(); dao.clearPets(); dao.clearEntries()
            e.sessions.chunked(500).forEach { dao.insertSessions(it) }; dao.insertOwn(e.own); dao.insertAffs(e.affs); dao.insertBottles(e.bottles)
            e.bells.chunked(500).forEach { dao.insertBells(it) }; dao.insertPets(e.pets); e.entries.chunked(500).forEach { dao.insertEntries(it) }
        }
        // the weather outside the window is fetched anew
        store.update { e.settings.copy(onboarded = true, outside = "") }
    }
}
