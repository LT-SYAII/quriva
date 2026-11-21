package com.quriva.app.data

import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.quriva.app.MainActivity
import com.quriva.app.R
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Keeps murottal playing while Quriva is in the background or the screen is off.
 *
 * Media3 owns the ExoPlayer instance here, outside the Compose UI, and exposes
 * it through a MediaSession so the system notification, Bluetooth controls, and
 * headset buttons can control playback.
 */
@OptIn(markerClass = [UnstableApi::class])
class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val preferences by lazy { AppPreferences(applicationContext) }
    private var autoAdvanceInProgress = false

    override fun onCreate() {
        super.onCreate()

        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                /* handleAudioFocus = */ true
            )
            .setHandleAudioBecomingNoisy(true)
            .build()
            .apply {
                setWakeMode(C.WAKE_MODE_NETWORK)
                setSeekBackIncrementMs(SEEK_INCREMENT_MS)
                setSeekForwardIncrementMs(SEEK_INCREMENT_MS)
                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        if (playbackState == Player.STATE_ENDED) {
                            continueWithNextSurah(this@apply)
                        }
                    }
                })
            }

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(openAppPendingIntent())
            .build()

        val notificationProvider = DefaultMediaNotificationProvider.Builder(this)
            .setChannelId(CHANNEL_ID)
            .setChannelName(R.string.playback_channel_name)
            .build()
            .apply { setSmallIcon(R.drawable.ic_notification) }
        setMediaNotificationProvider(notificationProvider)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        val shouldStop = player == null ||
            (!autoAdvanceInProgress &&
                (
                !player.playWhenReady ||
                    player.mediaItemCount == 0 ||
                    player.playbackState == Player.STATE_ENDED
                )
            )
        if (shouldStop) stopSelf()
    }

    override fun onDestroy() {
        serviceScope.cancel()
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
    }

    private fun continueWithNextSurah(player: Player) {
        if (player.repeatMode != Player.REPEAT_MODE_OFF || autoAdvanceInProgress) return
        val extras = player.currentMediaItem?.mediaMetadata?.extras ?: return
        val surah = extras.getInt(SurahAudioPlayer.EXTRA_SURAH, 0)
        val reciter = extras.getInt(SurahAudioPlayer.EXTRA_RECITER, 0)
        if (surah !in 1..113 || reciter <= 0) return

        autoAdvanceInProgress = true
        serviceScope.launch {
            try {
                val nextSurah = surah + 1
                val meta = withContext(Dispatchers.IO) {
                    QuranRepository.getSurahs(this@PlaybackService)
                        .firstOrNull { it.number == nextSurah }
                } ?: return@launch
                val reciterName = Reciters.byId(reciter).name
                val useVerifiedDownload = withContext(Dispatchers.IO) {
                    SurahAudioPlayer.isSurahDownloaded(
                        this@PlaybackService,
                        nextSurah,
                        reciter,
                        meta.ayahCount
                    )
                }
                val ayahAudio = if (useVerifiedDownload) {
                    (1..meta.ayahCount).map { ayah ->
                        AyahAudio(ayah, "$nextSurah:$ayah", "")
                    }
                } else {
                    try {
                        QuranApi.ayahAudioList(reciter, nextSurah)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Throwable) {
                        emptyList()
                    }
                }

                val currentExtras = player.currentMediaItem?.mediaMetadata?.extras
                val stillWaitingForNext = player.playbackState == Player.STATE_ENDED &&
                    player.repeatMode == Player.REPEAT_MODE_OFF &&
                    currentExtras?.getInt(SurahAudioPlayer.EXTRA_SURAH, 0) == surah
                if (!stillWaitingForNext) return@launch

                if (ayahAudio.isNotEmpty()) {
                    val items = ayahAudio.map { audio ->
                        buildAyahItem(
                            surahNumber = nextSurah,
                            surahName = meta.latinName,
                            reciterId = reciter,
                            reciterName = reciterName,
                            audio = audio,
                            useVerifiedDownload = useVerifiedDownload
                        )
                    }
                    player.setMediaItems(items, true)
                } else {
                    val url = QuranApi.chapterAudioUrl(reciter, nextSurah)
                    player.setMediaItem(
                        buildChapterItem(nextSurah, meta.latinName, reciter, reciterName, url)
                    )
                }
                player.prepare()
                player.play()
                preferences.saveLastAudio(nextSurah, 1, reciter, 0L)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Throwable) {
                // Keep the completed surah visible; the user can retry manually.
            } finally {
                autoAdvanceInProgress = false
            }
        }
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
            putInt(SurahAudioPlayer.EXTRA_SURAH, surahNumber)
            putString(SurahAudioPlayer.EXTRA_SURAH_NAME, surahName)
            putInt(SurahAudioPlayer.EXTRA_RECITER, reciterId)
            putInt(SurahAudioPlayer.EXTRA_AYAH, audio.ayahNumber)
            putString(SurahAudioPlayer.EXTRA_VERSE_KEY, audio.verseKey)
        }
        val local = File(
            SurahAudioPlayer.audioDir(this),
            "${reciterId}_${surahNumber}_${audio.ayahNumber}.mp3"
        )
        val uri = if (useVerifiedDownload && local.exists() && local.length() > 1024L) {
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

    private fun buildChapterItem(
        surahNumber: Int,
        surahName: String,
        reciterId: Int,
        reciterName: String,
        url: String
    ): MediaItem {
        val extras = Bundle().apply {
            putInt(SurahAudioPlayer.EXTRA_SURAH, surahNumber)
            putString(SurahAudioPlayer.EXTRA_SURAH_NAME, surahName)
            putInt(SurahAudioPlayer.EXTRA_RECITER, reciterId)
            putInt(SurahAudioPlayer.EXTRA_AYAH, 0)
        }
        return MediaItem.Builder()
            .setUri(url)
            .setMediaId("$surahNumber:$reciterId:0")
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle("$surahName · $reciterName")
                    .setArtist("Quriva · Murottal")
                    .setAlbumTitle("Surah $surahName")
                    .setExtras(extras)
                    .build()
            )
            .build()
    }

    private fun openAppPendingIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        const val CHANNEL_ID = "quriva_playback"
        private const val SEEK_INCREMENT_MS = 10_000L
    }
}
