package com.geoseek.core.permissions

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.lifecycle.compose.LifecycleResumeEffect

enum class PermissionStatus {
    GRANTED,

    /** Never asked: request directly. */
    NOT_REQUESTED,

    /** Denied once: explain why before asking again. */
    SHOW_RATIONALE,

    /** Denied with "don't ask again" (or twice on Android 11+): only Settings can grant it. */
    PERMANENTLY_DENIED,
}

/**
 * Android reports `shouldShowRequestPermissionRationale == false` both before the first request
 * and after a permanent denial; [requestedBefore] disambiguates. Known edge case: dismissing the
 * system dialog without answering can look permanent, so the UI still offers "Try again".
 */
fun resolvePermissionStatus(
    granted: Boolean,
    shouldShowRationale: Boolean,
    requestedBefore: Boolean,
): PermissionStatus =
    when {
        granted -> PermissionStatus.GRANTED
        shouldShowRationale -> PermissionStatus.SHOW_RATIONALE
        requestedBefore -> PermissionStatus.PERMANENTLY_DENIED
        else -> PermissionStatus.NOT_REQUESTED
    }

@Stable
class CameraPermissionState internal constructor(
    status: PermissionStatus,
    private val onRequest: () -> Unit,
    private val onOpenSettings: () -> Unit,
) {
    var status: PermissionStatus by mutableStateOf(status)
        internal set

    fun request() = onRequest()

    fun openSettings() = onOpenSettings()
}

/** Camera permission state that re-evaluates on resume (e.g. returning from system Settings). */
@Composable
fun rememberCameraPermissionState(): CameraPermissionState {
    val context = LocalContext.current
    val activity = LocalActivity.current
    val prefs = remember { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }

    fun evaluate(): PermissionStatus =
        resolvePermissionStatus(
            granted = ContextCompat.checkSelfPermission(context, PERMISSION) == PackageManager.PERMISSION_GRANTED,
            shouldShowRationale = activity?.shouldShowRequestPermissionRationale(PERMISSION) == true,
            requestedBefore = prefs.getBoolean(KEY_REQUESTED, false),
        )

    var state: CameraPermissionState? = null
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
            prefs.edit { putBoolean(KEY_REQUESTED, true) }
            state?.status = evaluate()
        }
    state =
        remember {
            CameraPermissionState(
                status = evaluate(),
                onRequest = { launcher.launch(PERMISSION) },
                onOpenSettings = {
                    context.startActivity(
                        Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", context.packageName, null),
                        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                },
            )
        }
    LifecycleResumeEffect(state) {
        state.status = evaluate()
        onPauseOrDispose {}
    }
    return state
}

private const val PERMISSION = Manifest.permission.CAMERA
private const val PREFS = "permissions"
private const val KEY_REQUESTED = "camera_requested"
