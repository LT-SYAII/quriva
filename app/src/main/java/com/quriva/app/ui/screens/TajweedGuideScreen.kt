package com.quriva.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quriva.app.R
import com.quriva.app.ui.TajweedRules

@Composable
fun TajweedGuideScreen(onBack: () -> Unit) {
    val rules = remember { TajweedRules.all() }
    var expandedRule by remember { mutableStateOf<String?>(null) }
    var quiz by remember { mutableIntStateOf(0) }
    var selectedAnswer by remember { mutableStateOf<String?>(null) }
    var score by remember { mutableIntStateOf(0) }
    val current = rules[quiz % rules.size]
    val options = remember(quiz) {
        (rules.filterNot { it.first == current.first }.shuffled().take(3) + current).shuffled()
    }
    val colors = MaterialTheme.colorScheme

    Surface(Modifier.fillMaxSize(), color = colors.background) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            FeatureTopBar("Belajar Tajwid", "Warna · penjelasan · latihan", onBack)
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 38.dp)
            ) {
                item(key = "tajweed-hero") {
                    Surface(
                        shape = RoundedCornerShape(27.dp),
                        color = colors.inverseSurface,
                        contentColor = colors.inverseOnSurface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(Modifier.fillMaxWidth()) {
                            Box(
                                Modifier.size(180.dp).align(Alignment.TopEnd).background(
                                    Brush.radialGradient(listOf(colors.inverseOnSurface.copy(alpha = .13f), Color.Transparent)),
                                    CircleShape
                                )
                            )
                            Column(Modifier.padding(20.dp)) {
                                Box(
                                    Modifier.size(42.dp).background(colors.inverseOnSurface.copy(alpha = .12f), RoundedCornerShape(14.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("ن", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(Modifier.height(14.dp))
                                Text("Warna yang bisa dijelaskan", style = MaterialTheme.typography.headlineSmall)
                                Spacer(Modifier.height(5.dp))
                                Text(
                                    "Ketuk teks tajwid berwarna saat membaca. Quriva akan menjelaskan hukum dan cara membacanya.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.inverseOnSurface.copy(alpha = .67f)
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(25.dp))
                    Text("Legenda Tajwid", style = MaterialTheme.typography.headlineSmall)
                    Text("Ketuk kartu untuk membuka penjelasan", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                    Spacer(Modifier.height(10.dp))
                }

                items(rules, key = { it.first }) { (id, info) ->
                    val expanded = expandedRule == id
                    Surface(
                        onClick = { expandedRule = if (expanded) null else id },
                        shape = RoundedCornerShape(19.dp),
                        border = BorderStroke(1.dp, colors.outlineVariant),
                        color = if (expanded) colors.inverseSurface else colors.surfaceVariant,
                        contentColor = if (expanded) colors.inverseOnSurface else colors.onBackground,
                        modifier = Modifier.fillMaxWidth().animateContentSize()
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier.size(34.dp).background(ruleColor(id), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(Modifier.size(11.dp).background(Color.White.copy(alpha = .85f), CircleShape))
                                }
                                Spacer(Modifier.width(12.dp))
                                Text(info.name, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                                Text(if (expanded) "Tutup" else "Pelajari", style = MaterialTheme.typography.labelSmall)
                            }
                            AnimatedVisibility(
                                visible = expanded,
                                enter = fadeIn() + expandVertically(),
                                exit = fadeOut() + shrinkVertically()
                            ) {
                                Column {
                                    Spacer(Modifier.height(12.dp))
                                    androidx.compose.material3.HorizontalDivider(color = colors.inverseOnSurface.copy(alpha = .12f))
                                    Spacer(Modifier.height(11.dp))
                                    Text(info.explanation, style = MaterialTheme.typography.bodyMedium, color = colors.inverseOnSurface.copy(alpha = .72f))
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }

                item(key = "tajweed-quiz") {
                    Spacer(Modifier.height(20.dp))
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                        Column(Modifier.weight(1f)) {
                            Text("Kuis Cepat", style = MaterialTheme.typography.headlineSmall)
                            Text("Kenali hukum dari penjelasannya", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                        }
                        Text("Skor $score", style = MaterialTheme.typography.labelLarge)
                    }
                    Spacer(Modifier.height(11.dp))
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = colors.surfaceVariant,
                        border = BorderStroke(1.dp, colors.outlineVariant),
                        modifier = Modifier.fillMaxWidth().animateContentSize()
                    ) {
                        Column(Modifier.padding(17.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("SOAL ${(quiz % rules.size) + 1}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                Spacer(Modifier.weight(1f))
                                Text("${rules.size} materi", style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                            }
                            Spacer(Modifier.height(10.dp))
                            Text("Hukum apakah yang berarti:", style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
                            Spacer(Modifier.height(4.dp))
                            Text(current.second.explanation, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(14.dp))
                            options.forEach { option ->
                                val chosen = selectedAnswer == option.first
                                val correct = option.first == current.first
                                val answered = selectedAnswer != null
                                val container = when {
                                    answered && correct -> colors.inverseSurface
                                    chosen -> colors.outline
                                    else -> colors.background
                                }
                                val content = if (answered && correct) colors.inverseOnSurface else colors.onBackground
                                Surface(
                                    onClick = {
                                        if (!answered) {
                                            selectedAnswer = option.first
                                            if (correct) score++
                                        }
                                    },
                                    enabled = !answered,
                                    shape = RoundedCornerShape(16.dp),
                                    color = container,
                                    contentColor = content,
                                    border = BorderStroke(1.dp, if (answered && correct) colors.inverseSurface else colors.outlineVariant),
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                                ) {
                                    Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            Modifier.size(9.dp).background(if (answered && correct) content else ruleColor(option.first), CircleShape)
                                        )
                                        Spacer(Modifier.width(10.dp))
                                        Text(option.second.name, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                                        if (answered && correct) Icon(painterResource(R.drawable.ic_check), null, Modifier.size(17.dp))
                                    }
                                }
                            }
                            AnimatedVisibility(selectedAnswer != null) {
                                Column {
                                    val correct = selectedAnswer == current.first
                                    Text(
                                        if (correct) "Benar — kamu mengenali hukumnya." else "Belum tepat. Jawabannya ${current.second.name}.",
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 7.dp),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        textAlign = TextAlign.Center
                                    )
                                    Button(
                                        onClick = { quiz++; selectedAnswer = null },
                                        modifier = Modifier.fillMaxWidth().height(50.dp),
                                        shape = CircleShape
                                    ) {
                                        Text("Soal berikutnya")
                                        Spacer(Modifier.width(7.dp))
                                        Icon(painterResource(R.drawable.ic_arrow_forward), null, Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

private fun ruleColor(id: String): Color = when {
    "madda" in id -> Color(0xFF2563EB)
    "ghunnah" in id -> Color(0xFF7C3AED)
    "ikhafa" in id -> Color(0xFF059669)
    "idgham" in id -> Color(0xFFD97706)
    "qalqalah" in id -> Color(0xFFDC2626)
    else -> Color(0xFF6B7280)
}
