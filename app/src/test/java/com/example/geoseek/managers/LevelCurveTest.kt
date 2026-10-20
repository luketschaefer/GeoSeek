package com.example.geoseek.managers

import com.example.geoseek.data.DailyProgress
import com.example.geoseek.data.Player
import com.example.geoseek.data.levelInfo
import org.junit.Assert.assertEquals
import org.junit.Test

/** Checks that a saved player's level always agrees with XPManager's curve. */
class LevelCurveTest {

    private fun playerWith(xp: Int) = Player(
        username = "tester",
        joinedAtMillis = 0L,
        xp = xp,
        huntsPlayed = 0,
        bestScore = 0,
        binder = emptyMap(),
        daily = DailyProgress(),
        questClaimedToday = false,
    )

    @Test
    fun playerLevelInfo_matchesXPManager() {
        listOf(0, 1, 99, 100, 101, 250, 999, 1000, Int.MAX_VALUE).forEach { xp ->
            val manager = XPManager().apply { addXp(xp) }
            val info = playerWith(xp).levelInfo
            assertEquals("level at $xp XP", manager.level, info.level)
            assertEquals("progress at $xp XP", manager.progressToNextLevel, info.progress, 0.0001f)
        }
    }

    @Test
    fun levelInfo_neededIsOneLevelOfXp() {
        assertEquals(XPManager.XP_PER_LEVEL, playerWith(150).levelInfo.needed)
        assertEquals(50, playerWith(150).levelInfo.intoLevel)
    }

    @Test
    fun negativeStoredXp_isTreatedAsZero() {
        val info = playerWith(-40).levelInfo
        assertEquals(1, info.level)
        assertEquals(0, info.intoLevel)
    }

    @Test
    fun addXpCapped_neverOverflows() {
        assertEquals(Int.MAX_VALUE, XPManager.addXpCapped(Int.MAX_VALUE, 1))
        assertEquals(Int.MAX_VALUE, XPManager.addXpCapped(Int.MAX_VALUE - 5, 100))
        assertEquals(15, XPManager.addXpCapped(10, 5))
        assertEquals(10, XPManager.addXpCapped(10, -5))
    }
}
