package com.quriva.app.ui.screens

import android.app.Application
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quriva.app.R
import com.quriva.app.model.JuzMeta
import com.quriva.app.ui.JuzUiState
import com.quriva.app.ui.JuzViewModel
import com.quriva.app.ui.JuzViewModelFactory
import com.quriva.app.ui.AnimatedListItem
import com.quriva.app.ui.interactiveClickable

@Composable
fun JuzScreen(
    onBack: () -> Unit,
    onOpenAyah: (surah: Int, ayah: Int) -> Unit
) {
    val application = LocalContext.current.applicationContext as Application
    val viewModel: JuzViewModel = viewModel(factory = JuzViewModelFactory(application))
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = MaterialTheme.colorScheme

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = colors.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            FeatureTopBar("Juz", "30 bagian Al-Qur'an", onBack)

            when (val current = state) {
                is JuzUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = colors.onBackground,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                is JuzUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp)
                            .padding(top = 56.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Daftar juz gagal dimuat",
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
                        Spacer(Modifier.height(20.dp))
                        Button(
                            onClick = viewModel::load,
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.onBackground,
                                contentColor = colors.background
                            )
                        ) {
                            Text("Coba lagi")
                        }
                    }
                }

                is JuzUiState.Success -> {
                    val names = current.surahs.associateBy { it.number }
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 28.dp)
                    ) {
                        item { JuzIntro() }
                        itemsIndexed(
                            items = current.juzs,
                            key = { _, juz -> juz.number },
                            contentType = { _, _ -> "juz" }
                        ) { index, juz ->
                            AnimatedListItem(index = index) {
                                JuzRow(
                                    juz = juz,
                                    startName = names[juz.startSurah]?.latinName ?: "Surah ${juz.startSurah}",
                                    endName = names[juz.endSurah]?.latinName ?: "Surah ${juz.endSurah}",
                                    onClick = { onOpenAyah(juz.startSurah, juz.startAyah) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun JuzIntro() {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 10.dp, bottom = 18.dp),
        shape = RoundedCornerShape(26.dp),
        color = colors.inverseSurface
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "AL-QUR'AN",
                    style = MaterialTheme.typography.labelSmall,
                    letterSpacing = 1.8.sp,
                    color = colors.inverseOnSurface.copy(alpha = 0.55f)
                )
                Spacer(Modifier.height(7.dp))
                Text(
                    text = "30 juz, satu perjalanan.",
                    style = MaterialTheme.typography.headlineSmall,
                    color = colors.inverseOnSurface
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    text = "6.236 ayat · 114 surah",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.inverseOnSurface.copy(alpha = 0.65f)
                )
            }
            Text(
                text = "30",
                style = MaterialTheme.typography.displayMedium,
                color = colors.inverseOnSurface
            )
        }
    }
}

@Composable
private fun JuzRow(
    juz: JuzMeta,
    startName: String,
    endName: String,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .interactiveClickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(52.dp)
                    .height(48.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = juz.number.toString().padStart(2, '0'),
                    style = MaterialTheme.typography.headlineMedium,
                    color = colors.onBackground
                )
            }
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Juz ${juz.number}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onBackground
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = "$startName ${juz.startAyah} — $endName ${juz.endAyah}",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                    maxLines = 1
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    text = "${juz.ayahCount} ayat",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
            Surface(
                shape = CircleShape,
                color = Color.Transparent,
                border = BorderStroke(1.dp, colors.outline)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_forward),
                    contentDescription = "Buka juz",
                    tint = colors.onBackground,
                    modifier = Modifier
                        .padding(8.dp)
                        .size(15.dp)
                )
            }
        }
        HorizontalDivider(
            modifier = Modifier.padding(start = 84.dp, end = 24.dp),
            color = colors.outlineVariant
        )
    }
}
