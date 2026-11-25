package com.example.geoseek.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import com.example.geoseek.data.DailyProgress
import com.example.geoseek.data.Player
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h1800dp-xxhdpi")
class HomeScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private fun testPlayer() = Player(
        username = "TestExplorer",
        joinedAtMillis = 0L,
        xp = 100,
        huntsPlayed = 2,
        bestScore = 50,
        binder = emptyMap(),
        daily = DailyProgress(),
        questClaimedToday = false,
    )

    @Test
    fun homeScreen_displaysSettingsGearButton() {
        compose.setContent {
            GeoTheme {
                HomeScreen(
                    player = testPlayer(),
                    onOpen = {},
                    onClaimQuest = {},
                    onOpenSettings = {},
                )
            }
        }

        compose.onNodeWithContentDescription("Settings").assertIsDisplayed()
    }

    @Test
    fun clickingGearButton_triggersOpenSettingsCallback() {
        var clicked = false
        compose.setContent {
            GeoTheme {
                HomeScreen(
                    player = testPlayer(),
                    onOpen = {},
                    onClaimQuest = {},
                    onOpenSettings = { clicked = true },
                )
            }
        }

        compose.onNodeWithContentDescription("Settings").performClick()

        assertTrue(clicked)
    }
}
