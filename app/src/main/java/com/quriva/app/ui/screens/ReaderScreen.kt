package com.quriva.app.ui.screens

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quriva.app.R
import com.quriva.app.data.AppPreferences
import com.quriva.app.data.QuranApi
import com.quriva.app.data.RepeatMode
import com.quriva.app.data.SurahAudioPlayer
import com.quriva.app.data.WordItem
import com.quriva.app.model.Ayah
import com.quriva.app.model.SurahDetail
import com.quriva.app.model.SurahMeta
import com.quriva.app.ui.AudioSheet
import com.quriva.app.ui.ReaderUiState
import com.quriva.app.ui.ReaderViewModel
import com.quriva.app.ui.ReaderViewModelFactory
import com.quriva.app.ui.TafsirSheet
import com.quriva.app.ui.theme.ArabicFontFamily
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextButton
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import com.quriva.app.ui.parseTajweed
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private const val BISMILLAH = "بِسْمِ ٱللَّهِ ٱلرَّحْمَـٰنِ ٱلرَّحِيمِ"

@Composable
fun ReaderScreen(
    surahNumber: Int,
    initialAyah: Int,
    prefs: AppPreferences,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val context = LocalContext.current
    val application = context.applicationContext as Application
    val viewModel: ReaderViewModel = viewModel(
        key = "reader-$surahNumber",
        factory = ReaderViewModelFactory(application, surahNumber)
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val detail = (state as? ReaderUiState.Success)?.detail
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val audioPlayer = remember { SurahAudioPlayer(context) }
    DisposableEffect(audioPlayer) {
        audioPlayer.onAyahPlayed = { _, _ -> prefs.recordPlayActivity(1) }
        onDispose { audioPlayer.release() }
    }

    var audioSheetTarget by remember { mutableStateOf<Triple<Int, String, Int>?>(null) }
    var tafsirAyah by remember { mutableStateOf<Int?>(null) }
    var noteAyah by remember { mutableStateOf<Int?>(null) }
    var tajweedMap by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var wordsMap by remember { mutableStateOf<Map<String, List<WordItem>>>(emptyMap()) }
    val bookmarkKeys = remember(prefs.bookmarks) {
        prefs.bookmarks.map { "${it.surahNumber}:${it.ayahNumber}" }.toSet()
    }
    val playingAyah = if (audioPlayer.activeSurahNumber == surahNumber) audioPlayer.activeAyahNumber else 0

    // Load optional tajweed/word-by-word data on demand
    LaunchedEffect(detail, prefs.showTajweed) {
        val d = detail ?: return@LaunchedEffect
        if (prefs.showTajweed && tajweedMap.isEmpty()) {
            tajweedMap = try { QuranApi.tajweedForSurah(d.meta.number) } catch (_: Throwable) { emptyMap() }
        }
    }
    LaunchedEffect(detail, prefs.showWordByWord, prefs.showTransliteration) {
        val d = detail ?: return@LaunchedEffect
        if ((prefs.showWordByWord || prefs.showTransliteration) && wordsMap.isEmpty()) {
            wordsMap = try { QuranApi.wordsForSurah(d.meta.number) } catch (_: Throwable) { emptyMap() }
        }
    }

    // Auto-scroll to playing ayah
    LaunchedEffect(playingAyah, prefs.autoScrollAudio) {
        if (playingAyah > 0 && prefs.autoScrollAudio) {
            try { listState.animateScrollToItem(playingAyah) } catch (_: Throwable) {}
        }
    }

    // Persist audio resume position periodically
    LaunchedEffect(audioPlayer.isPlaying, audioPlayer.activeAyahNumber) {
        while (audioPlayer.activeSurahNumber == surahNumber && audioPlayer.isPlaying) {
            prefs.saveLastAudio(surahNumber, audioPlayer.activeAyahNumber.coerceAtLeast(1), audioPlayer.activeReciterId, audioPlayer.positionMs)
            kotlinx.coroutines.delay(3000)
        }
        if (audioPlayer.activeSurahNumber == surahNumber && audioPlayer.activeAyahNumber > 0) {
            prefs.saveLastAudio(surahNumber, audioPlayer.activeAyahNumber, audioPlayer.activeReciterId, audioPlayer.positionMs)
        }
    }

    LaunchedEffect(detail) {
        val data = detail ?: return@LaunchedEffect
        val target = initialAyah.coerceIn(1, data.meta.ayahCount)
        if (target > 1) {
            listState.scrollToItem(target)
        }
        snapshotFlow { listState.isScrollInProgress to listState.firstVisibleItemIndex }
            .filter { !it.first }
            .map { it.second }
            .distinctUntilChanged()
            .collect { index ->
                val ayah = index.coerceIn(1, data.meta.ayahCount)
                prefs.saveLastRead(
                    surahNumber = data.meta.number,
                    surahName = data.meta.latinName,
                    ayah = ayah,
                    total = data.meta.ayahCount
                )
            }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            ReaderTopBar(
                title = detail?.meta?.latinName ?: "Quriva",
                subtitle = detail?.meta?.let { "${it.number} · ${it.place}" },
                totalAyah = detail?.meta?.ayahCount ?: 0,
                listState = listState,
                audioPlaying = audioPlayer.isPlaying,
                onBack = onBack,
                onOpenAudio = {
                    detail?.meta?.let { audioSheetTarget = Triple(it.number, it.latinName, it.ayahCount) }
                },
                onOpenSettings = onOpenSettings
            )
        },
        bottomBar = {
            val nowPlaying = audioPlayer.currentTitle
            if (nowPlaying != null) {
                NowPlayingBar(
                    title = nowPlaying,
                    isPlaying = audioPlayer.isPlaying,
                    isBuffering = audioPlayer.isBuffering,
                    onToggle = audioPlayer::toggle,
                    onOpen = {
                        val number = audioPlayer.activeSurahNumber
                        val name = audioPlayer.activeSurahName
                        if (number != null && name != null) {
                            audioSheetTarget = Triple(number, name, detail?.meta?.ayahCount ?: 0)
                        }
                    }
                )
            }
        },
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.navigationBarsPadding()
            ) { data ->
                Snackbar(
                    snackbarData = data,
                    shape = MaterialTheme.shapes.medium,
                    containerColor = MaterialTheme.colorScheme.inverseSurface,
                    contentColor = MaterialTheme.colorScheme.inverseOnSurface
                )
            }
        }
    ) { padding ->
        when (state) {
            is ReaderUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = padding.calculateTopPadding()),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onBackground,
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "Memuat surah...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            is ReaderUiState.Error -> {
                ReaderErrorView(
                    message = (state as ReaderUiState.Error).message,
                    onRetry = viewModel::load,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = padding.calculateTopPadding())
                )
            }

            is ReaderUiState.Success -> {
                val data = (state as ReaderUiState.Success).detail
                ReaderContent(
                    detail = data,
                    prefs = prefs,
                    listState = listState,
                    topPadding = padding.calculateTopPadding(),
                    bottomPadding = padding.calculateBottomPadding(),
                    bookmarkKeys = bookmarkKeys,
                    playingAyah = playingAyah,
                    tajweedMap = tajweedMap,
                    wordsMap = wordsMap,
                    onOpenAudio = {
                        audioSheetTarget = Triple(data.meta.number, data.meta.latinName, data.meta.ayahCount)
                    },
                    onCopy = { ayah ->
                        copyAyah(context, data, ayah)
                        scope.launch { snackbarHostState.showSnackbar("Ayat disalin ke clipboard") }
                    },
                    onShare = { ayah -> shareAyah(context, data, ayah) },
                    onShareImage = { ayah -> shareAyahImage(context, data, ayah) },
                    onTafsir = { ayah -> tafsirAyah = ayah.numberInSurah },
                    onNote = { ayah -> noteAyah = ayah.numberInSurah },
                    onPlayFrom = { ayah ->
                        // start playback from this ayah
                        if (audioPlayer.activeSurahNumber != data.meta.number) {
                            audioSheetTarget = Triple(data.meta.number, data.meta.latinName, data.meta.ayahCount)
                        }
                        scope.launch {
                            try {
                                val list = QuranApi.ayahAudioList(prefs.reciterId, data.meta.number)
                                audioPlayer.playSurah(data.meta.number, data.meta.latinName, prefs.reciterId, com.quriva.app.data.Reciters.byId(prefs.reciterId).name, list, ayah.numberInSurah, 0L)
                            } catch (_: Throwable) {
                                audioSheetTarget = Triple(data.meta.number, data.meta.latinName, data.meta.ayahCount)
                            }
                        }
                    },
                    onBookmark = { ayah ->
                        val added = prefs.toggleBookmark(
                            surahNumber = data.meta.number,
                            surahName = data.meta.latinName,
                            ayahNumber = ayah.numberInSurah,
                            arabic = ayah.arabic,
                            translation = ayah.translation
                        )
                        scope.launch {
                            snackbarHostState.showSnackbar(if (added) "Ayat ditandai di bookmark" else "Bookmark ayat dihapus")
                        }
                    }
                )
            }
        }
    }

    val currentAudioTarget = audioSheetTarget
    if (currentAudioTarget != null) {
        AudioSheet(
            surahNumber = currentAudioTarget.first,
            surahName = currentAudioTarget.second,
            ayahCount = currentAudioTarget.third,
            player = audioPlayer,
            prefs = prefs,
            onDismiss = { audioSheetTarget = null }
        )
    }

    val currentDetail = detail
    val currentTafsirAyah = tafsirAyah
    if (currentTafsirAyah != null && currentDetail != null) {
        TafsirSheet(
            surahNumber = currentDetail.meta.number,
            surahName = currentDetail.meta.latinName,
            ayahNumber = currentTafsirAyah,
            prefs = prefs,
            onDismiss = { tafsirAyah = null }
        )
    }

    val currentNoteAyah = noteAyah
    if (currentNoteAyah != null && currentDetail != null) {
        val existing = prefs.noteFor(currentDetail.meta.number, currentNoteAyah) ?: ""
        var noteText by remember(currentNoteAyah) { mutableStateOf(existing) }
        AlertDialog(
            onDismissRequest = { noteAyah = null },
            title = { Text("Catatan · ${currentDetail.meta.latinName} ${currentDetail.meta.number}:$currentNoteAyah") },
            text = {
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    modifier = Modifier.fillMaxWidth().height(140.dp),
                    placeholder = { Text("Tulis catatan/refleksi pribadi...") },
                    colors = OutlinedTextFieldDefaults.colors()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    prefs.saveNote(currentDetail.meta.number, currentNoteAyah, currentDetail.meta.latinName, noteText)
                    noteAyah = null
                }) { Text("Simpan") }
            },
            dismissButton = {
                TextButton(onClick = { noteAyah = null }) { Text("Tutup") }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}

@Composable
private fun ReaderTopBar(
    title: String,
    subtitle: String?,
    totalAyah: Int,
    listState: androidx.compose.foundation.lazy.LazyListState,
    audioPlaying: Boolean,
    onBack: () -> Unit,
    onOpenAudio: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.background)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    painter = painterResource(R.drawable.ic_back),
                    contentDescription = "Kembali",
                    tint = colors.onBackground,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant
                    )
                }
            }
            IconButton(onClick = onOpenAudio) {
                Box(contentAlignment = Alignment.TopEnd) {
                    Icon(
                        painter = painterResource(R.drawable.ic_headphones),
                        contentDescription = "Murottal surah",
                        tint = colors.onBackground,
                        modifier = Modifier.size(21.dp)
                    )
                    if (audioPlaying) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(colors.onBackground, CircleShape)
                        )
                    }
                }
            }
            IconButton(onClick = onOpenSettings) {
                Icon(
                    painter = painterResource(R.drawable.ic_tune),
                    contentDescription = "Pengaturan teks",
                    tint = colors.onBackground,
                    modifier = Modifier.size(21.dp)
                )
            }
        }
        ReaderProgressBar(totalAyah = totalAyah, listState = listState)
    }
}

@Composable
private fun ReaderProgressBar(
    totalAyah: Int,
    listState: androidx.compose.foundation.lazy.LazyListState
) {
    // Keep high-frequency scroll state local to this tiny composable instead
    // of invalidating the top bar or entire reader on every visible ayah.
    val progress by remember(listState, totalAyah) {
        derivedStateOf {
            if (totalAyah <= 0) 0f
            else if (!listState.canScrollForward) 1f
            else {
                val visible = listState.firstVisibleItemIndex.coerceIn(0, totalAyah)
                (visible.toFloat() / totalAyah.toFloat()).coerceIn(0f, 1f)
            }
        }
    }
    LinearProgressIndicator(
        progress = { progress },
        modifier = Modifier
            .fillMaxWidth()
            .height(2.dp),
        color = MaterialTheme.colorScheme.onBackground,
        trackColor = MaterialTheme.colorScheme.outlineVariant
    )
}

@Composable
private fun ReaderContent(
    detail: SurahDetail,
    prefs: AppPreferences,
    listState: androidx.compose.foundation.lazy.LazyListState,
    topPadding: Dp,
    bottomPadding: Dp,
    bookmarkKeys: Set<String>,
    playingAyah: Int,
    tajweedMap: Map<String, String>,
    wordsMap: Map<String, List<WordItem>>,
    onOpenAudio: () -> Unit,
    onCopy: (Ayah) -> Unit,
    onShare: (Ayah) -> Unit,
    onShareImage: (Ayah) -> Unit,
    onTafsir: (Ayah) -> Unit,
    onNote: (Ayah) -> Unit,
    onPlayFrom: (Ayah) -> Unit,
    onBookmark: (Ayah) -> Unit
) {
    val bottomModifier = if (bottomPadding > 0.dp) Modifier.padding(bottom = bottomPadding) else Modifier.navigationBarsPadding()
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(top = topPadding).then(bottomModifier),
        state = listState
    ) {
        item(key = "header", contentType = "header") { SurahHeader(detail.meta, onOpenAudio) }
        items(items = detail.ayahs, key = { it.numberInSurah }, contentType = { "ayah" }) { ayah ->
            val key = "${detail.meta.number}:${ayah.numberInSurah}"
            AyahBlock(
                ayah = ayah,
                surahNumber = detail.meta.number,
                arabicSize = prefs.arabicSize,
                translationSize = prefs.translationSize,
                showTranslation = prefs.showTranslation,
                showTajweed = prefs.showTajweed,
                showWordByWord = prefs.showWordByWord,
                showTransliteration = prefs.showTransliteration,
                isBookmarked = key in bookmarkKeys,
                isPlaying = playingAyah == ayah.numberInSurah,
                hasNote = prefs.noteFor(detail.meta.number, ayah.numberInSurah) != null,
                tajweed = tajweedMap[key],
                words = wordsMap[key],
                onCopy = { onCopy(ayah) },
                onShare = { onShare(ayah) },
                onShareImage = { onShareImage(ayah) },
                onTafsir = { onTafsir(ayah) },
                onNote = { onNote(ayah) },
                onPlayFrom = { onPlayFrom(ayah) },
                onBookmark = { onBookmark(ayah) }
            )
        }
        item(key = "footer", contentType = "footer") {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 36.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("۞", fontSize = 24.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                Text("Akhir Surah ${detail.meta.latinName}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun NowPlayingBar(
    title: String,
    isPlaying: Boolean,
    isBuffering: Boolean,
    onToggle: () -> Unit,
    onOpen: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = colors.background,
        border = BorderStroke(1.dp, colors.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .clickable(onClick = onOpen)
                .padding(start = 16.dp, end = 8.dp, top = 9.dp, bottom = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = colors.onBackground
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_headphones),
                    contentDescription = null,
                    tint = colors.background,
                    modifier = Modifier
                        .padding(8.dp)
                        .size(17.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Sedang diputar",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = onToggle, modifier = Modifier.size(44.dp)) {
                if (isBuffering) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = colors.onBackground
                    )
                } else {
                    Icon(
                        painter = painterResource(
                            if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
                        ),
                        contentDescription = if (isPlaying) "Jeda" else "Lanjutkan",
                        tint = colors.onBackground,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SurahHeader(surah: SurahMeta, onOpenAudio: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(top = 26.dp, bottom = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "SURAH ${surah.number}",
            style = MaterialTheme.typography.labelSmall,
            letterSpacing = 2.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(14.dp))
        Text(
            text = surah.nameArabic,
            fontFamily = ArabicFontFamily,
            fontSize = 46.sp,
            lineHeight = 62.sp,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = surah.latinName,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = surah.translation,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetaChip(surah.place)
            MetaChip("${surah.ayahCount} ayat")
            MetaChip("Hal. ${surah.pageStart}")
        }
        Spacer(Modifier.height(18.dp))
        Surface(
            onClick = onOpenAudio,
            shape = CircleShape,
            color = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onBackground,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_headphones),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Dengarkan murottal",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
        if (surah.bismillah) {
            Spacer(Modifier.height(28.dp))
            Text(
                text = BISMILLAH,
                fontFamily = ArabicFontFamily,
                fontSize = 27.sp,
                lineHeight = 46.sp,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .width(44.dp)
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.outline)
            )
        }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun MetaChip(label: String) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun AyahBlock(
    ayah: Ayah,
    surahNumber: Int,
    arabicSize: Float,
    translationSize: Float,
    showTranslation: Boolean,
    showTajweed: Boolean,
    showWordByWord: Boolean,
    showTransliteration: Boolean,
    isBookmarked: Boolean,
    isPlaying: Boolean,
    hasNote: Boolean,
    tajweed: String?,
    words: List<WordItem>?,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onShareImage: () -> Unit,
    onTafsir: () -> Unit,
    onNote: () -> Unit,
    onPlayFrom: () -> Unit,
    onBookmark: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val bg = if (isPlaying) colors.surfaceVariant.copy(alpha = 0.55f) else Color.Transparent
    Column(modifier = Modifier.fillMaxWidth().background(bg)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 8.dp, top = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(30.dp), contentAlignment = Alignment.Center) {
                Surface(modifier = Modifier.fillMaxSize(), shape = CircleShape, color = if (isPlaying) colors.onBackground else Color.Transparent, border = if (isPlaying) null else BorderStroke(1.dp, colors.outline)) {}
                Text(ayah.numberInSurah.toString(), style = MaterialTheme.typography.labelSmall, color = if (isPlaying) colors.background else colors.onBackground)
            }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onPlayFrom, modifier = Modifier.size(32.dp)) {
                Icon(painter = painterResource(R.drawable.ic_play), contentDescription = "Putar dari ayat", tint = if (isPlaying) colors.onBackground else colors.onSurfaceVariant, modifier = Modifier.size(15.dp))
            }
            IconButton(onClick = onTafsir, modifier = Modifier.size(32.dp)) {
                Icon(painter = painterResource(R.drawable.ic_book_open), contentDescription = "Tafsir", tint = colors.onSurfaceVariant, modifier = Modifier.size(15.dp))
            }
            IconButton(onClick = onNote, modifier = Modifier.size(32.dp)) {
                Icon(painter = painterResource(R.drawable.ic_copy), contentDescription = "Catatan", tint = if (hasNote) colors.onBackground else colors.onSurfaceVariant, modifier = Modifier.size(14.dp))
            }
            IconButton(onClick = onBookmark, modifier = Modifier.size(32.dp)) {
                Icon(painter = painterResource(if (isBookmarked) R.drawable.ic_bookmark_filled else R.drawable.ic_bookmark), contentDescription = "Bookmark", tint = if (isBookmarked) colors.onBackground else colors.onSurfaceVariant, modifier = Modifier.size(15.dp))
            }
            IconButton(onClick = onShare, modifier = Modifier.size(32.dp)) {
                Icon(painter = painterResource(R.drawable.ic_share), contentDescription = "Bagikan", tint = colors.onSurfaceVariant, modifier = Modifier.size(14.dp))
            }
            IconButton(onClick = onShareImage, modifier = Modifier.size(32.dp)) {
                Icon(painter = painterResource(R.drawable.ic_layers), contentDescription = "Kartu ayat", tint = colors.onSurfaceVariant, modifier = Modifier.size(14.dp))
            }
            IconButton(onClick = onCopy, modifier = Modifier.size(32.dp)) {
                Icon(painter = painterResource(R.drawable.ic_copy), contentDescription = "Salin", tint = colors.onSurfaceVariant, modifier = Modifier.size(14.dp))
            }
        }
        Spacer(Modifier.height(10.dp))
        SelectionContainer {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                val arabicAnnotated = if (showTajweed && !tajweed.isNullOrBlank()) {
                    parseTajweed(tajweed, colors.onBackground)
                } else {
                    androidx.compose.ui.text.AnnotatedString(ayah.arabic)
                }
                Text(
                    text = arabicAnnotated,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                    style = TextStyle(fontFamily = ArabicFontFamily, fontSize = arabicSize.sp, lineHeight = (arabicSize * 1.9f).sp, color = colors.onBackground),
                    textAlign = TextAlign.Start
                )
            }
        }
        if (showTranslation) {
            Spacer(Modifier.height(12.dp))
            Text(ayah.translation, modifier = Modifier.padding(horizontal = 24.dp), style = TextStyle(fontSize = translationSize.sp, lineHeight = (translationSize * 1.6f).sp, color = colors.onSurfaceVariant))
        }
        if ((showWordByWord || showTransliteration) && !words.isNullOrEmpty()) {
            Spacer(Modifier.height(10.dp))
            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                words.filter { !it.isEnd }.take(40).forEach { w ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                        if (showTransliteration && w.transliteration.isNotBlank()) {
                            Text(w.transliteration, style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant, modifier = Modifier.weight(1f))
                        }
                        if (showWordByWord && w.translation.isNotBlank()) {
                            Text(w.translation, style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant.copy(alpha = 0.9f), modifier = Modifier.weight(1f))
                        }
                        Text(w.text, fontFamily = ArabicFontFamily, fontSize = (arabicSize * 0.8f).sp, color = colors.onBackground)
                    }
                }
                if (words.size > 41) {
                    Text("…", style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                }
            }
        }
        Spacer(Modifier.height(18.dp))
        HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp), color = colors.outlineVariant)
    }
}

@Composable
private fun ReaderErrorView(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Konten gagal dimuat",
                style = MaterialTheme.typography.titleLarge,
                color = colors.onBackground,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(22.dp))
            Button(
                onClick = onRetry,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.onBackground,
                    contentColor = colors.background
                )
            ) {
                Text("Coba lagi", modifier = Modifier.padding(horizontal = 10.dp))
            }
        }
    }
}

private fun copyAyah(context: Context, detail: SurahDetail, ayah: Ayah) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(
        ClipData.newPlainText("Quriva", buildAyahText(detail, ayah))
    )
}

private fun shareAyah(context: Context, detail: SurahDetail, ayah: Ayah) {
    val text = buildAyahText(detail, ayah) +
        "\n\nhttps://quran.com/${detail.meta.number}/${ayah.numberInSurah}"
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "${detail.meta.latinName} ${detail.meta.number}:${ayah.numberInSurah}")
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "Bagikan ayat"))
}

private fun shareAyahImage(context: Context, detail: SurahDetail, ayah: Ayah) {
    try {
        val width = 1080
        val pad = 72f
        val title = "${detail.meta.latinName} ${detail.meta.number}:${ayah.numberInSurah}"
        val body = ayah.translation
        val footer = "Quriva · ${detail.meta.number}:${ayah.numberInSurah}"
        val paintBg = Paint().apply { color = android.graphics.Color.WHITE }
        val paintTitle = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.DKGRAY; textSize = 42f }
        val paintBody = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.BLACK; textSize = 46f }
        val paintFooter = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.GRAY; textSize = 34f }
        val lines = wrapText(body, paintBody, width - pad * 2)
        val height = (pad + 80 + 40 + lines.size * 64 + 80 + pad).toInt().coerceAtLeast(600)
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paintBg)
        var y = pad + 50
        canvas.drawText(title, pad, y, paintTitle)
        y += 70
        lines.forEach { line -> canvas.drawText(line, pad, y, paintBody); y += 64 }
        y += 40
        canvas.drawText(footer, pad, y.coerceAtMost(height - pad), paintFooter)
        val dir = File(context.cacheDir, "shared").apply { mkdirs() }
        val file = File(dir, "ayah_${detail.meta.number}_${ayah.numberInSurah}.png")
        FileOutputStream(file).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bmp.recycle()
        val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Bagikan kartu ayat"))
    } catch (_: Throwable) {
        shareAyah(context, detail, ayah)
    }
}

private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
    val words = text.split(Regex("\\s+")).filter { it.isNotBlank() }
    if (words.isEmpty()) return listOf("")
    val lines = mutableListOf<String>()
    var current = ""
    for (w in words) {
        val candidate = if (current.isEmpty()) w else "$current $w"
        if (paint.measureText(candidate) <= maxWidth) {
            current = candidate
        } else {
            if (current.isNotEmpty()) lines += current
            current = w
        }
    }
    if (current.isNotEmpty()) lines += current
    return lines.take(20)
}

private fun buildAyahText(detail: SurahDetail, ayah: Ayah): String =
    "${detail.meta.latinName} ${detail.meta.number}:${ayah.numberInSurah}\n\n" +
        "${ayah.arabic}\n\n${ayah.translation}"
