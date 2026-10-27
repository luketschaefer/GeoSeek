package com.example.geoseek.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CardDuelTest {
    private val stats = DuelStats(60, 15)

    @Test fun strikeDamagesBothCards() {
        val next = DuelState(60, 60).play(stats, stats)
        assertEquals(DuelState(45, 45, 2, 2), next)
    }

    @Test fun knockoutPreventsCounterattackAndFurtherTurns() {
        val next = DuelState(10, 10).play(stats, stats)
        assertEquals(10, next.playerHealth)
        assertEquals(0, next.rivalHealth)
        assertTrue(next.finished)
        assertEquals(next, next.play(stats, stats))
    }

    @Test fun recoveryCapsHealthAndStillAllowsRivalAttack() {
        val next = DuelState(55, 60).play(stats, stats, recover = true)
        assertEquals(DuelState(45, 60, 1, 2), next)
    }

    @Test fun exhaustedRecoveryDoesNotAdvanceTurn() {
        val state = DuelState(30, 60, recoveries = 0)
        assertEquals(state, state.play(stats, stats, recover = true))
    }

    @Test fun damageNeverProducesNegativeHealth() {
        val next = DuelState(1, 60).play(stats, stats)
        assertEquals(0, next.playerHealth)
        assertTrue(next.finished)
    }
}
