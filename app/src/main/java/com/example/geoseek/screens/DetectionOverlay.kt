// The AR layer over the camera preview: outlines for everything the detector sees, and a glowing trace that
// runs clockwise around the held object as the hold fills. Plus the confidence readout shown under it.
package com.example.geoseek.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.geoseek.detection.Box
import com.example.geoseek.detection.DetectionResult
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Draws [result]'s outlines over a FILL_CENTER camera preview of the same size as [modifier].
 * Other objects get faint corner brackets; the held one gets a full outline whose clockwise glow tracks the
 * hold progress, turning to the success color once identified.
 */
@Composable
fun DetectionOverlay(result: DetectionResult, accent: Color, modifier: Modifier = Modifier) {
    val lock = result.visibleLock
    val identified = result.recognizedObject != null
    val haptics = LocalHapticFeedback.current

    // The held outline springs after the object between frames; on a new object it snaps there and
    // the brackets slam in from outside, like a lock-on.
    val lockRect = remember { Animatable(Rect.Zero, Rect.VectorConverter) }
    val lockIn = remember { Animatable(0f) }
    var lockKey by remember { mutableStateOf<Any?>(null) }
    LaunchedEffect(lock) {
        if (lock == null) {
            lockKey = null
            lockIn.animateTo(0f, tween(150))
            return@LaunchedEffect
        }
        val key = lock.trackingId ?: lockKey ?: lock.box
        if (key != lockKey) {
            lockKey = key
            lockRect.snapTo(lock.box.toRect())
            haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
            lockIn.snapTo(0f)
            lockIn.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow))
        } else {
            lockRect.animateTo(lock.box.toRect(), spring(stiffness = Spring.StiffnessMedium))
        }
    }

    // On identification: a buzz, a flash inside the outline, and a ring bursting outward.
    val burst = remember { Animatable(0f) }
    LaunchedEffect(identified) {
        if (!identified) {
            burst.snapTo(0f)
            return@LaunchedEffect
        }
        haptics.performHapticFeedback(HapticFeedbackType.Confirm)
        burst.animateTo(1f, tween(700, easing = FastOutSlowInEasing))
    }

    val progress by animateFloatAsState(
        if (identified) 1f else result.progress, tween(90, easing = LinearEasing), label = "holdProgress",
    )
    val pulse by rememberInfiniteTransition(label = "glow")
        .animateFloat(0.55f, 1f, infiniteRepeatable(tween(520), RepeatMode.Reverse), label = "glowPulse")

    Canvas(modifier) {
        val iw = result.imageWidth.toFloat()
        val ih = result.imageHeight.toFloat()
        if (iw <= 0f || ih <= 0f) return@Canvas
        // Same mapping PreviewView's FILL_CENTER uses: scale to cover, then center-crop.
        val scale = max(size.width / iw, size.height / ih)
        val dx = (size.width - iw * scale) / 2f
        val dy = (size.height - ih * scale) / 2f
        fun Rect.toView() = Rect(left * scale + dx, top * scale + dy, right * scale + dx, bottom * scale + dy)

        val held = lockRect.value.takeIf { lock != null && it != Rect.Zero }?.toView()
        val radius = 14.dp.toPx()

        // Spotlight: dim everything but the held object.
        if (held != null) {
            val scrim = Path().apply {
                fillType = PathFillType.EvenOdd
                addRect(Rect(Offset.Zero, size))
                addPath(roundedOutline(held, radius))
            }
            drawPath(scrim, Color.Black.copy(alpha = 0.38f * lockIn.value.coerceIn(0f, 1f)))
        }

        for (other in result.boxes) {
            if (lock != null && other.isSameObjectAs(lock)) continue
            drawCornerBrackets(other.box.toRect().toView(), accent.copy(alpha = 0.45f))
        }

        if (held != null) {
            val color = if (identified) GeoColors.Success else accent
            val outline = roundedOutline(held, radius)
            val settle = lockIn.value
            drawPath(outline, color.copy(alpha = 0.3f * settle.coerceIn(0f, 1f)), style = Stroke(2.dp.toPx()))
            drawCornerBrackets(held.inflate((1f - settle) * 28.dp.toPx()), color.copy(alpha = settle.coerceIn(0f, 1f)))
            if (progress > 0f) drawTrace(outline, progress, color, glow = if (identified) 1f else pulse)

            val b = burst.value
            if (b > 0f && b < 1f) {
                drawPath(outline, Color.White.copy(alpha = 0.35f * (1f - b)))
                drawPath(
                    roundedOutline(held.inflate(b * 36.dp.toPx()), radius + b * 36.dp.toPx()),
                    color.copy(alpha = 1f - b),
                    style = Stroke((1f - b) * 8.dp.toPx() + 1.dp.toPx()),
                )
            }
        }
    }
}

/** Big confidence percentage for the held object, shown at the bottom of the viewfinder. */
@Composable
fun ConfidenceReadout(confidence: Float, accent: Color, done: Boolean) {
    val shown by animateFloatAsState(confidence.coerceIn(0f, 1f), tween(200), label = "confidence")
    val color = when {
        done -> GeoColors.Success
        shown >= 0.6f -> accent
        else -> GeoColors.TextSoft
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        OutlinedText("${(shown * 100).roundToInt()}%", GeoType.Display, color = color, outlineWidth = 3.dp)
        OutlinedText("CONFIDENCE", GeoType.Caption.copy(letterSpacing = 3.sp), color = GeoColors.TextSoft, outlineWidth = 2.dp)
        Spacer(Modifier.height(6.dp))
        PixelBar(shown, color, Modifier.width(140.dp), segments = 14)
        Spacer(Modifier.height(8.dp))
    }
}

private fun Box.toRect() = Rect(left, top, right, bottom)

/** A rounded-rect outline starting at the top center and running clockwise, so a partial trace reads like a dial. */
private fun roundedOutline(r: Rect, radius: Float): Path = Path().apply {
    val rr = min(radius, min(r.width, r.height) / 2f)
    val d = rr * 2f
    moveTo(r.center.x, r.top)
    lineTo(r.right - rr, r.top)
    arcTo(Rect(r.right - d, r.top, r.right, r.top + d), -90f, 90f, false)
    lineTo(r.right, r.bottom - rr)
    arcTo(Rect(r.right - d, r.bottom - d, r.right, r.bottom), 0f, 90f, false)
    lineTo(r.left + rr, r.bottom)
    arcTo(Rect(r.left, r.bottom - d, r.left + d, r.bottom), 90f, 90f, false)
    lineTo(r.left, r.top + rr)
    arcTo(Rect(r.left, r.top, r.left + d, r.top + d), 180f, 90f, false)
    close()
}

/** The first [progress] of [outline], drawn as layered strokes for a glow, with a bright head at the leading end. */
private fun DrawScope.drawTrace(outline: Path, progress: Float, color: Color, glow: Float) {
    val measure = PathMeasure().apply { setPath(outline, false) }
    val end = measure.length * progress.coerceIn(0f, 1f)
    val trace = Path()
    measure.getSegment(0f, end, trace, true)

    drawPath(trace, color.copy(alpha = 0.18f * glow), style = Stroke(16.dp.toPx(), cap = StrokeCap.Round))
    drawPath(trace, color.copy(alpha = 0.35f * glow), style = Stroke(9.dp.toPx(), cap = StrokeCap.Round))
    drawPath(trace, color, style = Stroke(4.dp.toPx(), cap = StrokeCap.Round))
    drawPath(trace, Color.White.copy(alpha = 0.7f), style = Stroke(1.5.dp.toPx(), cap = StrokeCap.Round))

    if (progress < 1f) {
        val head = measure.getPosition(end)
        drawCircle(color.copy(alpha = 0.4f * glow), radius = 10.dp.toPx(), center = head)
        drawCircle(Color.White, radius = 3.5.dp.toPx(), center = head)
    }
}

/** AR-style L brackets at each corner of [r]. */
private fun DrawScope.drawCornerBrackets(r: Rect, color: Color) {
    val len = min(r.width, r.height) * 0.22f
    val stroke = 3.dp.toPx()
    fun l(corner: Offset, hx: Float, vy: Float) {
        drawLine(color, corner, Offset(corner.x + hx * len, corner.y), stroke, StrokeCap.Square)
        drawLine(color, corner, Offset(corner.x, corner.y + vy * len), stroke, StrokeCap.Square)
    }
    l(r.topLeft, 1f, 1f)
    l(r.topRight, -1f, 1f)
    l(r.bottomLeft, 1f, -1f)
    l(r.bottomRight, -1f, -1f)
}
