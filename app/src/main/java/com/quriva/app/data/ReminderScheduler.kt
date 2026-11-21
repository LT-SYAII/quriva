package com.quriva.app.data

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Persists the next wall-clock reminder through WorkManager. Unlike an app
 * alarm, this survives reboot and OEM process cleanup without exact-alarm
 * permission. The worker appends tomorrow's occurrence after it runs.
 */
object ReminderScheduler {

    fun schedule(context: Context, hour: Int, minute: Int) {
        enqueue(context, hour, minute, ExistingWorkPolicy.REPLACE)
    }

    fun ensureScheduled(context: Context, hour: Int, minute: Int) {
        enqueue(context, hour, minute, ExistingWorkPolicy.KEEP)
    }

    internal fun scheduleNext(context: Context, hour: Int, minute: Int) {
        enqueue(context, hour, minute, ExistingWorkPolicy.APPEND_OR_REPLACE)
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }

    private fun enqueue(
        context: Context,
        hour: Int,
        minute: Int,
        policy: ExistingWorkPolicy
    ) {
        val now = System.currentTimeMillis()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour.coerceIn(0, 23))
            set(Calendar.MINUTE, minute.coerceIn(0, 59))
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= now) add(Calendar.DAY_OF_YEAR, 1)
        }.timeInMillis
        val request = OneTimeWorkRequestBuilder<DailyReminderWorker>()
            .setInitialDelay((target - now).coerceAtLeast(1_000L), TimeUnit.MILLISECONDS)
            .addTag(WORK_NAME)
            .build()
        WorkManager.getInstance(context.applicationContext)
            .enqueueUniqueWork(WORK_NAME, policy, request)
    }

    private const val WORK_NAME = "quriva_daily_reminder"
}
