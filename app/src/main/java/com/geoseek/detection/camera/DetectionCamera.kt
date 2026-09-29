package com.geoseek.detection.camera

import android.content.Context
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraState
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceRequest
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.lifecycle.awaitInstance
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.time.Duration.Companion.seconds

/**
 * CameraX Preview + ImageAnalysis for the Hunt screen.
 *
 * Camera ownership rule: ARCore and CameraX must never hold the camera at the same time. Before
 * navigating to AR, call [release]; it unbinds and suspends until CameraX reports CLOSED.
 */
class DetectionCamera
    @Inject
    constructor() {
        private val analysisExecutor: ExecutorService = Executors.newSingleThreadExecutor()
        private val _surfaceRequest = MutableStateFlow<SurfaceRequest?>(null)
        val surfaceRequest: StateFlow<SurfaceRequest?> = _surfaceRequest.asStateFlow()

        private val preview =
            Preview.Builder().build().apply {
                setSurfaceProvider { request -> _surfaceRequest.value = request }
            }
        private val analysis =
            ImageAnalysis
                .Builder()
                // Drop frames while the detector is busy; the analyzer closes each frame when done.
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

        private var provider: ProcessCameraProvider? = null
        private var camera: Camera? = null

        /** Must be called on the main thread. [analyzer] must close every ImageProxy it receives. */
        suspend fun bind(
            context: Context,
            lifecycleOwner: LifecycleOwner,
            analyzer: (ImageProxy) -> Unit,
        ) {
            val cameraProvider = ProcessCameraProvider.awaitInstance(context)
            provider = cameraProvider
            analysis.setAnalyzer(analysisExecutor, analyzer)
            cameraProvider.unbindAll()
            camera =
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    analysis,
                )
        }

        /** Unbinds all use cases and waits (bounded) until the camera device is closed. */
        suspend fun release() =
            withContext(Dispatchers.Main.immediate) {
                val bound = camera
                analysis.clearAnalyzer()
                provider?.unbindAll()
                camera = null
                _surfaceRequest.value = null
                if (bound != null) awaitClosed(bound)
            }

        /** Frees the analysis thread. Call once, from ViewModel.onCleared. */
        fun shutdown() {
            analysisExecutor.shutdown()
        }

        private suspend fun awaitClosed(camera: Camera) {
            val state = camera.cameraInfo.cameraState
            withTimeoutOrNull(CLOSE_TIMEOUT) {
                suspendCancellableCoroutine { cont ->
                    val observer =
                        object : Observer<CameraState> {
                            override fun onChanged(value: CameraState) {
                                if (value.type == CameraState.Type.CLOSED) {
                                    state.removeObserver(this)
                                    if (cont.isActive) cont.resume(Unit)
                                }
                            }
                        }
                    state.observeForever(observer)
                    // Timeout cancellation runs on the main dispatcher we are confined to.
                    cont.invokeOnCancellation { state.removeObserver(observer) }
                }
            }
        }

        private companion object {
            val CLOSE_TIMEOUT = 2.seconds
        }
    }
