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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    var query by remember { mutableStateOf("") }
    val visible = remember(notes, query) { notes.filter { query.isBlank() || it.surahName.contains(query, true) || it.text.contains(query, true) } }

    Surface(modifier = Modifier.fillMaxSize(), color = colors.background) {
        Column(modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            FeatureTopBar(
                "Catatan pribadi",
                if (notes.isEmpty()) "Belum ada catatan" else "${notes.size} catatan",
                onBack
            )

            if (notes.isNotEmpty()) {
                OutlinedTextField(value = query, onValueChange = { query = it }, placeholder = { Text("Cari catatan") }, singleLine = true, modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp))
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
                    itemsIndexed(visible, key = { _, note -> "${note.surahNumber}:${note.ayahNumber}" }) { index, note ->
                        AnimatedListItem(index = index) {
                            Surface(
                                onClick = { onOpenAyah(note.surahNumber, note.ayahNumber) },
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp),
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
                                color = colors.surfaceVariant,
                                border = BorderStroke(1.dp, colors.outlineVariant)
                            ) {
                                Column(modifier = Modifier.fillMaxWidth().padding(start = 15.dp, end = 7.dp, top = 12.dp, bottom = 15.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(shape = CircleShape, color = colors.background, border = BorderStroke(1.dp, colors.outlineVariant)) {
                                            Text(
                                                "${note.surahNumber}:${note.ayahNumber}",
                                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Spacer(Modifier.width(9.dp))
                                        Text(note.surahName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = colors.onBackground, modifier = Modifier.weight(1f))
                                        IconButton(onClick = { prefs.removeNote(note.surahNumber, note.ayahNumber) }, modifier = Modifier.size(48.dp)) {
                                            Icon(painter = painterResource(R.drawable.ic_delete), contentDescription = "Hapus catatan", tint = colors.onSurfaceVariant, modifier = Modifier.size(17.dp))
                                        }
                                    }
                                    Spacer(Modifier.height(5.dp))
                                    Text(note.text, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, maxLines = 4, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
