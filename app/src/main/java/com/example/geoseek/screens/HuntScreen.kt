// Hunt screen: pick an environment, count down, race the timer to find every target, then see the results.
// Finds come from the camera (ObjectDetector); tapping a target also marks it found, for testing without one.
// Scoring, the time bonus, and XP come from the round's GameMode.
package com.example.geoseek.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.geoseek.engine.GameScoreResult
import com.example.geoseek.engine.HuntRound
import com.example.geoseek.engine.ObjectFindResult
import com.example.geoseek.models.GameObject
import com.example.geoseek.models.OBJECT_LIST
import kotlinx.coroutines.delay

private data class HuntEnvironment(val name: String, val blurb: String, val durationSeconds: Int, val targetCount: Int)

private val ENVIRONMENTS = listOf(
    HuntEnvironment("Indoors", "Rooms, desks, and shelves", 90, 3),
    HuntEnvironment("Park", "Paths, benches, and trees", 120, 4),
    HuntEnvironment("Streets", "Sidewalks and storefronts", 150, 5),
)

private enum class HuntPhase { Pick, Countdown, Active, Results }

@Composable
fun HuntScreen(
    player: Player,
    onBack: () -> Unit,
    onImmersiveChange: (Boolean) -> Unit,
    onHuntFinished: (result: GameScoreResult, found: List<GameObject>) -> Unit,
) {
    val accent = Scenes.Hunt.accent
    var phase by remember { mutableStateOf(HuntPhase.Pick) }
    var selected by remember { mutableIntStateOf(1) }
    var round by remember { mutableStateOf<HuntRound?>(null) }
    var result by remember { mutableStateOf<GameScoreResult?>(null) }
    // Compose can't observe HuntRound's plain fields, so the screen mirrors what it shows.
    val found = remember { mutableStateListOf<GameObject>() }
    var score by remember { mutableIntStateOf(0) }
    var secondsLeft by remember { mutableIntStateOf(0) }
    var bestBefore by remember { mutableIntStateOf(player.bestScore) }
    var lastFind by remember { mutableStateOf<Pair<GameObject, Int>?>(null) }

    fun startHunt() {
        val env = ENVIRONMENTS[selected]
        // TODO: Pick targets per environment once OBJECT_LIST is grouped by environment.
        val targets = OBJECT_LIST.shuffled().take(env.targetCount.coerceAtMost(OBJECT_LIST.size))
        round = HuntRound(env.name, targets, env.durationSeconds)
        found.clear()
        score = 0
        lastFind = null
        result = null
        secondsLeft = env.durationSeconds
        bestBefore = player.bestScore
        phase = HuntPhase.Countdown
    }

    fun finish() {
        val r = round ?: return
        if (phase != HuntPhase.Active) return
        val final = r.finish()
        result = final
        phase = HuntPhase.Results
        onHuntFinished(final, found.toList())
    }

    fun find(obj: GameObject) {
        val r = round ?: return
        if (phase != HuntPhase.Active) return
        val outcome = r.onObjectFound(obj)
        if (outcome.result != ObjectFindResult.ACCEPTED) return
        found += obj
        score = r.score
        lastFind = obj to outcome.pointsEarned
        if (r.isOver()) finish()
    }

    LaunchedEffect(phase) { onImmersiveChange(phase == HuntPhase.Active || phase == HuntPhase.Countdown) }
    LaunchedEffect(phase) {
        val r = round
        if (phase != HuntPhase.Active || r == null) return@LaunchedEffect
        while (phase == HuntPhase.Active) {
            delay(1000)
            r.tickTime()
            secondsLeft = r.secondsLeft
            if (r.isOver()) finish()
        }
    }
    BackHandler(enabled = phase != HuntPhase.Pick) {
        if (phase == HuntPhase.Active) finish() else phase = HuntPhase.Pick
    }

    AnimatedContent(
        targetState = phase,
        transitionSpec = {
            (fadeIn(tween(350, delayMillis = 100)) + scaleIn(tween(450), initialScale = 0.94f)) togetherWith
                (fadeOut(tween(200)) + scaleOut(tween(300), targetScale = 1.04f))
        },
        modifier = Modifier.fillMaxSize(),
        label = "huntPhase",
    ) { p ->
        Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            when (p) {
                HuntPhase.Pick -> EnvironmentPicker(accent, selected, { selected = it }, ::startHunt, onBack)
                HuntPhase.Countdown -> Countdown(onDone = { phase = HuntPhase.Active })
                HuntPhase.Active -> round?.let { r ->
                    ActiveHunt(r, found, score, secondsLeft, accent, lastFind, onFind = ::find, onEnd = ::finish)
                }
                HuntPhase.Results -> {
                    val r = round
                    val final = result
                    if (r != null && final != null) {
                        Results(
                            r, found.toList(), final, bestBefore, timedOut = secondsLeft == 0, accent = accent,
                            onAgain = { phase = HuntPhase.Pick }, onMenu = onBack,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EnvironmentPicker(accent: Color, selected: Int, onSelect: (Int) -> Unit, onStart: () -> Unit, onBack: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        ScreenHeader("Hunt", "Pick where you are. Find every target before time runs out.", accent, onBack, Icons.Hunt)
        Spacer(Modifier.height(20.dp))
        ENVIRONMENTS.forEachIndexed { i, env ->
            val isSelected = i == selected
            val border by animateColorAsState(if (isSelected) accent else GeoColors.SlotBorder, tween(200), label = "envBorder")
            val lift by animateFloatAsState(if (isSelected) 1f else 0f, spring(0.6f, Spring.StiffnessMedium), label = "envLift")
            Row(
                Modifier
                    .fillMaxWidth()
                    .entrance(i)
                    .padding(vertical = 6.dp)
                    .graphicsLayer { scaleX = 1f + lift * 0.02f; scaleY = 1f + lift * 0.02f }
                    .pixelPanel(if (isSelected) accent.copy(alpha = 0.22f).compositeOverPanel() else GeoColors.Panel, border)
                    .clickable(remember { MutableInteractionSource() }, indication = null) { onSelect(i) }
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    OutlinedText(env.name, GeoType.Title, color = if (isSelected) GeoColors.Highlight else GeoColors.Text, outlineWidth = 3.dp)
                    Text(env.blurb, style = GeoType.Body, color = GeoColors.TextSoft)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PixelSprite(Icons.Clock, Modifier.size(14.dp), tint = accent)
                        Spacer(Modifier.width(6.dp))
                        Text(formatTime(env.durationSeconds), style = GeoType.Label, color = GeoColors.Text)
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PixelSprite(Icons.Hunt, Modifier.size(14.dp), tint = accent)
                        Spacer(Modifier.width(6.dp))
                        Text("${env.targetCount.coerceAtMost(OBJECT_LIST.size)} targets", style = GeoType.Label, color = GeoColors.Text)
                    }
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        PixelButton("Start Hunt", onStart, Modifier.fillMaxWidth().entrance(ENVIRONMENTS.size), color = accent, icon = Icons.Hunt)
        Spacer(Modifier.height(24.dp))
    }
}

private fun Color.compositeOverPanel(): Color = lerpColor(GeoColors.Panel, copy(alpha = 1f), alpha).copy(alpha = GeoColors.Panel.alpha)

@Composable
private fun Countdown(onDone: () -> Unit) {
    var count by remember { mutableIntStateOf(3) }
    LaunchedEffect(Unit) {
        while (count > 0) {
            delay(800)
            count--
        }
        delay(600)
        onDone()
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        AnimatedContent(
            targetState = count,
            transitionSpec = {
                (scaleIn(spring(0.4f, Spring.StiffnessMediumLow), initialScale = 2.2f) + fadeIn(tween(120))) togetherWith
                    (scaleOut(tween(200), targetScale = 0.5f) + fadeOut(tween(160)))
            },
            label = "count",
        ) { c ->
            OutlinedText(
                if (c > 0) "$c" else "GO!",
                GeoType.Logo.copy(fontSize = 110.sp),
                color = if (c > 0) GeoColors.Text else GeoColors.Highlight,
                outlineWidth = 8.dp,
            )
        }
    }
}

@Composable
private fun ActiveHunt(
    round: HuntRound,
    found: List<GameObject>,
    score: Int,
    secondsLeft: Int,
    accent: Color,
    lastFind: Pair<GameObject, Int>?,
    onFind: (GameObject) -> Unit,
    onEnd: () -> Unit,
) {
    val shownScore by animateIntAsState(score, tween(500), label = "score")
    val urgent = secondsLeft <= 10
    val timeColor by animateColorAsState(if (urgent) GeoColors.Danger else accent, tween(300), label = "timeColor")
    val pulse = rememberInfiniteTransition(label = "pulse")
    val beat by pulse.animateFloat(1f, 1.12f, infiniteRepeatable(tween(500), RepeatMode.Reverse), label = "beat")
    val remaining = round.targets.filter { it !in found }

    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 12.dp)) {
        // HUD: timer and score.
        Row(Modifier.fillMaxWidth().pixelPanel().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            PixelSprite(Icons.Clock, Modifier.size(22.dp), tint = timeColor)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                OutlinedText(
                    formatTime(secondsLeft), GeoType.Title, color = timeColor, outlineWidth = 3.dp,
                    modifier = Modifier.graphicsLayer { if (urgent) { scaleX = beat; scaleY = beat } },
                )
                Spacer(Modifier.height(6.dp))
                PixelBar(secondsLeft / round.durationSeconds.toFloat(), timeColor, Modifier.fillMaxWidth(), segments = 24)
            }
            Spacer(Modifier.width(18.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text("SCORE", style = GeoType.Caption.copy(letterSpacing = 2.sp), color = GeoColors.TextSoft)
                OutlinedText("$shownScore", GeoType.Number, color = GeoColors.Gold)
            }
        }

        Spacer(Modifier.height(14.dp))
        CameraScan(remaining, accent, Modifier.weight(1f).fillMaxWidth(), label = round.environment, onRecognized = onFind) {
            FindPopup(lastFind)
        }
        Spacer(Modifier.height(14.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedText("Targets", GeoType.Heading, outlineWidth = 2.5.dp)
            Spacer(Modifier.width(8.dp))
            Text("${found.size} / ${round.targets.size}", style = GeoType.Label, color = GeoColors.TextSoft)
            Spacer(Modifier.weight(1f))
            Text("Tap a target to mark it found", style = GeoType.Caption, color = GeoColors.TextMuted)
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            round.targets.forEach { obj ->
                TargetSlot(obj, obj in found, Modifier.weight(1f)) { onFind(obj) }
            }
        }
        Spacer(Modifier.height(14.dp))
        PixelButton("End Hunt", onEnd, Modifier.fillMaxWidth(), color = Color(0xFF8A90B8))
    }
}

@Composable
private fun TargetSlot(obj: GameObject, isFound: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val color = rarityColor(obj.rarity)
    val pop = remember { Animatable(1f) }
    LaunchedEffect(isFound) {
        if (isFound) {
            pop.snapTo(1.25f)
            pop.animateTo(1f, spring(0.35f, Spring.StiffnessMedium))
        }
    }
    Column(
        modifier
            .graphicsLayer { scaleX = pop.value; scaleY = pop.value }
            .slot(border = if (isFound) GeoColors.Success else color.copy(alpha = 0.8f), fill = if (isFound) Color(0xC0244A2E) else GeoColors.Slot)
            .clickable(remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(1f), contentAlignment = Alignment.Center) {
            PixelSprite(ObjectSprites.forObject(obj), Modifier.fillMaxSize(0.85f), tint = color, alpha = if (isFound) 0.45f else 1f)
            if (isFound) PixelSprite(Icons.Check, Modifier.fillMaxSize(0.6f), tint = GeoColors.Success)
        }
        Spacer(Modifier.height(4.dp))
        Text(obj.name, style = GeoType.Caption.copy(fontSize = 10.sp, lineHeight = 12.sp), color = GeoColors.Text, maxLines = 2, minLines = 2, textAlign = TextAlign.Center)
        Text("${obj.points}", style = GeoType.Caption.copy(fontSize = 10.sp), color = GeoColors.Gold)
    }
}

/** A "+points" burst that floats up from the middle of the viewfinder after each find. */
@Composable
private fun FindPopup(find: Pair<GameObject, Int>?) {
    if (find == null) return
    val (obj, points) = find
    val rise = remember(find) { Animatable(0f) }
    LaunchedEffect(find) { rise.animateTo(1f, tween(1300)) }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.graphicsLayer {
                translationY = -rise.value * 80.dp.toPx()
                alpha = (1f - rise.value) * 1.4f
                val s = 0.7f + (rise.value * 4f).coerceAtMost(1f) * 0.4f
                scaleX = s
                scaleY = s
            },
        ) {
            OutlinedText("+$points", GeoType.Display, color = GeoColors.Gold, outlineWidth = 4.dp)
            OutlinedText("${obj.name} found!", GeoType.Heading, color = rarityColor(obj.rarity), outlineWidth = 2.5.dp)
        }
    }
}

@Composable
private fun Results(
    round: HuntRound,
    found: List<GameObject>,
    result: GameScoreResult,
    bestBefore: Int,
    timedOut: Boolean,
    accent: Color,
    onAgain: () -> Unit,
    onMenu: () -> Unit,
) {
    val counted = remember { Animatable(0f) }
    LaunchedEffect(Unit) { counted.animateTo(result.totalScore.toFloat(), tween(1200)) }
    val cleared = found.size == round.targets.size
    val newBest = result.totalScore > bestBefore && result.totalScore > 0

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Spacer(Modifier.height(24.dp))
        OutlinedText(
            when {
                cleared -> "Hunt Complete!"
                timedOut -> "Time's Up!"
                else -> "Hunt Over"
            },
            GeoType.Display, Modifier.entrance(0), color = if (cleared) GeoColors.Highlight else GeoColors.Text, outlineWidth = 4.dp,
        )
        Spacer(Modifier.height(20.dp))
        Panel(Modifier.widthIn(max = 460.dp).fillMaxWidth().entrance(1)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text("SCORE", style = GeoType.Caption.copy(letterSpacing = 3.sp), color = GeoColors.TextSoft)
                OutlinedText("${counted.value.toInt()}", GeoType.Logo, color = GeoColors.Gold, outlineWidth = 5.dp)
                if (result.bonusPoints > 0) {
                    Text("${result.basePoints} + ${result.bonusPoints} time bonus", style = GeoType.Label, color = GeoColors.TextSoft)
                }
                if (newBest) OutlinedText("New best!", GeoType.Heading, color = GeoColors.Highlight, outlineWidth = 2.5.dp)
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatSlot("Found", "${found.size}/${round.targets.size}", Icons.Hunt, accent, Modifier.weight(1f))
                    StatSlot("XP", "+${result.totalXpEarned}", Icons.Star, GeoColors.Success, Modifier.weight(1f))
                }
                Spacer(Modifier.height(14.dp))
                round.targets.forEach { obj ->
                    val got = obj in found
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        PixelSprite(ObjectSprites.forObject(obj), Modifier.size(26.dp), tint = rarityColor(obj.rarity), alpha = if (got) 1f else 0.35f)
                        Spacer(Modifier.width(12.dp))
                        Text(obj.name, style = GeoType.Body, color = if (got) GeoColors.Text else GeoColors.TextMuted, modifier = Modifier.weight(1f))
                        Text(if (got) "+${obj.points}" else "missed", style = GeoType.Label, color = if (got) GeoColors.Gold else GeoColors.TextMuted)
                    }
                }
                if (found.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text("Found objects were added to your collection", style = GeoType.Caption, color = GeoColors.TextMuted)
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        PixelButton("Hunt Again", onAgain, Modifier.widthIn(max = 460.dp).fillMaxWidth().entrance(2), color = accent, icon = Icons.Hunt)
        Spacer(Modifier.height(8.dp))
        MenuItem("Back to Menu", onMenu, Modifier.entrance(3), icon = Icons.Back, accent = GeoColors.TextSoft, style = GeoType.Heading)
    }
}

private fun formatTime(seconds: Int): String = "%d:%02d".format(seconds / 60, seconds % 60)
