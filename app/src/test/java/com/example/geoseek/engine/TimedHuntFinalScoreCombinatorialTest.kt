package com.example.geoseek.engine

import com.example.geoseek.engine.modes.TimedHuntMode
import com.example.geoseek.models.GameObject
import com.example.geoseek.models.Rarity
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * CMPT 325 Assignment 4 (Luke Schaefer) - COMBINATORIAL TESTING of [TimedHuntMode.calculateFinalScore].
 *
 * Specification under test:
 *   fun calculateFinalScore(state: GameSessionState): GameScoreResult
 *   - The round is a victory when every target has been found (foundObjects.size >= targetObjects.size).
 *   - basePoints = state.score.
 *   - bonusPoints = secondsRemaining * timeBonusMultiplier on a victory, otherwise 0.
 *   - totalScore = basePoints + bonusPoints.
 *   - One card per found object, each worth points * 2 XP.
 *   - totalXpEarned = sum of card XP + 50 on a victory, or + 10 otherwise.
 *
 * Factors and levels (full combination: 3 x 3 x 2 = 18 cases):
 *   F1 targets found:          NONE (0 of 3) | PARTIAL (2 of 3) | ALL (3 of 3)
 *   F2 secondsRemaining:       0 | 30 | 60
 *   F3 timeBonusMultiplier:    5 (default constructor) | 10
 *
 * Expected values are written out by hand (not computed with the formula) so the oracle is independent of the code.
 */
@RunWith(Parameterized::class)
class TimedHuntFinalScoreCombinatorialTest(
    private val found: Progress,
    private val secondsRemaining: Int,
    private val multiplier: Int,
    private val expectedBase: Int,
    private val expectedBonus: Int,
    private val expectedTotal: Int,
    private val expectedXp: Int,
    private val expectedCards: Int,
) {
    enum class Progress { NONE, PARTIAL, ALL }

    companion object {
        private val chair = GameObject("Chair", Rarity.COMMON, 10)
        private val plant = GameObject("Plant", Rarity.COMMON, 15)
        private val bicycle = GameObject("Bicycle", Rarity.RARE, 50)
        private val targets = listOf(chair, plant, bicycle)

        @JvmStatic
        @Parameterized.Parameters(name = "{index}: found={0}, secondsLeft={1}, multiplier={2}")
        fun cases(): Collection<Array<Any>> = listOf(
            //        found             secs mult  base bonus total  xp  cards
            arrayOf(Progress.NONE,      0,   5,    0,   0,    0,    10, 0),
            arrayOf(Progress.NONE,      0,   10,   0,   0,    0,    10, 0),
            arrayOf(Progress.NONE,      30,  5,    0,   0,    0,    10, 0),
            arrayOf(Progress.NONE,      30,  10,   0,   0,    0,    10, 0),
            arrayOf(Progress.NONE,      60,  5,    0,   0,    0,    10, 0),
            arrayOf(Progress.NONE,      60,  10,   0,   0,    0,    10, 0),
            arrayOf(Progress.PARTIAL,   0,   5,    25,  0,    25,   60, 2),
            arrayOf(Progress.PARTIAL,   0,   10,   25,  0,    25,   60, 2),
            arrayOf(Progress.PARTIAL,   30,  5,    25,  0,    25,   60, 2),
            arrayOf(Progress.PARTIAL,   30,  10,   25,  0,    25,   60, 2),
            arrayOf(Progress.PARTIAL,   60,  5,    25,  0,    25,   60, 2),
            arrayOf(Progress.PARTIAL,   60,  10,   25,  0,    25,   60, 2),
            arrayOf(Progress.ALL,       0,   5,    75,  0,    75,   200, 3),
            arrayOf(Progress.ALL,       0,   10,   75,  0,    75,   200, 3),
            arrayOf(Progress.ALL,       30,  5,    75,  150,  225,  200, 3),
            arrayOf(Progress.ALL,       30,  10,   75,  300,  375,  200, 3),
            arrayOf(Progress.ALL,       60,  5,    75,  300,  375,  200, 3),
            arrayOf(Progress.ALL,       60,  10,   75,  600,  675,  200, 3),
        )
    }

    @Test
    fun finalScoreMatchesSpecification() {
        val foundObjects = when (found) {
            Progress.NONE -> mutableListOf()
            Progress.PARTIAL -> mutableListOf(chair, plant)
            Progress.ALL -> mutableListOf(chair, plant, bicycle)
        }
        val state = GameSessionState(
            targetObjects = targets,
            foundObjects = foundObjects,
            durationSeconds = 60,
            secondsRemaining = secondsRemaining,
            score = foundObjects.sumOf { it.points },
        )
        val mode = if (multiplier == 5) TimedHuntMode() else TimedHuntMode(timeBonusMultiplier = multiplier)

        val result = mode.calculateFinalScore(state)

        assertEquals("basePoints", expectedBase, result.basePoints)
        assertEquals("bonusPoints", expectedBonus, result.bonusPoints)
        assertEquals("totalScore", expectedTotal, result.totalScore)
        assertEquals("totalXpEarned", expectedXp, result.totalXpEarned)
        assertEquals("cardsCollected", expectedCards, result.cardsCollected.size)
        assertEquals("cards match found objects", foundObjects, result.cardsCollected.map { it.obj })
    }
}
