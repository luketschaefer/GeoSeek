package com.geoseek.domain.hunt

import com.geoseek.domain.catalog.ObjectId
import kotlin.time.Duration

sealed interface RoundEvent {
    data object Start : RoundEvent

    /** Absolute elapsed time since the round started (not a delta), so dropped ticks self-correct. */
    data class Tick(
        val elapsed: Duration,
    ) : RoundEvent

    data class TargetFound(
        val id: ObjectId,
    ) : RoundEvent

    data object Abandon : RoundEvent
}
