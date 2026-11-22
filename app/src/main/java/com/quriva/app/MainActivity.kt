package com.quriva.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.runtime.mutableIntStateOf
import com.quriva.app.data.AppPreferences
import com.quriva.app.data.AppUpdateWorker
import com.quriva.app.data.NotificationCenter
import com.quriva.app.data.ReminderScheduler
import com.quriva.app.data.IslamicReminderScheduler
import com.quriva.app.ui.QurivaApp

class MainActivity : ComponentActivity() {
    private val updateRequestKey = mutableIntStateOf(0)
    private var firstStart = true
    private var stoppedAtElapsed = 0L
    private lateinit var preferences: AppPreferences
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) AppUpdateWorker.runNow(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        if (intent.getBooleanExtra(EXTRA_CHECK_UPDATE, false)) {
            updateRequestKey.intValue++
            intent.removeExtra(EXTRA_CHECK_UPDATE)
        }
        preferences = AppPreferences(applicationContext)
        NotificationCenter.createChannels(this)
        AppUpdateWorker.schedule(this)
        if (preferences.reminderEnabled) {
            ReminderScheduler.ensureScheduled(
                this,
                preferences.reminderHour,
                preferences.reminderMinute
            )
        }
        if (preferences.fridayReminderEnabled) IslamicReminderScheduler.scheduleFriday(this, preferences.fridayReminderHour, preferences.fridayReminderMinute)
        if (preferences.thursdayNightReminder) IslamicReminderScheduler.scheduleThursdayNight(this)
        if (preferences.islamicEventNotifications) {
            IslamicReminderScheduler.scheduleEvents(this)
            IslamicReminderScheduler.checkEventsNow(this)
        }
        enableEdgeToEdge()
        setContent {
            QurivaApp(preferences, updateRequestKey.intValue)
        }
    }

    override fun onStart() {
        super.onStart()
        AppUpdateWorker.runNow(this)
        requestNotificationPermissionOnce()
        if (firstStart) {
            firstStart = false
        } else if (stoppedAtElapsed > 0L &&
            SystemClock.elapsedRealtime() - stoppedAtElapsed >= UPDATE_REFRESH_AFTER_BACKGROUND_MS
        ) {
            updateRequestKey.intValue++
        }
    }

    override fun onStop() {
        stoppedAtElapsed = SystemClock.elapsedRealtime()
        super.onStop()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra(EXTRA_CHECK_UPDATE, false)) {
            updateRequestKey.intValue++
            intent.removeExtra(EXTRA_CHECK_UPDATE)
        }
    }

    private fun requestNotificationPermissionOnce() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !preferences.notificationPermissionAsked &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            preferences.markNotificationPermissionAsked()
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    companion object {
        const val EXTRA_CHECK_UPDATE = "check_update"
        private const val UPDATE_REFRESH_AFTER_BACKGROUND_MS = 5 * 60 * 1000L
    }
}
