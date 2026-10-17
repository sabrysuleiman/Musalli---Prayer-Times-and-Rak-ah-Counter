package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.PrayerPreferences

class PrayerAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val prayerId = intent.getStringExtra(EXTRA_PRAYER_ID) ?: "fajr"
        val prayerName = intent.getStringExtra(EXTRA_PRAYER_NAME) ?: "الصلاة"
        val timeString = intent.getStringExtra(EXTRA_PRAYER_TIME) ?: ""

        val prefs = PrayerPreferences(context)
        val isDndActive = prefs.isDndCurrentlyActive()
        val canBypass = prefs.isPrayerBypassedInDnd(prayerId)

        if (isDndActive) {
            if (canBypass) {
                // Critical alert: Bypasses DND! (e.g. Fajr prayer)
                PrayerNotificationHelper.showPrayerNotification(
                    context = context,
                    prayerId = prayerId,
                    prayerNameArabic = prayerName,
                    timeString = timeString,
                    bypassedDnd = true,
                    wasSilencedByDnd = false
                )
            } else {
                // Silenced by DND
                PrayerNotificationHelper.showPrayerNotification(
                    context = context,
                    prayerId = prayerId,
                    prayerNameArabic = prayerName,
                    timeString = timeString,
                    bypassedDnd = false,
                    wasSilencedByDnd = true
                )
            }
        } else {
            // Normal Alert
            PrayerNotificationHelper.showPrayerNotification(
                context = context,
                prayerId = prayerId,
                prayerNameArabic = prayerName,
                timeString = timeString,
                bypassedDnd = false,
                wasSilencedByDnd = false
            )
        }
    }

    companion object {
        const val EXTRA_PRAYER_ID = "extra_prayer_id"
        const val EXTRA_PRAYER_NAME = "extra_prayer_name"
        const val EXTRA_PRAYER_TIME = "extra_prayer_time"
    }
}
