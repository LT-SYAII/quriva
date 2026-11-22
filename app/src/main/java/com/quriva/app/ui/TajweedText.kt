package com.quriva.app.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle

/**
 * Parses quran.com uthmani_tajweed markup into a colored [AnnotatedString].
 * Colors are chosen to be readable in both light/dark themes (saturated but not neon).
 */
fun parseTajweed(tagged: String, base: Color): AnnotatedString {
    val text = tagged
        .replace("<span class=end>", "")
        .replace("</span>", "")
    val regex = Regex("<tajweed class=([a-zA-Z0-9_\\-]+)>(.*?)</tajweed>", RegexOption.DOT_MATCHES_ALL)

    return buildAnnotatedString {
        var last = 0
        for (m in regex.findAll(text)) {
            if (m.range.first > last) {
                withStyle(SpanStyle(color = base)) { append(stripTags(text.substring(last, m.range.first))) }
            }
            val cls = m.groupValues[1]
            val inner = stripTags(m.groupValues[2])
            val color = tajweedColor(cls, base)
            val start = length
            withStyle(SpanStyle(color = color)) { append(inner) }
            addStringAnnotation("TAJWEED", cls, start, length)
            last = m.range.last + 1
        }
        if (last < text.length) {
            withStyle(SpanStyle(color = base)) { append(stripTags(text.substring(last))) }
        }
    }
}

data class TajweedRuleInfo(val name: String, val explanation: String)

object TajweedRules {
    private val rules = mapOf(
        "ham_wasl" to TajweedRuleInfo("Hamzah Wasal", "Dibaca ketika memulai dan gugur ketika disambung dari kata sebelumnya."),
        "laam_shamsiyah" to TajweedRuleInfo("Lam Syamsiyah", "Lam tidak dibaca dan huruf sesudahnya dibaca tasydid."),
        "madda_normal" to TajweedRuleInfo("Mad Thabi'i", "Dipanjangkan dua harakat."),
        "madda_permissible" to TajweedRuleInfo("Mad Jaiz", "Boleh dipanjangkan sesuai riwayat bacaan."),
        "madda_necessary" to TajweedRuleInfo("Mad Lazim", "Dibaca panjang enam harakat."),
        "madda_obligatory" to TajweedRuleInfo("Mad Wajib", "Dibaca lebih panjang karena bertemu hamzah dalam satu kata."),
        "ikhafa" to TajweedRuleInfo("Ikhfa", "Nun sukun atau tanwin dibaca samar dengan dengung."),
        "ikhafa_shafawi" to TajweedRuleInfo("Ikhfa Syafawi", "Mim sukun bertemu ba, dibaca samar dengan dengung."),
        "idgham_with_ghunnah" to TajweedRuleInfo("Idgham Bighunnah", "Nun sukun atau tanwin melebur dengan dengung."),
        "idgham_without_ghunnah" to TajweedRuleInfo("Idgham Bilaghunnah", "Nun sukun atau tanwin melebur tanpa dengung."),
        "iqlab" to TajweedRuleInfo("Iqlab", "Nun sukun atau tanwin berubah menjadi bunyi mim samar."),
        "qalqalah" to TajweedRuleInfo("Qalqalah", "Huruf qalqalah sukun dibaca memantul."),
        "ghunnah" to TajweedRuleInfo("Ghunnah", "Nun atau mim tasydid dibaca dengung dua harakat.")
    )
    fun info(id: String): TajweedRuleInfo = rules[id] ?: TajweedRuleInfo(id.replace('_', ' ').replaceFirstChar { it.uppercase() }, "Ikuti warna dan dengarkan bacaan qari untuk contoh pelafalan.")
    fun all(): List<Pair<String, TajweedRuleInfo>> = rules.toList()
}

private fun stripTags(s: String): String = s.replace(Regex("<[^>]+>"), "")

private fun tajweedColor(cls: String, base: Color): Color = when {
    cls.contains("ghunnah") || cls.contains("madda") -> Color(0xFF2E7D32) // green
    cls.contains("ikhafa") || cls.contains("iqlab") || cls.contains("idgh") -> Color(0xFFD84315) // deep orange
    cls.contains("qalqalah") -> Color(0xFF1565C0) // blue
    cls.contains("ham_wasl") || cls.contains("silent") || cls.contains("laam_shamsiyah") -> base.copy(alpha = 0.55f)
    cls.contains("slnt") -> base.copy(alpha = 0.55f)
    else -> Color(0xFF6A1B9A) // purple fallback for other rules
}
