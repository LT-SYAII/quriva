package com.quriva.app.data

import android.content.Context
import androidx.work.*
import java.util.Calendar
import java.util.concurrent.TimeUnit

object IslamicReminderScheduler {
    private const val FRIDAY_WORK = "quriva_friday_reminder"
    private const val EVENT_WORK = "quriva_islamic_event_reminder"
    private const val EVENT_NOW = "quriva_islamic_event_now"
    private const val THURSDAY_WORK = "quriva_thursday_night_reminder"

    fun scheduleFriday(context: Context, hour: Int, minute: Int, append: Boolean = false) {
        val delay = (IslamicCalendar.nextFriday(hour, minute) - System.currentTimeMillis()).coerceAtLeast(1_000)
        val request = OneTimeWorkRequestBuilder<FridayReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS).build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            FRIDAY_WORK,
            if (append) ExistingWorkPolicy.APPEND_OR_REPLACE else ExistingWorkPolicy.REPLACE,
            request
        )
    }
    fun cancelFriday(context: Context) = WorkManager.getInstance(context).cancelUniqueWork(FRIDAY_WORK)

    fun scheduleThursdayNight(context: Context, append: Boolean = false) {
        val now = Calendar.getInstance()
        val target = (now.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 18); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            val delta = (Calendar.THURSDAY - get(Calendar.DAY_OF_WEEK) + 7) % 7
            add(Calendar.DAY_OF_YEAR, if (delta == 0 && timeInMillis <= now.timeInMillis) 7 else delta)
        }
        val request = OneTimeWorkRequestBuilder<ThursdayNightWorker>().setInitialDelay((target.timeInMillis-now.timeInMillis).coerceAtLeast(1000), TimeUnit.MILLISECONDS).build()
        WorkManager.getInstance(context).enqueueUniqueWork(THURSDAY_WORK, if (append) ExistingWorkPolicy.APPEND_OR_REPLACE else ExistingWorkPolicy.REPLACE, request)
    }
    fun cancelThursdayNight(context: Context) = WorkManager.getInstance(context).cancelUniqueWork(THURSDAY_WORK)

    fun scheduleEvents(context: Context, append: Boolean = false) {
        val now = Calendar.getInstance()
        val target = (now.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, 1); set(Calendar.HOUR_OF_DAY, 8); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val request = OneTimeWorkRequestBuilder<IslamicEventWorker>()
            .setInitialDelay((target.timeInMillis - now.timeInMillis).coerceAtLeast(1_000), TimeUnit.MILLISECONDS).build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            EVENT_WORK,
            if (append) ExistingWorkPolicy.APPEND_OR_REPLACE else ExistingWorkPolicy.REPLACE,
            request
        )
    }
    fun cancelEvents(context: Context) = WorkManager.getInstance(context).cancelUniqueWork(EVENT_WORK)
    fun checkEventsNow(context: Context) {
        WorkManager.getInstance(context).enqueueUniqueWork(
            EVENT_NOW, ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<IslamicEventWorker>().setInputData(workDataOf("reschedule" to false)).build()
        )
    }
}

class FridayReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val p = AppPreferences(applicationContext)
        if (!p.fridayReminderEnabled) return Result.success()
        IslamicReminderScheduler.scheduleFriday(applicationContext, p.fridayReminderHour, p.fridayReminderMinute, append = true)
        NotificationCenter.notifyFriday(applicationContext)
        return Result.success()
    }
}

class ThursdayNightWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val p = AppPreferences(applicationContext)
        if (!p.thursdayNightReminder) return Result.success()
        IslamicReminderScheduler.scheduleThursdayNight(applicationContext, append = true)
        NotificationCenter.notifyIslamicEvent(applicationContext, "Malam Jumat", "Malam Jumat telah dimulai. Siapkan waktu untuk Al-Kahf, selawat, dan doa.", 5_001)
        return Result.success()
    }
}

class IslamicEventWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val p = AppPreferences(applicationContext)
        if (!p.islamicEventNotifications) return Result.success()
        if (inputData.getBoolean("reschedule", true)) {
            IslamicReminderScheduler.scheduleEvents(applicationContext, append = true)
        }
        val now = System.currentTimeMillis()
        val sent = applicationContext.getSharedPreferences("quriva_event_sent", Context.MODE_PRIVATE)
        IslamicCalendar.upcoming(p.hijriAdjustment, 50).forEach { upcoming ->
            val days = TimeUnit.MILLISECONDS.toDays((upcoming.targetMillis - now).coerceAtLeast(0))
            if (days in setOf(0L, 1L, 7L)) {
                val key = "${upcoming.event.id}-${upcoming.hijriYear}-$days"
                if (!sent.getBoolean(key, false)) {
                    val text = if (days == 0L) "Hari ini" else "$days hari lagi"
                    if (NotificationCenter.notifyIslamicEvent(applicationContext, upcoming.event.name, text, key.hashCode()))
                        sent.edit().putBoolean(key, true).apply()
                }
            }
        }
        return Result.success()
    }
}
