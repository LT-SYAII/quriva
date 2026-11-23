package com.quriva.app.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quriva.app.R
import com.quriva.app.data.AppPreferences
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class WorshipItem(val id: String, val label: String)

private val prayerItems = listOf(
    WorshipItem("subuh", "Subuh"),
    WorshipItem("dzuhur", "Dzuhur"),
    WorshipItem("ashar", "Ashar"),
    WorshipItem("maghrib", "Maghrib"),
    WorshipItem("isya", "Isya")
)

private val practiceItems = listOf(
    WorshipItem("dzikir_pagi", "Dzikir pagi"),
    WorshipItem("dzikir_petang", "Dzikir petang"),
    WorshipItem("tilawah", "Tilawah"),
    WorshipItem("hafalan", "Hafalan")
)

@Composable
fun WorshipTrackerCard(
    prefs: AppPreferences,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val allItems = remember { prayerItems + practiceItems }
    val done = allItems.count { prefs.isWorshipDone(it.id) }
    val dateLabel = remember {
        SimpleDateFormat("EEEE, d MMMM", Locale.forLanguageTag("id-ID")).format(Date())
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = colors.surfaceVariant,
        border = BorderStroke(1.dp, colors.outlineVariant)
    ) {
        Column(Modifier.padding(16.dp).animateContentSize()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "Ibadah hari ini",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        dateLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant
                    )
                }
                Surface(shape = CircleShape, color = colors.background) {
                    Text(
                        "$done/${allItems.size}",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(Modifier.height(11.dp))
            LinearProgressIndicator(
                progress = { done.toFloat() / allItems.size },
                modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                color = colors.onBackground,
                trackColor = colors.outlineVariant
            )
            Spacer(Modifier.height(13.dp))
            WorshipRow(label = "SALAT", items = prayerItems, prefs = prefs)
            Spacer(Modifier.height(8.dp))
            WorshipRow(label = "AMALAN", items = practiceItems, prefs = prefs)
            Spacer(Modifier.height(7.dp))
            Text(
                "Ketuk untuk menandai yang sudah dilakukan hari ini.",
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun WorshipRow(
    label: String,
    items: List<WorshipItem>,
    prefs: AppPreferences
) {
    val colors = MaterialTheme.colorScheme
    Column {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = colors.onSurfaceVariant,
            letterSpacing = 0.9.sp
        )
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            items.forEach { item ->
                val done = prefs.isWorshipDone(item.id)
                Surface(
                    onClick = { prefs.toggleWorship(item.id) },
                    shape = RoundedCornerShape(15.dp),
                    color = if (done) colors.inverseSurface else colors.background,
                    contentColor = if (done) colors.inverseOnSurface else colors.onBackground,
                    border = if (done) null else BorderStroke(1.dp, colors.outlineVariant)
                ) {
                    Row(
                        Modifier.padding(horizontal = 11.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .background(
                                    if (done) colors.inverseOnSurface.copy(alpha = .16f)
                                    else Color.Transparent,
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (done) {
                                Icon(
                                    painterResource(R.drawable.ic_check),
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp),
                                    tint = colors.inverseOnSurface
                                )
                            } else {
                                Box(
                                    Modifier
                                        .size(8.dp)
                                        .background(colors.outline, CircleShape)
                                )
                            }
                        }
                        Spacer(Modifier.width(6.dp))
                        Text(
                            item.label,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (done) FontWeight.SemiBold else FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
