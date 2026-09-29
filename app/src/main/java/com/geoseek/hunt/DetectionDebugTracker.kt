package com.geoseek.hunt

import com.geoseek.domain.catalog.CatalogObject
import com.geoseek.domain.detection.Detection
import com.geoseek.domain.detection.TargetMatcher

data class LabelUi(
    val label: String,
    val confidence: Float,
    /** True if this label alone would count toward the target this frame. */
    val countsForTarget: Boolean,
)

data class DebugOverlayState(
    val labels: List<LabelUi> = emptyList(),
    val streak: Int = 0,
    val requiredFrames: Int = TargetMatcher.DEFAULT_REQUIRED_CONSECUTIVE_FRAMES,
    val matched: Boolean = false,
    val framesAnalyzed: Long = 0,
)

/** Feeds detector output through [TargetMatcher] for one target and shapes it for the debug overlay. */
class DetectionDebugTracker(
    private val target: CatalogObject,
    private val requiredFrames: Int = TargetMatcher.DEFAULT_REQUIRED_CONSECUTIVE_FRAMES,
) {
    private val matcher = TargetMatcher(requiredFrames)
    private var state = DebugOverlayState(requiredFrames = requiredFrames)

    fun onFrame(detections: List<Detection>): DebugOverlayState {
        val frame = matcher.onFrame(detections, listOf(target))
        val matched = target.id in frame.confirmed
        state =
            DebugOverlayState(
                labels =
                    detections
                        .sortedByDescending { it.confidence }
                        .map { LabelUi(it.label, it.confidence, target.accepts(it.label, it.confidence)) },
                streak = if (matched) requiredFrames else frame.streaks[target.id] ?: 0,
                requiredFrames = requiredFrames,
                matched = matched,
                framesAnalyzed = state.framesAnalyzed + 1,
            )
        return state
    }
}
