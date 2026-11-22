package com.quriva.app.ui.screens

import android.app.TimePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.quriva.app.data.IslamicCalendar
import com.quriva.app.data.IslamicReminderScheduler
import com.quriva.app.data.NotificationCenter
import com.quriva.app.model.UpcomingIslamicEvent
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@Composable
fun IslamicCalendarScreen(prefs: AppPreferences, onBack: () -> Unit) {
    val context = LocalContext.current
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000)
            now = System.currentTimeMillis()
        }
    }
    val today = remember(now, prefs.hijriAdjustment) { IslamicCalendar.today(prefs.hijriAdjustment) }
    val events = remember(now, prefs.hijriAdjustment) { IslamicCalendar.upcoming(prefs.hijriAdjustment) }
    val colors = MaterialTheme.colorScheme

    Surface(Modifier.fillMaxSize(), color = colors.background) {
        Scaffold(
            modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing),
            containerColor = colors.background,
            topBar = {
                FeatureTopBar(
                    title = "Kalender Islam",
                    subtitle = "Indonesia · tanggal perkiraan",
                    onBack = onBack
                )
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(top = padding.calculateTopPadding()),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 38.dp)
            ) {
                item(key = "date-hero") {
                    HijriDateHero(
                        hijri = IslamicCalendar.hijriLabel(today),
                        gregorian = SimpleDateFormat(
                            "EEEE, d MMMM yyyy",
                            Locale.forLanguageTag("id-ID")
                        ).format(Date(now)),
                        nextEvent = events.firstOrNull(),
                        now = now
                    )
                    Spacer(Modifier.height(14.dp))
                }

                item(key = "calendar-settings") {
                    CalendarSettingsCard(
                        adjustment = prefs.hijriAdjustment,
                        notifications = prefs.islamicEventNotifications,
                        onAdjustment = { adjustment ->
                            prefs.updateIslamicEvents(prefs.islamicEventNotifications, adjustment)
                            if (prefs.islamicEventNotifications) IslamicReminderScheduler.scheduleEvents(context)
                        },
                        onNotifications = { enabled ->
                            prefs.updateIslamicEvents(enabled)
                            if (enabled) IslamicReminderScheduler.scheduleEvents(context)
                            else IslamicReminderScheduler.cancelEvents(context)
                        }
                    )
                    Spacer(Modifier.height(18.dp))
                }

                item(key = "weekly-fasts") {
                    Text("Puasa Pekanan", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "Persiapan puasa sunah terdekat",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(9.dp), modifier = Modifier.fillMaxWidth()) {
                        FastDayCard(
                            day = "Senin",
                            days = calendarDaysUntil(now, IslamicCalendar.nextWeekday(Calendar.MONDAY)),
                            modifier = Modifier.weight(1f)
                        )
                        FastDayCard(
                            day = "Kamis",
                            days = calendarDaysUntil(now, IslamicCalendar.nextWeekday(Calendar.THURSDAY)),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(Modifier.height(26.dp))
                }

                item(key = "event-heading") {
                    Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.weight(1f)) {
                            Text("Peristiwa Mendatang", style = MaterialTheme.typography.headlineSmall)
                            Text("Disusun menurut tanggal terdekat", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                        }
                        Surface(shape = CircleShape, color = colors.surfaceVariant) {
                            Text(
                                events.size.toString(),
                                Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                    Spacer(Modifier.height(11.dp))
                }

                items(events, key = { it.event.id + it.hijriYear }) { event ->
                    EventCard(event = event, now = now)
                    Spacer(Modifier.height(9.dp))
                }

                item(key = "calendar-note") {
                    Spacer(Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = colors.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Tanggal Hijriah adalah perhitungan offline. Awal Ramadan, Idulfitri, dan Iduladha tetap mengikuti pengumuman resmi.",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HijriDateHero(
    hijri: String,
    gregorian: String,
    nextEvent: UpcomingIslamicEvent?,
    now: Long
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = colors.inverseSurface,
        contentColor = colors.inverseOnSurface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(Modifier.fillMaxWidth()) {
            Box(
                Modifier.size(210.dp).align(Alignment.TopEnd).background(
                    Brush.radialGradient(
                        listOf(colors.inverseOnSurface.copy(alpha = .14f), Color.Transparent)
                    ),
                    CircleShape
                )
            )
            Column(Modifier.padding(21.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = CircleShape, color = colors.inverseOnSurface.copy(alpha = .11f)) {
                        Text(
                            "PERKIRAAN",
                            Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = .9.sp
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    Icon(painterResource(R.drawable.ic_schedule), null, Modifier.size(21.dp), tint = colors.inverseOnSurface.copy(alpha = .78f))
                }
                Spacer(Modifier.height(18.dp))
                Text(hijri, style = MaterialTheme.typography.headlineMedium, maxLines = 2)
                Spacer(Modifier.height(4.dp))
                Text(gregorian, style = MaterialTheme.typography.bodyMedium, color = colors.inverseOnSurface.copy(alpha = .66f))
                if (nextEvent != null) {
                    Spacer(Modifier.height(19.dp))
                    HorizontalDivider(color = colors.inverseOnSurface.copy(alpha = .12f))
                    Spacer(Modifier.height(13.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Berikutnya", style = MaterialTheme.typography.labelSmall, color = colors.inverseOnSurface.copy(alpha = .58f))
                            Text(nextEvent.event.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Text(
                            countdownLabel(calendarDaysUntil(now, nextEvent.targetMillis)),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarSettingsCard(
    adjustment: Int,
    notifications: Boolean,
    onAdjustment: (Int) -> Unit,
    onNotifications: (Boolean) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = colors.surfaceVariant,
        border = BorderStroke(1.dp, colors.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Row(Modifier.padding(start = 16.dp, end = 8.dp, top = 13.dp, bottom = 13.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Koreksi kalender", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("Sesuaikan hingga ±2 hari", style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                }
                IconButton(
                    onClick = { onAdjustment(adjustment - 1) },
                    enabled = adjustment > -2,
                    modifier = Modifier.size(48.dp)
                ) { Text("−", style = MaterialTheme.typography.titleLarge) }
                Surface(shape = CircleShape, color = colors.background, border = BorderStroke(1.dp, colors.outlineVariant)) {
                    Text(
                        if (adjustment > 0) "+$adjustment" else adjustment.toString(),
                        Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(
                    onClick = { onAdjustment(adjustment + 1) },
                    enabled = adjustment < 2,
                    modifier = Modifier.size(48.dp)
                ) { Text("+", style = MaterialTheme.typography.titleLarge) }
            }
            HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = colors.outlineVariant)
            Row(Modifier.padding(start = 16.dp, end = 11.dp, top = 10.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Pengingat peristiwa", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("H-7, H-1, dan hari berlangsung", style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                }
                Switch(checked = notifications, onCheckedChange = onNotifications)
            }
        }
    }
}

@Composable
private fun FastDayCard(day: String, days: Long, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = colors.surfaceVariant,
        border = BorderStroke(1.dp, colors.outlineVariant)
    ) {
        Column(Modifier.padding(15.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(7.dp).background(colors.onBackground, CircleShape))
                Spacer(Modifier.width(7.dp))
                Text(day.uppercase(Locale.forLanguageTag("id-ID")), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, letterSpacing = .8.sp)
            }
            Spacer(Modifier.height(10.dp))
            Text(countdownLabel(days), style = MaterialTheme.typography.titleLarge)
            Text("Puasa sunah", style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
        }
    }
}

@Composable
private fun EventCard(event: UpcomingIslamicEvent, now: Long) {
    val colors = MaterialTheme.colorScheme
    val days = calendarDaysUntil(now, event.targetMillis)
    val month = IslamicCalendar.monthNames[event.event.hijriMonth - 1]
    Surface(
        shape = RoundedCornerShape(21.dp),
        color = colors.surfaceVariant,
        border = BorderStroke(1.dp, colors.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (days == 0L) colors.onBackground else colors.background,
                contentColor = if (days == 0L) colors.background else colors.onBackground,
                border = if (days == 0L) null else BorderStroke(1.dp, colors.outlineVariant),
                modifier = Modifier.size(58.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Text(event.event.hijriDay.toString(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(month.take(3).uppercase(Locale.forLanguageTag("id-ID")), style = MaterialTheme.typography.labelSmall)
                }
            }
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Text(event.event.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${event.event.hijriDay} $month ${event.hijriYear} H", style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                Spacer(Modifier.height(3.dp))
                Text(event.event.description, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.width(8.dp))
            Surface(shape = CircleShape, color = colors.background) {
                Text(
                    countdownLabel(days),
                    Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun FridayScreen(prefs: AppPreferences, onBack: () -> Unit, onOpenKahf: () -> Unit) {
    val context = LocalContext.current
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var completed by rememberSaveable { mutableStateOf(emptyList<String>()) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000)
            now = System.currentTimeMillis()
        }
    }

    val target = IslamicCalendar.nextFriday(prefs.fridayReminderHour, prefs.fridayReminderMinute)
    val remaining = (target - now).coerceAtLeast(0)
    val days = TimeUnit.MILLISECONDS.toDays(remaining)
    val hours = TimeUnit.MILLISECONDS.toHours(remaining) % 24
    val minutes = TimeUnit.MILLISECONDS.toMinutes(remaining) % 60
    val practices = listOf(
        "Membaca Surah Al-Kahf",
        "Memperbanyak selawat",
        "Memperbanyak doa dan istigfar",
        "Mempersiapkan salat Jumat"
    )
    val colors = MaterialTheme.colorScheme

    Surface(Modifier.fillMaxSize(), color = colors.background) {
        Scaffold(
            modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing),
            containerColor = colors.background,
            snackbarHost = { SnackbarHost(snackbar) },
            topBar = {
                FeatureTopBar(
                    title = "Hari Jumat",
                    subtitle = "Al-Kahf · doa · selawat",
                    onBack = onBack
                )
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(top = padding.calculateTopPadding()),
                contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 38.dp)
            ) {
                item(key = "friday-countdown") {
                    FridayCountdownHero(days = days, hours = hours, minutes = minutes)
                    Spacer(Modifier.height(13.dp))
                    Button(
                        onClick = onOpenKahf,
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        shape = CircleShape
                    ) {
                        Icon(painterResource(R.drawable.ic_book_open), null, Modifier.size(19.dp))
                        Spacer(Modifier.width(9.dp))
                        Text("Baca Surah Al-Kahf")
                    }
                    Spacer(Modifier.height(26.dp))
                }

                item(key = "practice-heading") {
                    Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.weight(1f)) {
                            Text("Amalan Jumat", style = MaterialTheme.typography.headlineSmall)
                            Text("Ketuk untuk menandai yang sudah dilakukan", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                        }
                        Text("${completed.size}/${practices.size}", style = MaterialTheme.typography.labelLarge)
                    }
                    Spacer(Modifier.height(11.dp))
                }

                items(practices, key = { it }) { practice ->
                    FridayPracticeCard(
                        text = practice,
                        checked = practice in completed,
                        onClick = {
                            completed = if (practice in completed) completed - practice else completed + practice
                        }
                    )
                    Spacer(Modifier.height(8.dp))
                }

                item(key = "reminders") {
                    Spacer(Modifier.height(18.dp))
                    Text("Pengingat", style = MaterialTheme.typography.headlineSmall)
                    Text("Atur waktu yang paling nyaman", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                    Spacer(Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(23.dp),
                        color = colors.surfaceVariant,
                        border = BorderStroke(1.dp, colors.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            Row(Modifier.padding(start = 16.dp, end = 11.dp, top = 11.dp, bottom = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text("Jumat pagi", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        "Setiap Jumat · %02d:%02d".format(prefs.fridayReminderHour, prefs.fridayReminderMinute),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colors.onSurfaceVariant
                                    )
                                }
                                Switch(checked = prefs.fridayReminderEnabled, onCheckedChange = { enabled ->
                                    prefs.updateFridayReminder(enabled)
                                    if (enabled) IslamicReminderScheduler.scheduleFriday(context, prefs.fridayReminderHour, prefs.fridayReminderMinute)
                                    else IslamicReminderScheduler.cancelFriday(context)
                                })
                            }
                            AnimatedVisibility(prefs.fridayReminderEnabled) {
                                Column {
                                    HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = colors.outlineVariant)
                                    TextButton(
                                        onClick = {
                                            TimePickerDialog(
                                                context,
                                                { _, hour, minute ->
                                                    prefs.updateFridayReminder(true, hour, minute)
                                                    IslamicReminderScheduler.scheduleFriday(context, hour, minute)
                                                },
                                                prefs.fridayReminderHour,
                                                prefs.fridayReminderMinute,
                                                true
                                            ).show()
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(painterResource(R.drawable.ic_schedule), null, Modifier.size(17.dp))
                                        Spacer(Modifier.width(7.dp))
                                        Text("Ubah waktu pengingat")
                                    }
                                }
                            }
                            HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = colors.outlineVariant)
                            Row(Modifier.padding(start = 16.dp, end = 11.dp, top = 11.dp, bottom = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text("Kamis malam", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                    Text("Setiap Kamis · 18:00", style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                                }
                                Switch(prefs.thursdayNightReminder, { enabled ->
                                    prefs.updateFridayReminder(prefs.fridayReminderEnabled, thursdayNight = enabled)
                                    if (enabled) IslamicReminderScheduler.scheduleThursdayNight(context)
                                    else IslamicReminderScheduler.cancelThursdayNight(context)
                                })
                            }
                        }
                    }
                    Spacer(Modifier.height(9.dp))
                    OutlinedButton(
                        onClick = {
                            val sent = NotificationCenter.notifyFriday(context)
                            scope.launch {
                                snackbar.showSnackbar(if (sent) "Notifikasi uji dikirim" else "Izinkan notifikasi Quriva terlebih dahulu")
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = CircleShape
                    ) {
                        Icon(painterResource(R.drawable.ic_notification), null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Kirim notifikasi uji")
                    }
                }
            }
        }
    }
}

@Composable
private fun FridayCountdownHero(days: Long, hours: Long, minutes: Long) {
    val colors = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = colors.inverseSurface,
        contentColor = colors.inverseOnSurface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(21.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("JUMAT BERIKUTNYA", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, letterSpacing = 1.3.sp, color = colors.inverseOnSurface.copy(alpha = .62f))
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                CountdownMetric(days.toString(), "hari", Modifier.weight(1f))
                CountdownMetric(hours.toString().padStart(2, '0'), "jam", Modifier.weight(1f))
                CountdownMetric(minutes.toString().padStart(2, '0'), "menit", Modifier.weight(1f))
            }
            Spacer(Modifier.height(14.dp))
            Text("Siapkan ruang untuk Al-Kahf, selawat, dan doa.", style = MaterialTheme.typography.bodySmall, color = colors.inverseOnSurface.copy(alpha = .63f), textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun CountdownMetric(value: String, label: String, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Surface(modifier = modifier, shape = RoundedCornerShape(18.dp), color = colors.inverseOnSurface.copy(alpha = .1f)) {
        Column(Modifier.padding(vertical = 13.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            Text(label, style = MaterialTheme.typography.labelSmall, color = colors.inverseOnSurface.copy(alpha = .6f))
        }
    }
}

@Composable
private fun FridayPracticeCard(text: String, checked: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = if (checked) colors.inverseSurface else colors.surfaceVariant,
        contentColor = if (checked) colors.inverseOnSurface else colors.onBackground,
        border = if (checked) null else BorderStroke(1.dp, colors.outlineVariant),
        modifier = Modifier.fillMaxWidth().animateContentSize()
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(34.dp).background(
                    if (checked) colors.inverseOnSurface.copy(alpha = .14f) else colors.background,
                    CircleShape
                ),
                contentAlignment = Alignment.Center
            ) {
                if (checked) Icon(painterResource(R.drawable.ic_check), null, Modifier.size(17.dp))
                else Box(Modifier.size(9.dp).background(colors.outline, CircleShape))
            }
            Spacer(Modifier.width(12.dp))
            Text(text, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
            Text(if (checked) "Selesai" else "Tandai", style = MaterialTheme.typography.labelSmall)
        }
    }
}

private fun calendarDaysUntil(now: Long, target: Long): Long {
    val start = Calendar.getInstance().apply {
        timeInMillis = now
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val end = Calendar.getInstance().apply {
        timeInMillis = target
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return ((end.timeInMillis - start.timeInMillis + TimeUnit.HOURS.toMillis(12)) /
        TimeUnit.DAYS.toMillis(1)).coerceAtLeast(0)
}

private fun countdownLabel(days: Long): String = when (days) {
    0L -> "Hari ini"
    1L -> "Besok"
    else -> "$days hari"
}
