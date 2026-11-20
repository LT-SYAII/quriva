package com.quriva.app.ui

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
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
                val list = QuranApi.ayahAudioList(reciter.id, surahNumber)
                if (list.isEmpty()) {
                    // fallback to chapter audio
                    val url = QuranApi.chapterAudioUrl(reciter.id, surahNumber)
                    player.playChapterUrl(surahNumber, surahName, reciter.id, reciter.name, url)
                } else {
                    player.playSurah(surahNumber, surahName, reciter.id, reciter.name, list, fromAyah, positionMs)
                }
                player.setRepeat(RepeatMode.valueOf(prefs.repeatMode.ifBlank { "OFF" }))
                player.setSpeed(prefs.playbackSpeed)
                prefs.updateReciter(reciter.id)
                prefs.saveLastAudio(surahNumber, fromAyah, reciter.id, 0L)
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
                    modifier = Modifier.fillMaxWidth(),
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
                                Icon(painter = painterResource(R.drawable.ic_arrow_forward), contentDescription = "Sebelumnya", tint = colors.onBackground, modifier = Modifier.size(20.dp))
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
                        if (shownError != null) {
                            Spacer(Modifier.height(8.dp))
                            Text(shownError, style = MaterialTheme.typography.labelSmall, color = colors.onBackground)
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
                if (downloading && downloadProgress != null) {
                    Text("Mengunduh ${downloadProgress!!.first}/${downloadProgress!!.second} ayat...", style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                    Spacer(Modifier.height(6.dp))
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

            items(Reciters.all, key = { it.id }, contentType = { "reciter" }) { reciter ->
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 9.dp),
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
    val items = if (list.isNotEmpty()) list else (1..ayahCount).map { AyahAudio(it, "$surah:$it", "") }
    val dir = SurahAudioPlayer.audioDir(context)
    withContext(Dispatchers.IO) {
        items.forEachIndexed { idx, a ->
            onProgress(idx + 1, items.size)
            val out = File(dir, "${reciter}_${surah}_${a.ayahNumber}.mp3")
            if (out.exists() && out.length() > 1024) return@forEachIndexed
            if (a.url.isBlank()) return@forEachIndexed
            val conn = (URL(a.url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 15000
                readTimeout = 30000
                setRequestProperty("User-Agent", "Quriva/1.4 (Android)")
            }
            try {
                if (conn.responseCode in 200..299) {
                    conn.inputStream.use { input ->
                        FileOutputStream(out).use { output -> input.copyTo(output) }
                    }
                }
            } finally {
                conn.disconnect()
            }
        }
    }
}
