package com.example.geoseek.engine

import com.example.geoseek.engine.modes.CollectionMode
import com.example.geoseek.models.Card
import com.example.geoseek.models.GameObject
import com.example.geoseek.models.Rarity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit test suite for CollectionMode based on the formal test specification.
 *
 * Demonstrates the three testing techniques:
 * 1. Equivalence Class Partitioning
 * 2. Boundary Value Testing
 * 3. Combinatorial Testing
 */
class CollectionModeTest {

    private val commonChair = GameObject("Chair", Rarity.COMMON, 10)
    private val uncommonPlant = GameObject("Plant", Rarity.UNCOMMON, 20)
    private val rareBicycle = GameObject("Bicycle", Rarity.RARE, 40)
    private val epicPainting = GameObject("Painting", Rarity.EPIC, 100)
    private val legendaryStatue = GameObject("Statue", Rarity.LEGENDARY, 200)

    // =========================================================================
    // Technique 1: Equivalence Class Partitioning (ECP)
    // =========================================================================

    @Test
    fun testEvaluateFoundObject_EP1_DuplicateInSession() {
        val mode = CollectionMode()
        val state = GameSessionState(targetObjects = listOf(commonChair), durationSeconds = 0)
        state.foundObjects.add(commonChair)

        val outcome = mode.evaluateFoundObject(commonChair, state)

        assertEquals(ObjectFindResult.DUPLICATE, outcome.result)
        assertEquals(0, outcome.pointsEarned)
        assertNull(outcome.cardEarned)
        assertEquals(1, state.foundObjects.size)
        assertEquals(0, state.score)
    }

    @Test
    fun testEvaluateFoundObject_EP2_NewInSession() {
        val mode = CollectionMode()
        val state = GameSessionState(targetObjects = listOf(commonChair), durationSeconds = 0)

        val outcome = mode.evaluateFoundObject(commonChair, state)

        assertEquals(ObjectFindResult.ACCEPTED, outcome.result)
        assertEquals(60, outcome.pointsEarned) // (10 * 1.0) + 50 = 60
        assertEquals(60, state.score)
        assertEquals(1, state.foundObjects.size)
        assertTrue(outcome.cardEarned?.isNewDiscovery ?: false)
    }

    @Test
    fun testEvaluateFoundObject_EP3_ExistingInBinder() {
        val existingCard = Card(obj = commonChair, xpValue = 20, isNewDiscovery = false)
        val mode = CollectionMode(existingCardCollection = listOf(existingCard))
        val state = GameSessionState(targetObjects = listOf(commonChair), durationSeconds = 0)

        val outcome = mode.evaluateFoundObject(commonChair, state)

        assertEquals(ObjectFindResult.ACCEPTED, outcome.result)
        assertEquals(20, outcome.pointsEarned) // (10 * 1.0) + 10 = 20
        assertFalse(outcome.cardEarned?.isNewDiscovery ?: true)
    }

    @Test
    fun testEvaluateFoundObject_EP4_NewDiscoveryNotInBinder() {
        val mode = CollectionMode(existingCardCollection = emptyList())
        val state = GameSessionState(targetObjects = listOf(uncommonPlant), durationSeconds = 0)

        val outcome = mode.evaluateFoundObject(uncommonPlant, state)

        assertEquals(ObjectFindResult.ACCEPTED, outcome.result)
        assertEquals(80, outcome.pointsEarned) // (20 * 1.5) + 50 = 80
        assertTrue(outcome.cardEarned?.isNewDiscovery ?: false)
    }

    @Test
    fun testEvaluateFoundObject_EP5_CaseInsensitiveNameMatching() {
        val lowercaseChair = GameObject("chair", Rarity.COMMON, 10)
        val uppercaseChair = GameObject("CHAIR", Rarity.COMMON, 10)

        val mode = CollectionMode()
        val state = GameSessionState(targetObjects = listOf(lowercaseChair), durationSeconds = 0)
        state.foundObjects.add(lowercaseChair)

        val outcome = mode.evaluateFoundObject(uppercaseChair, state)

        assertEquals(ObjectFindResult.DUPLICATE, outcome.result)
        assertEquals(0, outcome.pointsEarned)
    }

    // =========================================================================
    // Technique 2: Boundary Value Testing (BVT)
    // =========================================================================

    @Test
    fun testEvaluateGameStatus_BVT1_ZeroFound() {
        val targets = listOf(commonChair, uncommonPlant, rareBicycle)
        val mode = CollectionMode()
        val state = GameSessionState(targetObjects = targets, durationSeconds = 0)

        assertEquals(GameStatus.IN_PROGRESS, mode.evaluateGameStatus(state))
    }

    @Test
    fun testEvaluateGameStatus_BVT2_BelowBoundary() {
        val targets = listOf(commonChair, uncommonPlant, rareBicycle)
        val mode = CollectionMode()
        val state = GameSessionState(targetObjects = targets, durationSeconds = 0)
        state.foundObjects.add(commonChair)
        state.foundObjects.add(uncommonPlant) // 2 found out of 3 targets (T - 1)

        assertEquals(GameStatus.IN_PROGRESS, mode.evaluateGameStatus(state))
    }

    @Test
    fun testEvaluateGameStatus_BVT3_OnPointBoundary() {
        val targets = listOf(commonChair, uncommonPlant, rareBicycle)
        val mode = CollectionMode()
        val state = GameSessionState(targetObjects = targets, durationSeconds = 0)
        state.foundObjects.add(commonChair)
        state.foundObjects.add(uncommonPlant)
        state.foundObjects.add(rareBicycle) // 3 found out of 3 targets (T)

        assertEquals(GameStatus.VICTORY, mode.evaluateGameStatus(state))
    }

    @Test
    fun testEvaluateGameStatus_BVT4_AboveBoundary() {
        val targets = listOf(commonChair, uncommonPlant, rareBicycle)
        val mode = CollectionMode()
        val state = GameSessionState(targetObjects = targets, durationSeconds = 0)
        state.foundObjects.addAll(listOf(commonChair, uncommonPlant, rareBicycle, epicPainting)) // 4 found out of 3 targets (T + 1)

        assertEquals(GameStatus.VICTORY, mode.evaluateGameStatus(state))
    }

    @Test
    fun testEvaluateGameStatus_BVT5_EmptyTargetListZeroFound() {
        val mode = CollectionMode()
        val state = GameSessionState(targetObjects = emptyList(), durationSeconds = 0)

        assertEquals(GameStatus.IN_PROGRESS, mode.evaluateGameStatus(state))
    }

    @Test
    fun testEvaluateGameStatus_BVT6_EmptyTargetListNonZeroFound() {
        val mode = CollectionMode()
        val state = GameSessionState(targetObjects = emptyList(), durationSeconds = 0)
        state.foundObjects.add(commonChair)

        assertEquals(GameStatus.IN_PROGRESS, mode.evaluateGameStatus(state))
    }

    // =========================================================================
    // Technique 3: Combinatorial Testing (CT-01 to CT-13)
    // =========================================================================

    @Test
    fun testCombinatorial_CT01_Common_NotInBinder_NotInSession() {
        val mode = CollectionMode()
        val state = GameSessionState(targetObjects = emptyList(), durationSeconds = 0)

        val outcome = mode.evaluateFoundObject(commonChair, state) // 10 pts

        assertEquals(ObjectFindResult.ACCEPTED, outcome.result)
        assertEquals(1.0f, outcome.comboMultiplier, 0.001f)
        assertEquals(60, outcome.pointsEarned) // (10 * 1.0) + 50 = 60
    }

    @Test
    fun testCombinatorial_CT02_Common_InBinder_NotInSession() {
        val binderCard = Card(obj = commonChair, xpValue = 10)
        val mode = CollectionMode(existingCardCollection = listOf(binderCard))
        val state = GameSessionState(targetObjects = emptyList(), durationSeconds = 0)

        val outcome = mode.evaluateFoundObject(commonChair, state)

        assertEquals(ObjectFindResult.ACCEPTED, outcome.result)
        assertEquals(1.0f, outcome.comboMultiplier, 0.001f)
        assertEquals(20, outcome.pointsEarned) // (10 * 1.0) + 10 = 20
    }

    @Test
    fun testCombinatorial_CT03_Common_AnyBinder_InSession() {
        val mode = CollectionMode()
        val state = GameSessionState(targetObjects = emptyList(), durationSeconds = 0)
        state.foundObjects.add(commonChair)

        val outcome = mode.evaluateFoundObject(commonChair, state)

        assertEquals(ObjectFindResult.DUPLICATE, outcome.result)
        assertEquals(0, outcome.pointsEarned)
    }

    @Test
    fun testCombinatorial_CT04_Uncommon_NotInBinder_NotInSession() {
        val mode = CollectionMode()
        val state = GameSessionState(targetObjects = emptyList(), durationSeconds = 0)

        val outcome = mode.evaluateFoundObject(uncommonPlant, state) // 20 pts * 1.5 = 30 + 50 = 80

        assertEquals(ObjectFindResult.ACCEPTED, outcome.result)
        assertEquals(1.5f, outcome.comboMultiplier, 0.001f)
        assertEquals(80, outcome.pointsEarned)
    }

    @Test
    fun testCombinatorial_CT05_Uncommon_InBinder_NotInSession() {
        val binderCard = Card(obj = uncommonPlant, xpValue = 20)
        val mode = CollectionMode(existingCardCollection = listOf(binderCard))
        val state = GameSessionState(targetObjects = emptyList(), durationSeconds = 0)

        val outcome = mode.evaluateFoundObject(uncommonPlant, state) // 20 pts * 1.5 = 30 + 10 = 40

        assertEquals(ObjectFindResult.ACCEPTED, outcome.result)
        assertEquals(1.5f, outcome.comboMultiplier, 0.001f)
        assertEquals(40, outcome.pointsEarned)
    }

    @Test
    fun testCombinatorial_CT06_Uncommon_AnyBinder_InSession() {
        val mode = CollectionMode()
        val state = GameSessionState(targetObjects = emptyList(), durationSeconds = 0)
        state.foundObjects.add(uncommonPlant)

        val outcome = mode.evaluateFoundObject(uncommonPlant, state)

        assertEquals(ObjectFindResult.DUPLICATE, outcome.result)
        assertEquals(0, outcome.pointsEarned)
    }

    @Test
    fun testCombinatorial_CT07_Rare_NotInBinder_NotInSession() {
        val mode = CollectionMode()
        val state = GameSessionState(targetObjects = emptyList(), durationSeconds = 0)

        val outcome = mode.evaluateFoundObject(rareBicycle, state) // 40 pts * 2.5 = 100 + 50 = 150

        assertEquals(ObjectFindResult.ACCEPTED, outcome.result)
        assertEquals(2.5f, outcome.comboMultiplier, 0.001f)
        assertEquals(150, outcome.pointsEarned)
    }

    @Test
    fun testCombinatorial_CT08_Rare_InBinder_NotInSession() {
        val binderCard = Card(obj = rareBicycle, xpValue = 40)
        val mode = CollectionMode(existingCardCollection = listOf(binderCard))
        val state = GameSessionState(targetObjects = emptyList(), durationSeconds = 0)

        val outcome = mode.evaluateFoundObject(rareBicycle, state) // 40 pts * 2.5 = 100 + 10 = 110

        assertEquals(ObjectFindResult.ACCEPTED, outcome.result)
        assertEquals(2.5f, outcome.comboMultiplier, 0.001f)
        assertEquals(110, outcome.pointsEarned)
    }

    @Test
    fun testCombinatorial_CT09_Epic_NotInBinder_NotInSession() {
        val mode = CollectionMode()
        val state = GameSessionState(targetObjects = emptyList(), durationSeconds = 0)

        val outcome = mode.evaluateFoundObject(epicPainting, state) // 100 pts * 4.0 = 400 + 50 = 450

        assertEquals(ObjectFindResult.ACCEPTED, outcome.result)
        assertEquals(4.0f, outcome.comboMultiplier, 0.001f)
        assertEquals(450, outcome.pointsEarned)
    }

    @Test
    fun testCombinatorial_CT10_Epic_InBinder_NotInSession() {
        val binderCard = Card(obj = epicPainting, xpValue = 100)
        val mode = CollectionMode(existingCardCollection = listOf(binderCard))
        val state = GameSessionState(targetObjects = emptyList(), durationSeconds = 0)

        val outcome = mode.evaluateFoundObject(epicPainting, state) // 100 pts * 4.0 = 400 + 10 = 410

        assertEquals(ObjectFindResult.ACCEPTED, outcome.result)
        assertEquals(4.0f, outcome.comboMultiplier, 0.001f)
        assertEquals(410, outcome.pointsEarned)
    }

    @Test
    fun testCombinatorial_CT11_Legendary_NotInBinder_NotInSession() {
        val mode = CollectionMode()
        val state = GameSessionState(targetObjects = emptyList(), durationSeconds = 0)

        val outcome = mode.evaluateFoundObject(legendaryStatue, state) // 200 pts * 8.0 = 1600 + 50 = 1650

        assertEquals(ObjectFindResult.ACCEPTED, outcome.result)
        assertEquals(8.0f, outcome.comboMultiplier, 0.001f)
        assertEquals(1650, outcome.pointsEarned)
    }

    @Test
    fun testCombinatorial_CT12_Legendary_InBinder_NotInSession() {
        val binderCard = Card(obj = legendaryStatue, xpValue = 200)
        val mode = CollectionMode(existingCardCollection = listOf(binderCard))
        val state = GameSessionState(targetObjects = emptyList(), durationSeconds = 0)

        val outcome = mode.evaluateFoundObject(legendaryStatue, state) // 200 pts * 8.0 = 1600 + 10 = 1610

        assertEquals(ObjectFindResult.ACCEPTED, outcome.result)
        assertEquals(8.0f, outcome.comboMultiplier, 0.001f)
        assertEquals(1610, outcome.pointsEarned)
    }

    @Test
    fun testCombinatorial_CT13_Legendary_AnyBinder_InSession() {
        val mode = CollectionMode()
        val state = GameSessionState(targetObjects = emptyList(), durationSeconds = 0)
        state.foundObjects.add(legendaryStatue)

        val outcome = mode.evaluateFoundObject(legendaryStatue, state)

        assertEquals(ObjectFindResult.DUPLICATE, outcome.result)
        assertEquals(0, outcome.pointsEarned)
    }

    // =========================================================================
    // Technique 4: Additional Score Calculation Tests (FS-1 to FS-4)
    // =========================================================================

    @Test
    fun testCalculateFinalScore_FS1_EmptySession() {
        val mode = CollectionMode()
        val state = GameSessionState(targetObjects = emptyList(), durationSeconds = 0)

        val scoreResult = mode.calculateFinalScore(state)

        assertEquals(0, scoreResult.basePoints)
        assertEquals(0, scoreResult.bonusPoints)
        assertEquals(0, scoreResult.totalScore)
        assertEquals(0, scoreResult.totalXpEarned)
        assertTrue(scoreResult.cardsCollected.isEmpty())
    }

    @Test
    fun testCalculateFinalScore_FS2_AllNewDiscoveries() {
        val mode = CollectionMode(existingCardCollection = emptyList())
        val state = GameSessionState(targetObjects = emptyList(), durationSeconds = 0)
        state.foundObjects.addAll(listOf(commonChair, uncommonPlant))
        state.score = 140

        val scoreResult = mode.calculateFinalScore(state)

        assertEquals(140, scoreResult.basePoints)
        assertEquals(50, scoreResult.bonusPoints) // 2 new discoveries * 25 = 50
        assertEquals(190, scoreResult.totalScore)
        assertEquals(2, scoreResult.cardsCollected.size)
        assertTrue(scoreResult.cardsCollected.all { it.isNewDiscovery })
    }

    @Test
    fun testCalculateFinalScore_FS3_MixedDiscoveries() {
        val existingCard1 = Card(obj = commonChair, xpValue = 10)
        val existingCard2 = Card(obj = uncommonPlant, xpValue = 20)
        val mode = CollectionMode(existingCardCollection = listOf(existingCard1, existingCard2))

        val state = GameSessionState(targetObjects = emptyList(), durationSeconds = 0)
        state.foundObjects.addAll(listOf(commonChair, uncommonPlant, rareBicycle))
        state.score = 210

        val scoreResult = mode.calculateFinalScore(state)

        assertEquals(210, scoreResult.basePoints)
        assertEquals(25, scoreResult.bonusPoints) // Only 1 new discovery (rareBicycle) * 25 = 25
        assertEquals(235, scoreResult.totalScore)
        assertEquals(3, scoreResult.cardsCollected.size)
    }

    @Test
    fun testCalculateFinalScore_FS4_NoNewDiscoveries() {
        val existingCard = Card(obj = commonChair, xpValue = 10)
        val mode = CollectionMode(existingCardCollection = listOf(existingCard))

        val state = GameSessionState(targetObjects = emptyList(), durationSeconds = 0)
        state.foundObjects.add(commonChair)
        state.score = 100

        val scoreResult = mode.calculateFinalScore(state)

        assertEquals(100, scoreResult.basePoints)
        assertEquals(0, scoreResult.bonusPoints) // 0 new discoveries * 25 = 0
        assertEquals(100, scoreResult.totalScore)
        assertEquals(1, scoreResult.cardsCollected.size)
        assertFalse(scoreResult.cardsCollected[0].isNewDiscovery)
    }
}
