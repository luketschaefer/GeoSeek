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
 * Scavenger Streak Mode strategy: Fast-paced hunt with combo streak multipliers and time extensions (+5s).
 *
 * Implements [GameMode] rules:
 * - Builds combo streaks for consecutive finds (1.0x, 1.5x, 2.0x, 3.0x).
 * - Grants time extensions (+5 seconds) per correct target found.
 *
 * @param timeBonusPerFindSeconds Time extension in seconds awarded per correct object found.
 */
class ScavengerStreakMode(
    private val timeBonusPerFindSeconds: Int = 5,
) : GameMode {
    override val name: String = "Scavenger Streak"
    override val description: String = "Build combo streaks for multiplier bonuses and gain extra time per find."

    /**
     * Evaluates a detected object, updates streak multiplier, and adds bonus time.
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
            state.comboCount = 0
            return GameRuleOutcome(result = ObjectFindResult.WRONG_TARGET, pointsEarned = 0)
        }

        val alreadyFound = state.foundObjects.any { (name, _, _) ->
            name.equals(foundObject.name, ignoreCase = true)
        }
        if (alreadyFound) {
            return GameRuleOutcome(result = ObjectFindResult.DUPLICATE, pointsEarned = 0)
        }

        state.comboCount += 1
        val multiplier = when (state.comboCount) {
            1 -> 1.0f
            2 -> 1.5f
            3 -> 2.0f
            else -> 3.0f
        }

        val pointsEarned = (foundObject.points * multiplier).toInt()
        state.score += pointsEarned
        state.foundObjects.add(foundObject)
        state.secondsRemaining += timeBonusPerFindSeconds

        val card = Card(obj = foundObject, xpValue = (foundObject.points * multiplier).toInt() * 2)

        return GameRuleOutcome(
            result = ObjectFindResult.ACCEPTED,
            pointsEarned = pointsEarned,
            comboMultiplier = multiplier,
            timeBonusSeconds = timeBonusPerFindSeconds,
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
     * Calculates the final score breakdown including streak bonus points.
     *
     * @param state The active [GameSessionState].
     * @return [GameScoreResult] summary payload.
     */
    override fun calculateFinalScore(state: GameSessionState): GameScoreResult {
        val basePoints = state.score
        val isVictory = state.foundObjects.size >= state.targetObjects.size
        val comboBonus = state.comboCount * 15
        val totalScore = basePoints + comboBonus
        val cards = state.foundObjects.map { Card(obj = it, xpValue = it.points * 3) }
        val totalXp = cards.sumOf { (_, _, xpValue) -> xpValue } + (if (isVictory) 100 else 25)

        return GameScoreResult(
            basePoints = basePoints,
            bonusPoints = comboBonus,
            totalScore = totalScore,
            totalXpEarned = totalXp,
            cardsCollected = cards,
        )
    }
}
