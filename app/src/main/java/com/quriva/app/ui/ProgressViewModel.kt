package com.quriva.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.quriva.app.data.AppPreferences
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToInt

enum class StreakRange(val label: String) { DAILY("Harian"), WEEKLY("Mingguan"), MONTHLY("Bulanan") }

data class DayStat(val date: String, val readCount: Int, val playCount: Int)
data class WeekStat(val label: String, val readCount: Int, val playCount: Int, val activeDays: Int)
data class MonthStat(val label: String, val readCount: Int, val playCount: Int, val activeDays: Int)

data class StreakStats(
    val streakDays: Int = 0,
    val todayRead: Int = 0,
    val todayPlay: Int = 0,
    val weekRead: Int = 0,
    val weekPlay: Int = 0,
    val monthRead: Int = 0,
    val monthPlay: Int = 0,
    val totalRead: Int = 0,
    val totalPlay: Int = 0,
    val activeDays30: Int = 0,
    val bestDayCount: Int = 0,
    val bestDayLabel: String = "",
    val avgDaily7: Int = 0,
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
            fun totalFor(key: String) = readFor(key) + playFor(key)

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

            // daily: last 35 days (5 weeks grid)
            val days = ArrayList<DayStat>(35)
            val dcal = Calendar.getInstance()
            dcal.add(Calendar.DAY_OF_YEAR, -34)
            repeat(35) {
                val k = fmt.format(dcal.time)
                days += DayStat(k, readFor(k), playFor(k))
                dcal.add(Calendar.DAY_OF_YEAR, 1)
            }

            // weekly: last 8 weeks (Mon-Sun buckets)
            val weeks = ArrayList<WeekStat>(8)
            val wcal = Calendar.getInstance()
            val dow = wcal.get(Calendar.DAY_OF_WEEK)
            val delta = if (dow == Calendar.SUNDAY) -6 else Calendar.MONDAY - dow
            wcal.add(Calendar.DAY_OF_YEAR, delta)
            wcal.add(Calendar.WEEK_OF_YEAR, -7)
            val weekLabelFmt = SimpleDateFormat("d MMM", Locale.forLanguageTag("id-ID"))
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
                weeks += WeekStat(weekLabelFmt.format(start), r, p, a)
            }

            // monthly: last 6 months
            val months = ArrayList<MonthStat>(6)
            val mcal = Calendar.getInstance()
            mcal.set(Calendar.DAY_OF_MONTH, 1)
            mcal.add(Calendar.MONTH, -5)
            val monthLabelFmt = SimpleDateFormat("MMM yy", Locale.forLanguageTag("id-ID"))
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

            var weekRead = 0; var weekPlay = 0
            val c7 = Calendar.getInstance()
            for (i in 0 until 7) { val k = fmt.format(c7.time); weekRead += readFor(k); weekPlay += playFor(k); c7.add(Calendar.DAY_OF_YEAR, -1) }
            var monthRead = 0; var monthPlay = 0
            var activeDays30 = 0
            val c30 = Calendar.getInstance()
            for (i in 0 until 30) {
                val k = fmt.format(c30.time)
                val rk = readFor(k)
                val pk = playFor(k)
                monthRead += rk
                monthPlay += pk
                if (rk > 0 || pk > 0) activeDays30++
                c30.add(Calendar.DAY_OF_YEAR, -1)
            }
            val best = days.maxByOrNull { it.readCount + it.playCount }
            val bestCount = (best?.readCount ?: 0) + (best?.playCount ?: 0)
            val bestLabel = if (best != null && bestCount > 0) {
                try { SimpleDateFormat("d MMM", Locale.forLanguageTag("id-ID")).format(fmt.parse(best.date)!!) } catch (_: Throwable) { "" }
            } else ""
            val avg7 = ((weekRead + weekPlay) / 7f).roundToInt()

            return StreakStats(
                streakDays = streak,
                todayRead = readFor(today),
                todayPlay = playFor(today),
                weekRead = weekRead,
                weekPlay = weekPlay,
                monthRead = monthRead,
                monthPlay = monthPlay,
                totalRead = totalRead,
                totalPlay = totalPlay,
                activeDays30 = activeDays30,
                bestDayCount = bestCount,
                bestDayLabel = bestLabel,
                avgDaily7 = avg7,
                days = days,
                weeks = weeks,
                months = months
            )
        }
    }
}
