package com.quriva.app.model

import androidx.compose.runtime.Immutable

@Immutable
data class QuranNavPoint(
    val surah: Int,
    val ayah: Int,
    val page: Int,
    val juz: Int,
    val hizb: Int,
    val rub: Int,
    val ruku: Int,
    val manzil: Int,
    val sajdah: Int
)

@Immutable
data class IslamicContentItem(
    val id: String,
    val category: String,
    val categoryName: String,
    val title: String,
    val arabic: String,
    val latin: String,
    val translation: String,
    val notes: String,
    val benefit: String,
    val source: String,
    val repeat: Int
)

@Immutable
data class AsmaName(
    val number: Int,
    val latin: String,
    val arabic: String,
    val meaning: String
)

@Immutable
data class HadithItem(
    val number: Int,
    val title: String,
    val arabic: String,
    val translationEnglish: String,
    val source: String
)

enum class MemorizationStatus(val label: String, val intervalDays: Int) {
    NEW("Baru", 1),
    LEARNING("Sedang dihafal", 1),
    REVIEW("Perlu murajaah", 3),
    FLUENT("Lancar", 14)
}

@Immutable
data class MemorizationEntry(
    val surah: Int,
    val surahName: String,
    val startAyah: Int,
    val endAyah: Int,
    val status: MemorizationStatus,
    val reviewAt: Long,
    val updatedAt: Long
) {
    val key: String get() = "$surah:$startAyah-$endAyah"
}

@Immutable
data class IslamicEvent(
    val id: String,
    val name: String,
    val hijriMonth: Int,
    val hijriDay: Int,
    val description: String,
    val estimated: Boolean = true,
    val durationDays: Int = 1
)

@Immutable
data class UpcomingIslamicEvent(
    val event: IslamicEvent,
    val targetMillis: Long,
    val hijriYear: Int
)
