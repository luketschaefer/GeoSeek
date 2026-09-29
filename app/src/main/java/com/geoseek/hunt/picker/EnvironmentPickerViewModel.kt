package com.geoseek.hunt.picker

import androidx.lifecycle.ViewModel
import com.geoseek.R
import com.geoseek.domain.catalog.Catalog
import com.geoseek.domain.catalog.Environment
import com.geoseek.quests.TodaysQuestProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

data class EnvironmentOption(
    val environment: Environment,
    val objectCount: Int,
    val hasDailyQuest: Boolean,
)

data class EnvironmentPickerUiState(
    val options: List<EnvironmentOption>,
)

@HiltViewModel
class EnvironmentPickerViewModel
    @Inject
    constructor(
        catalog: Catalog,
        questProvider: TodaysQuestProvider,
    ) : ViewModel() {
        private val questEnvironment = questProvider.today().environment

        private val _uiState =
            MutableStateFlow(
                EnvironmentPickerUiState(
                    options =
                        Environment.entries.map { env ->
                            EnvironmentOption(env, catalog.objectsIn(env).size, hasDailyQuest = env == questEnvironment)
                        },
                ),
            )
        val uiState: StateFlow<EnvironmentPickerUiState> = _uiState.asStateFlow()
    }

fun Environment.labelRes(): Int =
    when (this) {
        Environment.PARK -> R.string.env_park
        Environment.KITCHEN -> R.string.env_kitchen
        Environment.STREET -> R.string.env_street
        Environment.CAMPUS -> R.string.env_campus
    }
