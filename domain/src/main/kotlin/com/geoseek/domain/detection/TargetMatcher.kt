package com.geoseek.domain.detection

import com.geoseek.domain.catalog.CatalogObject
import com.geoseek.domain.catalog.ObjectId

/**
 * Debounces detector output: a target is confirmed only after it is seen (an accepted label at or
 * above its minConfidence) in [requiredConsecutiveFrames] consecutive frames. A frame without it
 * resets its streak. Each target is confirmed at most once until [reset].
 *
 * Stateful and not thread-safe: feed frames from a single coroutine/analyzer thread.
 */
class TargetMatcher(
    private val requiredConsecutiveFrames: Int = DEFAULT_REQUIRED_CONSECUTIVE_FRAMES,
) {
    init {
        require(requiredConsecutiveFrames > 0) { "requiredConsecutiveFrames must be > 0" }
    }

    private val streaks = mutableMapOf<ObjectId, Int>()
    private val confirmed = mutableSetOf<ObjectId>()

    fun onFrame(
        detections: List<Detection>,
        targets: List<CatalogObject>,
    ): MatchFrame {
        val activeIds = targets.map { it.id }.toSet()
        streaks.keys.retainAll(activeIds)

        val newlyConfirmed = mutableSetOf<ObjectId>()
        targets.filter { it.id !in confirmed }.forEach { target ->
            val seen = detections.any { target.accepts(it.label, it.confidence) }
            val streak = if (seen) (streaks[target.id] ?: 0) + 1 else 0
            if (streak >= requiredConsecutiveFrames) {
                confirmed += target.id
                newlyConfirmed += target.id
                streaks.remove(target.id)
            } else {
                streaks[target.id] = streak
            }
        }
        return MatchFrame(
            newlyConfirmed = newlyConfirmed,
            confirmed = confirmed.intersect(activeIds),
            streaks = streaks.toMap(),
        )
    }

    fun reset() {
        streaks.clear()
        confirmed.clear()
    }

    companion object {
        const val DEFAULT_REQUIRED_CONSECUTIVE_FRAMES = 3
    }
}

data class MatchFrame(
    /** Targets confirmed by this frame; act on these (e.g. dispatch RoundEvent.TargetFound). */
    val newlyConfirmed: Set<ObjectId>,
    /** All confirmed targets so far among the active ones. */
    val confirmed: Set<ObjectId>,
    /** Current streak per unconfirmed active target, for debug/progress UI. */
    val streaks: Map<ObjectId, Int>,
)
