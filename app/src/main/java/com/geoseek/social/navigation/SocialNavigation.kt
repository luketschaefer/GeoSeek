package com.geoseek.social.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.geoseek.social.TradingScreen
import kotlinx.serialization.Serializable

@Serializable
data object TradingRoute

fun NavGraphBuilder.socialGraph(navController: NavController) {
    composable<TradingRoute> { TradingScreen(onBack = navController::popBackStack) }
}
