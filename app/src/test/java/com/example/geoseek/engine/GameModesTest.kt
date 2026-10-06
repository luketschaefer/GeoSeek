package com.example.geoseek.engine

import com.example.geoseek.engine.modes.ClassicTimedMode
import com.example.geoseek.engine.modes.CollectionMode
import com.example.geoseek.engine.modes.RarityHunterMode
import com.example.geoseek.engine.modes.ScavengerStreakMode
import com.example.geoseek.engine.modes.TimedHuntMode
import com.example.geoseek.managers.XPManager
import com.example.geoseek.models.Card
import com.example.geoseek.models.GameObject
import com.example.geoseek.models.Rarity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GameModesTest {

    private val chair = GameObject("Chair", Rarity.COMMON, 10)
    private val plant = GameObject("Plant", Rarity.UNCOMMON, 25)
    private val bicycle = GameObject("Bicycle", Rarity.RARE, 50)
    private val targetList = listOf(chair, plant, bicycle)

    @Test
    fun testTimedHuntModeTimerAndScoring() {
        val round = HuntRound(
            environment = "Living Room",
            targets = targetList,
            durationSeconds = 60,
            mode = TimedHuntMode(timeLimitSeconds = 60),
        )

        assertFalse(round.isOver())
        assertEquals("Timed Mode", round.mode.name)
        assertEquals(60, round.secondsLeft)

        // Find Chair
        val (result1, points1) = round.onObjectFound(chair)
        assertEquals(ObjectFindResult.ACCEPTED, result1)
        assertEquals(10, points1)
        assertEquals(1, round.found.size)

        // Duplicate
        val duplicateOutcome = round.onObjectFound(chair)
        assertEquals(ObjectFindResult.DUPLICATE, duplicateOutcome.result)

        // Find remaining
        round.onObjectFound(plant)
        round.onObjectFound(bicycle)

        assertTrue(round.isOver())
        assertEquals(GameStatus.VICTORY, round.state.status)

        val finalScore = round.finish()
        assertEquals(85, finalScore.basePoints)
        assertEquals(300, finalScore.bonusPoints) // 60s * 5 = 300
        assertEquals(385, finalScore.totalScore)
        assertEquals(3, finalScore.cardsCollected.size)
    }

    @Test
    fun testClassicTimedAndStreakAndRarityModes() {
        val classicRound = HuntRound(
            environment = "Room",
            targets = targetList,
            mode = ClassicTimedMode(),
        )
        assertEquals("Classic Timed", classicRound.mode.name)

        val streakRound = HuntRound(
            environment = "Room",
            targets = targetList,
            mode = ScavengerStreakMode(),
        )
        assertEquals("Scavenger Streak", streakRound.mode.name)

        val rarityRound = HuntRound(
            environment = "Room",
            targets = targetList,
            mode = RarityHunterMode(),
        )
        assertEquals("Rarity Hunter", rarityRound.mode.name)

        val gameOverState = GameSessionState(targetObjects = emptyList(), durationSeconds = 10, status = GameStatus.GAME_OVER)
        assertEquals(GameStatus.GAME_OVER, gameOverState.status)
    }

    @Test
    fun testTimedHuntModeExpiration() {
        val round = HuntRound(
            environment = "Garage",
            targets = targetList,
            durationSeconds = 10,
            mode = TimedHuntMode(timeLimitSeconds = 10),
        )

        round.tickTime(10)
        assertTrue(round.isOver())
        assertEquals(GameStatus.TIME_UP, round.state.status)
    }

    @Test
    fun testCollectionModeTradingCardsAndXP() {
        val existingCard = Card(obj = chair, xpValue = 20, isNewDiscovery = false)
        val mode = CollectionMode(existingCardCollection = listOf(existingCard))

        val round = HuntRound(
            environment = "Park",
            targets = targetList,
            durationSeconds = 0,
            mode = mode,
        )

        assertEquals("Collection Mode", mode.name)

        // Found chair (existing in binder)
        val chairOutcome = round.onObjectFound(chair)
        assertEquals(ObjectFindResult.ACCEPTED, chairOutcome.result)
        assertNotNull(chairOutcome.cardEarned)
        assertFalse(chairOutcome.cardEarned?.isNewDiscovery ?: true)

        // Found plant (new discovery!)
        val plantOutcome = round.onObjectFound(plant)
        assertEquals(ObjectFindResult.ACCEPTED, plantOutcome.result)
        assertNotNull(plantOutcome.cardEarned)
        assertTrue(plantOutcome.cardEarned?.isNewDiscovery ?: false)

        val xpManager = XPManager()
        xpManager.addXp(plantOutcome.cardEarned?.xpValue ?: 0)
        assertTrue(xpManager.xp > 0)
        assertEquals(1, xpManager.level)
        assertEquals(100, xpManager.xpForNextLevel)
        assertTrue(xpManager.progressToNextLevel >= 0f)
    }
}
