package com.geoseek.settings

import androidx.lifecycle.ViewModel
import com.geoseek.BuildConfig
import com.geoseek.detection.ObjectDetector
import com.geoseek.domain.catalog.Catalog
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

data class SettingsUiState(
    val appVersion: String,
    val detectorName: String,
    val catalogSize: Int,
)

/** Stub: read-only diagnostics. Real settings (sound, haptics, account) are Could items. */
@HiltViewModel
class SettingsViewModel
    @Inject
    constructor(
        catalog: Catalog,
        private val detector: ObjectDetector,
    ) : ViewModel() {
        private val _uiState =
            MutableStateFlow(
                SettingsUiState(
                    appVersion = BuildConfig.VERSION_NAME,
                    detectorName = detector.name,
                    catalogSize = catalog.objects.size,
                ),
            )
        val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

        override fun onCleared() = detector.close()
    }
