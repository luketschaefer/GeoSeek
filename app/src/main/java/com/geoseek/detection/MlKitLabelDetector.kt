package com.geoseek.detection

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import com.geoseek.domain.detection.Detection
import com.google.mlkit.common.MlKitException
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import javax.inject.Inject

/**
 * On-device ML Kit Image Labeling with the bundled base model (447 labels, no download needed).
 * Labels describe the whole frame, not a located object; TargetMatcher debounces over frames.
 */
class MlKitLabelDetector
    @Inject
    constructor() : ObjectDetector {
        private val labeler =
            ImageLabeling.getClient(
                ImageLabelerOptions.Builder().setConfidenceThreshold(REPORT_THRESHOLD).build(),
            )

        override val name: String = "ML Kit (on-device)"

        @OptIn(ExperimentalGetImage::class)
        override suspend fun detect(frame: CameraFrame): List<Detection> {
            val mediaImage = frame.image.image ?: return emptyList()
            val input = InputImage.fromMediaImage(mediaImage, frame.image.imageInfo.rotationDegrees)
            val labels =
                try {
                    labeler.process(input).await()
                } catch (e: MlKitException) {
                    throw DetectionException("ML Kit labeling failed: ${e.message}", e)
                }
            return labels.map { Detection(label = it.text, confidence = it.confidence) }
        }

        override fun close() = labeler.close()

        private companion object {
            /**
             * Floor for what ML Kit reports at all. Kept below every catalog minConfidence so the
             * debug overlay shows near-misses; per-object thresholds are applied by TargetMatcher.
             */
            const val REPORT_THRESHOLD = 0.4f
        }
    }
