package com.geoseek.social

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.geoseek.domain.social.ProfileRemoteRepository
import com.geoseek.domain.social.TradeOffer
import com.geoseek.domain.social.TradeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

sealed interface TradingUiState {
    data object Loading : TradingUiState

    /** No account yet: trading needs sign-in (Firebase Auth, see ROADMAP.md). */
    data object SignedOut : TradingUiState

    data class SignedIn(
        val offers: List<TradeOffer>,
    ) : TradingUiState
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TradingViewModel
    @Inject
    constructor(
        remoteProfiles: ProfileRemoteRepository,
        trades: TradeRepository,
    ) : ViewModel() {
        val uiState: StateFlow<TradingUiState> =
            remoteProfiles
                .currentUser()
                .flatMapLatest { user ->
                    if (user == null) {
                        flowOf(TradingUiState.SignedOut)
                    } else {
                        trades.observeOffers(user).map { TradingUiState.SignedIn(it) }
                    }
                }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), TradingUiState.Loading)

        private companion object {
            const val STOP_TIMEOUT_MS = 5_000L
        }
    }
