package com.geoseek.social

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.geoseek.R
import com.geoseek.core.designsystem.GeoSeekTheme
import com.geoseek.core.ui.GeoScaffold
import com.geoseek.core.ui.LoadingView
import com.geoseek.core.ui.MessageView

@Composable
fun TradingScreen(
    onBack: () -> Unit,
    viewModel: TradingViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    TradingContent(state = state, onBack = onBack)
}

/** Stub: shows sign-in / offer count only. Trade UI is a Should item in ROADMAP.md. */
@Composable
fun TradingContent(
    state: TradingUiState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GeoScaffold(title = stringResource(R.string.trading_title), onBack = onBack, modifier = modifier) { padding ->
        val inner = Modifier.padding(padding)
        when (state) {
            TradingUiState.Loading -> {
                LoadingView(inner)
            }

            TradingUiState.SignedOut -> {
                MessageView(
                    title = stringResource(R.string.trading_signed_out_title),
                    body = stringResource(R.string.trading_signed_out_body),
                    modifier = inner,
                )
            }

            is TradingUiState.SignedIn -> {
                MessageView(
                    title = pluralStringResource(R.plurals.trading_offer_count, state.offers.size, state.offers.size),
                    body = stringResource(R.string.coming_soon),
                    modifier = inner,
                )
            }
        }
    }
}

@Preview
@Composable
private fun TradingPreview() {
    GeoSeekTheme { TradingContent(TradingUiState.SignedOut, onBack = {}) }
}
