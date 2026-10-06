package com.example.geoseek.detection

import android.os.SystemClock
import androidx.camera.core.ImageAnalysis
import androidx.camera.mlkit.vision.MlKitAnalyzer
import com.example.geoseek.models.GameObject
import com.example.geoseek.models.OBJECT_LIST
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import java.util.concurrent.Executor

data class DetectionResult(
    val recognizedObject: GameObject? = null,
    val confidence: Float = 0f,
    val visibleLabel: String? = null,
    val error: String? = null,
)

/**
 * Orchestrates object recognition using modular label matching, label sanitization,
 * and multi-frame temporal confirmation.
 *
 * @param target Specific [GameObject] to hunt for, or null to match any object in [targetPool].
 * @param targetPool Pool of valid findable objects when [target] is null.
 * @param confidenceThreshold Minimum label confidence (0.0 to 1.0) required for a match.
 * @param requiredStreak Number of consecutive matching frames needed to confirm a target.
 */
class ObjectDetector(
    val target: GameObject? = OBJECT_LIST.firstOrNull { (name, _, _) -> name == "Chair" },
    val targetPool: List<GameObject> = OBJECT_LIST,
    confidenceThreshold: Float = 0.60f,
    requiredStreak: Int = 3,
) : AutoCloseable {

    private val labeler = ImageLabeling.getClient(
        ImageLabelerOptions.Builder().setConfidenceThreshold(0.40f).build(),
    )
    private val matcher = ObjectLabelMatcher(defaultConfidenceThreshold = confidenceThreshold)
    private val sanitizer = LabelSanitizer()
    private val temporalFilter = TemporalFrameFilter(requiredStreak = requiredStreak)

    /**
     * Creates a camera analyzer that processes camera frames and reports detection progress.
     *
     * @param executor Serial executor for result callbacks (e.g. main executor).
     * @param onResult Callback for live detection updates and confirmed targets.
     */
    fun createAnalyzer(
        executor: Executor,
        onResult: (DetectionResult) -> Unit,
    ): MlKitAnalyzer = MlKitAnalyzer(
        listOf(labeler),
        ImageAnalysis.COORDINATE_SYSTEM_ORIGINAL,
        executor,
    ) { result ->
        val failure = result.getThrowable(labeler)
        if (failure != null) {
            temporalFilter.reset()
            onResult(DetectionResult(error = "Recognition failed. Tap Retry to scan again."))
            return@MlKitAnalyzer
        }

        val mlKitLabels = result.getValue(labeler).orEmpty()
        val rawLabels = mlKitLabels.map { RawLabel(it.text, it.confidence) }

        val activeTarget = target
        val match: LabelMatch? = if (activeTarget != null) {
            matcher.findTargetMatch(activeTarget, rawLabels)
        } else {
            matcher.findMatches(rawLabels).firstOrNull()
        }

        val visibleLabel = sanitizer.extractVisibleLabel(rawLabels)
            ?: rawLabels.maxByOrNull { it.confidence }?.text

        val filterResult = temporalFilter.processFrame(
            isMatch = match != null,
            confidence = match?.confidence ?: 0f,
            currentTimeMillis = SystemClock.elapsedRealtime(),
        )

        val confirmedObject = match?.gameObject?.takeIf { filterResult.isConfirmed }

        onResult(
            DetectionResult(
                recognizedObject = confirmedObject,
                confidence = if (match != null) filterResult.averageConfidence else 0f,
                visibleLabel = visibleLabel,
            ),
        )
    }

    override fun close() {
        labeler.close()
    }
}
