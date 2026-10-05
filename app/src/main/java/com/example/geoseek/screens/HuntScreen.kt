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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.geoseek.detection.DetectionResult
import com.example.geoseek.detection.ObjectDetector

/**
 * Displays the chair hunt, including camera permission requests and live recognition.
 *
 * Rechecks permission when the screen resumes, offers app settings after permanent
 * denial, and keeps a confirmed result visible until the user starts another scan.
 * Scan state is local to this screen and is not saved to the player's collection.
 */
@Composable
fun HuntScreen() {
    val context = LocalContext.current
    val activity = LocalActivity.current
    val lifecycleOwner = LocalLifecycleOwner.current
    // This triggers a fresh OS permission check after a dialog or a return from Settings.
    var permissionRevision by remember { mutableIntStateOf(0) }
    var permanentlyDenied by remember { mutableStateOf(false) }
    var showRationale by remember { mutableStateOf(false) }
    var scanId by remember { mutableIntStateOf(0) }
    var detection by remember { mutableStateOf(DetectionResult()) }
    var cameraError by remember { mutableStateOf<String?>(null) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) permissionRevision++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        permanentlyDenied = !granted && activity != null &&
            !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.CAMERA)
        permissionRevision++
    }
    val hasPermission = permissionRevision.let {
        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Find a chair", style = MaterialTheme.typography.headlineSmall)
        Text("Point the rear camera at a well-lit chair and hold steady. Recognition runs on your phone.")

        if (!hasPermission) {
            Text("Camera access is needed to recognize your chair.")
            Button(onClick = {
                when {
                    ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                        PackageManager.PERMISSION_GRANTED -> permissionRevision++
                    permanentlyDenied -> context.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                            .setData(Uri.fromParts("package", context.packageName, null))
                    )
                    activity != null && ActivityCompat.shouldShowRequestPermissionRationale(
                        activity, Manifest.permission.CAMERA
                    ) -> showRationale = true
                    else -> permissionLauncher.launch(Manifest.permission.CAMERA)
                }
            }) {
                Text(if (permanentlyDenied) "Open app settings" else "Enable camera")
            }
        } else {
            ChairCameraPreview(
                scanId = scanId,
                modifier = Modifier.fillMaxWidth().weight(1f),
                onResult = { detection = it },
                onError = { cameraError = it },
            )
            val recognized = detection.recognizedObject
            when {
                cameraError != null -> Text(cameraError!!, color = MaterialTheme.colorScheme.error)
                detection.error != null -> Text(detection.error!!, color = MaterialTheme.colorScheme.error)
                recognized != null -> Text(
                    "${recognized.name} recognized! ${(detection.confidence * 100).toInt()}% confidence",
                    style = MaterialTheme.typography.titleLarge,
                )
                else -> {
                    Text("Scanning for a chair…")
                    detection.visibleLabel?.let { Text("Camera sees: $it") }
                }
            }
            if (recognized != null || cameraError != null || detection.error != null) {
                Button(onClick = {
                    detection = DetectionResult()
                    cameraError = null
                    scanId++
                }) {
                    Text(if (recognized != null) "Scan again" else "Retry")
                }
            }
        }
    }

    if (showRationale) {
        AlertDialog(
            onDismissRequest = { showRationale = false },
            title = { Text("Allow camera access") },
            text = { Text("GeoSeek uses the camera to recognize a chair. Frames are processed on this phone and are not saved or uploaded by GeoSeek.") },
            confirmButton = {
                TextButton(onClick = {
                    showRationale = false
                    permissionLauncher.launch(Manifest.permission.CAMERA)
                }) { Text("Continue") }
            },
            dismissButton = {
                TextButton(onClick = { showRationale = false }) { Text("Cancel") }
            },
        )
    }
}

/**
 * Shows a rear-camera preview and analyzes frames while bound to the current lifecycle.
 *
 * Analysis stops after recognition or a detector error. Leaving the composition releases
 * the controller and detector and prevents pending callbacks from updating the screen.
 * Camera permission must already be granted; it is checked again before binding.
 *
 * @param scanId Changing this value creates a fresh camera controller and scan session.
 * @param modifier Layout and sizing for the camera preview.
 * @param onResult Receives detection progress and the final result on the main executor.
 * @param onError Receives a user-facing camera startup error on the main executor.
 */
@Composable
private fun ChairCameraPreview(
    scanId: Int,
    modifier: Modifier,
    onResult: (DetectionResult) -> Unit,
    onError: (String) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnResult by rememberUpdatedState(onResult)
    val currentOnError by rememberUpdatedState(onError)
    val controller = remember(context, scanId) {
        LifecycleCameraController(context).apply {
            cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            setEnabledUseCases(CameraController.IMAGE_ANALYSIS)
            imageAnalysisBackpressureStrategy = ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { PreviewView(it).apply {
            // TextureView avoids SurfaceView drawing over the Compose controls.
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        } },
        update = { it.controller = controller },
        onRelease = { it.controller = null },
    )

    DisposableEffect(controller, lifecycleOwner) {
        val detector = ObjectDetector()
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
        controller.initializationFuture.addListener({
            if (!disposed) {
                try {
                    controller.initializationFuture.get()
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) !=
                        PackageManager.PERMISSION_GRANTED
                    ) {
                        currentOnError("Camera permission was removed. Enable it in app settings.")
                    } else {
                        controller.setImageAnalysisAnalyzer(executor, analyzer)
                        controller.bindToLifecycle(lifecycleOwner)
                    }
                } catch (error: Exception) {
                    controller.clearImageAnalysisAnalyzer()
                    currentOnError("Could not start the rear camera. Close other camera apps and tap Retry.")
                }
            }
        }, executor)

        onDispose {
            disposed = true
            controller.clearImageAnalysisAnalyzer()
            controller.unbind()
            detector.close()
        }
    }
}
