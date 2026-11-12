// Saved hunt results and statistics, independent of cards and daily quest rewards.
package com.example.geoseek.data

import com.example.geoseek.engine.GameScoreResult
import com.example.geoseek.engine.HuntRound

enum class HuntOutcome(val label: String) {
    COMPLETED("Completed"), TIMED_OUT("Timed out"), ENDED_EARLY("Ended early"),
}

data class HuntRecord(
    val id: String,
    val environment: String,
    val finishedAtMillis: Long,
    val targets: List<String>,
    val found: List<String>,
    val score: Int,
    val xpEarned: Int,
    val outcome: HuntOutcome,
) {
    val missed: List<String> get() = targets.filter { it !in found }

    companion object {
        fun from(round: HuntRound, result: GameScoreResult, finishedAtMillis: Long = System.currentTimeMillis()) = HuntRecord(
            id = round.id,
            environment = round.environment,
            finishedAtMillis = finishedAtMillis,
            targets = round.targets.map { it.name },
            found = round.found.map { it.name },
            score = result.totalScore,
            xpEarned = result.totalXpEarned,
            outcome = when {
                round.found.size >= round.targets.size -> HuntOutcome.COMPLETED
                round.secondsLeft <= 0 -> HuntOutcome.TIMED_OUT
                else -> HuntOutcome.ENDED_EARLY
            },
        )
    }
}

/** Totals for all recorded hunts, including records removed by the history retention limit. */
data class HuntStats(
    val recordedHunts: Int = 0,
    val completed: Int = 0,
    val timedOut: Int = 0,
    val endedEarly: Int = 0,
    val personalBest: Int = 0,
    val totalScore: Long = 0,
    val totalXp: Long = 0,
    val environmentCounts: Map<String, Int> = emptyMap(),
) {
    val averageScore: Long get() = if (recordedHunts == 0) 0 else totalScore / recordedHunts
    val completionPercent: Long get() = if (recordedHunts == 0) 0 else completed * 100L / recordedHunts
    val mostPlayed: List<String> get() {
        val highest = environmentCounts.values.maxOrNull() ?: return emptyList()
        return environmentCounts.filterValues { it == highest }.keys.sorted()
    }

    internal fun adding(record: HuntRecord) = copy(
        recordedHunts = recordedHunts + 1,
        completed = completed + if (record.outcome == HuntOutcome.COMPLETED) 1 else 0,
        timedOut = timedOut + if (record.outcome == HuntOutcome.TIMED_OUT) 1 else 0,
        endedEarly = endedEarly + if (record.outcome == HuntOutcome.ENDED_EARLY) 1 else 0,
        personalBest = maxOf(personalBest, record.score),
        totalScore = totalScore + record.score,
        totalXp = totalXp + record.xpEarned,
        environmentCounts = environmentCounts + (record.environment to (environmentCounts.getOrDefault(record.environment, 0) + 1)),
    )
}

data class HuntHistory(
    val records: List<HuntRecord> = emptyList(),
    val stats: HuntStats = HuntStats(),
    // Keep only identities after trimming details, so an old callback cannot pay again.
    internal val recordedRoundIds: Set<String> = emptySet(),
) {
    internal fun adding(record: HuntRecord) = HuntHistory(
        (listOf(record) + records).take(LIMIT),
        stats.adding(record),
        recordedRoundIds + record.id,
    )

    companion object {
        const val LIMIT = 50
    }
}
