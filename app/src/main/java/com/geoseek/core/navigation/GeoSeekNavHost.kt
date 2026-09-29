package com.geoseek.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.geoseek.ar.navigation.ArRevealRoute
import com.geoseek.ar.navigation.arGraph
import com.geoseek.collector.navigation.CollectionRoute
import com.geoseek.collector.navigation.collectorGraph
import com.geoseek.home.HomeActions
import com.geoseek.home.HomeScreen
import com.geoseek.hunt.navigation.EnvironmentPickerRoute
import com.geoseek.hunt.navigation.huntGraph
import com.geoseek.profile.navigation.ProfileRoute
import com.geoseek.profile.navigation.profileGraph
import com.geoseek.settings.SettingsRoute
import com.geoseek.settings.settingsGraph
import com.geoseek.social.navigation.TradingRoute
import com.geoseek.social.navigation.socialGraph
import kotlinx.serialization.Serializable

@Serializable
data object HomeRoute

/**
 * The only place that knows every feature. Each feature contributes one `xxxGraph()` call;
 * cross-feature navigation is wired here via callbacks.
 */
@Composable
fun GeoSeekNavHost(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val toHome: () -> Unit = { navController.popBackStack(HomeRoute, inclusive = false) }
    val toReveal: (String) -> Unit = { navController.navigate(ArRevealRoute(it)) }

    NavHost(navController = navController, startDestination = HomeRoute, modifier = modifier) {
        composable<HomeRoute> { HomeScreen(actions = navController.homeActions()) }
        huntGraph(navController, onReveal = toReveal, onHome = toHome)
        arGraph(onDone = { navController.popBackStack() })
        collectorGraph(navController, onRevealInAr = toReveal)
        profileGraph(navController)
        socialGraph(navController)
        settingsGraph(navController)
    }
}

private fun NavController.homeActions() =
    HomeActions(
        onStartHunt = { navigate(EnvironmentPickerRoute) },
        onCollection = { navigate(CollectionRoute) },
        onProfile = { navigate(ProfileRoute) },
        onTrading = { navigate(TradingRoute) },
        onSettings = { navigate(SettingsRoute) },
    )
