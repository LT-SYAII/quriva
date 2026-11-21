package com.quriva.app.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quriva.app.R
import com.quriva.app.data.AppPreferences
import com.quriva.app.model.Bookmark
import com.quriva.app.ui.AnimatedListItem
import com.quriva.app.ui.interactiveClickable
import com.quriva.app.ui.theme.ArabicFontFamily

@Composable
fun BookmarksScreen(
    prefs: AppPreferences,
    onBack: () -> Unit,
    onOpenAyah: (surah: Int, ayah: Int) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val bookmarks = prefs.bookmarks
    var showClearDialog by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = colors.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        painter = painterResource(R.drawable.ic_back),
                        contentDescription = "Kembali",
                        tint = colors.onBackground,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Bookmark",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.onBackground
                    )
                    Text(
                        text = if (bookmarks.isEmpty()) "Belum ada ayat tersimpan" else "${bookmarks.size} ayat tersimpan",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant
                    )
                }
                if (bookmarks.isNotEmpty()) {
                    IconButton(onClick = { showClearDialog = true }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_delete),
                            contentDescription = "Hapus semua bookmark",
                            tint = colors.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            if (bookmarks.isEmpty()) {
                EmptyBookmarks()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 10.dp, bottom = 28.dp)
                ) {
                    itemsIndexed(
                        items = bookmarks,
                        key = { _, bookmark -> "${bookmark.surahNumber}:${bookmark.ayahNumber}" },
                        contentType = { _, _ -> "bookmark" }
                    ) { index, bookmark ->
                        AnimatedListItem(index = index) {
                            BookmarkRow(
                                bookmark = bookmark,
                                onClick = { onOpenAyah(bookmark.surahNumber, bookmark.ayahNumber) },
                                onRemove = {
                                    prefs.removeBookmark(bookmark.surahNumber, bookmark.ayahNumber)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Hapus semua bookmark?") },
            text = { Text("Semua ayat yang Anda simpan akan dihapus. Tindakan ini tidak dapat dibatalkan.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        prefs.clearBookmarks()
                        showClearDialog = false
                    }
                ) {
                    Text("Hapus", color = colors.onBackground)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Batal", color = colors.onSurfaceVariant)
                }
            },
            containerColor = colors.surface,
            titleContentColor = colors.onBackground,
            textContentColor = colors.onSurfaceVariant
        )
    }
}

@Composable
private fun BookmarkRow(
    bookmark: Bookmark,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .interactiveClickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 12.dp, top = 14.dp, bottom = 18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${bookmark.surahName} · ${bookmark.surahNumber}:${bookmark.ayahNumber}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onBackground,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onRemove, modifier = Modifier.size(34.dp)) {
                    Icon(
                        painter = painterResource(R.drawable.ic_bookmark_filled),
                        contentDescription = "Hapus bookmark",
                        tint = colors.onSurfaceVariant,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Text(
                    text = bookmark.arabic,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 12.dp),
                    fontFamily = ArabicFontFamily,
                    fontSize = 23.sp,
                    lineHeight = 38.sp,
                    color = colors.onBackground,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Start
                )
            }
            Spacer(Modifier.height(7.dp))
            Text(
                text = bookmark.translation,
                modifier = Modifier.padding(end = 12.dp),
                style = MaterialTheme.typography.bodySmall.copy(lineHeight = 20.sp),
                color = colors.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        HorizontalDivider(
            modifier = Modifier.padding(start = 24.dp, end = 24.dp),
            color = colors.outlineVariant
        )
    }
}

@Composable
private fun EmptyBookmarks() {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp)
            .padding(top = 72.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = CircleShape,
            color = Color.Transparent,
            border = BorderStroke(1.dp, colors.outline)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_bookmark),
                contentDescription = null,
                tint = colors.onBackground,
                modifier = Modifier
                    .padding(16.dp)
                    .size(26.dp)
            )
        }
        Spacer(Modifier.height(22.dp))
        Text(
            text = "Simpan ayat yang bermakna",
            style = MaterialTheme.typography.headlineSmall,
            color = colors.onBackground,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Tekan ikon bookmark di samping ayat saat membaca. Semua bookmark tersimpan di perangkat ini.",
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
