package com.example.data

import com.example.model.DayPrayerSchedule
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.math.*

/**
 * Astronomical calculation engine for Islamic Prayer Times.
 * Works completely offline with high precision based on solar positioning.
 */
object OfflinePrayerCalculator {

    enum class CalculationMethod(
        val arabicTitle: String,
        val fajrAngle: Double,
        val ishaAngle: Double,
        val ishaIntervalMins: Int = 0 // If > 0, Isha is fixed minutes after Maghrib
    ) {
        UMM_AL_QURA("أم القرى (مكة المكرمة)", 18.5, 0.0, ishaIntervalMins = 90),
        EGYPTIAN("الهيئة العامة المصرية للمساحة", 19.5, 17.5),
        MWL("رابطة العالم الإسلامي", 18.0, 17.0),
        ISNA("الجمعية الإسلامية لأمريكا الشمالية (ISNA)", 15.0, 15.0),
        KARACHI("جامعة العلوم الإسلامية بكراتشي", 18.0, 18.0)
    }

    enum class AsrJuristic(val shadowFactor: Double) {
        STANDARD(1.0), // Shafi'i, Maliki, Hanbali
        HANAFI(2.0)
    }

    fun calculate(
        latitude: Double,
        longitude: Double,
        calendar: Calendar = Calendar.getInstance(),
        method: CalculationMethod = CalculationMethod.UMM_AL_QURA,
        asrJuristic: AsrJuristic = AsrJuristic.STANDARD,
        cityName: String = "موقعي الحالي"
    ): DayPrayerSchedule {
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val tzOffset = calendar.timeZone.getOffset(calendar.timeInMillis) / 3600000.0

        // Julian date
        val julianDate = julianDay(year, month, day) - (longitude / (15.0 * 24.0))

        // Solar parameters
        val d = julianDate - 2451545.0
        val g = fixAngle(357.529 + 0.98560028 * d)
        val q = fixAngle(280.459 + 0.98564736 * d)
        val l = fixAngle(q + 1.915 * sin(Math.toRadians(g)) + 0.020 * sin(Math.toRadians(2 * g)))
        val e = 23.439 - 0.00000036 * d

        val sinDec = sin(Math.toRadians(e)) * sin(Math.toRadians(l))
        val declination = Math.toDegrees(asin(sinDec))
        val ra = Math.toDegrees(atan2(cos(Math.toRadians(e)) * sin(Math.toRadians(l)), cos(Math.toRadians(l)))) / 15.0
        val fixRa = fixHour(ra)

        val eqt = (q / 15.0) - fixRa

        // Midday (Dhuhr)
        val dhuhrHours = 12.0 + tzOffset - (longitude / 15.0) - eqt

        // Sunrise & Sunset
        val sunriseAngle = 0.833
        val sunriseHoursDiff = sunAngleHourDiff(latitude, declination, -sunriseAngle)
        val sunriseHours = dhuhrHours - sunriseHoursDiff
        val sunsetHours = dhuhrHours + sunriseHoursDiff

        // Fajr
        val fajrHoursDiff = sunAngleHourDiff(latitude, declination, -method.fajrAngle)
        val fajrHours = dhuhrHours - fajrHoursDiff

        // Asr
        val asrAngle = -Math.toDegrees(atan(1.0 / (asrJuristic.shadowFactor + tan(Math.toRadians(abs(latitude - declination))))))
        val asrHoursDiff = sunAngleHourDiff(latitude, declination, asrAngle)
        val asrHours = dhuhrHours + asrHoursDiff

        // Maghrib (sunset + 2 min buffer)
        val maghribHours = sunsetHours + (2.0 / 60.0)

        // Isha
        val ishaHours = if (method.ishaIntervalMins > 0) {
            maghribHours + (method.ishaIntervalMins / 60.0)
        } else {
            val ishaDiff = sunAngleHourDiff(latitude, declination, -method.ishaAngle)
            dhuhrHours + ishaDiff
        }

        val gregorianFormatted = String.format(Locale.getDefault(), "%02d/%02d/%d", day, month, year)
        val hijriFormatted = approximateHijriDate(calendar)

        return DayPrayerSchedule(
            fajr = formatTime(fajrHours),
            sunrise = formatTime(sunriseHours),
            dhuhr = formatTime(dhuhrHours),
            asr = formatTime(asrHours),
            maghrib = formatTime(maghribHours),
            isha = formatTime(ishaHours),
            hijriDate = hijriFormatted,
            gregorianDate = gregorianFormatted,
            locationName = cityName,
            latitude = latitude,
            longitude = longitude
        )
    }

    private fun sunAngleHourDiff(latitude: Double, declination: Double, angle: Double): Double {
        val latRad = Math.toRadians(latitude)
        val decRad = Math.toRadians(declination)
        val angRad = Math.toRadians(angle)
        val top = sin(angRad) - sin(latRad) * sin(decRad)
        val bottom = cos(latRad) * cos(decRad)
        val cosH = (top / bottom).coerceIn(-1.0, 1.0)
        return Math.toDegrees(acos(cosH)) / 15.0
    }

    private fun julianDay(year: Int, month: Int, day: Int): Double {
        var y = year
        var m = month
        if (m <= 2) {
            y -= 1
            m += 12
        }
        val a = floor(y / 100.0)
        val b = 2 - a + floor(a / 4.0)
        return floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + day + b - 1524.5
    }

    private fun fixAngle(a: Double): Double {
        var res = a - 360.0 * floor(a / 360.0)
        if (res < 0) res += 360.0
        return res
    }

    private fun fixHour(a: Double): Double {
        var res = a - 24.0 * floor(a / 24.0)
        if (res < 0) res += 24.0
        return res
    }

    fun formatTime(decimalHour: Double): String {
        var h = decimalHour
        while (h < 0) h += 24.0
        while (h >= 24.0) h -= 24.0
        val totalMinutes = (h * 60).roundToInt()
        val hours = (totalMinutes / 60) % 24
        val minutes = totalMinutes % 60
        return String.format(Locale.getDefault(), "%02d:%02d", hours, minutes)
    }

    private fun approximateHijriDate(calendar: Calendar): String {
        // Kuwaiti algorithm approximation
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val month = calendar.get(Calendar.MONTH) // 0-based
        val year = calendar.get(Calendar.YEAR)

        var m = month + 1
        var y = year
        if (m < 3) {
            y -= 1
            m += 12
        }
        var a = floor(y / 100.0)
        var b = 2 - a + floor(a / 4.0)
        if (y < 1583) b = 0.0
        if (y == 1582) {
            if (m > 10) b = -10.0
            if (m == 10) {
                b = 0.0
                if (day > 4) b = -10.0
            }
        }
        val jd = floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + day + b - 1524
        val bFactor = jd - 1948440 + 10632
        val nFactor = floor((bFactor - 1) / 10631.0)
        val b2 = bFactor - 10631 * nFactor + 354
        val jFactor = (floor((10985 - b2) / 5316.0)) * (floor((50 * b2) / 17719.0)) +
                (floor(b2 / 5670.0)) * (floor((43 * b2) / 15238.0))
        val b3 = b2 - (floor((30 - jFactor) / 15.0)) * (floor((17719 * jFactor) / 50.0)) -
                (floor(jFactor / 16.0)) * (floor((15238 * jFactor) / 43.0)) + 29
        val mHijri = floor((24 * b3) / 709.0).toInt()
        val dHijri = (b3 - floor((709 * mHijri) / 24.0)).toInt()
        val yHijri = (30 * nFactor + jFactor - 30).toInt()

        val hijriMonths = listOf(
            "محرم", "صفر", "ربيع الأول", "ربيع الآخر",
            "جمادى الأولى", "جمادى الآخرة", "رجب", "شعبان",
            "رمضان", "شوال", "ذو القعدة", "ذو الحجة"
        )
        val monthName = hijriMonths.getOrElse((mHijri - 1).coerceIn(0, 11)) { "رمضان" }
        return "$dHijri $monthName $yHijri هـ"
    }
}
