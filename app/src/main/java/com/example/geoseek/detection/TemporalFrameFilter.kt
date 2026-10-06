package com.example.geoseek.detection

/**
 * Result evaluation from multi-frame temporal streak processing.
 *
 * @property isConfirmed True when consecutive frame streak reaches or exceeds the required threshold.
 * @property streakCount Current number of consecutive matching frames in the active window.
 * @property averageConfidence Average confidence calculated over the matching streak window.
 */
data class FilterResult(
    val isConfirmed: Boolean,
    val streakCount: Int,
    val averageConfidence: Float,
)

/**
 * Provides temporal frame filtering and streak tracking over real-time camera streams.
 *
 * Prevents UI flickering and false positive detections in CameraX (`androidx.camera.core.ImageAnalysis`)
 * by enforcing a required number of consecutive matching frames within a maximum allowed time gap.
 *
 * @param requiredStreak Consecutive matching frame count needed to confirm recognition (default 3).
 * @param maxGapMillis Maximum time gap in milliseconds allowed between matching frames (default 1500 ms).
 */
class TemporalFrameFilter(
    private val requiredStreak: Int = 3,
    private val maxGapMillis: Long = 1_500L,
) {
    private var currentStreak = 0
    private var lastMatchTime = 0L
    private var confidenceSum = 0f

    /**
     * Evaluates a single frame match against temporal streak history.
     *
     * @param isMatch True if the frame contains the target object with acceptable confidence.
     * @param confidence Confidence score for the match in the current frame.
     * @param currentTimeMillis Current timestamp in milliseconds (typically from [android.os.SystemClock.elapsedRealtime]).
     * @return [FilterResult] indicating streak progress and whether detection is confirmed.
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
     * Resets the temporal tracker state to zero.
     *
     * Call when a frame analysis error occurs or when starting a new scan session.
     */
    fun reset() {
        currentStreak = 0
        lastMatchTime = 0L
        confidenceSum = 0f
    }
}
