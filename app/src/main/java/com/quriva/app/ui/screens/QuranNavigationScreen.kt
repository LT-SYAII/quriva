package com.quriva.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.quriva.app.R
import com.quriva.app.data.QuranNavigationRepository
import com.quriva.app.data.QuranRepository
import com.quriva.app.model.QuranNavPoint

private enum class NavKind(val label: String) { HIZB("Hizb"), RUB("Rub"), MANZIL("Manzil"), RUKU("Ruku"), SAJDAH("Sajdah") }

@Composable
fun QuranNavigationScreen(onBack: () -> Unit, onOpenAyah: (Int, Int) -> Unit, onOpenPages: () -> Unit) {
    val context = LocalContext.current
    var kind by remember { mutableStateOf(NavKind.HIZB) }
    val all = remember { QuranNavigationRepository.all(context) }
    val names = remember { QuranRepository.getSurahs(context).associate { it.number to it.latinName } }
    val rows = remember(kind, all) {
        when (kind) {
            NavKind.HIZB -> all.groupBy { it.hizb }.toSortedMap().map { it.key to it.value.first() }
            NavKind.RUB -> all.groupBy { it.rub }.toSortedMap().map { it.key to it.value.first() }
            NavKind.MANZIL -> all.groupBy { it.manzil }.toSortedMap().map { it.key to it.value.first() }
            NavKind.RUKU -> all.groupBy { it.ruku }.toSortedMap().map { it.key to it.value.first() }
            NavKind.SAJDAH -> all.filter { it.sajdah > 0 }.mapIndexed { i, p -> i + 1 to p }
        }.filter { it.first > 0 }
    }
    val c = MaterialTheme.colorScheme
    Surface(Modifier.fillMaxSize(), color = c.background) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            FeatureTopBar("Navigasi Lengkap", "Offline · ${all.size} ayat", onBack)
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onOpenPages, shape = CircleShape, modifier = Modifier.weight(1f)) {
                    Icon(painterResource(R.drawable.ic_layers), null, Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text("604 Halaman")
                }
            }
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NavKind.entries.forEach { item -> FilterChip(selected = kind == item, onClick = { kind = item }, label = { Text(item.label) }) }
            }
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 28.dp)) {
                items(rows, key = { "${kind.name}-${it.first}" }) { (number, point) ->
                    NavRow(kind, number, point, names[point.surah] ?: "Surah ${point.surah}") { onOpenAyah(point.surah, point.ayah) }
                }
            }
        }
    }
}

@Composable
private fun NavRow(kind: NavKind, number: Int, p: QuranNavPoint, surahName: String, onClick: () -> Unit) {
    val c = MaterialTheme.colorScheme
    Surface(onClick = onClick, color = c.background, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(horizontal = 20.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = c.surfaceVariant, border = BorderStroke(1.dp, c.outlineVariant), modifier = Modifier.size(42.dp)) {
                Box(contentAlignment = Alignment.Center) { Text(number.toString(), style = MaterialTheme.typography.labelLarge) }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("${kind.label} $number", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("$surahName · ${p.surah}:${p.ayah} · Hal. ${p.page} · Juz ${p.juz}", style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant)
            }
            Icon(painterResource(R.drawable.ic_arrow_forward), "Buka", Modifier.size(17.dp))
        }
    }
    HorizontalDivider(Modifier.padding(start = 76.dp, end = 20.dp), color = c.outlineVariant)
}
