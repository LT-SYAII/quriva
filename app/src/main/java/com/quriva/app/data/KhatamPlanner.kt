package com.quriva.app.data

import android.content.Context
import androidx.compose.runtime.Immutable
import com.quriva.app.model.LastRead
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Immutable
data class KhatamStats(
    val totalVerses: Int,
    val startVerse: Int,
    val currentVerse: Int,
    val readVerses: Int,
    val remainingVerses: Int,
    val percent: Float,
    val targetDays: Int,
    val elapsedDays: Int,
    val remainingDays: Int,
    val dailyVerses: Int,
    val dailyPages: Float,
    val currentPage: Int,
    val remainingPages: Int,
    val expectedVerse: Int,
    val onTrack: Boolean,
    val started: Boolean,
    val startDate: String,
    val estimatedFinishDays: Int?
)

/** Offline khatam projection based on the last reading position. */
object KhatamPlanner {
    private const val TOTAL_VERSES = 6236
    private const val TOTAL_PAGES = 604

    fun currentVerse(context: Context, lastRead: LastRead?): Int {
        if (lastRead == null) return 0
        val surahs = runCatching { QuranRepository.getSurahs(context) }.getOrElse { return 0 }
        var offset = 0
        for (surah in surahs) {
            if (surah.number == lastRead.surahNumber) {
                offset += (lastRead.ayah - 1).coerceIn(0, surah.ayahCount - 1)
                return offset.coerceIn(0, TOTAL_VERSES - 1)
            }
            offset += surah.ayahCount
        }
        return 0
    }

    fun currentPage(context: Context, lastRead: LastRead?): Int {
        if (lastRead == null) return 1
        val actual = runCatching {
            QuranRepository.getSurah(context, lastRead.surahNumber)
                .ayahs.firstOrNull { it.numberInSurah == lastRead.ayah }?.page ?: 0
        }.getOrDefault(0)
        if (actual in 1..TOTAL_PAGES) return actual
        val verse = currentVerse(context, lastRead)
        return (1 + verse.toLong() * TOTAL_PAGES / TOTAL_VERSES).toInt().coerceIn(1, TOTAL_PAGES)
    }

    fun compute(
        context: Context,
        prefs: AppPreferences,
        now: Long = System.currentTimeMillis()
    ): KhatamStats {
        val current = currentVerse(context, prefs.lastRead)
        val started = prefs.khatamStartDate.isNotBlank()
        val start = if (started) prefs.khatamStartVerse.coerceIn(0, TOTAL_VERSES - 1) else current
        val span = (TOTAL_VERSES - start).coerceAtLeast(1)
        val read = (current - start).coerceIn(0, span)
        val remaining = (span - read).coerceAtLeast(0)
        val percent = (read.toFloat() / span).coerceIn(0f, 1f)
        val target = prefs.khatamTargetDays.coerceIn(1, 365)
        val elapsed = if (started) daysSince(prefs.khatamStartDate, now) else 0
        val remainingDays = (target - elapsed).coerceAtLeast(0)
        val workDays = remainingDays.coerceAtLeast(1)
        val dailyVerses = if (remaining == 0) 0 else ((remaining + workDays - 1) / workDays)
        val expected = start + (span.toLong() * elapsed / target).toInt()
        val estimatedFinish = if (started && elapsed > 0 && read > 0 && remaining > 0) {
            val rate = read.toDouble() / elapsed
            (remaining / rate).toInt().coerceAtLeast(1)
        } else null
        val currentPage = currentPage(context, prefs.lastRead)
        val remainingPageCount = (TOTAL_PAGES - currentPage).coerceAtLeast(0)
        return KhatamStats(
            totalVerses = TOTAL_VERSES,
            startVerse = start,
            currentVerse = current,
            readVerses = read,
            remainingVerses = remaining,
            percent = percent,
            targetDays = target,
            elapsedDays = elapsed,
            remainingDays = remainingDays,
            dailyVerses = dailyVerses,
            dailyPages = dailyVerses.toFloat() * TOTAL_PAGES / TOTAL_VERSES,
            currentPage = currentPage,
            remainingPages = remainingPageCount,
            expectedVerse = expected,
            onTrack = current >= expected - (span / 50).coerceAtLeast(1),
            started = started,
            startDate = prefs.khatamStartDate,
            estimatedFinishDays = estimatedFinish
        )
    }

    fun todayDailyTarget(stats: KhatamStats): String = when {
        stats.remainingVerses == 0 -> "Khatam tercapai"
        stats.dailyVerses <= 1 -> {
            val pages = "%.1f".format(Locale.US, stats.dailyPages.coerceAtLeast(0.1f))
            "±$pages halaman"
        }
        else -> {
            val pages = "%.1f".format(Locale.US, stats.dailyPages)
            "${stats.dailyVerses} ayat · ±$pages halaman"
        }
    }

    private fun daysSince(dateKey: String, now: Long): Int {
        return try {
            val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val start = format.parse(dateKey) ?: return 0
            val startCal = Calendar.getInstance().apply {
                time = start
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val nowCal = Calendar.getInstance().apply {
                timeInMillis = now
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            ((nowCal.timeInMillis - startCal.timeInMillis) / 86_400_000L).toInt().coerceAtLeast(0)
        } catch (_: Exception) {
            0
        }
    }
}
