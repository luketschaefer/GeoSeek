package com.example.geoseek.managers

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * CMPT 325 Assignment 4 (Luke Schaefer) - BOUNDARY VALUE TESTING of [XPManager.addXp].
 *
 * Specification under test:
 *   fun addXp(amount: Int): Boolean
 *   - If amount <= 0 the call is ignored: xp is unchanged and false is returned.
 *   - Otherwise amount is added to xp. Level is (xp / 100) + 1, so a new level starts at every multiple of 100.
 *   - Returns true exactly when the call moved the player to a higher level (one or more levels).
 *   - Total xp never decreases as a result of a call.
 *
 * Boundaries tested:
 *   B1  validity of amount:        amount = 0          -> just below / on / just above: -1, 0, 1 (+ Int.MIN_VALUE extreme)
 *   B2  level threshold from 0:    xp crosses 100      -> amount 99, 100, 101
 *   B3  level threshold mid-level: from xp = 99 / 100  -> +1 (crosses), +99 / +100 from 100 (stays / crosses)
 *   B4  upper limit of Int:        xp near Int.MAX_VALUE
 */
class XPManagerBoundaryTest {

    /** Returns a manager whose xp has been primed to [startXp] using only the public API. */
    private fun managerAt(startXp: Int): XPManager {
        val manager = XPManager()
        if (startXp > 0) manager.addXp(startXp)
        assertEquals("test setup", startXp, manager.xp)
        return manager
    }

    // ---------- B1: amount must be > 0 ----------

    @Test
    fun b1_amountJustBelowZero_isIgnored() {
        val m = managerAt(0)
        assertFalse(m.addXp(-1))
        assertEquals(0, m.xp)
        assertEquals(1, m.level)
    }

    @Test
    fun b1_amountZero_isIgnored() {
        val m = managerAt(50)
        assertFalse(m.addXp(0))
        assertEquals(50, m.xp)
    }

    @Test
    fun b1_amountJustAboveZero_isAdded() {
        val m = managerAt(0)
        assertFalse(m.addXp(1))
        assertEquals(1, m.xp)
        assertEquals(1, m.level)
    }

    @Test
    fun b1_amountMinInt_isIgnored() {
        val m = managerAt(50)
        assertFalse(m.addXp(Int.MIN_VALUE))
        assertEquals(50, m.xp)
    }

    // ---------- B2: crossing the first level threshold (xp = 100) from 0 ----------

    @Test
    fun b2_ninetyNine_staysOnLevelOne() {
        val m = managerAt(0)
        assertFalse(m.addXp(99))
        assertEquals(99, m.xp)
        assertEquals(1, m.level)
    }

    @Test
    fun b2_exactlyOneHundred_levelsUp() {
        val m = managerAt(0)
        assertTrue(m.addXp(100))
        assertEquals(100, m.xp)
        assertEquals(2, m.level)
    }

    @Test
    fun b2_oneHundredOne_levelsUp() {
        val m = managerAt(0)
        assertTrue(m.addXp(101))
        assertEquals(101, m.xp)
        assertEquals(2, m.level)
    }

    // ---------- B3: thresholds when the player already has xp ----------

    @Test
    fun b3_fromNinetyNine_plusOne_levelsUp() {
        val m = managerAt(99)
        assertTrue(m.addXp(1))
        assertEquals(2, m.level)
    }

    @Test
    fun b3_fromOneHundred_plusNinetyNine_staysOnLevelTwo() {
        val m = managerAt(100)
        assertFalse(m.addXp(99))
        assertEquals(199, m.xp)
        assertEquals(2, m.level)
    }

    @Test
    fun b3_fromOneHundred_plusOneHundred_levelsUp() {
        val m = managerAt(100)
        assertTrue(m.addXp(100))
        assertEquals(3, m.level)
    }

    @Test
    fun b3_singleCallCrossingTwoThresholds_returnsTrueAndSkipsLevel() {
        val m = managerAt(0)
        assertTrue(m.addXp(250))
        assertEquals(3, m.level)
    }

    // ---------- B4: upper limit of the Int range ----------

    @Test
    fun b4_reachingExactlyMaxInt_isAdded() {
        val m = managerAt(Int.MAX_VALUE - 1)
        m.addXp(1)
        assertEquals(Int.MAX_VALUE, m.xp)
    }

    /**
     * DEFECT FOUND (A4 report): xp was a plain Int, so adding past Int.MAX_VALUE wrapped around to a large
     * negative number and the player's level became negative. Fixed by capping the addition in addXp.
     */
    @Test
    fun b4_justAboveMaxInt_doesNotDecreaseXp() {
        val m = managerAt(Int.MAX_VALUE)
        m.addXp(1)
        assertTrue("xp must never decrease, was ${m.xp}", m.xp >= Int.MAX_VALUE - 1)
        assertTrue("level must stay positive, was ${m.level}", m.level > 0)
    }
}
