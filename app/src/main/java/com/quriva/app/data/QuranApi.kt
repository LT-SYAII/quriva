package com.quriva.app.data

import androidx.compose.runtime.Immutable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException

@Immutable
data class Reciter(
    val id: Int,
    val name: String,
    val style: String
)

@Immutable
data class TafsirOption(
    val id: Int,
    val name: String,
    val author: String,
    val language: String,
    val isArabic: Boolean
)

@Immutable
data class TranslationOption(
    val id: Int,
    val name: String,
    val author: String,
    val language: String
)

@Immutable
data class TafsirContent(
    val resourceName: String,
    val languageName: String,
    val html: String
)

@Immutable
data class VerseText(
    val verseKey: String,
    val text: String
)

@Immutable
data class WordItem(
    val text: String,
    val translation: String,
    val transliteration: String,
    val isEnd: Boolean
)

@Immutable
data class PageAyah(
    val surah: Int,
    val ayah: Int,
    val verseKey: String,
    val text: String,
    val page: Int,
    val juz: Int,
    val hizb: Int,
    val rub: Int,
    val manzil: Int
)

object Reciters {
    val all = listOf(
        Reciter(7, "Mishary Rashid al-Afasy", "Murattal"),
        Reciter(2, "AbdulBaset AbdulSamad", "Murattal"),
        Reciter(3, "Abdur-Rahman as-Sudais", "Murottal"),
        Reciter(4, "Abu Bakr al-Shatri", "Murottal"),
        Reciter(5, "Hani ar-Rifai", "Murottal"),
        Reciter(6, "Mahmoud Khalil Al-Husary", "Murottal"),
        Reciter(9, "Mohamed Siddiq al-Minshawi", "Murattal"),
        Reciter(10, "Sa'ud ash-Shuraym", "Murottal"),
        Reciter(11, "Mohamed al-Tablawi", "Murottal"),
        Reciter(12, "Mahmoud Khalil Al-Husary", "Muallim"),
        Reciter(1, "AbdulBaset AbdulSamad", "Mujawwad"),
        Reciter(8, "Mohamed Siddiq al-Minshawi", "Mujawwad")
    )

    fun byId(id: Int): Reciter = all.firstOrNull { it.id == id } ?: all.first()
}

object Tafsirs {
    // Curated set including Arabic, English and popular ones available on quran.com.
    // Indonesian tafsir is not available in the quran.com API; users can pick
    // Arabic/English or use bundled Kemenag translation for understanding.
    val all = listOf(
        TafsirOption(169, "Ibn Kathir (Abridged)", "Hafiz Ibn Kathir", "Inggris", false),
        TafsirOption(168, "Ma'arif al-Qur'an", "Mufti Muhammad Shafi", "Inggris", false),
        TafsirOption(817, "Tazkirul Quran", "Maulana Wahiduddin Khan", "Inggris", false),
        TafsirOption(16, "Tafsir Muyassar", "المیسر", "Arab", true),
        TafsirOption(14, "Tafsir Ibn Kathir", "Ibn Kathir", "Arab", true),
        TafsirOption(90, "Al-Qurtubi", "Al-Qurtubi", "Arab", true),
        TafsirOption(91, "As-Sa'di", "As-Sa'di", "Arab", true),
        TafsirOption(94, "Al-Baghawi", "Al-Baghawi", "Arab", true)
    )

    fun byId(id: Int): TafsirOption = all.firstOrNull { it.id == id } ?: all.first()
}

object Translations {
    // Kemenag RI (33) is bundled offline in assets; others can be fetched online.
    val all = listOf(
        TranslationOption(33, "Kemenag RI", "Kementerian Agama RI", "Indonesia (offline)"),
        TranslationOption(134, "King Fahad Quran Complex", "King Fahad", "Indonesia"),
        TranslationOption(141, "The Sabiq Company", "Sabiq", "Indonesia"),
        TranslationOption(20, "Saheeh International", "Saheeh International", "Inggris"),
        TranslationOption(84, "T. Usmani", "Mufti Taqi Usmani", "Inggris"),
        TranslationOption(85, "M.A.S. Abdel Haleem", "Abdul Haleem", "Inggris"),
        TranslationOption(22, "Pickthall", "Pickthall", "Inggris"),
        TranslationOption(19, "Yusuf Ali", "Yusuf Ali", "Inggris")
    )

    fun byId(id: Int): TranslationOption = all.firstOrNull { it.id == id } ?: all.first()
}

/**
 * Client for the public Quran.com API v4. Core Quran text/translation (Kemenag)
 * stays bundled offline; this client supplies audio, per-ayah audio, tafsir,
 * extra translations, word-by-word, tajweed, and navigation metadata.
 */
object QuranApi {

    private const val BASE = "https://api.quran.com/api/v4"

    suspend fun chapterAudioUrl(reciterId: Int, surahNumber: Int): String =
        withContext(Dispatchers.IO) {
            val json = getJson("$BASE/chapter_recitations/$reciterId/$surahNumber")
            val file = json.optJSONObject("audio_file")
                ?: throw IOException("Audio untuk surah ini tidak tersedia.")
            normalizeAudioUrl(file.optString("audio_url"))
        }

    suspend fun ayahAudioList(reciterId: Int, surahNumber: Int): List<AyahAudio> =
        withContext(Dispatchers.IO) {
            val json = getJson("$BASE/recitations/$reciterId/by_chapter/$surahNumber?per_page=300")
            val arr = json.optJSONArray("audio_files") ?: JSONArray()
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: continue
                    val verseKey = o.optString("verse_key")
                    val ayah = verseKey.substringAfter(':').toIntOrNull() ?: continue
                    val url = normalizeAudioUrl(o.optString("url"))
                    add(AyahAudio(ayahNumber = ayah, verseKey = verseKey, url = url))
                }
            }.sortedBy { it.ayahNumber }
        }

    suspend fun tafsir(tafsirId: Int, surahNumber: Int, ayahNumber: Int): TafsirContent =
        withContext(Dispatchers.IO) {
            val key = "$surahNumber:$ayahNumber"
            val json = getJson("$BASE/tafsirs/$tafsirId/by_ayah/$key")
            val tafsir = json.optJSONObject("tafsir")
                ?: throw IOException("Tafsir untuk ayat ini tidak tersedia.")
            val html = tafsir.optString("text")
            if (html.isBlank()) throw IOException("Tafsir untuk ayat ini kosong.")
            val language = tafsir.optJSONObject("translated_name")
                ?.optString("language_name")
                .orEmpty()
            TafsirContent(
                resourceName = tafsir.optString("resource_name", "Tafsir"),
                languageName = language,
                html = html
            )
        }

    suspend fun translationForSurah(translationId: Int, surahNumber: Int): List<VerseText> =
        withContext(Dispatchers.IO) {
            val all = ArrayList<VerseText>(300)
            var page = 1
            while (true) {
                // The /quran/translations endpoint omits verse_key, so use the
                // verses endpoint which returns the key together with the text.
                val url = "$BASE/verses/by_chapter/$surahNumber" +
                    "?translations=$translationId&fields=text_uthmani&per_page=50&page=$page"
                val json = getJson(url)
                val arr = json.optJSONArray("verses") ?: JSONArray()
                for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: continue
                    val key = o.optString("verse_key")
                    val translations = o.optJSONArray("translations") ?: JSONArray()
                    var text = ""
                    for (j in 0 until translations.length()) {
                        val t = translations.optJSONObject(j) ?: continue
                        if (t.optInt("resource_id", translationId) == translationId) {
                            text = cleanHtml(t.optString("text"))
                            break
                        }
                    }
                    if (text.isBlank() && translations.length() > 0) {
                        text = cleanHtml(translations.optJSONObject(0)?.optString("text").orEmpty())
                    }
                    if (key.isNotBlank() && text.isNotBlank()) {
                        all += VerseText(verseKey = key, text = text)
                    }
                }
                val pagination = json.optJSONObject("pagination")
                val next = pagination?.optInt("next_page", -1) ?: -1
                if (next <= 0 || arr.length() == 0) break
                page = next
            }
            all
        }

    suspend fun wordsForSurah(surahNumber: Int): Map<String, List<WordItem>> =
        withContext(Dispatchers.IO) {
            val result = LinkedHashMap<String, List<WordItem>>()
            var page = 1
            while (true) {
                val url = "$BASE/verses/by_chapter/$surahNumber" +
                    "?language=id&words=true&word_fields=text_uthmani,transliteration" +
                    "&per_page=50&page=$page"
                val json = getJson(url)
                val verses = json.optJSONArray("verses") ?: JSONArray()
                for (i in 0 until verses.length()) {
                    val v = verses.optJSONObject(i) ?: continue
                    val key = v.optString("verse_key")
                    val wordsArr = v.optJSONArray("words") ?: JSONArray()
                    val words = ArrayList<WordItem>(wordsArr.length())
                    for (j in 0 until wordsArr.length()) {
                        val w = wordsArr.optJSONObject(j) ?: continue
                        val type = w.optString("char_type_name")
                        words += WordItem(
                            text = w.optString("text_uthmani").ifBlank { w.optString("text") },
                            translation = w.optJSONObject("translation")?.optString("text").orEmpty(),
                            transliteration = w.optJSONObject("transliteration")?.optString("text").orEmpty(),
                            isEnd = type == "end"
                        )
                    }
                    if (key.isNotBlank()) result[key] = words
                }
                val pagination = json.optJSONObject("pagination")
                val next = pagination?.optInt("next_page", -1) ?: -1
                if (next <= 0 || verses.length() == 0) break
                page = next
            }
            result
        }

    suspend fun tajweedForSurah(surahNumber: Int): Map<String, String> =
        withContext(Dispatchers.IO) {
            val json = getJson("$BASE/quran/verses/uthmani_tajweed?chapter_number=$surahNumber")
            val arr = json.optJSONArray("verses") ?: JSONArray()
            buildMap {
                for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: continue
                    val key = o.optString("verse_key")
                    val text = o.optString("text_uthmani_tajweed")
                    if (key.isNotBlank() && text.isNotBlank()) put(key, text)
                }
            }
        }

    suspend fun versesByPage(page: Int): List<PageAyah> =
        withContext(Dispatchers.IO) {
            val all = ArrayList<PageAyah>(20)
            var p = 1
            while (true) {
                val url = "$BASE/verses/by_page/$page" +
                    "?language=id&words=false&fields=text_uthmani,page_number,juz_number,hizb_number,rub_el_hizb_number,manzil_number,verse_key" +
                    "&per_page=50&page=$p"
                val json = getJson(url)
                val arr = json.optJSONArray("verses") ?: JSONArray()
                for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: continue
                    val key = o.optString("verse_key")
                    val surah = key.substringBefore(':').toIntOrNull() ?: continue
                    val ayah = key.substringAfter(':').toIntOrNull() ?: continue
                    all += PageAyah(
                        surah = surah,
                        ayah = ayah,
                        verseKey = key,
                        text = o.optString("text_uthmani"),
                        page = o.optInt("page_number", page),
                        juz = o.optInt("juz_number", 0),
                        hizb = o.optInt("hizb_number", 0),
                        rub = o.optInt("rub_el_hizb_number", 0),
                        manzil = o.optInt("manzil_number", 0)
                    )
                }
                val pagination = json.optJSONObject("pagination")
                val next = pagination?.optInt("next_page", -1) ?: -1
                if (next <= 0 || arr.length() == 0) break
                p = next
            }
            all
        }

    fun friendlyNetworkError(error: Throwable): String = when (error) {
        is UnknownHostException ->
            "Tidak ada koneksi internet. Sambungkan perangkat lalu coba lagi."
        is SocketTimeoutException ->
            "Koneksi terlalu lama. Coba lagi."
        else -> error.message ?: "Koneksi gagal. Coba lagi."
    }

    private fun normalizeAudioUrl(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.isBlank()) throw IOException("Audio tidak tersedia.")
        return when {
            trimmed.startsWith("http://") || trimmed.startsWith("https://") -> trimmed
            trimmed.startsWith("//") -> "https:$trimmed"
            else -> "https://verses.quran.com/$trimmed"
        }
    }

    private fun cleanHtml(html: String): String {
        if (html.isBlank()) return ""
        val plain = StringBuilder(html.length)
        var supDepth = 0
        Regex("(?s)<[^>]*>|[^<]+").findAll(html).forEach { match ->
            val token = match.value
            if (token.startsWith('<')) {
                val lower = token.lowercase()
                val opens = Regex("<sup\\b").findAll(lower).count()
                val closes = Regex("</sup\\s*>").findAll(lower).count()
                if (opens > 0) supDepth += opens
                if (closes > 0) supDepth = (supDepth - closes).coerceAtLeast(0)
                if (supDepth == 0 && (lower.startsWith("<br") || lower.startsWith("</p"))) {
                    plain.append('\n')
                } else if (supDepth == 0 && opens == 0 && closes == 0) {
                    plain.append(' ')
                }
            } else if (supDepth == 0) {
                plain.append(token)
            }
        }
        return plain.toString()
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun getJson(path: String): JSONObject {
        val connection = (URL(path).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15_000
            readTimeout = 30_000
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "Quriva/2.1.0 (Android)")
        }
        try {
            val code = connection.responseCode
            if (code !in 200..299) {
                throw IOException("Server mengembalikan kode $code.")
            }
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            return JSONObject(body)
        } catch (e: IOException) {
            throw e
        } catch (e: Exception) {
            throw IOException("Respons server tidak dapat dibaca.", e)
        } finally {
            connection.disconnect()
        }
    }
}
