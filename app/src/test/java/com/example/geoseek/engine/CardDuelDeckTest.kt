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

class CardDuelDeckTest {
    private fun battleCard(
        name: String,
        hp: Int = 60,
        attack: Int = 15,
        type: CardType = CardType.MISC,
    ) = DuelCard(name, maxHp = hp, attack = attack, cardType = type)

    private fun collectible(name: String, rarity: Rarity) =
        Card(obj = GameObject(name, rarity, points = 10), xpValue = 0)

    @Test
    fun twoCardDeckCanContainDuplicateCards() {
        val duplicate = battleCard("Twin")
        val state = DuelState(
            playerDeck = listOf(duplicate, duplicate),
            rivalDeck = listOf(battleCard("Rival"), battleCard("Rival")),
        )

        assertEquals(2, state.playerDeck.size)
        assertEquals(state.playerDeck[0], state.playerDeck[1])
    }

    @Test
    fun knockoutPromotesSecondCardAndDefeatWaitsUntilBothPlayerCardsAreDown() {
        val state = DuelState(
            playerDeck = listOf(battleCard("Player one", hp = 10, attack = 20), battleCard("Player two", hp = 10, attack = 20)),
            rivalDeck = listOf(battleCard("Rival one", hp = 10, attack = 10), battleCard("Rival two", hp = 100, attack = 10)),
        )

        val afterFirstTurn = state.play()

        assertTrue(afterFirstTurn.playerDeck[0].isKnockedOut)
        assertTrue(afterFirstTurn.rivalDeck[0].isKnockedOut)
        assertEquals(1, afterFirstTurn.playerActiveIndex)
        assertEquals(1, afterFirstTurn.rivalActiveIndex)
        assertFalse(afterFirstTurn.finished)

        val afterSecondTurn = afterFirstTurn.play()
        assertTrue(afterSecondTurn.playerDeck.all { it.isKnockedOut })
        assertEquals(DuelOutcome.DEFEAT, afterSecondTurn.outcome)
    }

    @Test
    fun victoryWaitsUntilBothOpponentCardsAreKnockedOut() {
        val state = DuelState(
            playerDeck = listOf(battleCard("Player one", attack = 50), battleCard("Player two", attack = 50)),
            rivalDeck = listOf(battleCard("Rival one", hp = 10, attack = 1), battleCard("Rival two", hp = 10, attack = 1)),
        )

        val afterFirstTurn = state.play()
        assertTrue(afterFirstTurn.rivalDeck[0].isKnockedOut)
        assertEquals(1, afterFirstTurn.rivalActiveIndex)
        assertFalse(afterFirstTurn.finished)

        val afterSecondTurn = afterFirstTurn.play()
        assertTrue(afterSecondTurn.rivalDeck.all { it.isKnockedOut })
        assertEquals(DuelOutcome.VICTORY, afterSecondTurn.outcome)
    }

    @Test
    fun swappingConsumesPlayerActionAndTheRivalAttacksImmediately() {
        val state = DuelState(
            playerDeck = listOf(battleCard("Active"), battleCard("Backup")),
            rivalDeck = listOf(battleCard("Rival", attack = 11)),
        )

        val next = state.swapCard(1)

        assertEquals(1, next.playerActiveIndex)
        assertEquals(60, next.rivalActiveCard.currentHp)
        assertEquals(49, next.playerActiveCard.currentHp)
        assertEquals(state.turn + 1, next.turn)
        assertTrue(next.statusMessage.startsWith("Swapped to Backup!"))
        assertTrue(next.statusMessage.contains("Enemy attacked for 11 damage"))
    }

    @Test
    fun knockedOutCardCannotBeSwappedInto() {
        val state = DuelState(
            playerDeck = listOf(battleCard("Active"), battleCard("Knocked out").copy(currentHp = 0)),
            rivalDeck = listOf(battleCard("Rival")),
        )

        assertSame(state, state.swapCard(1))
    }

    @Test
    fun opponentDeckMatchesPlayerRaritiesBySlot() {
        val playerDeck = listOf(collectible("Common player", Rarity.COMMON), collectible("Epic player", Rarity.EPIC))
        val available = listOf(collectible("Common rival", Rarity.COMMON), collectible("Epic rival", Rarity.EPIC))

        val opponentDeck = generateOpponentDeck(playerDeck, available, matchRarity = true, random = Random(7))

        assertEquals(playerDeck.size, opponentDeck.size)
        assertEquals(playerDeck.map { it.obj.rarity }, opponentDeck.map { it.obj.rarity })
    }
}
