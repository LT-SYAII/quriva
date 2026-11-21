package com.quriva.app.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Downloads a release APK into the app cache and hands it to the system
 * package installer through a FileProvider. This keeps the update flow inside
 * Quriva instead of sending users to a browser.
 */
object UpdateDownloader {

    private const val MIN_APK_BYTES = 1_000_000L

    enum class InstallResult {
        STARTED,
        PERMISSION_REQUIRED,
        INVALID_APK,
        FAILED
    }

    suspend fun download(
        context: Context,
        url: String,
        expectedSize: Long,
        onProgress: (Float) -> Unit
    ): File = withContext(Dispatchers.IO) {
        if (!url.startsWith("https://")) {
            throw IOException("Alamat pembaruan tidak aman.")
        }
        val dir = File(context.cacheDir, "updates").apply { mkdirs() }
        val finalFile = File(dir, "quriva-update.apk")
        val tempFile = File(dir, "quriva-update.apk.part")

        if (finalFile.exists() && finalFile.length() >= MIN_APK_BYTES &&
            (expectedSize <= 0 || finalFile.length() == expectedSize)
        ) {
            withContext(Dispatchers.Main.immediate) { onProgress(1f) }
            return@withContext finalFile
        }

        tempFile.delete()
        if (finalFile.exists()) finalFile.delete()

        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 20_000
            readTimeout = 60_000
            instanceFollowRedirects = true
            setRequestProperty("Accept", "application/octet-stream")
            setRequestProperty("User-Agent", "Quriva/1.5.1 (Android)")
        }

        try {
            val code = connection.responseCode
            if (code !in 200..299) {
                throw IOException("Server mengembalikan kode $code saat mengunduh pembaruan.")
            }
            val contentLength = connection.contentLengthLong.takeIf { it > 0 }
            val total = contentLength ?: expectedSize.takeIf { it > 0 } ?: 0L
            var downloaded = 0L
            var lastPercent = -1
            connection.inputStream.use { input ->
                FileOutputStream(tempFile).use { output ->
                    val buffer = ByteArray(16 * 1024)
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        downloaded += read
                        if (total > 0) {
                            val percent = ((downloaded * 100) / total).toInt()
                            if (percent != lastPercent) {
                                lastPercent = percent
                                val fraction = (downloaded.toFloat() / total.toFloat()).coerceIn(0f, 1f)
                                withContext(Dispatchers.Main.immediate) { onProgress(fraction) }
                            }
                        }
                    }
                }
            }
            if (tempFile.length() < MIN_APK_BYTES) {
                throw IOException("Berkas pembaruan tidak lengkap. Coba unduh lagi.")
            }
            if (expectedSize > 0 && tempFile.length() != expectedSize) {
                throw IOException("Ukuran berkas pembaruan tidak sesuai. Coba lagi.")
            }
            if (!tempFile.renameTo(finalFile)) {
                throw IOException("Berkas pembaruan tidak dapat disimpan dengan aman.")
            }
            withContext(Dispatchers.Main.immediate) { onProgress(1f) }
            finalFile
        } finally {
            connection.disconnect()
            if (tempFile.exists()) tempFile.delete()
        }
    }

    fun install(context: Context, file: File, expectedVersion: String): InstallResult {
        if (!file.exists() || file.length() < MIN_APK_BYTES) return InstallResult.INVALID_APK
        val archive = try {
            if (Build.VERSION.SDK_INT >= 33) {
                context.packageManager.getPackageArchiveInfo(
                    file.absolutePath,
                    android.content.pm.PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageArchiveInfo(file.absolutePath, 0)
            }
        } catch (_: Throwable) {
            null
        }
        if (archive?.packageName != context.packageName ||
            UpdateRepository.normalizeVersion(archive.versionName) !=
            UpdateRepository.normalizeVersion(expectedVersion)
        ) {
            file.delete()
            return InstallResult.INVALID_APK
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !context.packageManager.canRequestPackageInstalls()
        ) {
            try {
                context.startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        Uri.parse("package:${context.packageName}")
                    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            } catch (_: Throwable) { }
            return InstallResult.PERMISSION_REQUIRED
        }
        return try {
            val uri = FileProvider.getUriForFile(
                context,
                context.packageName + ".fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            InstallResult.STARTED
        } catch (_: Throwable) {
            InstallResult.FAILED
        }
    }
}
