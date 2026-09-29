package com.geoseek.collector.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
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
import com.geoseek.domain.catalog.Environment
import com.geoseek.domain.catalog.Rarity
import com.geoseek.hunt.picker.labelRes

@Composable
fun CardDetailScreen(
    onRevealInAr: (objectId: String) -> Unit,
    onBack: () -> Unit,
    viewModel: CardDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    CardDetailContent(state = state, onRevealInAr = { onRevealInAr(state.objectId) }, onBack = onBack)
}

@Composable
fun CardDetailContent(
    state: CardDetailUiState,
    onRevealInAr: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GeoScaffold(title = state.name, onBack = onBack, modifier = modifier) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            GeoCard(
                name = state.name,
                rarity = state.rarity,
                points = state.points,
                count = state.count,
                modifier = Modifier.fillMaxWidth(CARD_WIDTH_FRACTION),
            )
            Text(stringResource(R.string.card_detail_xp, state.xp), style = MaterialTheme.typography.bodyLarge)
            Text(
                stringResource(
                    R.string.card_detail_environments,
                    state.environments.map { stringResource(it.labelRes()) }.joinToString(),
                ),
            )
            Text(
                state.firstFound?.let { stringResource(R.string.card_detail_first_found, it.toString()) }
                    ?: stringResource(R.string.card_detail_not_found),
            )
            if (state.count > 0) {
                OutlinedButton(onClick = onRevealInAr) { Text(stringResource(R.string.card_detail_reveal)) }
            }
        }
    }
}

private const val CARD_WIDTH_FRACTION = 0.6f

@Preview
@Composable
private fun CardDetailPreview() {
    GeoSeekTheme {
        CardDetailContent(
            state = CardDetailUiState("cup", "Cup", Rarity.COMMON, 10, 5, listOf(Environment.KITCHEN), count = 2),
            onRevealInAr = {},
            onBack = {},
        )
    }
}
