package com.quriva.app.data

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.quriva.app.widget.QurivaWidgetProvider
import java.util.concurrent.TimeUnit

/**
 * Schedules one alarm at a time for the nearest enabled prayer reminder.
 * After it fires, the receiver schedules the following prayer. A boot receiver
 * restores the chain after a reboot or timezone change.
 */
object PrayerReminderScheduler {
    const val ACTION_REMIND = "com.quriva.app.action.PRAYER_REMINDER"
    private const val REQUEST_CODE = 45_201
    private const val EXTRA_PRAYER = "prayer"
    private const val EXTRA_MINUTES = "minutes_before"
    private const val EXTRA_TIME_LABEL = "time_label"

    fun reschedule(context: Context) {
        val appContext = context.applicationContext
        val prefs = AppPreferences(appContext)
        cancel(appContext)
        if (!prefs.prayerRemindersEnabled || prefs.prayerEnabledIds.isEmpty()) return
        val city = PrayerCities.byId(prefs.prayerCityId)
        val method = runCatching { PrayerMethod.valueOf(prefs.prayerMethod) }
            .getOrDefault(PrayerMethod.KEMENAG)
        val now = System.currentTimeMillis()
        val candidates = ArrayList<Candidate>()

        for (dayOffset in 0..2) {
            val schedule = PrayerTimes.schedule(
                city = city,
                method = method,
                asrHanafi = prefs.prayerAsrHanafi,
                now = now + TimeUnit.DAYS.toMillis(dayOffset.toLong())
            )
            schedule.times
                .filter { it.prayer.isPrayer && it.prayer.id in prefs.prayerEnabledIds }
                .forEach { time ->
                    val preMinutes = prefs.prayerReminderMinutes
                    if (preMinutes > 0) {
                        val preAt = time.millis - TimeUnit.MINUTES.toMillis(preMinutes.toLong())
                        if (preAt > now + 5_000L) {
                            candidates += Candidate(
                                prayer = time.prayer,
                                triggerAt = preAt,
                                prayerAt = time.millis,
                                minutesBefore = preMinutes
                            )
                        }
                    }
                    if (time.millis > now + 5_000L) {
                        candidates += Candidate(
                            prayer = time.prayer,
                            triggerAt = time.millis,
                            prayerAt = time.millis,
                            minutesBefore = 0
                        )
                    }
                }
        }

        val next = candidates.minByOrNull { it.triggerAt } ?: return
        val label = PrayerTimes.formatClock(next.prayerAt, city.timeZoneId)
        val intent = Intent(appContext, PrayerAlarmReceiver::class.java).apply {
            action = ACTION_REMIND
            putExtra(EXTRA_PRAYER, next.prayer.name)
            putExtra(EXTRA_MINUTES, next.minutesBefore)
            putExtra(EXTRA_TIME_LABEL, label)
        }
        val pending = PendingIntent.getBroadcast(
            appContext,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        runCatching {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    next.triggerAt,
                    pending
                )
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    next.triggerAt,
                    pending
                )
            }
        }
    }

    fun cancel(context: Context) {
        val appContext = context.applicationContext
        val intent = Intent(appContext, PrayerAlarmReceiver::class.java).apply {
            action = ACTION_REMIND
        }
        val pending = PendingIntent.getBroadcast(
            appContext,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pending)
        pending.cancel()
    }

    private data class Candidate(
        val prayer: Prayer,
        val triggerAt: Long,
        val prayerAt: Long,
        val minutesBefore: Int
    )
}

class PrayerAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != PrayerReminderScheduler.ACTION_REMIND) return
        val prefs = AppPreferences(context.applicationContext)
        val prayer = runCatching {
            Prayer.valueOf(intent.getStringExtra("prayer").orEmpty())
        }.getOrNull() ?: return
        val minutes = intent.getIntExtra("minutes_before", 0)
        val label = intent.getStringExtra("time_label").orEmpty()
        val city = PrayerCities.byId(prefs.prayerCityId)
        NotificationCenter.notifyPrayer(
            context = context,
            prayer = prayer,
            timeLabel = label,
            cityName = "${city.name}, ${city.province}",
            minutesBefore = minutes
        )
        if (prefs.prayerRemindersEnabled) {
            PrayerReminderScheduler.reschedule(context)
        }
    }
}

class PrayerBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED -> {
                PrayerReminderScheduler.reschedule(context)
                QurivaWidgetProvider.updateAll(context)
            }
        }
    }
}
