package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("مُصلّي", appName)
  }

  @Test
  fun `test prayer stats manager weekly and monthly calculations`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val statsManager = com.example.data.PrayerStatsManager(context)
    val weekly = statsManager.getWeeklyStats()
    val monthly = statsManager.getMonthlyStats()

    assertEquals(7, weekly.days.size)
    org.junit.Assert.assertTrue(weekly.totalOnTimePrayers >= 0)
    org.junit.Assert.assertTrue(weekly.weeklyCommitmentPercent in 0..100)
    org.junit.Assert.assertTrue(monthly.commitmentPercent in 0..100)
    assertEquals(4, monthly.weeklyBreakdown.size)

    val todayKey = statsManager.getTodayKey()
    statsManager.setPrayerStatus(todayKey, com.example.model.PrayerType.FAJR, com.example.model.PrayerLogStatus.ON_TIME)
    val todayLog = statsManager.getDayLog(todayKey)
    assertEquals(com.example.model.PrayerLogStatus.ON_TIME, todayLog.fajr)
  }
}
