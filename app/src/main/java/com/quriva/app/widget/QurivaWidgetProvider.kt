package com.quriva.app.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import android.text.format.DateFormat
import android.view.View
import android.widget.RemoteViews
import com.quriva.app.MainActivity
import com.quriva.app.R
import com.quriva.app.data.AppPreferences
import com.quriva.app.data.IslamicCalendar
import com.quriva.app.data.PrayerCities
import com.quriva.app.data.PrayerMethod
import com.quriva.app.data.PrayerTimes
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Monochrome home-screen widget with a live clock, a real-time countdown to the
 * next prayer, today's date, reading progress, and worship progress.
 *
 * The clock and countdown keep ticking on their own via [android.widget.TextClock]
 * and [android.widget.Chronometer]. A one-shot alarm refreshes the next-prayer
 * data exactly when the current prayer starts, and again at midnight.
 */
class QurivaWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetManager.updateAppWidget(appWidgetIds, buildViews(context))
        scheduleNextRefresh(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_REFRESH,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_DATE_CHANGED -> updateAll(context)
        }
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        cancelNextRefresh(context)
    }

    companion object {
        private const val ACTION_REFRESH = "com.quriva.app.action.WIDGET_REFRESH"
        private const val WIDGET_REQUEST_CODE = 46_100
        private const val REFRESH_REQUEST_CODE = 46_101

        fun updateAll(context: Context) {
            val appContext = context.applicationContext
            val manager = AppWidgetManager.getInstance(appContext)
            val component = ComponentName(appContext, QurivaWidgetProvider::class.java)
            val ids = manager.getAppWidgetIds(component)
            if (ids.isEmpty()) return
            manager.updateAppWidget(ids, buildViews(appContext))
            scheduleNextRefresh(appContext)
        }

        private fun buildViews(context: Context): RemoteViews {
            val prefs = AppPreferences(context)
            val views = RemoteViews(context.packageName, R.layout.quriva_widget)
            val now = System.currentTimeMillis()
            val locale = Locale.forLanguageTag("id-ID")

            val hijri = IslamicCalendar.hijriLabel(
                IslamicCalendar.today(prefs.hijriAdjustment)
            )
            val gregorian = SimpleDateFormat("d MMM", locale).format(Date(now))
            views.setTextViewText(R.id.widget_date, "$gregorian · $hijri")
            views.setViewVisibility(
                R.id.widget_clock_ampm,
                if (DateFormat.is24HourFormat(context)) View.GONE else View.VISIBLE
            )

            val city = PrayerCities.byId(prefs.prayerCityId)
            val method = runCatching { PrayerMethod.valueOf(prefs.prayerMethod) }
                .getOrDefault(PrayerMethod.KEMENAG)
            val schedule = PrayerTimes.schedule(
                city = city,
                method = method,
                asrHanafi = prefs.prayerAsrHanafi,
                now = now
            )
            val next = schedule.next(now)
            if (next != null) {
                val remaining = (next.millis - now).coerceAtLeast(0L)
                views.setTextViewText(
                    R.id.widget_next_time,
                    PrayerTimes.formatClock(next.millis, city.timeZoneId)
                )
                views.setTextViewText(R.id.widget_next_name, next.prayer.label)
                if (remaining > 0L) {
                    views.setViewVisibility(R.id.widget_countdown_row, View.VISIBLE)
                    views.setViewVisibility(R.id.widget_countdown_done, View.GONE)
                    views.setTextViewText(
                        R.id.widget_countdown_prefix,
                        context.getString(R.string.widget_menuju, next.prayer.label)
                    )
                    views.setChronometer(
                        R.id.widget_countdown,
                        SystemClock.elapsedRealtime() + remaining,
                        null,
                        true
                    )
                    views.setChronometerCountDown(R.id.widget_countdown, true)
                } else {
                    views.setViewVisibility(R.id.widget_countdown_row, View.GONE)
                    views.setViewVisibility(R.id.widget_countdown_done, View.VISIBLE)
                    views.setTextViewText(
                        R.id.widget_countdown_done,
                        context.getString(R.string.widget_arrived)
                    )
                }
            } else {
                views.setTextViewText(R.id.widget_next_time, "--:--")
                views.setTextViewText(
                    R.id.widget_next_name,
                    context.getString(R.string.widget_unavailable)
                )
                views.setViewVisibility(R.id.widget_countdown_row, View.GONE)
                views.setViewVisibility(R.id.widget_countdown_done, View.VISIBLE)
                views.setTextViewText(
                    R.id.widget_countdown_done,
                    context.getString(R.string.widget_unavailable)
                )
            }

            val lastRead = prefs.lastRead
            if (lastRead != null) {
                views.setTextViewText(
                    R.id.widget_continue_title,
                    "${lastRead.surahName} · ${lastRead.ayah}/${lastRead.totalAyah}"
                )
                views.setTextViewText(
                    R.id.widget_continue_sub,
                    context.getString(R.string.widget_continue_hint)
                )
            } else {
                views.setTextViewText(
                    R.id.widget_continue_title,
                    context.getString(R.string.widget_start_reading)
                )
                views.setTextViewText(
                    R.id.widget_continue_sub,
                    context.getString(R.string.widget_start_reading_sub)
                )
            }

            val worship = prefs.worshipCount().coerceIn(0, 9)
            views.setTextViewText(
                R.id.widget_footer,
                context.getString(R.string.widget_worship, worship, 9)
            )
            views.setProgressBar(R.id.widget_worship_progress, 9, worship, false)
            views.setContentDescription(
                R.id.widget_worship_progress,
                context.getString(R.string.widget_worship_description, worship, 9)
            )

            val openApp = openAppPendingIntent(context)
            views.setOnClickPendingIntent(R.id.widget_root, openApp)
            views.setOnClickPendingIntent(R.id.widget_next_group, openApp)
            views.setOnClickPendingIntent(R.id.widget_continue_group, openApp)
            return views
        }

        /**
         * Wakes the widget once when the current prayer starts (so the next
         * prayer card advances) and once after midnight (so the date rolls
         * over). Both the clock and countdown keep running without this alarm.
         */
        private fun scheduleNextRefresh(context: Context) {
            val appContext = context.applicationContext
            val prefs = AppPreferences(appContext)
            val city = PrayerCities.byId(prefs.prayerCityId)
            val method = runCatching { PrayerMethod.valueOf(prefs.prayerMethod) }
                .getOrDefault(PrayerMethod.KEMENAG)
            val now = System.currentTimeMillis()
            val schedule = PrayerTimes.schedule(
                city = city,
                method = method,
                asrHanafi = prefs.prayerAsrHanafi,
                now = now
            )
            val nextPrayer = schedule.next(now)?.millis ?: Long.MAX_VALUE
            val triggerAt = minOf(nextPrayer, nextLocalMidnight(now)) + 2_000L
            val alarmManager = appContext.getSystemService(AlarmManager::class.java) ?: return
            val pending = refreshPendingIntent(appContext)
            val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                alarmManager.canScheduleExactAlarms()
            if (canExact) {
                runCatching {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAt,
                        pending
                    )
                }.onFailure {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAt,
                        pending
                    )
                }
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAt,
                    pending
                )
            }
        }

        private fun nextLocalMidnight(now: Long): Long =
            Calendar.getInstance().apply {
                timeInMillis = now
                add(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 2)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

        private fun refreshPendingIntent(context: Context): PendingIntent {
            val intent = Intent(context, QurivaWidgetProvider::class.java).apply {
                action = ACTION_REFRESH
            }
            return PendingIntent.getBroadcast(
                context,
                REFRESH_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        private fun cancelNextRefresh(context: Context) {
            val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
            alarmManager.cancel(refreshPendingIntent(context))
        }

        private fun openAppPendingIntent(context: Context): PendingIntent {
            val openIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            return PendingIntent.getActivity(
                context,
                WIDGET_REQUEST_CODE,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }
}
