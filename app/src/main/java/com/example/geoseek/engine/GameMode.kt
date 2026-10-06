package com.example.geoseek.engine

import com.example.geoseek.models.Card
import com.example.geoseek.models.GameObject

/**
 * Represents the lifecycle status of an active hunt session.
 */
enum class GameStatus {
    IN_PROGRESS,
    VICTORY,
    TIME_UP,
    GAME_OVER,
}

/**
 * Result classification when an object is detected during a hunt session.
 */
enum class ObjectFindResult {
    ACCEPTED,
    DUPLICATE,
    WRONG_TARGET,
}

/**
 * Outcome returned after evaluating an object find event against mode rules.
 *
 * @property result Classification of the find ([ObjectFindResult.ACCEPTED], [ObjectFindResult.DUPLICATE], etc.).
 * @property pointsEarned Score points awarded for this find event.
 * @property comboMultiplier Active combo multiplier applied to points.
 * @property timeBonusSeconds Seconds added to the countdown clock (if applicable).
 * @property cardEarned Collectible [Card] earned for this discovery.
 */
data class GameRuleOutcome(
    val result: ObjectFindResult,
    val pointsEarned: Int,
    val comboMultiplier: Float = 1.0f,
    val timeBonusSeconds: Int = 0,
    val cardEarned: Card? = null,
)

/**
 * Final round summary score breakdown and rewards.
 *
 * @property basePoints Points earned from base object finds.
 * @property bonusPoints Bonus points awarded from remaining time or combos.
 * @property totalScore Final total round score.
 * @property totalXpEarned Total experience points awarded to player profile.
 * @property cardsCollected List of collectible trading [Card]s earned during the session.
 */
data class GameScoreResult(
    val basePoints: Int,
    val bonusPoints: Int,
    val totalScore: Int,
    val totalXpEarned: Int,
    val cardsCollected: List<Card>,
)

/**
 * Mutable state container holding session data for an active hunt round.
 *
 * @property targetObjects List of [GameObject] targets required for this session.
 * @property foundObjects List of [GameObject]s successfully found so far.
 * @property durationSeconds Initial time limit in seconds allocated for the session.
 * @property secondsRemaining Remaining time on the round countdown clock.
 * @property score Accumulated player score points.
 * @property comboCount Active streak count for combo multipliers.
 * @property status Current [GameStatus] lifecycle state.
 */
data class GameSessionState(
    val targetObjects: List<GameObject>,
    val foundObjects: MutableList<GameObject> = mutableListOf(),
    val durationSeconds: Int,
    var secondsRemaining: Int = durationSeconds,
    var score: Int = 0,
    var comboCount: Int = 0,
    var status: GameStatus = GameStatus.IN_PROGRESS,
)

/**
 * Strategy interface defining game mode rules, scoring calculations, and round status.
 *
 * Implements the **Strategy Design Pattern** to decouple domain game mode logic from UI layers
 * and Jetpack Compose state handlers.
 */
interface GameMode {
    /** Unique display name for the game mode. */
    val name: String

    /** Brief description explaining mode objective. */
    val description: String

    /**
     * Evaluates an object finding event according to the mode's scoring and streak rules.
     *
     * @param foundObject The [GameObject] detected by the vision detector.
     * @param state Current mutable [GameSessionState] for the session.
     * @return [GameRuleOutcome] containing points earned, multipliers, and trading cards.
     */
    fun evaluateFoundObject(
        foundObject: GameObject,
        state: GameSessionState,
    ): GameRuleOutcome

    /**
     * Evaluates whether the current game round has ended in victory or time expiration.
     *
     * @param state Current mutable [GameSessionState] for the session.
     * @return Updated [GameStatus] enum value.
     */
    fun evaluateGameStatus(state: GameSessionState): GameStatus

    /**
     * Calculates the final score summary, bonus multipliers, and XP rewards at round end.
     *
     * @param state Current mutable [GameSessionState] for the session.
     * @return [GameScoreResult] with score breakdown and earned collectible cards.
     */
    fun calculateFinalScore(state: GameSessionState): GameScoreResult
}
