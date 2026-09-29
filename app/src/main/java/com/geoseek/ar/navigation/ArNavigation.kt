package com.geoseek.ar.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.geoseek.ar.ArRevealScreen
import kotlinx.serialization.Serializable

@Serializable
data class ArRevealRoute(
    val objectId: String,
)

fun NavGraphBuilder.arGraph(onDone: () -> Unit) {
    composable<ArRevealRoute> { ArRevealScreen(onDone = onDone) }
}
