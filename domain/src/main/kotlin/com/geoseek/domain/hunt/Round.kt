package com.geoseek.domain.hunt

import com.geoseek.domain.catalog.CatalogObject
import com.geoseek.domain.catalog.Environment
import com.geoseek.domain.catalog.ObjectId
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

data class RoundConfig(
    val environment: Environment,
    val seed: Long,
    val targetCount: Int = DEFAULT_TARGET_COUNT,
    val duration: Duration = DEFAULT_DURATION,
) {
    init {
        require(targetCount > 0) { "targetCount must be > 0, was $targetCount" }
        require(duration.isPositive()) { "duration must be positive, was $duration" }
    }

    companion object {
        const val DEFAULT_TARGET_COUNT = 5
        val DEFAULT_DURATION = 5.minutes
    }
}

enum class RoundStatus {
    NOT_STARTED,
    RUNNING,
    WON,
    TIME_UP,
    ABANDONED,
    ;

    val isTerminal: Boolean get() = this == WON || this == TIME_UP || this == ABANDONED
}

/** Immutable round state. Only [RoundReducer] produces new instances from events. */
data class Round(
    val config: RoundConfig,
    val targets: List<CatalogObject>,
    val status: RoundStatus = RoundStatus.NOT_STARTED,
    val elapsed: Duration = Duration.ZERO,
    /** Target id to the elapsed time at which it was found. */
    val found: Map<ObjectId, Duration> = emptyMap(),
) {
    val remaining: Duration get() = (config.duration - elapsed).coerceAtLeast(Duration.ZERO)

    val remainingTargets: List<CatalogObject> get() = targets.filter { it.id !in found }

    fun isFound(id: ObjectId): Boolean = id in found
}
