package com.quriva.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.quriva.app.data.AppPreferences
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

enum class StreakRange(val label: String) { DAILY("Harian"), WEEKLY("Mingguan"), MONTHLY("Bulanan") }

data class DayStat(val date: String, val readCount: Int, val playCount: Int)

data class WeekStat(val label: String, val readCount: Int, val playCount: Int, val activeDays: Int)

data class MonthStat(val label: String, val readCount: Int, val playCount: Int, val activeDays: Int)

data class StreakStats(
    val streakDays: Int = 0,
    val todayRead: Int = 0,
    val todayPlay: Int = 0,
    val totalRead: Int = 0,
    val totalPlay: Int = 0,
    val days: List<DayStat> = emptyList(),
    val weeks: List<WeekStat> = emptyList(),
    val months: List<MonthStat> = emptyList()
)

class ProgressViewModel(application: Application) : AndroidViewModel(application) {
    companion object {
        fun compute(prefs: AppPreferences): StreakStats {
            val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val cal = Calendar.getInstance()
            val today = fmt.format(cal.time)
            val daily = prefs.readingProgress.daily
            val playDaily = prefs.readingProgress.playDaily

            fun readFor(key: String) = daily[key]?.size ?: 0
            fun playFor(key: String) = playDaily[key] ?: 0
            fun active(key: String) = readFor(key) > 0 || playFor(key) > 0

            // streak ending today or yesterday
            var streak = 0
            val c = Calendar.getInstance()
            var key = fmt.format(c.time)
            if (active(key)) {
                streak = 1
                c.add(Calendar.DAY_OF_YEAR, -1)
                while (true) {
                    key = fmt.format(c.time)
                    if (active(key)) { streak++; c.add(Calendar.DAY_OF_YEAR, -1) } else break
                }
            } else {
                c.add(Calendar.DAY_OF_YEAR, -1)
                key = fmt.format(c.time)
                if (active(key)) {
                    streak = 1
                    c.add(Calendar.DAY_OF_YEAR, -1)
                    while (true) {
                        key = fmt.format(c.time)
                        if (active(key)) { streak++; c.add(Calendar.DAY_OF_YEAR, -1) } else break
                    }
                }
            }

            // daily: last 30 days
            val days = ArrayList<DayStat>(30)
            val dcal = Calendar.getInstance()
            dcal.add(Calendar.DAY_OF_YEAR, -29)
            repeat(30) {
                val k = fmt.format(dcal.time)
                days += DayStat(k, readFor(k), playFor(k))
                dcal.add(Calendar.DAY_OF_YEAR, 1)
            }

            // weekly: last 8 weeks (Mon-Sun buckets)
            val weeks = ArrayList<WeekStat>(8)
            val wcal = Calendar.getInstance()
            // move to start of this week (Monday)
            val dow = wcal.get(Calendar.DAY_OF_WEEK)
            val delta = if (dow == Calendar.SUNDAY) -6 else Calendar.MONDAY - dow
            wcal.add(Calendar.DAY_OF_YEAR, delta)
            wcal.add(Calendar.WEEK_OF_YEAR, -7)
            val weekLabelFmt = SimpleDateFormat("d MMM", Locale("id", "ID"))
            repeat(8) {
                var r = 0; var p = 0; var a = 0
                val start = wcal.time
                for (i in 0 until 7) {
                    val k = fmt.format(wcal.time)
                    val rk = readFor(k); val pk = playFor(k)
                    r += rk; p += pk
                    if (rk > 0 || pk > 0) a++
                    wcal.add(Calendar.DAY_OF_YEAR, 1)
                }
                val label = weekLabelFmt.format(start)
                weeks += WeekStat(label, r, p, a)
            }

            // monthly: last 6 months
            val months = ArrayList<MonthStat>(6)
            val mcal = Calendar.getInstance()
            mcal.set(Calendar.DAY_OF_MONTH, 1)
            mcal.add(Calendar.MONTH, -5)
            val monthLabelFmt = SimpleDateFormat("MMM yy", Locale("id", "ID"))
            repeat(6) {
                val label = monthLabelFmt.format(mcal.time)
                val maxDay = mcal.getActualMaximum(Calendar.DAY_OF_MONTH)
                var r = 0; var p = 0; var a = 0
                for (d in 1..maxDay) {
                    val c2 = (mcal.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, d) }
                    val k = fmt.format(c2.time)
                    val rk = readFor(k); val pk = playFor(k)
                    r += rk; p += pk
                    if (rk > 0 || pk > 0) a++
                }
                months += MonthStat(label, r, p, a)
                mcal.add(Calendar.MONTH, 1)
            }

            val totalRead = daily.values.sumOf { it.size }
            val totalPlay = playDaily.values.sumOf { it }

            return StreakStats(
                streakDays = streak,
                todayRead = readFor(today),
                todayPlay = playFor(today),
                totalRead = totalRead,
                totalPlay = totalPlay,
                days = days,
                weeks = weeks,
                months = months
            )
        }
    }
}
