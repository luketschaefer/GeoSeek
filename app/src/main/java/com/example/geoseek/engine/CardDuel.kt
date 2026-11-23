package com.example.geoseek.engine

import com.example.geoseek.models.Card
import com.example.geoseek.models.CardType
import com.example.geoseek.models.GameObject
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.random.Random

/** Local practice rules. Battles never consume collected cards or change account progress. */
data class DuelStats(
    val health: Int,
    val attack: Int,
    val cardType: CardType = CardType.MISC,
    val name: String = "Card",
)

fun GameObject.duelStats(): DuelStats = DuelStats(
    health = 60 + rarity.ordinal * 15,
    attack = 12 + rarity.ordinal * 3 + points.coerceIn(0, 100) / 10,
    cardType = cardType,
    name = name,
)

/**
 * Builds an opponent deck the same size as [playerDeck]. Matching mode prefers each player's
 * rarity and falls back to the nearest unused tier; cards are reused only when the pool runs out.
 */
fun generateOpponentDeck(
    playerDeck: List<Card>,
    availableCards: List<Card>,
    matchRarity: Boolean,
    random: Random = Random.Default,
): List<Card> {
    if (playerDeck.isEmpty()) return emptyList()

    val candidates = (availableCards.ifEmpty { playerDeck }).distinctBy { it.id }
    val remaining = candidates.toMutableList()
    return playerDeck.map { playerCard ->
        val pool = remaining.ifEmpty { candidates }
        val selected = if (!matchRarity) {
            pool.random(random)
        } else {
            val exactRarity = remaining.filter { it.obj.rarity == playerCard.obj.rarity }
            val rarityPool = if (exactRarity.isNotEmpty()) {
                exactRarity
            } else {
                val fallbackPool = remaining.ifEmpty { candidates }
                val nearestDistance = fallbackPool.minOf { abs(it.obj.rarity.ordinal - playerCard.obj.rarity.ordinal) }
                fallbackPool.filter { abs(it.obj.rarity.ordinal - playerCard.obj.rarity.ordinal) == nearestDistance }
            }
            rarityPool.random(random)
        }
        remaining.removeAll { it.id == selected.id }
        selected
    }
}

data class DuelCard(
    val name: String,
    val maxHp: Int,
    val attack: Int,
    val cardType: CardType,
    val currentHp: Int = maxHp,
) {
    init {
        require(maxHp > 0) { "A battle card must have positive max HP." }
        require(attack >= 0) { "A battle card cannot have negative attack." }
        require(currentHp in 0..maxHp) { "Current HP must be between zero and max HP." }
    }

    val isKnockedOut: Boolean get() = currentHp <= 0
}

fun DuelStats.toDuelCard(): DuelCard =
    DuelCard(name = name, maxHp = health, attack = attack, cardType = cardType)

enum class DuelOutcome { IN_PROGRESS, VICTORY, DEFEAT }

data class DuelState(
    val playerDeck: List<DuelCard>,
    val rivalDeck: List<DuelCard>,
    val playerActiveIndex: Int = playerDeck.firstAliveIndex(),
    val rivalActiveIndex: Int = rivalDeck.firstAliveIndex(),
    val recoveries: Int = 2,
    val turn: Int = 1,
    val statusMessage: String = "",
    val awaitingOpponent: Boolean = false,
) {
    init {
        require(playerDeck.isNotEmpty()) { "Player deck must contain at least one card." }
        require(rivalDeck.isNotEmpty()) { "Rival deck must contain at least one card." }
        require(playerActiveIndex in playerDeck.indices)
        require(rivalActiveIndex in rivalDeck.indices)
        require(playerDeck.all { it.isKnockedOut } || !playerDeck[playerActiveIndex].isKnockedOut)
        require(rivalDeck.all { it.isKnockedOut } || !rivalDeck[rivalActiveIndex].isKnockedOut)
    }

    val playerActiveCard: DuelCard get() = playerDeck[playerActiveIndex]
    val rivalActiveCard: DuelCard get() = rivalDeck[rivalActiveIndex]

    val outcome: DuelOutcome
        get() = when {
            rivalDeck.all { it.isKnockedOut } -> DuelOutcome.VICTORY
            playerDeck.all { it.isKnockedOut } -> DuelOutcome.DEFEAT
            else -> DuelOutcome.IN_PROGRESS
        }

    val finished: Boolean get() = outcome != DuelOutcome.IN_PROGRESS

    /** Resolves only the player's action so the UI can show its hit before the rival responds. */
    fun playerAction(recover: Boolean = false, swapTo: Int? = null): DuelState {
        if (finished || awaitingOpponent || (recover && recoveries == 0)) return this
        if (swapTo != null) {
            if (swapTo !in playerDeck.indices || swapTo == playerActiveIndex || playerDeck[swapTo].isKnockedOut) return this
            return copy(
                playerActiveIndex = swapTo,
                awaitingOpponent = true,
                statusMessage = "Swapped to ${playerDeck[swapTo].name}!",
            )
        }
        if (recover) {
            val active = playerActiveCard
            return copy(
                playerDeck = playerDeck.updated(playerActiveIndex, active.copy(currentHp = (active.currentHp + 20).coerceAtMost(active.maxHp))),
                recoveries = recoveries - 1,
                awaitingOpponent = true,
                statusMessage = "Recovered up to 20 HP.",
            )
        }
        val hit = attack(playerDeck, playerActiveIndex, rivalDeck, rivalActiveIndex)
        return copy(
            rivalDeck = hit.defenderDeck,
            rivalActiveIndex = hit.defenderIndex,
            awaitingOpponent = true,
            statusMessage = "${hit.effectiveness}: You dealt ${hit.damage} damage." +
                if (hit.knockedOut) " ${hit.defenderName} was knocked out." else "",
        )
    }

    /** Completes a pending turn exactly once. A defeated rival cannot attack. */
    fun opponentTurn(): DuelState {
        if (!awaitingOpponent) return this
        if (finished) return copy(awaitingOpponent = false, turn = turn + 1)
        val hit = attack(rivalDeck, rivalActiveIndex, playerDeck, playerActiveIndex)
        return copy(
            playerDeck = hit.defenderDeck,
            playerActiveIndex = hit.defenderIndex,
            awaitingOpponent = false,
            turn = turn + 1,
            statusMessage = statusMessage + " ${hit.effectiveness}: Enemy attacked for ${hit.damage} damage." +
                if (hit.knockedOut) " Your ${hit.defenderName} was knocked out." else "",
        )
    }

    /** Convenience for callers that do not need animated turn presentation. */
    fun play(recover: Boolean = false): DuelState =
        if (awaitingOpponent) this else playerAction(recover).opponentTurn()

    fun swapCard(targetIndex: Int): DuelState =
        if (awaitingOpponent) this else playerAction(swapTo = targetIndex).opponentTurn()

    private data class AttackResult(
        val defenderDeck: List<DuelCard>,
        val defenderIndex: Int,
        val damage: Int,
        val effectiveness: String,
        val defenderName: String,
        val knockedOut: Boolean,
    )

    private fun attack(
        attackerDeck: List<DuelCard>,
        attackerIndex: Int,
        defenderDeck: List<DuelCard>,
        defenderIndex: Int,
    ): AttackResult {
        val attacker = attackerDeck[attackerIndex]
        val defender = defenderDeck[defenderIndex]
        val multiplier = attacker.cardType.getDamageMultiplier(defender.cardType)
        val damage = max(1, (attacker.attack * multiplier).roundToInt())
        val remainingHp = (defender.currentHp - damage).coerceAtLeast(0)
        val nextDeck = defenderDeck.updated(defenderIndex, defender.copy(currentHp = remainingHp))
        return AttackResult(
            defenderDeck = nextDeck,
            defenderIndex = if (remainingHp == 0) nextAliveIndex(nextDeck, defenderIndex) else defenderIndex,
            damage = damage,
            effectiveness = when (multiplier) {
                1.5 -> "Super Effective"
                0.75 -> "Not Very Effective"
                else -> "Neutral"
            },
            defenderName = defender.name,
            knockedOut = remainingHp == 0,
        )
    }

    private fun nextAliveIndex(deck: List<DuelCard>, knockedOutIndex: Int): Int =
        (1 until deck.size)
            .map { (knockedOutIndex + it) % deck.size }
            .firstOrNull { !deck[it].isKnockedOut }
            ?: knockedOutIndex
}

private fun <T> List<T>.updated(index: Int, value: T): List<T> =
    toMutableList().also { it[index] = value }

private fun List<DuelCard>.firstAliveIndex(): Int =
    indexOfFirst { !it.isKnockedOut }.takeIf { it >= 0 } ?: 0
