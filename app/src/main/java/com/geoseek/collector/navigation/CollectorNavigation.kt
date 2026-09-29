package com.geoseek.collector.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.geoseek.collector.CollectionScreen
import com.geoseek.collector.detail.CardDetailScreen
import kotlinx.serialization.Serializable

@Serializable
data object CollectionRoute

@Serializable
data class CardDetailRoute(
    val objectId: String,
)

fun NavGraphBuilder.collectorGraph(
    navController: NavController,
    onRevealInAr: (objectId: String) -> Unit,
) {
    composable<CollectionRoute> {
        CollectionScreen(
            onCardClick = { navController.navigate(CardDetailRoute(it)) },
            onBack = navController::popBackStack,
        )
    }
    composable<CardDetailRoute> {
        CardDetailScreen(onRevealInAr = onRevealInAr, onBack = navController::popBackStack)
    }
}
