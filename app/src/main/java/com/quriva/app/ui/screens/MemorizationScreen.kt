package com.quriva.app.ui.screens

import android.app.Application
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quriva.app.data.*
import com.quriva.app.model.MemorizationEntry
import com.quriva.app.model.MemorizationStatus
import com.quriva.app.ui.theme.ArabicFontFamily
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemorizationScreen(prefs: AppPreferences, player: SurahAudioPlayer, onBack: () -> Unit) {
    val context = LocalContext.current
    val surahs = remember { QuranRepository.getSurahs(context) }
    var surahNumber by rememberSaveable { mutableIntStateOf(1) }
    val detail = remember(surahNumber) { QuranRepository.getSurah(context, surahNumber) }
    var start by rememberSaveable(surahNumber) { mutableIntStateOf(1) }
    var end by rememberSaveable(surahNumber) { mutableIntStateOf(minOf(5, detail.meta.ayahCount)) }
    var status by remember { mutableStateOf(MemorizationStatus.LEARNING) }
    var hideArabic by remember { mutableStateOf(false) }
    var hideTranslation by remember { mutableStateOf(false) }
    var revealed by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    var repetitions by remember { mutableIntStateOf(3) }
    var completed by remember { mutableIntStateOf(0) }
    var quizIndex by remember { mutableIntStateOf(0) }
    var quizReveal by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val c = MaterialTheme.colorScheme
    val range = detail.ayahs.filter { it.numberInSurah in start..end }

    Surface(Modifier.fillMaxSize(), color = c.background) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            FeatureTopBar("Hafalan & Murajaah", "Rentang ayat · audio berulang", onBack)
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp)) {
                val due = prefs.memorizationEntries.filter { it.reviewAt <= System.currentTimeMillis() }
                if (due.isNotEmpty()) {
                    Surface(shape = RoundedCornerShape(20.dp), color = c.inverseSurface, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) { Text("Murajaah hari ini", color = c.inverseOnSurface, fontWeight = FontWeight.Bold); due.take(4).forEach { Text("• ${it.surahName} ${it.startAyah}–${it.endAyah} · ${it.status.label}", color = c.inverseOnSurface.copy(alpha=.75f), style = MaterialTheme.typography.bodySmall) } }
                    }
                    Spacer(Modifier.height(14.dp))
                }
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(value = detail.meta.latinName, onValueChange = {}, readOnly = true, label = { Text("Surah") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) }, modifier = Modifier.menuAnchor().fillMaxWidth())
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        surahs.forEach { s -> DropdownMenuItem(text = { Text("${s.number}. ${s.latinName}") }, onClick = { surahNumber = s.number; expanded = false }) }
                    }
                }
                Spacer(Modifier.height(14.dp)); Text("Mulai ayat $start")
                Slider(value = start.toFloat(), onValueChange = { start = it.toInt().coerceAtMost(end) }, valueRange = 1f..detail.meta.ayahCount.toFloat())
                Text("Sampai ayat $end")
                Slider(value = end.toFloat(), onValueChange = { end = it.toInt().coerceAtLeast(start) }, valueRange = 1f..detail.meta.ayahCount.toFloat())
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MemorizationStatus.entries.forEach { s -> FilterChip(selected = status == s, onClick = { status = s }, label = { Text(s.label) }) }
                }
                Row(verticalAlignment = Alignment.CenterVertically) { Text("Sembunyikan Arab", Modifier.weight(1f)); Switch(hideArabic, { hideArabic = it; revealed = false }) }
                Row(verticalAlignment = Alignment.CenterVertically) { Text("Sembunyikan terjemahan", Modifier.weight(1f)); Switch(hideTranslation, { hideTranslation = it; revealed = false }) }
                Surface(onClick = { revealed = !revealed }, shape = RoundedCornerShape(22.dp), color = c.surfaceVariant, border = BorderStroke(1.dp, c.outlineVariant), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp)) {
                        range.forEach { ayah ->
                            Text("Ayat ${ayah.numberInSurah}", style = MaterialTheme.typography.labelSmall)
                            if (!hideArabic || revealed) Text(ayah.arabic, fontFamily = ArabicFontFamily, fontSize = 27.sp, lineHeight = 50.sp, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth()) else Text("••••••••", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                            if (!hideTranslation || revealed) Text(ayah.translation, style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant)
                            Spacer(Modifier.height(12.dp))
                        }
                        if (hideArabic || hideTranslation) Text(if (revealed) "Ketuk untuk sembunyikan" else "Ketuk untuk ungkap jawaban", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                    }
                }
                if (range.size > 1) {
                    Spacer(Modifier.height(12.dp))
                    val current = range[quizIndex % (range.size - 1)]
                    val next = range[(quizIndex % (range.size - 1)) + 1]
                    Surface(shape = RoundedCornerShape(18.dp), color = c.surfaceVariant, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp)) {
                            Text("Kuis lanjutkan ayat", fontWeight = FontWeight.Bold)
                            Text("Apa lanjutan setelah ayat ${current.numberInSurah}?")
                            if (quizReveal) Text(next.arabic, fontFamily = ArabicFontFamily, fontSize = 25.sp, lineHeight = 46.sp, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth())
                            Row { TextButton(onClick = { quizReveal = !quizReveal }) { Text(if (quizReveal) "Sembunyikan" else "Lihat jawaban") }; TextButton(onClick = { quizIndex++; quizReveal = false }) { Text("Soal berikutnya") } }
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
                Text("Target pengulangan: $repetitions× · selesai $completed×")
                Slider(value = repetitions.toFloat(), onValueChange = { repetitions = it.toInt() }, valueRange = 1f..20f, steps = 18)
                Button(onClick = {
                    scope.launch {
                        runCatching {
                            val list = QuranApi.ayahAudioList(prefs.reciterId, surahNumber).filter { it.ayahNumber in start..end }
                            player.playSurah(surahNumber, detail.meta.latinName, prefs.reciterId, Reciters.byId(prefs.reciterId).name, list, start)
                            player.setRepeat(RepeatMode.SURAH)
                        }
                    }
                }, modifier = Modifier.fillMaxWidth(), shape = CircleShape) { Text("Putar rentang berulang") }
                OutlinedButton(onClick = { completed = (completed + 1).coerceAtMost(repetitions) }, modifier = Modifier.fillMaxWidth(), shape = CircleShape) { Text("Tandai satu pengulangan") }
                Button(onClick = {
                    val now = System.currentTimeMillis()
                    prefs.saveMemorization(MemorizationEntry(surahNumber, detail.meta.latinName, start, end, status, now + TimeUnit.DAYS.toMillis(status.intervalDays.toLong()), now))
                }, modifier = Modifier.fillMaxWidth(), shape = CircleShape) { Text("Simpan jadwal murajaah") }
                Spacer(Modifier.height(20.dp)); Text("Daftar Hafalan", style = MaterialTheme.typography.headlineSmall)
                prefs.memorizationEntries.forEach { entry ->
                    Surface(shape = RoundedCornerShape(16.dp), color = c.surfaceVariant, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) { Text("${entry.surahName} ${entry.startAyah}–${entry.endAyah}", fontWeight = FontWeight.SemiBold); Text(entry.status.label, style = MaterialTheme.typography.labelSmall) }
                            TextButton(onClick = { prefs.removeMemorization(entry.key) }) { Text("Hapus") }
                        }
                    }
                }
                Spacer(Modifier.height(30.dp))
            }
        }
    }
}
