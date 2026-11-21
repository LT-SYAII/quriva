package com.quriva.app.data

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import java.io.File

/**
 * UI-side connection to [PlaybackService]. Supports ayah-level playlists,
 * repeat modes, speed, sleep timer, auto-next surah, and background playback.
 */
@Stable
class SurahAudioPlayer(context: Context) {

    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())

    private val controllerFuture: ListenableFuture<MediaController> =
        MediaController.Builder(
            appContext,
            SessionToken(
                appContext,
                ComponentName(appContext, PlaybackService::class.java)
            )
        ).buildAsync()

    private var controller: MediaController? = null
    private var pendingAction: ((MediaController) -> Unit)? = null

    var onAyahPlayed: ((surah: Int, ayah: Int) -> Unit)? = null

    var isPlaying by mutableStateOf(false)
        private set
    var isBuffering by mutableStateOf(false)
        private set
    var durationMs by mutableLongStateOf(0L)
        private set
    var positionMs by mutableLongStateOf(0L)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set
    var currentTitle by mutableStateOf<String?>(null)
        private set

    var activeSurahNumber by mutableIntStateOf(0)
        private set
    var activeSurahName by mutableStateOf<String?>(null)
        private set
    var activeReciterId by mutableIntStateOf(0)
        private set
    var activeAyahNumber by mutableIntStateOf(0)
        private set
    var playlistSize by mutableIntStateOf(0)
        private set

    var repeatMode by mutableStateOf(RepeatMode.OFF)
        private set
    var playbackSpeed by mutableFloatStateOf(1.0f)
        private set
    var sleepTimerRemainingMs by mutableLongStateOf(0L)
        private set

    private var sleepAtElapsed: Long = 0L
    private val sleepTicker = object : Runnable {
        override fun run() {
            val remaining = sleepAtElapsed - android.os.SystemClock.elapsedRealtime()
            if (remaining <= 0L) {
                sleepTimerRemainingMs = 0L
                sleepAtElapsed = 0L
                controller?.pause()
            } else {
                sleepTimerRemainingMs = remaining
                mainHandler.postDelayed(this, 1000L)
            }
        }
    }

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            syncState()
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val prev = activeAyahNumber
            syncState()
            val ayah = activeAyahNumber
            val surah = activeSurahNumber
            if (ayah > 0 && surah > 0 && ayah != prev) {
                onAyahPlayed?.invoke(surah, ayah)
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            syncState()
        }

        override fun onPlayerError(error: PlaybackException) {
            errorMessage = when (error.errorCode) {
                PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
                PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ->
                    "Tidak dapat terhubung ke server audio. Periksa koneksi internet."
                PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND,
                PlaybackException.ERROR_CODE_IO_NO_PERMISSION ->
                    "File audio tidak ditemukan."
                else -> "Audio gagal diputar. Coba lagi."
            }
            isBuffering = false
        }
    }

    init {
        controllerFuture.addListener(
            {
                try {
                    val connected = controllerFuture.get()
                    controller = connected
                    connected.addListener(listener)
                    syncState()
                    pendingAction?.invoke(connected)
                    pendingAction = null
                } catch (_: Throwable) {
                    errorMessage = "Pemutar audio tidak dapat disiapkan."
                }
            },
            ContextCompat.getMainExecutor(appContext)
        )
    }

    /** Play a full surah as per-ayah playlist (preferred for highlight/repeat). */
    fun playSurah(
        surahNumber: Int,
        surahName: String,
        reciterId: Int,
        reciterName: String,
        ayahs: List<AyahAudio>,
        startAyah: Int = 1,
        startPositionMs: Long = 0L,
        useVerifiedDownload: Boolean = false
    ) {
        if (ayahs.isEmpty()) return
        val action: (MediaController) -> Unit = { c ->
            errorMessage = null
            val items = ayahs.map { a ->
                buildAyahItem(
                    surahNumber,
                    surahName,
                    reciterId,
                    reciterName,
                    a,
                    useVerifiedDownload
                )
            }
            val startIndex = (ayahs.indexOfFirst { it.ayahNumber == startAyah }).coerceAtLeast(0)
            c.setMediaItems(items, startIndex, startPositionMs.coerceAtLeast(0L))
            applyPlayerModes(c)
            c.prepare()
            c.play()
        }
        runOrQueue(action)
    }

    /** Fallback: play a single chapter audio URL. */
    fun playChapterUrl(
        surahNumber: Int,
        surahName: String,
        reciterId: Int,
        reciterName: String,
        url: String
    ) {
        val action: (MediaController) -> Unit = { c ->
            errorMessage = null
            val extras = Bundle().apply {
                putInt(EXTRA_SURAH, surahNumber)
                putString(EXTRA_SURAH_NAME, surahName)
                putInt(EXTRA_RECITER, reciterId)
                putInt(EXTRA_AYAH, 0)
            }
            val item = MediaItem.Builder()
                .setUri(url)
                .setMediaId("$surahNumber:$reciterId:0")
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle("$surahName · $reciterName")
                        .setArtist("Quriva · Murottal")
                        .setExtras(extras)
                        .build()
                )
                .build()
            c.setMediaItem(item)
            applyPlayerModes(c)
            c.prepare()
            c.play()
        }
        runOrQueue(action)
    }

    fun toggle() {
        val c = controller ?: return
        if (c.mediaItemCount == 0) return
        if (c.isPlaying) c.pause() else {
            if (c.playbackState == Player.STATE_ENDED) c.seekToDefaultPosition(0)
            c.play()
        }
    }

    fun seekToAyah(ayahNumber: Int) {
        val c = controller ?: return
        val count = c.mediaItemCount
        for (i in 0 until count) {
            val item = c.getMediaItemAt(i)
            val ayah = item.mediaMetadata.extras?.getInt(EXTRA_AYAH, 0) ?: 0
            if (ayah == ayahNumber) {
                c.seekToDefaultPosition(i)
                if (!c.isPlaying) c.play()
                return
            }
        }
    }

    fun next() { controller?.seekToNextMediaItem() }
    fun previous() { controller?.seekToPreviousMediaItem() }

    fun seekToFraction(fraction: Float) {
        val c = controller ?: return
        val duration = c.duration
        if (duration > 0) {
            c.seekTo((duration * fraction.coerceIn(0f, 1f)).toLong())
            syncPosition()
        }
    }

    fun setRepeat(mode: RepeatMode) {
        repeatMode = mode
        controller?.let { applyRepeat(it, mode) }
    }

    fun setSpeed(speed: Float) {
        playbackSpeed = speed.coerceIn(0.5f, 2.0f)
        controller?.setPlaybackSpeed(playbackSpeed)
    }

    fun setSleepTimer(minutes: Int) {
        mainHandler.removeCallbacks(sleepTicker)
        val mins = minutes.coerceIn(0, 120)
        if (mins <= 0) {
            sleepAtElapsed = 0L
            sleepTimerRemainingMs = 0L
        } else {
            val ms = mins * 60_000L
            sleepAtElapsed = android.os.SystemClock.elapsedRealtime() + ms
            sleepTimerRemainingMs = ms
            mainHandler.postDelayed(sleepTicker, 1000L)
        }
    }

    fun cancelSleepTimer() = setSleepTimer(0)

    fun syncPosition() {
        val c = controller ?: return
        positionMs = c.currentPosition.coerceAtLeast(0L)
        durationMs = c.duration.coerceAtLeast(0L)
    }

    fun release() {
        mainHandler.removeCallbacks(sleepTicker)
        controller?.removeListener(listener)
        controller = null
        pendingAction = null
        MediaController.releaseFuture(controllerFuture)
    }

    private fun runOrQueue(action: (MediaController) -> Unit) {
        val c = controller
        if (c != null) action(c) else pendingAction = action
    }

    private fun applyPlayerModes(c: MediaController) {
        applyRepeat(c, repeatMode)
        c.setPlaybackSpeed(playbackSpeed)
    }

    private fun applyRepeat(c: MediaController, mode: RepeatMode) {
        when (mode) {
            RepeatMode.OFF -> c.repeatMode = Player.REPEAT_MODE_OFF
            RepeatMode.SURAH -> c.repeatMode = Player.REPEAT_MODE_ALL
            RepeatMode.AYAH -> c.repeatMode = Player.REPEAT_MODE_ONE
        }
    }

    private fun syncState() {
        val c = controller ?: return
        isPlaying = c.isPlaying
        isBuffering = c.playbackState == Player.STATE_BUFFERING
        durationMs = c.duration.coerceAtLeast(0L)
        positionMs = c.currentPosition.coerceAtLeast(0L)
        playlistSize = c.mediaItemCount
        repeatMode = when (c.repeatMode) {
            Player.REPEAT_MODE_ONE -> RepeatMode.AYAH
            Player.REPEAT_MODE_ALL -> RepeatMode.SURAH
            else -> RepeatMode.OFF
        }
        playbackSpeed = c.playbackParameters.speed

        val item = c.currentMediaItem
        currentTitle = item?.mediaMetadata?.title?.toString()
        val extras = item?.mediaMetadata?.extras
        activeSurahNumber = extras?.getInt(EXTRA_SURAH, 0) ?: 0
        activeSurahName = extras?.getString(EXTRA_SURAH_NAME)
        activeReciterId = extras?.getInt(EXTRA_RECITER, 0) ?: 0
        activeAyahNumber = extras?.getInt(EXTRA_AYAH, 0) ?: 0
    }

    private fun buildAyahItem(
        surahNumber: Int,
        surahName: String,
        reciterId: Int,
        reciterName: String,
        audio: AyahAudio,
        useVerifiedDownload: Boolean
    ): MediaItem {
        val extras = Bundle().apply {
            putInt(EXTRA_SURAH, surahNumber)
            putString(EXTRA_SURAH_NAME, surahName)
            putInt(EXTRA_RECITER, reciterId)
            putInt(EXTRA_AYAH, audio.ayahNumber)
            putString(EXTRA_VERSE_KEY, audio.verseKey)
        }
        val local = File(audioDir(appContext), "${reciterId}_${surahNumber}_${audio.ayahNumber}.mp3")
        val uri = if (useVerifiedDownload && local.exists() && local.length() > 1024) {
            Uri.fromFile(local)
        } else {
            Uri.parse(audio.url)
        }
        return MediaItem.Builder()
            .setUri(uri)
            .setMediaId("$surahNumber:$reciterId:${audio.ayahNumber}")
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle("$surahName $surahNumber:${audio.ayahNumber} · $reciterName")
                    .setArtist("Quriva · Murottal")
                    .setAlbumTitle("Surah $surahName")
                    .setExtras(extras)
                    .build()
            )
            .build()
    }

    companion object {
        const val EXTRA_SURAH = "surah_number"
        const val EXTRA_SURAH_NAME = "surah_name"
        const val EXTRA_RECITER = "reciter_id"
        const val EXTRA_AYAH = "ayah_number"
        const val EXTRA_VERSE_KEY = "verse_key"

        fun audioDir(context: Context): File =
            File(context.filesDir, "audio").apply { mkdirs() }

        fun downloadMarker(context: Context, surah: Int, reciter: Int): File =
            File(audioDir(context), "${reciter}_${surah}.complete")

        fun isSurahDownloaded(context: Context, surah: Int, reciter: Int, ayahCount: Int): Boolean {
            if (ayahCount <= 0) return false
            val dir = audioDir(context)
            val marker = downloadMarker(context, surah, reciter)
            val markedAyahCount = runCatching {
                marker.takeIf { it.exists() }?.readText()?.trim()?.toIntOrNull()
            }.getOrNull()
            if (markedAyahCount != ayahCount) return false
            var count = 0
            for (a in 1..ayahCount) {
                val f = File(dir, "${reciter}_${surah}_${a}.mp3")
                if (f.exists() && f.length() > 1024) count++
            }
            return count >= ayahCount
        }

        fun deleteSurahDownload(context: Context, surah: Int, reciter: Int) {
            val dir = audioDir(context)
            dir.listFiles { _, name ->
                name.startsWith("${reciter}_${surah}_") &&
                    (name.endsWith(".mp3") || name.endsWith(".part"))
            }?.forEach { it.delete() }
            val marker = downloadMarker(context, surah, reciter)
            marker.delete()
            File(marker.absolutePath + ".part").delete()
        }
    }
}

fun formatDuration(ms: Long): String {
    if (ms <= 0) return "0:00"
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val hours = minutes / 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes % 60, seconds)
    else "%d:%02d".format(minutes, seconds)
}
