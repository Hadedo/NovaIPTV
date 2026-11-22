package com.hadedo.novaiptv

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import org.json.JSONObject

/** جسر بين الإشعار وواجهة التطبيق: عند الضغط على الإشعار يُفتح تبويب الرياضة. */
object SportsNav {
    const val EXTRA_OPEN_SPORTS = "devkan_open_sports"
    var openRequest by mutableIntStateOf(0)

    fun handle(intent: Intent?) {
        if (intent?.getBooleanExtra(EXTRA_OPEN_SPORTS, false) == true) {
            intent.removeExtra(EXTRA_OPEN_SPORTS)
            openRequest++
        }
    }
}

object SportsReminders {
    private const val PREFS = "devkan_sports_reminders"
    const val CHANNEL_ID = "devkan_sports_reminders"

    private fun prefs(ctx: Context) = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun isSet(ctx: Context, matchId: String): Boolean {
        val raw = prefs(ctx).getString(matchId, null) ?: return false
        return try { JSONObject(raw).getLong("at") > System.currentTimeMillis() } catch (_: Exception) { false }
    }

    /** يجدول تنبيهاً قبل kickoffMs بـ leadMin دقيقة. يعيد false إذا بدأت المباراة فعلاً. */
    fun schedule(ctx: Context, matchId: String, kickoffMs: Long, leadMin: Int, title: String, body: String): Boolean {
        val now = System.currentTimeMillis()
        if (kickoffMs <= now + 30_000L) return false
        val at = maxOf(kickoffMs - leadMin * 60_000L, now + 3_000L)
        prefs(ctx).edit()
            .putString(matchId, JSONObject().put("at", at).put("title", title).put("body", body).toString())
            .apply()
        setAlarm(ctx, matchId, at, title, body)
        return true
    }

    fun cancel(ctx: Context, matchId: String) {
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(pending(ctx, matchId, "", ""))
        remove(ctx, matchId)
    }

    fun remove(ctx: Context, matchId: String) {
        prefs(ctx).edit().remove(matchId).apply()
    }

    /** بعد إعادة تشغيل الجهاز. */
    fun rescheduleAll(ctx: Context) {
        val now = System.currentTimeMillis()
        for ((id, value) in prefs(ctx).all) {
            try {
                val o = JSONObject(value as String)
                val at = o.getLong("at")
                if (at > now) setAlarm(ctx, id, at, o.getString("title"), o.getString("body")) else remove(ctx, id)
            } catch (_: Exception) {
                remove(ctx, id)
            }
        }
    }

    private fun setAlarm(ctx: Context, matchId: String, at: Long, title: String, body: String) {
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending(ctx, matchId, title, body))
    }

    private fun pending(ctx: Context, matchId: String, title: String, body: String): PendingIntent {
        val i = Intent(ctx, SportsReminderReceiver::class.java)
            .putExtra("id", matchId).putExtra("title", title).putExtra("body", body)
        return PendingIntent.getBroadcast(ctx, matchId.hashCode(), i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }
}

class SportsReminderReceiver : BroadcastReceiver() {
    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getStringExtra("id") ?: return
        val title = intent.getStringExtra("title") ?: return
        val body = intent.getStringExtra("body").orEmpty()
        SportsReminders.remove(context, id)

        val nm = NotificationManagerCompat.from(context)
        if (!nm.areNotificationsEnabled()) return
        if (Build.VERSION.SDK_INT >= 26) {
            val ch = NotificationChannel(SportsReminders.CHANNEL_ID, "DEVKAN Sports", NotificationManager.IMPORTANCE_HIGH)
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
        }
        val open = Intent(context, MainActivity::class.java).apply {
            putExtra(SportsNav.EXTRA_OPEN_SPORTS, true)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pi = PendingIntent.getActivity(context, id.hashCode(), open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val n = NotificationCompat.Builder(context, SportsReminders.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()
        nm.notify(id.hashCode(), n)
    }
}

class SportsBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) SportsReminders.rescheduleAll(context)
    }
}
