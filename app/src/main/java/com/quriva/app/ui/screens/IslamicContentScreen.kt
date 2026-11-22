package com.quriva.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
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
import com.quriva.app.data.IslamicContentRepository
import com.quriva.app.model.AsmaName
import com.quriva.app.model.HadithItem
import com.quriva.app.model.IslamicContentItem
import com.quriva.app.ui.theme.ArabicFontFamily

private enum class ContentTab(val label: String) {
    DUA("Doa & Dzikir"),
    ASMA("Asmaul Husna"),
    HADITH("Arbain")
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun IslamicContentScreen(prefs: AppPreferences, onBack: () -> Unit) {
    val context = LocalContext.current
    var tab by remember { mutableStateOf(ContentTab.DUA) }
    var query by remember { mutableStateOf("") }
    var favoritesOnly by remember { mutableStateOf(false) }
    var selectedDua by remember { mutableStateOf<IslamicContentItem?>(null) }
    var selectedAsma by remember { mutableStateOf<AsmaName?>(null) }
    var selectedHadith by remember { mutableStateOf<HadithItem?>(null) }
    val duas = remember { IslamicContentRepository.content(context) }
    val asma = remember { IslamicContentRepository.asmaulHusna(context) }
    val hadith = remember { IslamicContentRepository.arbain(context) }
    val colors = MaterialTheme.colorScheme
    val q = query.trim()

    val filteredDua = remember(duas, q, favoritesOnly, prefs.contentFavorites) {
        duas.filter { item ->
            (!favoritesOnly || item.id in prefs.contentFavorites) &&
                (q.isBlank() || item.title.contains(q, true) || item.translation.contains(q, true) ||
                    item.source.contains(q, true) || item.categoryName.contains(q, true))
        }
    }
    val filteredAsma = remember(asma, q, favoritesOnly, prefs.contentFavorites) {
        asma.filter { item ->
            (!favoritesOnly || "asma-${item.number}" in prefs.contentFavorites) &&
                (q.isBlank() || item.latin.contains(q, true) || item.meaning.contains(q, true) || item.number.toString() == q)
        }
    }
    val filteredHadith = remember(hadith, q, favoritesOnly, prefs.contentFavorites) {
        hadith.filter { item ->
            (!favoritesOnly || "hadith-${item.number}" in prefs.contentFavorites) &&
                (q.isBlank() || item.title.contains(q, true) || item.translationEnglish.contains(q, true) || item.number.toString() == q)
        }
    }
    val resultCount = when (tab) {
        ContentTab.DUA -> filteredDua.size
        ContentTab.ASMA -> filteredAsma.size
        ContentTab.HADITH -> filteredHadith.size
    }

    Surface(Modifier.fillMaxSize(), color = colors.background) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            FeatureTopBar("Koleksi Islami", "Bacaan tepercaya · tersedia offline", onBack)
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 36.dp)
            ) {
                item(key = "content-hero") {
                    ContentOverviewCard(
                        total = duas.size + asma.size + hadith.size,
                        favorites = prefs.contentFavorites.size
                    )
                    Spacer(Modifier.height(16.dp))
                }

                item(key = "content-search") {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = { Text("Cari judul, arti, atau sumber") },
                        leadingIcon = { Icon(painterResource(R.drawable.ic_search), null, Modifier.size(19.dp)) },
                        trailingIcon = {
                            AnimatedVisibility(query.isNotEmpty(), enter = fadeIn(), exit = fadeOut()) {
                                IconButton(onClick = { query = "" }) {
                                    Icon(painterResource(R.drawable.ic_close), "Hapus pencarian", Modifier.size(18.dp))
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(19.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        ContentTab.entries.forEach { item ->
                            FilterChip(
                                selected = tab == item,
                                onClick = { tab = item },
                                label = { Text(item.label) }
                            )
                        }
                        FilterChip(
                            selected = favoritesOnly,
                            onClick = { favoritesOnly = !favoritesOnly },
                            leadingIcon = {
                                Icon(
                                    painterResource(if (favoritesOnly) R.drawable.ic_bookmark_filled else R.drawable.ic_bookmark),
                                    null,
                                    Modifier.size(16.dp)
                                )
                            },
                            label = { Text("Tersimpan") }
                        )
                    }
                    Spacer(Modifier.height(22.dp))
                }

                item(key = "result-heading") {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                        Column(Modifier.weight(1f)) {
                            Text(tab.label, style = MaterialTheme.typography.headlineSmall)
                            Text(
                                if (favoritesOnly) "Koleksi yang kamu simpan" else "Ketuk kartu untuk membaca lengkap",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.onSurfaceVariant
                            )
                        }
                        Surface(shape = CircleShape, color = colors.surfaceVariant) {
                            Text(
                                resultCount.toString(),
                                Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                    Spacer(Modifier.height(11.dp))
                }

                when (tab) {
                    ContentTab.DUA -> {
                        val grouped = filteredDua.groupBy { it.categoryName }
                        grouped.forEach { (category, entries) ->
                            item(key = "section-$category") {
                                ContentSectionLabel(category, entries.size)
                            }
                            items(entries, key = { it.id }) { item ->
                                ContentCard(
                                    title = item.title,
                                    subtitle = item.translation,
                                    arabic = item.arabic,
                                    badge = if (item.repeat > 1) "${item.repeat}×" else null,
                                    favorite = item.id in prefs.contentFavorites,
                                    onFavorite = { prefs.toggleContentFavorite(item.id) },
                                    onClick = { selectedDua = item }
                                )
                                Spacer(Modifier.height(9.dp))
                            }
                        }
                    }

                    ContentTab.ASMA -> items(filteredAsma, key = { "asma-${it.number}" }) { item ->
                        ContentCard(
                            title = item.latin,
                            subtitle = item.meaning,
                            arabic = item.arabic,
                            badge = item.number.toString().padStart(2, '0'),
                            favorite = "asma-${item.number}" in prefs.contentFavorites,
                            onFavorite = { prefs.toggleContentFavorite("asma-${item.number}") },
                            onClick = { selectedAsma = item }
                        )
                        Spacer(Modifier.height(9.dp))
                    }

                    ContentTab.HADITH -> items(filteredHadith, key = { "hadith-${it.number}" }) { item ->
                        ContentCard(
                            title = item.title,
                            subtitle = "Terjemahan Inggris · ${item.source}",
                            arabic = item.arabic,
                            badge = item.number.toString().padStart(2, '0'),
                            favorite = "hadith-${item.number}" in prefs.contentFavorites,
                            onFavorite = { prefs.toggleContentFavorite("hadith-${item.number}") },
                            onClick = { selectedHadith = item }
                        )
                        Spacer(Modifier.height(9.dp))
                    }
                }

                if (resultCount == 0) {
                    item(key = "content-empty") {
                        ContentEmptyState(favoritesOnly = favoritesOnly, query = query)
                    }
                }
            }
        }
    }

    selectedDua?.let { item ->
        DuaSheet(
            item = item,
            favorite = item.id in prefs.contentFavorites,
            onFavorite = { prefs.toggleContentFavorite(item.id) },
            onDismiss = { selectedDua = null }
        )
    }
    selectedAsma?.let { item ->
        AsmaSheet(
            item = item,
            favorite = "asma-${item.number}" in prefs.contentFavorites,
            onFavorite = { prefs.toggleContentFavorite("asma-${item.number}") },
            onDismiss = { selectedAsma = null }
        )
    }
    selectedHadith?.let { item ->
        HadithSheet(
            item = item,
            favorite = "hadith-${item.number}" in prefs.contentFavorites,
            onFavorite = { prefs.toggleContentFavorite("hadith-${item.number}") },
            onDismiss = { selectedHadith = null }
        )
    }
}

@Composable
private fun ContentOverviewCard(total: Int, favorites: Int) {
    val colors = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(27.dp),
        color = colors.inverseSurface,
        contentColor = colors.inverseOnSurface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(Modifier.fillMaxWidth()) {
            Box(
                Modifier.size(180.dp).align(Alignment.TopEnd).background(
                    Brush.radialGradient(listOf(colors.inverseOnSurface.copy(alpha = .13f), Color.Transparent)),
                    CircleShape
                )
            )
            Column(Modifier.padding(20.dp)) {
                Text("PUSTAKA OFFLINE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp, color = colors.inverseOnSurface.copy(alpha = .62f))
                Spacer(Modifier.height(8.dp))
                Text("$total bacaan", style = MaterialTheme.typography.headlineMedium)
                Text("Doa, dzikir, nama Allah, dan hadis pilihan.", style = MaterialTheme.typography.bodySmall, color = colors.inverseOnSurface.copy(alpha = .65f))
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OverviewPill("97", "doa & dzikir", Modifier.weight(1f))
                    OverviewPill("99", "asmaul husna", Modifier.weight(1f))
                    OverviewPill(favorites.toString(), "tersimpan", Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun OverviewPill(value: String, label: String, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Surface(modifier = modifier, shape = RoundedCornerShape(15.dp), color = colors.inverseOnSurface.copy(alpha = .1f)) {
        Column(Modifier.padding(10.dp)) {
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(label, style = MaterialTheme.typography.labelSmall, color = colors.inverseOnSurface.copy(alpha = .6f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun ContentCard(
    title: String,
    subtitle: String,
    arabic: String,
    badge: String?,
    favorite: Boolean,
    onFavorite: () -> Unit,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(21.dp),
        color = colors.surfaceVariant,
        border = BorderStroke(1.dp, colors.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(start = 15.dp, end = 6.dp, top = 13.dp, bottom = 13.dp), verticalAlignment = Alignment.CenterVertically) {
            if (badge != null) {
                Surface(
                    modifier = Modifier.size(39.dp),
                    shape = RoundedCornerShape(13.dp),
                    color = colors.background,
                    border = BorderStroke(1.dp, colors.outlineVariant)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(badge, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.width(11.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (arabic.isNotBlank()) {
                    Text(
                        arabic,
                        fontFamily = ArabicFontFamily,
                        fontSize = 21.sp,
                        lineHeight = 31.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.End
                    )
                }
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            IconButton(onClick = onFavorite, modifier = Modifier.size(48.dp)) {
                Icon(
                    painterResource(if (favorite) R.drawable.ic_bookmark_filled else R.drawable.ic_bookmark),
                    if (favorite) "Hapus dari tersimpan" else "Simpan bacaan",
                    Modifier.size(19.dp),
                    tint = colors.onBackground
                )
            }
        }
    }
}

@Composable
private fun ContentSectionLabel(text: String, count: Int) {
    Row(Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, top = 13.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.weight(1f))
        Text("$count bacaan", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ContentEmptyState(favoritesOnly: Boolean, query: String) {
    val colors = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth().padding(vertical = 48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(painterResource(R.drawable.ic_book_open), null, Modifier.size(31.dp), tint = colors.onSurfaceVariant)
        Spacer(Modifier.height(10.dp))
        Text(
            if (favoritesOnly) "Belum ada bacaan tersimpan" else "Bacaan tidak ditemukan",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            if (query.isNotBlank()) "Coba gunakan kata kunci lain." else "Simpan bacaan agar mudah dibuka kembali.",
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant
        )
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun DuaSheet(
    item: IslamicContentItem,
    favorite: Boolean,
    onFavorite: () -> Unit,
    onDismiss: () -> Unit
) {
    var count by remember(item.id) { mutableIntStateOf(0) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val colors = MaterialTheme.colorScheme
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.background
    ) {
        Column(
            Modifier.fillMaxWidth().heightIn(max = 700.dp).verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 34.dp)
        ) {
            DetailHeader(item.title, item.categoryName, favorite, onFavorite)
            Spacer(Modifier.height(18.dp))
            Surface(shape = RoundedCornerShape(23.dp), color = colors.surfaceVariant, modifier = Modifier.fillMaxWidth()) {
                Text(
                    item.arabic,
                    fontFamily = ArabicFontFamily,
                    fontSize = 29.sp,
                    lineHeight = 52.sp,
                    textAlign = TextAlign.End,
                    modifier = Modifier.padding(18.dp).fillMaxWidth()
                )
            }
            if (item.latin.isNotBlank()) {
                Spacer(Modifier.height(15.dp))
                DetailLabel("LATIN")
                Text(item.latin, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            }
            Spacer(Modifier.height(15.dp))
            DetailLabel("ARTI")
            Text(item.translation, style = MaterialTheme.typography.bodyLarge)
            if (item.benefit.isNotBlank()) {
                Spacer(Modifier.height(17.dp))
                Surface(shape = RoundedCornerShape(18.dp), color = colors.surfaceVariant, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(15.dp)) {
                        DetailLabel("FAEDAH")
                        Text(item.benefit, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Penghitung bacaan", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("Target ${item.repeat}×", style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                }
                Text("$count/${item.repeat}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(9.dp))
            LinearProgressIndicator(
                progress = { count.toFloat() / item.repeat.coerceAtLeast(1) },
                modifier = Modifier.fillMaxWidth().height(5.dp),
                color = colors.onBackground,
                trackColor = colors.outlineVariant
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = { count = (count + 1).coerceAtMost(item.repeat) },
                enabled = count < item.repeat,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = CircleShape
            ) {
                Icon(painterResource(R.drawable.ic_check), null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (count >= item.repeat) "Target selesai" else "Hitung satu bacaan")
            }
            if (count > 0) {
                TextButton(onClick = { count = 0 }, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("Ulangi hitungan") }
            }
            if (item.source.isNotBlank()) {
                Spacer(Modifier.height(14.dp))
                HorizontalDivider(color = colors.outlineVariant)
                Spacer(Modifier.height(11.dp))
                Text("Sumber · ${item.source}", style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
            }
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun AsmaSheet(
    item: AsmaName,
    favorite: Boolean,
    onFavorite: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val colors = MaterialTheme.colorScheme
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = colors.background) {
        Column(Modifier.fillMaxWidth().padding(start = 22.dp, end = 22.dp, bottom = 38.dp)) {
            DetailHeader(item.latin, "Asmaul Husna · ${item.number} dari 99", favorite, onFavorite)
            Spacer(Modifier.height(22.dp))
            Surface(shape = RoundedCornerShape(26.dp), color = colors.inverseSurface, contentColor = colors.inverseOnSurface, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(27.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(item.arabic, fontFamily = ArabicFontFamily, fontSize = 52.sp, lineHeight = 72.sp, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(9.dp))
                    Text(item.latin, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(6.dp))
                    Text(item.meaning, style = MaterialTheme.typography.bodyLarge, color = colors.inverseOnSurface.copy(alpha = .68f), textAlign = TextAlign.Center)
                }
            }
            Spacer(Modifier.height(17.dp))
            Text("Sumber data · Asmaul Husna API (ekel.dev)", style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun HadithSheet(
    item: HadithItem,
    favorite: Boolean,
    onFavorite: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val colors = MaterialTheme.colorScheme
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = colors.background) {
        Column(
            Modifier.fillMaxWidth().heightIn(max = 700.dp).verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 38.dp)
        ) {
            DetailHeader(item.title, "Arbain Nawawi · Hadis ${item.number}", favorite, onFavorite)
            Spacer(Modifier.height(18.dp))
            Surface(shape = RoundedCornerShape(23.dp), color = colors.surfaceVariant, modifier = Modifier.fillMaxWidth()) {
                Text(
                    item.arabic,
                    fontFamily = ArabicFontFamily,
                    fontSize = 26.sp,
                    lineHeight = 48.sp,
                    textAlign = TextAlign.End,
                    modifier = Modifier.padding(18.dp).fillMaxWidth()
                )
            }
            Spacer(Modifier.height(18.dp))
            DetailLabel("TERJEMAHAN INGGRIS")
            Text(item.translationEnglish, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = colors.outlineVariant)
            Spacer(Modifier.height(11.dp))
            Text("Sumber · Hadith API · Unlicense", style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
        }
    }
}

@Composable
private fun DetailHeader(title: String, subtitle: String, favorite: Boolean, onFavorite: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.headlineSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(10.dp))
        Surface(
            onClick = onFavorite,
            modifier = Modifier.size(48.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painterResource(if (favorite) R.drawable.ic_bookmark_filled else R.drawable.ic_bookmark),
                    if (favorite) "Hapus dari tersimpan" else "Simpan bacaan",
                    Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun DetailLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.1.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(Modifier.height(5.dp))
}

@Composable
internal fun FeatureTopBar(
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    actions: @Composable RowScope.() -> Unit = {}
) {
    val colors = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().height(66.dp).padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                Box(
                    modifier = Modifier.size(39.dp).background(colors.surfaceVariant, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_back),
                        contentDescription = "Kembali",
                        modifier = Modifier.size(20.dp),
                        tint = colors.onBackground
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            actions()
            Spacer(Modifier.width(2.dp))
        }
        HorizontalDivider(color = colors.outlineVariant.copy(alpha = .72f))
    }
}
