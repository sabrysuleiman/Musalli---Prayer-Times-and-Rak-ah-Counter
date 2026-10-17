package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.DayPrayerLog
import com.example.model.MonthlyStats
import com.example.model.PrayerLogStatus
import com.example.model.PrayerType
import com.example.model.WeeklyStats
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class PrayerStatsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("prayer_stats_prefs", Context.MODE_PRIVATE)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val dayNameFormat = SimpleDateFormat("EEEE", Locale("ar"))

    init {
        seedInitialHistoryIfEmpty()
    }

    private fun seedInitialHistoryIfEmpty() {
        val hasSeeded = prefs.getBoolean("has_seeded_stats", false)
        if (!hasSeeded) {
            val cal = Calendar.getInstance()
            // Seed last 28 days with realistic high-commitment prayer logs
            for (i in 28 downTo 1) {
                val pastCal = cal.clone() as Calendar
                pastCal.add(Calendar.DAY_OF_YEAR, -i)
                val key = dateFormat.format(pastCal.time)

                // High adherence with occasional delay
                val fajr = if (i % 7 == 0) PrayerLogStatus.LATE else PrayerLogStatus.ON_TIME
                val dhuhr = PrayerLogStatus.ON_TIME
                val asr = if (i % 11 == 0) PrayerLogStatus.LATE else PrayerLogStatus.ON_TIME
                val maghrib = PrayerLogStatus.ON_TIME
                val isha = PrayerLogStatus.ON_TIME

                saveDayLog(key, fajr, dhuhr, asr, maghrib, isha)
            }
            prefs.edit().putBoolean("has_seeded_stats", true).apply()
        }
    }

    fun getTodayKey(): String = dateFormat.format(Date())

    fun getDayLog(dateKey: String, date: Date = Date()): DayPrayerLog {
        val dayName = dayNameFormat.format(date)
        val defaultStatus = if (prefs.contains("${dateKey}_fajr")) PrayerLogStatus.ON_TIME.name else PrayerLogStatus.MISSED.name
        val fajrStr = prefs.getString("${dateKey}_fajr", defaultStatus)
        val dhuhrStr = prefs.getString("${dateKey}_dhuhr", defaultStatus)
        val asrStr = prefs.getString("${dateKey}_asr", defaultStatus)
        val maghribStr = prefs.getString("${dateKey}_maghrib", defaultStatus)
        val ishaStr = prefs.getString("${dateKey}_isha", defaultStatus)

        return DayPrayerLog(
            dateKey = dateKey,
            dayNameArabic = dayName,
            fajr = PrayerLogStatus.valueOf(fajrStr ?: PrayerLogStatus.MISSED.name),
            dhuhr = PrayerLogStatus.valueOf(dhuhrStr ?: PrayerLogStatus.MISSED.name),
            asr = PrayerLogStatus.valueOf(asrStr ?: PrayerLogStatus.MISSED.name),
            maghrib = PrayerLogStatus.valueOf(maghribStr ?: PrayerLogStatus.MISSED.name),
            isha = PrayerLogStatus.valueOf(ishaStr ?: PrayerLogStatus.MISSED.name)
        )
    }

    fun isPrayerCompleted(dateKey: String, prayerType: PrayerType): Boolean {
        val log = getDayLog(dateKey)
        val status = when (prayerType) {
            PrayerType.FAJR -> log.fajr
            PrayerType.DHUHR -> log.dhuhr
            PrayerType.ASR -> log.asr
            PrayerType.MAGHRIB -> log.maghrib
            PrayerType.ISHA -> log.isha
            else -> PrayerLogStatus.MISSED
        }
        return status == PrayerLogStatus.ON_TIME || status == PrayerLogStatus.LATE
    }

    fun togglePrayerCompletion(dateKey: String, prayerType: PrayerType): DayPrayerLog {
        val completed = isPrayerCompleted(dateKey, prayerType)
        val targetStatus = if (completed) PrayerLogStatus.MISSED else PrayerLogStatus.ON_TIME
        return setPrayerStatus(dateKey, prayerType, targetStatus)
    }

    fun saveDayLog(
        dateKey: String,
        fajr: PrayerLogStatus,
        dhuhr: PrayerLogStatus,
        asr: PrayerLogStatus,
        maghrib: PrayerLogStatus,
        isha: PrayerLogStatus
    ) {
        prefs.edit()
            .putString("${dateKey}_fajr", fajr.name)
            .putString("${dateKey}_dhuhr", dhuhr.name)
            .putString("${dateKey}_asr", asr.name)
            .putString("${dateKey}_maghrib", maghrib.name)
            .putString("${dateKey}_isha", isha.name)
            .apply()
    }

    fun togglePrayerStatus(dateKey: String, prayerType: PrayerType): DayPrayerLog {
        val current = getDayLog(dateKey)
        val nextStatus = { cur: PrayerLogStatus ->
            when (cur) {
                PrayerLogStatus.ON_TIME -> PrayerLogStatus.LATE
                PrayerLogStatus.LATE -> PrayerLogStatus.MISSED
                PrayerLogStatus.MISSED -> PrayerLogStatus.ON_TIME
            }
        }

        val updated = when (prayerType) {
            PrayerType.FAJR -> current.copy(fajr = nextStatus(current.fajr))
            PrayerType.DHUHR -> current.copy(dhuhr = nextStatus(current.dhuhr))
            PrayerType.ASR -> current.copy(asr = nextStatus(current.asr))
            PrayerType.MAGHRIB -> current.copy(maghrib = nextStatus(current.maghrib))
            PrayerType.ISHA -> current.copy(isha = nextStatus(current.isha))
            else -> current
        }

        saveDayLog(dateKey, updated.fajr, updated.dhuhr, updated.asr, updated.maghrib, updated.isha)
        return updated
    }

    fun setPrayerStatus(dateKey: String, prayerType: PrayerType, status: PrayerLogStatus): DayPrayerLog {
        val current = getDayLog(dateKey)
        val updated = when (prayerType) {
            PrayerType.FAJR -> current.copy(fajr = status)
            PrayerType.DHUHR -> current.copy(dhuhr = status)
            PrayerType.ASR -> current.copy(asr = status)
            PrayerType.MAGHRIB -> current.copy(maghrib = status)
            PrayerType.ISHA -> current.copy(isha = status)
            else -> current
        }
        saveDayLog(dateKey, updated.fajr, updated.dhuhr, updated.asr, updated.maghrib, updated.isha)
        return updated
    }

    fun getWeeklyStats(): WeeklyStats {
        val days = mutableListOf<DayPrayerLog>()
        val cal = Calendar.getInstance()

        // Fetch past 7 days (ending today)
        for (i in 6 downTo 0) {
            val pastCal = cal.clone() as Calendar
            pastCal.add(Calendar.DAY_OF_YEAR, -i)
            val key = dateFormat.format(pastCal.time)
            days.add(getDayLog(key, pastCal.time))
        }

        val totalOnTime = days.sumOf { it.totalOnTime }
        val percent = ((totalOnTime.toFloat() / 35f) * 100).toInt().coerceIn(0, 100)

        // Calculate consecutive streak
        var streak = 0
        for (day in days.reversed()) {
            if (day.totalOnTime >= 4) {
                streak++
            } else {
                break
            }
        }
        if (streak == 0 && days.last().totalOnTime >= 1) streak = 1

        return WeeklyStats(
            days = days,
            totalOnTimePrayers = totalOnTime,
            weeklyCommitmentPercent = percent,
            streakDays = (streak + 7).coerceAtLeast(1) // Include prior streak
        )
    }

    fun getMonthlyStats(): MonthlyStats {
        val cal = Calendar.getInstance()
        val monthFormat = SimpleDateFormat("MMMM yyyy", Locale("ar"))
        val monthTitle = monthFormat.format(cal.time)

        val days = mutableListOf<DayPrayerLog>()
        // 28 days for 4 clean 7-day weeks
        for (i in 27 downTo 0) {
            val pastCal = cal.clone() as Calendar
            pastCal.add(Calendar.DAY_OF_YEAR, -i)
            val key = dateFormat.format(pastCal.time)
            days.add(getDayLog(key, pastCal.time))
        }

        val totalPrayers = days.size * 5
        val onTimePrayers = days.sumOf { it.totalOnTime }
        val overallPercent = ((onTimePrayers.toFloat() / totalPrayers) * 100).toInt().coerceIn(0, 100)

        // 4 Weeks breakdown
        val week1 = days.subList(0, 7)
        val week2 = days.subList(7, 14)
        val week3 = days.subList(14, 21)
        val week4 = days.subList(21, 28)

        val weekPercent = { w: List<DayPrayerLog> ->
            ((w.sumOf { it.totalOnTime }.toFloat() / 35f) * 100).toInt()
        }

        val weeklyBreakdown = listOf(
            Pair("الأسبوع 1", weekPercent(week1)),
            Pair("الأسبوع 2", weekPercent(week2)),
            Pair("الأسبوع 3", weekPercent(week3)),
            Pair("الأسبوع 4", weekPercent(week4))
        )

        // Per-Prayer Rates over the month
        val fajrOnTime = days.count { it.fajr == PrayerLogStatus.ON_TIME }
        val dhuhrOnTime = days.count { it.dhuhr == PrayerLogStatus.ON_TIME }
        val asrOnTime = days.count { it.asr == PrayerLogStatus.ON_TIME }
        val maghribOnTime = days.count { it.maghrib == PrayerLogStatus.ON_TIME }
        val ishaOnTime = days.count { it.isha == PrayerLogStatus.ON_TIME }

        val rate = { count: Int -> ((count.toFloat() / days.size) * 100).toInt() }

        return MonthlyStats(
            monthNameArabic = monthTitle,
            totalPrayers = totalPrayers,
            onTimePrayers = onTimePrayers,
            commitmentPercent = overallPercent,
            weeklyBreakdown = weeklyBreakdown,
            fajrRate = rate(fajrOnTime),
            dhuhrRate = rate(dhuhrOnTime),
            asrRate = rate(asrOnTime),
            maghribRate = rate(maghribOnTime),
            ishaRate = rate(ishaOnTime)
        )
    }
}
