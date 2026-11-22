package com.quriva.app.ui.screens

import androidx.compose.foundation.BorderStroke
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.quriva.app.R
import com.quriva.app.data.AppPreferences
import com.quriva.app.data.IslamicCalendar
import com.quriva.app.data.IslamicReminderScheduler
import com.quriva.app.model.UpcomingIslamicEvent
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@Composable
fun IslamicCalendarScreen(prefs: AppPreferences, onBack: () -> Unit) {
    val context = LocalContext.current
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { delay(60_000); now = System.currentTimeMillis() } }
    val today = remember(now, prefs.hijriAdjustment) { IslamicCalendar.today(prefs.hijriAdjustment) }
    val events = remember(now, prefs.hijriAdjustment) { IslamicCalendar.upcoming(prefs.hijriAdjustment) }
    val c = MaterialTheme.colorScheme
    Surface(Modifier.fillMaxSize(), color = c.background) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            FeatureTopBar("Kalender Islam", "Indonesia · tanggal perkiraan", onBack)
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
                item {
                    Surface(shape = RoundedCornerShape(26.dp), color = c.inverseSurface, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(IslamicCalendar.hijriLabel(today), style = MaterialTheme.typography.headlineMedium, color = c.inverseOnSurface, textAlign = TextAlign.Center)
                            Text(SimpleDateFormat("EEEE, d MMMM yyyy", Locale.forLanguageTag("id-ID")).format(Date(now)), color = c.inverseOnSurface.copy(alpha = .7f))
                            Spacer(Modifier.height(12.dp)); Text("Tanggal Hijriah dapat berbeda mengikuti keputusan resmi.", style = MaterialTheme.typography.labelSmall, color = c.inverseOnSurface.copy(alpha = .65f), textAlign = TextAlign.Center)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Surface(shape = RoundedCornerShape(18.dp), color = c.surfaceVariant, border = BorderStroke(1.dp, c.outlineVariant), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) { Text("Koreksi kalender", fontWeight = FontWeight.SemiBold); Text("Sesuaikan -2 sampai +2 hari", style = MaterialTheme.typography.labelSmall) }
                                IconButton(onClick = { prefs.updateIslamicEvents(prefs.islamicEventNotifications, prefs.hijriAdjustment - 1) }) { Text("−") }
                                Text(if (prefs.hijriAdjustment > 0) "+${prefs.hijriAdjustment}" else prefs.hijriAdjustment.toString())
                                IconButton(onClick = { prefs.updateIslamicEvents(prefs.islamicEventNotifications, prefs.hijriAdjustment + 1) }) { Text("+") }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Notifikasi H-7, H-1, dan hari H", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                                Switch(checked = prefs.islamicEventNotifications, onCheckedChange = {
                                    prefs.updateIslamicEvents(it)
                                    if (it) IslamicReminderScheduler.scheduleEvents(context) else IslamicReminderScheduler.cancelEvents(context)
                                })
                            }
                        }
                    }
                    Spacer(Modifier.height(18.dp)); Text("Peristiwa Mendatang", style = MaterialTheme.typography.headlineSmall)
                    val monday = TimeUnit.MILLISECONDS.toDays((IslamicCalendar.nextWeekday(java.util.Calendar.MONDAY) - now).coerceAtLeast(0))
                    val thursday = TimeUnit.MILLISECONDS.toDays((IslamicCalendar.nextWeekday(java.util.Calendar.THURSDAY) - now).coerceAtLeast(0))
                    Text("Puasa Senin berikutnya: $monday hari lagi · Kamis: $thursday hari lagi", style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant, modifier = Modifier.padding(vertical = 8.dp))
                }
                items(events, key = { it.event.id + it.hijriYear }) { event -> EventCard(event, now) }
                item { Spacer(Modifier.height(30.dp)) }
            }
        }
    }
}

@Composable
private fun EventCard(item: UpcomingIslamicEvent, now: Long) {
    val c = MaterialTheme.colorScheme
    val remaining = (item.targetMillis - now).coerceAtLeast(0)
    val days = TimeUnit.MILLISECONDS.toDays(remaining)
    val hours = TimeUnit.MILLISECONDS.toHours(remaining) % 24
    Surface(shape = RoundedCornerShape(20.dp), color = c.surfaceVariant, border = BorderStroke(1.dp, c.outlineVariant), modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = c.onBackground, contentColor = c.background, modifier = Modifier.size(56.dp)) { Box(contentAlignment = Alignment.Center) { Text("$days\nhari", textAlign = TextAlign.Center, style = MaterialTheme.typography.labelMedium) } }
            Spacer(Modifier.width(14.dp)); Column(Modifier.weight(1f)) {
                Text(item.event.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("${item.event.hijriDay} ${IslamicCalendar.monthNames[item.event.hijriMonth - 1]} ${item.hijriYear} H · $days hari $hours jam", style = MaterialTheme.typography.labelSmall, color = c.onSurfaceVariant)
                Text(item.event.description, style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun FridayScreen(prefs: AppPreferences, onBack: () -> Unit, onOpenKahf: () -> Unit) {
    val context = LocalContext.current
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { delay(60_000); now = System.currentTimeMillis() } }
    val target = IslamicCalendar.nextFriday(prefs.fridayReminderHour, prefs.fridayReminderMinute)
    val remaining = (target - now).coerceAtLeast(0); val days = TimeUnit.MILLISECONDS.toDays(remaining); val hours = TimeUnit.MILLISECONDS.toHours(remaining) % 24; val mins = TimeUnit.MILLISECONDS.toMinutes(remaining) % 60
    val c = MaterialTheme.colorScheme
    Surface(Modifier.fillMaxSize(), color = c.background) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            FeatureTopBar("Hari Jumat", "Al-Kahf · doa · selawat", onBack)
            Column(Modifier.fillMaxSize().padding(20.dp)) {
                Surface(shape = RoundedCornerShape(28.dp), color = c.inverseSurface, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Jumat berikutnya", color = c.inverseOnSurface.copy(alpha=.7f)); Text("$days hari $hours jam $mins menit", style = MaterialTheme.typography.headlineMedium, color = c.inverseOnSurface, textAlign = TextAlign.Center)
                    }
                }
                Spacer(Modifier.height(16.dp)); Button(onClick = onOpenKahf, modifier = Modifier.fillMaxWidth(), shape = CircleShape) { Icon(painterResource(R.drawable.ic_book_open), null); Spacer(Modifier.width(8.dp)); Text("Baca Surah Al-Kahf") }
                Spacer(Modifier.height(16.dp)); Text("Amalan Jumat", style = MaterialTheme.typography.headlineSmall)
                listOf("Membaca Surah Al-Kahf", "Memperbanyak selawat", "Memperbanyak doa dan istigfar", "Mempersiapkan salat Jumat").forEach { Text("✓  $it", modifier = Modifier.padding(vertical = 7.dp)) }
                Spacer(Modifier.height(18.dp)); Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) { Text("Pengingat Jumat", fontWeight = FontWeight.SemiBold); Text("Setiap Jumat sekitar %02d:%02d".format(prefs.fridayReminderHour, prefs.fridayReminderMinute), style = MaterialTheme.typography.labelSmall) }
                    Switch(checked = prefs.fridayReminderEnabled, onCheckedChange = {
                        prefs.updateFridayReminder(it)
                        if (it) IslamicReminderScheduler.scheduleFriday(context, prefs.fridayReminderHour, prefs.fridayReminderMinute)
                        else IslamicReminderScheduler.cancelFriday(context)
                    })
                }
                OutlinedButton(onClick = {
                    TimePickerDialog(context, { _, h, m ->
                        prefs.updateFridayReminder(true, h, m)
                        IslamicReminderScheduler.scheduleFriday(context, h, m)
                    }, prefs.fridayReminderHour, prefs.fridayReminderMinute, true).show()
                }, modifier = Modifier.fillMaxWidth(), shape = CircleShape) { Text("Ubah waktu pengingat Jumat") }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Pengingat Kamis pukul 18.00", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                    Switch(prefs.thursdayNightReminder, {
                        prefs.updateFridayReminder(prefs.fridayReminderEnabled, thursdayNight = it)
                        if (it) IslamicReminderScheduler.scheduleThursdayNight(context) else IslamicReminderScheduler.cancelThursdayNight(context)
                    })
                }
            }
        }
    }
}
