package com.quriva.app.ui

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.quriva.app.R
import com.quriva.app.data.AppPreferences
import com.quriva.app.data.AyahAudio
import com.quriva.app.data.QuranApi
import com.quriva.app.data.Reciter
import com.quriva.app.data.Reciters
import com.quriva.app.data.RepeatMode
import com.quriva.app.data.SurahAudioPlayer
import com.quriva.app.data.formatDuration
import com.quriva.app.ui.AnimatedListItem
import com.quriva.app.ui.interactiveClickable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioSheet(
    surahNumber: Int,
    surahName: String,
    ayahCount: Int,
    player: SurahAudioPlayer,
    prefs: AppPreferences,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val colors = MaterialTheme.colorScheme

    var selectedId by rememberSaveable(surahNumber) {
        mutableIntStateOf(
            player.activeReciterId.takeIf { player.activeSurahNumber == surahNumber && it > 0 }
                ?: prefs.reciterId
        )
    }
    var loading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var downloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    val selected = Reciters.byId(selectedId)
    val isActive = player.activeSurahNumber == surahNumber && player.activeReciterId == selectedId
    val activeTitle = if (isActive && player.activeAyahNumber > 0) {
        "$surahName ${surahNumber}:${player.activeAyahNumber} · ${selected.name}"
    } else {
        "$surahName · ${selected.name}"
    }

    var isDownloaded by remember(selectedId, surahNumber) {
        mutableStateOf(SurahAudioPlayer.isSurahDownloaded(context, surahNumber, selectedId, ayahCount))
    }

    fun startPlayback(fromAyah: Int = 1, positionMs: Long = 0L) {
        if (loading) return
        val reciter = selected
        scope.launch {
            loading = true
            errorMessage = null
            try {
                val list = if (isDownloaded) {
                    (1..ayahCount).map { ayah ->
                        AyahAudio(ayah, "$surahNumber:$ayah", "")
                    }
                } else {
                    QuranApi.ayahAudioList(reciter.id, surahNumber)
                }
                if (list.isEmpty()) {
                    // fallback to chapter audio
                    val url = QuranApi.chapterAudioUrl(reciter.id, surahNumber)
                    player.playChapterUrl(surahNumber, surahName, reciter.id, reciter.name, url)
                } else {
                    player.playSurah(
                        surahNumber,
                        surahName,
                        reciter.id,
                        reciter.name,
                        list,
                        fromAyah,
                        positionMs,
                        isDownloaded
                    )
                }
                player.setRepeat(RepeatMode.valueOf(prefs.repeatMode.ifBlank { "OFF" }))
                player.setSpeed(prefs.playbackSpeed)
                player.setSleepTimer(prefs.sleepTimerMinutes)
                prefs.updateReciter(reciter.id)
                prefs.saveLastAudio(surahNumber, fromAyah, reciter.id, 0L)
            } catch (e: CancellationException) {
                throw e
            } catch (t: Throwable) {
                errorMessage = QuranApi.friendlyNetworkError(t)
            } finally {
                loading = false
            }
        }
    }

    fun resumeLast() {
        if (prefs.lastAudioSurah == surahNumber && prefs.lastAudioReciter == selectedId &&
            prefs.lastAudioAyah > 0
        ) {
            startPlayback(prefs.lastAudioAyah, prefs.lastAudioPositionMs)
        } else {
            startPlayback(1, 0L)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = { BottomSheetDefaults.DragHandle(color = colors.outline) }
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Murottal", style = MaterialTheme.typography.headlineSmall, color = colors.onBackground)
                        Text("Surah $surahName · $ayahCount ayat", style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(painter = painterResource(R.drawable.ic_close), contentDescription = "Tutup", tint = colors.onBackground)
                    }
                }
                Spacer(Modifier.height(14.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth().animateContentSize(),
                    shape = RoundedCornerShape(24.dp),
                    color = colors.surfaceVariant,
                    border = BorderStroke(1.dp, colors.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            activeTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.onBackground,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            when {
                                loading -> "Menyiapkan audio per ayat..."
                                isActive && player.isBuffering -> "Memuat..."
                                isActive && player.isPlaying -> "Sedang diputar${if (player.activeAyahNumber > 0) " · ayat ${player.activeAyahNumber}" else ""}"
                                isActive -> "Dijeda"
                                else -> "${selected.style} · siap diputar"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.onSurfaceVariant
                        )

                        Spacer(Modifier.height(10.dp))
                        AudioProgress(player = player, isActive = isActive)

                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { if (isActive) player.previous() }, enabled = isActive && player.activeAyahNumber > 1) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_arrow_forward),
                                    contentDescription = "Sebelumnya",
                                    tint = colors.onBackground,
                                    modifier = Modifier.size(20.dp).graphicsLayer { rotationZ = 180f }
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Button(
                                onClick = { if (isActive) player.toggle() else resumeLast() },
                                shape = CircleShape,
                                colors = ButtonDefaults.buttonColors(containerColor = colors.onBackground, contentColor = colors.background)
                            ) {
                                if (loading || (isActive && player.isBuffering)) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = colors.background)
                                } else {
                                    Icon(
                                        painter = painterResource(if (isActive && player.isPlaying) R.drawable.ic_pause else R.drawable.ic_play),
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(Modifier.width(8.dp))
                                Text(if (isActive && player.isPlaying) "Jeda" else if (isActive) "Lanjutkan" else "Putar")
                            }
                            Spacer(Modifier.width(8.dp))
                            IconButton(onClick = { if (isActive) player.next() }, enabled = isActive && player.activeAyahNumber in 1 until ayahCount) {
                                Icon(painter = painterResource(R.drawable.ic_arrow_forward), contentDescription = "Berikutnya", tint = colors.onBackground, modifier = Modifier.size(20.dp))
                            }
                        }

                        val shownError = errorMessage ?: if (isActive) player.errorMessage else null
                        AnimatedVisibility(visible = shownError != null) {
                            Column {
                                Spacer(Modifier.height(8.dp))
                                Text(shownError.orEmpty(), style = MaterialTheme.typography.labelSmall, color = colors.onBackground)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))
                Text("Mode & kontrol", style = MaterialTheme.typography.labelLarge, color = colors.onBackground)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                    RepeatMode.entries.forEach { mode ->
                        val selectedMode = (if (isActive) player.repeatMode else RepeatMode.valueOf(prefs.repeatMode.ifBlank { "OFF" })) == mode
                        FilterChip(
                            selected = selectedMode,
                            onClick = {
                                prefs.updateRepeatMode(mode.name)
                                if (isActive) player.setRepeat(mode)
                            },
                            label = { Text("Ulang: ${mode.label}") }
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text("Kecepatan ${"%.2f".format(prefs.playbackSpeed)}x", style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                Slider(
                    value = prefs.playbackSpeed,
                    onValueChange = {
                        prefs.updatePlaybackSpeed(it)
                        if (isActive) player.setSpeed(it)
                    },
                    valueRange = 0.5f..1.75f,
                    steps = 4,
                    colors = SliderDefaults.colors(thumbColor = colors.onBackground, activeTrackColor = colors.onBackground, inactiveTrackColor = colors.outline)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                    listOf(0, 10, 20, 30, 60).forEach { mins ->
                        val label = if (mins == 0) "Timer mati" else "Tidur ${mins}m"
                        val sel = prefs.sleepTimerMinutes == mins
                        FilterChip(
                            selected = sel,
                            onClick = {
                                prefs.updateSleepTimerMinutes(mins)
                                if (isActive) player.setSleepTimer(mins)
                            },
                            label = { Text(label) }
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Unduh untuk offline", style = MaterialTheme.typography.labelLarge, color = colors.onBackground, modifier = Modifier.weight(1f))
                    if (isDownloaded) {
                        Text("Tersedia", style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.height(6.dp))
                AnimatedVisibility(visible = downloading && downloadProgress != null) {
                    val progress = downloadProgress
                    if (progress != null) {
                        Column {
                            Text(
                                "Mengunduh ${progress.first}/${progress.second} ayat...",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.onSurfaceVariant
                            )
                            Spacer(Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = {
                                    if (progress.second > 0) {
                                        progress.first.toFloat() / progress.second.toFloat()
                                    } else 0f
                                },
                                modifier = Modifier.fillMaxWidth().height(3.dp),
                                color = colors.onBackground,
                                trackColor = colors.outlineVariant
                            )
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            if (downloading) return@Button
                            scope.launch {
                                downloading = true
                                try {
                                    downloadSurahAudio(context, surahNumber, selectedId, ayahCount) { d, t ->
                                        downloadProgress = d to t
                                    }
                                    isDownloaded = SurahAudioPlayer.isSurahDownloaded(context, surahNumber, selectedId, ayahCount)
                                } catch (e: CancellationException) {
                                    throw e
                                } catch (t: Throwable) {
                                    errorMessage = QuranApi.friendlyNetworkError(t)
                                } finally {
                                    downloading = false
                                    downloadProgress = null
                                }
                            }
                        },
                        enabled = !downloading && !isDownloaded,
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = colors.onBackground, contentColor = colors.background)
                    ) { Text(if (isDownloaded) "Sudah diunduh" else "Unduh surah") }
                    if (isDownloaded) {
                        androidx.compose.material3.OutlinedButton(
                            onClick = {
                                SurahAudioPlayer.deleteSurahDownload(context, surahNumber, selectedId)
                                isDownloaded = false
                            },
                            shape = CircleShape
                        ) { Text("Hapus") }
                    }
                }

                Spacer(Modifier.height(18.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Pilih qari", style = MaterialTheme.typography.labelLarge, color = colors.onBackground, modifier = Modifier.weight(1f))
                    Text("${Reciters.all.size} pilihan", style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                }
                Spacer(Modifier.height(6.dp))
            }

            itemsIndexed(Reciters.all, key = { _, reciter -> reciter.id }, contentType = { _, _ -> "reciter" }) { index, reciter ->
                AnimatedListItem(index = index) {
                    ReciterRow(
                        reciter = reciter,
                        selected = reciter.id == selectedId,
                        onClick = {
                            selectedId = reciter.id
                            prefs.updateReciter(reciter.id)
                            errorMessage = null
                        }
                    )
                }
            }

            item {
                Spacer(Modifier.height(10.dp))
                Text(
                    "Audio per ayat mendukung sorot & lompat ayat, pemutaran latar belakang, dan unduhan offline per surah. Koneksi internet diperlukan untuk streaming.",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AudioProgress(player: SurahAudioPlayer, isActive: Boolean) {
    val colors = MaterialTheme.colorScheme
    var seeking by remember { mutableStateOf<Float?>(null) }
    val duration = if (isActive) player.durationMs else 0L
    val position = if (isActive) player.positionMs else 0L
    val fraction = if (duration > 0) position.toFloat() / duration.toFloat() else 0f
    val sliderValue = seeking ?: fraction.coerceIn(0f, 1f)

    LaunchedEffect(isActive, player.isPlaying) {
        if (isActive) {
            while (player.isPlaying) {
                player.syncPosition()
                delay(1_000)
            }
            player.syncPosition()
        }
    }

    Slider(
        value = sliderValue,
        onValueChange = { seeking = it },
        onValueChangeFinished = {
            seeking?.let(player::seekToFraction)
            seeking = null
        },
        enabled = isActive && duration > 0,
        colors = SliderDefaults.colors(
            thumbColor = colors.onBackground,
            activeTrackColor = colors.onBackground,
            inactiveTrackColor = colors.outline
        )
    )
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(formatDuration(position), style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
        Spacer(Modifier.weight(1f))
        Text(formatDuration(duration), style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
    }
}

@Composable
private fun ReciterRow(reciter: Reciter, selected: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val background by animateColorAsState(
        targetValue = if (selected) colors.surfaceVariant else Color.Transparent,
        animationSpec = tween(220),
        label = "reciterBackground"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .interactiveClickable(onClick = onClick)
            .background(background, RoundedCornerShape(14.dp))
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(reciter.name, style = MaterialTheme.typography.bodyMedium, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal, color = colors.onBackground)
            Text(reciter.style, style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
        }
        if (selected) {
            Surface(shape = CircleShape, color = colors.onBackground) {
                Icon(painter = painterResource(R.drawable.ic_check), contentDescription = "Dipilih", tint = colors.background, modifier = Modifier.padding(5.dp).size(12.dp))
            }
        } else {
            Surface(modifier = Modifier.size(22.dp), shape = CircleShape, color = Color.Transparent, border = BorderStroke(1.dp, colors.outline)) {}
        }
    }
}

private suspend fun downloadSurahAudio(
    context: Context,
    surah: Int,
    reciter: Int,
    ayahCount: Int,
    onProgress: (Int, Int) -> Unit
) {
    val list = QuranApi.ayahAudioList(reciter, surah)
    val byAyah = list.associateBy { it.ayahNumber }
    val items = (1..ayahCount).map { ayah ->
        byAyah[ayah] ?: throw IOException("Audio ayat $surah:$ayah belum tersedia.")
    }
    val dir = SurahAudioPlayer.audioDir(context)
    withContext(Dispatchers.IO) {
        SurahAudioPlayer.deleteSurahDownload(context, surah, reciter)
        val marker = SurahAudioPlayer.downloadMarker(context, surah, reciter)
        val markerTemp = File(marker.absolutePath + ".part")
        try {
            withContext(Dispatchers.Main.immediate) { onProgress(0, items.size) }
            items.forEachIndexed { idx, a ->
                val out = File(dir, "${reciter}_${surah}_${a.ayahNumber}.mp3")
                val temp = File(out.absolutePath + ".part")
                temp.delete()
                val conn = (URL(a.url).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 15_000
                    readTimeout = 45_000
                    instanceFollowRedirects = true
                    setRequestProperty("User-Agent", "Quriva/2.0.0 (Android)")
                }
                try {
                    val code = conn.responseCode
                    if (code !in 200..299) {
                        throw IOException("Server audio mengembalikan kode $code.")
                    }
                    conn.inputStream.use { input ->
                        FileOutputStream(temp).use { output ->
                            val buffer = ByteArray(16 * 1024)
                            while (true) {
                                currentCoroutineContext().ensureActive()
                                val read = input.read(buffer)
                                if (read < 0) break
                                output.write(buffer, 0, read)
                            }
                        }
                    }
                    val expected = conn.contentLengthLong
                    if (temp.length() <= 1024L || (expected > 0L && temp.length() != expected)) {
                        throw IOException("Berkas audio ayat ${a.verseKey} tidak lengkap.")
                    }
                    if (!temp.renameTo(out)) {
                        throw IOException("Audio tidak dapat disimpan dengan aman.")
                    }
                } finally {
                    conn.disconnect()
                    temp.delete()
                }
                withContext(Dispatchers.Main.immediate) { onProgress(idx + 1, items.size) }
            }
            markerTemp.writeText(ayahCount.toString())
            if (!markerTemp.renameTo(marker)) {
                throw IOException("Status unduhan tidak dapat disimpan.")
            }
        } catch (t: Throwable) {
            SurahAudioPlayer.deleteSurahDownload(context, surah, reciter)
            throw t
        } finally {
            markerTemp.delete()
        }
    }
}
