package com.example.geoseek.detection

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.SystemClock
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.example.geoseek.audio.BitDefendersTheme
import com.example.geoseek.models.GameObject
import com.example.geoseek.models.OBJECT_LIST
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.ceil
import kotlin.math.floor

/**
 * Encapsulates live camera frame recognition detection results.
 *
 * @property recognizedObject Confirmed target [GameObject] once the player has held on it long enough.
 * @property confidence Smoothed confidence value (0.0f to 1.0f) for the held object.
 * @property visibleLabel Highest-confidence non-generic label text suitable for user display.
 * @property error User-facing error message if vision frame analysis fails.
 * @property boxes Every object outline in the frame, in upright image pixels.
 * @property visibleLock The outline currently held on, if any.
 * @property progress How far through the hold the player is, 0..1.
 * @property imageWidth Width of the upright analysis image the boxes are measured in.
 * @property imageHeight Height of the upright analysis image the boxes are measured in.
 * @property sawBitDefenders Whether this frame's text reading found the word "BitDefenders".
 */
data class DetectionResult(
    val recognizedObject: GameObject? = null,
    val confidence: Float = 0f,
    val visibleLabel: String? = null,
    val error: String? = null,
    val boxes: List<TrackedBox> = emptyList(),
    val visibleLock: TrackedBox? = null,
    val progress: Float = 0f,
    val imageWidth: Int = 0,
    val imageHeight: Int = 0,
    val sawBitDefenders: Boolean = false,
)

/**
 * Orchestrates real-time object recognition over CameraX frames.
 *
 * Per frame:
 * - **ML Kit Object Detection** outlines the objects in view (stream mode, so outlines keep tracking ids).
 * - [chooseLock] picks the one the player is aiming at; with no outlines, the middle of the frame stands in.
 * - **ML Kit Image Labeling**, plus the optional [CustomClassifier], label a crop around that object.
 * - [ObjectLabelMatcher] maps the labels to [targetPool], and [HoldTracker] confirms the object once it's been
 *   held on confidently for long enough.
 * - **ML Kit Text Recognition** reads the whole frame every [textIntervalMillis], looking for "BitDefenders".
 *
 * All analysis runs on the detector's own background thread ([analysisExecutor]); results are posted to the
 * callback executor passed to [createAnalyzer].
 *
 * @param targetPool Pool of valid findable objects; labels for anything else are ignored.
 * @param confidenceThreshold Minimum confidence required for a label match (default 0.50f).
 * @param labelIntervalMillis Minimum time between label readings on the same object. Outlines update every frame.
 * @param textIntervalMillis Minimum time between text readings of the whole frame.
 */
class ObjectDetector(
    context: Context,
    val targetPool: List<GameObject> = OBJECT_LIST,
    confidenceThreshold: Float = 0.50f,
    private val labelIntervalMillis: Long = 200L,
    private val textIntervalMillis: Long = 500L,
) : AutoCloseable {

    private val executor: ExecutorService = Executors.newSingleThreadExecutor()

    /** The thread [createAnalyzer]'s analyzer must run on. */
    val analysisExecutor: Executor get() = executor

    private val outliner = ObjectDetection.getClient(
        ObjectDetectorOptions.Builder()
            .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
            .enableMultipleObjects()
            .build(),
    )
    private val labeler = ImageLabeling.getClient(
        ImageLabelerOptions.Builder().setConfidenceThreshold(0.35f).build(),
    )
    private val textReader = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private val customClassifier = CustomClassifier.load(context.applicationContext)
    private val matcher = ObjectLabelMatcher(defaultConfidenceThreshold = confidenceThreshold)
    private val sanitizer = LabelSanitizer()
    private val hold = HoldTracker()
    private val poolNames = targetPool.map { it.name }.toSet()

    // Only touched on the analysis thread.
    private var lastLabelTime: Long? = null
    private var lastTextTime: Long? = null
    private var lastVisibleLabel: String? = null
    private var closed = false

    /**
     * Builds an analyzer to bind with `setImageAnalysisAnalyzer(analysisExecutor, analyzer)`.
     *
     * @param callbackExecutor Executor [onResult] runs on, typically the main thread.
     * @param onResult Callback invoked per frame with an updated [DetectionResult].
     */
    fun createAnalyzer(
        callbackExecutor: Executor,
        onResult: (DetectionResult) -> Unit,
    ): ImageAnalysis.Analyzer = ImageAnalysis.Analyzer { frame ->
        val result = frame.use {
            if (closed) return@Analyzer
            try {
                analyze(it)
            } catch (e: Exception) {
                Log.w(TAG, "Frame analysis failed", e)
                resetHold()
                DetectionResult(error = "Recognition failed. Retrying…")
            }
        }
        callbackExecutor.execute { onResult(result) }
    }

    private fun analyze(frame: ImageProxy): DetectionResult {
        val now = SystemClock.elapsedRealtime()
        val rotation = frame.imageInfo.rotationDegrees
        val buffer = frame.toBitmap()
        val sideways = rotation == 90 || rotation == 270
        val width = if (sideways) buffer.height else buffer.width
        val height = if (sideways) buffer.width else buffer.height

        // ML Kit applies the rotation itself, so outlines come back in upright coordinates.
        val objects = Tasks.await(outliner.process(InputImage.fromBitmap(buffer, rotation)))
        val boxes = objects.map { obj ->
            val r = obj.boundingBox
            TrackedBox(obj.trackingId, Box(r.left.toFloat(), r.top.toFloat(), r.right.toFloat(), r.bottom.toFloat()))
        }

        val centerX = width / 2f
        val centerY = height / 2f
        // Big things (a couch, a car) often get no outline at all; label the middle of the frame instead.
        val seen = chooseLock(boxes, centerX, centerY, hold.currentLock)
            ?: TrackedBox(trackingId = null, box = Box(width * 0.15f, height * 0.15f, width * 0.85f, height * 0.85f))

        val previous = hold.currentLock
        val newObject = previous == null || !seen.isSameObjectAs(previous)
        val dueForLabel = lastLabelTime.let { it == null || now - it >= labelIntervalMillis }
        val labeled = newObject || dueForLabel

        var matches = emptyList<LabelMatch>()
        if (labeled) {
            lastLabelTime = now
            val labels = labelCrop(buffer, seen.box, rotation, width, height)
            matches = matcher.findMatches(labels).filter { it.gameObject.name in poolNames }
            lastVisibleLabel = sanitizer.extractVisibleLabel(labels)
        }

        val state = hold.update(seen, matches, now, labeled)

        var sawBitDefenders = false
        if (lastTextTime.let { it == null || now - it >= textIntervalMillis }) {
            lastTextTime = now
            val text = Tasks.await(textReader.process(InputImage.fromBitmap(buffer, rotation))).text
            sawBitDefenders = BitDefendersTheme.mentionedIn(text)
        }

        return DetectionResult(
            recognizedObject = state.candidate?.takeIf { state.confirmed },
            confidence = state.confidence,
            visibleLabel = lastVisibleLabel,
            boxes = boxes,
            visibleLock = state.lock,
            progress = state.progress,
            imageWidth = width,
            imageHeight = height,
            sawBitDefenders = sawBitDefenders,
        )
    }

    /** Runs both labelers on a slightly padded crop around [box], given in upright coordinates. */
    private fun labelCrop(buffer: Bitmap, box: Box, rotation: Int, width: Int, height: Int): List<RawLabel> {
        val padX = box.width * 0.1f
        val padY = box.height * 0.1f
        val padded = Box(box.left - padX, box.top - padY, box.right + padX, box.bottom + padY).clampTo(width, height)
        val inBuffer = padded.uprightToBuffer(rotation, buffer.width, buffer.height)
        val left = floor(inBuffer.left).toInt().coerceIn(0, buffer.width - 1)
        val top = floor(inBuffer.top).toInt().coerceIn(0, buffer.height - 1)
        val right = ceil(inBuffer.right).toInt().coerceIn(left + 1, buffer.width)
        val bottom = ceil(inBuffer.bottom).toInt().coerceIn(top + 1, buffer.height)
        val crop = Bitmap.createBitmap(
            buffer, left, top, right - left, bottom - top,
            Matrix().apply { postRotate(rotation.toFloat()) }, true,
        )

        val stock = Tasks.await(labeler.process(InputImage.fromBitmap(crop, 0)))
            .map { RawLabel(it.text, it.confidence) }
        val custom = customClassifier?.classify(crop).orEmpty()
        return (stock + custom).sortedByDescending { it.confidence }
    }

    private fun resetHold() {
        hold.reset()
        lastLabelTime = null
        lastVisibleLabel = null
    }

    /**
     * Releases the ML Kit clients and the custom model. Runs on the analysis thread after any frame in progress,
     * so nothing is closed out from under it.
     *
     * Call when detaching from camera lifecycle to prevent resource leaks.
     */
    override fun close() {
        executor.execute {
            closed = true
            outliner.close()
            labeler.close()
            textReader.close()
            customClassifier?.close()
        }
        executor.shutdown()
    }

    private companion object {
        const val TAG = "ObjectDetector"
    }
}

private fun Box.clampTo(width: Int, height: Int) = Box(
    left.coerceIn(0f, width.toFloat()),
    top.coerceIn(0f, height.toFloat()),
    right.coerceIn(0f, width.toFloat()),
    bottom.coerceIn(0f, height.toFloat()),
)
