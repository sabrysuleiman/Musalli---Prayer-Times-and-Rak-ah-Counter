package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.QuranWardProgress
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class QuranPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("quran_ward_prefs", Context.MODE_PRIVATE)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    var currentPage: Int
        get() = prefs.getInt("current_page", 1)
        set(value) = prefs.edit().putInt("current_page", value.coerceIn(1, 604)).apply()

    var dailyGoalPages: Int
        get() = prefs.getInt("daily_goal_pages", 4)
        set(value) = prefs.edit().putInt("daily_goal_pages", value.coerceIn(1, 50)).apply()

    var pagesReadToday: Int
        get() {
            checkAndResetDailyProgress()
            return prefs.getInt("pages_read_today", 0)
        }
        set(value) {
            checkAndResetDailyProgress()
            prefs.edit().putInt("pages_read_today", value.coerceAtLeast(0)).apply()
        }

    var readingStreakDays: Int
        get() = prefs.getInt("reading_streak_days", 1)
        set(value) = prefs.edit().putInt("reading_streak_days", value.coerceAtLeast(0)).apply()

    var bookmarkPage: Int
        get() = prefs.getInt("bookmark_page", 1)
        set(value) = prefs.edit().putInt("bookmark_page", value.coerceIn(1, 604)).apply()

    var bookmarkSurahName: String
        get() = prefs.getString("bookmark_surah", "الفاتحة") ?: "الفاتحة"
        set(value) = prefs.edit().putString("bookmark_surah", value).apply()

    var completedKhatmahs: Int
        get() = prefs.getInt("completed_khatmahs", 0)
        set(value) = prefs.edit().putInt("completed_khatmahs", value.coerceAtLeast(0)).apply()

    private fun checkAndResetDailyProgress() {
        val todayStr = dateFormat.format(Date())
        val lastDate = prefs.getString("last_reading_date", "") ?: ""

        if (lastDate != todayStr) {
            // New day
            val yesterdayMillis = System.currentTimeMillis() - 24 * 3600 * 1000L
            val yesterdayStr = dateFormat.format(Date(yesterdayMillis))

            val currentStreak = prefs.getInt("reading_streak_days", 1)
            val newStreak = if (lastDate == yesterdayStr) currentStreak else 1

            prefs.edit()
                .putString("last_reading_date", todayStr)
                .putInt("pages_read_today", 0)
                .putInt("reading_streak_days", newStreak)
                .apply()
        }
    }

    fun getProgress(): QuranWardProgress {
        return QuranWardProgress(
            currentPage = currentPage,
            dailyGoalPages = dailyGoalPages,
            pagesReadToday = pagesReadToday,
            streakDays = readingStreakDays,
            bookmarkPage = bookmarkPage,
            bookmarkSurah = bookmarkSurahName,
            completedKhatmahs = completedKhatmahs
        )
    }

    fun markTodayCompleted() {
        val currentGoal = dailyGoalPages
        pagesReadToday = currentGoal
        val nextStart = (currentPage + currentGoal).coerceIn(1, 604)
        currentPage = nextStart

        if (nextStart >= 604) {
            completedKhatmahs = completedKhatmahs + 1
            currentPage = 1
        }
    }
}
