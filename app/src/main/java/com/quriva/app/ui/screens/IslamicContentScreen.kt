package com.quriva.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quriva.app.R
import com.quriva.app.data.AppPreferences
import com.quriva.app.data.IslamicContentRepository
import com.quriva.app.model.AsmaName
import com.quriva.app.model.HadithItem
import com.quriva.app.model.IslamicContentItem
import com.quriva.app.ui.theme.ArabicFontFamily

private enum class ContentTab(val label: String) { DUA("Doa & Dzikir"), ASMA("Asmaul Husna"), HADITH("Arbain") }

@Composable
fun IslamicContentScreen(prefs: AppPreferences, onBack: () -> Unit) {
    val context = LocalContext.current
    var tab by remember { mutableStateOf(ContentTab.DUA) }
    var query by remember { mutableStateOf("") }
    var selectedDua by remember { mutableStateOf<IslamicContentItem?>(null) }
    var selectedAsma by remember { mutableStateOf<AsmaName?>(null) }
    var selectedHadith by remember { mutableStateOf<HadithItem?>(null) }
    val duas = remember { IslamicContentRepository.content(context) }
    val asma = remember { IslamicContentRepository.asmaulHusna(context) }
    val hadith = remember { IslamicContentRepository.arbain(context) }
    val colors = MaterialTheme.colorScheme

    Surface(Modifier.fillMaxSize(), color = colors.background) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            FeatureTopBar("Koleksi Islami", "Offline · bersumber", onBack)
            Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ContentTab.entries.forEach { item ->
                    FilterChip(selected = tab == item, onClick = { tab = item }, label = { Text(item.label) })
                }
            }
            OutlinedTextField(
                value = query, onValueChange = { query = it },
                placeholder = { Text("Cari judul, arti, atau sumber") },
                leadingIcon = { Icon(painterResource(R.drawable.ic_search), null) },
                singleLine = true, shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)
            )
            val q = query.trim()
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 28.dp)) {
                when (tab) {
                    ContentTab.DUA -> {
                        val filtered = duas.filter { q.isBlank() || it.title.contains(q, true) || it.translation.contains(q, true) || it.source.contains(q, true) }
                        val grouped = filtered.groupBy { it.categoryName }
                        grouped.forEach { (category, entries) ->
                            item(key = "h-$category") { SectionLabel(category, entries.size) }
                            items(entries, key = { it.id }) { item ->
                                ContentRow(item.title, item.translation, item.arabic, item.id in prefs.contentFavorites,
                                    onFavorite = { prefs.toggleContentFavorite(item.id) }, onClick = { selectedDua = item })
                            }
                        }
                    }
                    ContentTab.ASMA -> items(asma.filter { q.isBlank() || it.latin.contains(q, true) || it.meaning.contains(q, true) }, key = { "asma-${it.number}" }) { item ->
                        ContentRow("${item.number}. ${item.latin}", item.meaning, item.arabic,
                            "asma-${item.number}" in prefs.contentFavorites,
                            onFavorite = { prefs.toggleContentFavorite("asma-${item.number}") }, onClick = { selectedAsma = item })
                    }
                    ContentTab.HADITH -> items(hadith.filter { q.isBlank() || it.title.contains(q, true) || it.translationEnglish.contains(q, true) }, key = { "hadith-${it.number}" }) { item ->
                        ContentRow(item.title, "Terjemahan Inggris · ${item.source}", item.arabic,
                            "hadith-${item.number}" in prefs.contentFavorites,
                            onFavorite = { prefs.toggleContentFavorite("hadith-${item.number}") }, onClick = { selectedHadith = item })
                    }
                }
            }
        }
    }

    selectedDua?.let { DuaDialog(it, onDismiss = { selectedDua = null }) }
    selectedAsma?.let { AsmaDialog(it, onDismiss = { selectedAsma = null }) }
    selectedHadith?.let { HadithDialog(it, onDismiss = { selectedHadith = null }) }
}

@Composable
private fun ContentRow(title: String, subtitle: String, arabic: String, favorite: Boolean, onFavorite: () -> Unit, onClick: () -> Unit) {
    val c = MaterialTheme.colorScheme
    Surface(onClick = onClick, color = c.background, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(start = 20.dp, end = 8.dp, top = 13.dp, bottom = 13.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                if (arabic.isNotBlank()) Text(arabic, fontFamily = ArabicFontFamily, fontSize = 22.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.End)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            IconButton(onClick = onFavorite) {
                Icon(painterResource(if (favorite) R.drawable.ic_bookmark_filled else R.drawable.ic_bookmark), "Simpan", tint = c.onBackground)
            }
        }
    }
    HorizontalDivider(Modifier.padding(horizontal = 20.dp), color = c.outlineVariant)
}

@Composable private fun SectionLabel(text: String, count: Int) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)) {
        Text(text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.weight(1f)); Text("$count", style = MaterialTheme.typography.labelSmall)
    }
}

@Composable private fun DuaDialog(item: IslamicContentItem, onDismiss: () -> Unit) {
    var count by remember(item.id) { mutableIntStateOf(0) }
    AlertDialog(onDismissRequest = onDismiss, confirmButton = { TextButton(onClick = onDismiss) { Text("Tutup") } },
        title = { Text(item.title) }, text = {
            Column(Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState())) {
                Text(item.arabic, fontFamily = ArabicFontFamily, fontSize = 27.sp, lineHeight = 48.sp, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth())
                if (item.latin.isNotBlank()) { Spacer(Modifier.height(10.dp)); Text(item.latin, style = MaterialTheme.typography.bodySmall) }
                Spacer(Modifier.height(10.dp)); Text(item.translation)
                if (item.benefit.isNotBlank()) { Spacer(Modifier.height(12.dp)); Text("Faedah", fontWeight = FontWeight.Bold); Text(item.benefit, style = MaterialTheme.typography.bodySmall) }
                if (item.source.isNotBlank()) { Spacer(Modifier.height(10.dp)); Text("Sumber: ${item.source}", style = MaterialTheme.typography.labelSmall) }
                Spacer(Modifier.height(14.dp)); Text("Target ${item.repeat}× · Selesai $count×", style = MaterialTheme.typography.labelLarge)
                Button(onClick = { count = (count + 1).coerceAtMost(item.repeat) }, modifier = Modifier.fillMaxWidth(), shape = CircleShape) { Text(if (count >= item.repeat) "Selesai" else "Hitung bacaan") }
            }
        })
}

@Composable private fun AsmaDialog(item: AsmaName, onDismiss: () -> Unit) = AlertDialog(
    onDismissRequest = onDismiss, confirmButton = { TextButton(onClick = onDismiss) { Text("Tutup") } },
    title = { Text(item.latin) }, text = { Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text(item.arabic, fontFamily = ArabicFontFamily, fontSize = 44.sp); Spacer(Modifier.height(12.dp)); Text(item.meaning, textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp)); Text("Sumber data: Asmaul Husna API (ekel.dev)", style = MaterialTheme.typography.labelSmall)
    } })

@Composable private fun HadithDialog(item: HadithItem, onDismiss: () -> Unit) = AlertDialog(
    onDismissRequest = onDismiss, confirmButton = { TextButton(onClick = onDismiss) { Text("Tutup") } },
    title = { Text(item.title) }, text = { Column(Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState())) {
        Text(item.arabic, fontFamily = ArabicFontFamily, fontSize = 24.sp, lineHeight = 43.sp, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(14.dp)); Text("Terjemahan Inggris", fontWeight = FontWeight.Bold); Text(item.translationEnglish, style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(10.dp)); Text("Sumber: Hadith API · Unlicense", style = MaterialTheme.typography.labelSmall)
    } })

@Composable
internal fun FeatureTopBar(title: String, subtitle: String, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(58.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) { Icon(painterResource(R.drawable.ic_back), "Kembali", Modifier.size(22.dp)) }
        Column(Modifier.weight(1f)) { Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold); Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}
