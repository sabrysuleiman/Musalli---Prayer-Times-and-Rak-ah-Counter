package com.example.data

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.*

data class PrayerSoundItem(
    val id: String,
    val nameArabic: String,
    val descArabic: String,
    val durationSeconds: Int,
    val isCriticalRecommended: Boolean = false
)

object PrayerSoundManager {

    val availableSounds = listOf(
        PrayerSoundItem(
            id = "adhan_makkah",
            nameArabic = "أذان مكة المكرمة",
            descArabic = "الأذان المكي الشهير بنبرة جليلة ومهيبة",
            durationSeconds = 6,
            isCriticalRecommended = true
        ),
        PrayerSoundItem(
            id = "adhan_madinah",
            nameArabic = "أذان المدينة المنورة",
            descArabic = "أذان الحرم النبوي الشريف العذب الهادئ",
            durationSeconds = 6,
            isCriticalRecommended = true
        ),
        PrayerSoundItem(
            id = "adhan_aqsa",
            nameArabic = "أذان المسجد الأقصى",
            descArabic = "أذان القدس الشريف بنبرة خاشعة ومؤثرة",
            durationSeconds = 5
        ),
        PrayerSoundItem(
            id = "fajr_special",
            nameArabic = "أذان الفجر (الصلاة خير من النوم)",
            descArabic = "نغمة أذان الفجر الخاصة مع نداء الصلاة خير من النوم",
            durationSeconds = 6,
            isCriticalRecommended = true
        ),
        PrayerSoundItem(
            id = "takbeerat",
            nameArabic = "تكبيرات الصلاة والأعياد",
            descArabic = "تكبيرات مباركة تملأ القلب سكينة وبهجة",
            durationSeconds = 5
        ),
        PrayerSoundItem(
            id = "tasbeeh_chime",
            nameArabic = "نغمة تسبيح هادئة",
            descArabic = "نغمات هادئة تشبه رنين الأجراس الخافتة للتنبيه الرفيق",
            durationSeconds = 4
        ),
        PrayerSoundItem(
            id = "silent_vibrate",
            nameArabic = "اهتزاز فقط (صامت)",
            descArabic = "تنبيه اهتزازي لطيف دون إصدار أصوات",
            durationSeconds = 0
        )
    )

    private var previewMediaPlayer: MediaPlayer? = null
    var currentlyPlayingSoundId: String? = null
        private set

    fun getSoundItem(id: String): PrayerSoundItem {
        return availableSounds.firstOrNull { it.id == id } ?: availableSounds.first()
    }

    /**
     * Ensures all audio wave files are generated in local storage.
     */
    fun initializeSounds(context: Context) {
        val soundDir = File(context.filesDir, "prayer_sounds")
        if (!soundDir.exists()) {
            soundDir.mkdirs()
        }

        availableSounds.forEach { sound ->
            if (sound.id != "silent_vibrate") {
                val soundFile = File(soundDir, "${sound.id}.wav")
                if (!soundFile.exists() || soundFile.length() < 1000) {
                    try {
                        generateWavFile(soundFile, sound.id)
                    } catch (e: Exception) {
                        Log.e("PrayerSoundManager", "Error generating sound ${sound.id}", e)
                    }
                }
            }
        }
    }

    fun getSoundFile(context: Context, soundId: String): File? {
        if (soundId == "silent_vibrate") return null
        val soundDir = File(context.filesDir, "prayer_sounds")
        val file = File(soundDir, "$soundId.wav")
        return if (file.exists()) file else null
    }

    fun playPreview(context: Context, soundId: String, onCompletion: () -> Unit = {}) {
        stopPreview()

        if (soundId == "silent_vibrate") {
            currentlyPlayingSoundId = null
            onCompletion()
            return
        }

        val file = getSoundFile(context, soundId)
        if (file == null) {
            initializeSounds(context)
        }
        val targetFile = getSoundFile(context, soundId) ?: return

        try {
            previewMediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .build()
                )
                setDataSource(context, Uri.fromFile(targetFile))
                prepare()
                setOnCompletionListener {
                    currentlyPlayingSoundId = null
                    onCompletion()
                }
                start()
            }
            currentlyPlayingSoundId = soundId
        } catch (e: Exception) {
            Log.e("PrayerSoundManager", "Failed to play sound preview", e)
            currentlyPlayingSoundId = null
            onCompletion()
        }
    }

    fun stopPreview() {
        try {
            previewMediaPlayer?.stop()
            previewMediaPlayer?.release()
        } catch (_: Exception) {}
        previewMediaPlayer = null
        currentlyPlayingSoundId = null
    }

    /**
     * Synthesizes audio waveforms for each Islamic melody
     */
    private fun generateWavFile(outputFile: File, soundId: String) {
        val sampleRate = 22050
        val durationSeconds = when (soundId) {
            "tasbeeh_chime" -> 4.0
            "takbeerat" -> 5.0
            else -> 6.0
        }
        val totalSamples = (sampleRate * durationSeconds).toInt()
        val samples = ShortArray(totalSamples)

        when (soundId) {
            "adhan_makkah" -> synthesizeMakkahAdhan(samples, sampleRate)
            "adhan_madinah" -> synthesizeMadinahAdhan(samples, sampleRate)
            "adhan_aqsa" -> synthesizeAqsaAdhan(samples, sampleRate)
            "fajr_special" -> synthesizeFajrSpecial(samples, sampleRate)
            "takbeerat" -> synthesizeTakbeerat(samples, sampleRate)
            "tasbeeh_chime" -> synthesizeTasbeehChime(samples, sampleRate)
            else -> synthesizeTasbeehChime(samples, sampleRate)
        }

        // Write WAV header and PCM data
        FileOutputStream(outputFile).use { fos ->
            val byteBuffer = ByteBuffer.allocate(44 + totalSamples * 2).order(ByteOrder.LITTLE_ENDIAN)
            writeWavHeader(byteBuffer, totalSamples * 2, sampleRate)
            for (sample in samples) {
                byteBuffer.putShort(sample)
            }
            fos.write(byteBuffer.array())
        }
    }

    private fun writeWavHeader(buffer: ByteBuffer, dataLength: Int, sampleRate: Int) {
        val totalLength = dataLength + 36
        val byteRate = sampleRate * 1 * 2 // 16-bit mono

        buffer.put("RIFF".toByteArray())
        buffer.putInt(totalLength)
        buffer.put("WAVE".toByteArray())
        buffer.put("fmt ".toByteArray())
        buffer.putInt(16) // Subchunk1Size (16 for PCM)
        buffer.putShort(1.toShort()) // AudioFormat (1 for PCM)
        buffer.putShort(1.toShort()) // NumChannels (1 mono)
        buffer.putInt(sampleRate)
        buffer.putInt(byteRate)
        buffer.putShort(2.toShort()) // BlockAlign (NumChannels * BitsPerSample/8)
        buffer.putShort(16.toShort()) // BitsPerSample
        buffer.put("data".toByteArray())
        buffer.putInt(dataLength)
    }

    private fun synthesizeMakkahAdhan(samples: ShortArray, sampleRate: Int) {
        // Maqam Bayati / Rast phrase (F4, Ab4, Bb4, C5, Bb4, Ab4, G4, F4)
        val notes = doubleArrayOf(349.23, 415.30, 466.16, 523.25, 466.16, 415.30, 392.00, 349.23)
        val noteDurations = doubleArrayOf(0.7, 0.7, 0.8, 1.2, 0.6, 0.6, 0.7, 1.2)
        renderHarmonicMelody(samples, sampleRate, notes, noteDurations, resonance = 0.35)
    }

    private fun synthesizeMadinahAdhan(samples: ShortArray, sampleRate: Int) {
        // Maqam Hijaz peaceful phrase (D4, Eb4, F#4, G4, A4, G4, F#4, Eb4, D4)
        val notes = doubleArrayOf(293.66, 311.13, 369.99, 392.00, 440.00, 392.00, 369.99, 311.13, 293.66)
        val noteDurations = doubleArrayOf(0.6, 0.5, 0.7, 0.9, 1.1, 0.6, 0.6, 0.5, 1.3)
        renderHarmonicMelody(samples, sampleRate, notes, noteDurations, resonance = 0.28)
    }

    private fun synthesizeAqsaAdhan(samples: ShortArray, sampleRate: Int) {
        // Solemn traditional phrase (C4, D4, Eb4, F4, G4, F4, Eb4, D4, C4)
        val notes = doubleArrayOf(261.63, 293.66, 311.13, 349.23, 392.00, 349.23, 311.13, 293.66, 261.63)
        val noteDurations = doubleArrayOf(0.6, 0.5, 0.6, 0.8, 1.1, 0.6, 0.5, 0.6, 1.2)
        renderHarmonicMelody(samples, sampleRate, notes, noteDurations, resonance = 0.3)
    }

    private fun synthesizeFajrSpecial(samples: ShortArray, sampleRate: Int) {
        // "الصلاة خير من النوم" distinct rising cadence (G4, C5, B4, A4, G4, A4, B4, C5)
        val notes = doubleArrayOf(392.00, 523.25, 493.88, 440.00, 392.00, 440.00, 493.88, 523.25)
        val noteDurations = doubleArrayOf(0.5, 1.1, 0.6, 0.7, 0.8, 0.6, 0.7, 1.4)
        renderHarmonicMelody(samples, sampleRate, notes, noteDurations, resonance = 0.4)
    }

    private fun synthesizeTakbeerat(samples: ShortArray, sampleRate: Int) {
        // "الله أكبر الله أكبر لا إله إلا الله"
        // G4, G4, C5, C5, B4, A4, G4, A4, G4
        val notes = doubleArrayOf(392.00, 392.00, 523.25, 523.25, 493.88, 440.00, 392.00, 440.00, 392.00)
        val noteDurations = doubleArrayOf(0.4, 0.4, 0.7, 0.7, 0.5, 0.5, 0.7, 0.5, 1.1)
        renderHarmonicMelody(samples, sampleRate, notes, noteDurations, resonance = 0.3)
    }

    private fun synthesizeTasbeehChime(samples: ShortArray, sampleRate: Int) {
        // Harmonic peaceful chimes with crystal bell decay
        val chords = listOf(
            doubleArrayOf(523.25, 659.25, 783.99), // C Major
            doubleArrayOf(587.33, 698.46, 880.00), // D minor
            doubleArrayOf(523.25, 659.25, 1046.50) // C Octave
        )
        val chordStarts = doubleArrayOf(0.0, 1.2, 2.4)

        for (cIndex in chords.indices) {
            val chord = chords[cIndex]
            val startTime = chordStarts[cIndex]
            val startSample = (startTime * sampleRate).toInt()

            for (note in chord) {
                for (i in 0 until (1.8 * sampleRate).toInt()) {
                    val sIdx = startSample + i
                    if (sIdx >= samples.size) break
                    val t = i.toDouble() / sampleRate
                    // Bell decay envelope
                    val envelope = exp(-2.4 * t)
                    val wave = sin(2 * PI * note * t) + 0.4 * sin(4 * PI * note * t) + 0.15 * sin(6 * PI * note * t)
                    val sampleVal = (wave * envelope * 9000).toInt()
                    val existing = samples[sIdx].toInt()
                    samples[sIdx] = (existing + sampleVal).coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                }
            }
        }
    }

    private fun renderHarmonicMelody(
        samples: ShortArray,
        sampleRate: Int,
        notes: DoubleArray,
        durations: DoubleArray,
        resonance: Double
    ) {
        var currentSample = 0
        for (i in notes.indices) {
            val freq = notes[i]
            val noteDuration = durations[i]
            val noteSamples = (noteDuration * sampleRate).toInt()

            for (j in 0 until noteSamples) {
                val sIdx = currentSample + j
                if (sIdx >= samples.size) break
                val t = j.toDouble() / sampleRate
                val relT = j.toDouble() / noteSamples

                // Smooth attack and release envelope
                val env = when {
                    relT < 0.1 -> relT / 0.1
                    relT > 0.8 -> (1.0 - relT) / 0.2
                    else -> 1.0
                }

                // Vocal formant simulation (fundamental + 2nd + 3rd harmonics + subtle vibrato)
                val vibrato = 1.0 + 0.006 * sin(2 * PI * 5.2 * t)
                val effectiveFreq = freq * vibrato

                val val1 = sin(2 * PI * effectiveFreq * t)
                val val2 = resonance * sin(4 * PI * effectiveFreq * t)
                val val3 = (resonance * 0.5) * sin(6 * PI * effectiveFreq * t)

                val sampleVal = ((val1 + val2 + val3) * env * 14000).toInt()
                samples[sIdx] = sampleVal.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }
            currentSample += noteSamples
        }
    }
}
