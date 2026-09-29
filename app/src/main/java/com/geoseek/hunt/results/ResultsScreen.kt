package com.geoseek.hunt.results

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
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
import com.geoseek.core.designsystem.GeoSeekTheme
import com.geoseek.core.ui.GeoScaffold
import com.geoseek.core.ui.LoadingView
import com.geoseek.core.ui.MessageView
import com.geoseek.domain.catalog.Environment
import com.geoseek.domain.hunt.RoundStatus
import com.geoseek.hunt.picker.labelRes

@Composable
fun ResultsScreen(
    onDone: () -> Unit,
    onPlayAgain: () -> Unit,
    viewModel: ResultsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ResultsContent(state = state, onDone = onDone, onPlayAgain = onPlayAgain)
}

@Composable
fun ResultsContent(
    state: ResultsUiState,
    onDone: () -> Unit,
    onPlayAgain: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GeoScaffold(title = stringResource(R.string.results_title), modifier = modifier) { padding ->
        val inner = Modifier.padding(padding)
        when (state) {
            ResultsUiState.Loading -> {
                LoadingView(inner)
            }

            ResultsUiState.NoRound -> {
                MessageView(
                    title = stringResource(R.string.results_none_title),
                    body = stringResource(R.string.results_none_body),
                    action = { Button(onClick = onDone) { Text(stringResource(R.string.results_home)) } },
                    modifier = inner,
                )
            }

            is ResultsUiState.Loaded -> {
                Column(
                    modifier = inner.fillMaxSize().padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(stringResource(state.status.labelRes()), style = MaterialTheme.typography.headlineMedium)
                    Text(stringResource(state.environment.labelRes()))
                    Text(
                        stringResource(R.string.results_score, state.score),
                        style = MaterialTheme.typography.displaySmall,
                    )
                    Text(stringResource(R.string.results_found, state.targetsFound, state.targetsTotal))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onPlayAgain) { Text(stringResource(R.string.results_play_again)) }
                        Button(onClick = onDone) { Text(stringResource(R.string.results_home)) }
                    }
                }
            }
        }
    }
}

private fun RoundStatus.labelRes(): Int =
    when (this) {
        RoundStatus.WON -> R.string.round_won
        RoundStatus.TIME_UP -> R.string.round_time_up
        RoundStatus.ABANDONED -> R.string.round_abandoned
        RoundStatus.NOT_STARTED, RoundStatus.RUNNING -> R.string.round_in_progress
    }

@Preview
@Composable
private fun ResultsPreview() {
    GeoSeekTheme {
        ResultsContent(
            state = ResultsUiState.Loaded(Environment.PARK, RoundStatus.WON, 170, 5, 5),
            onDone = {},
            onPlayAgain = {},
        )
    }
}
