package com.quriva.app.data

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.quriva.app.MainActivity
import com.quriva.app.R

enum class NotificationAccess {
    GRANTED,
    PERMISSION_MISSING,
    APP_BLOCKED,
    CHANNEL_BLOCKED
}

/** Centralized notification channels, access diagnostics, and local tests. */
object NotificationCenter {

    const val REMINDER_CHANNEL_ID = "quriva_daily_reminder"
    const val UPDATE_CHANNEL_ID = "quriva_app_updates"

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannels(
            listOf(
                NotificationChannel(
                    REMINDER_CHANNEL_ID,
                    "Pengingat Baca Al-Qur'an",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply { description = "Pengingat harian untuk membaca Al-Qur'an" },
                NotificationChannel(
                    UPDATE_CHANNEL_ID,
                    "Pembaruan Quriva",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply { description = "Pemberitahuan saat versi Quriva terbaru tersedia" }
            )
        )
    }

    fun access(context: Context, channelId: String = REMINDER_CHANNEL_ID): NotificationAccess {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return NotificationAccess.PERMISSION_MISSING
        }
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            return NotificationAccess.APP_BLOCKED
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (manager.getNotificationChannel(channelId)?.importance == NotificationManager.IMPORTANCE_NONE) {
                return NotificationAccess.CHANNEL_BLOCKED
            }
        }
        return NotificationAccess.GRANTED
    }

    fun openSettings(context: Context, channelId: String = REMINDER_CHANNEL_ID) {
        val appDetails = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            android.net.Uri.parse("package:${context.packageName}")
        )
        val currentAccess = access(context, channelId)
        val preferred = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            currentAccess == NotificationAccess.CHANNEL_BLOCKED
        ) {
            Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                .putExtra(Settings.EXTRA_CHANNEL_ID, channelId)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        } else {
            appDetails
        }
        val opened = runCatching {
            context.startActivity(preferred.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }.isSuccess
        if (!opened) {
            runCatching { context.startActivity(appDetails.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        }
    }

    fun notifyDaily(context: Context, test: Boolean = false): Boolean {
        createChannels(context)
        if (access(context, REMINDER_CHANNEL_ID) != NotificationAccess.GRANTED) return false
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val openApp = PendingIntent.getActivity(
            context,
            if (test) TEST_NOTIFICATION_ID else REMINDER_NOTIFICATION_ID,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val body = if (test) {
            "Notifikasi Quriva berfungsi. Pengingat harian siap digunakan."
        } else {
            "Luangkan waktu sejenak untuk tilawah hari ini. Sedikit demi sedikit, istiqamah lebih baik."
        }
        val notification = NotificationCompat.Builder(context, REMINDER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_quriva)
            .setContentTitle(if (test) "Uji notifikasi Quriva" else "Waktunya membaca Al-Qur'an")
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        return runCatching {
            manager.notify(if (test) TEST_NOTIFICATION_ID else REMINDER_NOTIFICATION_ID, notification)
            true
        }.getOrDefault(false)
    }

    fun notifyUpdate(context: Context, tag: String, title: String): Boolean {
        createChannels(context)
        if (access(context, UPDATE_CHANNEL_ID) != NotificationAccess.GRANTED) return false
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val openApp = PendingIntent.getActivity(
            context,
            tag.hashCode(),
            Intent(context, MainActivity::class.java).apply {
                putExtra(MainActivity.EXTRA_CHECK_UPDATE, true)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, UPDATE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_quriva)
            .setContentTitle("Pembaruan $tag tersedia")
            .setContentText(title)
            .setStyle(NotificationCompat.BigTextStyle().bigText(title))
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        return runCatching {
            manager.notify(UPDATE_NOTIFICATION_ID, notification)
            true
        }.getOrDefault(false)
    }

    private const val REMINDER_NOTIFICATION_ID = 41_001
    private const val TEST_NOTIFICATION_ID = 41_002
    private const val UPDATE_NOTIFICATION_ID = 41_003
}
