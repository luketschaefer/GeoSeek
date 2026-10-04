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

/** Recognizes the chair target locally; generic labels such as Furniture never count. */
class ObjectDetector : AutoCloseable {
    private val target = OBJECT_LIST.first { it.name == "Chair" }
    private val labeler = ImageLabeling.getClient(
        ImageLabelerOptions.Builder().setConfidenceThreshold(0.5f).build()
    )
    private var matchingFrames = 0
    private var previousMatchTime = 0L

    // MlKitAnalyzer owns frame rotation and closes every ImageProxy after ML finishes.
    fun createAnalyzer(executor: Executor, onResult: (DetectionResult) -> Unit): MlKitAnalyzer =
        MlKitAnalyzer(
            listOf(labeler),
            ImageAnalysis.COORDINATE_SYSTEM_ORIGINAL,
            executor,
        ) { result ->
            val failure = result.getThrowable(labeler)
            if (failure != null) {
                matchingFrames = 0
                onResult(DetectionResult(error = "Recognition failed. Tap Retry to scan again."))
            } else {
                val labels = result.getValue(labeler).orEmpty()
                val chair = labels.firstOrNull {
                    it.text.equals(target.name, ignoreCase = true) && it.confidence >= 0.75f
                }
                val now = SystemClock.elapsedRealtime()
                matchingFrames = if (chair == null) {
                    0
                } else if (now - previousMatchTime > 1_500L) {
                    1
                } else {
                    matchingFrames + 1
                }
                if (chair != null) previousMatchTime = now
                onResult(
                    DetectionResult(
                        recognizedObject = target.takeIf { matchingFrames >= 3 },
                        confidence = chair?.confidence ?: 0f,
                        visibleLabel = labels.maxByOrNull { it.confidence }?.text,
                    )
                )
            }
        }

    override fun close() {
        labeler.close()
    }
}
