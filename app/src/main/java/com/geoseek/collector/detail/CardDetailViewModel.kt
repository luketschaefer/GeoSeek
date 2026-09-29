package com.geoseek.collector.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.geoseek.BuildConfig
import com.geoseek.collector.navigation.CardDetailRoute
import com.geoseek.domain.catalog.Catalog
import com.geoseek.domain.catalog.Environment
import com.geoseek.domain.catalog.ObjectId
import com.geoseek.domain.catalog.Rarity
import com.geoseek.domain.collector.CollectionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

data class CardDetailUiState(
    val objectId: String,
    val name: String,
    val rarity: Rarity,
    val points: Int,
    val xp: Int,
    val environments: List<Environment>,
    val count: Int = 0,
    val firstFound: LocalDate? = null,
    /** Debug builds can open the reveal for any card, so AR can be tested without a real find. */
    val canReveal: Boolean = false,
)

@HiltViewModel
class CardDetailViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        catalog: Catalog,
        collection: CollectionRepository,
        clock: Clock,
    ) : ViewModel() {
        private val obj = catalog.require(ObjectId(savedStateHandle.toRoute<CardDetailRoute>().objectId))
        private val initial =
            CardDetailUiState(
                objectId = obj.id.value,
                name = obj.name,
                rarity = obj.rarity,
                points = obj.points,
                xp = obj.xp,
                environments = obj.environments.sortedBy { it.ordinal },
            )

        val uiState: StateFlow<CardDetailUiState> =
            collection
                .observeCard(obj.id)
                .map { card ->
                    initial.copy(
                        count = card?.count ?: 0,
                        canReveal = BuildConfig.DEBUG || (card?.count ?: 0) > 0,
                        firstFound = card?.firstAcquiredAt?.atZone(clock.zone)?.toLocalDate(),
                    )
                }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), initial)

        private companion object {
            const val STOP_TIMEOUT_MS = 5_000L
        }
    }
