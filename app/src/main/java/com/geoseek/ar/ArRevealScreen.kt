package com.geoseek.ar

import androidx.activity.compose.LocalActivity
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.geoseek.R
import com.geoseek.core.designsystem.GeoCard
import com.geoseek.core.designsystem.GeoSeekTheme
import com.geoseek.core.ui.GeoScaffold
import com.geoseek.core.ui.LoadingView
import com.geoseek.domain.catalog.Rarity
import com.google.ar.core.ArCoreApk
import com.google.ar.core.exceptions.UnavailableException
import io.github.sceneview.ar.ARSceneView

@Composable
fun ArRevealScreen(
    onDone: () -> Unit,
    viewModel: ArRevealViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val activity = LocalActivity.current
    // Returning from the Play Store ARCore install lands here via onResume.
    LifecycleResumeEffect(Unit) {
        if (state.availability == ArAvailability.NEEDS_INSTALL) viewModel.recheckAvailability()
        onPauseOrDispose {}
    }
    ArRevealContent(
        state = state,
        onDone = onDone,
        onInstallAr = {
            try {
                activity?.let { ArCoreApk.getInstance().requestInstall(it, true) }
            } catch (_: UnavailableException) {
                viewModel.useFallback()
            }
        },
        onSkipAr = viewModel::useFallback,
    )
}

@Composable
fun ArRevealContent(
    state: ArRevealUiState,
    onDone: () -> Unit,
    onInstallAr: () -> Unit,
    onSkipAr: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GeoScaffold(title = stringResource(R.string.reveal_title), modifier = modifier) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                state.availability == ArAvailability.CHECKING -> {
                    LoadingView()
                }

                state.showAr -> {
                    ArReveal(state, onDone = onDone, onArFail = onSkipAr)
                }

                state.availability == ArAvailability.NEEDS_INSTALL && !state.forceFallback -> {
                    InstallPrompt(onInstallAr = onInstallAr, onSkipAr = onSkipAr)
                }

                else -> {
                    Fallback2DReveal(state, onDone)
                }
            }
        }
    }
}

/** AR placeholder: live ARCore scene with plane detection; placing the 3D card model is a TODO. */
@Composable
private fun ArReveal(
    state: ArRevealUiState,
    onDone: () -> Unit,
    onArFail: () -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        // A session failure (e.g. camera still busy) drops to the 2D reveal instead of a black screen.
        ARSceneView(modifier = Modifier.fillMaxSize(), onSessionFailed = { onArFail() })
        Column(
            modifier = Modifier.align(Alignment.BottomCenter).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.reveal_ar_hint, state.objectName),
                style = MaterialTheme.typography.titleMedium,
            )
            Button(onClick = onDone) { Text(stringResource(R.string.done)) }
        }
    }
}

@Composable
private fun InstallPrompt(
    onInstallAr: () -> Unit,
    onSkipAr: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.reveal_install_title), style = MaterialTheme.typography.titleLarge)
        Text(stringResource(R.string.reveal_install_body))
        Button(onClick = onInstallAr) { Text(stringResource(R.string.reveal_install_action)) }
        OutlinedButton(onClick = onSkipAr) { Text(stringResource(R.string.reveal_skip_ar)) }
    }
}

/** 2D reveal for devices without ARCore: the card springs in. */
@Composable
fun Fallback2DReveal(
    state: ArRevealUiState,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scale = remember { Animatable(0.3f) }
    LaunchedEffect(Unit) {
        scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
    }
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.reveal_found, state.objectName), style = MaterialTheme.typography.headlineMedium)
        GeoCard(
            name = state.objectName,
            rarity = state.rarity,
            points = state.points,
            count = 1,
            modifier = Modifier.fillMaxWidth(CARD_WIDTH_FRACTION).scale(scale.value),
        )
        Button(onClick = onDone) { Text(stringResource(R.string.done)) }
    }
}

private const val CARD_WIDTH_FRACTION = 0.6f

@Preview
@Composable
private fun FallbackPreview() {
    GeoSeekTheme {
        ArRevealContent(
            state = ArRevealUiState("Helicopter", Rarity.LEGENDARY, 250, ArAvailability.UNSUPPORTED),
            onDone = {},
            onInstallAr = {},
            onSkipAr = {},
        )
    }
}
