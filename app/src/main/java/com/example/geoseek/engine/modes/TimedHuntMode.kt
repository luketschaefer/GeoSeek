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
 * Timed Game Mode strategy: Players must locate all target objects before time expires.
 *
 * Implements [GameMode] strategy rules:
 * - Accepts target object finds and awards point values.
 * - Tracks countdown time and evaluates victory when all target objects are found.
 * - Awards time-bonus score points based on remaining time upon victory.
 *
 * @param timeLimitSeconds Total duration allocated for the hunt session in seconds (default 60s).
 * @param timeBonusMultiplier Point multiplier per remaining second upon victory (default 5x).
 */
class TimedHuntMode(
    val timeLimitSeconds: Int = 60,
    private val timeBonusMultiplier: Int = 5,
) : GameMode {
    override val name: String = "Timed Mode"
    override val description: String = "Find all target objects before the countdown timer runs out."

    /**
     * Evaluates an object find against the target list and updates score state.
     *
     * @param foundObject The [GameObject] detected by the vision analyzer.
     * @param state The active [GameSessionState].
     * @return [GameRuleOutcome] indicating whether the object was accepted or duplicate.
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

        val card = Card(
            obj = foundObject,
            xpValue = foundObject.points * 2,
            isNewDiscovery = true,
        )

        return GameRuleOutcome(
            result = ObjectFindResult.ACCEPTED,
            pointsEarned = points,
            comboMultiplier = 1.0f,
            cardEarned = card,
        )
    }

    /**
     * Evaluates whether all target objects have been found or if time has expired.
     *
     * @param state The active [GameSessionState].
     * @return [GameStatus.VICTORY] if all targets found, [GameStatus.TIME_UP] if clock reaches 0.
     */
    override fun evaluateGameStatus(state: GameSessionState): GameStatus {
        return when {
            state.foundObjects.size >= state.targetObjects.size -> GameStatus.VICTORY
            state.secondsRemaining <= 0 -> GameStatus.TIME_UP
            else -> GameStatus.IN_PROGRESS
        }
    }

    /**
     * Computes the final score breakdown including time bonus points and collectible trading cards.
     *
     * @param state The active [GameSessionState].
     * @return [GameScoreResult] summary payload.
     */
    override fun calculateFinalScore(state: GameSessionState): GameScoreResult {
        val basePoints = state.score
        val isVictory = state.foundObjects.size >= state.targetObjects.size
        val timeBonus = if (isVictory) state.secondsRemaining * timeBonusMultiplier else 0
        val totalScore = basePoints + timeBonus

        val cards = state.foundObjects.map { obj ->
            Card(
                obj = obj,
                xpValue = obj.points * 2,
                isNewDiscovery = true,
            )
        }
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
