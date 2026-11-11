package app.puffling.ui.art

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import app.puffling.BuildConfig
import app.puffling.data.Pet
import app.puffling.data.Session
import app.puffling.domain.Element
import app.puffling.domain.Rand
import app.puffling.domain.Shop
import app.puffling.domain.hash
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/** Where the moon window is in the room picture, as parts of its width and height. */
const val WIN_X = .4987f; const val WIN_Y = .376f; const val WIN_R = .197f

@Composable
fun loop(ms: Int, label: String = "loop"): Float {
    val v by rememberInfiniteTransition(label = label).animateFloat(0f, 1f, infiniteRepeatable(tween(ms, easing = LinearEasing)), label = "t")
    return if (BuildConfig.SHOTS) .3f else v
}

/**
 * The tea house on the cliff: the sea in the moon window, the things bought for the room, and the team on
 * the floor – the leader in the middle, companions either side.
 */
@Composable
fun TeaHouse(team: List<Pet>, items: Set<String>, sky: Sky, tide: Float, modifier: Modifier = Modifier, pose: Pose = Pose.IDLE, lungs: Float? = null, preview: String? = null) {
    val ctx = LocalContext.current
    val t = loop(16000, "sky")
    val bg = Art.get(ctx, "room_bg")
    BoxWithConstraints(modifier.fillMaxWidth().aspectRatio(1.5f)) {
        val wDp = maxWidth; val hDp = maxHeight
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width; val h = size.height
            seaWindow(Offset(w * WIN_X, h * WIN_Y), w * WIN_R, sky, t, tide, lungs)
            if (bg != null) drawImage(bg, IntOffset.Zero, IntSize(bg.width, bg.height), IntOffset.Zero, IntSize(w.roundToInt(), h.roundToInt()), filterQuality = FilterQuality.Medium)
            else drawRect(Color(0xFFD9A867), Offset(0f, h * .75f), Size(w, h * .25f))
            val shown = Shop.all.filter { it.id in items || it.id == preview }.sortedBy { if (it.wall) 0 else 1 }
            for (it in shown) {
                val img = Art.item(ctx, it.id) ?: continue
                val iw = w * it.w; val ih = iw * img.height / img.width
                val x = w * it.x - iw / 2; val y = if (it.wall) h * it.y - ih / 2 else h * it.y - ih
                drawImage(img, IntOffset.Zero, IntSize(img.width, img.height), IntOffset(x.roundToInt(), y.roundToInt()), IntSize(iw.roundToInt(), ih.roundToInt()),
                    alpha = if (it.id == preview && it.id !in items) .8f else 1f, filterQuality = FilterQuality.Medium)
            }
            night(sky)
        }
        team.filter { it.slot in 1..2 }.forEach { p ->
            key(p.id) { val s = hDp * .34f
                Box(Modifier.offset(x = wDp * (if (p.slot == 1) .05f else .72f), y = hDp * .97f - s)) { PetView(p, s, if (pose == Pose.PRACTICE) Pose.PRACTICE else pose, lungs = lungs) } }
        }
        team.firstOrNull { it.slot == 0 }?.let { lead -> key(lead.id) { val s = hDp * .5f
            Box(Modifier.offset(x = (wDp - s) / 2, y = hDp * 1.0f - s)) { PetView(lead, s, pose, lungs = lungs) } } }
    }
}

/** The room darkens at night; the paper lamp keeps a warm corner. */
fun DrawScope.night(sky: Sky) {
    if (sky.day >= .9f) return
    val k = 1 - sky.day
    drawRect(Color(0xFF101830).copy(alpha = .34f * k))
    val lamp = Offset(size.width * .135f, size.height * .26f)
    drawCircle(Brush.radialGradient(listOf(Color(0xFFFFC977).copy(alpha = .35f * k), Color.Transparent), lamp, size.width * .45f), size.width * .45f, lamp)
}

/**
 * The dive of a breath hold: blue going dark with depth, bubbles, and what lives at each depth drifting
 * past. [secs] is the hold so far; [finds] are the ids already in the aquarium (shown brighter).
 */
fun DrawScope.deepWater(secs: Float, t: Float) {
    val w = size.width; val h = size.height
    val d = (secs / 210f).coerceIn(0f, 1f)
    val top = mixC(Color(0xFF3F8FC0), Color(0xFF07101E), d); val bottom = mixC(Color(0xFF1F5C86), Color(0xFF02060C), min(1f, d + .2f))
    drawRect(Brush.verticalGradient(listOf(top, bottom)))
    // light rays near the surface
    if (d < .5f) for (k in 0 until 5) {
        val x = w * (.1f + k * .2f) + w * .05f * sin(t * 6.28f + k)
        val p = Path().apply { moveTo(x, 0f); lineTo(x + w * .08f, 0f); lineTo(x + w * .2f, h); lineTo(x + w * .1f, h); close() }
        drawPath(p, Color.White.copy(alpha = .06f * (1 - d * 2)))
    }
    // drifting particles, rising as the pet sinks
    for (k in 0 until 40) {
        val x = w * ((k * .618f) % 1f); val y = h * (1f - ((t * .6f + k * .137f + secs * .01f) % 1f))
        drawCircle(Color.White.copy(alpha = .12f + .1f * ((k % 3) / 2f)), w * .004f * (1 + k % 3), Offset(x, y))
    }
}

/** A candle flame, drawn: it sways a little and breathes. */
@Composable
fun Flame(modifier: Modifier = Modifier) {
    val t = loop(2600, "flame")
    Canvas(modifier) {
        val w = size.width; val h = size.height
        val cx = w / 2; val base = h * .62f
        drawCircle(Brush.radialGradient(listOf(Color(0x66FFB347), Color.Transparent), Offset(cx, base - h * .12f), w * .5f), w * .5f, Offset(cx, base - h * .12f))
        // candle
        drawRoundRect(Brush.horizontalGradient(listOf(Color(0xFFE8DCC5), Color(0xFFFFF7E8), Color(0xFFD9CBB0)), cx - w * .09f, cx + w * .09f),
            Offset(cx - w * .09f, base), Size(w * .18f, h * .36f), androidx.compose.ui.geometry.CornerRadius(w * .02f))
        drawLine(Color(0xFF3A2E24), Offset(cx, base), Offset(cx, base - h * .03f), w * .01f)
        val sway = sin(t * 2 * PI.toFloat()) * w * .02f + sin(t * 6 * PI.toFloat()) * w * .006f
        val fh = h * (.22f + .015f * cos(t * 4 * PI.toFloat()))
        fun flame(scale: Float, col: Color) {
            val p = Path().apply {
                moveTo(cx, base - h * .02f)
                cubicTo(cx - w * .085f * scale, base - fh * .25f * scale, cx - w * .04f * scale + sway, base - fh * .7f * scale, cx + sway * 1.4f, base - fh * scale)
                cubicTo(cx + w * .04f * scale + sway, base - fh * .7f * scale, cx + w * .085f * scale, base - fh * .25f * scale, cx, base - h * .02f)
                close() }
            drawPath(p, col)
        }
        flame(1f, Color(0xFFFF9A3C)); flame(.72f, Color(0xFFFFD46B)); flame(.4f, Color(0xFFFFF6D8))
    }
}

/** Soft rings around the pacer that widen with the breath. */
fun DrawScope.breathRings(lungs: Float, color: Color) {
    val c = Offset(size.width / 2, size.height / 2)
    val r0 = min(size.width, size.height) * .3f
    for (k in 0 until 3) {
        val r = r0 * (1f + lungs * .55f) * (1f + k * .14f)
        drawCircle(color.copy(alpha = max(0f, .22f - k * .07f) * (.4f + .6f * lungs)), r, c)
    }
    drawCircle(color.copy(alpha = .5f), r0 * (1f + lungs * .55f), c, style = Stroke(r0 * .02f))
}

/** A soft photo-like crop of a creature bitmap, used on cards and the dive. */
fun DrawScope.sprite(img: androidx.compose.ui.graphics.ImageBitmap, cx: Float, foot: Float, box: Float, alpha: Float = 1f, flip: Boolean = false, filter: androidx.compose.ui.graphics.ColorFilter? = null) {
    val s = min(box / img.width, box / img.height); val w = img.width * s; val h = img.height * s
    withTransform({ if (flip) scale(-1f, 1f, Offset(cx, foot)) }) {
        drawImage(img, IntOffset.Zero, IntSize(img.width, img.height), IntOffset((cx - w / 2).roundToInt(), (foot - h).roundToInt()), IntSize(w.roundToInt(), h.roundToInt()),
            alpha = alpha, filterQuality = FilterQuality.Medium, colorFilter = filter)
    }
}
