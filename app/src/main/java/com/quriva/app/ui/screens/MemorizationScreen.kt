package com.quriva.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quriva.app.R
import com.quriva.app.data.AppPreferences
import com.quriva.app.data.AyahAudio
import com.quriva.app.data.QuranApi
import com.quriva.app.data.QuranRepository
import com.quriva.app.data.Reciters
import com.quriva.app.data.RepeatMode
import com.quriva.app.data.SurahAudioPlayer
import com.quriva.app.model.Ayah
import com.quriva.app.model.MemorizationEntry
import com.quriva.app.model.MemorizationStatus
import com.quriva.app.ui.theme.ArabicFontFamily
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun MemorizationScreen(
    prefs: AppPreferences,
    player: SurahAudioPlayer,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val surahs = remember { QuranRepository.getSurahs(context) }
    var surahNumber by rememberSaveable { mutableIntStateOf(1) }
    val detail = remember(surahNumber) { QuranRepository.getSurah(context, surahNumber) }
    var start by rememberSaveable { mutableIntStateOf(1) }
    var end by rememberSaveable { mutableIntStateOf(5) }
    var status by rememberSaveable { mutableStateOf(MemorizationStatus.LEARNING) }
    var hideArabic by rememberSaveable { mutableStateOf(false) }
    var hideTranslation by rememberSaveable { mutableStateOf(false) }
    var revealed by rememberSaveable { mutableStateOf(false) }
    var surahMenuOpen by remember { mutableStateOf(false) }
    var repetitions by rememberSaveable { mutableIntStateOf(3) }
    var completed by rememberSaveable { mutableIntStateOf(0) }
    var quizIndex by rememberSaveable { mutableIntStateOf(0) }
    var quizReveal by rememberSaveable { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val colors = MaterialTheme.colorScheme

    val safeEnd = end.coerceIn(start, detail.meta.ayahCount)
    val range = remember(detail, start, safeEnd) {
        detail.ayahs.filter { it.numberInSurah in start..safeEnd }
    }
    val due = prefs.memorizationEntries.filter { it.reviewAt <= System.currentTimeMillis() }
    val playingThisRange = player.activeSurahNumber == surahNumber &&
        player.activeAyahNumber in start..safeEnd

    LaunchedEffect(surahNumber, start, safeEnd) {
        completed = 0
        quizIndex = 0
        quizReveal = false
        revealed = false
    }

    fun loadEntry(entry: MemorizationEntry) {
        surahNumber = entry.surah
        start = entry.startAyah
        end = entry.endAyah
        status = entry.status
    }

    Surface(Modifier.fillMaxSize(), color = colors.background) {
        Scaffold(
            modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing),
            containerColor = colors.background,
            snackbarHost = { SnackbarHost(snackbar) },
            topBar = {
                FeatureTopBar(
                    title = "Hafalan & Murajaah",
                    subtitle = "Sesi terarah · progres tersimpan",
                    onBack = onBack
                )
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(top = padding.calculateTopPadding()),
                contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 36.dp)
            ) {
                if (due.isNotEmpty()) {
                    item(key = "due") {
                        DueReviewCard(entries = due.take(4), onSelect = ::loadEntry)
                        Spacer(Modifier.height(18.dp))
                    }
                }

                item(key = "session") {
                    SectionEyebrow("SUSUN SESI", "Pilih surah dan rentang ayat")
                    Spacer(Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = colors.surfaceVariant,
                        border = BorderStroke(1.dp, colors.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            ExposedDropdownMenuBox(
                                expanded = surahMenuOpen,
                                onExpandedChange = { surahMenuOpen = it }
                            ) {
                                OutlinedTextField(
                                    value = "${detail.meta.number}. ${detail.meta.latinName}",
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Surah") },
                                    supportingText = { Text("${detail.meta.ayahCount} ayat · ${detail.meta.place}") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(surahMenuOpen) },
                                    shape = RoundedCornerShape(18.dp),
                                    modifier = Modifier.menuAnchor().fillMaxWidth()
                                )
                                ExposedDropdownMenu(
                                    expanded = surahMenuOpen,
                                    onDismissRequest = { surahMenuOpen = false }
                                ) {
                                    surahs.forEach { surah ->
                                        DropdownMenuItem(
                                            text = {
                                                Column {
                                                    Text("${surah.number}. ${surah.latinName}")
                                                    Text(
                                                        "${surah.ayahCount} ayat · ${surah.translation}",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = colors.onSurfaceVariant
                                                    )
                                                }
                                            },
                                            onClick = {
                                                surahNumber = surah.number
                                                start = 1
                                                end = minOf(5, surah.ayahCount)
                                                surahMenuOpen = false
                                            }
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.height(18.dp))
                            RangeLabel("Ayat mulai", start, detail.meta.ayahCount)
                            androidx.compose.material3.Slider(
                                value = start.toFloat(),
                                onValueChange = {
                                    start = it.toInt().coerceIn(1, safeEnd)
                                },
                                valueRange = 1f..detail.meta.ayahCount.toFloat()
                            )
                            RangeLabel("Ayat akhir", safeEnd, detail.meta.ayahCount)
                            androidx.compose.material3.Slider(
                                value = safeEnd.toFloat(),
                                onValueChange = {
                                    end = it.toInt().coerceIn(start, detail.meta.ayahCount)
                                },
                                valueRange = 1f..detail.meta.ayahCount.toFloat()
                            )
                            Surface(
                                shape = CircleShape,
                                color = colors.onBackground,
                                contentColor = colors.background,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            ) {
                                Text(
                                    "${range.size} ayat dipilih · $start–$safeEnd",
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(20.dp))
                    Text("Status hafalan", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MemorizationStatus.entries.forEach { item ->
                            FilterChip(
                                selected = status == item,
                                onClick = { status = item },
                                label = { Text(item.label) }
                            )
                        }
                    }
                    Spacer(Modifier.height(22.dp))
                }

                item(key = "practice-heading") {
                    SectionEyebrow("MODE LATIHAN", "Atur bantuan yang ingin disembunyikan")
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PracticeToggle(
                            label = "Teks Arab",
                            hidden = hideArabic,
                            onClick = { hideArabic = !hideArabic; revealed = false },
                            modifier = Modifier.weight(1f)
                        )
                        PracticeToggle(
                            label = "Terjemahan",
                            hidden = hideTranslation,
                            onClick = { hideTranslation = !hideTranslation; revealed = false },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                }

                items(range, key = { "ayah-${it.numberInSurah}" }) { ayah ->
                    PracticeAyahCard(
                        ayah = ayah,
                        showArabic = !hideArabic || revealed,
                        showTranslation = !hideTranslation || revealed,
                        onReveal = {
                            if (hideArabic || hideTranslation) revealed = !revealed
                        }
                    )
                    Spacer(Modifier.height(10.dp))
                }

                if (hideArabic || hideTranslation) {
                    item(key = "reveal") {
                        OutlinedButton(
                            onClick = { revealed = !revealed },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape = CircleShape
                        ) {
                            Icon(
                                painter = painterResource(if (revealed) R.drawable.ic_close else R.drawable.ic_book_open),
                                contentDescription = null,
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(if (revealed) "Sembunyikan jawaban" else "Ungkap jawaban")
                        }
                        Spacer(Modifier.height(18.dp))
                    }
                }

                if (range.size > 1) {
                    item(key = "quiz") {
                        val questionIndex = quizIndex % (range.size - 1)
                        val current = range[questionIndex]
                        val next = range[questionIndex + 1]
                        QuizCard(
                            currentAyah = current.numberInSurah,
                            answer = next.arabic,
                            answerVisible = quizReveal,
                            question = questionIndex + 1,
                            total = range.size - 1,
                            onToggleAnswer = { quizReveal = !quizReveal },
                            onNext = {
                                quizIndex = (quizIndex + 1) % (range.size - 1)
                                quizReveal = false
                            }
                        )
                        Spacer(Modifier.height(18.dp))
                    }
                }

                item(key = "repeat") {
                    SectionEyebrow("PENGULANGAN", "Dengarkan, tirukan, lalu tandai progres")
                    Spacer(Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = colors.inverseSurface,
                        contentColor = colors.inverseOnSurface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(18.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text("Target sesi", style = MaterialTheme.typography.labelSmall, color = colors.inverseOnSurface.copy(alpha = .65f))
                                    Text("$completed dari $repetitions pengulangan", style = MaterialTheme.typography.titleLarge)
                                }
                                Text(
                                    "${if (repetitions > 0) completed * 100 / repetitions else 0}%",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(Modifier.height(12.dp))
                            LinearProgressIndicator(
                                progress = { completed.toFloat() / repetitions.coerceAtLeast(1) },
                                modifier = Modifier.fillMaxWidth().height(5.dp).clip(CircleShape),
                                color = colors.inverseOnSurface,
                                trackColor = colors.inverseOnSurface.copy(alpha = .16f)
                            )
                            Spacer(Modifier.height(13.dp))
                            Text("Ubah target · $repetitions×", style = MaterialTheme.typography.labelSmall, color = colors.inverseOnSurface.copy(alpha = .7f))
                            androidx.compose.material3.Slider(
                                value = repetitions.toFloat(),
                                onValueChange = {
                                    repetitions = it.toInt()
                                    completed = completed.coerceAtMost(repetitions)
                                },
                                valueRange = 1f..20f,
                                steps = 18
                            )
                            Button(
                                onClick = {
                                    if (playingThisRange) {
                                        player.toggle()
                                    } else {
                                        scope.launch {
                                            runCatching {
                                                val downloaded = withContext(Dispatchers.IO) {
                                                    SurahAudioPlayer.isSurahDownloaded(
                                                        context,
                                                        surahNumber,
                                                        prefs.reciterId,
                                                        detail.meta.ayahCount
                                                    )
                                                }
                                                val list = if (downloaded) {
                                                    (start..safeEnd).map { AyahAudio(it, "$surahNumber:$it", "") }
                                                } else {
                                                    QuranApi.ayahAudioList(prefs.reciterId, surahNumber)
                                                        .filter { it.ayahNumber in start..safeEnd }
                                                }
                                                check(list.isNotEmpty())
                                                player.playSurah(
                                                    surahNumber = surahNumber,
                                                    surahName = detail.meta.latinName,
                                                    reciterId = prefs.reciterId,
                                                    reciterName = Reciters.byId(prefs.reciterId).name,
                                                    ayahs = list,
                                                    startAyah = start,
                                                    useVerifiedDownload = downloaded
                                                )
                                                player.setRepeat(RepeatMode.SURAH)
                                            }.onFailure {
                                                snackbar.showSnackbar("Audio belum dapat diputar. Periksa koneksi atau unduh audio terlebih dahulu.")
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                shape = CircleShape,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colors.inverseOnSurface,
                                    contentColor = colors.inverseSurface
                                )
                            ) {
                                Icon(
                                    painter = painterResource(if (playingThisRange && player.isPlaying) R.drawable.ic_pause else R.drawable.ic_play),
                                    contentDescription = null,
                                    modifier = Modifier.size(19.dp)
                                )
                                Spacer(Modifier.width(9.dp))
                                Text(if (playingThisRange && player.isPlaying) "Jeda latihan" else "Putar rentang berulang")
                            }
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = { completed = (completed + 1).coerceAtMost(repetitions) },
                                enabled = completed < repetitions,
                                modifier = Modifier.fillMaxWidth().height(50.dp),
                                shape = CircleShape,
                                border = BorderStroke(1.dp, colors.inverseOnSurface.copy(alpha = .34f)),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = colors.inverseOnSurface,
                                    disabledContentColor = colors.inverseOnSurface.copy(alpha = .45f)
                                )
                            ) {
                                Icon(painterResource(R.drawable.ic_check), null, Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(if (completed >= repetitions) "Target sesi tercapai" else "Selesai satu pengulangan")
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = {
                            val now = System.currentTimeMillis()
                            prefs.saveMemorization(
                                MemorizationEntry(
                                    surah = surahNumber,
                                    surahName = detail.meta.latinName,
                                    startAyah = start,
                                    endAyah = safeEnd,
                                    status = status,
                                    reviewAt = now + TimeUnit.DAYS.toMillis(status.intervalDays.toLong()),
                                    updatedAt = now
                                )
                            )
                            scope.launch { snackbar.showSnackbar("Jadwal murajaah tersimpan") }
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = CircleShape
                    ) {
                        Icon(painterResource(R.drawable.ic_bookmark), null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Simpan sesi murajaah")
                    }
                    Spacer(Modifier.height(28.dp))
                }

                item(key = "saved-heading") {
                    Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.weight(1f)) {
                            Text("Daftar Hafalan", style = MaterialTheme.typography.headlineSmall)
                            Text("Ketuk untuk membuka kembali sesi", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                        }
                        Surface(shape = CircleShape, color = colors.surfaceVariant) {
                            Text(
                                prefs.memorizationEntries.size.toString(),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }

                if (prefs.memorizationEntries.isEmpty()) {
                    item(key = "saved-empty") {
                        EmptyMemorizationCard()
                    }
                } else {
                    items(prefs.memorizationEntries, key = { it.key }) { entry ->
                        SavedMemorizationCard(
                            entry = entry,
                            onOpen = { loadEntry(entry) },
                            onDelete = {
                                prefs.removeMemorization(entry.key)
                                scope.launch { snackbar.showSnackbar("Hafalan dihapus") }
                            }
                        )
                        Spacer(Modifier.height(9.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionEyebrow(label: String, subtitle: String) {
    val colors = MaterialTheme.colorScheme
    Text(
        label,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.3.sp,
        color = colors.onSurfaceVariant
    )
    Spacer(Modifier.height(3.dp))
    Text(subtitle, style = MaterialTheme.typography.titleLarge, color = colors.onBackground)
}

@Composable
private fun DueReviewCard(entries: List<MemorizationEntry>, onSelect: (MemorizationEntry) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(26.dp),
        color = colors.inverseSurface,
        contentColor = colors.inverseOnSurface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(vertical = 18.dp)) {
            Row(Modifier.padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(38.dp).background(colors.inverseOnSurface.copy(alpha = .12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(painterResource(R.drawable.ic_schedule), null, Modifier.size(19.dp))
                }
                Spacer(Modifier.width(11.dp))
                Column(Modifier.weight(1f)) {
                    Text("Murajaah hari ini", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("${entries.size} sesi siap diulang", style = MaterialTheme.typography.labelSmall, color = colors.inverseOnSurface.copy(alpha = .65f))
                }
            }
            Spacer(Modifier.height(10.dp))
            entries.forEachIndexed { index, entry ->
                if (index > 0) HorizontalDivider(Modifier.padding(horizontal = 18.dp), color = colors.inverseOnSurface.copy(alpha = .1f))
                Surface(onClick = { onSelect(entry) }, color = Color.Transparent) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("${entry.surahName} ${entry.startAyah}–${entry.endAyah}", fontWeight = FontWeight.Medium)
                            Text(entry.status.label, style = MaterialTheme.typography.labelSmall, color = colors.inverseOnSurface.copy(alpha = .62f))
                        }
                        Text("Mulai", style = MaterialTheme.typography.labelLarge)
                        Spacer(Modifier.width(5.dp))
                        Icon(painterResource(R.drawable.ic_arrow_forward), null, Modifier.size(14.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun RangeLabel(label: String, value: Int, total: Int) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.weight(1f))
        Text("$value / $total", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun PracticeToggle(label: String, hidden: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = if (hidden) colors.onBackground else colors.surfaceVariant,
        contentColor = if (hidden) colors.background else colors.onBackground,
        border = if (hidden) null else BorderStroke(1.dp, colors.outlineVariant)
    ) {
        Row(Modifier.padding(horizontal = 13.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(8.dp).background(
                    if (hidden) colors.background else colors.onSurfaceVariant.copy(alpha = .5f),
                    CircleShape
                )
            )
            Spacer(Modifier.width(8.dp))
            Column {
                Text(label, style = MaterialTheme.typography.labelLarge)
                Text(if (hidden) "Disembunyikan" else "Ditampilkan", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun PracticeAyahCard(
    ayah: Ayah,
    showArabic: Boolean,
    showTranslation: Boolean,
    onReveal: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onReveal,
        shape = RoundedCornerShape(22.dp),
        color = colors.surface,
        border = BorderStroke(1.dp, colors.outlineVariant),
        modifier = Modifier.fillMaxWidth().animateContentSize()
    ) {
        Column(Modifier.padding(17.dp)) {
            Surface(shape = CircleShape, color = colors.surfaceVariant) {
                Text(
                    "AYAT ${ayah.numberInSurah}",
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = .7.sp
                )
            }
            Spacer(Modifier.height(12.dp))
            if (showArabic) {
                Text(
                    ayah.arabic,
                    fontFamily = ArabicFontFamily,
                    fontSize = 27.sp,
                    lineHeight = 50.sp,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                HiddenAnswer("Teks Arab disembunyikan")
            }
            if (showTranslation) {
                Spacer(Modifier.height(9.dp))
                Text(ayah.translation, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            } else {
                Spacer(Modifier.height(9.dp))
                HiddenAnswer("Terjemahan disembunyikan")
            }
        }
    }
}

@Composable
private fun HiddenAnswer(text: String) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp)).background(colors.surfaceVariant).padding(14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
    }
}

@Composable
private fun QuizCard(
    currentAyah: Int,
    answer: String,
    answerVisible: Boolean,
    question: Int,
    total: Int,
    onToggleAnswer: () -> Unit,
    onNext: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = colors.surfaceVariant,
        border = BorderStroke(1.dp, colors.outlineVariant),
        modifier = Modifier.fillMaxWidth().animateContentSize()
    ) {
        Column(Modifier.padding(17.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("KUIS LANJUTKAN AYAT", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Spacer(Modifier.weight(1f))
                Text("$question/$total", style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
            }
            Spacer(Modifier.height(10.dp))
            Text("Apa lanjutan setelah ayat $currentAyah?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            AnimatedVisibility(answerVisible, enter = fadeIn(tween(180)), exit = fadeOut(tween(120))) {
                Text(
                    answer,
                    fontFamily = ArabicFontFamily,
                    fontSize = 25.sp,
                    lineHeight = 46.sp,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth().padding(top = 13.dp)
                )
            }
            if (!answerVisible) {
                Spacer(Modifier.height(13.dp))
                HiddenAnswer("Ingat sebelum membuka jawaban")
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = onToggleAnswer, modifier = Modifier.weight(1f), shape = CircleShape) {
                    Text(if (answerVisible) "Tutup jawaban" else "Lihat jawaban")
                }
                Button(onClick = onNext, modifier = Modifier.weight(1f), shape = CircleShape) {
                    Text("Berikutnya")
                }
            }
        }
    }
}

@Composable
private fun SavedMemorizationCard(entry: MemorizationEntry, onOpen: () -> Unit, onDelete: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val days = TimeUnit.MILLISECONDS.toDays((entry.reviewAt - System.currentTimeMillis()).coerceAtLeast(0))
    Surface(
        onClick = onOpen,
        shape = RoundedCornerShape(20.dp),
        color = colors.surfaceVariant,
        border = BorderStroke(1.dp, colors.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(start = 15.dp, end = 7.dp, top = 13.dp, bottom = 13.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(42.dp).background(colors.onBackground, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(entry.surah.toString(), style = MaterialTheme.typography.labelLarge, color = colors.background)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("${entry.surahName} ${entry.startAyah}–${entry.endAyah}", fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    "${entry.status.label} · ${if (entry.reviewAt <= System.currentTimeMillis()) "Murajaah hari ini" else "$days hari lagi"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant
                )
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(48.dp)) {
                Icon(painterResource(R.drawable.ic_delete), "Hapus hafalan", Modifier.size(18.dp), tint = colors.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun EmptyMemorizationCard() {
    val colors = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = colors.surfaceVariant,
        border = BorderStroke(1.dp, colors.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(painterResource(R.drawable.ic_book_open), null, Modifier.size(27.dp), tint = colors.onSurfaceVariant)
            Spacer(Modifier.height(9.dp))
            Text("Belum ada hafalan tersimpan", fontWeight = FontWeight.SemiBold)
            Text("Susun sesi di atas lalu simpan jadwalnya.", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant, textAlign = TextAlign.Center)
        }
    }
}
