package com.example.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.PrayerPreferences
import com.example.data.PrayerSoundManager
import java.io.File

object PrayerNotificationHelper {
    const val CHANNEL_ID = "salati_prayer_times_channel"
    const val CHANNEL_CRITICAL_ID = "salati_critical_fajr_channel"
    private const val CHANNEL_NAME = "مواقيت وتنبيهات الصلاة"
    private const val CHANNEL_CRITICAL_NAME = "تنبيهات صلاة الفجر والحرجة (تجاوز عدم الإزعاج)"

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val standardChannel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH).apply {
                description = "تنبيهات عند حلول وقت كل صلاة"
                enableVibration(true)
                enableLights(true)
            }

            val criticalChannel = NotificationChannel(CHANNEL_CRITICAL_ID, CHANNEL_CRITICAL_NAME, NotificationManager.IMPORTANCE_MAX).apply {
                description = "تنبيهات هامة للأذان تتجاوز وضع عدم الإزعاج"
                enableVibration(true)
                enableLights(true)
                setBypassDnd(true)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }

            manager.createNotificationChannel(standardChannel)
            manager.createNotificationChannel(criticalChannel)
        }
    }

    fun showPrayerNotification(
        context: Context,
        prayerId: String,
        prayerNameArabic: String,
        timeString: String,
        bypassedDnd: Boolean = false,
        wasSilencedByDnd: Boolean = false
    ) {
        createNotificationChannels(context)

        val prefs = PrayerPreferences(context)
        val soundId = prefs.getPrayerSound(prayerId)
        val soundItem = PrayerSoundManager.getSoundItem(soundId)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val targetChannelId = if (bypassedDnd) CHANNEL_CRITICAL_ID else CHANNEL_ID

        val title = when {
            wasSilencedByDnd -> "🌙 صلاة $prayerNameArabic ($timeString) [صامت - عدم الإزعاج]"
            bypassedDnd -> "⚡ حان موعد صلاة $prayerNameArabic ($timeString) [تنبيه حرج]"
            else -> "حان الآن موعد صلاة $prayerNameArabic ($timeString)"
        }

        val text = when {
            wasSilencedByDnd -> "تم كتم صوت التنبيه لأن وضع عدم الإزعاج نشط حالياً."
            bypassedDnd -> "تم تجاوز وضع عدم الإزعاج لضمان أداء الصلاة في وقتها المبارك."
            else -> "حيّ على الصلاة .. حيّ على الفلاح (${soundItem.nameArabic})"
        }

        val notificationBuilder = NotificationCompat.Builder(context, targetChannelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(if (bypassedDnd) NotificationCompat.PRIORITY_MAX else NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        if (!wasSilencedByDnd) {
            notificationBuilder.setVibrate(longArrayOf(0, 600, 200, 600, 200, 600))
            if (soundId != "silent_vibrate") {
                // Play custom synthesized audio alert
                PrayerSoundManager.playPreview(context, soundId)
            }
        }

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationId = (System.currentTimeMillis() % 10000).toInt()
        manager.notify(notificationId, notificationBuilder.build())
    }
}
