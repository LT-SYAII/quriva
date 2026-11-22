package com.quriva.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quriva.app.R
import com.quriva.app.data.AppPreferences
import com.quriva.app.ui.ProgressViewModel
import com.quriva.app.ui.StreakRange
import com.quriva.app.ui.StreakStats
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.max

@Composable
fun ProgressScreen(
    prefs: AppPreferences,
    onBack: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val stats = ProgressViewModel.compute(prefs)
    var range by remember { mutableStateOf(StreakRange.DAILY) }

    Surface(modifier = Modifier.fillMaxSize(), color = colors.background) {
        Column(modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            FeatureTopBar(
                title = "Aktivitas Tilawah",
                subtitle = "Perjalanan baca dan dengarmu",
                onBack = onBack,
                actions = {
                    Surface(shape = CircleShape, color = colors.surfaceVariant, border = BorderStroke(1.dp, colors.outlineVariant)) {
                        Text(
                            "🔥 ${stats.streakDays}",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.onBackground
                        )
                    }
                }
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 28.dp)
                    .animateContentSize()
            ) {
                Spacer(Modifier.height(6.dp))
                HeroStreakCard(stats)
                Spacer(Modifier.height(14.dp))
                TodayDashboard(stats)
                Spacer(Modifier.height(14.dp))
                InsightRow(stats)

                Spacer(Modifier.height(22.dp))
                Text("Aktivitas", style = MaterialTheme.typography.headlineSmall, color = colors.onBackground)
                Spacer(Modifier.height(4.dp))
                Text("Lihat ritme baca & dengar dari waktu ke waktu", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StreakRange.entries.forEach { r ->
                        FilterChip(
                            selected = range == r,
                            onClick = { range = r },
                            label = { Text(r.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = colors.onBackground,
                                selectedLabelColor = colors.background,
                                containerColor = colors.surfaceVariant,
                                labelColor = colors.onSurfaceVariant
                            ),
                            border = if (range == r) null else BorderStroke(1.dp, colors.outlineVariant)
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))

                AnimatedContent(
                    targetState = range,
                    transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(160)) },
                    label = "streakRange"
                ) { r ->
                    when (r) {
                        StreakRange.DAILY -> DailyView(stats)
                        StreakRange.WEEKLY -> WeeklyView(stats)
                        StreakRange.MONTHLY -> MonthlyView(stats)
                    }
                }

                Spacer(Modifier.height(18.dp))
                Text(
                    "Diperbarui langsung dari ayat yang terlihat saat dibaca dan ayat yang diputar.",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun HeroStreakCard(stats: StreakStats) {
    val colors = MaterialTheme.colorScheme
    val target = 30f
    val targetProgress = (stats.streakDays / target).coerceIn(0f, 1f)
    val progress by animateFloatAsState(targetProgress, tween(900), label = "heroRing")
    Surface(
        shape = RoundedCornerShape(30.dp),
        color = colors.inverseSurface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .size(180.dp)
                    .align(Alignment.TopEnd)
                    .background(
                        Brush.radialGradient(
                            listOf(colors.inverseOnSurface.copy(alpha = 0.12f), Color.Transparent)
                        ),
                        CircleShape
                    )
            )
            Row(
                modifier = Modifier.padding(22.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(104.dp)) {
                    Canvas(modifier = Modifier.size(104.dp)) {
                        val stroke = 10.dp.toPx()
                        drawArc(
                            color = colors.inverseOnSurface.copy(alpha = 0.15f),
                            startAngle = -220f, sweepAngle = 260f, useCenter = false,
                            style = Stroke(width = stroke, cap = StrokeCap.Round)
                        )
                        drawArc(
                            color = colors.inverseOnSurface,
                            startAngle = -220f, sweepAngle = 260f * progress, useCenter = false,
                            style = Stroke(width = stroke, cap = StrokeCap.Round)
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${stats.streakDays}", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = colors.inverseOnSurface)
                        Text("hari", style = MaterialTheme.typography.labelSmall, color = colors.inverseOnSurface.copy(alpha = 0.7f))
                    }
                }
                Spacer(Modifier.width(18.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Streak Saat Ini", style = MaterialTheme.typography.labelMedium, color = colors.inverseOnSurface.copy(alpha = 0.7f), letterSpacing = 1.sp)
                    Spacer(Modifier.height(4.dp))
                    val status = when {
                        stats.streakDays >= 30 -> "Luar biasa, istiqamah!"
                        stats.streakDays >= 7 -> "Konsisten, teruskan!"
                        stats.streakDays >= 1 -> "Bagus, jaga ritmenya"
                        else -> "Mulai hari ini"
                    }
                    Text(status, style = MaterialTheme.typography.titleLarge, color = colors.inverseOnSurface)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Target 30 hari · ${stats.activeDays30} hari aktif (30 hari)",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.inverseOnSurface.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
private fun TodayDashboard(stats: StreakStats) {
    val colors = MaterialTheme.colorScheme
    val todayTotal = stats.todayRead + stats.todayPlay
    val goal = 20
    val targetP = (todayTotal / goal.toFloat()).coerceIn(0f, 1f)
    val p by animateFloatAsState(targetP, tween(700), label = "todayGoal")
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = colors.surfaceVariant,
        border = BorderStroke(1.dp, colors.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp).animateContentSize()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Hari Ini", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = colors.onBackground)
                Spacer(Modifier.weight(1f))
                Text("$todayTotal aktivitas", style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
            }
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MiniMetric("Dibaca", "${stats.todayRead}", "ayat", Modifier.weight(1f))
                MiniMetric("Diputar", "${stats.todayPlay}", "ayat", Modifier.weight(1f))
                MiniMetric("Rata-rata", "${stats.avgDaily7}", "/hari", Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { p },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                color = colors.onBackground,
                trackColor = colors.outlineVariant
            )
            Spacer(Modifier.height(6.dp))
            Text("Target harian ringan: $goal aktivitas", style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
        }
    }
}

@Composable
private fun MiniMetric(title: String, value: String, unit: String, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Surface(modifier = modifier, shape = RoundedCornerShape(16.dp), color = colors.background, border = BorderStroke(1.dp, colors.outlineVariant)) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, color = colors.onBackground)
            Text(unit, style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
        }
    }
}

@Composable
private fun InsightRow(stats: StreakStats) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        InsightCard("7 Hari", "${stats.weekRead + stats.weekPlay}", "total aktivitas", Modifier.weight(1f))
        InsightCard("30 Hari", "${stats.monthRead + stats.monthPlay}", "total aktivitas", Modifier.weight(1f))
        InsightCard("Terbaik", if (stats.bestDayCount > 0) "${stats.bestDayCount}" else "-", stats.bestDayLabel.ifBlank { "belum ada" }, Modifier.weight(1f))
    }
}

@Composable
private fun InsightCard(title: String, value: String, sub: String, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Surface(modifier = modifier, shape = RoundedCornerShape(18.dp), color = colors.surfaceVariant, border = BorderStroke(1.dp, colors.outlineVariant)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.headlineSmall, color = colors.onBackground)
            Text(sub, style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant, maxLines = 1)
        }
    }
}

@Composable
private fun DailyView(stats: StreakStats) {
    val colors = MaterialTheme.colorScheme
    Surface(shape = RoundedCornerShape(24.dp), color = colors.surfaceVariant, border = BorderStroke(1.dp, colors.outlineVariant), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp).animateContentSize()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Kalender Aktivitas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = colors.onBackground)
                Spacer(Modifier.weight(1f))
                Text("35 hari", style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
            }
            Spacer(Modifier.height(12.dp))
            Heatmap(stats)
            Spacer(Modifier.height(10.dp))
            Legend()
            Spacer(Modifier.height(14.dp))
            Text("Rincian terbaru", style = MaterialTheme.typography.labelLarge, color = colors.onBackground)
            Spacer(Modifier.height(8.dp))
            val recent = stats.days.takeLast(7).reversed()
            recent.forEach { d ->
                val total = d.readCount + d.playCount
                val label = dayLabel(d.date)
                val targetW = (total / 40f).coerceIn(0f, 1f)
                val w by animateFloatAsState(targetW, tween(500), label = "daybar")
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(label, style = MaterialTheme.typography.labelMedium, color = colors.onBackground, modifier = Modifier.weight(1f))
                    Text("${d.readCount} baca · ${d.playCount} putar", style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                    Spacer(Modifier.width(8.dp))
                    Box(modifier = Modifier.width(46.dp).height(6.dp).clip(RoundedCornerShape(3.dp)).background(colors.outlineVariant)) {
                        Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(w).background(colors.onBackground, RoundedCornerShape(3.dp)))
                    }
                }
            }
        }
    }
}

@Composable
private fun Heatmap(stats: StreakStats) {
    val colors = MaterialTheme.colorScheme
    Column {
        stats.days.chunked(7).forEach { week ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
                week.forEach { d ->
                    val total = d.readCount + d.playCount
                    val (bg, fg) = when {
                        total >= 40 -> colors.onBackground to colors.background
                        total >= 20 -> colors.onBackground.copy(alpha = 0.7f) to colors.background
                        total >= 5 -> colors.onBackground.copy(alpha = 0.4f) to colors.onBackground
                        total > 0 -> colors.onBackground.copy(alpha = 0.18f) to colors.onBackground
                        else -> colors.background to colors.onSurfaceVariant
                    }
                    val dayNum = try { SimpleDateFormat("d", Locale.forLanguageTag("id-ID")).format(SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(d.date)!!) } catch (_: Throwable) { "" }
                    Surface(color = bg, shape = RoundedCornerShape(8.dp), border = if (total == 0) BorderStroke(1.dp, colors.outlineVariant) else null, modifier = Modifier.weight(1f).height(32.dp)) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(dayNum, style = MaterialTheme.typography.labelSmall, color = fg)
                        }
                    }
                }
                repeat((7 - week.size).coerceAtLeast(0)) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun Legend() {
    val colors = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Sedikit", style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
        listOf(0.18f, 0.4f, 0.7f, 1f).forEach { a ->
            Box(modifier = Modifier.size(12.dp).clip(RoundedCornerShape(3.dp)).background(colors.onBackground.copy(alpha = a)))
        }
        Text("Banyak", style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
    }
}

@Composable
private fun WeeklyView(stats: StreakStats) {
    val colors = MaterialTheme.colorScheme
    val maxVal = max(1, stats.weeks.maxOfOrNull { it.readCount + it.playCount } ?: 1)
    Surface(shape = RoundedCornerShape(24.dp), color = colors.surfaceVariant, border = BorderStroke(1.dp, colors.outlineVariant), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp).animateContentSize()) {
            Text("Tren 8 Minggu", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = colors.onBackground)
            Spacer(Modifier.height(14.dp))
            Row(modifier = Modifier.fillMaxWidth().height(140.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
                stats.weeks.forEach { w ->
                    val total = w.readCount + w.playCount
                    val targetH = (total / maxVal.toFloat()).coerceIn(0.06f, 1f)
                    val h by animateFloatAsState(targetH, tween(600), label = "bar")
                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("$total", style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                        Spacer(Modifier.height(4.dp))
                        Box(
                            modifier = Modifier.fillMaxWidth().height((100 * h).dp)
                                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 4.dp, bottomEnd = 4.dp))
                                .background(if (total > 0) colors.onBackground else colors.outlineVariant)
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(w.label.take(6), style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant, maxLines = 1)
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            stats.weeks.takeLast(4).reversed().forEach { w ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Minggu ${w.label}", style = MaterialTheme.typography.labelLarge, color = colors.onBackground)
                        Text("${w.activeDays} hari aktif · ${w.readCount} baca · ${w.playCount} putar", style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthlyView(stats: StreakStats) {
    val colors = MaterialTheme.colorScheme
    val maxVal = max(1, stats.months.maxOfOrNull { it.readCount + it.playCount } ?: 1)
    Surface(shape = RoundedCornerShape(24.dp), color = colors.surfaceVariant, border = BorderStroke(1.dp, colors.outlineVariant), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp).animateContentSize()) {
            Text("Ringkasan 6 Bulan", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = colors.onBackground)
            Spacer(Modifier.height(14.dp))
            stats.months.forEach { m ->
                val total = m.readCount + m.playCount
                val targetP = (total / maxVal.toFloat()).coerceIn(0f, 1f)
                val p by animateFloatAsState(targetP, tween(600), label = "month")
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(m.label, style = MaterialTheme.typography.labelLarge, color = colors.onBackground, modifier = Modifier.width(72.dp))
                        Box(modifier = Modifier.weight(1f).height(10.dp).clip(RoundedCornerShape(5.dp)).background(colors.outlineVariant)) {
                            Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(p).background(colors.onBackground, RoundedCornerShape(5.dp)))
                        }
                        Spacer(Modifier.width(10.dp))
                        Text("$total", style = MaterialTheme.typography.labelMedium, color = colors.onBackground)
                    }
                    Text("${m.activeDays} hari aktif · ${m.readCount} baca · ${m.playCount} putar", style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant, modifier = Modifier.padding(start = 72.dp, top = 2.dp))
                }
            }
        }
    }
}

private fun dayLabel(date: String): String = try {
    val d = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(date)!!
    SimpleDateFormat("EEE, d MMM", Locale.forLanguageTag("id-ID")).format(d)
} catch (_: Throwable) { date }
