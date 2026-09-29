package com.geoseek.domain.hunt

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Emits absolute elapsed time every [tickInterval] until [duration] is reached, then completes.
 * Built on `delay`, so tests control it with virtual time (`runTest`). Collect it and feed each
 * value to [RoundReducer] as [RoundEvent.Tick]. To resume after the app was backgrounded, pass the
 * round's current elapsed as [startAt].
 */
class RoundTimer(
    private val tickInterval: Duration = 1.seconds,
) {
    init {
        require(tickInterval.isPositive()) { "tickInterval must be positive" }
    }

    fun ticks(
        duration: Duration,
        startAt: Duration = Duration.ZERO,
    ): Flow<Duration> =
        flow {
            var elapsed = startAt.coerceIn(Duration.ZERO, duration)
            while (elapsed < duration) {
                val step = minOf(tickInterval, duration - elapsed)
                delay(step)
                elapsed += step
                emit(elapsed)
            }
        }
}
