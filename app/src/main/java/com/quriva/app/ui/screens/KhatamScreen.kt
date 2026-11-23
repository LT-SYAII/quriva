package com.quriva.app.ui.screens

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quriva.app.R
import com.quriva.app.data.AppPreferences
import com.quriva.app.data.KhatamPlanner
import com.quriva.app.data.KhatamStats
import java.util.Locale
import kotlin.math.roundToInt

private val khatamTargets = listOf(30, 60, 90, 180, 365)

@Composable
fun KhatamScreen(
    prefs: AppPreferences,
    onBack: () -> Unit,
    onOpenReading: (Int, Int) -> Unit
) {
    val context = LocalContext.current
    val stats = remember(
        prefs.lastRead,
        prefs.khatamStartDate,
        prefs.khatamStartVerse,
        prefs.khatamTargetDays
    ) {
        KhatamPlanner.compute(context, prefs)
    }
    val colors = MaterialTheme.colorScheme
    var confirmReset by remember { mutableStateOf(false) }
    val lastRead = prefs.lastRead
    val currentLabel = lastRead?.let { "${it.surahName} ${it.ayah}" } ?: "Awal Al-Qur'an"
    val currentVerse = remember(lastRead, stats.startVerse) {
        KhatamPlanner.currentVerse(context, lastRead)
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Mulai ulang rencana?") },
            text = {
                Text(
                    "Progres akan dihitung lagi dari posisi baca saat ini ($currentLabel). " +
                        "Riwayat bacaan tetap tersimpan."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        prefs.startKhatamPlan(prefs.khatamTargetDays, currentVerse)
                        confirmReset = false
                    }
                ) { Text("Mulai ulang") }
            },
            dismissButton = {
                TextButton(onClick = { confirmReset = false }) { Text("Batal") }
            }
        )
    }

    Surface(Modifier.fillMaxSize(), color = colors.background) {
        Scaffold(
            modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing),
            containerColor = colors.background,
            topBar = {
                FeatureTopBar(
                    title = "Rencana Khatam",
                    subtitle = if (stats.started) {
                        "Target ${stats.targetDays} hari · ${stats.elapsedDays} hari berjalan"
                    } else {
                        "Susun target tilawah yang realistis"
                    },
                    onBack = onBack
                )
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(top = padding.calculateTopPadding()),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 38.dp)
            ) {
                item(key = "khatam-hero") {
                    KhatamHero(stats = stats)
                    Spacer(Modifier.height(14.dp))
                }

                item(key = "khatam-target") {
                    KhatamTargetCard(
                        stats = stats,
                        onTargetSelected = { days ->
                            if (stats.started) prefs.updateKhatamTarget(days)
                            else prefs.startKhatamPlan(days, currentVerse)
                        }
                    )
                    Spacer(Modifier.height(14.dp))
                }

                if (stats.started) {
                    item(key = "khatam-projection") {
                        KhatamProjectionCard(stats)
                        Spacer(Modifier.height(14.dp))
                    }
                    item(key = "khatam-reset") {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = colors.surface,
                            border = BorderStroke(1.dp, colors.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        "Posisi sekarang",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        currentLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colors.onSurfaceVariant
                                    )
                                }
                                TextButton(onClick = { confirmReset = true }) {
                                    Text("Mulai ulang")
                                }
                            }
                        }
                        Spacer(Modifier.height(14.dp))
                    }
                } else {
                    item(key = "khatam-start-note") {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = colors.surfaceVariant,
                            border = BorderStroke(1.dp, colors.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text(
                                    "Mulai dari posisi sekarang",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(Modifier.height(3.dp))
                                Text(
                                    if (lastRead == null) {
                                        "Belum ada riwayat bacaan. Rencana dimulai dari Al-Fatihah."
                                    } else {
                                        "Rencana dimulai dari $currentLabel, bukan dari Al-Fatihah."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(Modifier.height(14.dp))
                    }
                }

                item(key = "khatam-guide") {
                    KhatamGuideCard(stats)
                    Spacer(Modifier.height(16.dp))
                }

                item(key = "khatam-action") {
                    Button(
                        onClick = {
                            val target = prefs.lastRead
                            onOpenReading(target?.surahNumber ?: 1, target?.ayah ?: 1)
                        },
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.onBackground,
                            contentColor = colors.background
                        ),
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    ) {
                        Text(
                            if (stats.started) "Lanjutkan tilawah" else "Mulai membaca",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun KhatamHero(stats: KhatamStats) {
    val colors = MaterialTheme.colorScheme
    val percent = (stats.percent * 100).roundToInt()
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = colors.inverseSurface,
        contentColor = colors.inverseOnSurface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(Modifier.fillMaxWidth()) {
            Box(
                Modifier.size(220.dp).align(Alignment.TopEnd).background(
                    Brush.radialGradient(
                        listOf(colors.inverseOnSurface.copy(alpha = .12f), Color.Transparent)
                    ),
                    CircleShape
                )
            )
            Column(Modifier.padding(21.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(40.dp).background(
                            colors.inverseOnSurface.copy(alpha = .12f),
                            RoundedCornerShape(13.dp)
                        ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painterResource(R.drawable.ic_book_open),
                            contentDescription = null,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                    Spacer(Modifier.width(11.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            "PROGRES KHATAM",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.3.sp,
                            color = colors.inverseOnSurface.copy(alpha = .6f)
                        )
                        Text(
                            if (stats.started) {
                                "${stats.readVerses} ayat dibaca · mulai ayat ${stats.startVerse + 1}"
                            } else {
                                "Belum dimulai"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.inverseOnSurface.copy(alpha = .72f)
                        )
                    }
                    Surface(shape = CircleShape, color = colors.inverseOnSurface.copy(alpha = .12f)) {
                        Text(
                            "$percent%",
                            Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(Modifier.height(18.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        "$percent",
                        fontSize = 62.sp,
                        fontWeight = FontWeight.Light,
                        lineHeight = 62.sp
                    )
                    Text(
                        "%",
                        style = MaterialTheme.typography.headlineSmall,
                        color = colors.inverseOnSurface.copy(alpha = .65f),
                        modifier = Modifier.padding(bottom = 8.dp, start = 2.dp)
                    )
                    Spacer(Modifier.weight(1f))
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            "${stats.remainingVerses} ayat tersisa",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            if (stats.started) "Halaman ${stats.currentPage} dari 604"
                            else "${stats.totalVerses} ayat total",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.inverseOnSurface.copy(alpha = .62f)
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                LinearProgressIndicator(
                    progress = { stats.percent },
                    modifier = Modifier.fillMaxWidth().height(5.dp).clip(CircleShape),
                    color = colors.inverseOnSurface,
                    trackColor = colors.inverseOnSurface.copy(alpha = .15f)
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    if (stats.started) {
                        if (stats.remainingVerses == 0) {
                            "Selamat, target khatam tercapai. Semoga menjadi amal jariyah."
                        } else {
                            "Sisa ${stats.remainingDays} hari · target ${KhatamPlanner.todayDailyTarget(stats)} per hari"
                        }
                    } else {
                        "Pilih target hari di bawah untuk memulai rencana khatam."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.inverseOnSurface.copy(alpha = .74f)
                )
            }
        }
    }
}

@Composable
private fun KhatamTargetCard(stats: KhatamStats, onTargetSelected: (Int) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = colors.surfaceVariant,
        border = BorderStroke(1.dp, colors.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "Target khatam",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                if (stats.started) {
                    "Ubah target kapan saja. Target harian menyesuaikan sisa ayat."
                } else {
                    "Ketuk salah satu target untuk memulai rencana."
                },
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant
            )
            Spacer(Modifier.height(10.dp))
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                khatamTargets.forEach { days ->
                    FilterChip(
                        selected = stats.targetDays == days,
                        onClick = { onTargetSelected(days) },
                        label = { Text("$days hari") }
                    )
                }
            }
        }
    }
}

@Composable
private fun KhatamProjectionCard(stats: KhatamStats) {
    val colors = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = colors.surfaceVariant,
        border = BorderStroke(1.dp, colors.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "Proyeksi harian",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "Berdasarkan target ${stats.targetDays} hari",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant
                    )
                }
                Surface(
                    shape = CircleShape,
                    color = if (stats.onTrack) colors.inverseSurface else colors.background,
                    contentColor = if (stats.onTrack) colors.inverseOnSurface else colors.onBackground,
                    border = if (stats.onTrack) null else BorderStroke(1.dp, colors.outlineVariant)
                ) {
                    Text(
                        if (stats.onTrack) "Sesuai jalur" else "Perlu dikejar",
                        Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                KhatamStat("Harian", KhatamPlanner.todayDailyTarget(stats), Modifier.weight(1f))
                KhatamStat(
                    "Sisa hari",
                    "${stats.remainingDays} hari",
                    Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                KhatamStat(
                    "Halaman",
                    "${stats.remainingPages} lagi",
                    Modifier.weight(1f)
                )
                KhatamStat(
                    "Perkiraan",
                    stats.estimatedFinishDays?.let { "±$it hari" } ?: "—",
                    Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun KhatamStat(label: String, value: String, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(colors.background)
            .padding(horizontal = 13.dp, vertical = 11.dp)
    ) {
        Text(
            label.uppercase(Locale.forLanguageTag("id-ID")),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            letterSpacing = .8.sp,
            color = colors.onSurfaceVariant
        )
        Spacer(Modifier.height(2.dp))
        Text(
            value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2
        )
    }
}

@Composable
private fun KhatamGuideCard(stats: KhatamStats) {
    val colors = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = colors.surface,
        border = BorderStroke(1.dp, colors.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "Cara kerja",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(9.dp))
            listOf(
                "Progres dihitung otomatis dari posisi ayat terakhir yang dibaca.",
                "Target harian dibagi rata dari sisa ayat dan sisa hari.",
                "Tandai tilawah di pelacak ibadah agar ritme harian tetap terjaga."
            ).forEachIndexed { index, text ->
                Row(Modifier.padding(vertical = 4.dp)) {
                    Box(
                        Modifier.size(22.dp).background(colors.surfaceVariant, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "${index + 1}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.width(9.dp))
                    Text(
                        text,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            if (!stats.started) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Rencana belum aktif. Pilih target hari di atas untuk memulai.",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
