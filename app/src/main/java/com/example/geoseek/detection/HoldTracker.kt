package com.example.geoseek.detection

import com.example.geoseek.models.GameObject
import kotlin.math.max
import kotlin.math.min

/**
 * An axis-aligned box in upright analysis-image pixels.
 */
data class Box(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
    val area: Float get() = max(0f, width) * max(0f, height)
    val centerX: Float get() = (left + right) / 2f
    val centerY: Float get() = (top + bottom) / 2f

    fun contains(x: Float, y: Float): Boolean = x in left..right && y in top..bottom

    /**
     * Maps this box from upright image coordinates back into the unrotated camera buffer it came from.
     * [rotationDegrees] is the clockwise rotation that makes the buffer upright; [bufferWidth] and
     * [bufferHeight] are the buffer's own dimensions.
     */
    fun uprightToBuffer(rotationDegrees: Int, bufferWidth: Int, bufferHeight: Int): Box {
        val w = bufferWidth.toFloat()
        val h = bufferHeight.toFloat()
        fun map(u: Float, v: Float): Pair<Float, Float> = when (rotationDegrees) {
            90 -> v to h - u
            180 -> w - u to h - v
            270 -> w - v to u
            else -> u to v
        }
        val (x1, y1) = map(left, top)
        val (x2, y2) = map(right, bottom)
        return Box(min(x1, x2), min(y1, y2), max(x1, x2), max(y1, y2))
    }

    /** Intersection over union, 0 when the boxes don't overlap. */
    fun iou(other: Box): Float {
        val overlap = Box(max(left, other.left), max(top, other.top), min(right, other.right), min(bottom, other.bottom))
        if (overlap.width <= 0f || overlap.height <= 0f) return 0f
        val union = area + other.area - overlap.area
        return if (union <= 0f) 0f else overlap.area / union
    }
}

/**
 * One object outline from the object detector.
 *
 * @property trackingId Stable id across frames from ML Kit's stream mode, or null if it didn't assign one.
 */
data class TrackedBox(val trackingId: Int?, val box: Box) {
    /** True when this is the same physical object as [other]: same tracking id, or heavily overlapping. */
    fun isSameObjectAs(other: TrackedBox): Boolean =
        if (trackingId != null && other.trackingId != null) trackingId == other.trackingId
        else box.iou(other.box) > 0.3f
}

/**
 * Picks the outline the player is aiming at: the previous lock if it still covers the center of the frame,
 * otherwise the smallest outline covering the center, otherwise the one closest to it.
 */
fun chooseLock(boxes: List<TrackedBox>, centerX: Float, centerY: Float, previous: TrackedBox?): TrackedBox? {
    if (boxes.isEmpty()) return null
    if (previous != null) {
        boxes.firstOrNull { it.isSameObjectAs(previous) && it.box.contains(centerX, centerY) }?.let { return it }
    }
    return boxes.filter { it.box.contains(centerX, centerY) }.minByOrNull { it.box.area }
        ?: boxes.minByOrNull {
            val dx = it.box.centerX - centerX
            val dy = it.box.centerY - centerY
            dx * dx + dy * dy
        }
}

/**
 * Where a hold-to-identify attempt stands after a frame.
 *
 * @property lock The outline currently held on, if any.
 * @property candidate What the labeler thinks the locked object is. Kept internal to the UI until [confirmed].
 * @property confidence Smoothed label confidence for [candidate], 0..1.
 * @property progress How far through the hold the player is, 0..1.
 * @property confirmed True once the object has been held on, confidently, for the whole hold.
 */
data class HoldState(
    val lock: TrackedBox? = null,
    val candidate: GameObject? = null,
    val confidence: Float = 0f,
    val progress: Float = 0f,
    val confirmed: Boolean = false,
)

/**
 * Requires the player to hold the camera on one object before it counts as identified.
 *
 * Progress only builds while the same object stays locked and the labeler keeps agreeing on what it is with
 * smoothed confidence at or above [minConfidence]. Dipping below pauses the hold. Once the labeler commits to a
 * candidate it sticks with it while it's among the matches; readings without it count as misses, and only
 * after it fades out does a different object get a turn (from zero). Losing the lock for longer than
 * [lostGraceMillis] starts over too.
 *
 * @param holdMillis Confident time on target needed to confirm.
 * @param minConfidence Smoothed confidence needed for the hold to advance.
 * @param lostGraceMillis How long the outline may drop out before the hold resets.
 * @param smoothing Weight of each new frame in the confidence moving average.
 * @param maxFrameGapMillis Longest single frame step credited toward the hold, so a stalled frame can't skip ahead.
 */
class HoldTracker(
    private val holdMillis: Long = 2_000L,
    private val minConfidence: Float = 0.60f,
    private val lostGraceMillis: Long = 500L,
    private val smoothing: Float = 0.35f,
    private val maxFrameGapMillis: Long = 250L,
) {
    private var lock: TrackedBox? = null
    private var candidate: GameObject? = null
    private var confidence = 0f
    private var heldMillis = 0L
    private var lastFrameTime: Long? = null
    private var lastSeenTime = 0L

    /** The outline currently held on, so the next frame can prefer it. */
    val currentLock: TrackedBox? get() = lock

    /**
     * Feeds one analyzed frame: the outline being aimed at (if any) and the label matches for it, best first.
     * Labels arrive slower than outlines, so pass [labeled] = false on frames with no new label reading;
     * the hold keeps running on the last reading instead of treating the gap as a miss.
     */
    fun update(seen: TrackedBox?, matches: List<LabelMatch>, now: Long, labeled: Boolean = true): HoldState {
        val step = lastFrameTime?.let { (now - it).coerceIn(0L, maxFrameGapMillis) } ?: 0L
        lastFrameTime = now

        if (seen == null) {
            if (lock != null && now - lastSeenTime > lostGraceMillis) clearLock()
            return snapshot()
        }

        val previous = lock
        if (previous == null || !seen.isSameObjectAs(previous)) clearLock()
        lock = seen
        lastSeenTime = now

        val current = candidate
        when {
            !labeled -> Unit
            current == null -> matches.firstOrNull()?.let {
                candidate = it.gameObject
                confidence = it.confidence
                heldMillis = 0L
            }
            else -> {
                val reading = matches.firstOrNull { it.gameObject.name == current.name }?.confidence ?: 0f
                confidence += smoothing * (reading - confidence)
                if (confidence < minConfidence / 3f) {
                    candidate = null
                    confidence = 0f
                    heldMillis = 0L
                }
            }
        }

        if (candidate != null && confidence >= minConfidence) {
            heldMillis = min(holdMillis, heldMillis + step)
        }
        return snapshot()
    }

    fun reset() {
        clearLock()
        lastFrameTime = null
    }

    private fun clearLock() {
        lock = null
        candidate = null
        confidence = 0f
        heldMillis = 0L
    }

    private fun snapshot(): HoldState {
        val progress = if (holdMillis <= 0L) 1f else heldMillis.toFloat() / holdMillis
        return HoldState(
            lock = lock,
            candidate = candidate,
            confidence = confidence,
            progress = progress,
            confirmed = candidate != null && progress >= 1f,
        )
    }
}
