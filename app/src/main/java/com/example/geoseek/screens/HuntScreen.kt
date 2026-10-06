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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.Alignment
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
import com.example.geoseek.models.GameObject
import com.example.geoseek.models.OBJECT_LIST

/**
 * Displays real-time object hunting with target selection, permission handling,
 * live camera preview, and modular ML Kit detection.
 */
@Composable
fun HuntScreen() {
    val context = LocalContext.current
    val activity = LocalActivity.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var permissionRevision by remember { mutableIntStateOf(0) }
    var permanentlyDenied by remember { mutableStateOf(value = false) }
    var showRationale by remember { mutableStateOf(value = false) }
    var selectedTarget by remember { mutableStateOf<GameObject?>(OBJECT_LIST.first()) }
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
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        permanentlyDenied = !granted && (activity != null) &&
            !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.CAMERA)
        permissionRevision++
    }

    val hasPermission = permissionRevision.let {
        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Object Hunt", style = MaterialTheme.typography.headlineSmall)

        TargetSelectionRow(
            targets = OBJECT_LIST,
            selectedTarget = selectedTarget,
        ) { newTarget ->
            selectedTarget = newTarget
            detection = DetectionResult()
            cameraError = null
            scanId++
        }

        val targetName = selectedTarget?.name ?: "Any Object"
        Text(
            text = "Point camera at a well-lit $targetName and hold steady.",
            style = MaterialTheme.typography.bodyMedium,
        )

        if (!hasPermission) {
            Text("Camera access is needed to recognize objects in real time.")
            Button(
                onClick = {
                    when {
                        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                            PackageManager.PERMISSION_GRANTED -> permissionRevision++
                        permanentlyDenied -> context.startActivity(
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                .setData(Uri.fromParts("package", context.packageName, null)),
                        )
                        (activity != null) && ActivityCompat.shouldShowRequestPermissionRationale(
                            activity, Manifest.permission.CAMERA,
                        ) -> showRationale = true
                        else -> permissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                },
            ) {
                Text(if (permanentlyDenied) "Open app settings" else "Enable camera")
            }
        } else {
            ObjectCameraPreview(
                target = selectedTarget,
                scanId = scanId,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                onResult = { detection = it },
                onError = { cameraError = it },
            )

            val recognized = detection.recognizedObject
            when {
                cameraError != null -> Text(cameraError!!, color = MaterialTheme.colorScheme.error)
                detection.error != null -> Text(detection.error!!, color = MaterialTheme.colorScheme.error)
                recognized != null -> Text(
                    "🎉 ${recognized.name} recognized! +${recognized.points} pts (${(detection.confidence * 100).toInt()}% confidence)",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                else -> {
                    Text("Scanning for $targetName…")
                    detection.visibleLabel?.let { Text("Camera sees: $it") }
                }
            }

            if ((recognized != null) || (cameraError != null) || (detection.error != null)) {
                Button(
                    onClick = {
                        detection = DetectionResult()
                        cameraError = null
                        scanId++
                    },
                ) {
                    Text(if (recognized != null) "Scan again" else "Retry")
                }
            }
        }
    }

    if (showRationale) {
        AlertDialog(
            onDismissRequest = { showRationale = false },
            title = { Text("Allow camera access") },
            text = { Text("GeoSeek uses the camera to recognize items locally on your phone. Frames are not saved or uploaded.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRationale = false
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    },
                ) { Text("Continue") }
            },
            dismissButton = {
                TextButton(
                    onClick = { showRationale = false },
                ) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun TargetSelectionRow(
    targets: List<GameObject>,
    selectedTarget: GameObject?,
    onTargetSelected: (GameObject?) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FilterChip(
            selected = selectedTarget == null,
            onClick = { onTargetSelected(null) },
            label = { Text("Any Object") },
        )
        targets.forEach { target ->
            FilterChip(
                selected = selectedTarget == target,
                onClick = { onTargetSelected(target) },
                label = { Text("${target.name} (${target.points} pts)") },
            )
        }
    }
}

/**
 * Shows a rear-camera preview and analyzes frames with [ObjectDetector].
 */
@Composable
private fun ObjectCameraPreview(
    target: GameObject?,
    scanId: Int,
    modifier: Modifier,
    onResult: (DetectionResult) -> Unit,
    onError: (String) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnResult by rememberUpdatedState(onResult)
    val currentOnError by rememberUpdatedState(onError)

    val controller = remember(context, scanId, target) {
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

    DisposableEffect(controller, lifecycleOwner, target) {
        val detector = ObjectDetector(target = target)
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
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) !=
                            PackageManager.PERMISSION_GRANTED
                        ) {
                            currentOnError("Camera permission removed. Enable in app settings.")
                        } else {
                            controller.setImageAnalysisAnalyzer(executor, analyzer)
                            controller.bindToLifecycle(lifecycleOwner)
                        }
                    } catch (_: Exception) {
                        controller.clearImageAnalysisAnalyzer()
                        currentOnError("Could not start rear camera. Close other camera apps and tap Retry.")
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
