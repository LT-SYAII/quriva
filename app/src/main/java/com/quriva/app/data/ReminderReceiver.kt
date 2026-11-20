package com.quriva.app.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.quriva.app.MainActivity
import com.quriva.app.R

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val prefs = AppPreferences(context)
        if (!prefs.reminderEnabled) return

        // Reschedule for tomorrow (helps when setAndAllowWhileIdle fallback used)
        try {
            ReminderScheduler.schedule(context, prefs.reminderHour, prefs.reminderMinute)
        } catch (_: Throwable) { }

        showNotification(context)
    }

    private fun showNotification(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Pengingat Baca Al-Qur'an",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Pengingat harian untuk membaca Al-Qur'an"
            }
            nm.createNotificationChannel(channel)
        }

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pi = PendingIntent.getActivity(
            context,
            2001,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notif = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_book_open)
            .setContentTitle("Waktunya membaca Al-Qur'an")
            .setContentText("Luangkan waktu sejenak untuk tilawah hari ini.")
            .setStyle(NotificationCompat.BigTextStyle().bigText("Luangkan waktu sejenak untuk tilawah hari ini. Sedikit demi sedikit, istiqamah lebih baik."))
            .setContentIntent(pi)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        try {
            nm.notify(NOTIF_ID, notif)
        } catch (_: SecurityException) {
            // POST_NOTIFICATIONS not granted
        } catch (_: Throwable) { }
    }

    companion object {
        const val CHANNEL_ID = "quriva_daily_reminder"
        const val NOTIF_ID = 41001
    }
}
