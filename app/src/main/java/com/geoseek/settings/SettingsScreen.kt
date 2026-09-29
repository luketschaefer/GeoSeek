package com.geoseek.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.geoseek.R
import com.geoseek.core.designsystem.GeoSeekTheme
import com.geoseek.core.ui.GeoScaffold

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    SettingsContent(state = state, onBack = onBack)
}

@Composable
fun SettingsContent(
    state: SettingsUiState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GeoScaffold(title = stringResource(R.string.settings_title), onBack = onBack, modifier = modifier) { padding ->
        Column(Modifier.padding(padding)) {
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_version)) },
                supportingContent = { Text(state.appVersion) },
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_detector)) },
                supportingContent = { Text(state.detectorName) },
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_catalog)) },
                supportingContent = { Text(state.catalogSize.toString()) },
            )
            ListItem(headlineContent = { Text(stringResource(R.string.coming_soon)) })
        }
    }
}

@Preview
@Composable
private fun SettingsPreview() {
    GeoSeekTheme { SettingsContent(SettingsUiState("0.1.0", "ML Kit (on-device)", 31), onBack = {}) }
}
