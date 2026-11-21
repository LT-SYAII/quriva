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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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
import com.quriva.app.data.AppUpdateInfo
import com.quriva.app.data.DailyContent
import com.quriva.app.data.ReminderScheduler
import com.quriva.app.data.UpdateCheckResult
import com.quriva.app.data.UpdateRepository
import com.quriva.app.model.LastRead
import com.quriva.app.model.SearchResult
import com.quriva.app.model.SurahMeta
import com.quriva.app.ui.HomeUiState
import com.quriva.app.ui.HomeViewModel
import com.quriva.app.ui.HomeViewModelFactory
import com.quriva.app.ui.ProgressViewModel
import com.quriva.app.ui.UpdateAvailableDialog
import com.quriva.app.ui.theme.ArabicFontFamily
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
    var pendingUpdate by remember { mutableStateOf<AppUpdateInfo?>(null) }

    LaunchedEffect(Unit) {
        val now = System.currentTimeMillis()
        if (now - prefs.lastUpdateCheckMs >= 12L * 60 * 60 * 1000) {
            when (val result = withContext(Dispatchers.IO) { UpdateRepository.checkForUpdate(context) }) {
                is UpdateCheckResult.Available -> {
                    prefs.markUpdateChecked()
                    if (result.info.tagName != prefs.dismissedUpdateTag) {
                        pendingUpdate = result.info
                    }
                }
                is UpdateCheckResult.UpToDate -> prefs.markUpdateChecked()
                is UpdateCheckResult.Error -> { /* silent on auto-check */ }
            }
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing),
                state = listState,
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                item(key = "header") {
                    HomeHeader(
                        onOpenSearch = onOpenSearch,
                        onOpenBookmarks = onOpenBookmarks,
                        onOpenSettings = onOpenSettings
                    )
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
                            ContinueReadingCard(lastRead = prefs.lastRead, onOpen = onOpenSurah)
                        }
                        item(key = "streak") {
                            StreakSummaryCard(
                                streakDays = streak.streakDays,
                                todayRead = streak.todayRead,
                                todayPlay = streak.todayPlay,
                                weekTotal = streak.weekRead + streak.weekPlay,
                                activeDays30 = streak.activeDays30,
                                onOpen = onOpenProgress
                            )
                        }
                        item(key = "explore") {
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

    val update = pendingUpdate
    if (update != null) {
        UpdateAvailableDialog(
            info = update,
            currentVersion = UpdateRepository.currentVersionName(context),
            onDismiss = { pendingUpdate = null },
            onSkipVersion = {
                prefs.dismissUpdateTag(update.tagName)
                pendingUpdate = null
            },
            onOpenRelease = {
                UpdateRepository.openUrl(context, update.htmlUrl)
            }
        )
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
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.985f else 1f, spring(stiffness = Spring.StiffnessMediumLow), label = "skipScale")
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
private fun ContinueReadingCard(lastRead: LastRead?, onOpen: (Int, Int) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val surahNumber = lastRead?.surahNumber ?: 1
    val ayah = lastRead?.ayah ?: 1
    val name = lastRead?.surahName ?: "Al-Fatihah"
    val total = lastRead?.totalAyah ?: 7
    val targetProgress = (ayah.toFloat() / total.toFloat()).coerceIn(0f, 1f)
    val progress by animateFloatAsState(targetValue = targetProgress, animationSpec = tween(700), label = "continueProgress")
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.975f else 1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow), label = "continueScale")

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
private fun StreakSummaryCard(
    streakDays: Int,
    todayRead: Int,
    todayPlay: Int,
    weekTotal: Int,
    activeDays30: Int,
    onOpen: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val todayTotal = todayRead + todayPlay
    val targetRing = (streakDays / 30f).coerceIn(0f, 1f)
    val ringProgress by animateFloatAsState(targetRing, tween(800), label = "ring")
    val dailyGoal = 20
    val targetDaily = (todayTotal / dailyGoal.toFloat()).coerceIn(0f, 1f)
    val dailyProgress by animateFloatAsState(targetDaily, tween(700), label = "daily")
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.975f else 1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow), label = "streakScale")

    Surface(
        onClick = onOpen,
        interactionSource = interaction,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 14.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale },
        shape = RoundedCornerShape(26.dp),
        color = colors.inverseSurface
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier.size(140.dp).align(Alignment.TopEnd)
                    .background(Brush.radialGradient(listOf(colors.inverseOnSurface.copy(alpha = 0.12f), Color.Transparent)), CircleShape)
            )
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(58.dp)) {
                        Canvas(modifier = Modifier.size(58.dp)) {
                            val sw = 6.dp.toPx()
                            drawArc(color = colors.inverseOnSurface.copy(alpha = 0.18f), startAngle = -220f, sweepAngle = 260f, useCenter = false, style = Stroke(width = sw, cap = StrokeCap.Round))
                            drawArc(color = colors.inverseOnSurface, startAngle = -220f, sweepAngle = 260f * ringProgress, useCenter = false, style = Stroke(width = sw, cap = StrokeCap.Round))
                        }
                        Text("$streakDays", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = colors.inverseOnSurface)
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Daily Streak", style = MaterialTheme.typography.labelSmall, color = colors.inverseOnSurface.copy(alpha = 0.7f), letterSpacing = 1.sp)
                        Text(if (streakDays > 0) "$streakDays hari berturut-turut" else "Mulai streak hari ini", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = colors.inverseOnSurface)
                        Text("30 hari: $activeDays30 aktif · 7 hari: $weekTotal aktivitas", style = MaterialTheme.typography.labelSmall, color = colors.inverseOnSurface.copy(alpha = 0.65f))
                    }
                    Icon(painter = painterResource(R.drawable.ic_arrow_forward), contentDescription = null, tint = colors.inverseOnSurface.copy(alpha = 0.8f), modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatPill("Baca", "$todayRead", "ayat", Modifier.weight(1f), colors)
                    StatPill("Dengar", "$todayPlay", "ayat", Modifier.weight(1f), colors)
                    StatPill("Hari ini", "$todayTotal", "aktivitas", Modifier.weight(1f), colors)
                }
                Spacer(Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { dailyProgress },
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                    color = colors.inverseOnSurface,
                    trackColor = colors.inverseOnSurface.copy(alpha = 0.18f)
                )
                Spacer(Modifier.height(4.dp))
                Text("Target harian: $dailyGoal aktivitas", style = MaterialTheme.typography.labelSmall, color = colors.inverseOnSurface.copy(alpha = 0.6f))
            }
        }
    }
}

@Composable
private fun StatPill(label: String, value: String, unit: String, modifier: Modifier = Modifier, colors: androidx.compose.material3.ColorScheme) {
    Surface(modifier = modifier, shape = RoundedCornerShape(14.dp), color = colors.inverseOnSurface.copy(alpha = 0.1f)) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = colors.inverseOnSurface.copy(alpha = 0.65f))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = colors.inverseOnSurface)
                Spacer(Modifier.width(2.dp))
                Text(unit, style = MaterialTheme.typography.labelSmall, color = colors.inverseOnSurface.copy(alpha = 0.6f), modifier = Modifier.padding(bottom = 1.dp))
            }
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
            IconOnlyTile(icon = R.drawable.ic_search, description = "Cari ayat", onClick = onOpenSearch, modifier = Modifier.weight(1f))
            IconOnlyTile(icon = R.drawable.ic_layers, description = "Daftar juz", onClick = onOpenJuz, modifier = Modifier.weight(1f))
            IconOnlyTile(icon = R.drawable.ic_bookmark, description = "Bookmark", onClick = onOpenBookmarks, modifier = Modifier.weight(1f))
            IconOnlyTile(icon = R.drawable.ic_book_open, description = "Ayat hari ini", onClick = onOpenAyahOfDay, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            IconOnlyTile(icon = R.drawable.ic_book_open, description = "Daily Streak", onClick = onOpenProgress, modifier = Modifier.weight(1f))
            IconOnlyTile(icon = R.drawable.ic_copy, description = "Catatan pribadi", onClick = onOpenNotes, modifier = Modifier.weight(1f))
            IconOnlyTile(icon = R.drawable.ic_layers, description = "Halaman mushaf", onClick = onOpenPages, modifier = Modifier.weight(1f))
            Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun IconOnlyTile(
    icon: Int,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.94f else 1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium), label = "tileScale")
    Surface(
        onClick = onClick,
        interactionSource = interaction,
        modifier = modifier.height(64.dp).graphicsLayer { scaleX = scale; scaleY = scale },
        shape = RoundedCornerShape(18.dp),
        color = colors.surfaceVariant,
        contentColor = colors.onBackground,
        border = BorderStroke(1.dp, colors.outlineVariant)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Icon(painter = painterResource(icon), contentDescription = description, tint = colors.onBackground, modifier = Modifier.size(22.dp))
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

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 24.dp).animateContentSize()) {
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
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.95f else 1f,
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
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.985f else 1f, spring(stiffness = Spring.StiffnessMediumLow), label = "row")
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .animateContentSize()
    ) {
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
