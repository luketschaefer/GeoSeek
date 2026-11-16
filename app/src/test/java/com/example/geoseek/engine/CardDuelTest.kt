package com.example.geoseek.engine

import com.example.geoseek.models.Card
import com.example.geoseek.models.CardType
import com.example.geoseek.models.GameObject
import com.example.geoseek.models.Rarity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class CardDuelTest {
    private fun card(
        name: String,
        hp: Int = 60,
        attack: Int = 15,
        type: CardType = CardType.MISC,
    ) = DuelCard(name, maxHp = hp, attack = attack, cardType = type)

    private fun collectible(name: String, rarity: Rarity, points: Int = 10) =
        Card(obj = GameObject(name, rarity, points), xpValue = 0)

    @Test
    fun neutralStrikeDamagesBothActiveCards() {
        val state = DuelState(listOf(card("Player")), listOf(card("Rival")))
        val next = state.play()

        assertEquals(45, next.playerActiveCard.currentHp)
        assertEquals(45, next.rivalActiveCard.currentHp)
        assertEquals(2, next.turn)
        assertTrue(next.statusMessage.contains("Neutral"))
    }

    @Test
    fun typeMatchupsScaleAndReportBothHits() {
        val state = DuelState(
            listOf(card("Tech", type = CardType.TECH)),
            listOf(card("Misc", type = CardType.MISC)),
        )
        val next = state.play()

        assertEquals(37, next.rivalActiveCard.currentHp)
        assertEquals(49, next.playerActiveCard.currentHp)
        assertTrue(next.statusMessage.contains("Super Effective"))
        assertTrue(next.statusMessage.contains("Not Very Effective"))
    }

    @Test
    fun knockoutAdvancesToNextAliveCardAndBattleContinues() {
        val state = DuelState(
            playerDeck = listOf(card("Player")),
            rivalDeck = listOf(card("First rival", hp = 10), card("Second rival")),
        )
        val next = state.play()

        assertTrue(next.rivalDeck[0].isKnockedOut)
        assertEquals(1, next.rivalActiveIndex)
        assertEquals("Second rival", next.rivalActiveCard.name)
        assertFalse(next.finished)
    }

    @Test
    fun playerKnockoutAdvancesActiveCardAndReceivesCounterattack() {
        val state = DuelState(
            playerDeck = listOf(card("First player", hp = 10), card("Second player")),
            rivalDeck = listOf(card("Rival")),
        )
        val next = state.play()

        assertTrue(next.playerDeck[0].isKnockedOut)
        assertEquals(1, next.playerActiveIndex)
        assertEquals(60, next.playerActiveCard.currentHp)
        assertFalse(next.finished)
    }

    @Test
    fun knockingOutFinalRivalCardWinsWithoutCounterattack() {
        val state = DuelState(
            playerDeck = listOf(card("Player", attack = 100)),
            rivalDeck = listOf(card("Last rival", hp = 20)),
        )
        val next = state.play()

        assertEquals(DuelOutcome.VICTORY, next.outcome)
        assertEquals(60, next.playerActiveCard.currentHp)
        assertTrue(next.finished)
        assertSame(next, next.play())
    }

    @Test
    fun swappingConsumesTurnAndRivalImmediatelyAttacks() {
        val state = DuelState(
            playerDeck = listOf(card("Active"), card("Backup", hp = 50)),
            rivalDeck = listOf(card("Rival")),
        )
        val next = state.swapCard(1)

        assertEquals(1, next.playerActiveIndex)
        assertEquals(60, next.playerDeck[0].currentHp)
        assertEquals(35, next.playerActiveCard.currentHp)
        assertEquals(60, next.rivalActiveCard.currentHp)
        assertEquals(2, next.turn)
        assertTrue(next.statusMessage.startsWith("Swapped to Backup!"))
        assertTrue(next.statusMessage.contains("Enemy attacked for 15 damage"))
    }

    @Test
    fun swappingToInvalidOrKnockedOutCardDoesNothing() {
        val state = DuelState(
            playerDeck = listOf(card("Active"), card("Knocked out").copy(currentHp = 0)),
            rivalDeck = listOf(card("Rival")),
        )

        assertSame(state, state.swapCard(-1))
        assertSame(state, state.swapCard(0))
        assertSame(state, state.swapCard(1))
        assertSame(state, state.swapCard(2))
    }

    @Test
    fun recoveryConsumesTurnAndCannotExceedMaxHp() {
        val state = DuelState(
            playerDeck = listOf(card("Player", hp = 60).copy(currentHp = 55)),
            rivalDeck = listOf(card("Rival")),
        )
        val next = state.play(recover = true)

        assertEquals(45, next.playerActiveCard.currentHp)
        assertEquals(60, next.rivalActiveCard.currentHp)
        assertEquals(1, next.recoveries)
        assertEquals(2, next.turn)
    }

    @Test
    fun exhaustedRecoveryDoesNotAdvanceTurn() {
        val state = DuelState(listOf(card("Player")), listOf(card("Rival")), recoveries = 0)
        assertSame(state, state.play(recover = true))
    }

    @Test
    fun opponentDeckMatchesSlotRarityAndUsesNearestTierWhenExactPoolRunsOut() {
        val playerDeck = listOf(
            collectible("Player common", Rarity.COMMON),
            collectible("Player rare", Rarity.RARE, points = 20),
        )
        val available = listOf(
            collectible("Rival common", Rarity.COMMON),
            collectible("Rival epic", Rarity.EPIC, points = 20),
        )

        val opponentDeck = generateOpponentDeck(playerDeck, available, matchRarity = true, random = Random(1))

        assertEquals(playerDeck.size, opponentDeck.size)
        assertEquals(Rarity.COMMON, opponentDeck[0].obj.rarity)
        assertEquals(Rarity.EPIC, opponentDeck[1].obj.rarity)
        assertEquals(playerDeck[0].obj.duelStats().health, opponentDeck[0].obj.duelStats().health)
        assertEquals(playerDeck[0].obj.duelStats().attack, opponentDeck[0].obj.duelStats().attack)
    }

    @Test
    fun opponentDeckAllowsDuplicatesWhenPlayerDeckAndAvailablePoolHaveOneCard() {
        val onlyCard = collectible("Only card", Rarity.UNCOMMON)
        val playerDeck = listOf(onlyCard, onlyCard)

        val opponentDeck = generateOpponentDeck(playerDeck, emptyList(), matchRarity = true, random = Random(2))

        assertEquals(2, opponentDeck.size)
        assertTrue(opponentDeck.all { it.id == onlyCard.id })
        assertTrue(opponentDeck.all { it.obj.rarity == Rarity.UNCOMMON })
    }

    @Test
    fun unmatchedOpponentDeckSelectsFromAnyAvailableRarity() {
        val playerDeck = listOf(collectible("Player", Rarity.EPIC), collectible("Player copy", Rarity.EPIC))
        val available = listOf(
            collectible("Common rival", Rarity.COMMON),
            collectible("Legendary rival", Rarity.LEGENDARY),
            collectible("Rare rival", Rarity.RARE),
        )

        val opponentDeck = generateOpponentDeck(playerDeck, available, matchRarity = false, random = Random(3))

        assertEquals(playerDeck.size, opponentDeck.size)
        assertTrue(opponentDeck.all { rival -> available.any { it.id == rival.id } })
    }
}
