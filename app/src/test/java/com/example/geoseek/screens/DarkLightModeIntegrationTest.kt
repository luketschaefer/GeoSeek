package com.example.geoseek.screens

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.example.geoseek.data.DailyProgress
import com.example.geoseek.data.DailyQuestClock
import com.example.geoseek.data.Player
import com.example.geoseek.data.PlayerPrefs
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h1800dp-xxhdpi")
class DarkLightModeIntegrationTest {

    @get:Rule
    val compose = createComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private fun prefs() = context.getSharedPreferences("geoseek_auth", Context.MODE_PRIVATE)

    private fun testPlayer() = Player(
        username = "ThemeTester",
        joinedAtMillis = 0L,
        xp = 100,
        huntsPlayed = 2,
        bestScore = 50,
        binder = emptyMap(),
        daily = DailyProgress(),
        questClaimedToday = false,
    )

    @Before
    fun setUp() {
        prefs().edit().clear().commit()
    }

    @After
    fun tearDown() {
        prefs().edit().clear().commit()
    }

    @Test
    fun openingSettingsFromHomeScreen_andTogglingDarkMode_persistsPreference() {
        val playerPrefs = PlayerPrefs(prefs(), DailyQuestClock.from(context, prefs()))
        assertTrue(playerPrefs.isDarkMode())

        compose.setContent {
            var isDarkMode by remember { mutableStateOf(playerPrefs.isDarkMode()) }
            var showSettingsOverlay by remember { mutableStateOf(false) }

            GeoTheme(isDarkMode = isDarkMode) {
                HomeScreen(
                    player = testPlayer(),
                    onOpen = {},
                    onClaimQuest = {},
                    onOpenSettings = { showSettingsOverlay = true },
                )

                if (showSettingsOverlay) {
                    SettingsOverlay(
                        isDarkMode = isDarkMode,
                        onToggleDarkMode = { enabled ->
                            isDarkMode = enabled
                            playerPrefs.setDarkMode(enabled)
                        },
                        onDismiss = { showSettingsOverlay = false },
                    )
                }
            }
        }

        // Verify top-right settings gear is displayed on HomeScreen
        compose.onNodeWithContentDescription("Settings").assertIsDisplayed()

        // Open settings overlay
        compose.onNodeWithContentDescription("Settings").performClick()

        // Verify overlay is open
        compose.onNodeWithText("SETTINGS").assertIsDisplayed()
        compose.onNodeWithText("Dark Mode (Moon & Stars)").assertIsDisplayed()

        // Toggle from Dark to Light Mode
        compose.onNodeWithText("Dark").performClick()

        // Verify toggle state changed in UI
        compose.onNodeWithText("Light Mode (Daylight & Sun)").assertIsDisplayed()

        // Verify preference was persisted to PlayerPrefs
        assertFalse(playerPrefs.isDarkMode())

        // Close overlay
        compose.onNodeWithText("Close").performClick()

        // Verify overlay closed
        compose.onNodeWithText("SETTINGS").assertDoesNotExist()
    }
}
