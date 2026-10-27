package com.example.geoseek.audio

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BitDefendersThemeTest {

    @Test
    fun mentionedIn_findsTheWordHoweverItIsCasedOrSplit() {
        assertTrue(BitDefendersTheme.mentionedIn("BitDefenders"))
        assertTrue(BitDefendersTheme.mentionedIn("play BITDEFENDERS today"))
        assertTrue(BitDefendersTheme.mentionedIn("Bit Defenders"))
        assertTrue(BitDefendersTheme.mentionedIn("Bit-\nDefenders"))
    }

    @Test
    fun mentionedIn_ignoresOtherText() {
        assertFalse(BitDefendersTheme.mentionedIn(""))
        assertFalse(BitDefendersTheme.mentionedIn("Bitdefender"))
        assertFalse(BitDefendersTheme.mentionedIn("Defenders of the bit"))
    }
}
