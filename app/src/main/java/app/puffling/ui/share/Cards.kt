package app.puffling.ui.share

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.Typeface
import android.provider.MediaStore
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import app.puffling.R
import app.puffling.data.Pet
import app.puffling.domain.Deep
import app.puffling.domain.Element
import app.puffling.domain.Pets
import app.puffling.domain.Shop
import app.puffling.domain.Span
import app.puffling.domain.Stage
import app.puffling.domain.Stats
import app.puffling.domain.clock
import app.puffling.domain.count
import app.puffling.domain.dayOf
import app.puffling.domain.depthOf
import app.puffling.domain.fmt
import app.puffling.domain.fmtDay
import app.puffling.domain.mins
import app.puffling.domain.nat
import app.puffling.domain.sp
import app.puffling.domain.stage
import app.puffling.domain.tr
import app.puffling.domain.weekStart
import app.puffling.ui.Data
import app.puffling.ui.art.Art
import app.puffling.ui.art.Pose
import app.puffling.ui.art.Sky
import app.puffling.ui.art.WIN_R
import app.puffling.ui.art.WIN_X
import app.puffling.ui.art.WIN_Y
import app.puffling.ui.art.frameOf
import app.puffling.ui.art.seaWindow
import app.puffling.ui.art.shinyFilter
import app.puffling.ui.art.sprite
import java.io.File
import kotlin.math.min

enum class CardKind(val ru: String, val en: String) {
    SESSION("Практика", "Practice"), WEEK("Неделя", "Week"), HOUSE("Чайная", "Tea house"),
    PET("Питомец", "Pet"), BESTIARY("Бестиарий", "Bestiary"), AQUARIUM("Аквариум", "Aquarium");
    val label get() = tr(ru, en)
}

data class ShareReq(val kind: CardKind, val id: Long = 0)

data class Look(val story: Boolean = false, val bg: Int = 0)

/** Backgrounds: two colours top to bottom and whether the text is light. */
val BACKS = listOf(
    Triple(0xFF3E8FB0.toInt(), 0xFF173A52.toInt(), true), Triple(0xFF1A2A3A.toInt(), 0xFF0B1219.toInt(), true), Triple(0xFF8C7BB8.toInt(), 0xFF3A2E5C.toInt(), true),
    Triple(0xFFF3F0EA.toInt(), 0xFFE6DCC8.toInt(), false), Triple(0xFFF2C38A.toInt(), 0xFFD9707A.toInt(), false),
)

class Painter(private val ctx: Context, private val d: Data, private val look: Look) {
    private val W = 1080f; private val H = if (look.story) 1920f else 1350f
    private val bold: Typeface = ResourcesCompat.getFont(ctx, R.font.nunito_bold) ?: Typeface.DEFAULT_BOLD
    private val reg: Typeface = ResourcesCompat.getFont(ctx, R.font.nunito_regular) ?: Typeface.DEFAULT
    private val num: Typeface = ResourcesCompat.getFont(ctx, R.font.inter_semibold) ?: Typeface.DEFAULT_BOLD
    private val back = BACKS[look.bg.coerceIn(0, BACKS.lastIndex)]
    private val light = back.third
    private val ink = if (light) 0xFFFFFFFF.toInt() else 0xFF22303A.toInt()
    private val sub = if (light) 0xC8FFFFFF.toInt() else 0xA022303A.toInt()
    private val panel = if (light) 0x24FFFFFF else 0x1422303A
    private val top = if (look.story) 300f else 200f
    private val bottom get() = H - 150f

    private fun draw(kicker: String, body: DrawScope.() -> Unit): Bitmap {
        val img = ImageBitmap(W.toInt(), H.toInt())
        val canvas = androidx.compose.ui.graphics.Canvas(img)
        CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, canvas, Size(W, H)) {
            drawRect(Brush.linearGradient(listOf(Color(back.first), Color(back.second)), Offset.Zero, Offset(W * .4f, H)))
            for ((x, y, r) in listOf(Triple(.92f, .08f, .42f), Triple(.05f, .62f, .36f), Triple(.8f, .95f, .3f)))
                drawCircle(Brush.radialGradient(listOf(Color.White.copy(alpha = if (light) .19f else .25f), Color.Transparent), Offset(W * x, H * y), W * r), W * r, Offset(W * x, H * y))
            val hy = if (look.story) 130f else 70f
            drawRoundRect(Color(panel), Offset(60f, hy), Size(260f, 64f), androidx.compose.ui.geometry.CornerRadius(32f))
            Art.pet(ctx, "puffin", 0)?.let { sprite(it, 106f, hy + 58f, 50f) }
            text("Puffling", 140f, hy + 44f, 32f)
            text(kicker.uppercase(), W - 60f, hy + 44f, 30f, bold, sub, Paint.Align.RIGHT)
            body()
            text(tr("Puffling · питомец, который дышит вместе с вами", "Puffling · a pet that breathes with you"), W / 2, H - 64f, 28f, reg, sub, Paint.Align.CENTER)
        }
        return img.asAndroidBitmap()
    }

    private fun DrawScope.text(s: String, x: Float, y: Float, size: Float, face: Typeface = bold, color: Int = ink, align: Paint.Align = Paint.Align.LEFT, maxW: Float = W - 120f) {
        val pt = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = size; typeface = face; this.color = color; textAlign = align }
        var str = s; if (pt.measureText(str) > maxW) { while (pt.measureText("$str…") > maxW && str.length > 2) str = str.dropLast(1); str += "…" }
        drawContext.canvas.nativeCanvas.drawText(str, x, y, pt)
    }
    /** Wrapped text, up to [lines] lines; returns the y below it. */
    private fun DrawScope.para(s: String, x: Float, y: Float, size: Float, face: Typeface = reg, color: Int = sub, w: Float = W - 120f, lines: Int = 4, center: Boolean = false): Float {
        val pt = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = size; typeface = face; this.color = color; textAlign = if (center) Paint.Align.CENTER else Paint.Align.LEFT }
        val out = ArrayList<String>(); var cur = ""
        for (word in s.split(' ')) { val t = if (cur.isEmpty()) word else "$cur $word"; if (pt.measureText(t) > w && cur.isNotEmpty()) { out += cur; cur = word } else cur = t }
        if (cur.isNotEmpty()) out += cur
        var yy = y
        out.take(lines).forEach { drawContext.canvas.nativeCanvas.drawText(it, x, yy, pt); yy += size * 1.3f }
        return yy
    }
    private fun DrawScope.facts(items: List<Pair<String, String>>, y: Float, h: Float = 170f) {
        val n = items.size; val gap = 18f; val w = (W - 120f - gap * (n - 1)) / n
        items.forEachIndexed { i, (v, l) -> val x = 60f + i * (w + gap)
            drawRoundRect(Color(panel), Offset(x, y), Size(w, h), androidx.compose.ui.geometry.CornerRadius(36f))
            text(v, x + w / 2, y + h * .52f, if (v.length > 7) 44f else 56f, num, ink, Paint.Align.CENTER, w - 24); text(l, x + w / 2, y + h * .8f, 28f, reg, sub, Paint.Align.CENTER, w - 24) }
    }
    private fun DrawScope.pet(p: Pet?, pose: Pose, cx: Float, foot: Float, box: Float) {
        p ?: return
        val st = p.stage()
        val img = (if (st == Stage.EGG) Art.egg(ctx, p.species) else Art.pet(ctx, p.species, frameOf(st, pose))) ?: return
        val k = when (st) { Stage.EGG -> .7f; Stage.BABY -> .62f; Stage.TEEN -> .8f; else -> .96f }
        drawOval(Color(0x33000000), Offset(cx - box * k * .3f, foot - box * .03f), Size(box * k * .6f, box * .06f))
        sprite(img, cx, foot, box * k, filter = if (p.shiny) shinyFilter() else null)
    }
    private fun DrawScope.house(x: Float, y: Float, w: Float) {
        val h = w / 1.5f
        translate(x, y) {
            val sky = Sky.of(d.s)
            seaWindow(Offset(w * WIN_X, h * WIN_Y), w * WIN_R, sky, .3f, d.tide)
            Art.get(ctx, "room_bg")?.let { drawImage(it, androidx.compose.ui.unit.IntOffset.Zero, androidx.compose.ui.unit.IntSize(it.width, it.height), androidx.compose.ui.unit.IntOffset.Zero, androidx.compose.ui.unit.IntSize(w.toInt(), h.toInt())) }
            for (it in Shop.all.filter { s -> s.id in d.s.items - d.s.hidden }.sortedBy { if (it.wall) 0 else 1 }) { val img = Art.item(ctx, it.id) ?: continue
                val iw = w * it.w; val ih = iw * img.height / img.width
                sprite(img, w * it.x, if (it.wall) h * it.y + ih / 2 else h * it.y, maxOf(iw, ih)) }
            d.team.filter { it.slot in 1..2 }.forEach { p -> pet(p, Pose.IDLE, w * (if (p.slot == 1) .2f else .8f), h * .97f, h * .34f) }
            pet(d.leader, Pose.IDLE, w / 2, h, h * .5f)
        }
    }

    fun render(r: ShareReq): Pair<Bitmap, String> = when (r.kind) {
        CardKind.SESSION -> session(r.id)
        CardKind.WEEK -> week()
        CardKind.HOUSE -> houseCard()
        CardKind.PET -> petCard(r.id)
        CardKind.BESTIARY -> bestiary()
        CardKind.AQUARIUM -> aquarium()
    }

    private fun session(id: Long): Pair<Bitmap, String> {
        val x = d.sessions.firstOrNull { it.id == id } ?: d.sessions.firstOrNull { it.practice != "bell" } ?: return week()
        val el = Element.of(x.el)
        val b = draw(tr("практика", "practice")) {
            var y = top + 80
            text(x.name, 60f, y, 76f); y += 60
            text(fmtDay(dayOf(x.start)) + " · " + el.label.lowercase(), 60f, y, 34f, reg, sub); y += 50
            val holds = x.holdList()
            facts(listOfNotNull(mins(x.secs) to tr("тишины", "of quiet"), if (holds.isNotEmpty()) clock(holds.max()) to tr("задержка", "hold") else null,
                if (holds.isNotEmpty()) "${depthOf(holds.max())} " + tr("м", "m") to tr("глубина", "depth") else "+${x.shells}" to tr("ракушек", "shells")), y); y += 210
            if (x.notes.isNotBlank()) y = para("«" + x.notes.lines().first() + "»", 60f, y + 20, 40f, reg, ink, lines = 3) + 10
            val room = bottom - y; if (room > 260) pet(d.leader, Pose.JOY, W / 2, bottom, min(room, 620f))
        }
        return b to tr("Подышал{|а} с Puffling: ${x.name.lowercase()}", "Breathed with Puffling: ${x.name.lowercase()}")
    }

    private fun week(): Pair<Bitmap, String> {
        val sp = Span.of(Span.WEEK); val st = Stats(d.sessions, d.bells, sp)
        val b = draw(tr("неделя", "week")) {
            var y = top + 80
            text(tr("Неделя спокойствия", "A week of calm"), 60f, y, 72f); y += 56
            text(sp.label(), 60f, y, 34f, reg, sub); y += 40
            facts(listOf(mins(st.secs) to tr("практики", "practice"), "${st.days.size}/7" to tr("дней", "days"), (if (st.bellsRung.isEmpty()) "–" else "${st.bellsAnswered}") to tr("колокольчиков", "bells")), y); y += 210
            val gw = W - 120f; val gh = gw / 2
            val bars = st.bars(sp.from); val mx = (bars.maxOfOrNull { it.second } ?: 1.0).coerceAtLeast(1.0)
            bars.forEachIndexed { i, (l, v) -> val bw = (gw - 6 * 18f) / 7; val x = 60f + i * (bw + 18f); val bh = ((gh - 60) * v / mx).toFloat().coerceAtLeast(6f)
                drawRoundRect(Color.White.copy(alpha = if (light) .85f else .9f).let { if (light) it else Color(0xFF3E8FB0) }, Offset(x, y + gh - 40 - bh), Size(bw, bh), androidx.compose.ui.geometry.CornerRadius(14f))
                text(l, x + bw / 2, y + gh, 28f, reg, sub, Paint.Align.CENTER) }
            y += gh + 40
            val room = bottom - y; if (room > 240) pet(d.leader, Pose.JOY, W / 2, bottom, min(room, 520f))
        }
        return b to tr("Моя неделя в Puffling", "My week in Puffling")
    }

    private fun houseCard(): Pair<Bitmap, String> {
        val b = draw(tr("моя чайная", "my tea house")) {
            val w = W - 120f
            house(60f, top + 20, w)
            var y = top + 70 + w / 1.5f
            text(tr("Чайная на скале", "The tea house on the cliff"), 60f, y + 40, 64f)
            text(d.team.joinToString(" · ") { "${it.name} (${it.sp().name.lowercase()})" }, 60f, y + 100, 32f, reg, sub)
            y += 140
            facts(listOf(mins(d.sessions.filter { it.practice != "bolt" }.sumOf { it.secs }) to tr("вместе", "together"), "${(d.s.items - d.s.hidden).size}/${Shop.all.size}" to tr("вещей", "things"),
                "${d.pets.count { it.hatched > 0 }}" to tr("питомцев", "pets")), y, 150f)
        }
        return b to tr("Моя чайная в Puffling", "My tea house in Puffling")
    }

    private fun petCard(id: Long): Pair<Bitmap, String> {
        val pt = d.pets.firstOrNull { it.id == id } ?: d.leader ?: return houseCard()
        val b = draw(tr("мой питомец", "my pet")) {
            val col = Color(Pets.of(pt.species).color)
            val cy = top + (if (look.story) 450 else 290)
            drawCircle(Brush.radialGradient(listOf(col.copy(alpha = .55f), col.copy(alpha = 0f)), Offset(W / 2, cy), 420f), if (look.story) 400f else 320f, Offset(W / 2, cy))
            pet(pt, Pose.PRACTICE, W / 2, cy + (if (look.story) 400 else 290), if (look.story) 820f else 560f)
            var y = cy + (if (look.story) 520 else 380)
            text(pt.name, W / 2, y, 96f, align = Paint.Align.CENTER)
            text("${pt.sp().name} · ${pt.stage().label}" + (app.puffling.domain.Form.of(pt.form)?.let { " · ${it.label}" } ?: ""), W / 2, y + 64, 38f, reg, sub, Paint.Align.CENTER)
            y += 110
            facts(listOf("${pt.sessions}" to tr("практик вместе", "practices"), fmt(pt.growth.toLong()) to tr("рост", "growth"), pt.nat().label to tr("характер", "nature")), y, 150f)
        }
        return b to "${pt.name} – ${pt.sp().name}"
    }

    private fun bestiary(): Pair<Bitmap, String> {
        val owned = d.pets.associateBy { it.species }
        val b = draw(tr("бестиарий", "bestiary")) {
            text(tr("${owned.size} из ${Pets.all.size} найдено", "${owned.size} of ${Pets.all.size} found"), 60f, top + 70, 72f)
            val cols = if (look.story) 4 else 5; val gap = 16f; val cw = (W - 120f - gap * (cols - 1)) / cols; val ch = if (look.story) cw * 1.05f else cw
            val sil = androidx.compose.ui.graphics.ColorFilter.tint(Color.Black.copy(alpha = .35f))
            Pets.all.forEachIndexed { i, s -> val x = 60f + (i % cols) * (cw + gap); val y = top + 130 + (i / cols) * (ch + gap)
                drawRoundRect(Color(panel), Offset(x, y), Size(cw, ch), androidx.compose.ui.geometry.CornerRadius(28f))
                val pt = owned[s.id]
                if (pt != null) pet(pt, Pose.IDLE, x + cw / 2, y + ch - 14, min(cw, ch) * .95f)
                else Art.pet(ctx, s.id, 2)?.let { sprite(it, x + cw / 2, y + ch - 14, min(cw, ch) * .7f, filter = sil) } }
        }
        return b to tr("Мой бестиарий в Puffling", "My Puffling bestiary")
    }

    private fun aquarium(): Pair<Bitmap, String> {
        val best = d.sessions.filter { it.practice != "bolt" }.flatMap { it.holdList() }.maxOrNull() ?: 0
        val b = draw(tr("аквариум", "aquarium")) {
            text(tr("${d.s.finds.size} из ${Deep.all.size} со дна", "${d.s.finds.size} of ${Deep.all.size} from the deep"), 60f, top + 70, 66f)
            text(tr("Глубже всего: ${depthOf(best)} м · задержка ${clock(best)}", "Deepest: ${depthOf(best)} m · a ${clock(best)} hold"), 60f, top + 120, 34f, reg, sub)
            val cols = 4; val gap = 16f; val cw = (W - 120f - gap * (cols - 1)) / cols; val ch = cw
            val dim = androidx.compose.ui.graphics.ColorFilter.tint(Color.Black.copy(alpha = .4f))
            Deep.all.forEachIndexed { i, f -> val x = 60f + (i % cols) * (cw + gap); val y = top + 170 + (i / cols) * (ch + gap)
                drawRoundRect(Brush.verticalGradient(listOf(Color(0xFF3F8FC0), Color(0xFF0B2236)), y, y + ch), Offset(x, y), Size(cw, ch), androidx.compose.ui.geometry.CornerRadius(28f), alpha = if (f.id in d.s.finds) 1f else .45f)
                Art.find(ctx, f.id)?.let { sprite(it, x + cw / 2, y + ch * .88f, cw * .75f, filter = if (f.id in d.s.finds) null else dim) } }
        }
        return b to tr("Мой аквариум в Puffling", "My Puffling aquarium")
    }
}

object Share {
    fun send(ctx: Context, bmp: Bitmap, title: String) {
        val dir = File(ctx.cacheDir, "share").apply { mkdirs() }; val f = File(dir, "puffling.png")
        f.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        val uri = FileProvider.getUriForFile(ctx, ctx.packageName + ".files", f)
        val i = Intent(Intent.ACTION_SEND).setType("image/png").putExtra(Intent.EXTRA_STREAM, uri).putExtra(Intent.EXTRA_TEXT, title).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        ctx.startActivity(Intent.createChooser(i, tr("Поделиться открыткой", "Share the card")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    /** Saves the card to Pictures/Puffling. */
    fun save(ctx: Context, bmp: Bitmap): Boolean = runCatching {
        val v = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "puffling-${System.currentTimeMillis()}.png"); put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Puffling") }
        val uri = ctx.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, v)!!
        ctx.contentResolver.openOutputStream(uri)!!.use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }; true }.getOrDefault(false)
}
