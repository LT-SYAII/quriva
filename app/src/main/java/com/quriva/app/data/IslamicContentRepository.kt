package com.quriva.app.data

import android.content.Context
import com.quriva.app.model.AsmaName
import com.quriva.app.model.HadithItem
import com.quriva.app.model.IslamicContentItem
import org.json.JSONArray

object IslamicContentRepository {
    @Volatile private var contentCache: List<IslamicContentItem>? = null
    @Volatile private var asmaCache: List<AsmaName>? = null
    @Volatile private var hadithCache: List<HadithItem>? = null

    fun content(context: Context): List<IslamicContentItem> = contentCache ?: synchronized(this) {
        contentCache ?: JSONArray(read(context, "content/dua_dhikr_id.json")).let { arr ->
            buildList(arr.length()) {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    add(
                        IslamicContentItem(
                            id = o.optString("id", "item-$i"),
                            category = o.optString("category"),
                            categoryName = o.optString("categoryName"),
                            title = o.optString("title"),
                            arabic = o.optString("arabic"),
                            latin = o.optString("latin"),
                            translation = o.optString("translation"),
                            notes = o.optString("notes"),
                            benefit = o.optString("fawaid"),
                            source = o.optString("source"),
                            repeat = o.optInt("repeat", 1).coerceAtLeast(1)
                        )
                    )
                }
            }.also { contentCache = it }
        }
    }

    fun asmaulHusna(context: Context): List<AsmaName> = asmaCache ?: synchronized(this) {
        asmaCache ?: JSONArray(read(context, "content/asmaul_husna_id.json")).let { arr ->
            buildList(arr.length()) {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    add(AsmaName(o.optInt("urutan"), o.optString("latin"), o.optString("arab"), o.optString("arti")))
                }
            }.also { asmaCache = it }
        }
    }

    fun arbain(context: Context): List<HadithItem> = hadithCache ?: synchronized(this) {
        hadithCache ?: JSONArray(read(context, "content/arbain_nawawi.json")).let { arr ->
            buildList(arr.length()) {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    add(
                        HadithItem(
                            number = o.optInt("number"),
                            title = o.optString("title"),
                            arabic = o.optString("arabic"),
                            translationEnglish = o.optString("translationEnglish"),
                            source = o.optString("source", "Arbain Nawawi")
                        )
                    )
                }
            }.also { hadithCache = it }
        }
    }

    private fun read(context: Context, path: String): String =
        context.assets.open(path).bufferedReader().use { it.readText() }
}
