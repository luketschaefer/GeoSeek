package com.geoseek.settings

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable
data object SettingsRoute

fun NavGraphBuilder.settingsGraph(navController: NavController) {
    composable<SettingsRoute> { SettingsScreen(onBack = navController::popBackStack) }
}
