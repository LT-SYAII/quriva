package com.quriva.app.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Re-schedules the daily reminder after a reboot or an app update. One-shot
 * alarms are intentionally used by [ReminderScheduler], so the schedule must
 * be restored whenever the system forgets pending alarms.
 */
class ReminderBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED -> {
                val prefs = AppPreferences(context)
                if (prefs.reminderEnabled) {
                    ReminderScheduler.schedule(
                        context,
                        prefs.reminderHour,
                        prefs.reminderMinute
                    )
                }
            }
        }
    }
}
