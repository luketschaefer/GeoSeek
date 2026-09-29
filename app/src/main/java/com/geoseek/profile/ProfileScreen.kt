package com.geoseek.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
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
import com.geoseek.core.designsystem.GeoSeekTheme
import com.geoseek.core.designsystem.labelRes
import com.geoseek.core.ui.GeoScaffold
import com.geoseek.core.ui.LoadingView

@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ProfileContent(state = state, onBack = onBack)
}

@Composable
fun ProfileContent(
    state: ProfileUiState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GeoScaffold(title = stringResource(R.string.profile_title), onBack = onBack, modifier = modifier) { padding ->
        if (state.isLoading) {
            LoadingView(Modifier.padding(padding))
            return@GeoScaffold
        }
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item { Text(state.displayName, style = MaterialTheme.typography.headlineMedium) }
            item { Text(stringResource(R.string.level_n, state.level), style = MaterialTheme.typography.titleMedium) }
            item {
                LinearProgressIndicator(
                    progress = {
                        if (state.xpForNextLevel ==
                            0L
                        ) {
                            0f
                        } else {
                            state.xpIntoLevel.toFloat() / state.xpForNextLevel
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item { Text(stringResource(R.string.profile_xp, state.xpIntoLevel, state.xpForNextLevel, state.totalXp)) }
            item { Text(stringResource(R.string.collection_progress, state.cardsCollected, state.cardsTotal)) }
            item {
                Text(
                    stringResource(R.string.profile_recent_rounds),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }
            if (state.recentRounds.isEmpty()) {
                item { Text(stringResource(R.string.profile_no_rounds)) }
            }
            items(state.recentRounds, key = { it.id }) { round ->
                ListItem(
                    headlineContent = { Text(stringResource(round.environment.labelRes())) },
                    supportingContent = { Text(round.status.name) },
                    trailingContent = { Text(stringResource(R.string.results_score, round.score)) },
                )
            }
        }
    }
}

@Preview
@Composable
private fun ProfilePreview() {
    GeoSeekTheme {
        ProfileContent(
            state = ProfileUiState(false, "Seeker", 3, 50, 300, 350, 4, 31),
            onBack = {},
        )
    }
}
