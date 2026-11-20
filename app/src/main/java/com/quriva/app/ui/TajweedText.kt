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
            withStyle(SpanStyle(color = color)) { append(inner) }
            last = m.range.last + 1
        }
        if (last < text.length) {
            withStyle(SpanStyle(color = base)) { append(stripTags(text.substring(last))) }
        }
    }
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
