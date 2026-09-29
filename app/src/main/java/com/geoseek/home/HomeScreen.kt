package com.geoseek.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.geoseek.core.designsystem.RarityBadge
import com.geoseek.core.ui.GeoScaffold
import com.geoseek.core.ui.LoadingView
import com.geoseek.domain.catalog.Environment
import com.geoseek.domain.catalog.Rarity
import com.geoseek.hunt.picker.labelRes

data class HomeActions(
    val onStartHunt: () -> Unit = {},
    val onCollection: () -> Unit = {},
    val onProfile: () -> Unit = {},
    val onTrading: () -> Unit = {},
    val onSettings: () -> Unit = {},
)

@Composable
fun HomeScreen(
    actions: HomeActions,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HomeContent(state = state, actions = actions)
}

@Composable
fun HomeContent(
    state: HomeUiState,
    actions: HomeActions,
    modifier: Modifier = Modifier,
) {
    GeoScaffold(title = stringResource(R.string.app_name), modifier = modifier) { padding ->
        if (state.isLoading) {
            LoadingView(Modifier.padding(padding))
            return@GeoScaffold
        }
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                stringResource(R.string.home_greeting, state.displayName),
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(stringResource(R.string.level_n, state.level), style = MaterialTheme.typography.titleMedium)
            LinearProgressIndicator(progress = { state.levelFraction }, modifier = Modifier.fillMaxWidth())
            state.quest?.let { QuestCard(it) }
            Button(onClick = actions.onStartHunt, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.home_start_hunt))
            }
            OutlinedButton(onClick = actions.onCollection, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.collection_title))
            }
            OutlinedButton(onClick = actions.onProfile, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.profile_title))
            }
            OutlinedButton(onClick = actions.onTrading, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.trading_title))
            }
            OutlinedButton(onClick = actions.onSettings, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.settings_title))
            }
        }
    }
}

@Composable
private fun QuestCard(quest: QuestUi) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stringResource(R.string.home_daily_quest), style = MaterialTheme.typography.labelLarge)
            Text(
                stringResource(
                    R.string.home_quest_body,
                    quest.targetName,
                    stringResource(quest.environment.labelRes()),
                ),
                style = MaterialTheme.typography.titleMedium,
            )
            RarityBadge(quest.rarity)
            Text(stringResource(R.string.home_quest_bonus, quest.bonusXp), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Preview
@Composable
private fun HomeScreenPreview() {
    GeoSeekTheme {
        HomeContent(
            state =
                HomeUiState(
                    isLoading = false,
                    displayName = "Seeker",
                    level = 3,
                    levelFraction = 0.4f,
                    quest = QuestUi("Bench", Rarity.COMMON, Environment.PARK, 55),
                ),
            actions = HomeActions(),
        )
    }
}
