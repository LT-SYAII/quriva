package com.quriva.app.data

import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import java.io.OutputStream

object BackupRepository {
    fun writePdf(output: OutputStream, prefs: AppPreferences) {
        val pdf = PdfDocument()
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 12f }
        val title = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 20f; isFakeBoldText = true }
        val lines = buildList {
            add("Quriva — Bookmark & Catatan")
            prefs.bookmarks.forEach { add("Bookmark · ${it.surahName} ${it.surahNumber}:${it.ayahNumber}"); add(it.translation) }
            prefs.notes.forEach { add("Catatan · ${it.surahName} ${it.surahNumber}:${it.ayahNumber}"); add(it.text) }
            prefs.memorizationEntries.forEach { add("Hafalan · ${it.surahName} ${it.startAyah}-${it.endAyah} · ${it.status.label}") }
        }
        var pageNo = 1; var index = 0
        while (index < lines.size) {
            val page = pdf.startPage(PdfDocument.PageInfo.Builder(595, 842, pageNo++).create())
            var y = 48f
            page.canvas.drawText(if (index == 0) lines[index++] else "Quriva — lanjutan", 36f, y, title); y += 30
            while (index < lines.size && y < 805) {
                wrap(lines[index++], 82).forEach { page.canvas.drawText(it, 36f, y, paint); y += 18 }
                y += 6
            }
            pdf.finishPage(page)
        }
        pdf.writeTo(output); pdf.close()
    }
    private fun wrap(text: String, limit: Int): List<String> {
        val words = text.split(' '); val result = mutableListOf<String>(); var line = ""
        words.forEach { w -> val next = if (line.isBlank()) w else "$line $w"; if (next.length > limit) { if (line.isNotBlank()) result += line; line = w } else line = next }
        if (line.isNotBlank()) result += line
        return result.ifEmpty { listOf("") }
    }
}
