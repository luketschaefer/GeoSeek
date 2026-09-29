package com.geoseek.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.geoseek.domain.catalog.Environment
import com.geoseek.domain.catalog.Rarity
import com.geoseek.domain.profile.ProfileRepository
import com.geoseek.quests.TodaysQuestProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class QuestUi(
    val targetName: String,
    val rarity: Rarity,
    val environment: Environment,
    val bonusXp: Int,
)

data class HomeUiState(
    val isLoading: Boolean = true,
    val displayName: String = "",
    val level: Int = 1,
    val levelFraction: Float = 0f,
    val quest: QuestUi? = null,
)

@HiltViewModel
class HomeViewModel
    @Inject
    constructor(
        profileRepository: ProfileRepository,
        questProvider: TodaysQuestProvider,
    ) : ViewModel() {
        private val quest =
            questProvider.today().let {
                QuestUi(
                    it.target.name,
                    it.target.rarity,
                    it.environment,
                    it.bonusXp,
                )
            }

        val uiState: StateFlow<HomeUiState> =
            profileRepository
                .observeProfile()
                .map { profile ->
                    HomeUiState(
                        isLoading = false,
                        displayName = profile.displayName,
                        level = profile.level.level,
                        levelFraction = profile.level.fraction,
                        quest = quest,
                    )
                }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), HomeUiState())

        private companion object {
            const val STOP_TIMEOUT_MS = 5_000L
        }
    }
