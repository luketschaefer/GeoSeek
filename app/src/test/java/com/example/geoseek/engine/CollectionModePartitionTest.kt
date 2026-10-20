package com.example.geoseek.engine

import com.example.geoseek.engine.modes.CollectionMode
import com.example.geoseek.models.Card
import com.example.geoseek.models.GameObject
import com.example.geoseek.models.Rarity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * CMPT 325 Assignment 4 (Luke Schaefer) - EQUIVALENCE CLASS PARTITIONING of [CollectionMode.evaluateFoundObject].
 *
 * Specification under test:
 *   fun evaluateFoundObject(foundObject: GameObject, state: GameSessionState): GameRuleOutcome
 *   - If an object with the same name (ignoring case) is already in state.foundObjects, the find is a DUPLICATE:
 *     0 points, no card, and state is not changed.
 *   - Otherwise the find is ACCEPTED (whether or not it is one of state.targetObjects), it is appended to
 *     state.foundObjects, and the player earns
 *         xp = floor(points * rarityMultiplier) + discoveryBonus
 *     where rarityMultiplier is COMMON 1.0, UNCOMMON 1.5, RARE 2.5, EPIC 4.0, LEGENDARY 8.0 and discoveryBonus is
 *     50 if no card for that object name (ignoring case) is in the player's binder, or 10 if one is.
 *   - xp is added to state.score and returned as pointsEarned; the outcome carries the multiplier and a Card
 *     with xpValue = xp and isNewDiscovery = (not in binder).
 *
 * Partitions (one representative per class; see report for rationale):
 *   P1 session history:  S1 already found this session  | S2 not yet found
 *   P2 binder:           B1 not in binder (new card)     | B2 already in binder
 *   P3 rarity:           COMMON | UNCOMMON | RARE | EPIC | LEGENDARY
 *   P4 target list:      T1 object is a target           | T2 object is not a target
 *   P5 name case:        C1 same case                    | C2 different case
 */
class CollectionModePartitionTest {

    private val chair = GameObject("Chair", Rarity.COMMON, 10)
    private val lamp = GameObject("Lamp", Rarity.UNCOMMON, 20)
    private val clock = GameObject("Clock", Rarity.RARE, 35)
    private val dog = GameObject("Dog", Rarity.EPIC, 75)
    private val airPods = GameObject("AirPods", Rarity.LEGENDARY, 100)

    private fun newState(vararg alreadyFound: GameObject) = GameSessionState(
        targetObjects = listOf(chair, lamp),
        foundObjects = alreadyFound.toMutableList(),
        durationSeconds = 0,
    )

    private fun binderWith(vararg objs: GameObject) = objs.map { Card(obj = it, xpValue = 0, isNewDiscovery = false) }

    // ---------- P1: session history ----------

    @Test
    fun s1_alreadyFoundThisSession_isDuplicateAndStateUnchanged() {
        val state = newState(chair)
        val outcome = CollectionMode().evaluateFoundObject(chair, state)

        assertEquals(ObjectFindResult.DUPLICATE, outcome.result)
        assertEquals(0, outcome.pointsEarned)
        assertNull(outcome.cardEarned)
        assertEquals(0, state.score)
        assertEquals(1, state.foundObjects.size)
    }

    @Test
    fun s1_alreadyFoundThisSessionAndInBinder_isStillDuplicate() {
        val state = newState(chair)
        val outcome = CollectionMode(binderWith(chair)).evaluateFoundObject(chair, state)
        assertEquals(ObjectFindResult.DUPLICATE, outcome.result)
        assertEquals(0, outcome.pointsEarned)
    }

    @Test
    fun s2_notYetFound_isAcceptedAndRecorded() {
        val state = newState(lamp)
        val outcome = CollectionMode().evaluateFoundObject(chair, state)

        assertEquals(ObjectFindResult.ACCEPTED, outcome.result)
        assertEquals(listOf(lamp, chair), state.foundObjects)
        assertEquals(outcome.pointsEarned, state.score)
    }

    // ---------- P2: binder ----------

    @Test
    fun b1_notInBinder_earnsNewDiscoveryBonus() {
        val state = newState()
        val outcome = CollectionMode(binderWith(lamp)).evaluateFoundObject(chair, state)

        assertEquals(60, outcome.pointsEarned) // 10 * 1.0 + 50
        val card = outcome.cardEarned
        assertNotNull(card)
        assertTrue(card!!.isNewDiscovery)
        assertEquals(60, card.xpValue)
        assertEquals(chair, card.obj)
    }

    @Test
    fun b2_alreadyInBinder_earnsRepeatBonusOnly() {
        val state = newState()
        val outcome = CollectionMode(binderWith(chair)).evaluateFoundObject(chair, state)

        assertEquals(ObjectFindResult.ACCEPTED, outcome.result)
        assertEquals(20, outcome.pointsEarned) // 10 * 1.0 + 10
        assertFalse(outcome.cardEarned!!.isNewDiscovery)
        assertEquals(20, state.score)
    }

    // ---------- P3: rarity (one representative per tier, all new discoveries) ----------

    private fun assertRarity(obj: GameObject, expectedMultiplier: Float, expectedXp: Int) {
        val outcome = CollectionMode().evaluateFoundObject(obj, newState())
        assertEquals(ObjectFindResult.ACCEPTED, outcome.result)
        assertEquals(expectedMultiplier, outcome.comboMultiplier, 0.0001f)
        assertEquals(expectedXp, outcome.pointsEarned)
    }

    @Test fun r_common() = assertRarity(chair, 1.0f, 60)       // 10 * 1.0  + 50
    @Test fun r_uncommon() = assertRarity(lamp, 1.5f, 80)      // 20 * 1.5  + 50
    @Test fun r_rare_truncatesFraction() = assertRarity(clock, 2.5f, 137) // 35 * 2.5 = 87.5 -> 87, + 50
    @Test fun r_epic() = assertRarity(dog, 4.0f, 350)          // 75 * 4.0  + 50
    @Test fun r_legendary() = assertRarity(airPods, 8.0f, 850) // 100 * 8.0 + 50

    // ---------- P4: target list ----------

    @Test
    fun t2_objectNotInTargetList_isStillAccepted() {
        val state = newState()
        val outcome = CollectionMode().evaluateFoundObject(dog, state) // targets are chair + lamp
        assertEquals(ObjectFindResult.ACCEPTED, outcome.result)
        assertTrue(state.foundObjects.contains(dog))
    }

    // ---------- P5: name case ----------

    @Test
    fun c2_sessionMatchIgnoresCase() {
        val state = newState(chair)
        val outcome = CollectionMode().evaluateFoundObject(GameObject("CHAIR", Rarity.COMMON, 10), state)
        assertEquals(ObjectFindResult.DUPLICATE, outcome.result)
    }

    @Test
    fun c2_binderMatchIgnoresCase() {
        val binder = binderWith(GameObject("chair", Rarity.COMMON, 10))
        val outcome = CollectionMode(binder).evaluateFoundObject(chair, newState())
        assertFalse(outcome.cardEarned!!.isNewDiscovery)
        assertEquals(20, outcome.pointsEarned)
    }
}
