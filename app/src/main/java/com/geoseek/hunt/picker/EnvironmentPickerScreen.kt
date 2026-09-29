package com.geoseek.hunt.picker

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.geoseek.R
import com.geoseek.core.designsystem.GeoSeekTheme
import com.geoseek.core.designsystem.labelRes
import com.geoseek.core.ui.GeoScaffold
import com.geoseek.domain.catalog.Environment

@Composable
fun EnvironmentPickerScreen(
    onPick: (Environment) -> Unit,
    onBack: () -> Unit,
    viewModel: EnvironmentPickerViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    EnvironmentPickerContent(state = state, onPick = onPick, onBack = onBack)
}

@Composable
fun EnvironmentPickerContent(
    state: EnvironmentPickerUiState,
    onPick: (Environment) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GeoScaffold(title = stringResource(R.string.picker_title), onBack = onBack, modifier = modifier) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(state.options, key = { it.environment }) { option ->
                Card(onClick = { onPick(option.environment) }, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(stringResource(option.environment.labelRes()), style = MaterialTheme.typography.titleLarge)
                        Text(
                            pluralStringResource(R.plurals.picker_object_count, option.objectCount, option.objectCount),
                        )
                        if (option.hasDailyQuest) {
                            Text(
                                stringResource(R.string.picker_daily_quest_here),
                                color = MaterialTheme.colorScheme.tertiary,
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun EnvironmentPickerPreview() {
    GeoSeekTheme {
        EnvironmentPickerContent(
            state =
                EnvironmentPickerUiState(
                    Environment.entries.map { EnvironmentOption(it, 8, it == Environment.PARK) },
                ),
            onPick = {},
            onBack = {},
        )
    }
}
