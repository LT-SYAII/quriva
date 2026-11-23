package com.quriva.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.quriva.app.MainActivity
import com.quriva.app.R
import com.quriva.app.data.AppPreferences
import com.quriva.app.data.IslamicCalendar
import com.quriva.app.data.PrayerCities
import com.quriva.app.data.PrayerMethod
import com.quriva.app.data.PrayerTimes
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Classic RemoteViews home-screen widget: next prayer, Hijri date, and the
 * latest reading position. Refreshed by the system every 30 minutes and by
 * [updateAll] whenever the app is opened or closed.
 */
class QurivaWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val views = buildViews(context)
        appWidgetManager.updateAppWidget(appWidgetIds, views)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_DATE_CHANGED -> updateAll(context)
        }
    }

    companion object {
        private const val WIDGET_REQUEST_CODE = 46_100

        fun updateAll(context: Context) {
            val appContext = context.applicationContext
            val manager = AppWidgetManager.getInstance(appContext)
            val component = ComponentName(appContext, QurivaWidgetProvider::class.java)
            val ids = manager.getAppWidgetIds(component)
            if (ids.isEmpty()) return
            manager.updateAppWidget(ids, buildViews(appContext))
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
                val remaining = (next.millis - now).coerceAtLeast(0)
                val timeLabel = PrayerTimes.formatClock(next.millis, city.timeZoneId)
                views.setTextViewText(
                    R.id.widget_next_label,
                    "SALAT BERIKUTNYA · ${city.name.uppercase(locale)}"
                )
                views.setTextViewText(R.id.widget_next_name, next.prayer.label)
                views.setTextViewText(R.id.widget_next_time, timeLabel)
                views.setTextViewText(
                    R.id.widget_next_countdown,
                    if (remaining < 60_000L) "waktu salat telah masuk"
                    else "${PrayerTimes.countdownLabel(remaining)} lagi"
                )
            } else {
                views.setTextViewText(R.id.widget_next_label, "SALAT BERIKUTNYA")
                views.setTextViewText(R.id.widget_next_name, "Jadwal tidak tersedia")
                views.setTextViewText(R.id.widget_next_time, "--:--")
                views.setTextViewText(R.id.widget_next_countdown, "")
            }

            val lastRead = prefs.lastRead
            if (lastRead != null) {
                views.setTextViewText(
                    R.id.widget_continue_title,
                    "${lastRead.surahName} · ${lastRead.ayah}/${lastRead.totalAyah}"
                )
                views.setTextViewText(R.id.widget_continue_sub, "Lanjutkan tilawah")
            } else {
                views.setTextViewText(R.id.widget_continue_title, "Mulai membaca Al-Qur'an")
                views.setTextViewText(R.id.widget_continue_sub, "Al-Fatihah menanti")
            }

            val worship = prefs.worshipCount().coerceIn(0, 9)
            views.setTextViewText(R.id.widget_footer, "Ibadah hari ini $worship/9")

            val openIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pending = PendingIntent.getActivity(
                context,
                WIDGET_REQUEST_CODE,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, pending)
            views.setOnClickPendingIntent(R.id.widget_next_group, pending)
            views.setOnClickPendingIntent(R.id.widget_continue_group, pending)
            return views
        }
    }
}
