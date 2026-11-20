package com.quriva.app.model

import androidx.compose.runtime.Immutable

@Immutable
data class SurahMeta(
    val number: Int,
    val nameArabic: String,
    val latinName: String,
    val translation: String,
    val place: String,
    val ayahCount: Int,
    val bismillah: Boolean,
    val pageStart: Int,
    val pageEnd: Int
)

@Immutable
data class Ayah(
    val numberInSurah: Int,
    val arabic: String,
    val translation: String,
    val page: Int = 0,
    val juz: Int = 0,
    val hizb: Int = 0
)

@Immutable
data class SurahDetail(
    val meta: SurahMeta,
    val ayahs: List<Ayah>
)

@Immutable
data class LastRead(
    val surahNumber: Int,
    val surahName: String,
    val ayah: Int,
    val totalAyah: Int
)

@Immutable
data class Bookmark(
    val surahNumber: Int,
    val surahName: String,
    val ayahNumber: Int,
    val arabic: String,
    val translation: String,
    val createdAt: Long,
    val folder: String = "",
    val tags: List<String> = emptyList()
)

@Immutable
data class VerseNote(
    val surahNumber: Int,
    val ayahNumber: Int,
    val surahName: String,
    val text: String,
    val updatedAt: Long
)

@Immutable
data class JuzMeta(
    val number: Int,
    val startSurah: Int,
    val startAyah: Int,
    val endSurah: Int,
    val endAyah: Int,
    val ayahCount: Int
)

@Immutable
data class SearchResult(
    val surahNumber: Int,
    val surahName: String,
    val surahArabic: String,
    val ayahNumber: Int,
    val arabic: String,
    val translation: String
)

@Immutable
data class SearchResponse(
    val results: List<SearchResult>,
    val total: Int
)

@Immutable
data class ReadingProgress(
    val daily: Map<String, Set<String>> = emptyMap(),
    val playDaily: Map<String, Int> = emptyMap(),
    val lastDate: String = "",
    val totalVersesRead: Int = 0,
    val totalPlayedAyah: Int = 0
)

@Immutable
data class PageRange(
    val page: Int,
    val startSurah: Int,
    val startAyah: Int,
    val endSurah: Int,
    val endAyah: Int,
    val juz: Int
)
