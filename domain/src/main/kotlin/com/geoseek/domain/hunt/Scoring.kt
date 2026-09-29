package com.geoseek.domain.hunt

data class ScoreBreakdown(
    val targetPoints: Int,
    val completionBonus: Int,
    val timeBonus: Int,
) {
    val total: Int get() = targetPoints + completionBonus + timeBonus

    companion object {
        val ZERO = ScoreBreakdown(0, 0, 0)
    }
}

/**
 * - Found targets always score their points (also on TIME_UP).
 * - WON adds [COMPLETION_BONUS_PERCENT]% of target points plus [TIME_BONUS_PER_SECOND] per whole
 *   remaining second.
 * - ABANDONED scores nothing, so quitting is never better than letting the timer run out.
 */
object Scoring {
    const val COMPLETION_BONUS_PERCENT = 50
    const val TIME_BONUS_PER_SECOND = 2

    fun score(round: Round): ScoreBreakdown {
        if (round.status == RoundStatus.ABANDONED) return ScoreBreakdown.ZERO
        val targetPoints = round.targets.filter { round.isFound(it.id) }.sumOf { it.points }
        if (round.status != RoundStatus.WON) return ScoreBreakdown(targetPoints, 0, 0)
        return ScoreBreakdown(
            targetPoints = targetPoints,
            completionBonus = targetPoints * COMPLETION_BONUS_PERCENT / 100,
            timeBonus = round.remaining.inWholeSeconds.toInt() * TIME_BONUS_PER_SECOND,
        )
    }
}
