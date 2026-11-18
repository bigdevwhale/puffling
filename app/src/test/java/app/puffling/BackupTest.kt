package app.puffling

import app.puffling.data.Backup
import app.puffling.data.Bell
import app.puffling.data.Bottle
import app.puffling.data.Entry
import app.puffling.data.OwnAff
import app.puffling.data.OwnPractice
import app.puffling.data.Pet
import app.puffling.data.Session
import app.puffling.data.Settings
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The backup file says everything it was given, and turns away files that aren't its own. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BackupTest {
    private val all = Backup.Everything(
        sessions = listOf(Session(7, 1_700_000_000_000, 600, "u3", "breath", "air", rounds = 3, holds = "60,75,90", bpm = 5.5, before = 4, after = 2, notes = "one\ntwo", said = 2,
            noted = "think:4,hear:2", food = 12.5, shells = 3, leader = "owl", depth = 9, course = "calm:2", name = "Mine")),
        own = listOf(OwnPractice(3, "Mine", "breath", "air", about = "slow", minutes = 7, pattern = "i4,h4,o8", ramp = .5, steps = "30|sit", archived = true, created = 5)),
        affs = listOf(OwnAff(2, "I am here", 9)),
        bottles = listOf(Bottle(4, "I am here", 19_000, 19_021, opened = true)),
        bells = listOf(Bell(5, 1_700_000_100_000, 1_700_000_160_000)),
        pets = listOf(Pet(11, "owl", "Hoot", "calm", 19_000, hatched = 19_003, growth = 42.0, belly = 1.5, form = "sea", els = "air:120", slot = 0, sessions = 8, shiny = true)),
        entries = listOf(Entry(6, 1_700_000_200_000, "joy:pride", 4, "work", "a good day", 2.0, 3)),
        settings = Settings(onboarded = true, shells = 31, items = setOf("lamp"), weekGoal = 90, fem = true, quests = listOf("a", "b"), town = "Riga", lat = 56.9, lon = 24.1,
            bells = true, bellDays = setOf(1, 3, 5), volume = .4f, favs = setOf("f1"), finds = setOf("ray"), whIn = 2.5),
    )

    private fun same(a: Backup.Everything, b: Backup.Everything) {
        assertEquals(a.sessions, b.sessions); assertEquals(a.own, b.own); assertEquals(a.affs, b.affs); assertEquals(a.bottles, b.bottles)
        assertEquals(a.bells, b.bells); assertEquals(a.pets, b.pets); assertEquals(a.entries, b.entries); assertEquals(a.settings, b.settings)
    }

    @Test fun `what goes into the file comes back`() {
        same(all, Backup.decode(Backup.encode(all, "1.2.0")))
        same(all, Backup.read(Backup.encode(all).byteInputStream()))
    }

    @Test fun `an empty app makes a file that reads`() {
        val none = Backup.Everything(emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), Settings())
        same(none, Backup.decode(Backup.encode(none)))
    }

    private fun refused(text: String) = runCatching { Backup.decode(text) }.exceptionOrNull() as? Backup.Unreadable

    @Test fun `other files are turned away`() {
        assertFalse(refused("not json")!!.newer)
        assertFalse(refused("{}")!!.newer)
        assertFalse(refused(JSONObject(Backup.encode(all)).put("app", "pumpling").toString())!!.newer)
        // a row without what it can't live without
        assertFalse(refused(JSONObject(Backup.encode(all)).put("pets", org.json.JSONArray().put(JSONObject().put("name", "x"))).toString())!!.newer)
    }

    @Test fun `a file from a newer version says so`() {
        assertTrue(refused(JSONObject(Backup.encode(all)).put("format", Backup.FORMAT + 1).toString())!!.newer)
    }
}
