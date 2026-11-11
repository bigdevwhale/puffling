package app.puffling.work

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioManager
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import app.puffling.MainActivity
import app.puffling.R
import app.puffling.container
import app.puffling.data.Bell
import app.puffling.data.Settings
import app.puffling.domain.Rand
import app.puffling.domain.hash
import app.puffling.domain.tr
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sqrt

private fun canPost(ctx: Context) = Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

/** Keeps the app awake while a session plays with the screen off: a quiet ongoing notification. */
class SessionService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CH, tr("Идёт практика", "Practice in progress"), NotificationManager.IMPORTANCE_LOW).apply { setShowBadge(false) })
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_IMMUTABLE)
        val n = NotificationCompat.Builder(this, CH).setSmallIcon(R.drawable.ic_stat).setContentTitle(intent?.getStringExtra("name") ?: "Puffling")
            .setContentText(tr("Питомец дышит вместе с вами", "Your pet is breathing with you")).setUsesChronometer(true).setWhen(System.currentTimeMillis())
            .setOngoing(true).setSilent(true).setContentIntent(open).build()
        runCatching { ServiceCompat.startForeground(this, ID, n, if (Build.VERSION.SDK_INT >= 29) ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK else 0) }
        return START_NOT_STICKY
    }

    companion object {
        private const val CH = "session"; private const val ID = 11
        fun start(ctx: Context, name: String) { runCatching { ContextCompat.startForegroundService(ctx, Intent(ctx, SessionService::class.java).putExtra("name", name)) } }
        fun stop(ctx: Context) { runCatching { ctx.stopService(Intent(ctx, SessionService::class.java)) } }
    }
}

/**
 * The phone lying on the belly: its tilt rises and falls with each breath. The slow drift is taken out,
 * the swing is scaled to 0..1, and breaths are counted on the way up.
 */
object BreathSensor : SensorEventListener {
    /** 0 empty .. 1 full, as far as the phone can tell. */
    var level by mutableFloatStateOf(.5f); private set
    /** Breaths a minute over the last minute; 0 until there are a few. */
    var bpm by mutableFloatStateOf(0f); private set
    var on = false; private set
    private var sm: SensorManager? = null
    private var a = 0.0; private var b = 0.0; private var slowA = Double.NaN; private var slowB = Double.NaN
    private var ampA = .001; private var ampB = .001
    private var up = false
    private val rises = ArrayDeque<Long>()
    private val samples = ArrayList<Float>()

    fun available(ctx: Context) = ctx.getSystemService(SensorManager::class.java)?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null

    fun start(ctx: Context) {
        if (on) return
        val m = ctx.getSystemService(SensorManager::class.java) ?: return
        val s = m.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) ?: return
        sm = m; on = true; slowA = Double.NaN; slowB = Double.NaN; ampA = .001; ampB = .001; rises.clear(); samples.clear(); bpm = 0f
        m.registerListener(this, s, 40_000)
    }
    fun stop() { sm?.unregisterListener(this); sm = null; on = false }
    /** The average breaths a minute while it was on. */
    fun average(): Double = if (samples.isEmpty()) 0.0 else samples.filter { it > 0 }.average().takeIf { !it.isNaN() } ?: 0.0

    override fun onSensorChanged(e: SensorEvent) {
        val x = e.values[0].toDouble(); val y = e.values[1].toDouble(); val z = e.values[2].toDouble()
        val g = sqrt(x * x + y * y + z * z).coerceAtLeast(1e-3)
        // two tilts; the one that swings more is the breath
        a += (atan2(y, z) - a) * .15; b += (atan2(x, z) - b) * .15
        if (slowA.isNaN()) { slowA = a; slowB = b }
        slowA += (a - slowA) * .006; slowB += (b - slowB) * .006
        val da = a - slowA; val db = b - slowB
        ampA += (abs(da) - ampA) * .01; ampB += (abs(db) - ampB) * .01
        val v = if (ampA >= ampB) da / (ampA * 2.2) else db / (ampB * 2.2)
        if (g < 4) return
        level = (.5f + .5f * v.toFloat()).coerceIn(0f, 1f)
        val now = System.currentTimeMillis()
        if (!up && v > .35) { up = true; if (rises.isEmpty() || now - rises.last() > 1500) rises.addLast(now) }
        if (up && v < -.2) up = false
        while (rises.isNotEmpty() && now - rises.first() > 60_000) rises.removeFirst()
        if (rises.size >= 3) { val span = (rises.last() - rises.first()) / 60_000f; bpm = if (span > 0) (rises.size - 1) / span else 0f
            if (samples.size < 2000) samples += bpm }
    }
    override fun onAccuracyChanged(s: Sensor?, a: Int) {}
}

/**
 * The mindfulness bell: a few times a day, at random inside the hours set, the phone rings softly.
 * The day's times are fixed by the date (one in each equal slice of the window), so moving the
 * settings doesn't shuffle them twice. Only the next one is ever scheduled.
 */
object Bells {
    private const val CH = "bell"; const val NOTE = 21

    fun times(s: Settings, day: LocalDate, zone: ZoneId = ZoneId.systemDefault()): List<Long> {
        if (!s.bells || day.dayOfWeek.value !in s.bellDays || s.bellTo <= s.bellFrom) return emptyList()
        val n = s.bellsPerDay.coerceIn(1, 12)
        val from = day.atTime(s.bellFrom, 0).atZone(zone).toInstant().toEpochMilli()
        val to = day.atTime(s.bellTo.coerceAtMost(23), if (s.bellTo >= 24) 59 else 0).atZone(zone).toInstant().toEpochMilli()
        val slot = (to - from) / n
        val r = Rand(hash("bell${day.toEpochDay()}"))
        return (0 until n).map { i -> from + slot * i + (slot * (.1f + .8f * r.next())).toLong() }
    }

    fun next(s: Settings, now: Long = System.currentTimeMillis()): Long? {
        val d = LocalDate.now()
        for (k in 0..7) { val t = times(s, d.plusDays(k.toLong())).firstOrNull { it > now + 30_000 }; if (t != null) return t }
        return null
    }

    private fun pending(ctx: Context) = PendingIntent.getBroadcast(ctx, 3, Intent(ctx, BellReceiver::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

    fun schedule(ctx: Context, s: Settings) {
        val am = ctx.getSystemService(AlarmManager::class.java)
        am.cancel(pending(ctx))
        val at = next(s) ?: return
        runCatching {
            if (Build.VERSION.SDK_INT >= 31 && !am.canScheduleExactAlarms()) am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending(ctx))
            else am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending(ctx))
        }
    }

    fun channel(ctx: Context) {
        ctx.getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CH, tr("Колокольчик", "The bell"), NotificationManager.IMPORTANCE_DEFAULT).apply { setSound(null, null); enableVibration(false); setShowBadge(false) })
    }

    /** Rings now unless a call is on; writes the bell down and offers the three breaths. */
    fun ring(ctx: Context, s: Settings, id: Long) {
        val audio = ctx.getSystemService(AudioManager::class.java)
        val inCall = audio.mode == AudioManager.MODE_IN_CALL || audio.mode == AudioManager.MODE_IN_COMMUNICATION
        if (inCall) return
        when (audio.ringerMode) {
            AudioManager.RINGER_MODE_NORMAL -> Synth.ring(s.bellKind, s.volume.coerceAtLeast(.35f))
            AudioManager.RINGER_MODE_VIBRATE -> runCatching { ctx.getSystemService(Vibrator::class.java)?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 60, 400, 60), -1)) }
        }
        if (!canPost(ctx)) return
        channel(ctx)
        val open = PendingIntent.getActivity(ctx, 5, Intent(ctx, MainActivity::class.java).putExtra("bell", id).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val lines = listOf(tr("Остановитесь на три вдоха.", "Stop for three breaths."), tr("Где вы сейчас? Три вдоха.", "Where are you right now? Three breaths."),
            tr("Плечи вниз, три медленных вдоха.", "Shoulders down, three slow breaths."), tr("Колокольчик звонит для вас.", "The bell rings for you."))
        val n = NotificationCompat.Builder(ctx, CH).setSmallIcon(R.drawable.ic_stat).setContentTitle(tr("Колокольчик", "The bell"))
            .setContentText(lines[(id % lines.size).toInt()]).setAutoCancel(true).setContentIntent(open).setTimeoutAfter(20 * 60_000L)
            .addAction(0, tr("Три вдоха", "Three breaths"), open).build()
        try { NotificationManagerCompat.from(ctx).notify(NOTE, n) } catch (_: SecurityException) { }
    }
}

class BellReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val res = goAsync()
        val c = context.container
        CoroutineScope(Dispatchers.Default).launch {
            runCatching {
                val s = c.settings.now()
                if (s.bells && !app.puffling.work.Player.let { it.plan != null && !it.done }) {
                    val id = c.dao.insert(Bell(at = System.currentTimeMillis()))
                    Bells.ring(context, s, id)
                }
                Bells.schedule(context, s)
            }
            kotlinx.coroutines.delay(6000)
            res.finish()
        }
    }
}

/** After a restart the next bell is set again. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val res = goAsync()
        CoroutineScope(Dispatchers.Default).launch { runCatching { Bells.schedule(context, context.container.settings.now()) }; res.finish() }
    }
}
