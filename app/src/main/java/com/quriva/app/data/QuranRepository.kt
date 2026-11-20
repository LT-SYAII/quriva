package com.quriva.app.data

import android.content.Context
import android.util.LruCache
import com.quriva.app.model.Ayah
import com.quriva.app.model.JuzMeta
import com.quriva.app.model.SearchResponse
import com.quriva.app.model.SearchResult
import com.quriva.app.model.SurahDetail
import com.quriva.app.model.SurahMeta
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

/**
 * Reads the Quran data bundled from quran.com.
 *
 * The data is split into a tiny chapter index (`surahs.json`) and one file per
 * surah (`quran/001.json` ... `quran/114.json`). This keeps startup fast and
 * avoids parsing several megabytes of JSON on a single load.
 */
object QuranRepository {

    private val verseReferenceRegex = Regex("^(\\d{1,3}):(\\d{1,3})$")

    @Volatile
    private var surahIndex: List<SurahMeta>? = null

    @Volatile
    private var searchIndex: List<SearchEntry>? = null

    @Volatile
    private var juzIndex: List<JuzMeta>? = null

    private val ayahCache = LruCache<Int, List<Ayah>>(4)

    /**
     * Pre-normalized index entry. Normalizing 6.236 ayat on every keystroke was
     * the main search cost, so the work is done once when the index is built.
     */
    private class SearchEntry(
        val result: SearchResult,
        val normalizedArabic: String,
        val lowerTranslation: String,
        val lowerName: String
    )

    fun getSurahs(context: Context): List<SurahMeta> {
        surahIndex?.let { return it }
        synchronized(this) {
            surahIndex?.let { return it }
            val json = context.assets.open("surahs.json").bufferedReader().use { it.readText() }
            val arr = JSONArray(json)
            if (arr.length() == 0) error("Indeks surah kosong")
            val result = ArrayList<SurahMeta>(arr.length())
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                result += SurahMeta(
                    number = o.getInt("number"),
                    nameArabic = o.getString("name"),
                    latinName = o.getString("latin"),
                    translation = o.optString("translation"),
                    place = o.optString("place"),
                    ayahCount = o.getInt("ayahCount"),
                    bismillah = o.optBoolean("bismillah", false),
                    pageStart = o.optInt("pageStart", 1),
                    pageEnd = o.optInt("pageEnd", 1)
                )
            }
            surahIndex = result
            return result
        }
    }

    fun getSurah(context: Context, number: Int): SurahDetail {
        val meta = getSurahs(context).firstOrNull { it.number == number }
            ?: error("Surah $number tidak ditemukan")
        val ayahs = ayahCache.get(number) ?: loadAyahs(context, number).also {
            ayahCache.put(number, it)
        }
        return SurahDetail(meta = meta, ayahs = ayahs)
    }

    fun getJuzs(context: Context): List<JuzMeta> {
        juzIndex?.let { return it }
        synchronized(this) {
            juzIndex?.let { return it }
            val json = context.assets.open("juzs.json").bufferedReader().use { it.readText() }
            val arr = JSONArray(json)
            val result = ArrayList<JuzMeta>(arr.length())
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                result += JuzMeta(
                    number = o.getInt("number"),
                    startSurah = o.getInt("startSurah"),
                    startAyah = o.getInt("startAyah"),
                    endSurah = o.getInt("endSurah"),
                    endAyah = o.getInt("endAyah"),
                    ayahCount = o.getInt("ayahCount")
                )
            }
            juzIndex = result
            return result
        }
    }

    /** Loads and caches the full text index used by the global ayah search. */
    fun prepareSearch(context: Context): Int {
        return getSearchIndex(context).size
    }

    /**
     * Searches the whole Quran offline. Arabic matching ignores harakat and
     * common letter variants; Indonesian matching is case-insensitive.
     */
    fun search(context: Context, rawQuery: String, limit: Int = 120): SearchResponse {
        val query = rawQuery.trim()
        if (query.length < 2) return SearchResponse(emptyList(), 0)

        val index = getSearchIndex(context)
        val reference = verseReferenceRegex.matchEntire(query)
        if (reference != null) {
            val surah = reference.groupValues[1].toIntOrNull() ?: 0
            val ayah = reference.groupValues[2].toIntOrNull() ?: 0
            val match = index.firstOrNull {
                it.result.surahNumber == surah && it.result.ayahNumber == ayah
            }
            return if (match == null) {
                SearchResponse(emptyList(), 0)
            } else {
                SearchResponse(listOf(match.result), 1)
            }
        }

        val normalizedQuery = normalizeArabic(query)
        val lowerQuery = query.lowercase(Locale.ROOT)
        val scored = ArrayList<Pair<SearchEntry, Int>>()

        index.forEach { item ->
            var score = 0

            if (normalizedQuery.isNotEmpty()) {
                when {
                    item.normalizedArabic == normalizedQuery -> score = 100
                    item.normalizedArabic.startsWith(normalizedQuery) -> score = 82
                    item.normalizedArabic.contains(normalizedQuery) -> score = 66
                }
            }
            if (score < 95) {
                when {
                    item.lowerTranslation == lowerQuery -> score = 95
                    item.lowerTranslation.startsWith(lowerQuery) -> {
                        if (score < 76) score = 76
                    }
                    item.lowerTranslation.contains(lowerQuery) -> {
                        if (score < 58) score = 58
                    }
                }
            }
            if (score < 44) {
                when {
                    item.lowerName == lowerQuery -> score = 44
                    item.lowerName.contains(lowerQuery) -> {
                        if (score < 30) score = 30
                    }
                }
            }

            if (score > 0) scored += item to score
        }

        val ordered = scored
            .sortedWith(
                compareByDescending<Pair<SearchEntry, Int>> { it.second }
                    .thenBy { it.first.result.surahNumber }
                    .thenBy { it.first.result.ayahNumber }
            )
            .map { it.first.result }

        return SearchResponse(ordered.take(limit), ordered.size)
    }

    private fun getSearchIndex(context: Context): List<SearchEntry> {
        searchIndex?.let { return it }
        synchronized(this) {
            searchIndex?.let { return it }
            val surahs = getSurahs(context)
            val result = ArrayList<SearchEntry>(6236)
            surahs.forEach { surah ->
                val lowerName = surah.latinName.lowercase(Locale.ROOT)
                loadAyahs(context, surah.number).forEach { ayah ->
                    result += SearchEntry(
                        result = SearchResult(
                            surahNumber = surah.number,
                            surahName = surah.latinName,
                            surahArabic = surah.nameArabic,
                            ayahNumber = ayah.numberInSurah,
                            arabic = ayah.arabic,
                            translation = ayah.translation
                        ),
                        normalizedArabic = normalizeArabic(ayah.arabic),
                        lowerTranslation = ayah.translation.lowercase(Locale.ROOT),
                        lowerName = lowerName
                    )
                }
            }
            if (result.isEmpty()) error("Indeks ayat kosong")
            searchIndex = result
            return result
        }
    }

    private fun loadAyahs(context: Context, number: Int): List<Ayah> {
        val path = "quran/${number.toString().padStart(3, '0')}.json"
        val json = context.assets.open(path).bufferedReader().use { it.readText() }
        val root = JSONObject(json)
        val arr = root.getJSONArray("ayahs")
        val result = ArrayList<Ayah>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            result += Ayah(
                numberInSurah = o.getInt("n"),
                arabic = o.getString("ar"),
                translation = o.getString("id")
            )
        }
        if (result.isEmpty()) error("Ayat surah $number kosong")
        return result
    }

    private fun normalizeArabic(value: String): String {
        val out = StringBuilder(value.length)
        value.forEach { c ->
            when {
                c in '\u0610'..'\u061A' -> Unit
                c in '\u064B'..'\u065F' -> Unit
                c == '\u0670' -> Unit
                c in '\u06D6'..'\u06ED' -> Unit
                c == '\u0640' -> Unit
                c == 'أ' || c == 'إ' || c == 'آ' || c == 'ٱ' -> out.append('ا')
                c == 'ى' -> out.append('ي')
                c == 'ؤ' -> out.append('و')
                c == 'ئ' -> out.append('ي')
                c == 'ة' -> out.append('ه')
                else -> out.append(c)
            }
        }
        return out.toString().lowercase(Locale.ROOT)
    }
}
