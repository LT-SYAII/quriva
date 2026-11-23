package com.quriva.app.data

import androidx.compose.runtime.Immutable
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.tan
import kotlin.math.PI

/**
 * Offline prayer-time calculation.
 *
 * Uses the standard solar declination/equation-of-time approximation (NOAA)
 * and the altitude-based angles used by Kemenag, MWL, ISNA, and Umm al-Qura.
 * No network or Play Services are required.
 */
enum class PrayerMethod(
    val label: String,
    val fajrAngle: Double,
    val ishaAngle: Double,
    val ishaMinutesAfterMaghrib: Int? = null
) {
    KEMENAG("Kemenag RI", 20.0, 18.0),
    MWL("Muslim World League", 18.0, 17.0),
    UMM_AL_QURA("Umm al-Qura", 18.5, 0.0, ishaMinutesAfterMaghrib = 90),
    ISNA("ISNA (Amerika)", 15.0, 15.0)
}

enum class Prayer(val id: String, val label: String, val arabic: String) {
    FAJR("FAJR", "Subuh", "الفجر"),
    SUNRISE("SUNRISE", "Terbit", "الشروق"),
    DHUHR("DHUHR", "Dzuhur", "الظهر"),
    ASR("ASR", "Ashar", "العصر"),
    MAGHRIB("MAGHRIB", "Maghrib", "المغرب"),
    ISHA("ISHA", "Isya", "العشاء");

    val isPrayer: Boolean get() = this != SUNRISE
}

@Immutable
data class PrayerCity(
    val id: String,
    val name: String,
    val province: String,
    val latitude: Double,
    val longitude: Double,
    val timeZoneId: String
) {
    val label: String get() = "$name · $province"
}

@Immutable
data class PrayerTime(
    val prayer: Prayer,
    val millis: Long
) {
    fun label(zoneId: String): String = PrayerTimes.formatClock(millis, zoneId)
}

@Immutable
data class PrayerSchedule(
    val times: List<PrayerTime>,
    val tomorrowFajr: Long
) {
    fun next(now: Long = System.currentTimeMillis()): PrayerTime? =
        times.firstOrNull { it.prayer.isPrayer && it.millis > now }
            ?: times.lastOrNull { it.prayer == Prayer.FAJR }?.copy(millis = tomorrowFajr)

    fun timeFor(prayer: Prayer): PrayerTime? = times.firstOrNull { it.prayer == prayer }
}

object PrayerCities {
    val all: List<PrayerCity> = listOf(
        PrayerCity("jakarta", "Jakarta", "DKI Jakarta", -6.2088, 106.8456, "Asia/Jakarta"),
        PrayerCity("bandung", "Bandung", "Jawa Barat", -6.9175, 107.6191, "Asia/Jakarta"),
        PrayerCity("semarang", "Semarang", "Jawa Tengah", -6.9932, 110.4203, "Asia/Jakarta"),
        PrayerCity("yogyakarta", "Yogyakarta", "DI Yogyakarta", -7.7956, 110.3695, "Asia/Jakarta"),
        PrayerCity("solo", "Surakarta", "Jawa Tengah", -7.5755, 110.8243, "Asia/Jakarta"),
        PrayerCity("surabaya", "Surabaya", "Jawa Timur", -7.2575, 112.7521, "Asia/Jakarta"),
        PrayerCity("malang", "Malang", "Jawa Timur", -7.9666, 112.6326, "Asia/Jakarta"),
        PrayerCity("serang", "Serang", "Banten", -6.1104, 106.1503, "Asia/Jakarta"),
        PrayerCity("bandarlampung", "Bandar Lampung", "Lampung", -5.3971, 105.2668, "Asia/Jakarta"),
        PrayerCity("palembang", "Palembang", "Sumatera Selatan", -2.9761, 104.7754, "Asia/Jakarta"),
        PrayerCity("padang", "Padang", "Sumatera Barat", -0.9471, 100.4172, "Asia/Jakarta"),
        PrayerCity("pekanbaru", "Pekanbaru", "Riau", 0.5071, 101.4478, "Asia/Jakarta"),
        PrayerCity("medan", "Medan", "Sumatera Utara", 3.5952, 98.6722, "Asia/Jakarta"),
        PrayerCity("batam", "Batam", "Kepulauan Riau", 1.1300, 104.0529, "Asia/Jakarta"),
        PrayerCity("pontianak", "Pontianak", "Kalimantan Barat", -0.0263, 109.3425, "Asia/Pontianak"),
        PrayerCity("banjarmasin", "Banjarmasin", "Kalimantan Selatan", -3.3186, 114.5944, "Asia/Makassar"),
        PrayerCity("balikpapan", "Balikpapan", "Kalimantan Timur", -1.2379, 116.8529, "Asia/Makassar"),
        PrayerCity("samarinda", "Samarinda", "Kalimantan Timur", -0.5017, 117.1536, "Asia/Makassar"),
        PrayerCity("makassar", "Makassar", "Sulawesi Selatan", -5.1477, 119.4327, "Asia/Makassar"),
        PrayerCity("manado", "Manado", "Sulawesi Utara", 1.4748, 124.8421, "Asia/Makassar"),
        PrayerCity("palu", "Palu", "Sulawesi Tengah", -0.8917, 119.8707, "Asia/Makassar"),
        PrayerCity("kendari", "Kendari", "Sulawesi Tenggara", -3.9778, 122.5150, "Asia/Makassar"),
        PrayerCity("mataram", "Mataram", "Nusa Tenggara Barat", -8.5833, 116.1167, "Asia/Makassar"),
        PrayerCity("denpasar", "Denpasar", "Bali", -8.6705, 115.2126, "Asia/Makassar"),
        PrayerCity("kupang", "Kupang", "Nusa Tenggara Timur", -10.1772, 123.6070, "Asia/Makassar"),
        PrayerCity("ambon", "Ambon", "Maluku", -3.6547, 128.1900, "Asia/Jayapura"),
        PrayerCity("ternate", "Ternate", "Maluku Utara", 0.7900, 127.3840, "Asia/Jayapura"),
        PrayerCity("jayapura", "Jayapura", "Papua", -2.5916, 140.6690, "Asia/Jayapura"),
        PrayerCity("merauke", "Merauke", "Papua Selatan", -8.4932, 140.4018, "Asia/Jayapura")
    )

    val default: PrayerCity get() = all.first()

    fun byId(id: String): PrayerCity = all.firstOrNull { it.id == id } ?: default
}

object PrayerTimes {
    private const val KAABA_LAT = 21.4225
    private const val KAABA_LNG = 39.8262
    private const val SUN_HORIZON = -0.833

    fun schedule(
        city: PrayerCity,
        method: PrayerMethod,
        asrHanafi: Boolean = false,
        now: Long = System.currentTimeMillis()
    ): PrayerSchedule {
        val zone = TimeZone.getTimeZone(city.timeZoneId)
        val calendar = Calendar.getInstance(zone).apply { timeInMillis = now }
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val today = calculate(city, method, asrHanafi, year, month, day)
        val tomorrowCal = Calendar.getInstance(zone).apply {
            timeInMillis = now
            add(Calendar.DAY_OF_YEAR, 1)
        }
        val tomorrow = calculate(
            city, method, asrHanafi,
            tomorrowCal.get(Calendar.YEAR),
            tomorrowCal.get(Calendar.MONTH),
            tomorrowCal.get(Calendar.DAY_OF_MONTH)
        )
        return PrayerSchedule(
            times = today,
            tomorrowFajr = tomorrow.firstOrNull { it.prayer == Prayer.FAJR }?.millis
                ?: now + 24L * 60L * 60L * 1000L
        )
    }

    fun calculate(
        city: PrayerCity,
        method: PrayerMethod,
        asrHanafi: Boolean,
        year: Int,
        month: Int,
        day: Int
    ): List<PrayerTime> {
        val zone = TimeZone.getTimeZone(city.timeZoneId)
        val noonCalendar = Calendar.getInstance(zone).apply {
            clear()
            set(year, month, day, 12, 0, 0)
        }
        val dayOfYear = noonCalendar.get(Calendar.DAY_OF_YEAR)
        val gamma = 2.0 * PI / 365.0 * (dayOfYear - 1)
        val equationOfTime = 229.18 * (
            0.000075 + 0.001868 * cos(gamma) - 0.032077 * sin(gamma) -
                0.014615 * cos(2 * gamma) - 0.040849 * sin(2 * gamma)
            )
        val declination = 0.006918 - 0.399912 * cos(gamma) + 0.070257 * sin(gamma) -
            0.006758 * cos(2 * gamma) + 0.000907 * sin(2 * gamma) -
            0.002697 * cos(3 * gamma) + 0.00148 * sin(3 * gamma)
        val zoneMinutes = (
            noonCalendar.get(Calendar.ZONE_OFFSET) + noonCalendar.get(Calendar.DST_OFFSET)
            ) / 60_000
        val solarNoon = 720.0 - 4.0 * city.longitude - equationOfTime + zoneMinutes
        val latRad = Math.toRadians(city.latitude)
        val decl = declination

        fun hourAngle(altitude: Double): Double {
            val cosH = (sin(Math.toRadians(altitude)) - sin(latRad) * sin(decl)) /
                (cos(latRad) * cos(decl))
            if (cosH.isNaN() || cosH < -1.0 || cosH > 1.0) return Double.NaN
            return Math.toDegrees(acos(cosH))
        }

        fun clockAngle(altitude: Double, morning: Boolean): Double {
            val angle = hourAngle(altitude)
            if (angle.isNaN()) return Double.NaN
            return if (morning) solarNoon - angle * 4.0 else solarNoon + angle * 4.0
        }

        val shadowFactor = if (asrHanafi) 2.0 else 1.0
        val asrAltitude = Math.toDegrees(
            atan(1.0 / (shadowFactor + tan(abs(latRad - decl))))
        )
        val maghrib = clockAngle(SUN_HORIZON, morning = false)
        val isha = if (method.ishaMinutesAfterMaghrib != null) {
            if (maghrib.isNaN()) Double.NaN
            else maghrib + method.ishaMinutesAfterMaghrib
        } else {
            clockAngle(-method.ishaAngle, morning = false)
        }
        val values = listOf(
            Prayer.FAJR to clockAngle(-method.fajrAngle, morning = true),
            Prayer.SUNRISE to clockAngle(SUN_HORIZON, morning = true),
            Prayer.DHUHR to solarNoon + 0.5,
            Prayer.ASR to clockAngle(asrAltitude, morning = false),
            Prayer.MAGHRIB to maghrib,
            Prayer.ISHA to isha
        )
        return values.mapNotNull { (prayer, minutes) ->
            if (minutes.isNaN() || minutes <= 0 || minutes >= 24 * 60) null
            else PrayerTime(prayer, toMillis(zone, year, month, day, minutes))
        }
    }

    fun qiblaBearing(latitude: Double, longitude: Double): Double {
        val lat = Math.toRadians(latitude)
        val kaaba = Math.toRadians(KAABA_LAT)
        val deltaLng = Math.toRadians(KAABA_LNG - longitude)
        val y = sin(deltaLng)
        val x = cos(lat) * tan(kaaba) - sin(lat) * cos(deltaLng)
        return (Math.toDegrees(atan2(y, x)) + 360.0) % 360.0
    }

    fun formatClock(millis: Long, zoneId: String): String {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone(zoneId)).apply { timeInMillis = millis }
        return String.format(
            Locale.US,
            "%02d:%02d",
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE)
        )
    }

    fun countdownLabel(remainingMillis: Long): String {
        val totalMinutes = (remainingMillis.coerceAtLeast(0) / 60_000L)
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return when {
            hours > 0 && minutes > 0 -> "$hours jam $minutes menit"
            hours > 0 -> "$hours jam"
            minutes > 0 -> "$minutes menit"
            else -> "kurang dari semenit"
        }
    }

    private fun toMillis(
        zone: TimeZone,
        year: Int,
        month: Int,
        day: Int,
        minutes: Double
    ): Long {
        val calendar = Calendar.getInstance(zone).apply {
            clear()
            set(year, month, day, 0, 0, 0)
        }
        calendar.add(Calendar.MILLISECOND, (minutes * 60_000.0).roundToInt())
        return calendar.timeInMillis
    }
}
