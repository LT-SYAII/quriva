package com.quriva.app.ui.screens

import android.app.Application
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
import com.quriva.app.data.IslamicCalendar
import com.quriva.app.data.KhatamPlanner
import com.quriva.app.data.KhatamStats
import com.quriva.app.data.PrayerCities
import com.quriva.app.data.PrayerMethod
import com.quriva.app.data.PrayerTimes
import com.quriva.app.model.LastRead
import com.quriva.app.model.SearchResult
import com.quriva.app.model.SurahMeta
import com.quriva.app.ui.HomeUiState
import com.quriva.app.ui.HomeViewModel
import com.quriva.app.ui.HomeViewModelFactory
import com.quriva.app.ui.LocalReduceMotion
import com.quriva.app.ui.ProgressViewModel
import com.quriva.app.ui.StreakStats
import com.quriva.app.ui.theme.ArabicFontFamily
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToInt

private enum class SurahFilter(val label: String) {
    ALL("Semua"),
    MAKKIYAH("Makkiyah"),
    MADANIYAH("Madaniyah")
}

private class HomeActions(
    val openSearch: () -> Unit,
    val openBookmarks: () -> Unit,
    val openJuz: () -> Unit,
    val openProgress: () -> Unit,
    val openNotes: () -> Unit,
    val openPages: () -> Unit,
    val openMemorization: () -> Unit,
    val openContent: () -> Unit,
    val openNavigation: () -> Unit,
    val openTajweed: () -> Unit,
    val openAudioDownloads: () -> Unit,
    val openCalendar: () -> Unit,
    val openFriday: () -> Unit,
    val openPrayer: () -> Unit,
    val openKhatam: () -> Unit,
    val openSettings: () -> Unit
)

private class HomeMenuEntry(
    val title: String,
    val subtitle: String,
    val icon: Int,
    val onClick: () -> Unit
)

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
    onOpenMemorization: () -> Unit,
    onOpenContent: () -> Unit,
    onOpenNavigation: () -> Unit,
    onOpenTajweed: () -> Unit,
    onOpenAudioDownloads: () -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenFriday: () -> Unit,
    onOpenPrayer: () -> Unit,
    onOpenKhatam: () -> Unit,
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

    val actions = HomeActions(
        openSearch = onOpenSearch,
        openBookmarks = onOpenBookmarks,
        openJuz = onOpenJuz,
        openProgress = onOpenProgress,
        openNotes = onOpenNotes,
        openPages = onOpenPages,
        openMemorization = onOpenMemorization,
        openContent = onOpenContent,
        openNavigation = onOpenNavigation,
        openTajweed = onOpenTajweed,
        openAudioDownloads = onOpenAudioDownloads,
        openCalendar = onOpenCalendar,
        openFriday = onOpenFriday,
        openPrayer = onOpenPrayer,
        openKhatam = onOpenKhatam,
        openSettings = onOpenSettings
    )

    val hijriLabel = remember(prefs.hijriAdjustment) {
        IslamicCalendar.hijriLabel(IslamicCalendar.today(prefs.hijriAdjustment))
    }
    val greeting = remember {
        when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
            in 4..10 -> "Selamat pagi"
            in 11..14 -> "Selamat siang"
            in 15..17 -> "Selamat sore"
            else -> "Selamat malam"
        }
    }

    LaunchedEffect(Unit) {
        prefs.evaluateSkipMessage()
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
    val resumeSurah = prefs.lastRead?.surahNumber ?: 1
    val resumeArabicName = allSurahs.firstOrNull { it.number == resumeSurah }?.nameArabic.orEmpty()
    val dueReviews = prefs.memorizationEntries.count { it.reviewAt <= System.currentTimeMillis() }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing),
            state = listState,
            contentPadding = PaddingValues(bottom = 36.dp)
        ) {
            item(key = "header") {
                HomeHeader(
                    greeting = greeting,
                    hijriLabel = hijriLabel,
                    hasDueReview = dueReviews > 0,
                    actions = actions
                )
            }

            item(key = "prayer_pill") {
                NextPrayerCard(prefs = prefs, onOpen = onOpenPrayer)
            }

            when (val uiState = state) {
                is HomeUiState.Loading -> {
                    item(key = "search_skeleton") { SearchSkeleton() }
                    items(count = 8, key = { "sk_$it" }) { SkeletonSurahRow() }
                }

                is HomeUiState.Error -> {
                    item(key = "error") {
                        ErrorState(
                            message = uiState.message,
                            onRetry = viewModel::load
                        )
                    }
                }

                is HomeUiState.Success -> {
                    item(key = "skip") {
                        AnimatedVisibility(
                            visible = skipMessage.isNotBlank(),
                            enter = fadeIn(tween(220)) + expandVertically(),
                            exit = fadeOut(tween(180)) + shrinkVertically()
                        ) {
                            SkipMessageCard(
                                message = skipMessage,
                                onDismiss = { prefs.dismissSkipMessage() }
                            )
                        }
                    }
                    item(key = "continue") {
                        ContinueReadingCard(
                            lastRead = prefs.lastRead,
                            arabicName = resumeArabicName,
                            onOpen = onOpenSurah
                        )
                    }
                    val ayat = ayahOfDay
                    if (ayat != null) {
                        item(key = "ayah_of_day") {
                            AyatOfDayCard(ayat, onOpen = { onOpenSurah(ayat.surahNumber, ayat.ayahNumber) })
                        }
                    }
                    item(key = "rhythm") {
                        TodayRhythmCard(stats = streak, onOpen = onOpenProgress)
                    }
                    item(key = "worship") {
                        WorshipTrackerCard(
                            prefs = prefs,
                            modifier = Modifier
                                .padding(horizontal = 20.dp)
                                .padding(top = 14.dp)
                        )
                    }
                    item(key = "khatam") {
                        val khatamStats = remember(
                            prefs.lastRead,
                            prefs.khatamStartDate,
                            prefs.khatamStartVerse,
                            prefs.khatamTargetDays
                        ) {
                            KhatamPlanner.compute(context, prefs)
                        }
                        KhatamHomeCard(
                            stats = khatamStats,
                            onOpen = onOpenKhatam,
                            modifier = Modifier
                                .padding(horizontal = 20.dp)
                                .padding(top = 14.dp)
                        )
                    }
                    item(key = "search_filters") {
                        SearchAndFilters(
                            query = query,
                            onQueryChange = { query = it },
                            filter = filter,
                            onFilterChange = { filterKey = it.name }
                        )
                    }
                    item(key = "section_header") {
                        SectionHeader(count = visibleSurahs.size)
                    }
                    items(items = visibleSurahs, key = { it.number }, contentType = { "surah" }) { surah ->
                        Box(modifier = Modifier.animateItem()) {
                            SurahRow(surah = surah, onClick = { onOpenSurah(surah.number, 1) })
                        }
                    }
                    if (visibleSurahs.isEmpty()) {
                        item(key = "empty") { EmptyState(query) }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeHeader(
    greeting: String,
    hijriLabel: String,
    hasDueReview: Boolean,
    actions: HomeActions
) {
    val colors = MaterialTheme.colorScheme
    var menuOpen by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp)
            .padding(top = 12.dp, bottom = 18.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(colors.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.mipmap.ic_launcher_foreground),
                    contentDescription = null,
                    modifier = Modifier.size(29.dp)
                )
            }
            Spacer(Modifier.width(11.dp))
            Column {
                Text(
                    text = "QURIVA",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 3.4.sp,
                    color = colors.onBackground
                )
                Text(
                    text = "Al-Qur'an & ibadah harian",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant
                )
            }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = actions.openSearch, modifier = Modifier.size(48.dp)) {
                Icon(
                    painter = painterResource(R.drawable.ic_search),
                    contentDescription = "Cari ayat",
                    tint = colors.onBackground,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(4.dp))
            Box {
                Surface(
                    onClick = { menuOpen = true },
                    modifier = Modifier.height(46.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = colors.surfaceVariant,
                    border = BorderStroke(1.dp, colors.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp).fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_menu),
                            contentDescription = null,
                            tint = colors.onBackground,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(7.dp))
                        Text("Menu", style = MaterialTheme.typography.labelLarge, color = colors.onBackground)
                        if (hasDueReview) {
                            Spacer(Modifier.width(7.dp))
                            Box(Modifier.size(6.dp).background(colors.onBackground, CircleShape))
                        }
                    }
                }
                DropdownMenu(
                    expanded = menuOpen,
                    onDismissRequest = { menuOpen = false },
                    modifier = Modifier.width(314.dp).heightIn(max = 548.dp),
                    shape = RoundedCornerShape(24.dp),
                    containerColor = colors.surface,
                    tonalElevation = 0.dp,
                    shadowElevation = 10.dp,
                    border = BorderStroke(1.dp, colors.outlineVariant)
                ) {
                    HomeFeatureMenu(actions = actions, onClose = { menuOpen = false })
                }
            }
        }

        Spacer(Modifier.height(28.dp))
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = greeting.uppercase(Locale.forLanguageTag("id-ID")),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                color = colors.onSurfaceVariant
            )
            Spacer(Modifier.weight(1f))
            Surface(
                shape = CircleShape,
                color = colors.surfaceVariant,
                border = BorderStroke(1.dp, colors.outlineVariant)
            ) {
                Text(
                    text = hijriLabel,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp).widthIn(max = 164.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Baca dengan tenang.",
            style = MaterialTheme.typography.displaySmall,
            color = colors.onBackground
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Satu ayat hari ini, satu langkah lebih dekat.",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant
        )
    }
}

@Composable
private fun ColumnScope.HomeFeatureMenu(actions: HomeActions, onClose: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 10.dp)
    ) {
        Text(
            text = "Jelajahi Quriva",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = "Semua fitur, dikelompokkan agar mudah ditemukan.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    val groups = listOf(
        "Baca" to listOf(
            HomeMenuEntry("Juz", "30 juz Al-Qur'an", R.drawable.ic_layers, actions.openJuz),
            HomeMenuEntry("Halaman mushaf", "604 halaman Al-Qur'an", R.drawable.ic_book_open, actions.openPages),
            HomeMenuEntry("Navigasi lengkap", "Hizb, rub, manzil, dan sajdah", R.drawable.ic_navigation, actions.openNavigation)
        ),
        "Ibadah" to listOf(
            HomeMenuEntry("Jadwal salat & kiblat", "Jadwal offline, pengingat, dan arah kiblat", R.drawable.ic_schedule, actions.openPrayer),
            HomeMenuEntry("Rencana khatam", "Target 30–365 hari dan proyeksi harian", R.drawable.ic_chart, actions.openKhatam),
            HomeMenuEntry("Hafalan & murajaah", "Rentang ayat, audio, dan jadwal", R.drawable.ic_book_open, actions.openMemorization),
            HomeMenuEntry("Koleksi Islami", "Doa, dzikir, asmaul husna, hadis", R.drawable.ic_bookmark, actions.openContent),
            HomeMenuEntry("Kalender Islam", "Peristiwa dan hitung mundur", R.drawable.ic_schedule, actions.openCalendar),
            HomeMenuEntry("Hari Jumat", "Al-Kahf dan amalan Jumat", R.drawable.ic_notification, actions.openFriday)
        ),
        "Belajar & audio" to listOf(
            HomeMenuEntry("Audio offline", "Unduh surah atau satu juz", R.drawable.ic_headphones, actions.openAudioDownloads),
            HomeMenuEntry("Panduan tajwid", "Warna, penjelasan, dan kuis", R.drawable.ic_tune, actions.openTajweed)
        ),
        "Simpanan" to listOf(
            HomeMenuEntry("Bookmark", "Folder, tag, dan pencarian", R.drawable.ic_bookmark, actions.openBookmarks),
            HomeMenuEntry("Catatan", "Refleksi pribadi per ayat", R.drawable.ic_copy, actions.openNotes),
            HomeMenuEntry("Aktivitas", "Streak dan ritme tilawah", R.drawable.ic_chart, actions.openProgress)
        ),
        "Aplikasi" to listOf(
            HomeMenuEntry("Pengaturan", "Tampilan, aksesibilitas, dan pengingat", R.drawable.ic_tune, actions.openSettings)
        )
    )

    groups.forEachIndexed { index, (title, entries) ->
        if (index > 0) {
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 3.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )
        }
        Text(
            text = title.uppercase(Locale.forLanguageTag("id-ID")),
            modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 9.dp, bottom = 3.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.1.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        entries.forEach { entry ->
            DropdownMenuItem(
                text = {
                    Column {
                        Text(
                            text = entry.title,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = entry.subtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                onClick = {
                    onClose()
                    entry.onClick()
                },
                leadingIcon = {
                    Surface(
                        modifier = Modifier.size(36.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                painter = painterResource(entry.icon),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                },
                trailingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow_forward),
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                contentPadding = PaddingValues(start = 16.dp, end = 14.dp, top = 2.dp, bottom = 2.dp)
            )
        }
    }
}

@Composable
private fun NextPrayerCard(prefs: AppPreferences, onOpen: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    var now by remember { androidx.compose.runtime.mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(30_000)
            now = System.currentTimeMillis()
        }
    }
    val city = remember(prefs.prayerCityId) { PrayerCities.byId(prefs.prayerCityId) }
    val method = remember(prefs.prayerMethod) {
        runCatching { PrayerMethod.valueOf(prefs.prayerMethod) }.getOrDefault(PrayerMethod.KEMENAG)
    }
    val dayKey = remember(now) {
        SimpleDateFormat("yyyyMMdd", Locale.US).format(java.util.Date(now))
    }
    val schedule = remember(city, method, prefs.prayerAsrHanafi, dayKey) {
        PrayerTimes.schedule(city, method, prefs.prayerAsrHanafi, now)
    }
    val next = schedule.next(now)
    val remaining = next?.millis?.minus(now) ?: 0L

    Surface(
        onClick = onOpen,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(20.dp),
        color = colors.surfaceVariant,
        border = BorderStroke(1.dp, colors.outlineVariant)
    ) {
        Row(
            Modifier.padding(start = 14.dp, end = 12.dp, top = 11.dp, bottom = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(38.dp)
                    .background(colors.background, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painterResource(R.drawable.ic_schedule),
                    contentDescription = null,
                    tint = colors.onBackground,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "SALAT BERIKUTNYA · ${city.name.uppercase(Locale.forLanguageTag("id-ID"))}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = .7.sp,
                    color = colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(1.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        next?.prayer?.label ?: "Jadwal salat",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (next != null) {
                        Text(
                            " · ${PrayerTimes.formatClock(next.millis, city.timeZoneId)}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(Modifier.width(7.dp))
                    Text(
                        if (next == null) "Atur kota"
                        else if (remaining < 60_000L) "sekarang"
                        else "${PrayerTimes.countdownLabel(remaining)} lagi",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(Modifier.width(6.dp))
            Icon(
                painterResource(R.drawable.ic_arrow_forward),
                contentDescription = "Buka jadwal salat",
                tint = colors.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun KhatamHomeCard(
    stats: KhatamStats,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val percent = (stats.percent * 100).roundToInt()
    Surface(
        onClick = onOpen,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = colors.surface,
        border = BorderStroke(1.dp, colors.outlineVariant)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(38.dp)
                        .background(colors.surfaceVariant, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painterResource(R.drawable.ic_book_open),
                        contentDescription = null,
                        tint = colors.onBackground,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(Modifier.width(11.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "RENCANA KHATAM",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.1.sp,
                        color = colors.onSurfaceVariant
                    )
                    Text(
                        if (stats.started) "$percent% perjalanan khatam" else "Mulai rencana khatam",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Surface(
                    shape = CircleShape,
                    color = colors.onBackground,
                    contentColor = colors.background
                ) {
                    Text(
                        if (stats.started) "$percent%" else "Mulai",
                        Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { stats.percent },
                modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                color = colors.onBackground,
                trackColor = colors.outlineVariant
            )
            Spacer(Modifier.height(8.dp))
            Text(
                if (stats.started) {
                    "Sisa ${stats.remainingVerses} ayat · target ${KhatamPlanner.todayDailyTarget(stats)} per hari"
                } else {
                    "Pilih target 30–365 hari, progres dihitung dari posisi terakhir dibaca."
                },
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SkipMessageCard(message: String, onDismiss: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val reduceMotion = LocalReduceMotion.current
    val scale by animateFloatAsState(
        if (!reduceMotion && pressed) 0.985f else 1f,
        spring(stiffness = Spring.StiffnessMediumLow),
        label = "skipScale"
    )
    Surface(
        onClick = onDismiss,
        interactionSource = interaction,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 12.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale },
        shape = RoundedCornerShape(18.dp),
        color = colors.surfaceVariant,
        border = BorderStroke(1.dp, colors.outlineVariant)
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(painter = painterResource(R.drawable.ic_book_open), contentDescription = null, tint = colors.onBackground, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Text(message, style = MaterialTheme.typography.bodySmall, color = colors.onBackground, modifier = Modifier.weight(1f))
            Icon(painter = painterResource(R.drawable.ic_close), contentDescription = "Tutup", tint = colors.onSurfaceVariant, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun ContinueReadingCard(
    lastRead: LastRead?,
    arabicName: String,
    onOpen: (Int, Int) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val surahNumber = lastRead?.surahNumber ?: 1
    val ayah = lastRead?.ayah ?: 1
    val name = lastRead?.surahName ?: "Al-Fatihah"
    val total = lastRead?.totalAyah ?: 7
    val targetProgress = (ayah.toFloat() / total.toFloat()).coerceIn(0f, 1f)
    val progress by animateFloatAsState(targetValue = targetProgress, animationSpec = tween(700), label = "continueProgress")
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val reduceMotion = LocalReduceMotion.current
    val scale by animateFloatAsState(
        if (!reduceMotion && pressed) 0.977f else 1f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "continueScale"
    )

    Card(
        onClick = { onOpen(surahNumber, ayah) },
        interactionSource = interaction,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale },
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = colors.inverseSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            if (arabicName.isNotBlank()) {
                Text(
                    text = arabicName,
                    fontFamily = ArabicFontFamily,
                    fontSize = 68.sp,
                    lineHeight = 76.sp,
                    color = colors.inverseOnSurface.copy(alpha = 0.09f),
                    maxLines = 1,
                    modifier = Modifier.align(Alignment.TopEnd).offset(x = 12.dp, y = (-8).dp)
                )
            }
            Box(
                modifier = Modifier
                    .size(190.dp)
                    .align(Alignment.BottomEnd)
                    .offset(x = 66.dp, y = 72.dp)
                    .background(
                        Brush.radialGradient(
                            listOf(colors.inverseOnSurface.copy(alpha = 0.12f), Color.Transparent)
                        ),
                        CircleShape
                    )
            )
            Column(modifier = Modifier.padding(23.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (lastRead == null) "MULAI MEMBACA" else "TERAKHIR DIBACA",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                        color = colors.inverseOnSurface.copy(alpha = 0.58f)
                    )
                    Spacer(Modifier.weight(1f))
                    Surface(
                        shape = CircleShape,
                        color = colors.inverseOnSurface.copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = "QS $surahNumber",
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.inverseOnSurface.copy(alpha = 0.8f)
                        )
                    }
                }
                Spacer(Modifier.height(13.dp))
                Text(
                    text = name,
                    style = MaterialTheme.typography.headlineMedium,
                    color = colors.inverseOnSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = if (lastRead == null) "$total ayat · mulai dari awal" else "Ayat $ayah dari $total",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.inverseOnSurface.copy(alpha = 0.64f)
                )
                Spacer(Modifier.height(22.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)),
                    color = colors.inverseOnSurface,
                    trackColor = colors.inverseOnSurface.copy(alpha = 0.17f)
                )
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (lastRead == null) "Bismillah, mulai" else "Lanjutkan tilawah",
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.inverseOnSurface
                    )
                    Spacer(Modifier.width(9.dp))
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .background(colors.inverseOnSurface.copy(alpha = 0.12f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_forward),
                            contentDescription = null,
                            tint = colors.inverseOnSurface,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = "${(targetProgress * 100).roundToInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.inverseOnSurface.copy(alpha = 0.58f)
                    )
                }
            }
        }
    }
}

@Composable
private fun AyatOfDayCard(item: SearchResult, onOpen: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val reduceMotion = LocalReduceMotion.current
    val scale by animateFloatAsState(
        if (!reduceMotion && pressed) 0.985f else 1f,
        spring(stiffness = Spring.StiffnessMediumLow),
        label = "ayahScale"
    )

    Surface(
        onClick = onOpen,
        interactionSource = interaction,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 14.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale },
        shape = RoundedCornerShape(26.dp),
        color = colors.surfaceVariant,
        border = BorderStroke(1.dp, colors.outlineVariant)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "”",
                fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
                fontSize = 104.sp,
                lineHeight = 86.sp,
                color = colors.onBackground.copy(alpha = 0.06f),
                modifier = Modifier.align(Alignment.TopEnd).padding(end = 14.dp)
            )
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(6.dp).background(colors.onBackground, CircleShape))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "AYAT HARI INI",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.35.sp,
                        color = colors.onSurfaceVariant
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = "QS. ${item.surahName} : ${item.ayahNumber}",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant,
                        maxLines = 1
                    )
                }
                Spacer(Modifier.height(18.dp))
                Text(
                    text = item.arabic,
                    fontFamily = ArabicFontFamily,
                    fontSize = 25.sp,
                    lineHeight = 47.sp,
                    color = colors.onBackground,
                    textAlign = TextAlign.End,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(13.dp))
                Text(
                    text = item.translation,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(17.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Buka ayat", style = MaterialTheme.typography.labelLarge, color = colors.onBackground)
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow_forward),
                        contentDescription = null,
                        tint = colors.onBackground,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = "Renungan harian",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun TodayRhythmCard(stats: StreakStats, onOpen: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val reduceMotion = LocalReduceMotion.current
    val scale by animateFloatAsState(
        if (!reduceMotion && pressed) 0.985f else 1f,
        spring(stiffness = Spring.StiffnessMediumLow),
        label = "rhythmScale"
    )
    var barsVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { barsVisible = true }

    val week = remember(stats.days) { stats.days.takeLast(7) }
    val maxValue = remember(week) {
        week.maxOfOrNull { it.readCount + it.playCount }?.coerceAtLeast(1) ?: 1
    }
    val labels = remember(week) {
        val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val formatter = SimpleDateFormat("EE", Locale.forLanguageTag("id-ID"))
        week.map { day ->
            runCatching { formatter.format(parser.parse(day.date)!!).replace(".", "") }
                .getOrDefault(day.date.takeLast(2))
        }
    }
    val todayTotal = stats.todayRead + stats.todayPlay

    Surface(
        onClick = onOpen,
        interactionSource = interaction,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 14.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale },
        shape = RoundedCornerShape(26.dp),
        color = colors.surface,
        border = BorderStroke(1.dp, colors.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(19.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "RITME TILAWAH",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.35.sp,
                        color = colors.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = if (stats.streakDays > 0) "${stats.streakDays} hari berturut-turut" else "Mulai ritme hari ini",
                        style = MaterialTheme.typography.titleLarge,
                        color = colors.onBackground
                    )
                }
                Surface(
                    shape = CircleShape,
                    color = colors.onBackground
                ) {
                    Text(
                        text = if (stats.streakDays > 0) "${stats.streakDays}🔥" else "0🔥",
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.background
                    )
                }
            }

            Spacer(Modifier.height(18.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(9.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                week.forEachIndexed { index, day ->
                    val total = day.readCount + day.playCount
                    val fraction = (total.toFloat() / maxValue.toFloat()).coerceIn(0f, 1f)
                    val target = if (barsVisible) 5f + 32f * fraction else 3f
                    val barHeight by animateFloatAsState(
                        targetValue = target,
                        animationSpec = tween(
                            durationMillis = if (reduceMotion) 1 else 520,
                            delayMillis = if (reduceMotion) 0 else index * 35
                        ),
                        label = "rhythmBar$index"
                    )
                    val isToday = index == week.lastIndex
                    val barColor = when {
                        total == 0 -> colors.outlineVariant
                        isToday -> colors.onBackground
                        else -> colors.onSurfaceVariant.copy(alpha = 0.5f)
                    }
                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier.height(39.dp),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(18.dp)
                                    .height(barHeight.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(barColor)
                            )
                        }
                        Spacer(Modifier.height(7.dp))
                        Text(
                            text = labels.getOrElse(index) { "" },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
                            color = if (isToday) colors.onBackground else colors.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(Modifier.height(15.dp))
            HorizontalDivider(color = colors.outlineVariant)
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (todayTotal == 0) "Belum ada aktivitas hari ini" else "Hari ini $todayTotal aktivitas",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = "Rata-rata ${stats.avgDaily7}/hari",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant
                )
                Spacer(Modifier.width(7.dp))
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_forward),
                    contentDescription = null,
                    tint = colors.onBackground,
                    modifier = Modifier.size(14.dp)
                )
            }
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

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 26.dp).animateContentSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Cari surah, arti, atau nomor", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant) },
            leadingIcon = { Icon(painter = painterResource(R.drawable.ic_search), contentDescription = null, tint = colors.onSurfaceVariant, modifier = Modifier.size(20.dp)) },
            trailingIcon = {
                AnimatedVisibility(visible = query.isNotEmpty(), enter = fadeIn(), exit = fadeOut()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(painter = painterResource(R.drawable.ic_close), contentDescription = "Hapus pencarian", tint = colors.onSurfaceVariant, modifier = Modifier.size(18.dp))
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(20.dp),
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

        Spacer(Modifier.height(13.dp))
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
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val reduceMotion = LocalReduceMotion.current
    val scale by animateFloatAsState(
        targetValue = if (!reduceMotion && pressed) 0.95f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "pill"
    )
    Surface(
        onClick = onClick,
        interactionSource = interaction,
        modifier = Modifier.graphicsLayer { scaleX = scale; scaleY = scale },
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
        modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp).padding(top = 28.dp, bottom = 10.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        Text("Surah", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.weight(1f))
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Text(
                text = "$count / 114",
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SurahRow(surah: SurahMeta, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val reduceMotion = LocalReduceMotion.current
    val scale by animateFloatAsState(
        targetValue = if (!reduceMotion && pressed) 0.985f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "row"
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .animateContentSize()
    ) {
        Surface(
            onClick = onClick,
            interactionSource = interaction,
            color = Color.Transparent,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(43.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = surah.number.toString(),
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.onBackground
                    )
                }
                Spacer(Modifier.width(15.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = surah.latinName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.onBackground
                    )
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
                    Text(
                        text = surah.nameArabic,
                        fontFamily = ArabicFontFamily,
                        fontSize = 23.sp,
                        lineHeight = 31.sp,
                        color = colors.onBackground,
                        textAlign = TextAlign.End
                    )
                    Spacer(Modifier.height(1.dp))
                    Text(text = surah.place, style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                }
            }
        }
        HorizontalDivider(modifier = Modifier.padding(start = 80.dp, end = 22.dp), color = colors.outlineVariant)
    }
}

@Composable
private fun SearchSkeleton() {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 28.dp, bottom = 34.dp).height(56.dp)
            .clip(RoundedCornerShape(20.dp)).background(colors.surfaceVariant)
    )
}

@Composable
private fun SkeletonSurahRow() {
    val colors = MaterialTheme.colorScheme
    val placeholder = colors.surfaceVariant.copy(alpha = 0.62f)
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 17.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(43.dp).clip(RoundedCornerShape(14.dp)).background(placeholder))
        Spacer(Modifier.width(15.dp))
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
