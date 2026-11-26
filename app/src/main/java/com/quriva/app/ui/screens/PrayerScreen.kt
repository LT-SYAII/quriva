package com.quriva.app.ui.screens

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.quriva.app.R
import com.quriva.app.data.AppPreferences
import com.quriva.app.data.NotificationAccess
import com.quriva.app.data.NotificationCenter
import com.quriva.app.data.Prayer
import com.quriva.app.data.PrayerCities
import com.quriva.app.data.PrayerCity
import com.quriva.app.data.PrayerMethod
import com.quriva.app.data.PrayerReminderScheduler
import com.quriva.app.data.PrayerTimes
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun PrayerScreen(prefs: AppPreferences, onBack: () -> Unit) {
    val context = LocalContext.current
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1_000)
            now = System.currentTimeMillis()
        }
    }
    val city = remember(prefs.prayerCityId) { PrayerCities.byId(prefs.prayerCityId) }
    val method = remember(prefs.prayerMethod) {
        runCatching { PrayerMethod.valueOf(prefs.prayerMethod) }.getOrDefault(PrayerMethod.KEMENAG)
    }
    val dateKey = remember(now, city.id) {
        SimpleDateFormat("yyyyMMdd", Locale.US).apply {
            timeZone = TimeZone.getTimeZone(city.timeZoneId)
        }.format(Date(now))
    }
    val schedule = remember(city, method, prefs.prayerAsrHanafi, dateKey) {
        PrayerTimes.schedule(city, method, prefs.prayerAsrHanafi, now)
    }
    val next = schedule.next(now)
    val colors = MaterialTheme.colorScheme
    var cityMenuOpen by remember { mutableStateOf(false) }
    var methodMenuOpen by remember { mutableStateOf(false) }
    var notificationAccess by remember {
        mutableStateOf(NotificationCenter.access(context, NotificationCenter.PRAYER_CHANNEL_ID))
    }
    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        notificationAccess = NotificationCenter.access(context, NotificationCenter.PRAYER_CHANNEL_ID)
        if (granted) PrayerReminderScheduler.reschedule(context)
    }
    val alarmManager = remember(context) {
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    }
    val exactAllowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    LifecycleResumeEffect(Unit) {
        notificationAccess = NotificationCenter.access(context, NotificationCenter.PRAYER_CHANNEL_ID)
        onPauseOrDispose { }
    }
    LaunchedEffect(Unit) {
        PrayerReminderScheduler.reschedule(context)
    }

    fun refreshReminders() {
        PrayerReminderScheduler.reschedule(context)
    }

    fun requestNotificationAccess() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            NotificationCenter.openSettings(context, NotificationCenter.PRAYER_CHANNEL_ID)
        }
    }

    Surface(Modifier.fillMaxSize(), color = colors.background) {
        Scaffold(
            modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing),
            containerColor = colors.background,
            topBar = {
                FeatureTopBar(
                    title = "Jadwal Salat",
                    subtitle = "${city.name} · ${method.label}",
                    onBack = onBack
                )
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(top = padding.calculateTopPadding()),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 38.dp)
            ) {
                item(key = "prayer-hero") {
                    NextPrayerHero(
                        city = city,
                        next = next,
                        now = now,
                        remindersEnabled = prefs.prayerRemindersEnabled
                    )
                    Spacer(Modifier.height(14.dp))
                }

                item(key = "prayer-schedule") {
                    DayScheduleCard(
                        times = schedule.times,
                        next = next,
                        zoneId = city.timeZoneId,
                        now = now
                    )
                    Spacer(Modifier.height(14.dp))
                }

                item(key = "prayer-settings") {
                    PrayerSettingsCard(
                        prefs = prefs,
                        city = city,
                        method = method,
                        cityMenuOpen = cityMenuOpen,
                        onCityMenuOpen = { cityMenuOpen = it },
                        methodMenuOpen = methodMenuOpen,
                        onMethodMenuOpen = { methodMenuOpen = it },
                        onCitySelected = {
                            prefs.updatePrayerCity(it)
                            refreshReminders()
                        },
                        onMethodSelected = {
                            prefs.updatePrayerMethod(it)
                            refreshReminders()
                        },
                        onAsrChanged = {
                            prefs.updatePrayerAsrHanafi(it)
                            refreshReminders()
                        }
                    )
                    Spacer(Modifier.height(14.dp))
                }

                item(key = "prayer-qibla") {
                    QiblaCard(city = city)
                    Spacer(Modifier.height(14.dp))
                }

                item(key = "prayer-reminders") {
                    PrayerReminderCard(
                        prefs = prefs,
                        notificationAccess = notificationAccess,
                        exactAllowed = exactAllowed,
                        onToggle = { enabled ->
                            prefs.updatePrayerReminders(enabled)
                            refreshReminders()
                            if (enabled && notificationAccess != NotificationAccess.GRANTED) {
                                requestNotificationAccess()
                            }
                        },
                        onMinutes = { minutes ->
                            prefs.updatePrayerReminders(prefs.prayerRemindersEnabled, minutes)
                            refreshReminders()
                        },
                        onPrayerToggle = { prayer, enabled ->
                            val ids = prefs.prayerEnabledIds.toMutableSet()
                            if (enabled) ids += prayer.id else ids -= prayer.id
                            prefs.updatePrayerReminders(
                                prefs.prayerRemindersEnabled,
                                prefs.prayerReminderMinutes,
                                ids
                            )
                            refreshReminders()
                        },
                        onOpenNotificationSettings = ::requestNotificationAccess,
                        onOpenExactAlarmSettings = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                runCatching {
                                    context.startActivity(
                                        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                            data = Uri.parse("package:${context.packageName}")
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                    )
                                }.onFailure {
                                    NotificationCenter.openSettings(context, NotificationCenter.PRAYER_CHANNEL_ID)
                                }
                            }
                        }
                    )
                    Spacer(Modifier.height(14.dp))
                }

                item(key = "prayer-worship") {
                    WorshipTrackerCard(prefs = prefs)
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Jadwal dihitung offline dari koordinat kota. Selisih satu hingga dua menit dengan jadwal resmi dapat terjadi.",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun NextPrayerHero(
    city: PrayerCity,
    next: com.quriva.app.data.PrayerTime?,
    now: Long,
    remindersEnabled: Boolean
) {
    val colors = MaterialTheme.colorScheme
    val progressTarget = if (next == null) 0f else {
        ((next.millis - now).coerceIn(0L, 6L * 60 * 60 * 1000) / (6f * 60 * 60 * 1000)).coerceIn(0f, 1f)
    }
    val progress by animateFloatAsState(progressTarget, tween(800), label = "nextPrayerProgress")
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = colors.inverseSurface,
        contentColor = colors.inverseOnSurface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(Modifier.fillMaxWidth()) {
            Box(
                Modifier.size(200.dp).align(Alignment.TopEnd).background(
                    Brush.radialGradient(
                        listOf(colors.inverseOnSurface.copy(alpha = .13f), Color.Transparent)
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
                        Icon(painterResource(R.drawable.ic_schedule), null, Modifier.size(19.dp))
                    }
                    Spacer(Modifier.width(11.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            "SALAT BERIKUTNYA",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.3.sp,
                            color = colors.inverseOnSurface.copy(alpha = .6f)
                        )
                        Text(
                            city.name,
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.inverseOnSurface.copy(alpha = .72f)
                        )
                    }
                    if (remindersEnabled) {
                        Surface(shape = CircleShape, color = colors.inverseOnSurface.copy(alpha = .12f)) {
                            Text(
                                "Pengingat aktif",
                                Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
                if (next == null) {
                    Text("Jadwal tidak tersedia", style = MaterialTheme.typography.headlineSmall)
                } else {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            next.prayer.label,
                            style = MaterialTheme.typography.headlineLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            next.label(city.timeZoneId),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        if (next.millis <= now) "Sekarang" else "${PrayerTimes.countdownLabel(next.millis - now)} lagi",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.inverseOnSurface.copy(alpha = .78f)
                    )
                    Spacer(Modifier.height(16.dp))
                    androidx.compose.material3.LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                        color = colors.inverseOnSurface,
                        trackColor = colors.inverseOnSurface.copy(alpha = .15f)
                    )
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    next?.prayer?.arabic.orEmpty(),
                    fontSize = 22.sp,
                    color = colors.inverseOnSurface.copy(alpha = .5f),
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}

@Composable
private fun DayScheduleCard(
    times: List<com.quriva.app.data.PrayerTime>,
    next: com.quriva.app.data.PrayerTime?,
    zoneId: String,
    now: Long
) {
    val colors = MaterialTheme.colorScheme
    val dateLabel = remember(zoneId, now) {
        SimpleDateFormat("EEEE, d MMMM yyyy", Locale.forLanguageTag("id-ID")).apply {
            timeZone = TimeZone.getTimeZone(zoneId)
        }.format(Date(now))
    }
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = colors.surfaceVariant,
        border = BorderStroke(1.dp, colors.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(vertical = 8.dp)) {
            Row(Modifier.padding(horizontal = 17.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Jadwal hari ini", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(dateLabel, style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                }
                Surface(shape = CircleShape, color = colors.background) {
                    Text(
                        "Offline",
                        Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
            times.forEachIndexed { index, time ->
                val isNext = next?.prayer == time.prayer && next.millis == time.millis
                if (index > 0) {
                    HorizontalDivider(
                        Modifier.padding(horizontal = 17.dp),
                        color = colors.outlineVariant.copy(alpha = .7f)
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (isNext) colors.background else Color.Transparent)
                        .padding(horizontal = 17.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier.size(if (isNext) 9.dp else 6.dp).background(
                            if (isNext) colors.onBackground else colors.outline,
                            CircleShape
                        )
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            time.prayer.label,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = if (isNext) FontWeight.Bold else FontWeight.Medium,
                            color = if (time.prayer == Prayer.SUNRISE) colors.onSurfaceVariant else colors.onBackground
                        )
                        Text(
                            if (time.prayer == Prayer.SUNRISE) "Matahari terbit" else time.prayer.arabic,
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.onSurfaceVariant
                        )
                    }
                    if (isNext) {
                        Text(
                            "Berikutnya",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.onSurfaceVariant
                        )
                        Spacer(Modifier.width(10.dp))
                    }
                    Text(
                        time.label(zoneId),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (isNext) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PrayerSettingsCard(
    prefs: AppPreferences,
    city: PrayerCity,
    method: PrayerMethod,
    cityMenuOpen: Boolean,
    onCityMenuOpen: (Boolean) -> Unit,
    methodMenuOpen: Boolean,
    onMethodMenuOpen: (Boolean) -> Unit,
    onCitySelected: (String) -> Unit,
    onMethodSelected: (PrayerMethod) -> Unit,
    onAsrChanged: (Boolean) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = colors.surfaceVariant,
        border = BorderStroke(1.dp, colors.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Lokasi & metode", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                "Pilih kota terdekat agar jadwal lebih akurat.",
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            ExposedDropdownMenuBox(
                expanded = cityMenuOpen,
                onExpandedChange = onCityMenuOpen
            ) {
                OutlinedTextField(
                    value = city.label,
                    onValueChange = {},
                    readOnly = true,
                    singleLine = true,
                    label = { Text("Kota") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(cityMenuOpen) },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = cityMenuOpen,
                    onDismissRequest = { onCityMenuOpen(false) }
                ) {
                    PrayerCities.all.forEach { item ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(item.name)
                                    Text(
                                        item.province,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colors.onSurfaceVariant
                                    )
                                }
                            },
                            onClick = {
                                onCitySelected(item.id)
                                onCityMenuOpen(false)
                            }
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            ExposedDropdownMenuBox(
                expanded = methodMenuOpen,
                onExpandedChange = onMethodMenuOpen
            ) {
                OutlinedTextField(
                    value = method.label,
                    onValueChange = {},
                    readOnly = true,
                    singleLine = true,
                    label = { Text("Metode perhitungan") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(methodMenuOpen) },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = methodMenuOpen,
                    onDismissRequest = { onMethodMenuOpen(false) }
                ) {
                    PrayerMethod.entries.forEach { item ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(item.label)
                                    Text(
                                        if (item.ishaMinutesAfterMaghrib != null) {
                                            "Fajr ${item.fajrAngle.toInt()}° · Isya +${item.ishaMinutesAfterMaghrib} menit"
                                        } else {
                                            "Fajr ${item.fajrAngle.toInt()}° · Isya ${item.ishaAngle.toInt()}°"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colors.onSurfaceVariant
                                    )
                                }
                            },
                            onClick = {
                                onMethodSelected(item)
                                onMethodMenuOpen(false)
                            }
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Ashar mazhab Hanafi", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Bayangan dua kali tinggi benda",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant
                    )
                }
                Switch(checked = prefs.prayerAsrHanafi, onCheckedChange = onAsrChanged)
            }
        }
    }
}

@Composable
private fun QiblaCard(city: PrayerCity) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    val sensorManager = remember(context) {
        context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    }
    val rotationSensor = remember(sensorManager) {
        sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    }
    var heading by remember { mutableFloatStateOf(0f) }
    var sensorReady by remember { mutableStateOf(rotationSensor != null) }
    val qibla = remember(city) { PrayerTimes.qiblaBearing(city.latitude, city.longitude) }

    DisposableEffect(sensorManager, rotationSensor, city.id) {
        if (sensorManager == null || rotationSensor == null) {
            sensorReady = false
            return@DisposableEffect onDispose { }
        }
        val matrix = FloatArray(9)
        val orientation = FloatArray(3)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                SensorManager.getRotationMatrixFromVector(matrix, event.values)
                SensorManager.getOrientation(matrix, orientation)
                val raw = ((Math.toDegrees(orientation[0].toDouble()) + 360.0) % 360.0).toFloat()
                val delta = ((raw - heading + 540f) % 360f) - 180f
                heading = (heading + delta * 0.18f + 360f) % 360f
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        sensorReady = sensorManager.registerListener(
            listener,
            rotationSensor,
            SensorManager.SENSOR_DELAY_UI
        )
        onDispose { sensorManager.unregisterListener(listener) }
    }

    val rounded = qibla.roundToInt()
    val angleToQibla = ((qibla - heading + 540.0) % 360.0) - 180.0
    val aligned = sensorReady && abs(angleToQibla) <= 4.0

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = colors.surfaceVariant,
        border = BorderStroke(1.dp, colors.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Arah Kiblat", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Ka'bah · $rounded° dari utara",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant
                    )
                }
                Surface(shape = CircleShape, color = colors.background) {
                    Text(
                        "${city.name} · ${city.timeZoneId.substringAfterLast('/')}",
                        Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Box(
                modifier = Modifier.fillMaxWidth().height(218.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(
                    Modifier
                        .size(206.dp)
                        .semantics {
                            contentDescription = if (aligned) {
                                "Kompas kiblat. Arah kiblat $rounded derajat dari utara. Ponsel sudah menghadap kiblat."
                            } else {
                                "Kompas kiblat. Arah kiblat $rounded derajat dari utara. Putar ponsel hingga penanda kiblat sejajar dengan jarum tetap di atas."
                            }
                        }
                ) {
                    val radius = size.minDimension / 2f
                    val center = Offset(size.width / 2f, size.height / 2f)
                    drawCircle(
                        color = colors.background,
                        radius = radius,
                        center = center
                    )
                    drawCircle(
                        color = colors.outlineVariant,
                        radius = radius,
                        center = center,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx())
                    )
                    rotate(-heading, center) {
                        for (i in 0 until 24) {
                            val angle = i * 15f
                            val long = i % 6 == 0
                            val startRadius = radius - if (long) 15.dp.toPx() else 8.dp.toPx()
                            val endRadius = radius - 4.dp.toPx()
                            val rad = Math.toRadians(angle.toDouble() - 90.0)
                            drawLine(
                                color = if (long) colors.onBackground else colors.outline,
                                start = Offset(
                                    center.x + (startRadius * Math.cos(rad)).toFloat(),
                                    center.y + (startRadius * Math.sin(rad)).toFloat()
                                ),
                                end = Offset(
                                    center.x + (endRadius * Math.cos(rad)).toFloat(),
                                    center.y + (endRadius * Math.sin(rad)).toFloat()
                                ),
                                strokeWidth = if (long) 2.dp.toPx() else 1.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        }
                        val labelPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                            color = colors.onBackground.toArgb()
                            textSize = 13.sp.toPx()
                            textAlign = android.graphics.Paint.Align.CENTER
                            typeface = android.graphics.Typeface.DEFAULT_BOLD
                        }
                        listOf("U" to 0f, "T" to 90f, "S" to 180f, "B" to 270f).forEach { (label, angle) ->
                            val rad = Math.toRadians(angle.toDouble() - 90.0)
                            val r = radius - 31.dp.toPx()
                            drawContext.canvas.nativeCanvas.drawText(
                                label,
                                center.x + (r * Math.cos(rad)).toFloat(),
                                center.y + (r * Math.sin(rad)).toFloat() + labelPaint.textSize / 3f,
                                labelPaint
                            )
                        }
                        val qiblaRad = Math.toRadians(qibla - 90.0)
                        val markerRadius = radius - 22.dp.toPx()
                        val markerX = center.x + (markerRadius * Math.cos(qiblaRad)).toFloat()
                        val markerY = center.y + (markerRadius * Math.sin(qiblaRad)).toFloat()
                        val markerHalf = if (aligned) 6.dp.toPx() else 4.5.dp.toPx()
                        val marker = Path().apply {
                            moveTo(markerX, markerY - markerHalf)
                            lineTo(markerX + markerHalf, markerY)
                            lineTo(markerX, markerY + markerHalf)
                            lineTo(markerX - markerHalf, markerY)
                            close()
                        }
                        drawPath(
                            marker,
                            color = colors.onBackground
                        )
                        val rimInset = 4.dp.toPx()
                        drawArc(
                            color = colors.onBackground,
                            startAngle = qibla.toFloat() - 95f,
                            sweepAngle = 10f,
                            useCenter = false,
                            topLeft = Offset(
                                center.x - radius + rimInset,
                                center.y - radius + rimInset
                            ),
                            size = Size((radius - rimInset) * 2f, (radius - rimInset) * 2f),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(
                                width = if (aligned) 3.5.dp.toPx() else 2.5.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        )
                    }
                    // The needle stays fixed at the top; only the dial and the
                    // qibla marker rotate with the phone.
                    val needleLength = radius * 0.56f
                    val needleTail = radius * 0.14f
                    val needleHalfWidth = radius * 0.048f
                    val fixedNeedle = Path().apply {
                        moveTo(center.x, center.y - needleLength)
                        lineTo(center.x - needleHalfWidth, center.y)
                        lineTo(center.x, center.y + needleTail)
                        lineTo(center.x + needleHalfWidth, center.y)
                        close()
                    }
                    drawPath(fixedNeedle, color = colors.onSurfaceVariant)
                    drawCircle(color = colors.onBackground, radius = 5.dp.toPx(), center = center)
                    drawCircle(color = colors.background, radius = 2.5.dp.toPx(), center = center)
                }
            }
            Text(
                when {
                    !sensorReady ->
                        "Sensor arah tidak tersedia. Gunakan angka $rounded° dari utara sebagai panduan arah kiblat."
                    aligned ->
                        "Tepat menghadap kiblat. Pertahankan posisi ponsel."
                    else ->
                        "Letakkan ponsel mendatar. Putar ponsel hingga penanda kiblat di tepi kompas sejajar dengan jarum tetap di atas."
                },
                style = MaterialTheme.typography.labelSmall,
                color = if (aligned) colors.onBackground else colors.onSurfaceVariant,
                fontWeight = if (aligned) FontWeight.SemiBold else FontWeight.Normal,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun PrayerReminderCard(
    prefs: AppPreferences,
    notificationAccess: NotificationAccess,
    exactAllowed: Boolean,
    onToggle: (Boolean) -> Unit,
    onMinutes: (Int) -> Unit,
    onPrayerToggle: (Prayer, Boolean) -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onOpenExactAlarmSettings: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = colors.surfaceVariant,
        border = BorderStroke(1.dp, colors.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp).animateContentSize()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Pengingat salat", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Notifikasi offline sesuai jadwal kota",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant
                    )
                }
                Switch(checked = prefs.prayerRemindersEnabled, onCheckedChange = onToggle)
            }
            AnimatedVisibility(prefs.prayerRemindersEnabled) {
                Column {
                    Spacer(Modifier.height(14.dp))
                    Text("Ingatkan sebelum", style = MaterialTheme.typography.labelMedium)
                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        listOf(0, 5, 10, 15, 20).forEach { minutes ->
                            FilterChip(
                                selected = prefs.prayerReminderMinutes == minutes,
                                onClick = { onMinutes(minutes) },
                                label = { Text(if (minutes == 0) "Tepat waktu" else "$minutes menit") }
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text("Salat yang diingatkan", style = MaterialTheme.typography.labelMedium)
                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        listOf(Prayer.FAJR, Prayer.DHUHR, Prayer.ASR, Prayer.MAGHRIB, Prayer.ISHA).forEach { prayer ->
                            FilterChip(
                                selected = prayer.id in prefs.prayerEnabledIds,
                                onClick = { onPrayerToggle(prayer, prayer.id !in prefs.prayerEnabledIds) },
                                label = { Text(prayer.label) }
                            )
                        }
                    }
                    if (notificationAccess != NotificationAccess.GRANTED) {
                        Spacer(Modifier.height(12.dp))
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = colors.background,
                            border = BorderStroke(1.dp, colors.outlineVariant)
                        ) {
                            Row(Modifier.padding(start = 13.dp, end = 6.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text("Izin notifikasi diperlukan", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        when (notificationAccess) {
                                            NotificationAccess.PERMISSION_MISSING -> "Izinkan Quriva mengirim pengingat salat."
                                            NotificationAccess.APP_BLOCKED -> "Notifikasi aplikasi sedang diblokir."
                                            NotificationAccess.CHANNEL_BLOCKED -> "Kanal adzan salat sedang dimatikan."
                                            NotificationAccess.GRANTED -> ""
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colors.onSurfaceVariant
                                    )
                                }
                                TextButton(onClick = onOpenNotificationSettings) { Text("Buka") }
                            }
                        }
                    }
                    if (!exactAllowed) {
                        Spacer(Modifier.height(9.dp))
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = colors.background,
                            border = BorderStroke(1.dp, colors.outlineVariant)
                        ) {
                            Row(Modifier.padding(start = 13.dp, end = 6.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text("Alarm presisi belum aktif", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        "Pengingat tetap berjalan, tetapi bisa terlambat beberapa menit.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colors.onSurfaceVariant
                                    )
                                }
                                TextButton(onClick = onOpenExactAlarmSettings) { Text("Aktifkan") }
                            }
                        }
                    }
                }
            }
        }
    }
}
