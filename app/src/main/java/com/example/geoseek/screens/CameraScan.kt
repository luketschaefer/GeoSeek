// The camera viewfinder used by Hunt and Free Look: permission flow, live CameraX preview,
// and ML Kit recognition through ObjectDetector. Falls back to a placeholder without camera access.
package com.example.geoseek.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.geoseek.detection.DetectionResult
import com.example.geoseek.detection.ObjectDetector
import com.example.geoseek.models.GameObject
import kotlinx.coroutines.delay

/**
 * A live camera viewfinder that reports each object from [targetPool] it recognizes.
 * After a recognition it pauses briefly, then keeps scanning.
 */
@Composable
fun CameraScan(
    targetPool: List<GameObject>,
    accent: Color,
    modifier: Modifier = Modifier,
    label: String? = null,
    onRecognized: (GameObject) -> Unit,
    overlay: @Composable () -> Unit = {},
) {
    val context = LocalContext.current
    val activity = LocalActivity.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var permissionRevision by remember { mutableIntStateOf(0) }
    var permanentlyDenied by remember { mutableStateOf(false) }
    var scanId by remember { mutableIntStateOf(0) }
    var detection by remember { mutableStateOf(DetectionResult()) }
    var cameraError by remember { mutableStateOf<String?>(null) }
    val currentOnRecognized by rememberUpdatedState(onRecognized)

    // Re-check the permission whenever the screen resumes (e.g. coming back from Settings).
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) permissionRevision++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        permanentlyDenied = !granted && activity != null &&
            !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.CAMERA)
        permissionRevision++
    }
    val hasPermission = permissionRevision.let {
        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }

    // Hand each confirmed object up once, then resume scanning after a short beat.
    LaunchedEffect(detection.recognizedObject) {
        val found = detection.recognizedObject ?: return@LaunchedEffect
        currentOnRecognized(found)
        delay(1500)
        detection = DetectionResult()
        scanId++
    }

    // A failed frame analysis retries on its own rather than leaving the viewfinder stuck.
    LaunchedEffect(detection.error) {
        if (detection.error == null) return@LaunchedEffect
        delay(2000)
        detection = DetectionResult()
        scanId++
    }

    val status = when {
        !hasPermission -> null
        cameraError != null -> cameraError
        detection.error != null -> detection.error
        targetPool.isEmpty() -> "Nothing left to find here"
        else -> detection.visibleLabel?.let { "Camera sees: $it" } ?: "Scanning…"
    }

    Viewfinder(accent, modifier, label = label, status = status, transparent = hasPermission) {
        if (hasPermission && targetPool.isNotEmpty()) {
            CameraPreview(
                targetPool = targetPool,
                scanId = scanId,
                modifier = Modifier.fillMaxSize().padding(4.dp).clip(PixelShape(4.dp)),
                onResult = { detection = it },
                onError = { cameraError = it },
            )
        }
        if (!hasPermission) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                OutlinedText("Camera needed", GeoType.Heading, outlineWidth = 2.5.dp)
                Text(
                    "Objects are recognized on your phone. Frames are never saved or uploaded.",
                    style = GeoType.Caption, color = GeoColors.TextSoft, textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(12.dp))
                PixelButton(
                    if (permanentlyDenied) "Open Settings" else "Enable Camera",
                    {
                        if (permanentlyDenied) {
                            context.startActivity(
                                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                    .setData(Uri.fromParts("package", context.packageName, null)),
                            )
                        } else {
                            permissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    },
                    color = accent,
                )
            }
        }
        overlay()
    }
}

/** Pixel corner brackets, a sweeping scan line, and a reticle around whatever [content] shows. */
@Composable
fun Viewfinder(
    accent: Color,
    modifier: Modifier = Modifier,
    label: String? = null,
    status: String? = null,
    transparent: Boolean = false,
    content: @Composable () -> Unit = {},
) {
    val motion = rememberInfiniteTransition(label = "scan")
    val scan by motion.animateFloat(0f, 1f, infiniteRepeatable(tween(2600, easing = LinearEasing), RepeatMode.Reverse), label = "scanLine")
    val breathe by motion.animateFloat(0.85f, 1f, infiniteRepeatable(tween(1400), RepeatMode.Reverse), label = "reticle")

    Box(modifier.pixelPanel(if (transparent) Color(0xFF060A20) else Color(0x66060A20), accent.copy(alpha = 0.5f)), contentAlignment = Alignment.Center) {
        content()
        Canvas(Modifier.fillMaxSize().padding(14.dp)) {
            val p = 4.dp.toPx()
            val len = 7
            for (i in 0 until len) {
                drawRect(accent, Offset(i * p, 0f), Size(p, p))
                drawRect(accent, Offset(0f, i * p), Size(p, p))
                drawRect(accent, Offset(size.width - (i + 1) * p, 0f), Size(p, p))
                drawRect(accent, Offset(size.width - p, i * p), Size(p, p))
                drawRect(accent, Offset(i * p, size.height - p), Size(p, p))
                drawRect(accent, Offset(0f, size.height - (i + 1) * p), Size(p, p))
                drawRect(accent, Offset(size.width - (i + 1) * p, size.height - p), Size(p, p))
                drawRect(accent, Offset(size.width - p, size.height - (i + 1) * p), Size(p, p))
            }
            val y = (size.height * scan / p).toInt() * p
            drawRect(accent.copy(alpha = 0.55f), Offset(p * 2, y), Size(size.width - p * 4, p))
            drawRect(accent.copy(alpha = 0.15f), Offset(p * 2, y - p * 3), Size(size.width - p * 4, p * 3))
            val c = center
            val r = (6 * breathe).toInt()
            for (i in 2..r) {
                drawRect(accent.copy(alpha = 0.8f), Offset(c.x - i * p, c.y), Size(p, p))
                drawRect(accent.copy(alpha = 0.8f), Offset(c.x + i * p, c.y), Size(p, p))
                drawRect(accent.copy(alpha = 0.8f), Offset(c.x, c.y - i * p), Size(p, p))
                drawRect(accent.copy(alpha = 0.8f), Offset(c.x, c.y + i * p), Size(p, p))
            }
        }
        if (label != null || status != null) {
            Column(Modifier.align(Alignment.BottomCenter).padding(bottom = 26.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                if (label != null) OutlinedText(label.uppercase(), GeoType.Caption.copy(letterSpacing = 3.sp), color = accent, outlineWidth = 2.dp)
                if (status != null) OutlinedText(status, GeoType.Caption, color = GeoColors.TextSoft, outlineWidth = 2.dp)
            }
        }
    }
}

/**
 * CameraX preview bound to an [ObjectDetector] for [targetPool]. Recreated whenever [scanId] or the pool
 * changes, which resets the detector's frame streak.
 */
@Composable
private fun CameraPreview(
    targetPool: List<GameObject>,
    scanId: Int,
    modifier: Modifier,
    onResult: (DetectionResult) -> Unit,
    onError: (String) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnResult by rememberUpdatedState(onResult)
    val currentOnError by rememberUpdatedState(onError)

    val controller = remember(context, scanId, targetPool) {
        LifecycleCameraController(context).apply {
            cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            setEnabledUseCases(CameraController.IMAGE_ANALYSIS)
            imageAnalysisBackpressureStrategy = ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
        }
    }

    AndroidView(
        modifier = modifier,
        factory = {
            PreviewView(it).apply {
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }
        },
        update = { it.controller = controller },
        onRelease = { it.controller = null },
    )

    DisposableEffect(controller, lifecycleOwner) {
        val detector = ObjectDetector(target = null, targetPool = targetPool)
        val executor = ContextCompat.getMainExecutor(context)
        var disposed = false
        var finished = false

        val analyzer = detector.createAnalyzer(executor) { result ->
            if (!disposed && !finished) {
                currentOnResult(result)
                if (result.recognizedObject != null || result.error != null) {
                    finished = true
                    controller.clearImageAnalysisAnalyzer()
                }
            }
        }

        controller.initializationFuture.addListener(
            {
                if (!disposed) {
                    try {
                        controller.initializationFuture.get()
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                            currentOnError("Camera permission removed. Enable it in app settings.")
                        } else {
                            controller.setImageAnalysisAnalyzer(executor, analyzer)
                            controller.bindToLifecycle(lifecycleOwner)
                        }
                    } catch (_: Exception) {
                        controller.clearImageAnalysisAnalyzer()
                        currentOnError("Couldn't start the camera. Close other camera apps and try again.")
                    }
                }
            },
            executor,
        )

        onDispose {
            disposed = true
            controller.clearImageAnalysisAnalyzer()
            controller.unbind()
            detector.close()
        }
    }
}
