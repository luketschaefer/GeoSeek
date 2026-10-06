// The app shell: splash, then sign in or the main menu, all over one shared pixel world.
// The main menu is a hub; each mode opens from it, and the world travels to that mode's biome.
// Call GeoSeekRoot() from MainActivity's setContent.
package com.example.geoseek.screens

import android.graphics.Color as AndroidColor
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.geoseek.engine.GameScoreResult
import com.example.geoseek.models.GameObject
import kotlinx.coroutines.delay

enum class Destination(val scene: Scene) {
    Home(Scenes.Home),
    Hunt(Scenes.Hunt),
    Explore(Scenes.Explore),
    Collection(Scenes.Collection),
    Profile(Scenes.Profile),
}

private enum class Stage { Splash, Auth, Main }

@Composable
fun GeoSeekRoot() {
    val context = LocalContext.current
    val activity = LocalActivity.current
    val store = remember { AuthStore(context) }

    var player by remember { mutableStateOf(store.currentPlayer()) }
    var splashDone by rememberSaveable { mutableStateOf(false) }
    var destination by rememberSaveable { mutableStateOf(Destination.Home) }
    var immersive by remember { mutableStateOf(false) }
    // Keeps the menu drawn while it animates out after signing out.
    var lastPlayer by remember { mutableStateOf(player) }
    if (player != null) lastPlayer = player

    // Draw behind the system bars with light icons, whatever the window theme says.
    LaunchedEffect(activity) {
        (activity as? ComponentActivity)?.enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
        )
    }

    val stage = when {
        !splashDone -> Stage.Splash
        player == null -> Stage.Auth
        else -> Stage.Main
    }
    val scene = if (stage == Stage.Main) destination.scene else Scenes.Title

    Box(Modifier.fillMaxSize().background(GeoColors.Night)) {
        WorldBackground(scene, Modifier.fillMaxSize(), dim = if (immersive) 0.5f else 0f)

        AnimatedContent(
            targetState = stage,
            transitionSpec = {
                (fadeIn(tween(600, delayMillis = 200)) + scaleIn(tween(700, delayMillis = 200), initialScale = 0.92f)) togetherWith
                    (fadeOut(tween(300)) + scaleOut(tween(400), targetScale = 1.08f))
            },
            label = "stage",
        ) { s ->
            when (s) {
                Stage.Splash -> Splash(player, onFinished = { splashDone = true })
                Stage.Auth -> AuthScreen(store, onAuthenticated = {
                    destination = Destination.Home
                    player = it
                })
                Stage.Main -> lastPlayer?.let { current ->
                    MainContent(
                        player = current,
                        destination = destination,
                        onNavigate = { destination = it },
                        onImmersiveChange = { immersive = it },
                        onHuntFinished = { result, found -> player = store.recordHunt(current, result.totalScore, result.totalXpEarned, found) },
                        onCollect = { obj -> player = store.collect(current, obj) },
                        onClaimQuest = { reward -> player = store.claimQuest(current, reward) },
                        onSignOut = {
                            store.signOut()
                            immersive = false
                            player = null
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun Splash(player: Player?, onFinished: () -> Unit) {
    var started by remember { mutableStateOf(false) }
    var dots by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        started = true
        repeat(6) {
            delay(280)
            dots = (dots + 1) % 4
        }
        onFinished()
    }
    val drop by animateFloatAsState(if (started) 1f else 0f, spring(0.45f, Spring.StiffnessLow), label = "drop")
    val sub by animateFloatAsState(if (started) 1f else 0f, tween(500, delayMillis = 500), label = "sub")

    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        GeoLogo(Modifier.graphicsLayer {
            translationY = (1f - drop) * -260.dp.toPx()
            alpha = drop.coerceIn(0f, 1f)
        })
        Spacer(Modifier.height(18.dp))
        OutlinedText(
            if (player != null) "Welcome back, ${player.username}" + ".".repeat(dots) else "Generating world" + ".".repeat(dots),
            GeoType.Heading,
            Modifier.graphicsLayer { alpha = sub },
            color = GeoColors.TextSoft,
        )
    }
}

@Composable
private fun MainContent(
    player: Player,
    destination: Destination,
    onNavigate: (Destination) -> Unit,
    onImmersiveChange: (Boolean) -> Unit,
    onHuntFinished: (result: GameScoreResult, found: List<GameObject>) -> Unit,
    onCollect: (GameObject) -> Unit,
    onClaimQuest: (Int) -> Unit,
    onSignOut: () -> Unit,
) {
    val goHome = { onNavigate(Destination.Home) }
    BackHandler(enabled = destination != Destination.Home) {
        onImmersiveChange(false)
        goHome()
    }

    AnimatedContent(
        targetState = destination,
        transitionSpec = {
            // Into a mode, the menu slides off to the left as the world travels right; back out, the reverse.
            val dir = if (targetState == Destination.Home) -1 else 1
            (fadeIn(tween(420, delayMillis = 160)) + slideInHorizontally(tween(560, delayMillis = 80)) { it / 3 * dir }) togetherWith
                (fadeOut(tween(220)) + slideOutHorizontally(tween(420)) { -it / 3 * dir })
        },
        label = "destination",
    ) { d ->
        when (d) {
            Destination.Home -> HomeScreen(player, onOpen = onNavigate, onClaimQuest = onClaimQuest)
            Destination.Hunt -> HuntScreen(
                player = player,
                onBack = goHome,
                onImmersiveChange = onImmersiveChange,
                onHuntFinished = onHuntFinished,
            )
            Destination.Explore -> ExploreScreen(player, onBack = goHome, onCollect = onCollect)
            Destination.Collection -> CollectionScreen(player, onBack = goHome)
            Destination.Profile -> ProfileScreen(player, onBack = goHome, onSignOut = onSignOut)
        }
    }
}
