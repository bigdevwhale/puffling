package app.puffling.ui.art

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import app.puffling.BuildConfig
import app.puffling.data.Pet
import app.puffling.domain.Pets
import app.puffling.domain.Stage
import app.puffling.domain.stage
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/** Pictures from res/drawable-nodpi by name, decoded once. */
object Art {
    private val cache = HashMap<String, ImageBitmap?>()
    @SuppressLint("DiscouragedApi")
    @Synchronized fun get(ctx: Context, name: String): ImageBitmap? = cache.getOrPut(name) {
        val id = ctx.resources.getIdentifier(name, "drawable", ctx.packageName)
        if (id == 0) null else BitmapFactory.decodeResource(ctx.resources, id, BitmapFactory.Options().apply { inScaled = false })?.asImageBitmap()
    }
    fun pet(ctx: Context, species: String, frame: Int) = get(ctx, "pet_${species}_$frame")
    fun egg(ctx: Context, species: String) = get(ctx, "egg_$species")
    fun item(ctx: Context, id: String) = get(ctx, "item_$id")
    fun find(ctx: Context, id: String) = get(ctx, "find_$id")
}

/** What a pet is doing: standing about, practising (eyes closed), glowing with joy, asleep. */
enum class Pose { IDLE, PRACTICE, JOY, SLEEP }

/** The frame of the sheet for a stage and pose: 0 baby, 1 teen, 2 adult, 3 practice, 4 joy, 5 asleep. */
fun frameOf(stage: Stage, pose: Pose): Int = when {
    pose == Pose.SLEEP && stage >= Stage.ADULT -> 5
    pose == Pose.PRACTICE && stage >= Stage.ADULT -> 3
    pose == Pose.JOY && stage >= Stage.ADULT -> 4
    stage == Stage.BABY -> 0
    stage == Stage.TEEN -> 1
    else -> 2
}

private val still = BuildConfig.SHOTS

/** Hue turned for the rare pets. */
fun shinyFilter(): ColorFilter {
    val a = PI.toFloat() * .55f; val c = cos(a); val s = sin(a)
    val m = floatArrayOf(
        .213f + c * .787f - s * .213f, .715f - c * .715f - s * .715f, .072f - c * .072f + s * .928f, 0f, 0f,
        .213f - c * .213f + s * .143f, .715f + c * .285f + s * .140f, .072f - c * .072f - s * .283f, 0f, 0f,
        .213f - c * .213f - s * .787f, .715f - c * .715f + s * .715f, .072f + c * .928f + s * .072f, 0f, 0f,
        0f, 0f, 0f, 1f, 0f)
    return ColorFilter.colorMatrix(ColorMatrix(m))
}

/**
 * A pet in a square of [size]. With [lungs] (0..1) it breathes with the practice: it fills out on the
 * in-breath and settles on the out-breath. Without it, it breathes slowly on its own.
 */
@Composable
fun PetView(pet: Pet, size: Dp, pose: Pose = Pose.IDLE, modifier: Modifier = Modifier, lungs: Float? = null) {
    val st = pet.stage()
    if (st == Stage.EGG) { EggView(pet.species, size, (pet.growth / Pets.HATCH).toFloat(), modifier); return }
    Creature(pet.species, st, pose, size, pet.shiny, modifier, phase = (pet.id % 7) / 7f, lungs = lungs)
}

@Composable
fun Creature(species: String, st: Stage, pose: Pose, size: Dp, shiny: Boolean = false, modifier: Modifier = Modifier, phase: Float = 0f, lungs: Float? = null, glow: Boolean = true) {
    val ctx = LocalContext.current
    val img = Art.pet(ctx, species, frameOf(st, pose))
    val t by rememberInfiniteTransition(label = "pet").animateFloat(0f, 1f, infiniteRepeatable(tween(4200, easing = LinearEasing), RepeatMode.Restart), label = "t")
    val time = if (still) .25f else (t + phase) % 1f
    Canvas(modifier.size(size)) {
        val w = this.size.width; val h = this.size.height
        val l = lungs ?: (.5f - .5f * cos(time * 2 * PI.toFloat()))
        drawOval(Color(0x2A000000), Offset(w * (.24f - l * .02f), h * .9f), Size(w * (.52f + l * .04f), h * .07f))
        if (glow && st == Stage.MASTER) drawCircle(Brush.radialGradient(listOf(Color(0x55FFE6A8), Color.Transparent), Offset(w / 2, h * .55f), w * .55f), w * .55f, Offset(w / 2, h * .55f))
        val k = if (lungs != null) 1f else .35f
        val sx = 1f + l * .07f * k; val sy = 1f + l * .045f * k
        if (img == null) { fallback(species, st, w, h); return@Canvas }
        val scale = when (st) { Stage.BABY -> .62f; Stage.TEEN -> .8f; else -> .96f }
        val box = minOf(w / img.width, h * .9f / img.height) * scale
        val bw = (img.width * box).roundToInt(); val bh = (img.height * box).roundToInt()
        val foot = Offset(w / 2, h * .93f)
        withTransform({ scale(sx, sy, foot) }) {
            drawImage(img, IntOffset.Zero, IntSize(img.width, img.height), IntOffset(((w - bw) / 2).roundToInt(), (foot.y - bh).roundToInt()),
                IntSize(bw, bh), filterQuality = FilterQuality.Medium, colorFilter = if (shiny) shinyFilter() else null)
        }
        if (pose == Pose.SLEEP && !still) {
            val z = (time * 2) % 1f
            drawCircle(Color(0x88FFFFFF), w * .02f * (1 + z), Offset(w * (.72f + z * .08f), h * (.35f - z * .15f)))
        }
    }
}

private fun DrawScope.fallback(species: String, st: Stage, w: Float, h: Float) {
    val col = Color(Pets.of(species).color)
    val r = when (st) { Stage.BABY -> .22f; Stage.TEEN -> .28f; else -> .34f } * w
    drawOval(col, Offset(w / 2 - r, h * .9f - r * 1.7f), Size(r * 2, r * 1.7f))
}

/** An egg that wobbles more the closer it is to hatching ([ready] 0..1). */
@Composable
fun EggView(species: String, size: Dp, ready: Float, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val img = Art.egg(ctx, species)
    val t by rememberInfiniteTransition(label = "egg").animateFloat(0f, 1f, infiniteRepeatable(tween(2400, easing = LinearEasing)), label = "t")
    val time = if (still) .3f else t
    Box(modifier.size(size)) {
        Canvas(Modifier.size(size)) {
            val w = this.size.width; val h = this.size.height
            drawOval(Color(0x2A000000), Offset(w * .3f, h * .88f), Size(w * .4f, h * .06f))
            val wob = (if (time < .3f) sin(time / .3f * 4 * PI).toFloat() else 0f) * (3f + 9f * ready.coerceIn(0f, 1f))
            withTransform({ rotate(wob, Offset(w / 2, h * .9f)) }) {
                if (img != null) {
                    val box = minOf(w * .6f / img.width, h * .78f / img.height); val dw = img.width * box; val dh = img.height * box
                    drawImage(img, IntOffset.Zero, IntSize(img.width, img.height), IntOffset(((w - dw) / 2).roundToInt(), (h * .91f - dh).roundToInt()), IntSize(dw.roundToInt(), dh.roundToInt()), filterQuality = FilterQuality.Medium)
                } else {
                    val col = Color(Pets.of(species).color)
                    drawOval(Brush.verticalGradient(listOf(col.copy(alpha = .75f), col)), Offset(w * .28f, h * .2f), Size(w * .44f, h * .7f))
                }
            }
        }
    }
}

/** A picture from the drawables, fitted into [size] (a sea find, a tea house thing). */
@Composable
fun Picture(name: String, size: Dp, modifier: Modifier = Modifier, dim: Boolean = false) {
    val ctx = LocalContext.current
    val img = Art.get(ctx, name)
    Canvas(modifier.size(size)) {
        img ?: return@Canvas
        val s = minOf(this.size.width / img.width, this.size.height / img.height) * .92f
        val w = img.width * s; val h = img.height * s
        drawImage(img, IntOffset.Zero, IntSize(img.width, img.height), IntOffset(((this.size.width - w) / 2).roundToInt(), ((this.size.height - h) / 2).roundToInt()),
            IntSize(w.roundToInt(), h.roundToInt()), filterQuality = FilterQuality.Medium,
            colorFilter = if (dim) ColorFilter.colorMatrix(ColorMatrix(floatArrayOf(0f, 0f, 0f, 0f, 40f, 0f, 0f, 0f, 0f, 50f, 0f, 0f, 0f, 0f, 60f, 0f, 0f, 0f, .3f, 0f))) else null)
    }
}
