package com.quriva.app.data

import android.content.Context
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.quriva.app.model.Bookmark
import com.quriva.app.model.LastRead
import com.quriva.app.model.ReadingProgress
import com.quriva.app.model.VerseNote
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Stable
class AppPreferences(context: Context) {

    private val prefs = context.getSharedPreferences("quriva_prefs", Context.MODE_PRIVATE)

    var themeMode by mutableStateOf(prefs.getString(KEY_THEME, "system") ?: "system")
        private set

    var arabicSize by mutableFloatStateOf(prefs.getFloat(KEY_ARABIC_SIZE, 27f))
        private set

    var translationSize by mutableFloatStateOf(prefs.getFloat(KEY_TRANSLATION_SIZE, 15f))
        private set

    var showTranslation by mutableStateOf(prefs.getBoolean(KEY_SHOW_TRANSLATION, true))
        private set

    var showWordByWord by mutableStateOf(prefs.getBoolean(KEY_SHOW_WBW, false))
        private set

    var showTransliteration by mutableStateOf(prefs.getBoolean(KEY_SHOW_TRANSLIT, false))
        private set

    var showTajweed by mutableStateOf(prefs.getBoolean(KEY_SHOW_TAJWEED, false))
        private set

    var lastRead by mutableStateOf(readLastRead())
        private set

    var bookmarks by mutableStateOf(readBookmarks())
        private set

    var notes by mutableStateOf(readNotes())
        private set

    var reciterId by mutableIntStateOf(prefs.getInt(KEY_RECITER, 7))
        private set

    var tafsirId by mutableIntStateOf(prefs.getInt(KEY_TAFSIR, 169))
        private set

    var translationId by mutableIntStateOf(prefs.getInt(KEY_TRANSLATION, 33))
        private set

    var playbackSpeed by mutableFloatStateOf(prefs.getFloat(KEY_PLAYBACK_SPEED, 1.0f))
        private set

    var repeatMode by mutableStateOf(prefs.getString(KEY_REPEAT_MODE, "OFF") ?: "OFF")
        private set

    var sleepTimerMinutes by mutableIntStateOf(prefs.getInt(KEY_SLEEP_TIMER, 0))
        private set

    var autoScrollAudio by mutableStateOf(prefs.getBoolean(KEY_AUTO_SCROLL_AUDIO, true))
        private set

    var ayahAudioMode by mutableStateOf(prefs.getBoolean(KEY_AYAH_AUDIO_MODE, true))
        private set

    var searchHistory by mutableStateOf(readStringList(KEY_SEARCH_HISTORY))
        private set

    var readingProgress by mutableStateOf(readProgress())
        private set

    var khatamTargetDays by mutableIntStateOf(prefs.getInt(KEY_KHATAM_TARGET, 30))
        private set

    var reminderEnabled by mutableStateOf(prefs.getBoolean(KEY_REMINDER_ENABLED, false))
        private set

    var reminderHour by mutableIntStateOf(prefs.getInt(KEY_REMINDER_HOUR, 20))
        private set

    var reminderMinute by mutableIntStateOf(prefs.getInt(KEY_REMINDER_MINUTE, 0))
        private set

    var notificationPermissionAsked by mutableStateOf(
        prefs.getBoolean(KEY_NOTIFICATION_PERMISSION_ASKED, false)
    )
        private set

    var lastAudioSurah by mutableIntStateOf(prefs.getInt(KEY_LAST_AUDIO_SURAH, 0))
        private set
    var lastAudioAyah by mutableIntStateOf(prefs.getInt(KEY_LAST_AUDIO_AYAH, 0))
        private set
    var lastAudioReciter by mutableIntStateOf(prefs.getInt(KEY_LAST_AUDIO_RECITER, 0))
        private set
    var lastAudioPositionMs by mutableLongStateOf(prefs.getLong(KEY_LAST_AUDIO_POS, 0L))
        private set

    var skipMessageDate by mutableStateOf(prefs.getString(KEY_SKIP_MSG_DATE, "") ?: "")
        private set
    var skipMessageText by mutableStateOf(prefs.getString(KEY_SKIP_MSG_TEXT, "") ?: "")
        private set

    var lastUpdateCheckMs by mutableLongStateOf(prefs.getLong(KEY_LAST_UPDATE_CHECK, 0L))
        private set
    var dismissedUpdateTag by mutableStateOf(prefs.getString(KEY_DISMISSED_UPDATE, "") ?: "")
        private set
    var updateNotifiedTag by mutableStateOf(prefs.getString(KEY_UPDATE_NOTIFIED, "") ?: "")
        private set

    fun updateThemeMode(mode: String) {
        themeMode = mode
        prefs.edit().putString(KEY_THEME, mode).apply()
    }

    fun updateArabicSize(sp: Float) {
        arabicSize = sp.coerceIn(20f, 44f)
        prefs.edit().putFloat(KEY_ARABIC_SIZE, arabicSize).apply()
    }

    fun updateTranslationSize(sp: Float) {
        translationSize = sp.coerceIn(12f, 24f)
        prefs.edit().putFloat(KEY_TRANSLATION_SIZE, translationSize).apply()
    }

    fun updateShowTranslation(enabled: Boolean) {
        showTranslation = enabled
        prefs.edit().putBoolean(KEY_SHOW_TRANSLATION, enabled).apply()
    }

    fun updateShowWordByWord(enabled: Boolean) {
        showWordByWord = enabled
        prefs.edit().putBoolean(KEY_SHOW_WBW, enabled).apply()
    }

    fun updateShowTransliteration(enabled: Boolean) {
        showTransliteration = enabled
        prefs.edit().putBoolean(KEY_SHOW_TRANSLIT, enabled).apply()
    }

    fun updateShowTajweed(enabled: Boolean) {
        showTajweed = enabled
        prefs.edit().putBoolean(KEY_SHOW_TAJWEED, enabled).apply()
    }

    fun updateReciter(id: Int) {
        reciterId = id
        prefs.edit().putInt(KEY_RECITER, id).apply()
    }

    fun updateTafsir(id: Int) {
        tafsirId = id
        prefs.edit().putInt(KEY_TAFSIR, id).apply()
    }

    fun updateTranslation(id: Int) {
        translationId = id
        prefs.edit().putInt(KEY_TRANSLATION, id).apply()
    }

    fun updatePlaybackSpeed(speed: Float) {
        playbackSpeed = speed.coerceIn(0.5f, 2.0f)
        prefs.edit().putFloat(KEY_PLAYBACK_SPEED, playbackSpeed).apply()
    }

    fun updateRepeatMode(mode: String) {
        repeatMode = mode
        prefs.edit().putString(KEY_REPEAT_MODE, mode).apply()
    }

    fun updateSleepTimerMinutes(minutes: Int) {
        sleepTimerMinutes = minutes.coerceIn(0, 120)
        prefs.edit().putInt(KEY_SLEEP_TIMER, sleepTimerMinutes).apply()
    }

    fun updateAutoScrollAudio(enabled: Boolean) {
        autoScrollAudio = enabled
        prefs.edit().putBoolean(KEY_AUTO_SCROLL_AUDIO, enabled).apply()
    }

    fun updateAyahAudioMode(enabled: Boolean) {
        ayahAudioMode = enabled
        prefs.edit().putBoolean(KEY_AYAH_AUDIO_MODE, enabled).apply()
    }

    fun updateKhatamTarget(days: Int) {
        khatamTargetDays = days.coerceIn(1, 365)
        prefs.edit().putInt(KEY_KHATAM_TARGET, khatamTargetDays).apply()
    }

    fun updateReminder(enabled: Boolean, hour: Int = reminderHour, minute: Int = reminderMinute) {
        reminderEnabled = enabled
        reminderHour = hour.coerceIn(0, 23)
        reminderMinute = minute.coerceIn(0, 59)
        prefs.edit()
            .putBoolean(KEY_REMINDER_ENABLED, enabled)
            .putInt(KEY_REMINDER_HOUR, reminderHour)
            .putInt(KEY_REMINDER_MINUTE, reminderMinute)
            .apply()
    }

    fun markNotificationPermissionAsked() {
        notificationPermissionAsked = true
        prefs.edit().putBoolean(KEY_NOTIFICATION_PERMISSION_ASKED, true).apply()
    }

    fun saveLastRead(
        surahNumber: Int,
        surahName: String,
        ayah: Int,
        total: Int,
        visibleAyahs: Collection<Int> = listOf(ayah)
    ) {
        if (ayah <= 0 || total <= 0) return
        lastRead = LastRead(surahNumber, surahName, ayah, total)
        prefs.edit()
            .putInt(KEY_LAST_SURAH, surahNumber)
            .putString(KEY_LAST_SURAH_NAME, surahName)
            .putInt(KEY_LAST_AYAH, ayah)
            .putInt(KEY_LAST_TOTAL, total)
            .apply()
        recordReadingProgress(surahNumber, visibleAyahs)
    }

    fun saveLastAudio(surah: Int, ayah: Int, reciter: Int, positionMs: Long) {
        lastAudioSurah = surah
        lastAudioAyah = ayah
        lastAudioReciter = reciter
        lastAudioPositionMs = positionMs
        prefs.edit()
            .putInt(KEY_LAST_AUDIO_SURAH, surah)
            .putInt(KEY_LAST_AUDIO_AYAH, ayah)
            .putInt(KEY_LAST_AUDIO_RECITER, reciter)
            .putLong(KEY_LAST_AUDIO_POS, positionMs)
            .apply()
    }

    fun addSearchHistory(query: String) {
        val q = query.trim()
        if (q.length < 2) return
        val updated = (listOf(q) + searchHistory.filterNot { it.equals(q, true) }).take(12)
        searchHistory = updated
        saveStringList(KEY_SEARCH_HISTORY, updated)
    }

    fun clearSearchHistory() {
        searchHistory = emptyList()
        saveStringList(KEY_SEARCH_HISTORY, emptyList())
    }

    fun isBookmarked(surahNumber: Int, ayahNumber: Int): Boolean =
        bookmarks.any { it.surahNumber == surahNumber && it.ayahNumber == ayahNumber }

    fun toggleBookmark(
        surahNumber: Int,
        surahName: String,
        ayahNumber: Int,
        arabic: String,
        translation: String,
        folder: String = "",
        tags: List<String> = emptyList()
    ): Boolean {
        val existing = bookmarks.firstOrNull {
            it.surahNumber == surahNumber && it.ayahNumber == ayahNumber
        }
        bookmarks = if (existing == null) {
            (listOf(
                Bookmark(
                    surahNumber = surahNumber,
                    surahName = surahName,
                    ayahNumber = ayahNumber,
                    arabic = arabic,
                    translation = translation,
                    createdAt = System.currentTimeMillis(),
                    folder = folder,
                    tags = tags
                )
            ) + bookmarks).take(MAX_BOOKMARKS)
        } else {
            bookmarks.filterNot {
                it.surahNumber == surahNumber && it.ayahNumber == ayahNumber
            }
        }
        persistBookmarks()
        return existing == null
    }

    fun updateBookmarkMeta(surahNumber: Int, ayahNumber: Int, folder: String, tags: List<String>) {
        bookmarks = bookmarks.map {
            if (it.surahNumber == surahNumber && it.ayahNumber == ayahNumber) {
                it.copy(folder = folder, tags = tags)
            } else it
        }
        persistBookmarks()
    }

    fun removeBookmark(surahNumber: Int, ayahNumber: Int) {
        bookmarks = bookmarks.filterNot {
            it.surahNumber == surahNumber && it.ayahNumber == ayahNumber
        }
        persistBookmarks()
    }

    fun clearBookmarks() {
        bookmarks = emptyList()
        persistBookmarks()
    }

    fun bookmarkFolders(): List<String> =
        bookmarks.map { it.folder.trim() }.filter { it.isNotBlank() }.distinct().sorted()

    fun noteFor(surahNumber: Int, ayahNumber: Int): String? =
        notes.firstOrNull { it.surahNumber == surahNumber && it.ayahNumber == ayahNumber }?.text

    fun saveNote(surahNumber: Int, ayahNumber: Int, surahName: String, text: String) {
        val clean = text.trim()
        val existing = notes.filterNot { it.surahNumber == surahNumber && it.ayahNumber == ayahNumber }
        notes = if (clean.isBlank()) existing else {
            (listOf(
                VerseNote(
                    surahNumber = surahNumber,
                    ayahNumber = ayahNumber,
                    surahName = surahName,
                    text = clean,
                    updatedAt = System.currentTimeMillis()
                )
            ) + existing).take(MAX_NOTES)
        }
        persistNotes()
    }

    fun removeNote(surahNumber: Int, ayahNumber: Int) {
        notes = notes.filterNot { it.surahNumber == surahNumber && it.ayahNumber == ayahNumber }
        persistNotes()
    }

    fun recordReadingProgress(surahNumber: Int, ayahNumber: Int) =
        recordReadingProgress(surahNumber, listOf(ayahNumber))

    fun recordReadingProgress(surahNumber: Int, ayahNumbers: Collection<Int>) {
        val validAyahs = ayahNumbers.asSequence().filter { it > 0 }.distinct().toList()
        if (validAyahs.isEmpty()) return
        val today = todayKey()
        val current = readingProgress
        val dayMap = current.daily.toMutableMap()
        val daySet = (dayMap[today] ?: emptySet()).toMutableSet()
        val before = daySet.size
        validAyahs.forEach { ayah -> daySet += "$surahNumber:$ayah" }
        val added = daySet.size - before
        if (added <= 0) return
        dayMap[today] = daySet
        val trimmed = trimDaily(dayMap)
        readingProgress = current.copy(
            daily = trimmed,
            lastDate = today,
            totalVersesRead = (current.totalVersesRead + added).coerceAtMost(500000)
        )
        persistProgress()
        clearSkipMessageIfActive()
    }

    fun recordPlayActivity(count: Int = 1) {
        if (count <= 0) return
        val today = todayKey()
        val current = readingProgress
        val play = current.playDaily.toMutableMap()
        play[today] = (play[today] ?: 0) + count
        val trimmedPlay = play.entries.sortedByDescending { it.key }.take(120).associate { it.key to it.value }
        readingProgress = current.copy(
            playDaily = trimmedPlay,
            lastDate = today,
            totalPlayedAyah = (current.totalPlayedAyah + count).coerceAtMost(500000)
        )
        persistProgress()
        clearSkipMessageIfActive()
    }

    fun hasActivityOn(date: String): Boolean {
        val d = readingProgress.daily[date]
        val p = readingProgress.playDaily[date] ?: 0
        return (d?.isNotEmpty() == true) || p > 0
    }

    fun evaluateSkipMessage() {
        val today = todayKey()
        val yesterday = dayOffset(-1)
        val beforeYesterday = dayOffset(-2)
        // show message today if yesterday had no activity (and it's a new day context)
        if (!hasActivityOn(yesterday) && (hasActivityOn(beforeYesterday) || readingProgress.lastDate.isNotBlank() || lastRead != null)) {
            val msg = "Kamu belum membaca Al-Qur'an selama 1 hari"
            if (skipMessageDate != today || skipMessageText != msg) {
                skipMessageDate = today
                skipMessageText = msg
                prefs.edit().putString(KEY_SKIP_MSG_DATE, today).putString(KEY_SKIP_MSG_TEXT, msg).apply()
            }
        } else if (skipMessageDate.isNotBlank() && skipMessageDate != today) {
            // clear old
            skipMessageDate = ""
            skipMessageText = ""
            prefs.edit().remove(KEY_SKIP_MSG_DATE).remove(KEY_SKIP_MSG_TEXT).apply()
        }
    }

    fun dismissSkipMessage() {
        skipMessageDate = ""
        skipMessageText = ""
        prefs.edit().remove(KEY_SKIP_MSG_DATE).remove(KEY_SKIP_MSG_TEXT).apply()
    }

    private fun clearSkipMessageIfActive() {
        if (skipMessageDate == todayKey() && skipMessageText.isNotBlank()) {
            dismissSkipMessage()
        }
    }

    fun markUpdateChecked() {
        lastUpdateCheckMs = System.currentTimeMillis()
        prefs.edit().putLong(KEY_LAST_UPDATE_CHECK, lastUpdateCheckMs).apply()
    }

    fun dismissUpdateTag(tag: String) {
        dismissedUpdateTag = tag
        prefs.edit().putString(KEY_DISMISSED_UPDATE, tag).apply()
    }

    fun markUpdateNotified(tag: String) {
        updateNotifiedTag = tag
        prefs.edit().putString(KEY_UPDATE_NOTIFIED, tag).apply()
    }

    fun clearDismissedUpdate() {
        dismissedUpdateTag = ""
        prefs.edit().remove(KEY_DISMISSED_UPDATE).apply()
    }

    private fun trimDaily(dayMap: MutableMap<String, Set<String>>): Map<String, Set<String>> =
        dayMap.entries.sortedByDescending { it.key }.take(120).associate { it.key to it.value }

    private fun dayOffset(offset: Int): String {
        val cal = java.util.Calendar.getInstance()
        cal.add(java.util.Calendar.DAY_OF_YEAR, offset)
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
    }

    fun resetReadingProgress() {
        readingProgress = ReadingProgress()
        persistProgress()
    }

    private fun persistBookmarks() {
        val array = JSONArray()
        bookmarks.forEach { bookmark ->
            array.put(
                JSONObject()
                    .put("surah", bookmark.surahNumber)
                    .put("surahName", bookmark.surahName)
                    .put("ayah", bookmark.ayahNumber)
                    .put("arabic", bookmark.arabic)
                    .put("translation", bookmark.translation)
                    .put("createdAt", bookmark.createdAt)
                    .put("folder", bookmark.folder)
                    .put("tags", JSONArray(bookmark.tags))
            )
        }
        prefs.edit().putString(KEY_BOOKMARKS, array.toString()).apply()
    }

    private fun readBookmarks(): List<Bookmark> {
        val raw = prefs.getString(KEY_BOOKMARKS, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val o = array.getJSONObject(i)
                    val surah = o.optInt("surah")
                    val ayah = o.optInt("ayah")
                    if (surah > 0 && ayah > 0) {
                        val tagsArr = o.optJSONArray("tags")
                        val tags = buildList {
                            if (tagsArr != null) for (j in 0 until tagsArr.length()) {
                                val t = tagsArr.optString(j).trim()
                                if (t.isNotBlank()) add(t)
                            }
                        }
                        add(
                            Bookmark(
                                surahNumber = surah,
                                surahName = o.optString("surahName", "Surah $surah"),
                                ayahNumber = ayah,
                                arabic = o.optString("arabic"),
                                translation = o.optString("translation"),
                                createdAt = o.optLong("createdAt"),
                                folder = o.optString("folder"),
                                tags = tags
                            )
                        )
                    }
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun persistNotes() {
        val array = JSONArray()
        notes.forEach { n ->
            array.put(
                JSONObject()
                    .put("surah", n.surahNumber)
                    .put("ayah", n.ayahNumber)
                    .put("surahName", n.surahName)
                    .put("text", n.text)
                    .put("updatedAt", n.updatedAt)
            )
        }
        prefs.edit().putString(KEY_NOTES, array.toString()).apply()
    }

    private fun readNotes(): List<VerseNote> {
        val raw = prefs.getString(KEY_NOTES, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val o = array.getJSONObject(i)
                    val surah = o.optInt("surah")
                    val ayah = o.optInt("ayah")
                    val text = o.optString("text")
                    if (surah > 0 && ayah > 0 && text.isNotBlank()) {
                        add(
                            VerseNote(
                                surahNumber = surah,
                                ayahNumber = ayah,
                                surahName = o.optString("surahName", "Surah $surah"),
                                text = text,
                                updatedAt = o.optLong("updatedAt")
                            )
                        )
                    }
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun persistProgress() {
        val obj = JSONObject()
            .put("lastDate", readingProgress.lastDate)
            .put("total", readingProgress.totalVersesRead)
            .put("totalPlayed", readingProgress.totalPlayedAyah)
        val daily = JSONObject()
        readingProgress.daily.forEach { (k, v) ->
            daily.put(k, JSONArray(v.toList()))
        }
        obj.put("daily", daily)
        val playDaily = JSONObject()
        readingProgress.playDaily.forEach { (k, v) -> playDaily.put(k, v) }
        obj.put("playDaily", playDaily)
        prefs.edit().putString(KEY_PROGRESS, obj.toString()).apply()
    }

    private fun readProgress(): ReadingProgress {
        val raw = prefs.getString(KEY_PROGRESS, null) ?: return ReadingProgress()
        return try {
            val obj = JSONObject(raw)
            val dailyObj = obj.optJSONObject("daily") ?: JSONObject()
            val daily = LinkedHashMap<String, Set<String>>()
            dailyObj.keys().forEach { k ->
                val arr = dailyObj.optJSONArray(k) ?: JSONArray()
                val set = buildSet {
                    for (i in 0 until arr.length()) add(arr.optString(i))
                }
                if (set.isNotEmpty()) daily[k] = set
            }
            val playObj = obj.optJSONObject("playDaily") ?: JSONObject()
            val playDaily = LinkedHashMap<String, Int>()
            playObj.keys().forEach { k ->
                val v = playObj.optInt(k, 0)
                if (v > 0) playDaily[k] = v
            }
            ReadingProgress(
                daily = daily,
                playDaily = playDaily,
                lastDate = obj.optString("lastDate"),
                totalVersesRead = obj.optInt("total", 0),
                totalPlayedAyah = obj.optInt("totalPlayed", 0)
            )
        } catch (_: Exception) {
            ReadingProgress()
        }
    }

    private fun readLastRead(): LastRead? {
        val surah = prefs.getInt(KEY_LAST_SURAH, 0)
        val ayah = prefs.getInt(KEY_LAST_AYAH, 0)
        val total = prefs.getInt(KEY_LAST_TOTAL, 0)
        if (surah <= 0 || ayah <= 0 || total <= 0) return null
        return LastRead(
            surahNumber = surah,
            surahName = prefs.getString(KEY_LAST_SURAH_NAME, "") ?: "Surah $surah",
            ayah = ayah,
            totalAyah = total
        )
    }

    private fun saveStringList(key: String, values: List<String>) {
        prefs.edit().putString(key, JSONArray(values).toString()).apply()
    }

    private fun readStringList(key: String): List<String> {
        val raw = prefs.getString(key, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            List(arr.length()) { arr.optString(it) }.filter { it.isNotBlank() }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun todayKey(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    private companion object {
        const val KEY_THEME = "theme_mode"
        const val KEY_ARABIC_SIZE = "arabic_size"
        const val KEY_TRANSLATION_SIZE = "translation_size"
        const val KEY_SHOW_TRANSLATION = "show_translation"
        const val KEY_SHOW_WBW = "show_wbw"
        const val KEY_SHOW_TRANSLIT = "show_translit"
        const val KEY_SHOW_TAJWEED = "show_tajweed"
        const val KEY_LAST_SURAH = "last_surah"
        const val KEY_LAST_SURAH_NAME = "last_surah_name"
        const val KEY_LAST_AYAH = "last_ayah"
        const val KEY_LAST_TOTAL = "last_total"
        const val KEY_BOOKMARKS = "bookmarks"
        const val KEY_NOTES = "verse_notes"
        const val KEY_RECITER = "reciter_id"
        const val KEY_TAFSIR = "tafsir_id"
        const val KEY_TRANSLATION = "translation_id"
        const val KEY_PLAYBACK_SPEED = "playback_speed"
        const val KEY_REPEAT_MODE = "repeat_mode"
        const val KEY_SLEEP_TIMER = "sleep_timer"
        const val KEY_AUTO_SCROLL_AUDIO = "auto_scroll_audio"
        const val KEY_AYAH_AUDIO_MODE = "ayah_audio_mode"
        const val KEY_SEARCH_HISTORY = "search_history"
        const val KEY_PROGRESS = "reading_progress"
        const val KEY_KHATAM_TARGET = "khatam_target"
        const val KEY_REMINDER_ENABLED = "reminder_enabled"
        const val KEY_REMINDER_HOUR = "reminder_hour"
        const val KEY_REMINDER_MINUTE = "reminder_minute"
        const val KEY_NOTIFICATION_PERMISSION_ASKED = "notification_permission_asked"
        const val KEY_LAST_AUDIO_SURAH = "last_audio_surah"
        const val KEY_LAST_AUDIO_AYAH = "last_audio_ayah"
        const val KEY_LAST_AUDIO_RECITER = "last_audio_reciter"
        const val KEY_LAST_AUDIO_POS = "last_audio_pos"
        const val KEY_SKIP_MSG_DATE = "skip_msg_date"
        const val KEY_SKIP_MSG_TEXT = "skip_msg_text"
        const val KEY_LAST_UPDATE_CHECK = "last_update_check_ms"
        const val KEY_DISMISSED_UPDATE = "dismissed_update_tag"
        const val KEY_UPDATE_NOTIFIED = "update_notified_tag"
        const val MAX_BOOKMARKS = 1000
        const val MAX_NOTES = 1000
    }
}
