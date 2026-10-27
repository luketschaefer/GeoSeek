package com.example.geoseek.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.geoseek.engine.DuelState
import com.example.geoseek.engine.duelStats
import com.example.geoseek.models.GameObject
import com.example.geoseek.models.OBJECT_LIST

/** Card selection and a local, turn-based practice arena using only the player's discoveries. */
@Composable
fun VersusScreen(player: Player, onBack: () -> Unit, onFindCards: () -> Unit) {
    val owned = OBJECT_LIST.filter { it.name in player.cards }
    var selectedName by rememberSaveable(player.username) { mutableStateOf<String?>(null) }
    var battling by rememberSaveable(player.username) { mutableStateOf(false) }
    val fighter = owned.firstOrNull { it.name == selectedName }
    val accent = GeoColors.Danger

    BackHandler(enabled = battling) { battling = false }
    if (battling && fighter != null) {
        DuelArena(fighter, onBack = { battling = false })
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(140.dp),
        modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 32.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            ScreenHeader("Versus", "", accent, onBack, Icons.Versus)
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            Panel(Modifier.fillMaxWidth(), border = accent) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedText("PRACTICE ARENA", GeoType.Title, color = accent)
                    Text(
                        "Choose a found card for a turn-based duel against a computer rival. Cards stay in your collection.",
                        style = GeoType.Body, color = GeoColors.TextSoft,
                    )
                    Text("Local practice • No cards or XP at stake", style = GeoType.Caption, color = GeoColors.Gold)
                }
            }
        }
        if (owned.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Panel(Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedText("Your first fighter awaits", GeoType.Heading)
                        Text("Find an object in Free Look to unlock its card, then return here to battle.",
                            style = GeoType.Body, color = GeoColors.TextSoft)
                        PixelButton("Find cards", onFindCards, Modifier.fillMaxWidth(), color = accent, icon = Icons.Explore)
                    }
                }
            }
        } else {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedText("Choose your fighter", GeoType.Heading)
                    fighter?.let { card ->
                        val stats = card.duelStats()
                        Text("${card.name} • ${stats.health} HP • ${stats.attack} attack",
                            style = GeoType.Body, color = GeoColors.TextSoft)
                    }
                    PixelButton(
                        if (fighter == null) "Select a card below" else "Enter arena",
                        { battling = true },
                        Modifier.fillMaxWidth(),
                        color = accent, enabled = fighter != null, icon = Icons.Versus,
                    )
                }
            }
            items(owned, key = { it.name }) { card ->
                val isSelected = fighter?.name == card.name
                Column(
                    Modifier.semantics { selected = isSelected },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    TradingCard(
                        card, discovered = true,
                        modifier = if (isSelected) Modifier.pixelPanel(border = accent).padding(6.dp) else Modifier.padding(6.dp),
                        onClick = { selectedName = card.name },
                    )
                    Text(if (isSelected) "SELECTED" else "Tap to select",
                        style = GeoType.Caption, color = if (isSelected) accent else GeoColors.TextSoft)
                }
            }
        }
    }
}

@Composable
private fun DuelArena(fighter: GameObject, onBack: () -> Unit) {
    // A mirrored rival keeps practice fair for every rarity, including a player's very first card.
    val stats = fighter.duelStats()
    var playerHp by rememberSaveable(fighter.name) { mutableIntStateOf(stats.health) }
    var rivalHp by rememberSaveable(fighter.name) { mutableIntStateOf(stats.health) }
    var recoveries by rememberSaveable(fighter.name) { mutableIntStateOf(2) }
    var turn by rememberSaveable(fighter.name) { mutableIntStateOf(1) }
    var message by rememberSaveable(fighter.name) { mutableStateOf("Your move! Strike first or recover health.") }
    val state = DuelState(playerHp, rivalHp, recoveries, turn)
    val accent = GeoColors.Danger

    fun play(recover: Boolean) {
        val next = state.play(stats, stats, recover)
        playerHp = next.playerHealth
        rivalHp = next.rivalHealth
        recoveries = next.recoveries
        turn = next.turn
        message = when {
            rivalHp == 0 -> "Victory! Your ${fighter.name} wins the duel."
            playerHp == 0 -> "Your card is knocked out. Try again!"
            recover -> "You recovered up to 20 HP. Rival strikes for ${stats.attack}."
            else -> "You strike for ${stats.attack}. Rival counters for ${stats.attack}."
        }
    }

    Column(
        Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ScreenHeader("Card duel", "Local practice • Mirror rival", accent, onBack, Icons.Versus)
        Row(
            Modifier.widthIn(max = 600.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BattleCard(fighter, "YOU", playerHp, stats.health, GeoColors.Success, Modifier.weight(1f))
            OutlinedText("VS", GeoType.Heading, color = accent)
            BattleCard(fighter, "COMPUTER", rivalHp, stats.health, accent, Modifier.weight(1f))
        }
        Panel(Modifier.widthIn(max = 600.dp).fillMaxWidth(), border = accent) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedText(
                    if (state.finished) (if (rivalHp == 0) "VICTORY!" else "DEFEAT") else "TURN $turn",
                    GeoType.Title, color = if (rivalHp == 0) GeoColors.Gold else accent,
                )
                Text(message, style = GeoType.Body, color = GeoColors.TextSoft)
                if (!state.finished) {
                    PixelButton("Strike • ${stats.attack} damage", { play(false) },
                        Modifier.fillMaxWidth(), color = accent)
                    PixelButton("Recover • ${recoveries} left", { play(true) },
                        Modifier.fillMaxWidth(), color = GeoColors.Success,
                        enabled = recoveries > 0 && playerHp < stats.health)
                    Text("You act first, then the rival attacks. Recovery uses your turn.",
                        style = GeoType.Caption, color = GeoColors.TextSoft)
                } else {
                    PixelButton("Rematch", {
                        playerHp = stats.health
                        rivalHp = stats.health
                        recoveries = 2
                        turn = 1
                        message = "New duel. Your move!"
                    }, Modifier.fillMaxWidth(), color = accent)
                    PixelButton("Choose another card", onBack, Modifier.fillMaxWidth(), color = GeoColors.Gold)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun BattleCard(
    card: GameObject, owner: String, health: Int, maxHealth: Int,
    accent: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(owner, style = GeoType.Caption, color = accent)
        TradingCard(card, discovered = true, modifier = Modifier.fillMaxWidth())
        PixelBar(health / maxHealth.toFloat(), accent, Modifier.fillMaxWidth())
        Text("$health / $maxHealth HP", style = GeoType.Caption, color = GeoColors.TextSoft)
    }
}
