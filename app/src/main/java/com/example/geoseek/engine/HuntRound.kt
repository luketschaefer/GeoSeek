// Runs a single hunt session delegating rule evaluation to a modular GameMode.
package com.example.geoseek.engine

import com.example.geoseek.engine.modes.TimedHuntMode
import com.example.geoseek.models.GameObject

/**
 * Encapsulates an active hunt round session, orchestrating game timers, target progress,
 * and rule evaluations by delegating to a modular [GameMode] strategy.
 *
 * Integrates with:
 * - **Domain Game Modes (`com.example.geoseek.engine.modes`)**: Delegates scoring, streak, and win/loss checks.
 * - **State Management**: Exposes read-only state views for Jetpack Compose UI observers.
 *
 * @property environment Display name of the hunt location/environment.
 * @property targets Target [GameObject]s required for this round.
 * @property durationSeconds Time limit allocated for the round (default 60 seconds).
 * @property mode Active [GameMode] strategy handling scoring and rules (defaults to [TimedHuntMode]).
 */
class HuntRound(
    val environment: String,
    val targets: List<GameObject>,
    val durationSeconds: Int = 60,
    val mode: GameMode = TimedHuntMode(timeLimitSeconds = durationSeconds),
) {
    /** Internal mutable session state instance. */
    val state = GameSessionState(
        targetObjects = targets,
        durationSeconds = durationSeconds,
    )

    /** Read-only view of remaining clock time in seconds. */
    val secondsLeft: Int
        get() = state.secondsRemaining

    /** Read-only view of current score points. */
    val score: Int
        get() = state.score

    /** Read-only list of objects successfully found so far. */
    val found: List<GameObject>
        get() = state.foundObjects

    /**
     * Advances the countdown timer clock by the specified seconds.
     *
     * @param secondsElapsed Number of seconds elapsed since last tick (default 1).
     */
    fun tickTime(secondsElapsed: Int = 1) {
        state.secondsRemaining = (state.secondsRemaining - secondsElapsed).coerceAtLeast(0)
    }

    /**
     * Processes a detected object finding event through the active [mode].
     *
     * @param obj The [GameObject] detected by vision analysis.
     * @return [GameRuleOutcome] containing points earned and card rewards.
     */
    fun onObjectFound(obj: GameObject): GameRuleOutcome {
        val outcome = mode.evaluateFoundObject(obj, state)
        state.status = mode.evaluateGameStatus(state)
        return outcome
    }

    /**
     * Checks whether the current round session has concluded.
     *
     * @return True if the session has reached victory, time expiration, or game over.
     */
    fun isOver(): Boolean {
        state.status = mode.evaluateGameStatus(state)
        return state.status != GameStatus.IN_PROGRESS
    }

    /**
     * Concludes the session and calculates the final score summary and card rewards.
     *
     * @return [GameScoreResult] with score breakdown and earned trading cards.
     */
    fun finish(): GameScoreResult {
        state.status = mode.evaluateGameStatus(state)
        return mode.calculateFinalScore(state)
    }
}
