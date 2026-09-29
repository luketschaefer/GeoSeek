package com.geoseek.hunt.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.geoseek.hunt.HuntScreen
import com.geoseek.hunt.picker.EnvironmentPickerScreen
import com.geoseek.hunt.results.ResultsScreen
import kotlinx.serialization.Serializable

@Serializable
data object EnvironmentPickerRoute

/**
 * Environment is passed by name: Navigation lint requires @Keep on enum nav args, and the
 * pure-Kotlin domain module can't depend on androidx.annotation.
 */
@Serializable
data class HuntRoute(
    val environmentName: String,
)

/** [roundId] of [ResultsRoute.NO_ROUND] shows the "nothing recorded" state. */
@Serializable
data class ResultsRoute(
    val roundId: Long,
) {
    companion object {
        const val NO_ROUND = -1L
    }
}

/**
 * Hunt feature destinations. Cross-feature navigation goes through the callbacks so this
 * package never imports another feature.
 */
fun NavGraphBuilder.huntGraph(
    navController: NavController,
    onReveal: (objectId: String) -> Unit,
    onHome: () -> Unit,
) {
    composable<EnvironmentPickerRoute> {
        EnvironmentPickerScreen(
            onPick = { navController.navigate(HuntRoute(it.name)) },
            onBack = navController::popBackStack,
        )
    }
    composable<HuntRoute> {
        HuntScreen(
            onReveal = onReveal,
            onFinish = {
                navController.navigate(ResultsRoute(ResultsRoute.NO_ROUND)) {
                    popUpTo<EnvironmentPickerRoute>()
                }
            },
            onBack = navController::popBackStack,
        )
    }
    composable<ResultsRoute> {
        ResultsScreen(onDone = onHome, onPlayAgain = { navController.popBackStack() })
    }
}
