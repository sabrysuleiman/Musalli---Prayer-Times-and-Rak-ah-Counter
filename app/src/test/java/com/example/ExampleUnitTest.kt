package com.example

import com.example.data.OfflinePrayerCalculator
import com.example.data.PrayerPreferences
import com.example.data.PrayerSoundManager
import com.example.data.QiblaUtil
import com.example.data.QuranTextProvider
import com.example.model.AzkarCategory
import com.example.model.AzkarRepository
import com.example.model.PrayerType
import com.example.model.QuranMetadata
import com.example.model.QuranWardProgress
import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar

class PrayerAndRakahUnitTest {

    @Test
    fun testPrayerRakahs_areAccurate() {
        assertEquals(2, PrayerType.FAJR.defaultRakahs)
        assertEquals(4, PrayerType.DHUHR.defaultRakahs)
        assertEquals(4, PrayerType.ASR.defaultRakahs)
        assertEquals(3, PrayerType.MAGHRIB.defaultRakahs)
        assertEquals(4, PrayerType.ISHA.defaultRakahs)
        assertEquals(2, PrayerType.JUMMAH.defaultRakahs)
        assertEquals(3, PrayerType.WITR.defaultRakahs)
    }

    @Test
    fun testOfflinePrayerCalculator_calculatesCorrectSchedule() {
        val cal = Calendar.getInstance()
        cal.set(2026, Calendar.OCTOBER, 15, 12, 0, 0)

        // Cairo Coordinates: 30.0444 N, 31.2357 E
        val schedule = OfflinePrayerCalculator.calculate(
            latitude = 30.0444,
            longitude = 31.2357,
            calendar = cal,
            method = OfflinePrayerCalculator.CalculationMethod.EGYPTIAN,
            cityName = "القاهرة"
        )

        assertNotNull(schedule.fajr)
        assertNotNull(schedule.dhuhr)
        assertNotNull(schedule.asr)
        assertNotNull(schedule.maghrib)
        assertNotNull(schedule.isha)

        assertTrue(schedule.fajr.matches(Regex("\\d{2}:\\d{2}")))
        assertTrue(schedule.dhuhr.matches(Regex("\\d{2}:\\d{2}")))
        assertTrue(schedule.maghrib.matches(Regex("\\d{2}:\\d{2}")))
        assertTrue(schedule.hijriDate.contains("هـ"))
    }

    @Test
    fun testQiblaCalculator_calculatesDirectionAccurately() {
        val bearing = QiblaUtil.calculateQiblaBearing(30.0444, 31.2357)
        assertTrue("Qibla bearing from Cairo should be between 130 and 145 degrees", bearing in 130.0..145.0)

        val distance = QiblaUtil.calculateDistanceToKaabaKm(30.0444, 31.2357)
        assertTrue("Distance from Cairo to Makkah should be ~1200-1400km", distance in 1100..1500)
    }

    @Test
    fun testPrayerSounds_providesAtLeastFiveDistinctSounds() {
        val sounds = PrayerSoundManager.availableSounds
        assertTrue("Must provide at least 5 different sounds", sounds.size >= 5)

        val soundIds = sounds.map { it.id }
        assertTrue(soundIds.contains("adhan_makkah"))
        assertTrue(soundIds.contains("adhan_madinah"))
        assertTrue(soundIds.contains("adhan_aqsa"))
        assertTrue(soundIds.contains("fajr_special"))
        assertTrue(soundIds.contains("takbeerat"))
        assertTrue(soundIds.contains("tasbeeh_chime"))
    }

    @Test
    fun testAzkarRepository_hasAllCategories() {
        val categories = AzkarCategory.entries
        assertEquals(5, categories.size)

        val allItems = AzkarRepository.items
        assertTrue("Azkar repository must contain authentic items", allItems.size >= 15)

        val morningItems = allItems.filter { it.category == AzkarCategory.MORNING }
        val eveningItems = allItems.filter { it.category == AzkarCategory.EVENING }
        val postPrayerItems = allItems.filter { it.category == AzkarCategory.POST_PRAYER }

        assertTrue(morningItems.isNotEmpty())
        assertTrue(eveningItems.isNotEmpty())
        assertTrue(postPrayerItems.isNotEmpty())
    }

    @Test
    fun testQuranMetadata_hasAll114Surahs() {
        assertEquals(114, QuranMetadata.surahs.size)
        assertEquals("الفاتحة", QuranMetadata.surahs.first().nameArabic)
        assertEquals("الناس", QuranMetadata.surahs.last().nameArabic)

        val page1Content = QuranTextProvider.getPage(1)
        assertEquals(1, page1Content.pageNumber)
        assertEquals("الفاتحة", page1Content.surah.nameArabic)
        assertEquals(7, page1Content.ayahs.size)

        val progress = QuranWardProgress(currentPage = 302, dailyGoalPages = 10)
        assertEquals(50, progress.progressPercent)
        assertEquals(16, progress.currentJuz)
        assertEquals(302, progress.remainingPages)
    }
}
