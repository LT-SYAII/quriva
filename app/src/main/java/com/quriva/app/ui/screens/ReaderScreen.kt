package com.quriva.app.ui.screens

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import com.quriva.app.data.QuranRepository
import com.quriva.app.data.RepeatMode
import com.quriva.app.data.SurahAudioPlayer
import com.quriva.app.data.Translations
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
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextButton
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import com.quriva.app.ui.parseTajweed
import com.quriva.app.ui.TajweedRules
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val BISMILLAH = "بِسْمِ ٱللَّهِ ٱلرَّحْمَـٰنِ ٱلرَّحِيمِ"

@Composable
fun ReaderScreen(
    surahNumber: Int,
    initialAyah: Int,
    prefs: AppPreferences,
    audioPlayer: SurahAudioPlayer,
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

    var audioSheetTarget by remember { mutableStateOf<Triple<Int, String, Int>?>(null) }
    var tafsirAyah by remember { mutableStateOf<Int?>(null) }
    var noteAyah by remember { mutableStateOf<Int?>(null) }
    var shareImageAyah by remember { mutableStateOf<Ayah?>(null) }
    var tajweedMap by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var tajweedRuleId by remember { mutableStateOf<String?>(null) }
    var wordsMap by remember { mutableStateOf<Map<String, List<WordItem>>>(emptyMap()) }
    var translationMap by remember(surahNumber) { mutableStateOf<Map<String, String>>(emptyMap()) }
    var translationLoading by remember(surahNumber) { mutableStateOf(false) }
    var translationError by remember(surahNumber) { mutableStateOf<String?>(null) }
    val bookmarkKeys = remember(prefs.bookmarks) {
        prefs.bookmarks.map { "${it.surahNumber}:${it.ayahNumber}" }.toSet()
    }
    val playingAyah = if (audioPlayer.activeSurahNumber == surahNumber) audioPlayer.activeAyahNumber else 0

    // Load optional tajweed/word-by-word data on demand
    LaunchedEffect(detail, prefs.showTajweed) {
        val d = detail ?: return@LaunchedEffect
        if (prefs.showTajweed && tajweedMap.isEmpty()) {
            tajweedMap = try {
                QuranApi.tajweedForSurah(d.meta.number)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Throwable) {
                emptyMap()
            }
        }
    }
    LaunchedEffect(detail, prefs.showWordByWord, prefs.showTransliteration) {
        val d = detail ?: return@LaunchedEffect
        if ((prefs.showWordByWord || prefs.showTransliteration) && wordsMap.isEmpty()) {
            wordsMap = try {
                QuranApi.wordsForSurah(d.meta.number)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Throwable) {
                emptyMap()
            }
        }
    }
    LaunchedEffect(detail, prefs.translationId) {
        val d = detail ?: return@LaunchedEffect
        translationMap = emptyMap()
        translationError = null
        if (prefs.translationId == 33) {
            translationLoading = false
            return@LaunchedEffect
        }
        translationLoading = true
        try {
            val fetched = QuranApi.translationForSurah(prefs.translationId, d.meta.number)
            if (fetched.isEmpty()) {
                error("Terjemahan ini belum tersedia untuk surah tersebut.")
            }
            translationMap = fetched.associate { it.verseKey to it.text }
        } catch (e: CancellationException) {
            throw e
        } catch (t: Throwable) {
            translationError = QuranApi.friendlyNetworkError(t)
        } finally {
            translationLoading = false
        }
    }

    // Auto-scroll to playing ayah
    LaunchedEffect(playingAyah, prefs.autoScrollAudio) {
        if (playingAyah > 0 && prefs.autoScrollAudio) {
            try { listState.animateScrollToItem(playingAyah) } catch (_: Throwable) {}
        }
    }

    LaunchedEffect(detail) {
        val data = detail ?: return@LaunchedEffect
        val target = initialAyah.coerceIn(1, data.meta.ayahCount)
        if (target > 1) {
            listState.scrollToItem(target)
        }
        snapshotFlow {
            val visibleAyahs = listState.layoutInfo.visibleItemsInfo
                .map { it.index }
                .filter { it in 1..data.meta.ayahCount }
                .distinct()
            listState.isScrollInProgress to visibleAyahs
        }
            .filter { (scrolling, visibleAyahs) -> !scrolling && visibleAyahs.isNotEmpty() }
            .map { it.second }
            .distinctUntilChanged()
            .collect { visibleAyahs ->
                val ayah = visibleAyahs.first().coerceIn(1, data.meta.ayahCount)
                prefs.saveLastRead(
                    surahNumber = data.meta.number,
                    surahName = data.meta.latinName,
                    ayah = ayah,
                    total = data.meta.ayahCount,
                    visibleAyahs = visibleAyahs
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
                        if (number in 1..114 && name != null) {
                            val count = detail?.takeIf { it.meta.number == number }?.meta?.ayahCount
                                ?: runCatching {
                                    QuranRepository.getSurahs(context)
                                        .firstOrNull { it.number == number }
                                        ?.ayahCount
                                }.getOrNull()
                                ?: 0
                            audioSheetTarget = Triple(number, name, count)
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
                    translationId = prefs.translationId,
                    translationName = Translations.byId(prefs.translationId).name,
                    translationMap = translationMap,
                    translationLoading = translationLoading,
                    translationError = translationError,
                    onTajweedRule = { tajweedRuleId = it },
                    onOpenAudio = {
                        audioSheetTarget = Triple(data.meta.number, data.meta.latinName, data.meta.ayahCount)
                    },
                    onCopy = { ayah ->
                        copyAyah(context, data, ayah)
                        scope.launch { snackbarHostState.showSnackbar("Ayat disalin ke clipboard") }
                    },
                    onShare = { ayah -> shareAyah(context, data, ayah) },
                    onShareImage = { ayah -> shareImageAyah = ayah },
                    onTafsir = { ayah -> tafsirAyah = ayah.numberInSurah },
                    onNote = { ayah -> noteAyah = ayah.numberInSurah },
                    onPlayFrom = { ayah ->
                        // start playback from this ayah
                        if (audioPlayer.activeSurahNumber != data.meta.number) {
                            audioSheetTarget = Triple(data.meta.number, data.meta.latinName, data.meta.ayahCount)
                        }
                        scope.launch {
                            try {
                                val downloaded = withContext(Dispatchers.IO) {
                                    SurahAudioPlayer.isSurahDownloaded(
                                        context,
                                        data.meta.number,
                                        prefs.reciterId,
                                        data.meta.ayahCount
                                    )
                                }
                                val list = if (downloaded) {
                                    (1..data.meta.ayahCount).map { number ->
                                        com.quriva.app.data.AyahAudio(
                                            number,
                                            "${data.meta.number}:$number",
                                            ""
                                        )
                                    }
                                } else {
                                    QuranApi.ayahAudioList(prefs.reciterId, data.meta.number)
                                }
                                audioPlayer.playSurah(
                                    data.meta.number,
                                    data.meta.latinName,
                                    prefs.reciterId,
                                    com.quriva.app.data.Reciters.byId(prefs.reciterId).name,
                                    list,
                                    ayah.numberInSurah,
                                    0L,
                                    downloaded
                                )
                            } catch (e: CancellationException) {
                                throw e
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
    tajweedRuleId?.let { id ->
        val info = TajweedRules.info(id)
        AlertDialog(onDismissRequest = { tajweedRuleId = null }, title = { Text(info.name) },
            text = { Text(info.explanation) }, confirmButton = { TextButton(onClick = { tajweedRuleId = null }) { Text("Mengerti") } })
    }
    val currentShareAyah = shareImageAyah
    if (currentShareAyah != null && currentDetail != null) {
        AyahCardTemplateSheet(
            detail = currentDetail,
            ayah = currentShareAyah,
            onDismiss = { shareImageAyah = null },
            onSelect = { template ->
                shareAyahImage(context, currentDetail, currentShareAyah, template)
                shareImageAyah = null
            }
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
    translationId: Int,
    translationName: String,
    translationMap: Map<String, String>,
    translationLoading: Boolean,
    translationError: String?,
    onTajweedRule: (String) -> Unit,
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
        if (translationId != 33) {
            item(key = "translation_status", contentType = "status") {
                TranslationStatus(
                    name = translationName,
                    loading = translationLoading,
                    error = translationError,
                    loaded = translationMap.isNotEmpty()
                )
            }
        }
        items(items = detail.ayahs, key = { it.numberInSurah }, contentType = { "ayah" }) { ayah ->
            val key = "${detail.meta.number}:${ayah.numberInSurah}"
            val selectedTranslation = translationMap[key]
            val displayedAyah = if (selectedTranslation.isNullOrBlank()) ayah
                else ayah.copy(translation = selectedTranslation)
            Box(modifier = Modifier.animateItem()) {
                AyahBlock(
                    ayah = displayedAyah,
                    arabicSize = prefs.arabicSize,
                    arabicLineSpacing = prefs.arabicLineSpacing,
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
                    onTajweedRule = onTajweedRule,
                    onCopy = { onCopy(displayedAyah) },
                    onShare = { onShare(displayedAyah) },
                    onShareImage = { onShareImage(displayedAyah) },
                    onTafsir = { onTafsir(ayah) },
                    onNote = { onNote(ayah) },
                    onPlayFrom = { onPlayFrom(ayah) },
                    onBookmark = { onBookmark(displayedAyah) }
                )
            }
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
private fun TranslationStatus(
    name: String,
    loading: Boolean,
    error: String?,
    loaded: Boolean
) {
    val colors = MaterialTheme.colorScheme
    AnimatedVisibility(visible = loading || error != null || loaded) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
            shape = MaterialTheme.shapes.medium,
            color = colors.surfaceVariant,
            border = BorderStroke(1.dp, colors.outlineVariant)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(15.dp),
                        strokeWidth = 1.5.dp,
                        color = colors.onBackground
                    )
                    Spacer(Modifier.width(9.dp))
                }
                Text(
                    when {
                        loading -> "Memuat terjemahan $name…"
                        error != null -> "Terjemahan online gagal dimuat. Menampilkan Kemenag RI offline."
                        else -> "Terjemahan $name · online"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant
                )
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
    arabicSize: Float,
    arabicLineSpacing: Float,
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
    onTajweedRule: (String) -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onShareImage: () -> Unit,
    onTafsir: () -> Unit,
    onNote: () -> Unit,
    onPlayFrom: () -> Unit,
    onBookmark: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val bg by animateColorAsState(
        targetValue = if (isPlaying) colors.surfaceVariant.copy(alpha = 0.55f) else Color.Transparent,
        animationSpec = tween(260),
        label = "playingAyahBackground"
    )
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
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            val arabicAnnotated = if (showTajweed && !tajweed.isNullOrBlank()) parseTajweed(tajweed, colors.onBackground)
            else androidx.compose.ui.text.AnnotatedString(ayah.arabic)
            if (showTajweed && !tajweed.isNullOrBlank()) {
                ClickableText(
                    text = arabicAnnotated,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                    style = TextStyle(fontFamily = ArabicFontFamily, fontSize = arabicSize.sp, lineHeight = (arabicSize * arabicLineSpacing).sp, color = colors.onBackground, textAlign = TextAlign.Start),
                    onClick = { offset -> arabicAnnotated.getStringAnnotations("TAJWEED", offset, offset).firstOrNull()?.let { onTajweedRule(it.item) } }
                )
            } else SelectionContainer {
                Text(text = arabicAnnotated, modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), style = TextStyle(fontFamily = ArabicFontFamily, fontSize = arabicSize.sp, lineHeight = (arabicSize * arabicLineSpacing).sp, color = colors.onBackground), textAlign = TextAlign.Start)
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

private enum class AyahCardTemplate(
    val label: String,
    val description: String,
    val darkPreview: Boolean
) {
    CLASSIC("Klasik", "Terjemahan dengan judul dan identitas Quriva", false),
    NIGHT("Malam", "Latar hitam, teks putih, kontras tinggi", true),
    ARABIC("Arab", "Teks Arab besar disertai terjemahan ringkas", false),
    MINIMAL("Minimal", "Hanya teks Arab dan referensi ayat", true)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AyahCardTemplateSheet(
    detail: SurahDetail,
    ayah: Ayah,
    onDismiss: () -> Unit,
    onSelect: (AyahCardTemplate) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.background
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            Text(
                "Kartu ayat",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(2.dp))
            Text(
                "Pilih template monokrom untuk dibagikan.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant
            )
            Spacer(Modifier.height(14.dp))
            Text(
                "QS. ${detail.meta.latinName} : ${ayah.numberInSurah}",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = colors.onSurfaceVariant
            )
            Spacer(Modifier.height(10.dp))
            AyahCardTemplate.entries.forEach { template ->
                Surface(
                    onClick = { onSelect(template) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = colors.surface,
                    border = BorderStroke(1.dp, colors.outlineVariant)
                ) {
                    Row(
                        Modifier.padding(11.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AyahTemplatePreview(template)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                template.label,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                template.description,
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_forward),
                            contentDescription = null,
                            tint = colors.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun AyahTemplatePreview(template: AyahCardTemplate) {
    val colors = MaterialTheme.colorScheme
    val background = if (template.darkPreview) colors.inverseSurface else colors.surfaceVariant
    val foreground = if (template.darkPreview) colors.inverseOnSurface else colors.onSurface
    Surface(
        modifier = Modifier.size(width = 56.dp, height = 48.dp),
        shape = RoundedCornerShape(11.dp),
        color = background,
        border = BorderStroke(1.dp, colors.outlineVariant)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = when (template) {
                    AyahCardTemplate.ARABIC, AyahCardTemplate.MINIMAL -> "اقرأ"
                    AyahCardTemplate.NIGHT -> "Aa"
                    AyahCardTemplate.CLASSIC -> "Aa"
                },
                fontFamily = if (template == AyahCardTemplate.ARABIC || template == AyahCardTemplate.MINIMAL) {
                    ArabicFontFamily
                } else {
                    androidx.compose.ui.text.font.FontFamily.Serif
                },
                fontSize = if (template == AyahCardTemplate.ARABIC || template == AyahCardTemplate.MINIMAL) 17.sp else 18.sp,
                fontWeight = FontWeight.Medium,
                color = foreground
            )
        }
    }
}

private fun shareAyahImage(
    context: Context,
    detail: SurahDetail,
    ayah: Ayah,
    template: AyahCardTemplate
) {
    try {
        val bmp = renderAyahCard(context, detail, ayah, template)
        val dir = File(context.cacheDir, "shared").apply { mkdirs() }
        val file = File(
            dir,
            "ayah_${detail.meta.number}_${ayah.numberInSurah}_${template.name.lowercase()}.png"
        )
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

private fun renderAyahCard(
    context: Context,
    detail: SurahDetail,
    ayah: Ayah,
    template: AyahCardTemplate
): Bitmap {
    val width = 1080
    val dark = template == AyahCardTemplate.NIGHT || template == AyahCardTemplate.MINIMAL
    val pad = if (template == AyahCardTemplate.MINIMAL) 96 else 76
    val contentWidth = width - pad * 2
    val background = if (dark) 0xFF0B0B0B.toInt() else 0xFFFFFFFF.toInt()
    val ink = if (dark) 0xFFFFFFFF.toInt() else 0xFF111111.toInt()
    val muted = if (dark) 0xFFA6A6A6.toInt() else 0xFF6B6B6B.toInt()
    val rule = if (dark) 0xFF2C2C2C.toInt() else 0xFFE0E0E0.toInt()

    val labelPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = muted
        textSize = 25f
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        letterSpacing = 0.14f
    }
    val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ink
        textSize = 38f
        typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
    }
    val bodyPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ink
        textSize = 41f
        typeface = Typeface.create("sans-serif", Typeface.NORMAL)
    }
    val arabicPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ink
        textSize = if (template == AyahCardTemplate.MINIMAL) 62f else 57f
        typeface = ResourcesCompat.getFont(context, R.font.amiri) ?: Typeface.SERIF
    }
    val footerPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = muted
        textSize = 25f
        typeface = Typeface.create("sans-serif", Typeface.NORMAL)
    }

    val reference = "${detail.meta.latinName} · ${detail.meta.number}:${ayah.numberInSurah}"
    val footer = "Quriva · quran.com/${detail.meta.number}/${ayah.numberInSurah}"
    val label = "QURIVA · ${template.label.uppercase()}"

    val titleLayout = buildCardLayout(
        text = reference,
        paint = titlePaint,
        width = contentWidth,
        align = Layout.Alignment.ALIGN_NORMAL,
        spacing = 1.1f,
        maxLines = 1
    )
    val bodyLayout = buildCardLayout(
        text = ayah.translation,
        paint = bodyPaint,
        width = contentWidth,
        align = Layout.Alignment.ALIGN_NORMAL,
        spacing = 1.32f,
        maxLines = 22
    )
    val audioArabicLayout = buildCardLayout(
        text = ayah.arabic,
        paint = arabicPaint,
        width = contentWidth,
        align = Layout.Alignment.ALIGN_NORMAL,
        spacing = 1.42f,
        maxLines = 24,
        rtl = true
    )
    val arabicBodyLayout = buildCardLayout(
        text = ayah.translation,
        paint = TextPaint(bodyPaint).apply { textSize = 35f },
        width = contentWidth,
        align = Layout.Alignment.ALIGN_NORMAL,
        spacing = 1.3f,
        maxLines = 16
    )

    var y = pad + 32f
    val footerSpace = 50f
    val contentHeight = when (template) {
        AyahCardTemplate.CLASSIC, AyahCardTemplate.NIGHT ->
            y + titleLayout.height + 30f + bodyLayout.height + 48f + footerSpace
        AyahCardTemplate.ARABIC ->
            y + 30f + audioArabicLayout.height + 34f + 32f + arabicBodyLayout.height + 42f + footerSpace
        AyahCardTemplate.MINIMAL ->
            y + audioArabicLayout.height + 64f + 38f + footerSpace
    }
    val height = (contentHeight + pad).toInt().coerceIn(620, 4200)
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.drawColor(background)

    if (template == AyahCardTemplate.NIGHT) {
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2f
            color = rule
        }
        canvas.drawRoundRect(
            26f,
            26f,
            width - 26f,
            height - 26f,
            28f,
            28f,
            borderPaint
        )
    }

    canvas.drawText(label, pad.toFloat(), y, labelPaint)
    y += 18f

    when (template) {
        AyahCardTemplate.CLASSIC, AyahCardTemplate.NIGHT -> {
            y += 26f
            canvas.drawLayout(titleLayout, pad.toFloat(), y)
            y += titleLayout.height + 30f
            canvas.drawLayout(bodyLayout, pad.toFloat(), y)
            y += bodyLayout.height + 48f
        }
        AyahCardTemplate.ARABIC -> {
            y += 20f
            canvas.drawLayout(titleLayout, pad.toFloat(), y)
            y += titleLayout.height + 30f
            canvas.drawLayout(audioArabicLayout, pad.toFloat(), y)
            y += audioArabicLayout.height + 34f
            canvas.drawLine(
                pad.toFloat(),
                y,
                (width - pad).toFloat(),
                y,
                Paint().apply { color = rule; strokeWidth = 1.4f }
            )
            y += 32f
            canvas.drawLayout(arabicBodyLayout, pad.toFloat(), y)
            y += arabicBodyLayout.height + 42f
        }
        AyahCardTemplate.MINIMAL -> {
            y += 36f
            canvas.drawLayout(audioArabicLayout, pad.toFloat(), y)
            y += audioArabicLayout.height + 64f
            val centered = TextPaint(titlePaint).apply {
                textSize = 29f
                color = muted
                typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            }
            canvas.drawText(
                reference,
                (width - centered.measureText(reference)) / 2f,
                y,
                centered
            )
            y += 38f
        }
    }

    val footerWidth = footerPaint.measureText(footer)
    canvas.drawText(
        footer,
        if (template == AyahCardTemplate.MINIMAL) (width - footerWidth) / 2f else pad.toFloat(),
        y.coerceAtMost(height - pad / 2f),
        footerPaint
    )

    return bitmap
}

private fun buildCardLayout(
    text: String,
    paint: TextPaint,
    width: Int,
    align: Layout.Alignment,
    spacing: Float,
    maxLines: Int,
    rtl: Boolean = false
): StaticLayout {
    val builder = StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
        .setAlignment(align)
        .setIncludePad(false)
        .setLineSpacing(0f, spacing)
        .setMaxLines(maxLines)
        .setEllipsize(TextUtils.TruncateAt.END)
    if (rtl) builder.setTextDirection(android.text.TextDirectionHeuristics.RTL)
    return builder.build()
}

private fun Canvas.drawLayout(layout: StaticLayout, x: Float, y: Float) {
    save()
    translate(x, y)
    layout.draw(this)
    restore()
}

private fun buildAyahText(detail: SurahDetail, ayah: Ayah): String =
    "${detail.meta.latinName} ${detail.meta.number}:${ayah.numberInSurah}\n\n" +
        "${ayah.arabic}\n\n${ayah.translation}"
