package com.quriva.app.data

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.TimeUnit

/** Periodically announces a newer GitHub release once per tag. */
class AppUpdateWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = checkMutex.withLock {
        NotificationCenter.createChannels(applicationContext)
        if (NotificationCenter.access(
                applicationContext,
                NotificationCenter.UPDATE_CHANNEL_ID
            ) != NotificationAccess.GRANTED
        ) {
            return@withLock Result.success()
        }
        val prefs = AppPreferences(applicationContext)
        val result = UpdateRepository.checkForUpdate(applicationContext)
        if (result !is UpdateCheckResult.Available) return@withLock Result.success()
        val info = result.info
        if (info.tagName == prefs.dismissedUpdateTag || info.tagName == prefs.updateNotifiedTag) {
            return@withLock Result.success()
        }
        if (runCatching {
                NotificationCenter.notifyUpdate(applicationContext, info.tagName, info.name)
            }.getOrDefault(false)
        ) {
            prefs.markUpdateNotified(info.tagName)
        }
        Result.success()
    }

    companion object {
        private const val PERIODIC_WORK = "quriva_update_check"
        private const val IMMEDIATE_WORK = "quriva_update_check_now"
        private val checkMutex = Mutex()

        private fun connectedConstraints() = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<AppUpdateWorker>(6, TimeUnit.HOURS)
                .setConstraints(connectedConstraints())
                .build()
            WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
                PERIODIC_WORK,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun runNow(context: Context) {
            val request = OneTimeWorkRequestBuilder<AppUpdateWorker>()
                .setConstraints(connectedConstraints())
                .build()
            WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
                IMMEDIATE_WORK,
                ExistingWorkPolicy.KEEP,
                request
            )
        }
    }
}
