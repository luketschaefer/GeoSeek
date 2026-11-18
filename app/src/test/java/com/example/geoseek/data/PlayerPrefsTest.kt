package com.example.geoseek.data

import android.content.Context
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class PlayerPrefsTest {
    private val app = RuntimeEnvironment.getApplication()
    private val prefs get() = app.getSharedPreferences("geoseek_auth", Context.MODE_PRIVATE)

    private fun store() = PlayerPrefs(prefs, DailyQuestClock(prefs, { 0L }, { 0L }, { 1 }))

    @Before
    fun setUp() {
        prefs.edit().clear().commit()
    }

    @Test
    fun isDarkMode_defaultsToTrue() {
        val playerPrefs = store()
        assertTrue(playerPrefs.isDarkMode())
    }

    @Test
    fun isDarkMode_respectsCustomDefaultWhenNotSet() {
        val playerPrefs = store()
        assertFalse(playerPrefs.isDarkMode(default = false))
    }

    @Test
    fun setDarkMode_persistsPreference() {
        val playerPrefs = store()
        playerPrefs.setDarkMode(false)
        assertFalse(playerPrefs.isDarkMode())

        playerPrefs.setDarkMode(true)
        assertTrue(playerPrefs.isDarkMode())
    }
}
