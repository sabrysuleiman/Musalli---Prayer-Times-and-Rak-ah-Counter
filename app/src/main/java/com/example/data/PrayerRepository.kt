package com.example.data

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
import android.util.Log
import com.example.model.CityLocation
import com.example.model.DayPrayerSchedule
import com.example.model.PopularCities
import com.example.model.PrayerTiming
import com.example.model.PrayerType
import com.example.receiver.PrayerAlarmReceiver
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class PrayerRepository(private val context: Context) {
    private val api = AladhanApiService.create()
    val preferences = PrayerPreferences(context)
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private val _schedule = MutableStateFlow<DayPrayerSchedule?>(null)
    val schedule: StateFlow<DayPrayerSchedule?> = _schedule.asStateFlow()

    private val _timingsList = MutableStateFlow<List<PrayerTiming>>(emptyList())
    val timingsList: StateFlow<List<PrayerTiming>> = _timingsList.asStateFlow()

    private val _nextPrayer = MutableStateFlow<PrayerTiming?>(null)
    val nextPrayer: StateFlow<PrayerTiming?> = _nextPrayer.asStateFlow()

    private val _currentPrayer = MutableStateFlow<PrayerTiming?>(null)
    val currentPrayer: StateFlow<PrayerTiming?> = _currentPrayer.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    @SuppressLint("MissingPermission")
    suspend fun refreshWithCurrentLocation(hasPermission: Boolean) {
        _isLoading.value = true
        _errorMessage.value = null

        var lat = preferences.selectedLatitude
        var lng = preferences.selectedLongitude
        var locationTitle = preferences.selectedCityName

        if (hasPermission && preferences.isGpsEnabled) {
            try {
                val loc: Location? = withContext(Dispatchers.IO) {
                    kotlin.coroutines.suspendCoroutine { cont ->
                        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
                            .addOnSuccessListener { cont.resumeWith(Result.success(it)) }
                            .addOnFailureListener { cont.resumeWith(Result.success(null)) }
                    }
                }
                if (loc != null) {
                    lat = loc.latitude
                    lng = loc.longitude
                    locationTitle = "الموقع الحالي"
                    preferences.selectedLatitude = lat
                    preferences.selectedLongitude = lng
                }
            } catch (e: Exception) {
                Log.w("PrayerRepo", "Location fetch failed, using stored location", e)
            }
        }

        loadTimings(lat, lng, locationTitle)
    }

    suspend fun setCity(city: CityLocation) {
        preferences.isGpsEnabled = false
        preferences.selectedCityName = city.nameArabic
        preferences.selectedLatitude = city.latitude
        preferences.selectedLongitude = city.longitude
        loadTimings(city.latitude, city.longitude, city.nameArabic)
    }

    suspend fun loadTimings(lat: Double, lng: Double, locationTitle: String) = withContext(Dispatchers.IO) {
        _isLoading.value = true
        val methodCode = when (preferences.calculationMethodIndex) {
            0 -> 4 // Umm Al-Qura
            1 -> 5 // Egyptian General Authority
            2 -> 3 // MWL
            3 -> 2 // ISNA
            else -> 1 // Karachi
        }

        val offlineMethod = when (preferences.calculationMethodIndex) {
            0 -> OfflinePrayerCalculator.CalculationMethod.UMM_AL_QURA
            1 -> OfflinePrayerCalculator.CalculationMethod.EGYPTIAN
            2 -> OfflinePrayerCalculator.CalculationMethod.MWL
            3 -> OfflinePrayerCalculator.CalculationMethod.ISNA
            else -> OfflinePrayerCalculator.CalculationMethod.KARACHI
        }

        var loadedSchedule: DayPrayerSchedule? = null

        // 1. Attempt Aladhan API (free, reliable online service)
        try {
            val epochSeconds = System.currentTimeMillis() / 1000
            val response = api.getTimings(epochSeconds, lat, lng, methodCode)
            val timings = response.data?.timings
            val hijri = response.data?.date?.hijri
            val hijriFormatted = if (hijri != null) {
                "${hijri.day ?: ""} ${hijri.month?.ar ?: hijri.month?.en ?: ""} ${hijri.year ?: ""} هـ"
            } else {
                null
            }

            if (timings != null && timings.fajr != null) {
                val cleanTime = { raw: String? ->
                    raw?.split(" ")?.firstOrNull() ?: "00:00"
                }
                val gregorianDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())

                loadedSchedule = DayPrayerSchedule(
                    fajr = cleanTime(timings.fajr),
                    sunrise = cleanTime(timings.sunrise),
                    dhuhr = cleanTime(timings.dhuhr),
                    asr = cleanTime(timings.asr),
                    maghrib = cleanTime(timings.maghrib),
                    isha = cleanTime(timings.isha),
                    hijriDate = hijriFormatted ?: OfflinePrayerCalculator.calculate(lat, lng, cityName = locationTitle).hijriDate,
                    gregorianDate = gregorianDate,
                    locationName = locationTitle,
                    latitude = lat,
                    longitude = lng
                )
            }
        } catch (e: Exception) {
            Log.w("PrayerRepo", "Online API failed or offline, falling back to astronomical calculation engine", e)
        }

        // 2. Fallback to offline astronomical calculation engine if API was unavailable
        if (loadedSchedule == null) {
            loadedSchedule = OfflinePrayerCalculator.calculate(
                latitude = lat,
                longitude = lng,
                method = offlineMethod,
                cityName = locationTitle
            )
        }

        _schedule.value = loadedSchedule
        updateCalculatedTimings(loadedSchedule)
        schedulePrayerAlarms(loadedSchedule)
        _isLoading.value = false
    }

    fun updateCalculatedTimings(schedule: DayPrayerSchedule) {
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance()

        fun parseMillis(timeStr: String): Long {
            val parts = timeStr.split(":")
            val h = parts.getOrNull(0)?.toIntOrNull() ?: 0
            val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
            val c = calendar.clone() as Calendar
            c.set(Calendar.HOUR_OF_DAY, h)
            c.set(Calendar.MINUTE, m)
            c.set(Calendar.SECOND, 0)
            c.set(Calendar.MILLISECOND, 0)
            return c.timeInMillis
        }

        val rawList = listOf(
            PrayerTiming(PrayerType.FAJR, schedule.fajr, parseMillis(schedule.fajr)),
            PrayerTiming(PrayerType.SUNRISE, schedule.sunrise, parseMillis(schedule.sunrise)),
            PrayerTiming(PrayerType.DHUHR, schedule.dhuhr, parseMillis(schedule.dhuhr)),
            PrayerTiming(PrayerType.ASR, schedule.asr, parseMillis(schedule.asr)),
            PrayerTiming(PrayerType.MAGHRIB, schedule.maghrib, parseMillis(schedule.maghrib)),
            PrayerTiming(PrayerType.ISHA, schedule.isha, parseMillis(schedule.isha))
        )

        // Find next prayer and current prayer
        var nextFound: PrayerTiming? = null
        var currentFound: PrayerTiming? = null

        for (i in rawList.indices) {
            val item = rawList[i]
            if (item.timestampMillis > now) {
                if (nextFound == null) {
                    nextFound = item
                    if (i > 0) {
                        currentFound = rawList[i - 1]
                    }
                }
            }
        }

        // If all prayers today have passed, next is tomorrow's Fajr, current is Isha
        if (nextFound == null) {
            val tomorrowFajrMillis = rawList.first().timestampMillis + 24 * 3600 * 1000
            nextFound = rawList.first().copy(timestampMillis = tomorrowFajrMillis)
            currentFound = rawList.last()
        } else if (currentFound == null) {
            // It's before Fajr, current prayer is yesterday's Isha
            currentFound = rawList.last().copy(timestampMillis = rawList.last().timestampMillis - 24 * 3600 * 1000)
        }

        val updatedList = rawList.map { item ->
            val isPassed = item.timestampMillis < now
            val isNext = item.type == nextFound?.type && (nextFound.timestampMillis - now <= 24 * 3600 * 1000)
            val isCurrent = item.type == currentFound?.type
            item.copy(isPassed = isPassed, isNext = isNext, isCurrent = isCurrent)
        }

        _timingsList.value = updatedList
        _nextPrayer.value = nextFound
        _currentPrayer.value = currentFound
    }

    private fun schedulePrayerAlarms(schedule: DayPrayerSchedule) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val now = System.currentTimeMillis()

        val prayersToSchedule = listOf(
            Triple(PrayerType.FAJR, schedule.fajr, preferences.notifyFajr),
            Triple(PrayerType.DHUHR, schedule.dhuhr, preferences.notifyDhuhr),
            Triple(PrayerType.ASR, schedule.asr, preferences.notifyAsr),
            Triple(PrayerType.MAGHRIB, schedule.maghrib, preferences.notifyMaghrib),
            Triple(PrayerType.ISHA, schedule.isha, preferences.notifyIsha)
        )

        val cal = Calendar.getInstance()
        for ((type, timeStr, enabled) in prayersToSchedule) {
            if (!enabled) continue
            val parts = timeStr.split(":")
            val h = parts.getOrNull(0)?.toIntOrNull() ?: continue
            val m = parts.getOrNull(1)?.toIntOrNull() ?: continue

            cal.set(Calendar.HOUR_OF_DAY, h)
            cal.set(Calendar.MINUTE, m)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)

            var triggerAt = cal.timeInMillis
            if (triggerAt < now) {
                // Schedule for tomorrow if today's time has already passed
                triggerAt += 24 * 3600 * 1000
            }

            val intent = Intent(context, PrayerAlarmReceiver::class.java).apply {
                putExtra(PrayerAlarmReceiver.EXTRA_PRAYER_ID, type.id)
                putExtra(PrayerAlarmReceiver.EXTRA_PRAYER_NAME, type.arabicName)
                putExtra(PrayerAlarmReceiver.EXTRA_PRAYER_TIME, timeStr)
            }
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
            val pendingIntent = PendingIntent.getBroadcast(context, type.ordinal, intent, flags)

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
                } else {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
                }
            } catch (e: SecurityException) {
                Log.w("PrayerRepo", "Exact alarm permission not granted", e)
            }
        }
    }
}
