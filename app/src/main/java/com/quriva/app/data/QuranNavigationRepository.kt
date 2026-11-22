package com.quriva.app.data

import android.content.Context
import com.quriva.app.model.QuranNavPoint
import org.json.JSONArray

object QuranNavigationRepository {
    @Volatile private var cache: List<QuranNavPoint>? = null

    fun all(context: Context): List<QuranNavPoint> = cache ?: synchronized(this) {
        cache ?: run {
            val raw = context.assets.open("quran_navigation.json").bufferedReader().use { it.readText() }
            val arr = JSONArray(raw)
            buildList(arr.length()) {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val key = o.getString("k")
                    add(
                        QuranNavPoint(
                            surah = key.substringBefore(':').toInt(),
                            ayah = key.substringAfter(':').toInt(),
                            page = o.getInt("p"),
                            juz = o.getInt("j"),
                            hizb = o.getInt("h"),
                            rub = o.getInt("q"),
                            ruku = o.getInt("r"),
                            manzil = o.getInt("m"),
                            sajdah = o.getInt("s")
                        )
                    )
                }
            }.also { cache = it }
        }
    }

    fun starts(context: Context, selector: (QuranNavPoint) -> Int): List<Pair<Int, QuranNavPoint>> =
        all(context).groupBy(selector).toSortedMap().mapNotNull { (number, points) ->
            if (number > 0) number to points.first() else null
        }

    fun page(context: Context, page: Int): List<QuranNavPoint> = all(context).filter { it.page == page }
}
