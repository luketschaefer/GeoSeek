package com.example.geoseek.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h1800dp-xxhdpi")
class SettingsOverlayTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun displaysSettingsTitleAndDarkModeOption() {
        compose.setContent {
            GeoTheme(isDarkMode = true) {
                SettingsOverlay(
                    isDarkMode = true,
                    onToggleDarkMode = {},
                    onDismiss = {},
                )
            }
        }

        compose.onNodeWithText("SETTINGS").assertIsDisplayed()
        compose.onNodeWithText("Theme Mode").assertIsDisplayed()
        compose.onNodeWithText("Dark Mode (Moon & Stars)").assertIsDisplayed()
        compose.onNodeWithText("Dark").assertIsDisplayed()
    }

    @Test
    fun togglingDarkMode_triggersCallback() {
        var toggled = false
        var newValue = false

        compose.setContent {
            GeoTheme(isDarkMode = true) {
                SettingsOverlay(
                    isDarkMode = true,
                    onToggleDarkMode = {
                        toggled = true
                        newValue = it
                    },
                    onDismiss = {},
                )
            }
        }

        compose.onNodeWithText("Dark").performClick()

        assertTrue(toggled)
        assertEquals(false, newValue)
    }

    @Test
    fun clickingClose_triggersDismissCallback() {
        var dismissed = false

        compose.setContent {
            GeoTheme(isDarkMode = true) {
                SettingsOverlay(
                    isDarkMode = true,
                    onToggleDarkMode = {},
                    onDismiss = { dismissed = true },
                )
            }
        }

        compose.onNodeWithText("Close").performClick()

        assertTrue(dismissed)
    }
}
