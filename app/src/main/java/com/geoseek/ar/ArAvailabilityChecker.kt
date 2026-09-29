package com.geoseek.ar

import android.content.Context
import com.google.ar.core.ArCoreApk
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import kotlin.coroutines.resume

enum class ArAvailability {
    CHECKING,
    SUPPORTED,

    /** Device is capable but Google Play Services for AR is missing or too old. */
    NEEDS_INSTALL,

    /** Not capable, or the check failed: use the 2D reveal. */
    UNSUPPORTED,
}

fun interface ArAvailabilityChecker {
    suspend fun check(): ArAvailability
}

class ArCoreAvailabilityChecker
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
    ) : ArAvailabilityChecker {
        override suspend fun check(): ArAvailability =
            suspendCancellableCoroutine { cont ->
                ArCoreApk.getInstance().checkAvailabilityAsync(context) { availability ->
                    if (cont.isActive) cont.resume(availability.toArAvailability())
                }
            }
    }

internal fun ArCoreApk.Availability.toArAvailability(): ArAvailability =
    when (this) {
        ArCoreApk.Availability.SUPPORTED_INSTALLED -> ArAvailability.SUPPORTED

        ArCoreApk.Availability.SUPPORTED_NOT_INSTALLED,
        ArCoreApk.Availability.SUPPORTED_APK_TOO_OLD,
        -> ArAvailability.NEEDS_INSTALL

        // The async check only reports final values; treat a stray CHECKING like a failed check.
        ArCoreApk.Availability.UNKNOWN_CHECKING,
        ArCoreApk.Availability.UNSUPPORTED_DEVICE_NOT_CAPABLE,
        ArCoreApk.Availability.UNKNOWN_ERROR,
        ArCoreApk.Availability.UNKNOWN_TIMED_OUT,
        -> ArAvailability.UNSUPPORTED
    }
