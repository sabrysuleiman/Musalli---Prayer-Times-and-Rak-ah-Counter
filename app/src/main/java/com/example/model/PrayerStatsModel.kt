package com.example.model

enum class PrayerLogStatus {
    ON_TIME,   // أديت في وقتها
    LATE,      // قضاء / متأخرة
    MISSED     // لم تسجل
}

data class DayPrayerLog(
    val dateKey: String, // yyyy-MM-dd
    val dayNameArabic: String, // السبت، الأحد...
    val fajr: PrayerLogStatus = PrayerLogStatus.ON_TIME,
    val dhuhr: PrayerLogStatus = PrayerLogStatus.ON_TIME,
    val asr: PrayerLogStatus = PrayerLogStatus.ON_TIME,
    val maghrib: PrayerLogStatus = PrayerLogStatus.ON_TIME,
    val isha: PrayerLogStatus = PrayerLogStatus.ON_TIME
) {
    val totalOnTime: Int
        get() = listOf(fajr, dhuhr, asr, maghrib, isha).count { it == PrayerLogStatus.ON_TIME }

    val completionRate: Float
        get() = totalOnTime / 5f

    val isFullDayCompleted: Boolean
        get() = totalOnTime == 5
}

data class WeeklyStats(
    val days: List<DayPrayerLog>,
    val totalOnTimePrayers: Int,
    val totalPossiblePrayers: Int = 35,
    val weeklyCommitmentPercent: Int,
    val streakDays: Int
)

data class MonthlyStats(
    val monthNameArabic: String,
    val totalPrayers: Int,
    val onTimePrayers: Int,
    val commitmentPercent: Int,
    val weeklyBreakdown: List<Pair<String, Int>>, // Week label to percent
    val fajrRate: Int,
    val dhuhrRate: Int,
    val asrRate: Int,
    val maghribRate: Int,
    val ishaRate: Int
)
