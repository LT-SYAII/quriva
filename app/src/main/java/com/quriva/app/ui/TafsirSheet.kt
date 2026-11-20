package com.quriva.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.text.HtmlCompat
import com.quriva.app.R
import com.quriva.app.data.AppPreferences
import com.quriva.app.data.QuranApi
import com.quriva.app.data.TafsirContent
import com.quriva.app.data.TafsirOption
import com.quriva.app.data.Tafsirs
import com.quriva.app.ui.theme.ArabicFontFamily

private sealed interface TafsirUiState {
    data object Loading : TafsirUiState
    data class Success(val content: TafsirContent) : TafsirUiState
    data class Error(val message: String) : TafsirUiState
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TafsirSheet(
    surahNumber: Int,
    surahName: String,
    ayahNumber: Int,
    prefs: AppPreferences,
    onDismiss: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    var tafsirId by rememberSaveable { mutableIntStateOf(prefs.tafsirId) }
    var state by remember { mutableStateOf<TafsirUiState>(TafsirUiState.Loading) }
    var retryKey by remember { mutableIntStateOf(0) }
    val selected = Tafsirs.byId(tafsirId)
    val scrollState = rememberScrollState()

    LaunchedEffect(tafsirId, retryKey) {
        state = TafsirUiState.Loading
        state = try {
            val content = QuranApi.tafsir(tafsirId, surahNumber, ayahNumber)
            scrollState.scrollTo(0)
            TafsirUiState.Success(content)
        } catch (t: Throwable) {
            TafsirUiState.Error(QuranApi.friendlyNetworkError(t))
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
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 22.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Tafsir",
                        style = MaterialTheme.typography.headlineSmall,
                        color = colors.onBackground
                    )
                    Text(
                        text = "$surahName · ayat $ayahNumber",
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        painter = painterResource(R.drawable.ic_close),
                        contentDescription = "Tutup",
                        tint = colors.onBackground
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
            ) {
                Tafsirs.all.forEach { option ->
                    TafsirChip(
                        option = option,
                        selected = option.id == tafsirId,
                        onClick = {
                            tafsirId = option.id
                            prefs.updateTafsir(option.id)
                        }
                    )
                }
            }

            Spacer(Modifier.height(18.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 470.dp)
            ) {
                when (val current = state) {
                    is TafsirUiState.Loading -> Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 44.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            color = colors.onBackground,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(27.dp)
                        )
                        Spacer(Modifier.height(14.dp))
                        Text(
                            text = "Memuat tafsir...",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant
                        )
                    }

                    is TafsirUiState.Error -> Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 30.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Tafsir gagal dimuat",
                            style = MaterialTheme.typography.titleLarge,
                            color = colors.onBackground,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = current.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(18.dp))
                        Button(
                            onClick = { retryKey++ },
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.onBackground,
                                contentColor = colors.background
                            )
                        ) {
                            Text("Coba lagi")
                        }
                    }

                    is TafsirUiState.Success -> Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(scrollState)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = current.content.resourceName,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.onBackground,
                                modifier = Modifier.weight(1f)
                            )
                            Surface(
                                shape = CircleShape,
                                color = colors.surfaceVariant,
                                border = BorderStroke(1.dp, colors.outlineVariant)
                            ) {
                                Text(
                                    text = selected.language,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(Modifier.height(14.dp))
                        SelectionContainer {
                            if (selected.isArabic) {
                                CompositionLocalProvider(
                                    LocalLayoutDirection provides LayoutDirection.Rtl
                                ) {
                                    Text(
                                        text = htmlToPlainText(current.content.html),
                                        modifier = Modifier.fillMaxWidth(),
                                        fontFamily = ArabicFontFamily,
                                        fontSize = 20.sp,
                                        lineHeight = 38.sp,
                                        color = colors.onBackground,
                                        textAlign = TextAlign.Start
                                    )
                                }
                            } else {
                                Text(
                                    text = htmlToPlainText(current.content.html),
                                    modifier = Modifier.fillMaxWidth(),
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        lineHeight = 24.sp
                                    ),
                                    color = colors.onBackground
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            Text(
                text = "Tafsir berbahasa Indonesia belum tersedia untuk saat ini. Pilih tafsir Inggris atau Arab di atas.",
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TafsirChip(
    option: TafsirOption,
    selected: Boolean,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (selected) colors.onBackground else Color.Transparent,
        contentColor = if (selected) colors.background else colors.onSurfaceVariant,
        border = if (selected) null else BorderStroke(1.dp, colors.outline)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = option.name,
                style = MaterialTheme.typography.labelMedium
            )
            Spacer(Modifier.size(6.dp))
            Text(
                text = option.language,
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) colors.background.copy(alpha = 0.7f)
                else colors.onSurfaceVariant.copy(alpha = 0.8f)
            )
        }
    }
}

private fun htmlToPlainText(html: String): String {
    val withBreaks = html
        .replace(Regex("(?i)</(p|h[1-6]|li|div|blockquote)>"), "\n\n")
        .replace(Regex("(?i)<br\\s*/?>"), "\n")
    val text = HtmlCompat.fromHtml(withBreaks, HtmlCompat.FROM_HTML_MODE_LEGACY).toString()
    return text
        .replace('\u00A0', ' ')
        .lines()
        .joinToString("\n") { it.trim() }
        .replace(Regex("\n{3,}"), "\n\n")
        .trim()
}
