package com.example.geoseek.detection

data class FilterResult(
    val isConfirmed: Boolean,
    val streakCount: Int,
    val averageConfidence: Float,
)

/**
 * Tracks multi-frame temporal stability for object recognition to eliminate flickering
 * and false positives.
 */
class TemporalFrameFilter(
    private val requiredStreak: Int = 3,
    private val maxGapMillis: Long = 1_500L,
) {
    private var currentStreak = 0
    private var lastMatchTime = 0L
    private var confidenceSum = 0f

    /**
     * Evaluates a frame match against temporal history.
     *
     * @param isMatch True if the frame contains the target object with acceptable confidence.
     * @param confidence Confidence score for the match in the current frame.
     * @param currentTimeMillis Current timestamp in milliseconds.
     */
    fun processFrame(
        isMatch: Boolean,
        confidence: Float,
        currentTimeMillis: Long,
    ): FilterResult {
        if (!isMatch) {
            reset()
            return FilterResult(isConfirmed = false, streakCount = 0, averageConfidence = 0f)
        }

        val gap = currentTimeMillis - lastMatchTime
        if ((lastMatchTime > 0L) && (gap > maxGapMillis)) {
            // Gap was too long, reset streak and start fresh at 1
            currentStreak = 1
            confidenceSum = confidence
        } else {
            currentStreak += 1
            confidenceSum += confidence
        }

        lastMatchTime = currentTimeMillis
        val avgConfidence = confidenceSum / currentStreak
        val confirmed = currentStreak >= requiredStreak

        return FilterResult(
            isConfirmed = confirmed,
            streakCount = currentStreak,
            averageConfidence = avgConfidence,
        )
    }

    /**
     * Resets the temporal tracker state.
     */
    fun reset() {
        currentStreak = 0
        lastMatchTime = 0L
        confidenceSum = 0f
    }
}
