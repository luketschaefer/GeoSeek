package com.example.geoseek.engine.modes

import com.example.geoseek.engine.GameMode
import com.example.geoseek.engine.GameRuleOutcome
import com.example.geoseek.engine.GameScoreResult
import com.example.geoseek.engine.GameSessionState
import com.example.geoseek.engine.GameStatus
import com.example.geoseek.engine.ObjectFindResult
import com.example.geoseek.models.Card
import com.example.geoseek.models.GameObject

/**
 * Classic Timed Mode strategy: Standard timed hunt mode finding target objects before time expires.
 *
 * Implements [GameMode] rules:
 * - Checks target presence and prevents duplicate scoring.
 * - Adds time bonus points for remaining clock seconds upon victory.
 */
class ClassicTimedMode : GameMode {
    override val name: String = "Classic Timed"
    override val description: String = "Find all target objects before the clock runs out."

    /**
     * Evaluates a detected object against the target list.
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

        state.foundObjects.add(foundObject)
        val points = foundObject.points
        state.score += points

        val cardEarned = Card(obj = foundObject, xpValue = foundObject.points * 2)

        return GameRuleOutcome(
            result = ObjectFindResult.ACCEPTED,
            pointsEarned = points,
            comboMultiplier = 1.0f,
            cardEarned = cardEarned,
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
     * Calculates the final score breakdown including time bonus points.
     *
     * @param state The active [GameSessionState].
     * @return [GameScoreResult] summary payload.
     */
    override fun calculateFinalScore(state: GameSessionState): GameScoreResult {
        val basePoints = state.score
        val isVictory = state.foundObjects.size >= state.targetObjects.size
        val timeBonus = if (isVictory) state.secondsRemaining * 5 else 0
        val totalScore = basePoints + timeBonus
        val cards = state.foundObjects.map { Card(obj = it, xpValue = it.points * 2) }
        val totalXp = cards.sumOf { (_, _, xpValue) -> xpValue } + (if (isVictory) 50 else 10)

        return GameScoreResult(
            basePoints = basePoints,
            bonusPoints = timeBonus,
            totalScore = totalScore,
            totalXpEarned = totalXp,
            cardsCollected = cards,
        )
    }
}
