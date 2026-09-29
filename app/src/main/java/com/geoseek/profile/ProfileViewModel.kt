package com.geoseek.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.geoseek.domain.catalog.Catalog
import com.geoseek.domain.catalog.Environment
import com.geoseek.domain.collector.CollectionRepository
import com.geoseek.domain.history.RoundHistoryRepository
import com.geoseek.domain.hunt.RoundStatus
import com.geoseek.domain.profile.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class RoundSummaryUi(
    val id: Long,
    val environment: Environment,
    val status: RoundStatus,
    val score: Int,
)

data class ProfileUiState(
    val isLoading: Boolean = true,
    val displayName: String = "",
    val level: Int = 1,
    val xpIntoLevel: Long = 0,
    val xpForNextLevel: Long = 0,
    val totalXp: Long = 0,
    val cardsCollected: Int = 0,
    val cardsTotal: Int = 0,
    val recentRounds: List<RoundSummaryUi> = emptyList(),
)

@HiltViewModel
class ProfileViewModel
    @Inject
    constructor(
        profileRepository: ProfileRepository,
        collection: CollectionRepository,
        history: RoundHistoryRepository,
        catalog: Catalog,
    ) : ViewModel() {
        val uiState: StateFlow<ProfileUiState> =
            combine(
                profileRepository.observeProfile(),
                collection.observeCards(),
                history.observeRecent(RECENT_ROUNDS),
            ) { profile, cards, rounds ->
                val level = profile.level
                ProfileUiState(
                    isLoading = false,
                    displayName = profile.displayName,
                    level = level.level,
                    xpIntoLevel = level.xpIntoLevel,
                    xpForNextLevel = level.xpForNextLevel,
                    totalXp = profile.totalXp,
                    cardsCollected = cards.count { it.count > 0 },
                    cardsTotal = catalog.objects.size,
                    recentRounds = rounds.map { RoundSummaryUi(it.id, it.environment, it.status, it.score) },
                )
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), ProfileUiState())

        private companion object {
            const val RECENT_ROUNDS = 10
            const val STOP_TIMEOUT_MS = 5_000L
        }
    }
