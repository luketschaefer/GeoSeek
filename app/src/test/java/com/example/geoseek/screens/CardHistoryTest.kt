package com.example.geoseek.screens

import com.example.geoseek.data.CardRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CardHistoryTest {

    @Test
    fun singleCard_withoutDate() {
        assertEquals("Found once", cardHistory(CardRecord(1, 0L)))
    }

    @Test
    fun duplicates_withoutDate() {
        assertEquals("Found 3 times", cardHistory(CardRecord(3, 0L)))
    }

    @Test
    fun withDate_addsWhenItWasFirstFound() {
        val text = cardHistory(CardRecord(2, 1_790_000_000_000L))
        assertTrue(text, text.startsWith("Found 2 times, first on "))
    }
}
