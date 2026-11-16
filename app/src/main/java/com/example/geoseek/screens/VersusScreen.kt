package com.example.geoseek.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.geoseek.data.Player
import com.example.geoseek.engine.DuelCard
import com.example.geoseek.engine.DuelOutcome
import com.example.geoseek.engine.DuelState
import com.example.geoseek.engine.duelStats
import com.example.geoseek.engine.generateOpponentDeck
import com.example.geoseek.engine.toDuelCard
import com.example.geoseek.models.Card
import com.example.geoseek.models.CardType
import com.example.geoseek.models.GameObject
import com.example.geoseek.models.OBJECT_LIST

/** Card selection and a local, turn-based practice arena using only the player's discoveries. */
@Composable
fun VersusScreen(player: Player, onBack: () -> Unit, onFindCards: () -> Unit) {
    val owned = OBJECT_LIST.filter { it.name in player.cards }
    var selectedNames by rememberSaveable(player.username) { mutableStateOf(listOf("", "")) }
    var selectedSlot by rememberSaveable(player.username) { mutableIntStateOf(0) }
    var battling by rememberSaveable(player.username) { mutableStateOf(false) }
    var matchRarity by rememberSaveable(player.username) { mutableStateOf(true) }
    val selectedDeck = selectedNames.map { name -> owned.firstOrNull { it.name == name } }
    val deckReady = selectedDeck.all { it != null }
    val accent = GeoColors.Danger

    BackHandler(enabled = battling) { battling = false }
    if (battling && deckReady) {
        DuelArena(selectedDeck.filterNotNull(), matchRarity, onBack = { battling = false })
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
                        "Choose two cards for a turn-based duel against a computer rival. Cards stay in your collection.",
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
                        Text("Find an object in Free Look to unlock a card. You can use that card in both deck slots.",
                            style = GeoType.Body, color = GeoColors.TextSoft)
                        PixelButton("Find cards", onFindCards, Modifier.fillMaxWidth(), color = accent, icon = Icons.Explore)
                    }
                }
            }
        } else {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedText("Choose your deck", GeoType.Heading)
                    selectedNames.forEachIndexed { index, name ->
                        val selected = owned.firstOrNull { it.name == name }
                        PixelButton(
                            "Card ${index + 1}: ${name.ifEmpty { "Choose a card" }}",
                            { selectedSlot = index },
                            Modifier.fillMaxWidth(),
                            color = if (selectedSlot == index) accent else GeoColors.Gold,
                        )
                        if (selected != null) {
                            val stats = selected.duelStats()
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                CardTypeBadge(selected.cardType)
                                Text(
                                    "${rarityLabel(selected.rarity)} • ${stats.health} HP • ${stats.attack} ATK",
                                    style = GeoType.Caption,
                                    color = rarityColor(selected.rarity),
                                )
                            }
                        } else {
                            Text("Choose a card below for this slot.", style = GeoType.Caption, color = GeoColors.TextMuted)
                        }
                    }
                    Text("Tap a card below to fill the selected slot. Duplicate cards are allowed.",
                        style = GeoType.Caption, color = GeoColors.TextSoft)
                    TypeMatchupLegend()
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Match Opponent Rarity", style = GeoType.Label, color = GeoColors.TextSoft)
                        Switch(
                            checked = matchRarity,
                            onCheckedChange = { matchRarity = it },
                            modifier = Modifier.semantics { contentDescription = "Match Opponent Rarity" },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = GeoColors.Highlight,
                                checkedTrackColor = GeoColors.Gold.copy(alpha = 0.5f),
                                checkedBorderColor = GeoColors.Gold,
                                uncheckedThumbColor = GeoColors.TextMuted,
                                uncheckedTrackColor = GeoColors.PanelDeep,
                                uncheckedBorderColor = GeoColors.SlotBorder,
                            ),
                        )
                    }
                    PixelButton(
                        if (deckReady) "Start Battle" else "Choose both cards below",
                        { battling = true },
                        Modifier.fillMaxWidth(),
                        color = accent, enabled = deckReady, icon = Icons.Versus,
                    )
                }
            }
            items(owned, key = { it.name }) { card ->
                val selectionCount = selectedNames.count { it == card.name }
                val stats = card.duelStats()
                Column(
                    Modifier.semantics { selected = selectionCount > 0 },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    TradingCard(
                        card, discovered = true,
                        modifier = if (selectionCount > 0) Modifier.pixelPanel(border = accent).padding(6.dp) else Modifier.padding(6.dp),
                        onClick = {
                            selectedNames = selectedNames.toMutableList().also { it[selectedSlot] = card.name }
                            if (selectedSlot == 0 && selectedNames[1].isEmpty()) selectedSlot = 1
                        },
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CardTypeBadge(card.cardType, compact = true)
                        Text(rarityLabel(card.rarity), style = GeoType.Caption.copy(fontSize = 10.sp), color = rarityColor(card.rarity))
                    }
                    Text("${stats.health} HP • ${stats.attack} ATK", style = GeoType.Caption, color = GeoColors.TextSoft)
                    Text(
                        if (selectionCount > 1) "SELECTED ×$selectionCount" else if (selectionCount == 1) "SELECTED" else "Tap to select",
                        style = GeoType.Caption,
                        color = if (selectionCount > 0) accent else GeoColors.TextSoft,
                    )
                }
            }
        }
    }
}

@Composable
private fun DuelArena(deck: List<GameObject>, matchRarity: Boolean, onBack: () -> Unit) {
    val availableCards = remember { OBJECT_LIST.map { Card(obj = it, xpValue = 0) } }
    val playerCards = remember(deck) { deck.map { Card(obj = it, xpValue = 0) } }
    val opponentCards = remember(deck, matchRarity) {
        generateOpponentDeck(playerCards, availableCards, matchRarity = matchRarity)
    }
    val playerBattleDeck = remember(deck) { playerCards.map { it.obj.duelStats().toDuelCard() } }
    val opponentBattleDeck = remember(deck) { opponentCards.map { it.obj.duelStats().toDuelCard() } }
    val initialState = remember(deck) {
        DuelState(playerDeck = playerBattleDeck, rivalDeck = opponentBattleDeck)
    }
    var state by remember(deck) { mutableStateOf(initialState) }
    val accent = GeoColors.Danger

    fun play(recover: Boolean) {
        state = state.play(recover)
    }

    Column(
        Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ScreenHeader(
            "Card duel",
            if (matchRarity) "Local practice • Rarity-matched rival" else "Local practice • Random-rarity rival",
            accent, onBack, Icons.Versus,
        )
        Row(
            Modifier.widthIn(max = 600.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val playerCard = deck[state.playerActiveIndex]
            val rivalCard = opponentCards[state.rivalActiveIndex].obj
            val multiplier = state.playerActiveCard.cardType.getDamageMultiplier(state.rivalActiveCard.cardType)
            val playerTypeHighlight = when {
                multiplier > 1.0 -> GeoColors.Success
                multiplier < 1.0 -> GeoColors.Danger
                else -> null
            }
            BattleCard(
                playerCard, "YOU", state.playerActiveCard.currentHp, state.playerActiveCard.maxHp,
                GeoColors.Success, Modifier.weight(1f), playerTypeHighlight,
            )
            OutlinedText("VS", GeoType.Heading, color = accent)
            BattleCard(
                rivalCard, "COMPUTER", state.rivalActiveCard.currentHp, state.rivalActiveCard.maxHp,
                accent, Modifier.weight(1f), null,
            )
        }
        TypeMatchupStatus(state.playerActiveCard.cardType, state.rivalActiveCard.cardType)
        Row(
            Modifier.widthIn(max = 600.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            val playerBenchIndex = state.playerDeck.indices.firstOrNull { it != state.playerActiveIndex }
            val rivalBenchIndex = state.rivalDeck.indices.firstOrNull { it != state.rivalActiveIndex }
            BenchStatus(
                title = "YOUR BENCH",
                card = playerBenchIndex?.let(state.playerDeck::get),
                modifier = Modifier.weight(1f),
            )
            BenchStatus(
                title = "OPPONENT BENCH",
                card = rivalBenchIndex?.let(state.rivalDeck::get),
                modifier = Modifier.weight(1f),
            )
        }
        Panel(Modifier.widthIn(max = 600.dp).fillMaxWidth(), border = accent) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedText(
                    if (state.finished) {
                        if (state.outcome == DuelOutcome.VICTORY) "VICTORY!" else "DEFEAT"
                    } else {
                        "TURN ${state.turn}"
                    },
                    GeoType.Title, color = if (state.outcome == DuelOutcome.VICTORY) GeoColors.Gold else accent,
                )
                Text(state.statusMessage.ifEmpty { "Your move! Strike, recover, or switch cards." },
                    style = GeoType.Body, color = GeoColors.TextSoft)
                if (!state.finished) {
                    val playerBenchIndex = state.playerDeck.indices.firstOrNull { it != state.playerActiveIndex }
                    val canSwap = playerBenchIndex?.let { state.playerDeck[it].currentHp > 0 } == true
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        PixelButton("Attack", { play(false) }, Modifier.weight(1f), color = accent)
                        PixelButton(
                            "Recover",
                            { play(true) },
                            Modifier.weight(1f),
                            color = GeoColors.Success,
                            enabled = state.recoveries > 0 && state.playerActiveCard.currentHp < state.playerActiveCard.maxHp,
                        )
                        PixelButton(
                            "Swap",
                            { playerBenchIndex?.let { state = state.swapCard(it) } },
                            Modifier.weight(1f),
                            color = GeoColors.Gold,
                            enabled = canSwap,
                        )
                    }
                    Text("You act first, then the rival attacks. Recovery and switching use your turn.",
                        style = GeoType.Caption, color = GeoColors.TextSoft)
                } else {
                    PixelButton("Rematch", {
                        state = initialState.copy(statusMessage = "New duel. Your move!")
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
    accent: Color, modifier: Modifier = Modifier, typeHighlight: Color? = null,
) {
    val stats = card.duelStats()
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(owner, style = GeoType.Caption, color = accent)
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            CardTypeBadge(card.cardType, compact = true, highlight = typeHighlight)
            Text(rarityLabel(card.rarity), style = GeoType.Caption.copy(fontSize = 10.sp), color = rarityColor(card.rarity))
        }
        TradingCard(card, discovered = true, modifier = Modifier.fillMaxWidth())
        PixelBar(health / maxHealth.toFloat(), accent, Modifier.fillMaxWidth())
        Text("$health/$maxHealth HP • ${stats.attack} ATK", style = GeoType.Caption, color = GeoColors.TextSoft)
    }
}

@Composable
private fun CardTypeBadge(
    type: CardType,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    highlight: Color? = null,
) {
    val color = highlight ?: cardTypeColor(type)
    Box(
        modifier
            .pixelPanel(fill = color.copy(alpha = 0.18f), border = color, step = 3.dp)
            .padding(horizontal = if (compact) 5.dp else 8.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            type.displayLabel,
            style = GeoType.Caption.copy(fontSize = if (compact) 9.sp else 11.sp),
            color = color,
            maxLines = 1,
        )
    }
}

private fun cardTypeColor(type: CardType): Color = when (type) {
    CardType.TECH -> Color(0xFF5AA2FF)
    CardType.NATURE -> GeoColors.Success
    CardType.HOUSEHOLD -> GeoColors.Gold
    CardType.MISC -> Color(0xFFC86BFF)
}

@Composable
private fun TypeMatchupLegend() {
    val cycle = generateSequence(CardType.TECH) { it.strongAgainst }
        .take(CardType.entries.size)
        .joinToString(" > ") { it.displayLabel }
    Column(
        Modifier.fillMaxWidth()
            .pixelPanel(fill = GeoColors.PanelDeep, border = GeoColors.PanelBorder, step = 3.dp)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text("TYPE MATCHUPS", style = GeoType.Caption, color = GeoColors.Highlight)
        Text(
            "Strengths: $cycle > ${CardType.TECH.displayLabel}. Weaknesses reverse the cycle.",
            style = GeoType.Caption,
            color = GeoColors.TextSoft,
        )
    }
}

@Composable
private fun TypeMatchupStatus(playerType: CardType, opponentType: CardType) {
    val multiplier = playerType.getDamageMultiplier(opponentType)
    val (message, color) = when {
        multiplier > 1.0 -> "TYPE ADVANTAGE • 1.5× DAMAGE" to GeoColors.Success
        multiplier < 1.0 -> "TYPE DISADVANTAGE • 0.75× DAMAGE" to GeoColors.Danger
        else -> "NEUTRAL MATCHUP • 1× DAMAGE" to GeoColors.TextSoft
    }
    Text(message, style = GeoType.Label, color = color)
}

@Composable
private fun BenchStatus(title: String, card: DuelCard?, modifier: Modifier = Modifier) {
    Column(
        modifier.pixelPanel(fill = GeoColors.PanelDeep, border = GeoColors.SlotBorder, step = 3.dp)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Text(title, style = GeoType.Caption, color = GeoColors.TextMuted)
        if (card == null) {
            Text("No bench card", style = GeoType.Caption, color = GeoColors.TextSoft)
        } else {
            Text(card.name, style = GeoType.Label, color = GeoColors.Text, maxLines = 1)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                CardTypeBadge(card.cardType, compact = true)
                Text(
                    if (card.isKnockedOut) "KO" else "${card.currentHp}/${card.maxHp} HP",
                    style = GeoType.Caption,
                    color = if (card.isKnockedOut) GeoColors.Danger else GeoColors.TextSoft,
                )
            }
        }
    }
}
