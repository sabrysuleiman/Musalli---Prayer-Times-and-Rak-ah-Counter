package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.PrayerPreferences
import com.example.data.PrayerRepository
import com.example.data.PrayerSoundManager
import com.example.model.CityLocation
import com.example.model.DayPrayerSchedule
import com.example.model.PrayerTiming
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale

data class NextPrayerCountdown(
    val prayerTitleArabic: String = "",
    val timeFormatted: String = "",
    val hoursRemaining: Long = 0,
    val minutesRemaining: Long = 0,
    val secondsRemaining: Long = 0,
    val progressFraction: Float = 0f
)

data class DndStatusState(
    val isActive: Boolean = false,
    val untilTimestamp: Long = 0L,
    val remainingFormatted: String = "",
    val bypassFajr: Boolean = true
)

class PrayerViewModel(application: Application) : AndroidViewModel(application) {

    val repository = PrayerRepository(application)
    val preferences: PrayerPreferences = repository.preferences

    val schedule: StateFlow<DayPrayerSchedule?> = repository.schedule
    val timingsList: StateFlow<List<PrayerTiming>> = repository.timingsList
    val nextPrayer: StateFlow<PrayerTiming?> = repository.nextPrayer
    val currentPrayer: StateFlow<PrayerTiming?> = repository.currentPrayer
    val isLoading: StateFlow<Boolean> = repository.isLoading

    private val _countdown = MutableStateFlow(NextPrayerCountdown())
    val countdown: StateFlow<NextPrayerCountdown> = _countdown.asStateFlow()

    private val _dndStatus = MutableStateFlow(DndStatusState())
    val dndStatus: StateFlow<DndStatusState> = _dndStatus.asStateFlow()

    private val _previewSoundId = MutableStateFlow<String?>(null)
    val previewSoundId: StateFlow<String?> = _previewSoundId.asStateFlow()

    init {
        // Initialize sounds generator asynchronously in background
        viewModelScope.launch {
            PrayerSoundManager.initializeSounds(application)
        }

        // Periodic ticker for prayer countdown and DND timer
        viewModelScope.launch {
            while (isActive) {
                updateCountdown()
                updateDndTick()
                delay(1000)
            }
        }
    }

    fun loadInitial(hasLocationPermission: Boolean) {
        viewModelScope.launch {
            repository.refreshWithCurrentLocation(hasLocationPermission)
        }
    }

    fun selectCity(city: CityLocation) {
        viewModelScope.launch {
            repository.setCity(city)
        }
    }

    fun updateCalculationMethod(methodIndex: Int) {
        preferences.calculationMethodIndex = methodIndex
        viewModelScope.launch {
            repository.loadTimings(
                preferences.selectedLatitude,
                preferences.selectedLongitude,
                preferences.selectedCityName
            )
        }
    }

    fun toggleNotification(prayerId: String, enabled: Boolean) {
        when (prayerId) {
            "fajr" -> preferences.notifyFajr = enabled
            "dhuhr" -> preferences.notifyDhuhr = enabled
            "asr" -> preferences.notifyAsr = enabled
            "maghrib" -> preferences.notifyMaghrib = enabled
            "isha" -> preferences.notifyIsha = enabled
        }
        val currentSchedule = repository.schedule.value
        if (currentSchedule != null) {
            viewModelScope.launch {
                repository.loadTimings(
                    currentSchedule.latitude,
                    currentSchedule.longitude,
                    currentSchedule.locationName
                )
            }
        }
    }

    // ==========================================
    // DND MODE ACTIONS
    // ==========================================
    fun activateDnd(durationMinutes: Int) {
        val untilMillis = System.currentTimeMillis() + (durationMinutes * 60 * 1000L)
        preferences.activateDnd(untilMillis)
        updateDndTick()
    }

    fun activateDndUntil(timestampMillis: Long) {
        preferences.activateDnd(timestampMillis)
        updateDndTick()
    }

    fun deactivateDnd() {
        preferences.deactivateDnd()
        updateDndTick()
    }

    fun setDndBypass(prayerId: String, bypass: Boolean) {
        when (prayerId.lowercase()) {
            "fajr" -> preferences.dndBypassFajr = bypass
            "dhuhr" -> preferences.dndBypassDhuhr = bypass
            "asr" -> preferences.dndBypassAsr = bypass
            "maghrib" -> preferences.dndBypassMaghrib = bypass
            "isha" -> preferences.dndBypassIsha = bypass
        }
        updateDndTick()
    }

    private fun updateDndTick() {
        val isActive = preferences.isDndCurrentlyActive()
        val until = preferences.dndUntilTimestamp
        val now = System.currentTimeMillis()

        val remainingStr = if (isActive && until > now) {
            val totalSec = (until - now) / 1000
            val hrs = totalSec / 3600
            val mins = (totalSec % 3600) / 60
            val secs = totalSec % 60
            if (hrs > 0) {
                String.format(Locale.getDefault(), "%02d:%02d:%02d", hrs, mins, secs)
            } else {
                String.format(Locale.getDefault(), "%02d:%02d", mins, secs)
            }
        } else {
            ""
        }

        _dndStatus.value = DndStatusState(
            isActive = isActive,
            untilTimestamp = until,
            remainingFormatted = remainingStr,
            bypassFajr = preferences.dndBypassFajr
        )
    }

    // ==========================================
    // SOUND CUSTOMIZATION ACTIONS
    // ==========================================
    fun setPrayerSound(prayerId: String, soundId: String) {
        preferences.setPrayerSound(prayerId, soundId)
    }

    fun setAllPrayersSound(soundId: String) {
        preferences.setAllPrayersSound(soundId)
    }

    fun getPrayerSound(prayerId: String): String {
        return preferences.getPrayerSound(prayerId)
    }

    fun playSoundPreview(soundId: String) {
        if (_previewSoundId.value == soundId) {
            stopSoundPreview()
        } else {
            _previewSoundId.value = soundId
            PrayerSoundManager.playPreview(getApplication(), soundId) {
                _previewSoundId.value = null
            }
        }
    }

    fun stopSoundPreview() {
        PrayerSoundManager.stopPreview()
        _previewSoundId.value = null
    }

    override fun onCleared() {
        super.onCleared()
        PrayerSoundManager.stopPreview()
    }

    private fun updateCountdown() {
        val next = repository.nextPrayer.value ?: return
        val current = repository.currentPrayer.value
        val now = System.currentTimeMillis()

        val diffMillis = next.timestampMillis - now
        if (diffMillis <= 0) {
            val s = repository.schedule.value
            if (s != null) {
                repository.updateCalculatedTimings(s)
            }
            return
        }

        val totalSeconds = diffMillis / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        val fraction = if (current != null && next.timestampMillis > current.timestampMillis) {
            val totalSpan = (next.timestampMillis - current.timestampMillis).toFloat()
            val elapsed = (now - current.timestampMillis).toFloat()
            (elapsed / totalSpan).coerceIn(0f, 1f)
        } else {
            0.5f
        }

        _countdown.value = NextPrayerCountdown(
            prayerTitleArabic = next.type.arabicName,
            timeFormatted = next.timeFormatted,
            hoursRemaining = hours,
            minutesRemaining = minutes,
            secondsRemaining = seconds,
            progressFraction = fraction
        )
    }
}
