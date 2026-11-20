package com.quriva.app.data

import androidx.compose.runtime.Immutable

@Immutable
data class AyahAudio(
    val ayahNumber: Int,
    val verseKey: String,
    val url: String
)

enum class RepeatMode(val label: String) {
    OFF("Mati"),
    SURAH("Surah"),
    AYAH("Ayat")
}

@Immutable
data class AudioStateSnapshot(
    val surahNumber: Int,
    val surahName: String,
    val reciterId: Int,
    val reciterName: String,
    val ayahNumber: Int,
    val positionMs: Long
)
