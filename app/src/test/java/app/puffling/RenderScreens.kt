package app.puffling

import android.graphics.Bitmap
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import app.puffling.data.DemoSeed
import app.puffling.domain.Lang
import app.puffling.domain.Practices
import app.puffling.domain.WimHofLevel
import app.puffling.ui.Root
import app.puffling.ui.Start
import app.puffling.ui.share.CardKind
import app.puffling.ui.share.Look
import app.puffling.ui.share.Painter
import app.puffling.ui.share.ShareReq
import app.puffling.ui.theme.PuffTheme
import app.puffling.work.Plan
import app.puffling.work.Player
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Every screen of the demo drawn to PNGs in app/build/art/screens (run with `-Prender`). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "ru-w393dp-h2400dp-xxhdpi")
class RenderScreens {
    @get:Rule val rule = createAndroidComposeRule<androidx.activity.ComponentActivity>()
    private val out = File(System.getProperty("render.out") ?: "build/art", "screens").apply { mkdirs() }
    private val dark = System.getProperty("render.dark") == "1"

    private fun settle() = repeat(6) { rule.mainClock.advanceTimeBy(400); Thread.sleep(150); org.robolectric.shadows.ShadowLooper.idleMainLooper() }
    private fun shot(name: String) {
        settle()
        val v = rule.activity.window.decorView
        val b = Bitmap.createBitmap(v.width, v.height, Bitmap.Config.ARGB_8888)
        rule.runOnUiThread { v.draw(android.graphics.Canvas(b)) }
        File(out, "${if (dark) "d-" else ""}$name.png").outputStream().use { b.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test fun render() {
        Lang.ru = true
        val c = ApplicationProvider.getApplicationContext<PufflingApp>().container
        runBlocking { DemoSeed.seedIfEmpty(c); c.loadOwn() }
        val pets = runBlocking { c.dao.petList() }
        val last = runBlocking { c.dao.sessionList().first { it.practice == "gratitude" } }
        val shots = listOf(
            "1-home" to (0 to null), "2-library" to (1 to null), "3-pets" to (2 to null), "4-stats" to (3 to null),
            "5-pet" to (2 to "pet:${pets.first().id}"), "6-session" to (3 to "session:${last.id}"), "7-begin-box" to (1 to "begin:box"), "7b-begin-wimhof" to (1 to "begin:wimhof"),
            "8-shop" to (0 to "shop"), "9-settings" to (0 to "settings"), "10-aquarium" to (1 to "aquarium"), "11-affirmations" to (1 to "affirmations"),
            "12-course" to (1 to "course:calm"), "13-bolt" to (1 to "bolt"), "15-quick" to (0 to "quick"), "16-own" to (1 to "own:b"),
            "17-share-sheet" to (0 to "share:WEEK"),
        )
        var i by mutableIntStateOf(-1)
        rule.mainClock.autoAdvance = false
        rule.setContent {
            val s by c.settings.settings.collectAsState(null)
            PuffTheme(dark) { s?.let { st -> key(i) { if (i >= 0) Root(st) } } }
        }
        for ((n, sh) in shots.withIndex()) {
            Start.tab = sh.second.first; Start.open = sh.second.second
            rule.runOnUiThread { i = n }
            shot(sh.first)
        }
        Start.tab = 2; Start.open = null; rule.runOnUiThread { i = 150 }
        settle(); rule.onAllNodesWithText("Бестиарий")[0].performClick(); shot("3b-bestiary")
        runBlocking { c.settings.update { it.copy(outside = app.puffling.data.Outside(app.puffling.data.Weather.RAIN, 90, 6.0, 0, 0, System.currentTimeMillis()).json()) } }
        Start.tab = 0; rule.runOnUiThread { i = 160 }; shot("1b-home-rain")
        // sessions playing
        val s = runBlocking { c.settings.now() }
        fun play(id: String, name: String, after: () -> Unit = {}) {
            val p = Practices.of(id)
            rule.runOnUiThread { Player.start(rule.activity, Plan(p, p.minutes, 3, 30, "none", "bowl", 0f, false, false, false, level = if (p.kind.code == "w") WimHofLevel.BEGINNER else null)); after() }
            Start.tab = 0; Start.open = null; rule.runOnUiThread { i += 1 }
            repeat(4) { Thread.sleep(400); org.robolectric.shadows.ShadowLooper.idleMainLooper() }
            shot(name)
            rule.runOnUiThread { Player.stop(rule.activity) }
        }
        rule.runOnUiThread { i = 300 }
        play("box", "20-play-box")
        play("wimhof", "21-play-wimhof") { Player.wh?.tick(12.0) }
        play("wimhof", "22-play-hold") { Player.wh?.tick(200.0); Player.wh?.tick(70.0) }
        play("candle", "23-play-candle")
        play("affirm", "24-play-affirm")
        play("gratitude", "25-play-gratitude") { Player.next() }
        // every card, feed and story
        val data = runBlocking { app.puffling.ui.Data(c.settings.now(), c.dao.sessionList(), c.dao.petList(), c.dao.ownList(), c.dao.affList(), c.dao.bottleList(), c.dao.bellList()) }
        val wim = data.sessions.first { it.kind == "w" }
        val reqs = listOf(ShareReq(CardKind.SESSION, wim.id), ShareReq(CardKind.WEEK), ShareReq(CardKind.HOUSE), ShareReq(CardKind.PET, pets.first().id),
            ShareReq(CardKind.BESTIARY), ShareReq(CardKind.AQUARIUM))
        val cards = File(out.parentFile, "cards").apply { mkdirs() }
        reqs.forEachIndexed { k, r -> listOf(false, true).forEach { story ->
            val bmp = Painter(rule.activity, data, Look(story, k % 5)).render(r).first
            File(cards, "${k}-${r.kind.name.lowercase()}${if (story) "-story" else ""}.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 90, it) } } }
        check(s.onboarded)
    }
}
