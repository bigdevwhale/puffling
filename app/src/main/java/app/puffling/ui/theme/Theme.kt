package app.puffling.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import app.puffling.R

/** Colour tokens: sand for the page, sea blue for action, sage for done, shell gold for shells. */
class Pal(
    val dark: Boolean, val bg: Color, val card: Color, val card2: Color, val ink: Color, val mute: Color, val line: Color, val track: Color,
    val acc: Color, val accInk: Color, val accSoft: Color, val ok: Color, val okSoft: Color, val gold: Color, val goldSoft: Color, val bad: Color,
    val heat: List<Color>, val nav: Color, val shadow: Color, val night: Color,
)

private fun c(hex: Long) = Color(0xFF000000 or hex)

val Light = Pal(false, c(0xF3F0EA), c(0xFFFFFF), c(0xEAE6DE), c(0x22303A), c(0x7B8790), c(0xE3DED5), c(0xE6E1D8),
    c(0x3E8FB0), c(0xFFFFFF), c(0xDCEEF5), c(0x5E9C76), c(0xDEEEE3), c(0xD99A2B), c(0xFAEED6), c(0xD9606A),
    listOf(c(0xE6E1D8), c(0xC7E2EE), c(0x8CC3DB), c(0x4E9EC0), c(0x2B6F8E)), c(0xFBFAF7), Color(0x22304A5A), c(0x1A2A3A))

val Dark = Pal(true, c(0x11161C), c(0x1A2129), c(0x222B34), c(0xE9EEF2), c(0x8E9AA4), c(0x2A333D), c(0x26303A),
    c(0x6EB8D6), c(0x0D1A22), c(0x1C3341), c(0x7DC096), c(0x1B3326), c(0xF0B455), c(0x3A2E1A), c(0xF07A82),
    listOf(c(0x26303A), c(0x1F4457), c(0x2C6884), c(0x4C97BA), c(0x8FD0EA)), c(0x161C23), Color(0x55000000), c(0x0B1219))

/** The palette in use; switched by the theme setting before the tree is drawn. */
object C {
    @Volatile var p: Pal = Light
    val dark get() = p.dark
    val bg get() = p.bg; val card get() = p.card; val card2 get() = p.card2; val ink get() = p.ink; val mute get() = p.mute
    val line get() = p.line; val track get() = p.track; val acc get() = p.acc; val accInk get() = p.accInk; val accSoft get() = p.accSoft
    val ok get() = p.ok; val okSoft get() = p.okSoft; val gold get() = p.gold; val goldSoft get() = p.goldSoft; val bad get() = p.bad
    val heat get() = p.heat; val nav get() = p.nav; val shadow get() = p.shadow; val night get() = p.night
}

val Nunito = FontFamily(Font(R.font.nunito_regular, FontWeight.Normal), Font(R.font.nunito_semibold, FontWeight.SemiBold), Font(R.font.nunito_bold, FontWeight.Bold), Font(R.font.nunito_bold, FontWeight.ExtraBold))
val Inter = FontFamily(Font(R.font.inter_regular, FontWeight.Normal), Font(R.font.inter_medium, FontWeight.Medium), Font(R.font.inter_semibold, FontWeight.SemiBold), Font(R.font.inter_semibold, FontWeight.Bold))
val Pixel = FontFamily(Font(R.font.press_start, FontWeight.Normal))

object T {
    val title get() = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.ExtraBold, fontSize = 26.sp, lineHeight = 30.sp, color = C.ink)
    val h2 get() = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.Bold, fontSize = 19.sp, lineHeight = 24.sp, color = C.ink)
    val h3 get() = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 21.sp, color = C.ink)
    val body get() = TextStyle(fontFamily = Nunito, fontSize = 15.sp, lineHeight = 21.sp, color = C.ink)
    val bodyB get() = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.Bold, fontSize = 15.sp, lineHeight = 21.sp, color = C.ink)
    val small get() = TextStyle(fontFamily = Nunito, fontSize = 13.sp, lineHeight = 17.sp, color = C.mute)
    val smallB get() = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.Bold, fontSize = 13.sp, lineHeight = 17.sp, color = C.ink)
    val tiny get() = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, lineHeight = 14.sp, color = C.mute)
    val sec get() = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, letterSpacing = 1.sp, color = C.mute)
    val num get() = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = C.ink)
    val numS get() = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = C.ink)
    val big get() = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 40.sp, lineHeight = 44.sp, color = C.ink)
    val btn get() = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
    val pix get() = TextStyle(fontFamily = Pixel, fontSize = 9.sp, lineHeight = 12.sp, color = C.ink)
}

@Composable
fun PuffTheme(dark: Boolean, content: @Composable () -> Unit) {
    C.p = if (dark) Dark else Light
    val p = C.p
    val scheme = if (dark) darkColorScheme(primary = p.acc, background = p.bg, surface = p.card, onSurface = p.ink, onBackground = p.ink)
    else lightColorScheme(primary = p.acc, background = p.bg, surface = p.card, onSurface = p.ink, onBackground = p.ink)
    MaterialTheme(colorScheme = scheme, content = content)
}
