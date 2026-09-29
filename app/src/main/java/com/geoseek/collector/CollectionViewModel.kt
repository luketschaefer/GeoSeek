package com.geoseek.collector

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.geoseek.domain.catalog.Catalog
import com.geoseek.domain.catalog.Rarity
import com.geoseek.domain.collector.CollectionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class CollectionItem(
    val objectId: String,
    val name: String,
    val rarity: Rarity,
    val points: Int,
    val count: Int,
)

data class CollectionUiState(
    val isLoading: Boolean = true,
    val items: List<CollectionItem> = emptyList(),
) {
    val collectedCount: Int get() = items.count { it.count > 0 }
}

@HiltViewModel
class CollectionViewModel
    @Inject
    constructor(
        catalog: Catalog,
        collection: CollectionRepository,
    ) : ViewModel() {
        val uiState: StateFlow<CollectionUiState> =
            collection
                .observeCards()
                .map { cards ->
                    val counts = cards.associate { it.objectId to it.count }
                    CollectionUiState(
                        isLoading = false,
                        items =
                            catalog.objects
                                .sortedWith(compareBy({ it.rarity.ordinal }, { it.name }))
                                .map { CollectionItem(it.id.value, it.name, it.rarity, it.points, counts[it.id] ?: 0) },
                    )
                }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), CollectionUiState())

        private companion object {
            const val STOP_TIMEOUT_MS = 5_000L
        }
    }
