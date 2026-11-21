package com.quriva.app.data

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class DailyReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val prefs = AppPreferences(applicationContext)
        if (!prefs.reminderEnabled) return Result.success()
        ReminderScheduler.scheduleNext(
            applicationContext,
            prefs.reminderHour,
            prefs.reminderMinute
        )
        runCatching { NotificationCenter.notifyDaily(applicationContext) }
        return Result.success()
    }
}
