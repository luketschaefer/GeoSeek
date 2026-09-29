package com.geoseek.hunt

import androidx.camera.core.ImageProxy
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.geoseek.core.di.DefaultDispatcher
import com.geoseek.detection.CameraFrame
import com.geoseek.detection.DetectionException
import com.geoseek.detection.ObjectDetector
import com.geoseek.detection.camera.DetectionCamera
import com.geoseek.domain.catalog.Catalog
import com.geoseek.domain.catalog.Environment
import com.geoseek.domain.catalog.ObjectId
import com.geoseek.hunt.navigation.HuntRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HuntTargetUi(
    val id: String,
    val name: String,
    val acceptedLabels: List<String>,
    val minConfidence: Float,
)

data class HuntUiState(
    val environment: Environment,
    val detectorName: String,
    val target: HuntTargetUi,
    val debug: DebugOverlayState = DebugOverlayState(),
    val lastError: String? = null,
    /** One-shot navigation: set after the camera is fully released; the screen consumes it. */
    val pendingRevealObjectId: String? = null,
)

/**
 * Wiring proof: CameraX frame → ObjectDetector → TargetMatcher → debug overlay, for a
 * hard-coded target. No timer, scoring or saving yet (see ROADMAP.md, Must: hunt round).
 */
@HiltViewModel
class HuntViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        catalog: Catalog,
        private val detector: ObjectDetector,
        val camera: DetectionCamera,
        @param:DefaultDispatcher private val dispatcher: CoroutineDispatcher,
    ) : ViewModel() {
        private val environment = Environment.valueOf(savedStateHandle.toRoute<HuntRoute>().environmentName)
        private val target = catalog.require(ObjectId(DEBUG_TARGET_ID))
        private val tracker = DetectionDebugTracker(target)

        private val _uiState =
            MutableStateFlow(
                HuntUiState(
                    environment = environment,
                    detectorName = detector.name,
                    target =
                        HuntTargetUi(
                            id = target.id.value,
                            name = target.name,
                            acceptedLabels = target.acceptedLabels.sorted(),
                            minConfidence = target.minConfidence,
                        ),
                ),
            )
        val uiState: StateFlow<HuntUiState> = _uiState.asStateFlow()

        /** CameraX analyzer callback (analysis thread). Always closes [image]. */
        fun analyze(image: ImageProxy) {
            viewModelScope.launch(dispatcher) {
                try {
                    val detections = detector.detect(CameraFrame(image))
                    val debug = tracker.onFrame(detections)
                    _uiState.update { it.copy(debug = debug, lastError = null) }
                } catch (e: DetectionException) {
                    _uiState.update { it.copy(lastError = e.message) }
                } finally {
                    image.close()
                }
            }
        }

        fun onRevealClicked() {
            viewModelScope.launch {
                camera.release()
                _uiState.update { it.copy(pendingRevealObjectId = target.id.value) }
            }
        }

        fun onRevealHandled() = _uiState.update { it.copy(pendingRevealObjectId = null) }

        override fun onCleared() {
            detector.close()
            camera.shutdown()
        }

        companion object {
            /** "cup" is easy to find indoors, which makes on-device verification quick. */
            const val DEBUG_TARGET_ID = "cup"
        }
    }
