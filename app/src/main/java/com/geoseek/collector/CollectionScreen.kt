package com.geoseek.collector

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.geoseek.R
import com.geoseek.core.designsystem.GeoCard
import com.geoseek.core.designsystem.GeoSeekTheme
import com.geoseek.core.ui.GeoScaffold
import com.geoseek.core.ui.LoadingView
import com.geoseek.domain.catalog.Rarity

@Composable
fun CollectionScreen(
    onCardClick: (objectId: String) -> Unit,
    onBack: () -> Unit,
    viewModel: CollectionViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    CollectionContent(state = state, onCardClick = onCardClick, onBack = onBack)
}

@Composable
fun CollectionContent(
    state: CollectionUiState,
    onCardClick: (objectId: String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GeoScaffold(title = stringResource(R.string.collection_title), onBack = onBack, modifier = modifier) { padding ->
        if (state.isLoading) {
            LoadingView(Modifier.padding(padding))
            return@GeoScaffold
        }
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 140.dp),
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    stringResource(R.string.collection_progress, state.collectedCount, state.items.size),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            items(state.items, key = { it.objectId }) { item ->
                GeoCard(
                    name = item.name,
                    rarity = item.rarity,
                    points = item.points,
                    count = item.count,
                    onClick = { onCardClick(item.objectId) },
                )
            }
        }
    }
}

@Preview
@Composable
private fun CollectionPreview() {
    GeoSeekTheme {
        CollectionContent(
            state =
                CollectionUiState(
                    isLoading = false,
                    items =
                        listOf(
                            CollectionItem("cup", "Cup", Rarity.COMMON, 10, 3),
                            CollectionItem("helicopter", "Helicopter", Rarity.LEGENDARY, 250, 0),
                        ),
                ),
            onCardClick = {},
            onBack = {},
        )
    }
}
