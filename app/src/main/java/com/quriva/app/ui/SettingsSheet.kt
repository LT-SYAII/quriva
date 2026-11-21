package com.quriva.app.ui

import android.Manifest
import android.app.TimePickerDialog
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.quriva.app.R
import com.quriva.app.data.AppPreferences
import com.quriva.app.data.AppUpdateInfo
import com.quriva.app.data.AppUpdateWorker
import com.quriva.app.data.NotificationAccess
import com.quriva.app.data.NotificationCenter
import com.quriva.app.data.ReminderScheduler
import com.quriva.app.data.Translations
import com.quriva.app.data.UpdateCheckResult
import com.quriva.app.data.UpdateRepository
import com.quriva.app.ui.theme.ArabicFontFamily
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

private const val PREVIEW_BISMILLAH = "بِسْمِ ٱللَّهِ ٱلرَّحْمَـٰنِ ٱلرَّحِيمِ"

private enum class NotificationPermissionAction { ENABLE_REMINDER, SEND_TEST }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    prefs: AppPreferences,
    onUpdateFound: (AppUpdateInfo) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var checkingUpdate by remember { mutableStateOf(false) }
    var updateStatus by remember { mutableStateOf<String?>(null) }
    var notificationStatus by remember { mutableStateOf<String?>(null) }
    var notificationAccess by remember { mutableStateOf(NotificationCenter.access(context)) }
    var updateNotificationAccess by remember {
        mutableStateOf(NotificationCenter.access(context, NotificationCenter.UPDATE_CHANNEL_ID))
    }
    var permissionAction by remember { mutableStateOf<NotificationPermissionAction?>(null) }
    val currentVersion = remember { UpdateRepository.currentVersionName(context) }
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(Unit) {
        NotificationCenter.createChannels(context)
        notificationAccess = NotificationCenter.access(context)
        updateNotificationAccess = NotificationCenter.access(
            context,
            NotificationCenter.UPDATE_CHANNEL_ID
        )
    }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                notificationAccess = NotificationCenter.access(context)
                updateNotificationAccess = NotificationCenter.access(
                    context,
                    NotificationCenter.UPDATE_CHANNEL_ID
                )
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val notifPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        val action = permissionAction
        permissionAction = null
        notificationAccess = NotificationCenter.access(context)
        updateNotificationAccess = NotificationCenter.access(
            context,
            NotificationCenter.UPDATE_CHANNEL_ID
        )
        if (granted) {
            AppUpdateWorker.runNow(context)
            when (action) {
                NotificationPermissionAction.ENABLE_REMINDER -> {
                    prefs.updateReminder(true)
                    ReminderScheduler.schedule(context, prefs.reminderHour, prefs.reminderMinute)
                    notificationStatus = "Pengingat harian berhasil diaktifkan."
                }
                NotificationPermissionAction.SEND_TEST -> {
                    notificationStatus = if (NotificationCenter.notifyDaily(context, test = true)) {
                        "Notifikasi uji dikirim. Jika tidak terlihat, periksa mode Jangan Ganggu atau pembatasan baterai."
                    } else {
                        "Notifikasi masih diblokir oleh sistem."
                    }
                }
                null -> Unit
            }
        } else {
            if (action == NotificationPermissionAction.ENABLE_REMINDER) {
                prefs.updateReminder(false)
                ReminderScheduler.cancel(context)
            }
            notificationStatus = "Izin notifikasi ditolak. Buka pengaturan Android untuk mengaktifkannya."
        }
    }

    fun toggleReminder() {
        if (prefs.reminderEnabled) {
            prefs.updateReminder(false)
            ReminderScheduler.cancel(context)
            notificationStatus = "Pengingat harian dinonaktifkan."
        } else {
            notificationAccess = NotificationCenter.access(context)
            when (notificationAccess) {
                NotificationAccess.GRANTED -> {
                    prefs.updateReminder(true)
                    ReminderScheduler.schedule(context, prefs.reminderHour, prefs.reminderMinute)
                    notificationStatus = "Pengingat harian berhasil diaktifkan."
                }
                NotificationAccess.PERMISSION_MISSING -> {
                    permissionAction = NotificationPermissionAction.ENABLE_REMINDER
                    prefs.markNotificationPermissionAsked()
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
                NotificationAccess.APP_BLOCKED,
                NotificationAccess.CHANNEL_BLOCKED -> {
                    notificationStatus = "Notifikasi diblokir. Aktifkan kembali melalui pengaturan Android."
                    NotificationCenter.openSettings(context)
                }
            }
        }
    }

    fun sendTestNotification() {
        notificationAccess = NotificationCenter.access(context)
        when (notificationAccess) {
            NotificationAccess.GRANTED -> {
                notificationStatus = if (NotificationCenter.notifyDaily(context, test = true)) {
                    "Notifikasi uji dikirim. Jika tidak terlihat, periksa mode Jangan Ganggu atau pembatasan baterai."
                } else {
                    "Notifikasi uji gagal dikirim."
                }
            }
            NotificationAccess.PERMISSION_MISSING -> {
                permissionAction = NotificationPermissionAction.SEND_TEST
                prefs.markNotificationPermissionAsked()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
            NotificationAccess.APP_BLOCKED,
            NotificationAccess.CHANNEL_BLOCKED -> {
                notificationStatus = "Notifikasi diblokir. Periksa pengaturan Android."
                NotificationCenter.openSettings(context)
            }
        }
    }

    fun pickReminderTime() {
        TimePickerDialog(
            context,
            { _, hour, minute ->
                prefs.updateReminder(prefs.reminderEnabled, hour, minute)
                if (prefs.reminderEnabled) {
                    ReminderScheduler.schedule(context, hour, minute)
                }
            },
            prefs.reminderHour,
            prefs.reminderMinute,
            true
        ).show()
    }

    fun checkUpdateNow() {
        if (checkingUpdate) return
        checkingUpdate = true
        updateStatus = null
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                UpdateRepository.checkForUpdate(context, force = true)
            }
            prefs.markUpdateChecked()
            checkingUpdate = false
            when (result) {
                is UpdateCheckResult.UpToDate -> {
                    updateStatus = "Kamu sudah menggunakan versi terbaru (v$currentVersion)."
                }
                is UpdateCheckResult.Available -> {
                    updateStatus = "Pembaruan ${result.info.tagName} tersedia."
                    onUpdateFound(result.info)
                }
                is UpdateCheckResult.Error -> {
                    updateStatus = result.message
                }
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = { BottomSheetDefaults.DragHandle(color = colors.outline) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 28.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Pengaturan",
                    style = MaterialTheme.typography.headlineSmall,
                    color = colors.onBackground,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        painter = painterResource(R.drawable.ic_close),
                        contentDescription = "Tutup",
                        tint = colors.onBackground
                    )
                }
            }

            Spacer(Modifier.height(22.dp))
            Text(
                text = "Tema",
                style = MaterialTheme.typography.labelLarge,
                color = colors.onBackground
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemeOption(label = "Sistem", selected = prefs.themeMode == "system", onClick = { prefs.updateThemeMode("system") })
                ThemeOption(label = "Terang", selected = prefs.themeMode == "light", onClick = { prefs.updateThemeMode("light") })
                ThemeOption(label = "Gelap", selected = prefs.themeMode == "dark", onClick = { prefs.updateThemeMode("dark") })
            }

            Spacer(Modifier.height(28.dp))
            SettingLabel(title = "Ukuran teks Arab", value = "${prefs.arabicSize.roundToInt()} sp")
            Slider(value = prefs.arabicSize, onValueChange = { prefs.updateArabicSize(it) }, valueRange = 20f..44f, colors = sliderColors())

            Spacer(Modifier.height(14.dp))
            SettingLabel(title = "Ukuran terjemahan", value = "${prefs.translationSize.roundToInt()} sp")
            Slider(value = prefs.translationSize, onValueChange = { prefs.updateTranslationSize(it) }, valueRange = 12f..24f, colors = sliderColors())

            Spacer(Modifier.height(20.dp))
            Text("Terjemahan", style = MaterialTheme.typography.labelLarge, color = colors.onBackground)
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Translations.all.forEach { option ->
                    FilterChip(
                        selected = prefs.translationId == option.id,
                        onClick = { prefs.updateTranslation(option.id) },
                        label = { Text(option.name) }
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                if (prefs.translationId == 33) "Terjemahan Kemenag RI tersedia offline."
                else "Terjemahan tambahan diambil online saat membaca. Kemenag RI tetap menjadi cadangan offline.",
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant
            )
            Spacer(Modifier.height(10.dp))
            SettingToggle("Tampilkan terjemahan", "Sembunyikan untuk mode fokus teks Arab", prefs.showTranslation) { prefs.updateShowTranslation(it) }
            Spacer(Modifier.height(10.dp))
            SettingToggle("Tajwid berwarna", "Warna hukum tajwid pada teks Arab", prefs.showTajweed) { prefs.updateShowTajweed(it) }
            Spacer(Modifier.height(10.dp))
            SettingToggle("Kata per kata", "Terjemahan tiap kata di bawah ayat", prefs.showWordByWord) { prefs.updateShowWordByWord(it) }
            Spacer(Modifier.height(10.dp))
            SettingToggle("Transliterasi Latin", "Tampilkan transliterasi per kata", prefs.showTransliteration) { prefs.updateShowTransliteration(it) }
            Spacer(Modifier.height(10.dp))
            SettingToggle("Audio per ayat", "Putar per ayat untuk sorot & lompat ayat", prefs.ayahAudioMode) { prefs.updateAyahAudioMode(it) }
            Spacer(Modifier.height(10.dp))
            SettingToggle("Ikuti ayat diputar", "Gulir otomatis ke ayat yang sedang dibaca", prefs.autoScrollAudio) { prefs.updateAutoScrollAudio(it) }

            Spacer(Modifier.height(20.dp))
            Text("Pengingat harian", style = MaterialTheme.typography.labelLarge, color = colors.onBackground)
            Spacer(Modifier.height(6.dp))
            SettingToggle(
                "Notifikasi baca harian",
                if (prefs.reminderEnabled) "Dijadwalkan setiap hari sekitar pukul %02d:%02d".format(prefs.reminderHour, prefs.reminderMinute)
                else "Kirim pengingat harian untuk membaca Al-Qur'an",
                prefs.reminderEnabled
            ) { toggleReminder() }
            Spacer(Modifier.height(8.dp))
            Surface(
                onClick = { pickReminderTime() },
                shape = RoundedCornerShape(14.dp),
                color = colors.surfaceVariant,
                border = BorderStroke(1.dp, colors.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_schedule),
                        contentDescription = null,
                        tint = colors.onBackground,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Waktu pengingat", style = MaterialTheme.typography.labelLarge, color = colors.onBackground)
                        Text("Ketuk untuk mengubah jam", style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                    }
                    Text(
                        "%02d:%02d".format(prefs.reminderHour, prefs.reminderMinute),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.onBackground
                    )
                }
            }
            if (prefs.reminderEnabled) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "Jadwal disimpan oleh sistem dan dipulihkan otomatis setelah perangkat dinyalakan ulang.",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant
                )
            }
            if (notificationAccess != NotificationAccess.GRANTED) {
                Spacer(Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = colors.surfaceVariant,
                    border = BorderStroke(1.dp, colors.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            when (notificationAccess) {
                                NotificationAccess.PERMISSION_MISSING -> "Izin notifikasi belum diberikan."
                                NotificationAccess.APP_BLOCKED -> "Semua notifikasi Quriva diblokir oleh Android."
                                NotificationAccess.CHANNEL_BLOCKED -> "Channel pengingat Quriva dinonaktifkan."
                                NotificationAccess.GRANTED -> ""
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onBackground
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { NotificationCenter.openSettings(context) },
                            shape = CircleShape
                        ) { Text("Buka pengaturan notifikasi") }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { sendTestNotification() },
                modifier = Modifier.fillMaxWidth(),
                shape = CircleShape,
                border = BorderStroke(1.dp, colors.outline)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_stat_quriva),
                    contentDescription = null,
                    tint = colors.onBackground,
                    modifier = Modifier.size(17.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("Kirim notifikasi uji", color = colors.onBackground)
            }
            AnimatedVisibility(visible = !notificationStatus.isNullOrBlank()) {
                Text(
                    notificationStatus.orEmpty(),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Spacer(Modifier.height(22.dp))
            PreviewCard(prefs)

            Spacer(Modifier.height(24.dp))
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = colors.surfaceVariant,
                border = BorderStroke(1.dp, colors.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp).animateContentSize()) {
                    Text("TENTANG APLIKASI", style = MaterialTheme.typography.labelSmall, letterSpacing = 1.4.sp, color = colors.onSurfaceVariant)
                    Spacer(Modifier.height(10.dp))
                    AboutRow("Nama aplikasi", "Quriva")
                    Spacer(Modifier.height(6.dp))
                    AboutRow("Versi", currentVersion)
                    Spacer(Modifier.height(6.dp))
                    AboutRow("Pembuat", "Bang syaii")
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Aplikasi Al-Qur'an sederhana untuk membaca, mendengar, dan menjaga kebiasaan tilawah harian.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant
                    )
                    Spacer(Modifier.height(14.dp))
                    OutlinedButton(
                        onClick = { checkUpdateNow() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = CircleShape,
                        border = BorderStroke(1.dp, colors.outline),
                        enabled = !checkingUpdate
                    ) {
                        if (checkingUpdate) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = colors.onBackground)
                            Spacer(Modifier.width(8.dp))
                            Text("Memeriksa…", color = colors.onBackground)
                        } else {
                            Icon(painter = painterResource(R.drawable.ic_arrow_forward), contentDescription = null, modifier = Modifier.size(16.dp), tint = colors.onBackground)
                            Spacer(Modifier.width(8.dp))
                            Text("Periksa pembaruan", color = colors.onBackground)
                        }
                    }
                    AnimatedVisibility(visible = !updateStatus.isNullOrBlank()) {
                        Text(
                            text = updateStatus.orEmpty(),
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.onSurfaceVariant,
                            modifier = Modifier.padding(top = 10.dp)
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Pembaruan diperiksa otomatis saat aplikasi dibuka. APK terbaru dapat diunduh dan dipasang langsung dari aplikasi, tanpa perlu membuka GitHub.",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                    if (updateNotificationAccess != NotificationAccess.GRANTED) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Notifikasi pembaruan sedang diblokir oleh Android.",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.onBackground
                        )
                        TextButton(
                            onClick = {
                                NotificationCenter.openSettings(
                                    context,
                                    NotificationCenter.UPDATE_CHANNEL_ID
                                )
                            }
                        ) {
                            Text("Periksa channel pembaruan", color = colors.onBackground)
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = colors.onBackground, contentColor = colors.background)
            ) {
                Text("Selesai", modifier = Modifier.padding(vertical = 4.dp))
            }
        }
    }

}

@Composable
private fun AboutRow(label: String, value: String) {
    val colors = MaterialTheme.colorScheme
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = colors.onBackground)
    }
}

@Composable
private fun SettingToggle(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.labelLarge, color = colors.onBackground)
            Spacer(Modifier.height(2.dp))
            Text(text = subtitle, style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
        }
        Switch(
            checked = checked,
            onCheckedChange = { onCheckedChange(it) },
            colors = SwitchDefaults.colors(
                checkedThumbColor = colors.background,
                checkedTrackColor = colors.onBackground,
                checkedBorderColor = colors.onBackground,
                uncheckedThumbColor = colors.onSurfaceVariant,
                uncheckedTrackColor = colors.surfaceVariant,
                uncheckedBorderColor = colors.outline
            )
        )
    }
}

@Composable
private fun ThemeOption(label: String, selected: Boolean, onClick: () -> Unit) {
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
private fun SettingLabel(title: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(text = title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.weight(1f))
        Text(text = value, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun sliderColors() = SliderDefaults.colors(
    thumbColor = MaterialTheme.colorScheme.onBackground,
    activeTrackColor = MaterialTheme.colorScheme.onBackground,
    inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant
)

@Composable
private fun PreviewCard(prefs: AppPreferences) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = colors.surfaceVariant,
        border = BorderStroke(1.dp, colors.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Text(
                    text = PREVIEW_BISMILLAH,
                    fontFamily = ArabicFontFamily,
                    fontSize = prefs.arabicSize.sp,
                    lineHeight = (prefs.arabicSize * 1.9f).sp,
                    color = colors.onBackground,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (prefs.showTranslation) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "Dengan nama Allah Yang Maha Pengasih, Maha Penyayang.",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = prefs.translationSize.sp, lineHeight = (prefs.translationSize * 1.6f).sp),
                    color = colors.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
