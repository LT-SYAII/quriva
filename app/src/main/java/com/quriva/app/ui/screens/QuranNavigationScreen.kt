package com.quriva.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quriva.app.R
import com.quriva.app.data.QuranNavigationRepository
import com.quriva.app.data.QuranRepository
import com.quriva.app.model.QuranNavPoint

private enum class NavKind(val label: String, val description: String) {
    HIZB("Hizb", "60 bagian bacaan"),
    RUB("Rub", "240 seperempat hizb"),
    MANZIL("Manzil", "7 bagian pekanan"),
    RUKU("Ruku", "Pembagian tema ayat"),
    SAJDAH("Sajdah", "Ayat-ayat sajdah")
}

@Composable
fun QuranNavigationScreen(
    onBack: () -> Unit,
    onOpenAyah: (Int, Int) -> Unit,
    onOpenPages: () -> Unit
) {
    val context = LocalContext.current
    var kind by remember { mutableStateOf(NavKind.HIZB) }
    var query by remember { mutableStateOf("") }
    val all = remember { QuranNavigationRepository.all(context) }
    val names = remember { QuranRepository.getSurahs(context).associate { it.number to it.latinName } }
    val rows = remember(kind, all) {
        when (kind) {
            NavKind.HIZB -> all.groupBy { it.hizb }.toSortedMap().map { it.key to it.value.first() }
            NavKind.RUB -> all.groupBy { it.rub }.toSortedMap().map { it.key to it.value.first() }
            NavKind.MANZIL -> all.groupBy { it.manzil }.toSortedMap().map { it.key to it.value.first() }
            NavKind.RUKU -> all.groupBy { it.ruku }.toSortedMap().map { it.key to it.value.first() }
            NavKind.SAJDAH -> all.filter { it.sajdah > 0 }.mapIndexed { index, point -> index + 1 to point }
        }.filter { it.first > 0 }
    }
    val visible = remember(rows, names, query) {
        val normalized = query.trim()
        rows.filter { (number, point) ->
            normalized.isBlank() || number.toString() == normalized ||
                names[point.surah].orEmpty().contains(normalized, true) ||
                "${point.surah}:${point.ayah}".contains(normalized)
        }
    }
    val colors = MaterialTheme.colorScheme

    Surface(Modifier.fillMaxSize(), color = colors.background) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            FeatureTopBar("Navigasi Al-Qur'an", "6.236 ayat · seluruh data offline", onBack)
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 34.dp)
            ) {
                item(key = "navigation-hero") {
                    Surface(
                        shape = RoundedCornerShape(25.dp),
                        color = colors.inverseSurface,
                        contentColor = colors.inverseOnSurface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(19.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    Modifier.size(42.dp).background(colors.inverseOnSurface.copy(alpha = .12f), RoundedCornerShape(14.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(painterResource(R.drawable.ic_navigation), null, Modifier.size(20.dp))
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text("Temukan titik bacaan", style = MaterialTheme.typography.titleLarge)
                                    Text("Hizb, rub, manzil, ruku, dan sajdah", style = MaterialTheme.typography.labelSmall, color = colors.inverseOnSurface.copy(alpha = .62f))
                                }
                            }
                            Spacer(Modifier.height(15.dp))
                            Button(
                                onClick = onOpenPages,
                                shape = CircleShape,
                                modifier = Modifier.fillMaxWidth().height(50.dp),
                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                    containerColor = colors.inverseOnSurface,
                                    contentColor = colors.inverseSurface
                                )
                            ) {
                                Icon(painterResource(R.drawable.ic_layers), null, Modifier.size(17.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Buka 604 halaman mushaf")
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }

                item(key = "navigation-controls") {
                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        NavKind.entries.forEach { item ->
                            FilterChip(
                                selected = kind == item,
                                onClick = { kind = item; query = "" },
                                label = { Text(item.label) }
                            )
                        }
                    }
                    Spacer(Modifier.height(9.dp))
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = { Text("Cari nomor, surah, atau 2:255") },
                        leadingIcon = { Icon(painterResource(R.drawable.ic_search), null, Modifier.size(19.dp)) },
                        trailingIcon = {
                            AnimatedVisibility(query.isNotEmpty()) {
                                IconButton(onClick = { query = "" }) {
                                    Icon(painterResource(R.drawable.ic_close), "Hapus pencarian", Modifier.size(18.dp))
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(20.dp))
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                        Column(Modifier.weight(1f)) {
                            Text(kind.label, style = MaterialTheme.typography.headlineSmall)
                            Text(kind.description, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                        }
                        Surface(shape = CircleShape, color = colors.surfaceVariant) {
                            Text(visible.size.toString(), Modifier.padding(horizontal = 10.dp, vertical = 6.dp), style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    Spacer(Modifier.height(11.dp))
                }

                if (visible.isEmpty()) {
                    item(key = "navigation-empty") {
                        Column(Modifier.fillMaxWidth().padding(vertical = 46.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(painterResource(R.drawable.ic_search), null, Modifier.size(29.dp), tint = colors.onSurfaceVariant)
                            Spacer(Modifier.height(9.dp))
                            Text("Titik bacaan tidak ditemukan", fontWeight = FontWeight.SemiBold)
                            Text("Coba nomor atau nama surah lain.", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                        }
                    }
                } else {
                    items(visible, key = { "${kind.name}-${it.first}" }) { (number, point) ->
                        NavCard(
                            kind = kind,
                            number = number,
                            point = point,
                            surahName = names[point.surah] ?: "Surah ${point.surah}",
                            onClick = { onOpenAyah(point.surah, point.ayah) }
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun NavCard(
    kind: NavKind,
    number: Int,
    point: QuranNavPoint,
    surahName: String,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = colors.surfaceVariant,
        border = BorderStroke(1.dp, colors.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = colors.background,
                border = BorderStroke(1.dp, colors.outlineVariant),
                modifier = Modifier.size(45.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(number.toString(), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("${kind.label} $number", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("$surahName · ${point.surah}:${point.ayah}", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("Halaman ${point.page} · Juz ${point.juz}", style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
            }
            Spacer(Modifier.width(7.dp))
            Icon(painterResource(R.drawable.ic_arrow_forward), "Buka ayat", Modifier.size(16.dp), tint = colors.onSurfaceVariant)
        }
    }
}
