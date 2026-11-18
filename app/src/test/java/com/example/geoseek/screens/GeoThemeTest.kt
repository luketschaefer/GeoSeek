package com.example.geoseek.screens

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GeoThemeTest {

    @Test
    fun darkGeoColorScheme_hasCorrectDefaultProperties() {
        assertTrue(DarkGeoColorScheme.isDark)
        assertEquals(Color(0xFF070A1F), DarkGeoColorScheme.night)
        assertEquals(Color(0xFFFFFFFF), DarkGeoColorScheme.text)
        assertEquals(Color(0xD8192157), DarkGeoColorScheme.panel)
    }

    @Test
    fun lightGeoColorScheme_hasCorrectProperties() {
        assertFalse(LightGeoColorScheme.isDark)
        assertNotEquals(DarkGeoColorScheme.night, LightGeoColorScheme.night)
        assertNotEquals(DarkGeoColorScheme.text, LightGeoColorScheme.text)
        assertNotEquals(DarkGeoColorScheme.panel, LightGeoColorScheme.panel)
    }
}
