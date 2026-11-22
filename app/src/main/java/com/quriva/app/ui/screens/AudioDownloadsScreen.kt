package com.quriva.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.quriva.app.data.*
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioDownloadsScreen(prefs: AppPreferences, onBack: () -> Unit) {
    val context = LocalContext.current
    val surahs = remember { QuranRepository.getSurahs(context) }
    val juzs = remember { QuranRepository.getJuzs(context) }
    var refresh by remember { mutableIntStateOf(0) }
    var selectedJuz by remember { mutableIntStateOf(30) }
    var expanded by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { while (true) { delay(3_000); refresh++ } }
    val downloaded = remember(refresh, prefs.reciterId) { surahs.filter { SurahAudioPlayer.isSurahDownloaded(context, it.number, prefs.reciterId, it.ayahCount) }.map { it.number }.toSet() }
    val used = remember(refresh) { SurahAudioPlayer.audioDir(context).walkTopDown().filter { it.isFile }.sumOf { it.length() } }
    val c = MaterialTheme.colorScheme
    Surface(Modifier.fillMaxSize(), color = c.background) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            FeatureTopBar("Audio Offline", "${downloaded.size} surah · ${formatStorage(used)} · sisa ${formatStorage(context.filesDir.usableSpace)}", onBack)
            Surface(shape = RoundedCornerShape(20.dp), color = c.surfaceVariant, border = BorderStroke(1.dp, c.outlineVariant), modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Column(Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) { Text("Hanya melalui Wi-Fi", fontWeight = FontWeight.SemiBold); Text("Berlaku untuk antrean baru", style = MaterialTheme.typography.labelSmall) }
                        Switch(prefs.audioWifiOnly, { prefs.updateAudioWifiOnly(it) })
                    }
                    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                        OutlinedTextField(value = "Juz $selectedJuz", onValueChange = {}, readOnly = true, label = { Text("Unduh satu juz") }, modifier = Modifier.menuAnchor().fillMaxWidth())
                        ExposedDropdownMenu(expanded, { expanded = false }) { juzs.forEach { j -> DropdownMenuItem(text = { Text("Juz ${j.number}") }, onClick = { selectedJuz = j.number; expanded = false }) } }
                    }
                    Spacer(Modifier.height(8.dp)); Button(onClick = {
                        val j = juzs.first { it.number == selectedJuz }
                        surahs.filter { it.number in j.startSurah..j.endSurah }.forEach { s -> AudioDownloadWorker.enqueue(context, s.number, s.latinName, s.ayahCount, prefs.reciterId, prefs.audioWifiOnly) }
                    }, modifier = Modifier.fillMaxWidth(), shape = CircleShape) { Text("Tambahkan Juz $selectedJuz ke antrean") }
                    TextButton(onClick = { AudioDownloadWorker.cancelAll(context) }, modifier = Modifier.align(Alignment.End)) { Text("Batalkan antrean") }
                }
            }
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(vertical = 10.dp)) {
                items(surahs, key = { it.number }) { s ->
                    val done = s.number in downloaded
                    Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) { Text("${s.number}. ${s.latinName}", fontWeight = FontWeight.SemiBold); Text("${s.ayahCount} ayat · ${if (done) "Tersedia offline" else "Belum diunduh"}", style = MaterialTheme.typography.labelSmall, color = c.onSurfaceVariant) }
                        if (done) OutlinedButton(onClick = { SurahAudioPlayer.deleteSurahDownload(context, s.number, prefs.reciterId); refresh++ }, shape = CircleShape) { Text("Hapus") }
                        else Button(onClick = { AudioDownloadWorker.enqueue(context, s.number, s.latinName, s.ayahCount, prefs.reciterId, prefs.audioWifiOnly) }, shape = CircleShape) { Text("Unduh") }
                    }
                    HorizontalDivider(Modifier.padding(horizontal = 18.dp), color = c.outlineVariant)
                }
            }
        }
    }
}

private fun formatStorage(bytes: Long): String = if (bytes >= 1024 * 1024) "%.1f MB".format(bytes / 1048576f) else "${bytes / 1024} KB"
