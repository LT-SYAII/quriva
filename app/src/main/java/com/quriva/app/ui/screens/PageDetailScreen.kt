package com.quriva.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quriva.app.R
import com.quriva.app.data.PageAyah
import com.quriva.app.data.QuranApi
import com.quriva.app.ui.theme.ArabicFontFamily

@Composable
fun PageDetailScreen(
    page: Int,
    onBack: () -> Unit,
    onOpenAyah: (surah: Int, ayah: Int) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var verses by remember { mutableStateOf<List<PageAyah>>(emptyList()) }

    LaunchedEffect(page) {
        loading = true
        error = null
        try {
            verses = QuranApi.versesByPage(page)
        } catch (t: Throwable) {
            error = QuranApi.friendlyNetworkError(t)
        } finally {
            loading = false
        }
    }

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
                    Text("Halaman $page", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = colors.onBackground)
                    val meta = verses.firstOrNull()
                    Text(
                        if (meta != null) "Juz ${meta.juz} · Hizb ${meta.hizb} · Manzil ${meta.manzil}" else "Mushaf Madinah",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant
                    )
                }
            }

            when {
                loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = colors.onBackground, strokeWidth = 2.dp)
                        Spacer(Modifier.height(12.dp))
                        Text("Memuat halaman...", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                    }
                }
                error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                        Text("Gagal memuat halaman", style = MaterialTheme.typography.titleLarge, color = colors.onBackground)
                        Spacer(Modifier.height(8.dp))
                        Text(error ?: "", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { /* retry by recomposition via key not needed; simple */ }, enabled = false, colors = ButtonDefaults.buttonColors(containerColor = colors.onBackground, contentColor = colors.background)) { Text("Muat ulang") }
                    }
                }
                else -> LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 28.dp)) {
                    items(verses, key = { it.verseKey }) { v ->
                        Column(modifier = Modifier.fillMaxWidth().clickable { onOpenAyah(v.surah, v.ayah) }.padding(horizontal = 24.dp, vertical = 14.dp)) {
                            Text("${v.verseKey} · Juz ${v.juz}", style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                            Spacer(Modifier.height(8.dp))
                            Text(v.text, fontFamily = ArabicFontFamily, fontSize = 24.sp, lineHeight = 42.sp, color = colors.onBackground, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth())
                        }
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp), color = colors.outlineVariant)
                    }
                    if (verses.isEmpty()) {
                        item { Text("Tidak ada ayat pada halaman ini.", modifier = Modifier.padding(24.dp), color = colors.onSurfaceVariant) }
                    }
                }
            }
        }
    }
}
