package com.quriva.app.ui.screens

import android.os.StatFs
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.quriva.app.R
import com.quriva.app.data.AppPreferences
import com.quriva.app.data.AudioDownloadWorker
import com.quriva.app.data.QuranRepository
import com.quriva.app.data.Reciters
import com.quriva.app.data.SurahAudioPlayer
import com.quriva.app.model.SurahMeta
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

private enum class AudioFilter(val label: String) {
    ALL("Semua"),
    OFFLINE("Tersimpan"),
    QUEUE("Antrean")
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun AudioDownloadsScreen(prefs: AppPreferences, onBack: () -> Unit) {
    val context = LocalContext.current
    val surahs = remember { QuranRepository.getSurahs(context) }
    val juzs = remember { QuranRepository.getJuzs(context) }
    val workManager = remember(context) { WorkManager.getInstance(context) }
    val workInfos by remember(workManager) {
        workManager.getWorkInfosForUniqueWorkFlow(AudioDownloadWorker.WORK_NAME)
    }.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    var refreshKey by remember { mutableIntStateOf(0) }
    var downloaded by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var usedBytes by remember { mutableStateOf(0L) }
    var freeBytes by remember { mutableStateOf(0L) }
    var selectedJuz by remember { mutableIntStateOf(30) }
    var juzMenuOpen by remember { mutableStateOf(false) }
    var filter by remember { mutableStateOf(AudioFilter.ALL) }
    var query by remember { mutableStateOf("") }
    var selectionMode by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var confirmDelete by remember { mutableStateOf(false) }
    val colors = MaterialTheme.colorScheme

    val activeInfos = remember(workInfos) {
        workInfos.filter { info ->
            info.state == WorkInfo.State.ENQUEUED ||
                info.state == WorkInfo.State.RUNNING ||
                info.state == WorkInfo.State.BLOCKED
        }
    }
    val activeBySurah = remember(activeInfos) {
        activeInfos.mapNotNull { info ->
            AudioDownloadWorker.surahFromTags(info.tags).takeIf { it in 1..114 }?.let { it to info }
        }.toMap()
    }

    LaunchedEffect(refreshKey, prefs.reciterId, workInfos) {
        val snapshot = withContext(Dispatchers.IO) {
            val complete = surahs.asSequence()
                .filter { SurahAudioPlayer.isSurahDownloaded(context, it.number, prefs.reciterId, it.ayahCount) }
                .map { it.number }
                .toSet()
            val directory = SurahAudioPlayer.audioDir(context)
            val size = directory.walkTopDown().filter { it.isFile }.sumOf { it.length() }
            val free = runCatching { StatFs(context.filesDir.absolutePath).availableBytes }.getOrDefault(0L)
            Triple(complete, size, free)
        }
        downloaded = snapshot.first
        usedBytes = snapshot.second
        freeBytes = snapshot.third
    }

    val visible = remember(surahs, downloaded, activeBySurah, filter, query) {
        surahs.filter { surah ->
            val matchesFilter = when (filter) {
                AudioFilter.ALL -> true
                AudioFilter.OFFLINE -> surah.number in downloaded
                AudioFilter.QUEUE -> surah.number in activeBySurah
            }
            val matchesQuery = query.isBlank() ||
                surah.latinName.contains(query.trim(), ignoreCase = true) ||
                surah.number.toString() == query.trim()
            matchesFilter && matchesQuery
        }
    }

    fun enqueue(surah: SurahMeta) {
        if (surah.number in activeBySurah || surah.number in downloaded) return
        AudioDownloadWorker.enqueue(
            context = context,
            surah = surah.number,
            name = surah.latinName,
            count = surah.ayahCount,
            reciter = prefs.reciterId,
            wifiOnly = prefs.audioWifiOnly
        )
        scope.launch { snackbar.showSnackbar("${surah.latinName} ditambahkan ke antrean") }
    }

    Surface(Modifier.fillMaxSize(), color = colors.background) {
        Scaffold(
            modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing),
            containerColor = colors.background,
            snackbarHost = { SnackbarHost(snackbar) },
            topBar = {
                FeatureTopBar(
                    title = "Audio Offline",
                    subtitle = "Kelola unduhan murottal",
                    onBack = onBack
                )
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(top = padding.calculateTopPadding()),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 34.dp)
            ) {
                item(key = "summary") {
                    AudioStorageHero(
                        downloadedCount = downloaded.size,
                        usedBytes = usedBytes,
                        freeBytes = freeBytes,
                        activeCount = activeInfos.size,
                        reciter = Reciters.byId(prefs.reciterId).name
                    )
                    Spacer(Modifier.height(14.dp))
                }

                item(key = "queue-controls") {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = colors.surfaceVariant,
                        border = BorderStroke(1.dp, colors.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier.size(40.dp).background(colors.background, RoundedCornerShape(13.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(painterResource(R.drawable.ic_headphones), null, Modifier.size(19.dp))
                                }
                                Spacer(Modifier.width(11.dp))
                                Column(Modifier.weight(1f)) {
                                    Text("Unduh satu juz", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                    Text("Surah terkait ditambahkan berurutan", style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                                }
                            }
                            Spacer(Modifier.height(14.dp))
                            ExposedDropdownMenuBox(
                                expanded = juzMenuOpen,
                                onExpandedChange = { juzMenuOpen = it }
                            ) {
                                OutlinedTextField(
                                    value = "Juz $selectedJuz",
                                    onValueChange = {},
                                    readOnly = true,
                                    singleLine = true,
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(juzMenuOpen) },
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.menuAnchor().fillMaxWidth()
                                )
                                ExposedDropdownMenu(
                                    expanded = juzMenuOpen,
                                    onDismissRequest = { juzMenuOpen = false }
                                ) {
                                    juzs.forEach { juz ->
                                        DropdownMenuItem(
                                            text = { Text("Juz ${juz.number}") },
                                            onClick = {
                                                selectedJuz = juz.number
                                                juzMenuOpen = false
                                            }
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.height(9.dp))
                            Button(
                                onClick = {
                                    val juz = juzs.first { it.number == selectedJuz }
                                    val targets = surahs.filter { it.number in juz.startSurah..juz.endSurah }
                                        .filterNot { it.number in activeBySurah || it.number in downloaded }
                                    targets.forEach(::enqueue)
                                    if (targets.isEmpty()) {
                                        scope.launch { snackbar.showSnackbar("Semua surah Juz $selectedJuz sudah tersimpan atau sedang diantrekan") }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(50.dp),
                                shape = CircleShape
                            ) {
                                Icon(painterResource(R.drawable.ic_headphones), null, Modifier.size(17.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Tambahkan Juz $selectedJuz")
                            }
                            Spacer(Modifier.height(10.dp))
                            HorizontalDivider(color = colors.outlineVariant)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text("Hanya melalui Wi-Fi", style = MaterialTheme.typography.labelLarge)
                                    Text("Menghemat kuota seluler", style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                                }
                                Switch(prefs.audioWifiOnly, prefs::updateAudioWifiOnly)
                            }
                            AnimatedVisibility(activeInfos.isNotEmpty()) {
                                TextButton(
                                    onClick = {
                                        AudioDownloadWorker.cancelAll(context)
                                        scope.launch { snackbar.showSnackbar("Antrean unduhan dibatalkan") }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Batalkan ${activeInfos.size} antrean")
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                }

                item(key = "library-heading") {
                    Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.weight(1f)) {
                            Text("Pustaka Audio", style = MaterialTheme.typography.headlineSmall)
                            Text("Unduh per surah untuk didengar tanpa internet", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                        }
                        if (downloaded.isNotEmpty()) {
                            TextButton(onClick = {
                                selectionMode = !selectionMode
                                if (!selectionMode) selected = emptySet()
                            }) {
                                Text(if (selectionMode) "Selesai" else "Pilih")
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = { Text("Cari nama atau nomor surah") },
                        leadingIcon = { Icon(painterResource(R.drawable.ic_search), null, Modifier.size(19.dp)) },
                        singleLine = true,
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        AudioFilter.entries.forEach { item ->
                            FilterChip(
                                selected = filter == item,
                                onClick = { filter = item },
                                label = { Text(item.label) }
                            )
                        }
                    }
                    AnimatedVisibility(selectionMode) {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = colors.inverseSurface,
                            contentColor = colors.inverseOnSurface,
                            modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
                        ) {
                            Row(Modifier.padding(start = 15.dp, end = 8.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("${selected.size} dipilih", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                                TextButton(onClick = {
                                    selected = if (selected.size == downloaded.size) emptySet() else downloaded
                                }) { Text(if (selected.size == downloaded.size) "Batal pilih" else "Pilih semua", color = colors.inverseOnSurface) }
                                TextButton(onClick = { if (selected.isNotEmpty()) confirmDelete = true }) {
                                    Text("Hapus", color = colors.inverseOnSurface)
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                }

                if (visible.isEmpty()) {
                    item(key = "empty") {
                        AudioEmptyState(filter = filter, query = query)
                    }
                } else {
                    items(visible, key = { it.number }) { surah ->
                        val info = activeBySurah[surah.number]
                        AudioSurahCard(
                            surah = surah,
                            downloaded = surah.number in downloaded,
                            workInfo = info,
                            selectionMode = selectionMode,
                            selected = surah.number in selected,
                            onSelect = {
                                if (surah.number in downloaded) {
                                    selected = if (surah.number in selected) selected - surah.number else selected + surah.number
                                }
                            },
                            onDownload = { enqueue(surah) },
                            onDelete = {
                                SurahAudioPlayer.deleteSurahDownload(context, surah.number, prefs.reciterId)
                                refreshKey++
                                scope.launch { snackbar.showSnackbar("Audio ${surah.latinName} dihapus") }
                            }
                        )
                        Spacer(Modifier.height(9.dp))
                    }
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Hapus audio terpilih?") },
            text = { Text("${selected.size} surah akan dihapus dari penyimpanan perangkat.") },
            confirmButton = {
                Button(onClick = {
                    selected.forEach { SurahAudioPlayer.deleteSurahDownload(context, it, prefs.reciterId) }
                    val count = selected.size
                    selected = emptySet()
                    selectionMode = false
                    confirmDelete = false
                    refreshKey++
                    scope.launch { snackbar.showSnackbar("$count audio surah dihapus") }
                }) { Text("Hapus") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Batal") } }
        )
    }
}

@Composable
private fun AudioStorageHero(
    downloadedCount: Int,
    usedBytes: Long,
    freeBytes: Long,
    activeCount: Int,
    reciter: String
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = colors.inverseSurface,
        contentColor = colors.inverseOnSurface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier.size(170.dp).align(Alignment.TopEnd).background(
                    androidx.compose.ui.graphics.Brush.radialGradient(
                        listOf(colors.inverseOnSurface.copy(alpha = .13f), Color.Transparent)
                    ),
                    CircleShape
                )
            )
            Column(Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(43.dp).background(colors.inverseOnSurface.copy(alpha = .12f), RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(painterResource(R.drawable.ic_headphones), null, Modifier.size(21.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Audio siap offline", style = MaterialTheme.typography.labelSmall, color = colors.inverseOnSurface.copy(alpha = .65f))
                        Text("$downloadedCount surah", style = MaterialTheme.typography.headlineMedium)
                    }
                    if (activeCount > 0) {
                        Surface(shape = CircleShape, color = colors.inverseOnSurface.copy(alpha = .12f)) {
                            Text("$activeCount antre", Modifier.padding(horizontal = 10.dp, vertical = 6.dp), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                Spacer(Modifier.height(18.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AudioMetric("Terpakai", formatStorage(usedBytes), Modifier.weight(1f))
                    AudioMetric("Ruang kosong", formatStorage(freeBytes), Modifier.weight(1f))
                }
                Spacer(Modifier.height(12.dp))
                Text("Qari · $reciter", style = MaterialTheme.typography.labelSmall, color = colors.inverseOnSurface.copy(alpha = .62f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun AudioMetric(label: String, value: String, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Surface(modifier = modifier, shape = RoundedCornerShape(15.dp), color = colors.inverseOnSurface.copy(alpha = .1f)) {
        Column(Modifier.padding(11.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = colors.inverseOnSurface.copy(alpha = .62f))
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun AudioSurahCard(
    surah: SurahMeta,
    downloaded: Boolean,
    workInfo: WorkInfo?,
    selectionMode: Boolean,
    selected: Boolean,
    onSelect: () -> Unit,
    onDownload: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val progress = workInfo?.progress?.getInt("progress", 0) ?: 0
    val running = workInfo?.state == WorkInfo.State.RUNNING
    val waiting = workInfo != null && !running
    Surface(
        onClick = {
            if (selectionMode) onSelect()
            else if (!downloaded && workInfo == null) onDownload()
        },
        shape = RoundedCornerShape(20.dp),
        color = if (selected) colors.inverseSurface else colors.surfaceVariant,
        contentColor = if (selected) colors.inverseOnSurface else colors.onBackground,
        border = if (selected) null else BorderStroke(1.dp, colors.outlineVariant),
        modifier = Modifier.fillMaxWidth().animateContentSize()
    ) {
        Column {
            Row(Modifier.padding(start = 14.dp, end = 7.dp, top = 12.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(43.dp).background(
                        if (selected) colors.inverseOnSurface.copy(alpha = .13f) else colors.background,
                        RoundedCornerShape(14.dp)
                    ),
                    contentAlignment = Alignment.Center
                ) {
                    if (selected) {
                        Icon(painterResource(R.drawable.ic_check), null, Modifier.size(18.dp))
                    } else {
                        Text(surah.number.toString(), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(surah.latinName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        when {
                            downloaded -> "${surah.ayahCount} ayat · tersedia offline"
                            running -> "Mengunduh · $progress%"
                            waiting -> "Menunggu giliran"
                            else -> "${surah.ayahCount} ayat · belum diunduh"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (selected) colors.inverseOnSurface.copy(alpha = .64f) else colors.onSurfaceVariant
                    )
                }
                if (!selectionMode) {
                    when {
                        downloaded -> IconButton(onClick = onDelete, modifier = Modifier.size(48.dp)) {
                            Icon(painterResource(R.drawable.ic_delete), "Hapus audio", Modifier.size(18.dp), tint = colors.onSurfaceVariant)
                        }
                        workInfo != null -> Surface(shape = CircleShape, color = colors.background) {
                            Text(
                                if (running) "$progress%" else "Antre",
                                Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                        else -> IconButton(onClick = onDownload, modifier = Modifier.size(48.dp)) {
                            Icon(painterResource(R.drawable.ic_headphones), "Unduh ${surah.latinName}", Modifier.size(19.dp))
                        }
                    }
                }
            }
            if (workInfo != null) {
                LinearProgressIndicator(
                    progress = { if (running) progress / 100f else 0f },
                    modifier = Modifier.fillMaxWidth().height(3.dp),
                    color = colors.onBackground,
                    trackColor = colors.outlineVariant
                )
            }
        }
    }
}

@Composable
private fun AudioEmptyState(filter: AudioFilter, query: String) {
    val colors = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth().padding(vertical = 42.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(painterResource(R.drawable.ic_headphones), null, Modifier.size(30.dp), tint = colors.onSurfaceVariant)
        Spacer(Modifier.height(10.dp))
        Text(
            when {
                query.isNotBlank() -> "Surah tidak ditemukan"
                filter == AudioFilter.OFFLINE -> "Belum ada audio tersimpan"
                filter == AudioFilter.QUEUE -> "Antrean masih kosong"
                else -> "Tidak ada audio"
            },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            if (filter == AudioFilter.QUEUE) "Pilih surah untuk mulai mengunduh." else "Coba ubah pencarian atau filter.",
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant
        )
    }
}

private fun formatStorage(bytes: Long): String = when {
    bytes >= 1024L * 1024L * 1024L -> "%.1f GB".format(Locale.US, bytes / 1_073_741_824f)
    bytes >= 1024L * 1024L -> "%.1f MB".format(Locale.US, bytes / 1_048_576f)
    bytes >= 1024L -> "${bytes / 1024} KB"
    else -> "$bytes B"
}
