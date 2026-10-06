package com.example.geoseek.engine.modes

import com.example.geoseek.engine.GameMode
import com.example.geoseek.engine.GameRuleOutcome
import com.example.geoseek.engine.GameScoreResult
import com.example.geoseek.engine.GameSessionState
import com.example.geoseek.engine.GameStatus
import com.example.geoseek.engine.ObjectFindResult
import com.example.geoseek.models.Card
import com.example.geoseek.models.GameObject
import com.example.geoseek.models.Rarity

/**
 * Collection Game Mode strategy: Untimed exploration focused on discovering real-world objects
 * to collect trading cards and gain XP.
 *
 * Implements [GameMode] strategy rules:
 * - Operates without a countdown clock to allow relaxed exploration.
 * - Generates collectible trading [Card]s with rarity multipliers (`COMMON`, `UNCOMMON`, `RARE`, `EPIC`, `LEGENDARY`).
 * - Tracks whether an object is a new discovery or a duplicate in the player's binder, awarding discovery XP.
 *
 * @param existingCardCollection Player's current card collection binder used to check for duplicate cards.
 */
class CollectionMode(
    private val existingCardCollection: List<Card> = emptyList(),
) : GameMode {
    override val name: String = "Collection Mode"
    override val description: String = "Discover real-world objects to collect trading cards, earn XP, and fill your card binder."

    /**
     * Evaluates an object discovery, generates a collectible trading card, and awards rarity/discovery XP.
     *
     * @param foundObject The [GameObject] detected by the vision analyzer.
     * @param state The active [GameSessionState].
     * @return [GameRuleOutcome] containing the generated [Card] and XP awarded.
     */
    override fun evaluateFoundObject(
        foundObject: GameObject,
        state: GameSessionState,
    ): GameRuleOutcome {
        val alreadyFoundInSession = state.foundObjects.any { (name, _, _) ->
            name.equals(foundObject.name, ignoreCase = true)
        }
        if (alreadyFoundInSession) {
            return GameRuleOutcome(result = ObjectFindResult.DUPLICATE, pointsEarned = 0)
        }

        val inBinder = existingCardCollection.any { (_, obj) ->
            obj.name.equals(foundObject.name, ignoreCase = true)
        }
        val isNewDiscovery = !inBinder

        state.foundObjects.add(foundObject)

        val rarityMultiplier = when (foundObject.rarity) {
            Rarity.COMMON -> 1.0f
            Rarity.UNCOMMON -> 1.5f
            Rarity.RARE -> 2.5f
            Rarity.EPIC -> 4.0f
            Rarity.LEGENDARY -> 8.0f
        }

        val basePoints = foundObject.points
        val discoveryBonus = if (isNewDiscovery) 50 else 10
        val xpEarned = ((basePoints * rarityMultiplier) + discoveryBonus).toInt()

        state.score += xpEarned

        val card = Card(
            obj = foundObject,
            xpValue = xpEarned,
            isNewDiscovery = isNewDiscovery,
        )

        return GameRuleOutcome(
            result = ObjectFindResult.ACCEPTED,
            pointsEarned = xpEarned,
            comboMultiplier = rarityMultiplier,
            cardEarned = card,
        )
    }

    /**
     * Evaluates round status for Collection Mode (remains in progress during exploration).
     *
     * @param state The active [GameSessionState].
     * @return [GameStatus.VICTORY] if target goal reached, otherwise [GameStatus.IN_PROGRESS].
     */
    override fun evaluateGameStatus(state: GameSessionState): GameStatus {
        val targetsSet = state.targetObjects
        if ((targetsSet.isNotEmpty()) && (state.foundObjects.size >= targetsSet.size)) {
            return GameStatus.VICTORY
        }
        return GameStatus.IN_PROGRESS
    }

    /**
     * Calculates total XP earned, completion bonus for new card discoveries, and final cards payload.
     *
     * @param state The active [GameSessionState].
     * @return [GameScoreResult] summary payload.
     */
    override fun calculateFinalScore(state: GameSessionState): GameScoreResult {
        val totalXp = state.score
        val cards = state.foundObjects.map { obj ->
            val inBinder = existingCardCollection.any { (_, cardObj) ->
                cardObj.name.equals(obj.name, ignoreCase = true)
            }
            Card(
                obj = obj,
                xpValue = obj.points * 2,
                isNewDiscovery = !inBinder,
            )
        }
        val newDiscoveryCount = cards.count { (_, _, _, _, isNewDiscovery) -> isNewDiscovery }
        val completionBonus = newDiscoveryCount * 25

        return GameScoreResult(
            basePoints = totalXp,
            bonusPoints = completionBonus,
            totalScore = totalXp + completionBonus,
            totalXpEarned = totalXp + completionBonus,
            cardsCollected = cards,
        )
    }
}
