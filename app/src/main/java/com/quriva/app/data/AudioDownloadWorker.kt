package com.quriva.app.data

import android.content.Context
import androidx.work.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class AudioDownloadWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val surah = inputData.getInt(KEY_SURAH, 0)
        val reciter = inputData.getInt(KEY_RECITER, 0)
        val count = inputData.getInt(KEY_COUNT, 0)
        val name = inputData.getString(KEY_NAME).orEmpty()
        if (surah !in 1..114 || reciter <= 0 || count <= 0) return Result.failure()
        return try {
            download(surah, reciter, count, name)
            Result.success()
        } catch (e: kotlinx.coroutines.CancellationException) { throw e }
        catch (_: Throwable) { if (runAttemptCount < 3) Result.retry() else Result.failure() }
    }

    private suspend fun download(surah: Int, reciter: Int, count: Int, name: String) {
        val list = QuranApi.ayahAudioList(reciter, surah).associateBy { it.ayahNumber }
        if ((1..count).any { it !in list }) throw IOException("Audio tidak lengkap")
        val dir = SurahAudioPlayer.audioDir(applicationContext)
        withContext(Dispatchers.IO) {
            SurahAudioPlayer.downloadMarker(applicationContext, surah, reciter).delete()
            try {
                for (ayah in 1..count) {
                    currentCoroutineContext().ensureActive()
                    val audio = list.getValue(ayah)
                    val out = File(dir, "${reciter}_${surah}_${ayah}.mp3")
                    val temp = File(out.absolutePath + ".part")
                    val verified = File(out.absolutePath + ".ok")
                    val verifiedSize = runCatching { verified.takeIf { it.exists() }?.readText()?.toLongOrNull() }.getOrNull()
                    if (out.exists() && verifiedSize == out.length() && out.length() > 1024) {
                        val progress = ayah * 100 / count
                        setProgress(workDataOf("progress" to progress, KEY_SURAH to surah))
                        continue
                    }
                    out.delete(); verified.delete()
                    val conn = (URL(audio.url).openConnection() as HttpURLConnection).apply {
                        connectTimeout = 15_000; readTimeout = 45_000; instanceFollowRedirects = true
                        setRequestProperty("User-Agent", "Quriva/2.1.0 (Android)")
                    }
                    try {
                        if (conn.responseCode !in 200..299) throw IOException("HTTP ${conn.responseCode}")
                        conn.inputStream.use { input -> FileOutputStream(temp).use { output ->
                            val buffer = ByteArray(16 * 1024)
                            while (true) { currentCoroutineContext().ensureActive(); val read = input.read(buffer); if (read < 0) break; output.write(buffer, 0, read) }
                        } }
                        if (temp.length() <= 1024 || !temp.renameTo(out)) throw IOException("File audio rusak")
                        verified.writeText(out.length().toString())
                    } finally { conn.disconnect(); temp.delete() }
                    val progress = ayah * 100 / count
                    setProgress(workDataOf("progress" to progress, KEY_SURAH to surah))
                    NotificationCenter.notifyDownload(applicationContext, "Surah $name", progress)
                }
                SurahAudioPlayer.downloadMarker(applicationContext, surah, reciter).writeText(count.toString())
                NotificationCenter.notifyDownload(applicationContext, "Surah $name selesai", 100, done = true)
            } catch (t: Throwable) { throw t }
        }
    }

    companion object {
        const val WORK_NAME = "quriva_audio_queue"
        const val TAG_PREFIX = "quriva_audio_surah_"
        const val QUEUE_TAG = "quriva_audio_download"
        const val KEY_SURAH = "surah"
        private const val KEY_RECITER = "reciter"
        private const val KEY_COUNT = "count"
        private const val KEY_NAME = "name"

        fun enqueue(context: Context, surah: Int, name: String, count: Int, reciter: Int, wifiOnly: Boolean) {
            val constraints = Constraints.Builder().setRequiredNetworkType(if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED).build()
            val request = OneTimeWorkRequestBuilder<AudioDownloadWorker>().setConstraints(constraints)
                .addTag(QUEUE_TAG)
                .addTag("$TAG_PREFIX$surah")
                .setInputData(workDataOf(KEY_SURAH to surah, KEY_NAME to name, KEY_COUNT to count, KEY_RECITER to reciter)).build()
            WorkManager.getInstance(context).enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.APPEND_OR_REPLACE, request)
        }
        fun cancelAll(context: Context) = WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)

        fun surahFromTags(tags: Set<String>): Int = tags.firstNotNullOfOrNull { tag ->
            tag.takeIf { it.startsWith(TAG_PREFIX) }?.removePrefix(TAG_PREFIX)?.toIntOrNull()
        } ?: 0
    }
}
