package com.example.geoseek.data

import android.content.Context
import java.time.Instant
import java.util.Locale
import java.util.TimeZone
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class DailyQuestClockTest {
    private val app = RuntimeEnvironment.getApplication()
    private val prefs get() = app.getSharedPreferences("quest_clock_test", Context.MODE_PRIVATE)
    private val start = Instant.parse("2026-11-10T23:59:00Z").toEpochMilli()
    private var wall = start
    private var elapsed = 10_000L
    private var boot = 1
    private lateinit var originalZone: TimeZone
    private lateinit var originalLocale: Locale

    private fun clock() = DailyQuestClock(prefs, { wall }, { elapsed }, { boot })

    @Before
    fun setUp() {
        prefs.edit().clear().commit()
        originalZone = TimeZone.getDefault()
        originalLocale = Locale.getDefault()
    }

    @After
    fun tearDown() {
        TimeZone.setDefault(originalZone)
        Locale.setDefault(originalLocale)
        prefs.edit().clear().commit()
    }

    @Test
    fun dayUsesUtcRegardlessOfDeviceTimeZone() {
        TimeZone.setDefault(TimeZone.getTimeZone("Pacific/Kiritimati"))
        assertEquals(2026314, clock().today())
        TimeZone.setDefault(TimeZone.getTimeZone("Etc/GMT+12"))
        assertEquals(2026314, clock().today())
    }

    @Test
    fun forwardAndBackwardWallClockEditsDoNotChangeQuestTime() {
        assertEquals(start, clock().nowMillis())
        wall += 10 * DAY
        elapsed += 1_000L
        assertEquals(start + 1_000L, clock().nowMillis())
        wall -= 20 * DAY
        elapsed += 1_000L
        assertEquals(start + 2_000L, clock().nowMillis())
    }

    @Test
    fun deviceLocaleDoesNotChangeTheQuestCalendar() {
        assertEquals(2026314, clock().today())
        Locale.setDefault(Locale.forLanguageTag("th-TH"))
        assertEquals(2026314, clock().today())
    }

    @Test
    fun elapsedTimeIncludingTimeAwayFromAppAdvancesPersistedClock() {
        assertEquals(start, clock().nowMillis())
        elapsed += DAY
        wall -= DAY
        assertEquals(start + DAY, clock().nowMillis())
        assertEquals(2026315, clock().today())
    }

    @Test
    fun midnightRollsOverOnlyWhenElapsedTimeReachesIt() {
        assertEquals(2026314, clock().today())
        elapsed += 59_999L
        assertEquals(2026314, clock().today())
        elapsed++
        assertEquals(2026315, clock().today())
    }

    @Test
    fun rolloverHandlesNewYear() {
        wall = Instant.parse("2026-12-31T23:59:59Z").toEpochMilli()
        assertEquals(2026365, clock().today())
        elapsed += 1_000L
        assertEquals(2027001, clock().today())
    }

    @Test
    fun rebootKeepsCheckpointInsteadOfTrustingEditedWallClock() {
        clock().nowMillis()
        elapsed += 1_000L
        val checkpoint = clock().nowMillis()
        boot++
        elapsed = 100L
        wall += 10 * DAY
        assertEquals(checkpoint, clock().nowMillis())
        elapsed += DAY
        assertEquals(checkpoint + DAY, clock().nowMillis())
    }

    @Test
    fun bootCountDetectsRebootEvenWhenNewElapsedTimeIsLarger() {
        clock().nowMillis()
        boot++
        elapsed += DAY
        wall += 10 * DAY
        assertEquals(start, clock().nowMillis())
    }

    @Test
    fun lowerElapsedTimeIsConservativeWhenBootCountIsUnavailable() {
        boot = -1
        clock().nowMillis()
        elapsed = 0
        wall -= DAY
        assertEquals(start, clock().nowMillis())
        elapsed += DAY
        assertEquals(start + DAY, clock().nowMillis())
    }

    companion object {
        private const val DAY = 86_400_000L
    }
}
