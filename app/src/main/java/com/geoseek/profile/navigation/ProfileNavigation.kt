package com.geoseek.profile.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.geoseek.profile.ProfileScreen
import kotlinx.serialization.Serializable

@Serializable
data object ProfileRoute

fun NavGraphBuilder.profileGraph(navController: NavController) {
    composable<ProfileRoute> { ProfileScreen(onBack = navController::popBackStack) }
}
