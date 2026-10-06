// Collection screen: every card in the catalog, found ones face up, with a rarity filter and a card inspector.
package com.example.geoseek.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.geoseek.models.GameObject
import com.example.geoseek.models.OBJECT_LIST
import com.example.geoseek.models.Rarity

@Composable
fun CollectionScreen(player: Player, onBack: () -> Unit) {
    val accent = Scenes.Collection.accent
    var filter by remember { mutableStateOf<Rarity?>(null) }
    var inspecting by remember { mutableStateOf<GameObject?>(null) }
    BackHandler(enabled = inspecting != null) { inspecting = null }

    val cards = OBJECT_LIST
        .sortedWith(compareByDescending<GameObject> { it.rarity }.thenBy { it.name })
        .filter { filter == null || it.rarity == filter }
    val found = OBJECT_LIST.count { it.name in player.cards }

    Box(Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(104.dp),
            modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                ScreenHeader("Collection", "Every object you find becomes a card.", accent, onBack, Icons.Cards)
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                Panel(Modifier.fillMaxWidth().entrance(0)) {
                    Column {
                        Row(verticalAlignment = Alignment.Bottom) {
                            OutlinedText("$found", GeoType.Number, color = accent)
                            OutlinedText(" / ${OBJECT_LIST.size} cards", GeoType.Heading, Modifier.padding(bottom = 4.dp), color = GeoColors.TextSoft, outlineWidth = 2.dp)
                        }
                        Spacer(Modifier.height(10.dp))
                        PixelBar(found / OBJECT_LIST.size.coerceAtLeast(1).toFloat(), accent, Modifier.fillMaxWidth())
                    }
                }
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                RarityFilter(filter, Modifier.entrance(1)) { filter = it }
            }
            items(cards, key = { it.name }) { obj ->
                TradingCard(
                    obj,
                    discovered = obj.name in player.cards,
                    modifier = Modifier.animateItem().entrance(2 + cards.indexOf(obj).coerceAtMost(12), 40),
                    onClick = { inspecting = obj },
                )
            }
        }

        CardInspector(inspecting, inspecting?.name in player.cards, onClose = { inspecting = null })
    }
}

@Composable
private fun RarityFilter(selected: Rarity?, modifier: Modifier = Modifier, onSelect: (Rarity?) -> Unit) {
    // All six chips share the row's width, weighted by label length, so nothing scrolls off screen.
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        (listOf<Rarity?>(null) + Rarity.entries.reversed()).forEach { r ->
            val isSelected = r == selected
            val color = r?.let(::rarityColor) ?: GeoColors.Text
            val label = r?.let(::rarityLabel) ?: "All"
            Box(
                Modifier
                    .weight(label.length + 2f)
                    .pixelPanel(if (isSelected) color.copy(alpha = 0.35f) else GeoColors.Slot, if (isSelected) color else GeoColors.SlotBorder, 3.dp)
                    .clickable(remember { MutableInteractionSource() }, indication = null) { onSelect(r) }
                    .padding(horizontal = 2.dp, vertical = 9.dp),
                contentAlignment = Alignment.Center,
            ) {
                OutlinedText(label, GeoType.Label.copy(fontSize = 11.sp, letterSpacing = 0.sp), color = if (isSelected) GeoColors.Highlight else color, outlineWidth = 2.dp, maxLines = 1)
            }
        }
    }
}

/** A trading card. Undiscovered cards show their back with a question mark. */
@Composable
fun TradingCard(obj: GameObject, discovered: Boolean, modifier: Modifier = Modifier, large: Boolean = false, onClick: (() -> Unit)? = null) {
    val color = rarityColor(obj.rarity)
    val shimmer = rememberInfiniteTransition(label = "shimmer")
    val sweep by shimmer.animateFloat(-0.6f, 1.6f, infiniteRepeatable(tween(2600, delayMillis = 900, easing = FastOutSlowInEasing), RepeatMode.Restart), label = "sweep")
    val shine = discovered && obj.rarity >= Rarity.RARE

    Box(
        modifier
            .aspectRatio(0.7f)
            .pixelPanel(
                fill = if (discovered) Color(0xF0141A42) else Color(0xE00C0F2A),
                border = if (discovered) color else GeoColors.SlotBorder.copy(alpha = 0.6f),
                step = if (large) 6.dp else 4.dp,
            )
            .let { if (onClick != null) it.clickable(remember { MutableInteractionSource() }, indication = null, onClick = onClick) else it }
            .padding(if (large) 18.dp else 9.dp),
    ) {
        if (discovered) {
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    rarityLabel(obj.rarity).uppercase(),
                    style = GeoType.Caption.copy(fontSize = if (large) 13.sp else 9.sp, letterSpacing = 1.5.sp),
                    color = color,
                )
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Canvas(Modifier.fillMaxSize()) {
                        drawCircle(
                            Brush.radialGradient(listOf(color.copy(alpha = 0.35f), Color.Transparent)),
                            radius = size.minDimension * 0.55f,
                        )
                    }
                    PixelSprite(ObjectSprites.forObject(obj), Modifier.fillMaxSize(0.78f), tint = color)
                }
                OutlinedText(
                    obj.name, if (large) GeoType.Title else GeoType.Label.copy(fontSize = 12.sp),
                    outlineWidth = if (large) 3.dp else 2.dp, textAlign = TextAlign.Center, maxLines = 1,
                )
                Text("${obj.points} pts", style = GeoType.Caption.copy(fontSize = if (large) 13.sp else 10.sp), color = GeoColors.Gold)
            }
            if (shine) {
                Canvas(Modifier.fillMaxSize()) {
                    val x = size.width * sweep
                    drawRect(
                        Brush.linearGradient(
                            listOf(Color.Transparent, Color.White.copy(alpha = 0.18f), Color.Transparent),
                            start = Offset(x - size.width * 0.3f, 0f),
                            end = Offset(x + size.width * 0.3f, size.height),
                        ),
                    )
                }
            }
        } else {
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                PixelSprite(Icons.Question, Modifier.fillMaxWidth(0.4f).aspectRatio(1f), tint = GeoColors.SlotBorder)
                Spacer(Modifier.height(8.dp))
                Text(
                    rarityLabel(obj.rarity).uppercase(),
                    style = GeoType.Caption.copy(fontSize = if (large) 13.sp else 9.sp, letterSpacing = 1.5.sp),
                    color = color.copy(alpha = 0.7f),
                )
            }
        }
    }
}

/** Full-screen look at one card. It flips in, and tapping anywhere closes it. */
@Composable
fun CardInspector(obj: GameObject?, discovered: Boolean, onClose: () -> Unit, headline: String? = null) {
    // Remember the last card so it stays drawn while the overlay fades out.
    var shown by remember { mutableStateOf(obj) }
    if (obj != null) shown = obj

    AnimatedVisibility(obj != null, enter = fadeIn(tween(200)), exit = fadeOut(tween(220))) {
        val card = shown ?: return@AnimatedVisibility
        val flip = remember(card) { Animatable(90f) }
        LaunchedEffect(card) { flip.animateTo(0f, spring(dampingRatio = 0.55f, stiffness = 180f)) }

        Box(
            Modifier
                .fillMaxSize()
                .background(Color(0xCC03051A))
                .clickable(remember { MutableInteractionSource() }, indication = null, onClick = onClose)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(28.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (headline != null) {
                    OutlinedText(headline, GeoType.Display, color = GeoColors.Highlight, outlineWidth = 4.dp)
                    Spacer(Modifier.height(18.dp))
                }
                TradingCard(
                    card, discovered,
                    Modifier
                        .widthIn(max = 280.dp)
                        .fillMaxWidth(0.72f)
                        .graphicsLayer {
                            rotationY = flip.value
                            cameraDistance = 14f * density
                        },
                    large = true,
                )
                Spacer(Modifier.height(20.dp))
                OutlinedText(
                    if (discovered) "Worth ${card.points} XP the first time it's found" else "Not found yet. Look for one in Free Look or a Hunt.",
                    GeoType.Body, color = GeoColors.TextSoft, outlineWidth = 2.dp, textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(10.dp))
                Text("Tap anywhere to close", style = GeoType.Caption, color = GeoColors.TextMuted)
            }
        }
    }
}
