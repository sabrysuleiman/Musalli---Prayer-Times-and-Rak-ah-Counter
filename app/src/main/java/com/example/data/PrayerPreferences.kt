package com.example.data

import android.content.Context
import android.content.SharedPreferences

class PrayerPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("salati_prefs", Context.MODE_PRIVATE)

    var isGpsEnabled: Boolean
        get() = prefs.getBoolean("is_gps_enabled", true)
        set(value) = prefs.edit().putBoolean("is_gps_enabled", value).apply()

    var selectedCityName: String
        get() = prefs.getString("selected_city_name", "مكة المكرمة") ?: "مكة المكرمة"
        set(value) = prefs.edit().putString("selected_city_name", value).apply()

    var selectedLatitude: Double
        get() = prefs.getString("selected_lat", "21.4225")?.toDoubleOrNull() ?: 21.4225
        set(value) = prefs.edit().putString("selected_lat", value.toString()).apply()

    var selectedLongitude: Double
        get() = prefs.getString("selected_lng", "39.8262")?.toDoubleOrNull() ?: 39.8262
        set(value) = prefs.edit().putString("selected_lng", value.toString()).apply()

    var calculationMethodIndex: Int
        get() = prefs.getInt("calc_method", 0) // 0: Umm Al-Qura, 1: Egyptian, 2: MWL, 3: ISNA, 4: Karachi
        set(value) = prefs.edit().putInt("calc_method", value).apply()

    // Notification Toggles
    var notifyFajr: Boolean
        get() = prefs.getBoolean("notify_fajr", true)
        set(value) = prefs.edit().putBoolean("notify_fajr", value).apply()

    var notifyDhuhr: Boolean
        get() = prefs.getBoolean("notify_dhuhr", true)
        set(value) = prefs.edit().putBoolean("notify_dhuhr", value).apply()

    var notifyAsr: Boolean
        get() = prefs.getBoolean("notify_asr", true)
        set(value) = prefs.edit().putBoolean("notify_asr", value).apply()

    var notifyMaghrib: Boolean
        get() = prefs.getBoolean("notify_maghrib", true)
        set(value) = prefs.edit().putBoolean("notify_maghrib", value).apply()

    var notifyIsha: Boolean
        get() = prefs.getBoolean("notify_isha", true)
        set(value) = prefs.edit().putBoolean("notify_isha", value).apply()

    // Rakah Counter Preferences
    var counterHapticEnabled: Boolean
        get() = prefs.getBoolean("counter_haptic", true)
        set(value) = prefs.edit().putBoolean("counter_haptic", value).apply()

    var counterSajdahMode: Boolean
        get() = prefs.getBoolean("counter_sajdah_mode", true) // Track both sajdahs and rakahs
        set(value) = prefs.edit().putBoolean("counter_sajdah_mode", value).apply()

    var completedPrayersCount: Int
        get() = prefs.getInt("completed_prayers_count", 0)
        set(value) = prefs.edit().putInt("completed_prayers_count", value).apply()

    fun incrementCompletedPrayers() {
        completedPrayersCount = completedPrayersCount + 1
    }

    // ==========================================
    // DO NOT DISTURB (DND) MODE
    // ==========================================
    var isDndActive: Boolean
        get() = prefs.getBoolean("is_dnd_active", false)
        set(value) = prefs.edit().putBoolean("is_dnd_active", value).apply()

    var dndUntilTimestamp: Long
        get() = prefs.getLong("dnd_until_timestamp", 0L)
        set(value) = prefs.edit().putLong("dnd_until_timestamp", value).apply()

    // Critical Alert Bypass Settings (Fajr defaults to true)
    var dndBypassFajr: Boolean
        get() = prefs.getBoolean("dnd_bypass_fajr", true)
        set(value) = prefs.edit().putBoolean("dnd_bypass_fajr", value).apply()

    var dndBypassDhuhr: Boolean
        get() = prefs.getBoolean("dnd_bypass_dhuhr", false)
        set(value) = prefs.edit().putBoolean("dnd_bypass_dhuhr", value).apply()

    var dndBypassAsr: Boolean
        get() = prefs.getBoolean("dnd_bypass_asr", false)
        set(value) = prefs.edit().putBoolean("dnd_bypass_asr", value).apply()

    var dndBypassMaghrib: Boolean
        get() = prefs.getBoolean("dnd_bypass_maghrib", false)
        set(value) = prefs.edit().putBoolean("dnd_bypass_maghrib", value).apply()

    var dndBypassIsha: Boolean
        get() = prefs.getBoolean("dnd_bypass_isha", false)
        set(value) = prefs.edit().putBoolean("dnd_bypass_isha", value).apply()

    fun isDndCurrentlyActive(): Boolean {
        if (!isDndActive) return false
        val now = System.currentTimeMillis()
        if (dndUntilTimestamp > 0 && now >= dndUntilTimestamp) {
            // Expired
            isDndActive = false
            return false
        }
        return true
    }

    fun isPrayerBypassedInDnd(prayerId: String): Boolean {
        return when (prayerId.lowercase()) {
            "fajr" -> dndBypassFajr
            "dhuhr" -> dndBypassDhuhr
            "asr" -> dndBypassAsr
            "maghrib" -> dndBypassMaghrib
            "isha" -> dndBypassIsha
            else -> false
        }
    }

    fun activateDnd(untilMillis: Long) {
        isDndActive = true
        dndUntilTimestamp = untilMillis
    }

    fun deactivateDnd() {
        isDndActive = false
        dndUntilTimestamp = 0L
    }

    // ==========================================
    // CUSTOMIZABLE ALERT SOUNDS PER PRAYER
    // ==========================================
    fun getPrayerSound(prayerId: String): String {
        val defaultSound = when (prayerId.lowercase()) {
            "fajr" -> "fajr_special"
            "dhuhr" -> "adhan_makkah"
            "asr" -> "adhan_madinah"
            "maghrib" -> "adhan_aqsa"
            "isha" -> "adhan_makkah"
            else -> "adhan_makkah"
        }
        return prefs.getString("sound_for_$prayerId", defaultSound) ?: defaultSound
    }

    fun setPrayerSound(prayerId: String, soundId: String) {
        prefs.edit().putString("sound_for_$prayerId", soundId).apply()
    }

    fun setAllPrayersSound(soundId: String) {
        prefs.edit()
            .putString("sound_for_fajr", soundId)
            .putString("sound_for_dhuhr", soundId)
            .putString("sound_for_asr", soundId)
            .putString("sound_for_maghrib", soundId)
            .putString("sound_for_isha", soundId)
            .apply()
    }
}
