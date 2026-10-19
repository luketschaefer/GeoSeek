package com.example.geoseek.detection

import android.graphics.RectF
import android.os.SystemClock
import androidx.camera.core.ImageAnalysis
import androidx.camera.mlkit.vision.MlKitAnalyzer
import com.example.geoseek.models.GameObject
import com.example.geoseek.models.OBJECT_LIST
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import java.util.concurrent.Executor

/**
 * Encapsulates live camera frame recognition detection results.
 *
 * @property recognizedObject Confirmed target [GameObject] once multi-frame streak rules pass.
 * @property confidence Smoothed confidence value (0.0f to 1.0f) for the detected match.
 * @property visibleLabel Highest-confidence non-generic label text suitable for user display.
 * @property error User-facing error message if vision frame analysis fails.
 */
data class DetectionResult(
    val recognizedObject: GameObject? = null,
    val confidence: Float = 0f,
    val visibleLabel: String? = null,
    val error: String? = null,
    // Bounds in PreviewView pixels, already transformed by CameraX.
    val objectBounds: List<RectF> = emptyList(),
)

/**
 * Orchestrates real-time object recognition by integrating CameraX analysis with Google ML Kit.
 *
 * Integrates directly with:
 * - **CameraX (`androidx.camera.core.ImageAnalysis`)**: Consumes live camera frames.
 * - **CameraX ML Kit Interop (`androidx.camera.mlkit.vision.MlKitAnalyzer`)**: Coordinates rotation and automatic frame release.
 * - **Google ML Kit Image Labeling (`com.google.mlkit.vision.label.ImageLabeler`)**: Runs local, on-device image labeling models.
 * - **Modular Helpers**: Delegates filtering to [ObjectLabelMatcher], [LabelSanitizer], and [TemporalFrameFilter].
 *
 * @param target Specific [GameObject] to hunt for, or null to match any candidate in [targetPool].
 * @param targetPool Pool of valid findable objects when [target] is null.
 * @param confidenceThreshold Minimum confidence required for a label match (default 0.50f).
 * @param requiredStreak Number of consecutive matching frames required to confirm recognition (default 2).
 */
class ObjectDetector(
    val target: GameObject? = OBJECT_LIST.firstOrNull { (name, _, _) -> name == "Chair" },
    val targetPool: List<GameObject> = OBJECT_LIST,
    confidenceThreshold: Float = 0.50f,
    requiredStreak: Int = 2,
) : AutoCloseable {

    private val labeler = ImageLabeling.getClient(
        ImageLabelerOptions.Builder().setConfidenceThreshold(0.35f).build(),
    )
    private val objectLocator = ObjectDetection.getClient(
        ObjectDetectorOptions.Builder()
            .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
            .enableMultipleObjects()
            .build(),
    )
    private val matcher = ObjectLabelMatcher(defaultConfidenceThreshold = confidenceThreshold)
    private val sanitizer = LabelSanitizer()
    private val temporalFilter = TemporalFrameFilter(requiredStreak = requiredStreak)

    /**
     * Builds an [MlKitAnalyzer] bridge connecting CameraX frame pipelines to ML Kit models.
     *
     * @param executor Serial callback [Executor] for thread-safe UI updates.
     * @param onResult Callback function invoked per frame with an updated [DetectionResult].
     * @return An [MlKitAnalyzer] instance ready to bind to CameraX preview controllers.
     */
    fun createAnalyzer(
        executor: Executor,
        onResult: (DetectionResult) -> Unit,
    ): MlKitAnalyzer = MlKitAnalyzer(
        listOf(labeler, objectLocator),
        ImageAnalysis.COORDINATE_SYSTEM_VIEW_REFERENCED,
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
            matcher.findMatches(rawLabels).firstOrNull { candidate ->
                targetPool.any { it.name.equals(candidate.gameObject.name, ignoreCase = true) }
            }
        }

        val visibleLabel = sanitizer.extractVisibleLabel(rawLabels)
            ?: rawLabels.maxByOrNull { (_, confidence) -> confidence }?.text

        val (isConfirmed, _, averageConfidence) = temporalFilter.processFrame(
            isMatch = match != null,
            confidence = match?.confidence ?: 0f,
            currentTimeMillis = SystemClock.elapsedRealtime(),
        )

        val confirmedObject = match?.gameObject?.takeIf { isConfirmed }

        onResult(
            DetectionResult(
                recognizedObject = confirmedObject,
                confidence = if (match != null) averageConfidence else 0f,
                visibleLabel = visibleLabel,
                // Localization failure must not interrupt existing label recognition.
                objectBounds = result.getValue(objectLocator).orEmpty().map { RectF(it.boundingBox) },
            ),
        )
    }

    /**
     * Releases underlying ML Kit native image labeler resources.
     *
     * Call when detaching from camera lifecycle to prevent resource leaks.
     */
    override fun close() {
        labeler.close()
        objectLocator.close()
    }
}
