package com.example.geoseek.screens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class GeoIconsTest {

    @Test
    fun gearIcon_hasValidDimensionsAndRows() {
        val gear = Icons.Gear
        assertNotNull(gear)
        assertEquals(13, gear.width)
        assertEquals(13, gear.height)
        assertEquals(13, gear.rows.size)
    }
}
