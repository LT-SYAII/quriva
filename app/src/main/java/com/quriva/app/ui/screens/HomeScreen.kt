package com.quriva.app.ui.screens

import android.app.Application
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.quriva.app.R
import com.quriva.app.data.AppPreferences
import com.quriva.app.data.DailyContent
import com.quriva.app.data.ReminderScheduler
import com.quriva.app.model.LastRead
import com.quriva.app.model.SearchResult
import com.quriva.app.model.SurahMeta
import com.quriva.app.ui.HomeUiState
import com.quriva.app.ui.HomeViewModel
import com.quriva.app.ui.HomeViewModelFactory
import com.quriva.app.ui.ProgressViewModel
import com.quriva.app.ui.theme.ArabicFontFamily

private enum class SurahFilter(val label: String) {
    ALL("Semua"),
    MAKKIYAH("Makkiyah"),
    MADANIYAH("Madaniyah")
}

@Composable
fun HomeScreen(
    prefs: AppPreferences,
    onOpenSurah: (surah: Int, ayah: Int) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenBookmarks: () -> Unit,
    onOpenJuz: () -> Unit,
    onOpenProgress: () -> Unit,
    onOpenNotes: () -> Unit,
    onOpenPages: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val context = LocalContext.current
    val application = context.applicationContext as Application
    val viewModel: HomeViewModel = viewModel(factory = HomeViewModelFactory(application))
    val state by viewModel.state.collectAsStateWithLifecycle()

    var query by rememberSaveable { mutableStateOf("") }
    var filterKey by rememberSaveable { mutableStateOf(SurahFilter.ALL.name) }
    val filter = SurahFilter.valueOf(filterKey)
    val listState = rememberLazyListState()

    val success = state as? HomeUiState.Success
    val allSurahs = success?.surahs.orEmpty()
    var ayahOfDay by remember { mutableStateOf<SearchResult?>(null) }

    LaunchedEffect(Unit) {
        prefs.evaluateSkipMessage()
        if (prefs.reminderEnabled) {
            ReminderScheduler.schedule(application, prefs.reminderHour, prefs.reminderMinute)
        }
    }
    LaunchedEffect(success) {
        if (success != null) {
            ayahOfDay = try { DailyContent.ayahOfTheDay(application) } catch (_: Throwable) { null }
        }
    }

    val visibleSurahs = remember(allSurahs, query, filter) {
        val q = query.trim()
        allSurahs.filter { surah ->
            val matchesFilter = when (filter) {
                SurahFilter.ALL -> true
                SurahFilter.MAKKIYAH -> surah.place == "Makkiyah"
                SurahFilter.MADANIYAH -> surah.place == "Madaniyah"
            }
            val matchesQuery = q.isEmpty() ||
                surah.latinName.contains(q, ignoreCase = true) ||
                surah.translation.contains(q, ignoreCase = true) ||
                surah.nameArabic.contains(q) ||
                surah.number.toString() == q
            matchesFilter && matchesQuery
        }
    }

    val streak = ProgressViewModel.compute(prefs)
    val skipMessage = if (prefs.skipMessageDate.isNotBlank()) prefs.skipMessageText else ""

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing),
            state = listState,
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item {
                HomeHeader(
                    onOpenSearch = onOpenSearch,
                    onOpenBookmarks = onOpenBookmarks,
                    onOpenSettings = onOpenSettings
                )
            }

            when (state) {
                is HomeUiState.Loading -> {
                    item { SearchSkeleton() }
                    items(count = 8) { SkeletonSurahRow() }
                }

                is HomeUiState.Error -> {
                    item {
                        ErrorState(
                            message = (state as HomeUiState.Error).message,
                            onRetry = viewModel::load
                        )
                    }
                }

                is HomeUiState.Success -> {
                    if (skipMessage.isNotBlank()) {
                        item {
                            SkipMessageCard(
                                message = skipMessage,
                                onDismiss = { prefs.dismissSkipMessage() }
                            )
                        }
                    }
                    item {
                        ContinueReadingCard(lastRead = prefs.lastRead, onOpen = onOpenSurah)
                    }
                    item {
                        StreakSummaryCard(
                            streakDays = streak.streakDays,
                            todayRead = streak.todayRead,
                            todayPlay = streak.todayPlay,
                            onOpen = onOpenProgress
                        )
                    }
                    item {
                        ExploreSection(
                            onOpenSearch = onOpenSearch,
                            onOpenBookmarks = onOpenBookmarks,
                            onOpenJuz = onOpenJuz,
                            onOpenProgress = onOpenProgress,
                            onOpenNotes = onOpenNotes,
                            onOpenPages = onOpenPages,
                            onOpenAyahOfDay = {
                                ayahOfDay?.let { onOpenSurah(it.surahNumber, it.ayahNumber) }
                            }
                        )
                    }
                    item {
                        SearchAndFilters(
                            query = query,
                            onQueryChange = { query = it },
                            filter = filter,
                            onFilterChange = { filterKey = it.name }
                        )
                    }
                    item { SectionHeader(count = visibleSurahs.size) }
                    items(items = visibleSurahs, key = { it.number }, contentType = { "surah" }) { surah ->
                        SurahRow(surah = surah, onClick = { onOpenSurah(surah.number, 1) })
                    }
                    if (visibleSurahs.isEmpty()) {
                        item { EmptyState(query) }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeHeader(
    onOpenSearch: () -> Unit,
    onOpenBookmarks: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(top = 10.dp, bottom = 20.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(R.mipmap.ic_launcher_foreground),
                contentDescription = null,
                modifier = Modifier.size(42.dp).clip(RoundedCornerShape(12.dp))
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = "QURIVA",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                letterSpacing = 4.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onOpenSearch, modifier = Modifier.size(40.dp)) {
                Icon(painter = painterResource(R.drawable.ic_search), contentDescription = "Cari ayat", tint = MaterialTheme.colorScheme.onBackground, modifier = Modifier.size(20.dp))
            }
            IconButton(onClick = onOpenBookmarks, modifier = Modifier.size(40.dp)) {
                Icon(painter = painterResource(R.drawable.ic_bookmark), contentDescription = "Bookmark", tint = MaterialTheme.colorScheme.onBackground, modifier = Modifier.size(19.dp))
            }
            IconButton(onClick = onOpenSettings, modifier = Modifier.size(40.dp)) {
                Icon(painter = painterResource(R.drawable.ic_tune), contentDescription = "Pengaturan", tint = MaterialTheme.colorScheme.onBackground, modifier = Modifier.size(21.dp))
            }
        }

        Spacer(Modifier.height(26.dp))
        Text("Baca Al-Qur'an\ndengan tenang.", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(10.dp))
        Text("Teks Utsmani dan terjemahan Bahasa Indonesia, tersedia offline.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SkipMessageCard(message: String, onDismiss: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 12.dp),
        shape = RoundedCornerShape(18.dp),
        color = colors.surfaceVariant,
        border = BorderStroke(1.dp, colors.outlineVariant)
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(painter = painterResource(R.drawable.ic_book_open), contentDescription = null, tint = colors.onBackground, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Text(message, style = MaterialTheme.typography.bodySmall, color = colors.onBackground, modifier = Modifier.weight(1f))
            IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                Icon(painter = painterResource(R.drawable.ic_close), contentDescription = "Tutup", tint = colors.onSurfaceVariant, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun ContinueReadingCard(lastRead: LastRead?, onOpen: (Int, Int) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val surahNumber = lastRead?.surahNumber ?: 1
    val ayah = lastRead?.ayah ?: 1
    val name = lastRead?.surahName ?: "Al-Fatihah"
    val total = lastRead?.totalAyah ?: 7
    val progress = (ayah.toFloat() / total.toFloat()).coerceIn(0f, 1f)

    Card(
        onClick = { onOpen(surahNumber, ayah) },
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = colors.inverseSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Image(
                painter = painterResource(R.mipmap.ic_launcher_monochrome),
                contentDescription = null,
                colorFilter = ColorFilter.tint(colors.inverseOnSurface.copy(alpha = 0.10f)),
                modifier = Modifier.size(220.dp).align(Alignment.TopEnd).offset(x = 54.dp, y = (-50).dp)
            )
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = if (lastRead == null) "MULAI MEMBACA" else "TERAKHIR DIBACA",
                    style = MaterialTheme.typography.labelSmall,
                    letterSpacing = 1.6.sp,
                    color = colors.inverseOnSurface.copy(alpha = 0.55f)
                )
                Spacer(Modifier.height(10.dp))
                Text(text = name, style = MaterialTheme.typography.headlineMedium, color = colors.inverseOnSurface)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = if (lastRead == null) "$total ayat" else "Ayat $ayah dari $total",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.inverseOnSurface.copy(alpha = 0.65f)
                )
                Spacer(Modifier.height(22.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(2.dp),
                    color = colors.inverseOnSurface,
                    trackColor = colors.inverseOnSurface.copy(alpha = 0.18f)
                )
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (lastRead == null) "Bismillah, mulai" else "Lanjutkan membaca",
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.inverseOnSurface
                    )
                    Spacer(Modifier.width(6.dp))
                    Icon(painter = painterResource(R.drawable.ic_arrow_forward), contentDescription = null, tint = colors.inverseOnSurface, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun StreakSummaryCard(streakDays: Int, todayRead: Int, todayPlay: Int, onOpen: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onOpen,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 14.dp),
        shape = RoundedCornerShape(22.dp),
        color = colors.surfaceVariant,
        border = BorderStroke(1.dp, colors.outlineVariant)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = colors.onBackground, modifier = Modifier.size(42.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(painter = painterResource(R.drawable.ic_book_open), contentDescription = null, tint = colors.background, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Streak $streakDays hari", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = colors.onBackground)
                Text("Hari ini: $todayRead ayat dibaca · $todayPlay diputar", style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
            }
            Icon(painter = painterResource(R.drawable.ic_arrow_forward), contentDescription = null, tint = colors.onSurfaceVariant, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun ExploreSection(
    onOpenSearch: () -> Unit,
    onOpenBookmarks: () -> Unit,
    onOpenJuz: () -> Unit,
    onOpenProgress: () -> Unit,
    onOpenNotes: () -> Unit,
    onOpenPages: () -> Unit,
    onOpenAyahOfDay: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(top = 26.dp, bottom = 10.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            Text("Jelajahi", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground)
        }
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            IconOnlyTile(icon = R.drawable.ic_search, onClick = onOpenSearch, modifier = Modifier.weight(1f))
            IconOnlyTile(icon = R.drawable.ic_layers, onClick = onOpenJuz, modifier = Modifier.weight(1f))
            IconOnlyTile(icon = R.drawable.ic_bookmark, onClick = onOpenBookmarks, modifier = Modifier.weight(1f))
            IconOnlyTile(icon = R.drawable.ic_book_open, onClick = onOpenAyahOfDay, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            IconOnlyTile(icon = R.drawable.ic_book_open, onClick = onOpenProgress, modifier = Modifier.weight(1f))
            IconOnlyTile(icon = R.drawable.ic_copy, onClick = onOpenNotes, modifier = Modifier.weight(1f))
            IconOnlyTile(icon = R.drawable.ic_layers, onClick = onOpenPages, modifier = Modifier.weight(1f))
            Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun IconOnlyTile(icon: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        modifier = modifier.height(64.dp),
        shape = RoundedCornerShape(18.dp),
        color = colors.surfaceVariant,
        contentColor = colors.onBackground,
        border = BorderStroke(1.dp, colors.outlineVariant)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Icon(painter = painterResource(icon), contentDescription = null, tint = colors.onBackground, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
private fun SearchAndFilters(
    query: String,
    onQueryChange: (String) -> Unit,
    filter: SurahFilter,
    onFilterChange: (SurahFilter) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val focusManager = LocalFocusManager.current

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 24.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Cari surah, arti, atau nomor", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant) },
            leadingIcon = { Icon(painter = painterResource(R.drawable.ic_search), contentDescription = null, tint = colors.onSurfaceVariant, modifier = Modifier.size(20.dp)) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(painter = painterResource(R.drawable.ic_close), contentDescription = "Hapus pencarian", tint = colors.onSurfaceVariant, modifier = Modifier.size(18.dp))
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(18.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = colors.surfaceVariant,
                unfocusedContainerColor = colors.surfaceVariant,
                focusedBorderColor = colors.outline,
                unfocusedBorderColor = Color.Transparent,
                cursorColor = colors.onSurface
            )
        )

        Spacer(Modifier.height(14.dp))
        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SurahFilter.entries.forEach { item ->
                FilterPill(label = item.label, selected = filter == item, onClick = { onFilterChange(item) })
            }
        }
    }
}

@Composable
private fun FilterPill(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (selected) colors.onBackground else Color.Transparent,
        contentColor = if (selected) colors.background else colors.onSurfaceVariant,
        border = if (selected) null else BorderStroke(1.dp, colors.outline)
    ) {
        Text(text = label, modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp), style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun SectionHeader(count: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(top = 28.dp, bottom = 10.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        Text("Surah", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.weight(1f))
        Text("$count / 114", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 3.dp))
    }
}

@Composable
private fun SurahRow(surah: SurahMeta, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 15.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                Surface(modifier = Modifier.fillMaxSize(), shape = CircleShape, color = Color.Transparent, border = BorderStroke(1.dp, colors.outline)) {}
                Text(text = surah.number.toString(), style = MaterialTheme.typography.labelLarge, color = colors.onBackground)
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = surah.latinName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = colors.onBackground)
                Spacer(Modifier.height(3.dp))
                Text(
                    text = "${surah.translation} · ${surah.ayahCount} ayat",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(text = surah.nameArabic, fontFamily = ArabicFontFamily, fontSize = 23.sp, lineHeight = 31.sp, color = colors.onBackground, textAlign = TextAlign.End)
                Spacer(Modifier.height(1.dp))
                Text(text = surah.place, style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
            }
        }
        HorizontalDivider(modifier = Modifier.padding(start = 84.dp, end = 24.dp), color = colors.outlineVariant)
    }
}

@Composable
private fun SearchSkeleton() {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 28.dp, bottom = 34.dp).height(56.dp)
            .clip(RoundedCornerShape(18.dp)).background(colors.surfaceVariant)
    )
}

@Composable
private fun SkeletonSurahRow() {
    val colors = MaterialTheme.colorScheme
    val placeholder = colors.surfaceVariant.copy(alpha = 0.62f)
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(placeholder))
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Box(modifier = Modifier.width(150.dp).height(14.dp).clip(RoundedCornerShape(6.dp)).background(placeholder))
            Spacer(Modifier.height(8.dp))
            Box(modifier = Modifier.width(100.dp).height(10.dp).clip(RoundedCornerShape(5.dp)).background(placeholder.copy(alpha = 0.5f)))
        }
        Box(modifier = Modifier.width(64.dp).height(26.dp).clip(RoundedCornerShape(8.dp)).background(placeholder))
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(top = 56.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.size(58.dp), contentAlignment = Alignment.Center) {
            Surface(modifier = Modifier.fillMaxSize(), shape = CircleShape, color = Color.Transparent, border = BorderStroke(1.dp, colors.outline)) {}
            Text(text = "!", style = MaterialTheme.typography.headlineSmall, color = colors.onBackground)
        }
        Spacer(Modifier.height(20.dp))
        Text(text = "Konten gagal dimuat", style = MaterialTheme.typography.titleLarge, color = colors.onBackground, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(text = message, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant, textAlign = TextAlign.Center)
        Spacer(Modifier.height(22.dp))
        Button(onClick = onRetry, shape = CircleShape, colors = ButtonDefaults.buttonColors(containerColor = colors.onBackground, contentColor = colors.background)) {
            Text("Coba lagi", modifier = Modifier.padding(horizontal = 10.dp))
        }
    }
}

@Composable
private fun EmptyState(query: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(top = 48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = "Surah tidak ditemukan", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(6.dp))
        Text(
            text = if (query.isBlank()) "Coba ubah filter." else "Tidak ada hasil untuk \"$query\".",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
