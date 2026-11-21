package com.quriva.app.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.quriva.app.R
import com.quriva.app.data.AppPreferences
import com.quriva.app.ui.AnimatedListItem
import com.quriva.app.ui.interactiveClickable

@Composable
fun NotesScreen(
    prefs: AppPreferences,
    onBack: () -> Unit,
    onOpenAyah: (surah: Int, ayah: Int) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val notes = prefs.notes

    Surface(modifier = Modifier.fillMaxSize(), color = colors.background) {
        Column(modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            Row(
                modifier = Modifier.fillMaxWidth().height(58.dp).padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(painter = painterResource(R.drawable.ic_back), contentDescription = "Kembali", tint = colors.onBackground, modifier = Modifier.size(22.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Catatan pribadi", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = colors.onBackground)
                    Text(if (notes.isEmpty()) "Belum ada catatan" else "${notes.size} catatan", style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                }
            }

            if (notes.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp).padding(top = 72.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(shape = CircleShape, color = Color.Transparent, border = BorderStroke(1.dp, colors.outline)) {
                        Icon(painter = painterResource(R.drawable.ic_book_open), contentDescription = null, tint = colors.onBackground, modifier = Modifier.padding(16.dp).size(24.dp))
                    }
                    Spacer(Modifier.height(18.dp))
                    Text("Tulis refleksi Anda", style = MaterialTheme.typography.headlineSmall, color = colors.onBackground, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(8.dp))
                    Text("Buka ayat di pembaca lalu gunakan fitur catatan untuk menyimpan refleksi pribadi pada ayat tertentu.", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant, textAlign = TextAlign.Center)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 28.dp)) {
                    itemsIndexed(notes, key = { _, note -> "${note.surahNumber}:${note.ayahNumber}" }) { index, note ->
                        AnimatedListItem(index = index) {
                            Column(modifier = Modifier.fillMaxWidth().interactiveClickable { onOpenAyah(note.surahNumber, note.ayahNumber) }) {
                                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("${note.surahName} · ${note.surahNumber}:${note.ayahNumber}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = colors.onBackground, modifier = Modifier.weight(1f))
                                        IconButton(onClick = { prefs.removeNote(note.surahNumber, note.ayahNumber) }, modifier = Modifier.size(32.dp)) {
                                            Icon(painter = painterResource(R.drawable.ic_delete), contentDescription = "Hapus catatan", tint = colors.onSurfaceVariant, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                    Spacer(Modifier.height(6.dp))
                                    Text(note.text, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant, maxLines = 4, overflow = TextOverflow.Ellipsis)
                                }
                                HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp), color = colors.outlineVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}
