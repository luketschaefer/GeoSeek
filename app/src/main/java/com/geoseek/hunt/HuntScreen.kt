package com.geoseek.hunt

import androidx.camera.compose.CameraXViewfinder
import androidx.camera.core.ImageProxy
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.geoseek.R
import com.geoseek.core.designsystem.GeoSeekTheme
import com.geoseek.core.permissions.CameraPermissionState
import com.geoseek.core.permissions.PermissionStatus
import com.geoseek.core.permissions.rememberCameraPermissionState
import com.geoseek.core.ui.GeoScaffold
import com.geoseek.core.ui.MessageView
import com.geoseek.detection.camera.DetectionCamera
import com.geoseek.domain.catalog.Environment
import com.geoseek.hunt.picker.labelRes
import java.util.Locale

@Composable
fun HuntScreen(
    onReveal: (objectId: String) -> Unit,
    onFinish: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HuntViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val permission = rememberCameraPermissionState()

    val currentOnReveal by rememberUpdatedState(onReveal)
    LaunchedEffect(state.pendingRevealObjectId) {
        state.pendingRevealObjectId?.let {
            viewModel.onRevealHandled()
            currentOnReveal(it)
        }
    }

    GeoScaffold(
        title = stringResource(R.string.hunt_title, stringResource(state.environment.labelRes())),
        onBack = onBack,
        modifier = modifier,
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            if (permission.status == PermissionStatus.GRANTED) {
                CameraPreview(camera = viewModel.camera, analyzer = viewModel::analyze)
                DebugOverlay(
                    state = state,
                    onReveal = viewModel::onRevealClicked,
                    onFinish = onFinish,
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            } else {
                PermissionGate(permission)
            }
        }
    }
}

@Composable
private fun CameraPreview(
    camera: DetectionCamera,
    analyzer: (ImageProxy) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val surfaceRequest by camera.surfaceRequest.collectAsStateWithLifecycle()
    val currentAnalyzer by rememberUpdatedState(analyzer)
    LaunchedEffect(camera, lifecycleOwner) {
        camera.bind(context, lifecycleOwner) { image -> currentAnalyzer(image) }
    }
    surfaceRequest?.let { CameraXViewfinder(surfaceRequest = it, modifier = modifier.fillMaxSize()) }
}

@Composable
private fun PermissionGate(permission: CameraPermissionState) {
    when (permission.status) {
        PermissionStatus.GRANTED -> {
            // The caller shows the camera instead of this gate.
        }

        PermissionStatus.NOT_REQUESTED -> {
            MessageView(
                title = stringResource(R.string.camera_permission_title),
                body = stringResource(R.string.camera_permission_body),
                action = { AllowCameraButton(permission) },
            )
        }

        PermissionStatus.SHOW_RATIONALE -> {
            MessageView(
                title = stringResource(R.string.camera_permission_rationale_title),
                body = stringResource(R.string.camera_permission_rationale_body),
                action = { AllowCameraButton(permission) },
            )
        }

        PermissionStatus.PERMANENTLY_DENIED -> {
            MessageView(
                title = stringResource(R.string.camera_permission_denied_title),
                body = stringResource(R.string.camera_permission_denied_body),
                action = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Button(onClick = permission::openSettings) { Text(stringResource(R.string.open_settings)) }
                        OutlinedButton(onClick = permission::request) { Text(stringResource(R.string.try_again)) }
                    }
                },
            )
        }
    }
}

@Composable
private fun AllowCameraButton(permission: CameraPermissionState) {
    Button(onClick = permission::request) { Text(stringResource(R.string.camera_permission_allow)) }
}

@Composable
private fun DebugOverlay(
    state: HuntUiState,
    onReveal: () -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val debug = state.debug
    Column(
        modifier = modifier.fillMaxWidth().background(Color.Black.copy(alpha = 0.6f)).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            stringResource(R.string.debug_header, state.detectorName, debug.framesAnalyzed),
            color = Color.White,
            style = MaterialTheme.typography.labelSmall,
        )
        Text(
            stringResource(
                R.string.debug_target,
                state.target.name,
                state.target.acceptedLabels.joinToString(),
                state.target.minConfidence.asPercent(),
            ),
            color = Color.White,
            style = MaterialTheme.typography.labelMedium,
        )
        debug.labels.take(MAX_LABELS).forEach { label ->
            Text(
                "${label.label}  ${label.confidence.asPercent()}",
                color = if (label.countsForTarget) Color.Green else Color.White,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Text(
            if (debug.matched) {
                stringResource(R.string.debug_matched)
            } else {
                stringResource(R.string.debug_streak, debug.streak, debug.requiredFrames)
            },
            color = if (debug.matched) Color.Green else Color.Yellow,
            style = MaterialTheme.typography.titleMedium,
        )
        state.lastError?.let { Text(it, color = Color.Red, style = MaterialTheme.typography.bodySmall) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onReveal, enabled = debug.matched) { Text(stringResource(R.string.debug_reveal)) }
            OutlinedButton(onClick = onFinish) { Text(stringResource(R.string.debug_finish), color = Color.White) }
        }
    }
}

private fun Float.asPercent(): String = String.format(Locale.ROOT, "%.0f%%", this * PERCENT)

private const val MAX_LABELS = 6
private const val PERCENT = 100

@Preview
@Composable
private fun DebugOverlayPreview() {
    GeoSeekTheme {
        DebugOverlay(
            state =
                HuntUiState(
                    environment = Environment.KITCHEN,
                    detectorName = "ML Kit (on-device)",
                    target = HuntTargetUi("cup", "Cup", listOf("Cup"), 0.7f),
                    debug =
                        DebugOverlayState(
                            labels = listOf(LabelUi("Cup", 0.82f, true), LabelUi("Tableware", 0.6f, false)),
                            streak = 2,
                            framesAnalyzed = 120,
                        ),
                ),
            onReveal = {},
            onFinish = {},
        )
    }
}
