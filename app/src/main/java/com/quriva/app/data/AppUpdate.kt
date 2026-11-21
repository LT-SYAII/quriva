package com.quriva.app.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.compose.runtime.Immutable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

@Immutable
data class AppUpdateInfo(
    val tagName: String,
    val versionName: String,
    val name: String,
    val body: String,
    val htmlUrl: String,
    val apkDownloadUrl: String?,
    val apkSize: Long,
    val publishedAt: String
)

sealed interface UpdateCheckResult {
    data object UpToDate : UpdateCheckResult
    data class Available(val info: AppUpdateInfo) : UpdateCheckResult
    data class Error(val message: String) : UpdateCheckResult
}

object UpdateRepository {
    private const val LATEST_API = "https://api.github.com/repos/LT-SYAII/quriva/releases/latest"
    private const val MEMORY_CACHE_MS = 60_000L
    private val checkMutex = Mutex()
    private var cachedAt = 0L
    private var cachedResult: UpdateCheckResult? = null

    suspend fun checkForUpdate(
        context: Context,
        force: Boolean = false
    ): UpdateCheckResult = checkMutex.withLock {
        val now = System.currentTimeMillis()
        cachedResult?.takeIf { !force && now - cachedAt < MEMORY_CACHE_MS }?.let {
            return@withLock it
        }
        val result = withContext(Dispatchers.IO) {
            try {
                val json = getJson(LATEST_API)
                val tag = json.optString("tag_name").trim()
                val latestVersion = normalizeVersion(tag.ifBlank { json.optString("name") })
                val currentVersion = normalizeVersion(currentVersionName(context))
                if (latestVersion.isBlank() || !isNewer(latestVersion, currentVersion)) {
                    return@withContext UpdateCheckResult.UpToDate
                }
                val assets = json.optJSONArray("assets")
                var apkUrl: String? = null
                var apkSize = 0L
                if (assets != null) {
                    for (i in 0 until assets.length()) {
                        val a = assets.optJSONObject(i) ?: continue
                        val name = a.optString("name")
                        val dl = a.optString("browser_download_url")
                        if (name.endsWith(".apk", ignoreCase = true) && dl.startsWith("https://")) {
                            apkUrl = dl
                            apkSize = a.optLong("size", 0L)
                            break
                        }
                    }
                }
                val info = AppUpdateInfo(
                    tagName = tag.ifBlank { "v$latestVersion" },
                    versionName = latestVersion,
                    name = json.optString("name").ifBlank { "Quriva $latestVersion" },
                    body = json.optString("body"),
                    htmlUrl = json.optString("html_url").ifBlank { "https://github.com/LT-SYAII/quriva/releases/latest" },
                    apkDownloadUrl = apkUrl,
                    apkSize = apkSize,
                    publishedAt = json.optString("published_at")
                )
                UpdateCheckResult.Available(info)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                UpdateCheckResult.Error(friendlyError(e))
            }
        }
        cachedAt = now
        cachedResult = result
        result
    }

    fun currentVersionName(context: Context): String {
        return try {
            val pm = context.packageManager
            val pkg = context.packageName
            val pInfo = if (Build.VERSION.SDK_INT >= 33) {
                pm.getPackageInfo(pkg, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(pkg, 0)
            }
            pInfo.versionName ?: "1.5.2"
        } catch (_: Throwable) {
            "1.5.2"
        }
    }

    fun normalizeVersion(raw: String?): String =
        raw.orEmpty().trim().removePrefix("v").removePrefix("V").trim()

    fun isNewer(latest: String, current: String): Boolean {
        val l = parseParts(latest)
        val c = parseParts(current)
        val n = maxOf(l.size, c.size)
        for (i in 0 until n) {
            val lv = l.getOrElse(i) { 0 }
            val cv = c.getOrElse(i) { 0 }
            if (lv != cv) return lv > cv
        }
        return false
    }

    private fun parseParts(v: String): List<Int> =
        normalizeVersion(v).split('.', '-', '_')
            .mapNotNull { part -> part.takeWhile { it.isDigit() }.toIntOrNull() }

    fun friendlyError(e: Throwable): String = when (e) {
        is java.net.UnknownHostException -> "Tidak ada koneksi internet. Coba lagi saat online."
        is java.net.SocketTimeoutException -> "Koneksi terlalu lama. Coba lagi."
        else -> e.message?.takeIf { it.isNotBlank() } ?: "Gagal memeriksa pembaruan."
    }

    fun openUrl(context: Context, url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Throwable) { }
    }

    private fun getJson(path: String): JSONObject {
        val connection = (URL(path).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15_000
            readTimeout = 20_000
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "Quriva-Android")
        }
        try {
            val code = connection.responseCode
            if (code == 403 || code == 429) {
                throw java.io.IOException("Batas pemeriksaan pembaruan tercapai. Coba lagi beberapa saat lagi.")
            }
            if (code !in 200..299) {
                throw java.io.IOException("Server mengembalikan kode $code.")
            }
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            return JSONObject(body)
        } finally {
            connection.disconnect()
        }
    }
}
