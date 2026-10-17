package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import com.example.data.PrayerPreferences
import com.example.model.CountingMode
import com.example.model.PrayerType
import com.example.model.RakahSessionState
import com.example.model.RakahStepSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class RakahViewModel(application: Application) : AndroidViewModel(application) {

    private val preferences = PrayerPreferences(application)
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = application.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vibratorManager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        application.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    private val _state = MutableStateFlow(
        RakahSessionState(
            countingMode = if (preferences.counterSajdahMode) CountingMode.SAJDAH_ASSIST else CountingMode.RAKAH_DIRECT
        )
    )
    val state: StateFlow<RakahSessionState> = _state.asStateFlow()

    fun selectPrayer(prayer: PrayerType) {
        if (_state.value.isSessionActive) return // Don't change prayer during active prayer
        val count = when (prayer) {
            PrayerType.FAJR -> 2
            PrayerType.DHUHR -> 4
            PrayerType.ASR -> 4
            PrayerType.MAGHRIB -> 3
            PrayerType.ISHA -> 4
            PrayerType.JUMMAH -> 2
            PrayerType.WITR -> 3
            PrayerType.QIYAM -> 2
            PrayerType.TARAWEEH -> 2
            PrayerType.CUSTOM -> _state.value.targetRakahs
            else -> 2
        }
        _state.value = _state.value.copy(
            selectedPrayer = prayer,
            targetRakahs = count,
            currentRakah = 1,
            currentSajdah = 0,
            isSessionCompleted = false,
            guidanceMessage = "جاهز لصلاة ${prayer.arabicName} ($count ركعات). اضغط 'ابدأ الصلاة'"
        )
    }

    fun setCustomRakahs(rakahs: Int) {
        if (_state.value.isSessionActive) return
        val clamped = rakahs.coerceIn(1, 20)
        _state.value = _state.value.copy(
            targetRakahs = clamped,
            selectedPrayer = PrayerType.CUSTOM,
            currentRakah = 1,
            currentSajdah = 0,
            isSessionCompleted = false,
            guidanceMessage = "جاهز لصلاة نافلة ($clamped ركعات)"
        )
    }

    fun setCountingMode(mode: CountingMode) {
        preferences.counterSajdahMode = (mode == CountingMode.SAJDAH_ASSIST)
        _state.value = _state.value.copy(countingMode = mode)
    }

    fun startPrayer() {
        _state.value = _state.value.copy(
            isSessionActive = true,
            isSessionCompleted = false,
            currentRakah = 1,
            currentSajdah = 0,
            guidanceMessage = if (_state.value.countingMode == CountingMode.SAJDAH_ASSIST) {
                "أنت الآن في الركعة الأولى. المس الشاشة عند السجود الأول."
            } else {
                "أنت الآن في الركعة الأولى. المس الشاشة عند إتمام الركعة."
            },
            promptTashahhud = false,
            historyStack = emptyList()
        )
        triggerVibration(longArrayOf(0, 100))
    }

    /**
     * Primary action triggered when the worshiper touches the screen anywhere!
     */
    fun onScreenTapped() {
        val current = _state.value
        if (!current.isSessionActive || current.isSessionCompleted) return

        // Push current step to history for undo
        val snapshot = RakahStepSnapshot(
            currentRakah = current.currentRakah,
            currentSajdah = current.currentSajdah,
            note = current.guidanceMessage
        )
        val newHistory = current.historyStack + snapshot

        if (current.countingMode == CountingMode.SAJDAH_ASSIST) {
            handleSajdahTap(current, newHistory)
        } else {
            handleRakahTap(current, newHistory)
        }
    }

    private fun handleSajdahTap(current: RakahSessionState, history: List<RakahStepSnapshot>) {
        when (current.currentSajdah) {
            0 -> {
                // First Sajdah
                triggerVibration(longArrayOf(0, 150))
                _state.value = current.copy(
                    currentSajdah = 1,
                    promptTashahhud = false,
                    guidanceMessage = "السجدة الأولى من الركعة ${current.currentRakah}. المس الشاشة عند السجدة الثانية.",
                    historyStack = history
                )
            }
            1 -> {
                // Second Sajdah completed!
                val completedRakah = current.currentRakah
                val isFinalRakah = completedRakah >= current.targetRakahs
                val needsFirstTashahhud = completedRakah == 2 && current.targetRakahs > 2

                if (isFinalRakah) {
                    // Prayer completed!
                    triggerVibration(longArrayOf(0, 200, 150, 250))
                    preferences.incrementCompletedPrayers()
                    _state.value = current.copy(
                        currentSajdah = 2,
                        isSessionActive = false,
                        isSessionCompleted = true,
                        promptTashahhud = true,
                        guidanceMessage = "ما شاء الله! تمت جميع الركعات (${current.targetRakahs} ركعات). اجلس للتشهد الأخير والتسليم.",
                        historyStack = history
                    )
                } else {
                    // Advance to next rakah
                    triggerVibration(longArrayOf(0, 150, 80, 150))
                    val nextRakah = completedRakah + 1
                    val msg = if (needsFirstTashahhud) {
                        "تمت الركعة 2. اجلس للتشهد الأول، ثم قف للركعة 3 والمس عند السجود."
                    } else {
                        "تمت الركعة $completedRakah. قف للركعة $nextRakah والمس الشاشة عند السجدة الأولى."
                    }
                    _state.value = current.copy(
                        currentRakah = nextRakah,
                        currentSajdah = 0,
                        promptTashahhud = needsFirstTashahhud,
                        guidanceMessage = msg,
                        historyStack = history
                    )
                }
            }
            2 -> {
                // In case it was at state 2
                val nextRakah = (current.currentRakah + 1).coerceAtMost(current.targetRakahs)
                _state.value = current.copy(
                    currentRakah = nextRakah,
                    currentSajdah = 0,
                    historyStack = history
                )
            }
        }
    }

    private fun handleRakahTap(current: RakahSessionState, history: List<RakahStepSnapshot>) {
        val completedRakah = current.currentRakah
        val isFinal = completedRakah >= current.targetRakahs
        val needsMiddleTashahhud = completedRakah == 2 && current.targetRakahs > 2

        if (isFinal) {
            triggerVibration(longArrayOf(0, 200, 150, 250))
            preferences.incrementCompletedPrayers()
            _state.value = current.copy(
                isSessionActive = false,
                isSessionCompleted = true,
                promptTashahhud = true,
                guidanceMessage = "ما شاء الله! تمت الصلاة كاملاً (${current.targetRakahs} ركعات). تقبل الله طاعتكم.",
                historyStack = history
            )
        } else {
            triggerVibration(longArrayOf(0, 150))
            val nextRakah = completedRakah + 1
            val msg = if (needsMiddleTashahhud) {
                "الركعة 2 تمت. الجلوس للتشهد الأول، ثم الركعة 3."
            } else {
                "الركعة $nextRakah من ${current.targetRakahs}. المس عند إتمامها."
            }
            _state.value = current.copy(
                currentRakah = nextRakah,
                promptTashahhud = needsMiddleTashahhud,
                guidanceMessage = msg,
                historyStack = history
            )
        }
    }

    fun undo() {
        val current = _state.value
        val history = current.historyStack
        if (history.isEmpty()) return

        val lastSnapshot = history.last()
        _state.value = current.copy(
            currentRakah = lastSnapshot.currentRakah,
            currentSajdah = lastSnapshot.currentSajdah,
            guidanceMessage = lastSnapshot.note,
            isSessionCompleted = false,
            isSessionActive = true,
            historyStack = history.dropLast(1)
        )
        triggerVibration(longArrayOf(0, 80))
    }

    fun resetPrayer() {
        val prayer = _state.value.selectedPrayer
        selectPrayer(prayer)
    }

    fun finishPrayerEarly() {
        val current = _state.value
        preferences.incrementCompletedPrayers()
        _state.value = current.copy(
            isSessionActive = false,
            isSessionCompleted = true,
            guidanceMessage = "تم إنهاء الصلاة. تقبل الله طاعتكم."
        )
    }

    private fun triggerVibration(timings: LongArray) {
        if (!preferences.counterHapticEnabled || vibrator == null || !vibrator.hasVibrator()) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(timings, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(timings, -1)
            }
        } catch (_: Exception) {}
    }
}
