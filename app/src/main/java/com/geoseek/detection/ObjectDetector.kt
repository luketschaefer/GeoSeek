package com.geoseek.detection

import androidx.camera.core.ImageProxy
import com.geoseek.domain.detection.Detection
import java.io.Closeable

/** One camera frame. The caller owns [image] and closes it after [ObjectDetector.detect] returns. */
class CameraFrame(
    val image: ImageProxy,
)

/**
 * Turns a camera frame into (label, confidence) results. Implementations must be safe to call
 * from a background thread, must not close the frame, and must wrap backend failures in
 * [DetectionException]. Owners call [close] when done (e.g. ViewModel.onCleared).
 */
interface ObjectDetector : Closeable {
    /** Human-readable name for debug UI and settings. */
    val name: String

    suspend fun detect(frame: CameraFrame): List<Detection>
}

class DetectionException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)
