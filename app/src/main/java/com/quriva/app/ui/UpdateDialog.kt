package com.quriva.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.quriva.app.R
import com.quriva.app.data.AppUpdateInfo
import com.quriva.app.data.UpdateDownloader
import com.quriva.app.data.UpdateRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun UpdateAvailableDialog(
    info: AppUpdateInfo,
    currentVersion: String,
    onDismiss: () -> Unit,
    onSkipVersion: () -> Unit,
    onOpenRelease: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var downloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableStateOf(0f) }
    var downloadError by remember { mutableStateOf<String?>(null) }
    val visibleState = remember {
        MutableTransitionState(false).apply { targetState = true }
    }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        AnimatedVisibility(
            visibleState = visibleState,
            enter = fadeIn() + scaleIn(initialScale = 0.92f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)),
            exit = fadeOut() + scaleOut(targetScale = 0.96f)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(28.dp),
                color = colors.surface,
                tonalElevation = 2.dp,
                border = BorderStroke(1.dp, colors.outlineVariant)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    listOf(colors.surfaceVariant, colors.surface)
                                )
                            )
                            .padding(horizontal = 22.dp, vertical = 20.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = colors.onBackground,
                                modifier = Modifier.size(46.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_book_open),
                                        contentDescription = null,
                                        tint = colors.background,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Pembaruan tersedia",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.onBackground
                                )
                                Text(
                                    "v$currentVersion  →  ${info.tagName}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = colors.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .padding(horizontal = 22.dp)
                            .padding(bottom = 8.dp)
                    ) {
                        if (info.name.isNotBlank() && !info.name.equals(info.tagName, true)) {
                            Text(
                                info.name,
                                style = MaterialTheme.typography.labelLarge,
                                color = colors.onBackground
                            )
                            Spacer(Modifier.height(6.dp))
                        }
                        val notes = info.body.trim()
                            .replace(Regex("(?m)^#{1,6}\\s*"), "• ")
                            .replace(Regex("\\*\\*(.+?)\\*\\*"), "$1")
                            .replace(Regex("`(.+?)`"), "$1")
                            .trim()
                            .ifBlank { "Versi terbaru Quriva sudah tersedia. Perbarui untuk mendapatkan fitur dan perbaikan terbaru." }
                        val preview = if (notes.length > 700) notes.take(700).trimEnd() + "…" else notes
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 220.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(colors.surfaceVariant)
                        ) {
                            Text(
                                text = preview,
                                style = MaterialTheme.typography.bodySmall.copy(lineHeight = 19.sp),
                                color = colors.onSurfaceVariant,
                                modifier = Modifier
                                    .padding(14.dp)
                                    .verticalScroll(rememberScrollState())
                            )
                        }

                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = {
                                val url = info.apkDownloadUrl
                                if (url.isNullOrBlank()) {
                                    onOpenRelease()
                                } else if (!downloading) {
                                    downloading = true
                                    downloadProgress = 0f
                                    downloadError = null
                                    scope.launch {
                                        try {
                                            val file = UpdateDownloader.download(
                                                context = context,
                                                url = url,
                                                expectedSize = info.apkSize,
                                                onProgress = { downloadProgress = it }
                                            )
                                            when (UpdateDownloader.install(context, file, info.versionName)) {
                                                UpdateDownloader.InstallResult.STARTED -> onDismiss()
                                                UpdateDownloader.InstallResult.PERMISSION_REQUIRED -> {
                                                    downloadError = "Izinkan pemasangan dari Quriva, lalu tekan tombol unduh lagi."
                                                }
                                                UpdateDownloader.InstallResult.INVALID_APK -> {
                                                    downloadError = "Berkas pembaruan tidak valid. Coba unduh lagi."
                                                }
                                                UpdateDownloader.InstallResult.FAILED -> {
                                                    downloadError = "Pemasang Android tidak dapat dibuka. Coba lagi."
                                                }
                                            }
                                        } catch (e: CancellationException) {
                                            throw e
                                        } catch (t: Throwable) {
                                            downloadError = UpdateRepository.friendlyError(t)
                                        } finally {
                                            downloading = false
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = CircleShape,
                            enabled = !downloading,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.onBackground,
                                contentColor = colors.background
                            )
                        ) {
                            if (downloading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = colors.background
                                )
                            } else {
                                Icon(painter = painterResource(R.drawable.ic_arrow_forward), contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                            Spacer(Modifier.width(8.dp))
                            val sizeLabel = if (info.apkSize > 0) " · ${formatBytes(info.apkSize)}" else ""
                            Text(
                                when {
                                    downloading -> "Mengunduh… ${(downloadProgress * 100).roundToInt()}%"
                                    info.apkDownloadUrl != null -> "Unduh pembaruan$sizeLabel"
                                    else -> "Buka halaman unduh"
                                },
                                modifier = Modifier.padding(vertical = 3.dp)
                            )
                        }
                        AnimatedVisibility(visible = downloading) {
                            LinearProgressIndicator(
                                progress = { downloadProgress },
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp).height(3.dp),
                                color = colors.onBackground,
                                trackColor = colors.outlineVariant
                            )
                        }
                        AnimatedVisibility(visible = !downloadError.isNullOrBlank()) {
                            Text(
                                text = downloadError.orEmpty(),
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.onBackground,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = onOpenRelease,
                                modifier = Modifier.weight(1f),
                                shape = CircleShape,
                                border = BorderStroke(1.dp, colors.outline)
                            ) {
                                Text("Rincian", color = colors.onBackground, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            OutlinedButton(
                                onClick = onDismiss,
                                modifier = Modifier.weight(1f),
                                shape = CircleShape,
                                border = BorderStroke(1.dp, colors.outline)
                            ) {
                                Text("Nanti", color = colors.onBackground)
                            }
                        }
                        TextButton(
                            onClick = onSkipVersion,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        ) {
                            Text("Lewati versi ini", color = colors.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
                        }
                        Spacer(Modifier.height(4.dp))
                    }
                }
            }
        }
    }
}

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1024L * 1024L -> "%.1f MB".format(bytes / (1024f * 1024f))
    bytes >= 1024L -> "${bytes / 1024L} KB"
    else -> "$bytes B"
}
