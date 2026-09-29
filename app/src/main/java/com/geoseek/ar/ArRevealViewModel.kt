package com.geoseek.ar

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.geoseek.ar.navigation.ArRevealRoute
import com.geoseek.domain.catalog.Catalog
import com.geoseek.domain.catalog.ObjectId
import com.geoseek.domain.catalog.Rarity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ArRevealUiState(
    val objectName: String,
    val rarity: Rarity,
    val points: Int,
    val availability: ArAvailability = ArAvailability.CHECKING,
    /** User chose the 2D reveal even though AR could work (e.g. declined the ARCore install). */
    val forceFallback: Boolean = false,
) {
    val showAr: Boolean get() = availability == ArAvailability.SUPPORTED && !forceFallback
}

/**
 * The Hunt screen releases CameraX *before* navigating here, so ARCore can open the camera.
 * Unsupported devices, or users who skip installing ARCore, get the 2D reveal.
 */
@HiltViewModel
class ArRevealViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        catalog: Catalog,
        private val availabilityChecker: ArAvailabilityChecker,
    ) : ViewModel() {
        private val obj = catalog.require(ObjectId(savedStateHandle.toRoute<ArRevealRoute>().objectId))

        private val _uiState = MutableStateFlow(ArRevealUiState(obj.name, obj.rarity, obj.points))
        val uiState: StateFlow<ArRevealUiState> = _uiState.asStateFlow()

        init {
            recheckAvailability()
        }

        /** Call after returning from the ARCore install flow. */
        fun recheckAvailability() {
            viewModelScope.launch {
                val availability = availabilityChecker.check()
                _uiState.update { it.copy(availability = availability) }
            }
        }

        fun useFallback() = _uiState.update { it.copy(forceFallback = true) }
    }
