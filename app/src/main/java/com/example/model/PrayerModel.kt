package com.example.model

enum class PrayerType(
    val id: String,
    val arabicName: String,
    val defaultRakahs: Int,
    val hasSunnahBefore: Int = 0,
    val hasSunnahAfter: Int = 0
) {
    FAJR("fajr", "الفجر", 2, hasSunnahBefore = 2),
    SUNRISE("sunrise", "الشروق", 0),
    DHUHR("dhuhr", "الظهر", 4, hasSunnahBefore = 4, hasSunnahAfter = 2),
    ASR("asr", "العصر", 4, hasSunnahBefore = 0),
    MAGHRIB("maghrib", "المغرب", 3, hasSunnahAfter = 2),
    ISHA("isha", "العشاء", 4, hasSunnahAfter = 2),
    JUMMAH("jummah", "الجمعة", 2, hasSunnahAfter = 2),
    WITR("witr", "الوتر", 3),
    QIYAM("qiyam", "قيام الليل", 2),
    TARAWEEH("taraweeh", "التراويح", 2),
    CUSTOM("custom", "صلاة نافلة / مخصصة", 2);

    companion object {
        fun fromId(id: String): PrayerType {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: FAJR
        }
    }
}

data class PrayerTiming(
    val type: PrayerType,
    val timeFormatted: String, // e.g. "05:12"
    val timestampMillis: Long,
    val isPassed: Boolean = false,
    val isCurrent: Boolean = false,
    val isNext: Boolean = false
)

data class DayPrayerSchedule(
    val fajr: String,
    val sunrise: String,
    val dhuhr: String,
    val asr: String,
    val maghrib: String,
    val isha: String,
    val hijriDate: String,
    val gregorianDate: String,
    val locationName: String,
    val latitude: Double,
    val longitude: Double
)

data class CityLocation(
    val nameArabic: String,
    val countryArabic: String,
    val latitude: Double,
    val longitude: Double,
    val timezone: String
)
