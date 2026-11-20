package com.quriva.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.quriva.app.R
import com.quriva.app.data.AppPreferences
import com.quriva.app.ui.ProgressViewModel
import com.quriva.app.ui.StreakRange
import java.text.SimpleDateFormat
import java.util.Locale

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
            Row(
                modifier = Modifier.fillMaxWidth().height(58.dp).padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(painter = painterResource(R.drawable.ic_back), contentDescription = "Kembali", tint = colors.onBackground, modifier = Modifier.size(22.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Daily Streak", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = colors.onBackground)
                    Text("Kebiasaan baca & dengar", style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 28.dp)
            ) {
                Spacer(Modifier.height(8.dp))
                Surface(shape = RoundedCornerShape(26.dp), color = colors.inverseSurface, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(painter = painterResource(R.drawable.ic_book_open), contentDescription = null, tint = colors.inverseOnSurface, modifier = Modifier.size(28.dp))
                        Spacer(Modifier.height(10.dp))
                        Text("${stats.streakDays} hari", style = MaterialTheme.typography.headlineMedium, color = colors.inverseOnSurface)
                        Text("streak saat ini", style = MaterialTheme.typography.bodySmall, color = colors.inverseOnSurface.copy(alpha = 0.7f))
                    }
                }

                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    StatCard("Hari ini", "${stats.todayRead}", "ayat dibaca", Modifier.weight(1f))
                    StatCard("Hari ini", "${stats.todayPlay}", "ayat diputar", Modifier.weight(1f))
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    StatCard("Total", "${stats.totalRead}", "ayat dibaca", Modifier.weight(1f))
                    StatCard("Total", "${stats.totalPlay}", "ayat diputar", Modifier.weight(1f))
                }

                Spacer(Modifier.height(22.dp))
                Text("Aktivitas", style = MaterialTheme.typography.labelLarge, color = colors.onBackground)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                    StreakRange.entries.forEach { r ->
                        FilterChip(selected = range == r, onClick = { range = r }, label = { Text(r.label) })
                    }
                }
                Spacer(Modifier.height(12.dp))

                when (range) {
                    StreakRange.DAILY -> DailyCalendar(stats)
                    StreakRange.WEEKLY -> WeeklyList(stats)
                    StreakRange.MONTHLY -> MonthlyList(stats)
                }

                Spacer(Modifier.height(18.dp))
                Text(
                    "Aktivitas dihitung dari ayat yang dibaca dan ayat yang diputar.",
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
private fun DailyCalendar(stats: com.quriva.app.ui.StreakStats) {
    val colors = MaterialTheme.colorScheme
    val dayFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val labelFmt = SimpleDateFormat("d", Locale("id", "ID"))
    // show last 30 days as a grid-like rows of 7
    val days = stats.days
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        days.chunked(7).forEach { week ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                week.forEach { d ->
                    val active = d.readCount > 0 || d.playCount > 0
                    val level = when {
                        d.readCount + d.playCount >= 50 -> 1.0f
                        d.readCount + d.playCount >= 20 -> 0.75f
                        d.readCount + d.playCount > 0 -> 0.45f
                        else -> 0f
                    }
                    val bg = if (active) colors.onBackground.copy(alpha = 0.15f + 0.55f * level) else colors.surfaceVariant
                    val fg = if (active && level >= 0.45f) colors.background else colors.onSurfaceVariant
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(bg),
                            contentAlignment = Alignment.Center
                        ) {
                            val dayNum = try { labelFmt.format(dayFmt.parse(d.date)!!) } catch (_: Throwable) { "" }
                            Text(dayNum, style = MaterialTheme.typography.labelSmall, color = fg)
                        }
                    }
                }
                repeat((7 - week.size).coerceAtLeast(0)) { Spacer(Modifier.weight(1f)) }
            }
        }
        Spacer(Modifier.height(4.dp))
        Text("30 hari terakhir · warna lebih pekat = lebih aktif", style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
    }
}

@Composable
private fun WeeklyList(stats: com.quriva.app.ui.StreakStats) {
    val colors = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        stats.weeks.forEach { w ->
            Surface(shape = RoundedCornerShape(16.dp), color = colors.surfaceVariant, border = BorderStroke(1.dp, colors.outlineVariant), modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Minggu ${w.label}", style = MaterialTheme.typography.labelLarge, color = colors.onBackground)
                        Text("${w.activeDays} hari aktif", style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("${w.readCount} baca", style = MaterialTheme.typography.labelMedium, color = colors.onBackground)
                        Text("${w.playCount} putar", style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthlyList(stats: com.quriva.app.ui.StreakStats) {
    val colors = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        stats.months.forEach { m ->
            Surface(shape = RoundedCornerShape(16.dp), color = colors.surfaceVariant, border = BorderStroke(1.dp, colors.outlineVariant), modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(m.label, style = MaterialTheme.typography.labelLarge, color = colors.onBackground)
                        Text("${m.activeDays} hari aktif", style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("${m.readCount} baca", style = MaterialTheme.typography.labelMedium, color = colors.onBackground)
                        Text("${m.playCount} putar", style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(title: String, value: String, unit: String, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Surface(modifier = modifier, shape = RoundedCornerShape(18.dp), color = colors.surfaceVariant, border = BorderStroke(1.dp, colors.outlineVariant)) {
        Column(modifier = Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
            Spacer(Modifier.height(6.dp))
            Text(value, style = MaterialTheme.typography.headlineSmall, color = colors.onBackground, textAlign = TextAlign.Center)
            Text(unit, style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant, textAlign = TextAlign.Center)
        }
    }
}
