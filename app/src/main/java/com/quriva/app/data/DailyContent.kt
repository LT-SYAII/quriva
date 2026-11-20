package com.quriva.app.data

import android.content.Context
import com.quriva.app.model.SearchResult
import java.util.Calendar

object DailyContent {
    /** Deterministic "ayah of the day" based on the current date. */
    fun ayahOfTheDay(context: Context): SearchResult? {
        val total = try { QuranRepository.prepareSearch(context) } catch (_: Throwable) { 0 }
        if (total <= 0) return null
        val cal = Calendar.getInstance()
        val key = cal.get(Calendar.YEAR) * 1000 + cal.get(Calendar.DAY_OF_YEAR)
        val index = (key % total).let { if (it < 0) it + total else it }
        return try {
            // access via search index by searching broadly is inefficient; use internal approach:
            // search for empty not possible; instead load surahs and pick sequentially.
            pickByIndex(context, index)
        } catch (_: Throwable) { null }
    }

    private fun pickByIndex(context: Context, index: Int): SearchResult? {
        var remaining = index
        val surahs = QuranRepository.getSurahs(context)
        for (s in surahs) {
            if (remaining < s.ayahCount) {
                val detail = QuranRepository.getSurah(context, s.number)
                val ayah = detail.ayahs.getOrNull(remaining) ?: return null
                return SearchResult(
                    surahNumber = s.number,
                    surahName = s.latinName,
                    surahArabic = s.nameArabic,
                    ayahNumber = ayah.numberInSurah,
                    arabic = ayah.arabic,
                    translation = ayah.translation
                )
            }
            remaining -= s.ayahCount
        }
        return null
    }
}
