package com.example.geoseek.screens

import org.junit.Assert.assertTrue
import org.junit.Test

class WorldBackgroundTest {

    @Test
    fun effectiveTimeOfDay_inDarkMode_shiftsDaytimeToNighttime() {
        val todDay = 0.45f // Daytime hour (sun is up)
        val todNight = effectiveTimeOfDay(todDay, isDarkMode = true)

        // 0.95f is nighttime (moon is up, sun is below horizon)
        assertTrue("Expected night time of day in dark mode, got $todNight", todNight >= 0.8f || todNight <= 0.2f)
    }

    @Test
    fun effectiveTimeOfDay_inDarkMode_preservesNighttime() {
        val todNight = 0.90f // Nighttime hour (moon is up)
        val result = effectiveTimeOfDay(todNight, isDarkMode = true)

        assertTrue("Expected night time of day preserved in dark mode, got $result", result >= 0.8f || result <= 0.2f)
    }

    @Test
    fun effectiveTimeOfDay_inLightMode_shiftsNighttimeToDaytime() {
        val todNight = 0.90f // Nighttime hour
        val todDay = effectiveTimeOfDay(todNight, isDarkMode = false)

        // Daytime hour (sun is up)
        assertTrue("Expected day time of day in light mode, got $todDay", todDay in 0.3f..0.7f)
    }

    @Test
    fun effectiveTimeOfDay_inLightMode_preservesDaytime() {
        val todDay = 0.45f // Daytime hour
        val result = effectiveTimeOfDay(todDay, isDarkMode = false)

        assertTrue("Expected day time of day preserved in light mode, got $result", result in 0.3f..0.7f)
    }
}
