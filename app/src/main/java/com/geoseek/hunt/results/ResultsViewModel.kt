package com.geoseek.hunt.results

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.geoseek.domain.catalog.Environment
import com.geoseek.domain.history.RoundHistoryRepository
import com.geoseek.domain.hunt.RoundStatus
import com.geoseek.hunt.navigation.ResultsRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ResultsUiState {
    data object Loading : ResultsUiState

    data object NoRound : ResultsUiState

    data class Loaded(
        val environment: Environment,
        val status: RoundStatus,
        val score: Int,
        val targetsFound: Int,
        val targetsTotal: Int,
    ) : ResultsUiState
}

@HiltViewModel
class ResultsViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        history: RoundHistoryRepository,
    ) : ViewModel() {
        private val roundId = savedStateHandle.toRoute<ResultsRoute>().roundId
        private val _uiState = MutableStateFlow<ResultsUiState>(ResultsUiState.Loading)
        val uiState: StateFlow<ResultsUiState> = _uiState.asStateFlow()

        init {
            viewModelScope.launch {
                val record = if (roundId == ResultsRoute.NO_ROUND) null else history.get(roundId)
                _uiState.value = record?.let {
                    ResultsUiState.Loaded(it.environment, it.status, it.score, it.targetsFound, it.targetsTotal)
                } ?: ResultsUiState.NoRound
            }
        }
    }
