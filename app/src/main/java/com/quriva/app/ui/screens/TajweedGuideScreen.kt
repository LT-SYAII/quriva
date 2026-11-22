package com.quriva.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.quriva.app.ui.TajweedRules

@Composable
fun TajweedGuideScreen(onBack: () -> Unit) {
    val rules = remember { TajweedRules.all() }
    var quiz by remember { mutableIntStateOf(0) }
    var answer by remember { mutableStateOf<String?>(null) }
    val c = MaterialTheme.colorScheme
    Surface(Modifier.fillMaxSize(), color = c.background) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            FeatureTopBar("Belajar Tajwid", "Legenda · materi · kuis", onBack)
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
                item {
                    Surface(shape = RoundedCornerShape(22.dp), color = c.inverseSurface, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(18.dp)) {
                            Text("Ketuk teks tajwid berwarna di pembaca", color = c.inverseOnSurface, style = MaterialTheme.typography.titleMedium)
                            Text("Quriva akan menampilkan nama hukum dan cara membacanya.", color = c.inverseOnSurface.copy(alpha=.7f), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Spacer(Modifier.height(18.dp)); Text("Legenda Tajwid", style = MaterialTheme.typography.headlineSmall)
                }
                items(rules, key = { it.first }) { (id, info) ->
                    Surface(shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, c.outlineVariant), color = c.surfaceVariant, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
                            Surface(shape = CircleShape, color = ruleColor(id), modifier = Modifier.size(18.dp)) {}
                            Spacer(Modifier.width(12.dp)); Column { Text(info.name, fontWeight = FontWeight.SemiBold); Text(info.explanation, style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant) }
                        }
                    }
                }
                item {
                    Spacer(Modifier.height(22.dp)); Text("Kuis Cepat", style = MaterialTheme.typography.headlineSmall)
                    val current = rules[quiz % rules.size]
                    val options = remember(quiz) { (rules.filterNot { it.first == current.first }.shuffled().take(3) + current).shuffled() }
                    Text("Hukum apakah yang berarti: ${current.second.explanation}", modifier = Modifier.padding(vertical = 10.dp))
                    options.forEach { option ->
                        OutlinedButton(onClick = { answer = if (option.first == current.first) "Benar" else "Belum tepat" }, modifier = Modifier.fillMaxWidth(), shape = CircleShape) { Text(option.second.name) }
                    }
                    answer?.let { Text(it, modifier = Modifier.fillMaxWidth().padding(8.dp), fontWeight = FontWeight.Bold) }
                    Button(onClick = { quiz++; answer = null }, modifier = Modifier.fillMaxWidth(), shape = CircleShape) { Text("Soal berikutnya") }
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
