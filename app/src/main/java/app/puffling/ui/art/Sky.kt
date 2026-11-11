package app.puffling.ui.art

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import app.puffling.data.Outside
import app.puffling.data.Settings
import app.puffling.data.Sun
import app.puffling.data.Weather
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * What the moon window shows: [day] 0 night .. 1 full day, [glow] 0..1 how much dawn or dusk colours it,
 * and the weather (CLEAR when unknown).
 */
data class Sky(val day: Float, val glow: Float, val weather: Weather) {
    val night get() = day < .25f

    companion object {
        fun of(s: Settings, now: Long = System.currentTimeMillis()): Sky {
            val o = Outside.of(s.outside)?.takeIf { now - it.at < 6 * 3600_000L }
            val today = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val (rise, set) = when {
                o != null && o.sunrise in today..(today + 86400_000) -> o.sunrise to o.sunset
                !s.lat.isNaN() -> Sun.riseSet(s.lat, s.lon, now)
                else -> (today + 7 * 3600_000L) to (today + 19 * 3600_000L)
            }
            val h = 3600_000f
            val day = min(((now - rise) / h + .5f).coerceIn(0f, 1f), ((set - now) / h + .5f).coerceIn(0f, 1f))
            val glow = max(1f - abs(now - rise) / (1.2f * h), 1f - abs(now - set) / (1.2f * h)).coerceIn(0f, 1f)
            return Sky(day, glow, o?.weather ?: Weather.CLEAR)
        }
    }
}

fun mixC(a: Color, b: Color, t: Float): Color { val k = t.coerceIn(0f, 1f); return Color(a.red + (b.red - a.red) * k, a.green + (b.green - a.green) * k, a.blue + (b.blue - a.blue) * k, 1f) }

/**
 * The sea seen through a round window of centre [c] and radius [r]: the sky by the hour and weather,
 * the sea as high as the week's [tide] (0..1), its waves swelling with [lungs] when a breath is given.
 * [t] 0..1 loops for clouds, rain, twinkles and waves.
 */
fun DrawScope.seaWindow(c: Offset, r: Float, sky: Sky, t: Float, tide: Float, lungs: Float? = null) {
    val circle = Path().apply { addOval(androidx.compose.ui.geometry.Rect(c, r)) }
    clipPath(circle) {
        val grey = sky.weather in setOf(Weather.OVERCAST, Weather.RAIN, Weather.STORM, Weather.FOG, Weather.SNOW)
        var top = mixC(Color(0xFF0B1430), Color(0xFF5BA3DE), sky.day); var bottom = mixC(Color(0xFF223463), Color(0xFFB8DFF5), sky.day)
        if (sky.glow > 0) { top = mixC(top, Color(0xFF5A4C9C), sky.glow * .6f); bottom = mixC(bottom, Color(0xFFF7A46C), sky.glow * .8f) }
        if (grey) { top = mixC(top, mixC(Color(0xFF262C38), Color(0xFF8C96A2), sky.day), .75f); bottom = mixC(bottom, mixC(Color(0xFF2E3442), Color(0xFFB9C1CA), sky.day), .75f) }
        val horizon = c.y + r * (.28f - tide.coerceIn(0f, 1f) * .22f)
        drawRect(Brush.verticalGradient(listOf(top, bottom), c.y - r, horizon), Offset(c.x - r, c.y - r), Size(2 * r, horizon - (c.y - r) + 2))
        val tw = t * 2 * PI.toFloat()
        // sun or moon
        if (sky.night && !grey) {
            for (k in 0 until 14) { val a = .5f + .5f * sin(tw * 2 + k * 1.7f)
                drawCircle(Color(0xFFFFF6D8).copy(alpha = .4f + .5f * a), r * .012f, Offset(c.x - r * .9f + r * 1.8f * ((k * 37) % 100) / 100f, c.y - r * .95f + (horizon - c.y + r) * .8f * ((k * 53) % 100) / 100f)) }
            drawCircle(Color(0xFFFFF1C2), r * .11f, Offset(c.x + r * .38f, c.y - r * .48f)); drawCircle(top, r * .095f, Offset(c.x + r * .42f, c.y - r * .52f))
        } else if (!grey) drawCircle(Color(0xFFFFE89A), r * .12f, Offset(c.x - r * .35f, horizon - r * (.25f + .45f * sky.day)))
        // clouds
        val cloud = if (sky.night) Color(0xFF4A5576) else Color.White
        val nClouds = when (sky.weather) { Weather.CLEAR -> 1; Weather.CLOUDY -> 3; else -> 4 }
        for (k in 0 until nClouds) {
            val cx = c.x - r * 1.3f + r * 2.6f * ((t * .5f + k * .37f) % 1f); val cy = c.y - r * (.65f - k * .16f)
            val col = if (grey) mixC(Color(0xFF39404E), Color(0xFFD5DAE0), sky.day) else cloud
            drawCircle(col.copy(alpha = .9f), r * .09f, Offset(cx, cy)); drawCircle(col.copy(alpha = .9f), r * .12f, Offset(cx + r * .11f, cy - r * .04f)); drawCircle(col.copy(alpha = .9f), r * .08f, Offset(cx + r * .22f, cy))
        }
        // a far rock with a lighthouse
        val rock = mixC(Color(0xFF1B2433), Color(0xFF6C7A86), sky.day)
        val rp = Path().apply { moveTo(c.x + r * .45f, horizon + 1); lineTo(c.x + r * .55f, horizon - r * .1f); lineTo(c.x + r * .7f, horizon - r * .13f); lineTo(c.x + r * .82f, horizon - r * .05f); lineTo(c.x + r, horizon + 1); close() }
        drawPath(rp, rock)
        drawRect(mixC(Color(0xFFDCD6CC), Color(0xFFF4F0E8), sky.day), Offset(c.x + r * .655f, horizon - r * .26f), Size(r * .035f, r * .14f))
        drawRect(Color(0xFFC0473F), Offset(c.x + r * .655f, horizon - r * .2f), Size(r * .035f, r * .025f))
        if (sky.day < .6f) { val beam = .5f + .5f * sin(tw * 3)
            drawCircle(Color(0xFFFFE9A8).copy(alpha = .35f + .5f * beam), r * .03f, Offset(c.x + r * .672f, horizon - r * .27f)) }
        // the sea
        val sea0 = mixC(Color(0xFF0E2238), Color(0xFF3D86B5), sky.day); val sea1 = mixC(Color(0xFF07131F), Color(0xFF1F5C86), sky.day)
        drawRect(Brush.verticalGradient(listOf(if (grey) mixC(sea0, Color(0xFF58707F), .4f) else sea0, sea1), horizon, c.y + r), Offset(c.x - r, horizon), Size(2 * r, c.y + r - horizon))
        val swell = lungs ?: (.5f + .5f * sin(tw))
        for (k in 0 until 6) {
            val y = horizon + (c.y + r - horizon) * (k + .6f) / 6.5f
            val amp = r * (.008f + .02f * swell) * (1 + k * .4f)
            val p = Path(); var x = c.x - r
            p.moveTo(x, y)
            while (x <= c.x + r) { x += r * .04f; p.lineTo(x, y + amp * sin((x / r) * (5f + k) + tw * (1 + k * .2f) + k)) }
            drawPath(p, Color.White.copy(alpha = .10f + .05f * k * (if (sky.night) .4f else 1f)), style = Stroke(r * .008f * (1 + k * .3f)))
        }
        // rain, snow, fog
        if (sky.weather == Weather.RAIN || sky.weather == Weather.STORM) for (k in 0 until 26) {
            val fx = ((k * .137f) % 1f); val fy = (t * 4 + k * .23f) % 1f
            val x = c.x - r + 2 * r * fx; val y = c.y - r + 2 * r * fy
            drawLine(Color(0x99C8D8EE), Offset(x, y), Offset(x - r * .03f, y + r * .08f), r * .006f)
        }
        if (sky.weather == Weather.STORM && t % .5f < .02f) drawRect(Color.White.copy(alpha = .5f), Offset(c.x - r, c.y - r), Size(2 * r, 2 * r))
        if (sky.weather == Weather.SNOW) for (k in 0 until 30) {
            val fx = ((k * .173f) % 1f) + .02f * sin(t * 12 + k); val fy = (t * 1.5f + k * .1f) % 1f
            drawCircle(Color.White.copy(alpha = .9f), r * .011f, Offset(c.x - r + 2 * r * fx, c.y - r + 2 * r * fy))
        }
        if (sky.weather == Weather.FOG) drawRect(Color(0xFFE6E9EE).copy(alpha = .55f), Offset(c.x - r, c.y - r), Size(2 * r, 2 * r))
    }
}
