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
 * Rarity Hunter Mode strategy: Earn elevated score points and XP by hunting for high-rarity objects.
 *
 * Implements [GameMode] rules:
 * - Multiplies base point values by rarity tier (`COMMON`: 1x, `UNCOMMON`: 2x, `RARE`: 3x, `EPIC`: 5x, `LEGENDARY`: 10x).
 * - Rewards rare trading cards with boosted XP ratings.
 */
class RarityHunterMode : GameMode {
    override val name: String = "Rarity Hunter"
    override val description: String = "Hunt for rare, epic, and legendary items for massive points and card rewards."

    /**
     * Evaluates a detected object, applying rarity point multipliers.
     *
     * @param foundObject The [GameObject] detected by the vision analyzer.
     * @param state The active [GameSessionState].
     * @return [GameRuleOutcome] result payload.
     */
    override fun evaluateFoundObject(
        foundObject: GameObject,
        state: GameSessionState,
    ): GameRuleOutcome {
        val isTarget = state.targetObjects.any { (name, _, _) ->
            name.equals(foundObject.name, ignoreCase = true)
        }
        if (!isTarget) {
            return GameRuleOutcome(result = ObjectFindResult.WRONG_TARGET, pointsEarned = 0)
        }

        val alreadyFound = state.foundObjects.any { (name, _, _) ->
            name.equals(foundObject.name, ignoreCase = true)
        }
        if (alreadyFound) {
            return GameRuleOutcome(result = ObjectFindResult.DUPLICATE, pointsEarned = 0)
        }

        val rarityMultiplier = when (foundObject.rarity) {
            Rarity.COMMON -> 1.0f
            Rarity.UNCOMMON -> 2.0f
            Rarity.RARE -> 3.0f
            Rarity.EPIC -> 5.0f
            Rarity.LEGENDARY -> 10.0f
        }

        val pointsEarned = (foundObject.points * rarityMultiplier).toInt()
        state.score += pointsEarned
        state.foundObjects.add(foundObject)

        val card = Card(obj = foundObject, xpValue = pointsEarned * 3)

        return GameRuleOutcome(
            result = ObjectFindResult.ACCEPTED,
            pointsEarned = pointsEarned,
            comboMultiplier = rarityMultiplier,
            cardEarned = card,
        )
    }

    /**
     * Evaluates whether the round has ended in victory or time expiration.
     *
     * @param state The active [GameSessionState].
     * @return Updated [GameStatus].
     */
    override fun evaluateGameStatus(state: GameSessionState): GameStatus {
        return when {
            state.foundObjects.size >= state.targetObjects.size -> GameStatus.VICTORY
            state.secondsRemaining <= 0 -> GameStatus.TIME_UP
            else -> GameStatus.IN_PROGRESS
        }
    }

    /**
     * Calculates the final score breakdown including rarity bonus points.
     *
     * @param state The active [GameSessionState].
     * @return [GameScoreResult] summary payload.
     */
    override fun calculateFinalScore(state: GameSessionState): GameScoreResult {
        val basePoints = state.score
        val rareCount = state.foundObjects.count { (_, rarity, _) -> rarity != Rarity.COMMON }
        val rarityBonus = rareCount * 50
        val totalScore = basePoints + rarityBonus
        val cards = state.foundObjects.map { Card(obj = it, xpValue = it.points * 4) }
        val totalXp = cards.sumOf { (_, _, xpValue) -> xpValue } + 75

        return GameScoreResult(
            basePoints = basePoints,
            bonusPoints = rarityBonus,
            totalScore = totalScore,
            totalXpEarned = totalXp,
            cardsCollected = cards,
        )
    }
}
