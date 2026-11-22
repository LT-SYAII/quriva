package com.quriva.app.data

import com.quriva.app.model.IslamicEvent
import com.quriva.app.model.UpcomingIslamicEvent
import java.util.Calendar
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.min

/** Offline civil-Hijri estimate. Official moon-sighting dates may differ by a day. */
object IslamicCalendar {
    private const val ISLAMIC_EPOCH = 1_948_439.5

    val monthNames = listOf(
        "Muharam", "Safar", "Rabiulawal", "Rabiulakhir", "Jumadilawal", "Jumadilakhir",
        "Rajab", "Syakban", "Ramadan", "Syawal", "Zulkaidah", "Zulhijah"
    )

    val events = listOf(
        IslamicEvent("new-year", "Tahun Baru Islam", 1, 1, "Awal tahun Hijriah."),
        IslamicEvent("ashura", "Hari Asyura", 1, 10, "Tanggal 10 Muharam."),
        IslamicEvent("mawlid", "Maulid Nabi", 3, 12, "Peringatan yang umum dipakai di Indonesia."),
        IslamicEvent("isra-miraj", "Isra Mikraj", 7, 27, "Peringatan Isra dan Mikraj."),
        IslamicEvent("nisfu-shaban", "Nisfu Syakban", 8, 15, "Pertengahan bulan Syakban."),
        IslamicEvent("ramadan", "Awal Ramadan", 9, 1, "Awal bulan puasa; menunggu penetapan resmi."),
        IslamicEvent("nuzulul-quran", "Nuzulul Quran", 9, 17, "Peringatan yang umum dipakai di Indonesia."),
        IslamicEvent("last-ten", "Sepuluh Malam Terakhir", 9, 21, "Awal rentang sepuluh malam terakhir Ramadan.", durationDays = 10),
        IslamicEvent("eid-fitr", "Idulfitri", 10, 1, "1 Syawal; menunggu penetapan resmi."),
        IslamicEvent("shawwal-fast", "Puasa Syawal", 10, 2, "Awal waktu pelaksanaan enam hari puasa Syawal.", durationDays = 29),
        IslamicEvent("dhulhijjah", "Awal Zulhijah", 12, 1, "Awal sepuluh hari pertama Zulhijah."),
        IslamicEvent("tarwiyah", "Hari Tarwiyah", 12, 8, "Tanggal 8 Zulhijah."),
        IslamicEvent("arafah", "Hari Arafah", 12, 9, "Tanggal 9 Zulhijah."),
        IslamicEvent("eid-adha", "Iduladha", 12, 10, "10 Zulhijah; menunggu penetapan resmi."),
        IslamicEvent("tashriq", "Hari Tasyrik", 12, 11, "Tanggal 11–13 Zulhijah.", durationDays = 3)
    )

    data class HijriDate(val year: Int, val month: Int, val day: Int)

    fun today(adjustment: Int = 0): HijriDate {
        val cal = Calendar.getInstance()
        return fromGregorian(cal, adjustment)
    }

    fun fromGregorian(calendar: Calendar, adjustment: Int = 0): HijriDate {
        val jd = gregorianToJd(
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH) + 1,
            calendar.get(Calendar.DAY_OF_MONTH)
        ) + adjustment
        val year = floor((30.0 * (jd - ISLAMIC_EPOCH) + 10_646.0) / 10_631.0).toInt()
        val month = min(
            12,
            ceil((jd - 29 - islamicToJd(year, 1, 1)) / 29.5).toInt() + 1
        ).coerceAtLeast(1)
        val day = (jd - islamicToJd(year, month, 1) + 1).toInt().coerceAtLeast(1)
        return HijriDate(year, month, day)
    }

    fun upcoming(adjustment: Int = 0, count: Int = 30): List<UpcomingIslamicEvent> {
        val now = Calendar.getInstance()
        val currentJd = gregorianToJd(
            now.get(Calendar.YEAR), now.get(Calendar.MONTH) + 1, now.get(Calendar.DAY_OF_MONTH)
        ) + adjustment
        val currentHijri = fromGregorian(now, adjustment)
        val recurring = (1..12).flatMap { month ->
            (13..15).map { day ->
                IslamicEvent("ayyam-$month-$day", "Ayyamul Bidh", month, day, "Puasa sunah tanggal 13, 14, dan 15 bulan Hijriah.")
            }
        }
        return (events + recurring).map { event ->
            var year = currentHijri.year
            var targetJd = islamicToJd(year, event.hijriMonth, event.hijriDay)
            if (targetJd < currentJd) {
                year++
                targetJd = islamicToJd(year, event.hijriMonth, event.hijriDay)
            }
            val days = (targetJd - currentJd).toInt()
            val target = (now.clone() as Calendar).apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                add(Calendar.DAY_OF_YEAR, days)
            }.timeInMillis
            UpcomingIslamicEvent(event, target, year)
        }.sortedBy { it.targetMillis }.take(count)
    }

    fun hijriLabel(date: HijriDate): String =
        "${date.day} ${monthNames.getOrElse(date.month - 1) { "" }} ${date.year} H"

    fun nextFriday(hour: Int = 6, minute: Int = 0): Long {
        val now = Calendar.getInstance()
        return (now.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, hour.coerceIn(0, 23))
            set(Calendar.MINUTE, minute.coerceIn(0, 59))
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            val delta = (Calendar.FRIDAY - get(Calendar.DAY_OF_WEEK) + 7) % 7
            add(Calendar.DAY_OF_YEAR, if (delta == 0 && timeInMillis <= now.timeInMillis) 7 else delta)
        }.timeInMillis
    }

    fun nextWeekday(dayOfWeek: Int): Long {
        val now = Calendar.getInstance()
        return (now.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            var delta = (dayOfWeek - get(Calendar.DAY_OF_WEEK) + 7) % 7
            if (delta == 0 && timeInMillis <= now.timeInMillis) delta = 7
            add(Calendar.DAY_OF_YEAR, delta)
        }.timeInMillis
    }

    private fun islamicToJd(year: Int, month: Int, day: Int): Double =
        day + ceil(29.5 * (month - 1)) + (year - 1) * 354 +
            floor((3 + 11 * year) / 30.0) + ISLAMIC_EPOCH - 1

    private fun gregorianToJd(year: Int, month: Int, day: Int): Double {
        val a = (14 - month) / 12
        val y = year + 4800 - a
        val m = month + 12 * a - 3
        val jdn = day + (153 * m + 2) / 5 + 365 * y + y / 4 - y / 100 + y / 400 - 32045
        return jdn - 0.5
    }
}
