// Free look screen: discover new objects, earn discovery & rarity XP, and collect cards.
// Only reveals the card inspector when a NEW object is found.
package com.example.geoseek.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.geoseek.data.Player
import com.example.geoseek.engine.GameSessionState
import com.example.geoseek.engine.modes.CollectionMode
import com.example.geoseek.models.Card
import com.example.geoseek.models.GameObject
import com.example.geoseek.models.OBJECT_LIST

@Composable
fun ExploreScreen(player: Player, onBack: () -> Unit, onCollect: (GameObject, Int?) -> Unit) {
    val accent = Scenes.Explore.accent
    var revealed by remember { mutableStateOf<GameObject?>(null) }
    var lastXpEarned by remember { mutableIntStateOf(0) }
    BackHandler(enabled = revealed != null) { revealed = null }

    val binderCards = remember(player.cards) {
        player.cards.mapNotNull { name ->
            OBJECT_LIST.find { it.name == name }?.let { obj ->
                Card(obj = obj, xpValue = obj.points * 2, isNewDiscovery = false)
            }
        }
    }
    val collectionMode = remember(binderCards) { CollectionMode(existingCardCollection = binderCards) }
    val sessionState = remember { GameSessionState(targetObjects = OBJECT_LIST, durationSeconds = 0) }

    fun discover(obj: GameObject) {
        val isAlreadyOwned = obj.name in player.cards
        if (isAlreadyOwned) {
            // Unless something new is found, don't show anything.
            return
        }
        val outcome = collectionMode.evaluateFoundObject(obj, sessionState)
        val xpEarned = outcome.pointsEarned
        lastXpEarned = xpEarned
        onCollect(obj, xpEarned)
        revealed = obj
    }

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            ScreenHeader("Free Look", "Collection Mode: Wander and spot new objects for your card binder.", accent, onBack, Icons.Explore)
            Spacer(Modifier.height(18.dp))
            CameraScan(
                targetPool = OBJECT_LIST.filter { it.name !in player.cards },
                accent = accent,
                modifier = Modifier.fillMaxWidth().height(260.dp).entrance(0),
                label = "Free Look",
                onRecognized = { obj ->
                    if (revealed == null) {
                        discover(obj)
                    }
                },
            )
            Spacer(Modifier.height(22.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.entrance(1)) {
                OutlinedText("Out there", GeoType.Title, outlineWidth = 3.dp)
                Spacer(Modifier.weight(1f))
                Text("Tap an uncollected item to discover it", style = GeoType.Caption, color = GeoColors.TextMuted)
            }
            Spacer(Modifier.height(10.dp))
            OBJECT_LIST.forEachIndexed { i, obj ->
                val owned = obj.name in player.cards
                DiscoverableRow(obj, owned, Modifier.entrance(2 + i, 50)) {
                    discover(obj)
                }
            }
            Spacer(Modifier.height(24.dp))
        }

        CardInspector(
            revealed, discovered = true, onClose = { revealed = null },
            headline = "New Card! +$lastXpEarned XP",
        )
    }
}

@Composable
private fun DiscoverableRow(obj: GameObject, owned: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val color = rarityColor(obj.rarity)
    Row(
        modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .pixelPanel(GeoColors.Panel, if (owned) color.copy(alpha = 0.7f) else GeoColors.SlotBorder)
            .clickable(remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(52.dp).slot(border = color.copy(alpha = 0.7f)).padding(8.dp), contentAlignment = Alignment.Center) {
            if (owned) {
                PixelSprite(ObjectSprites.forObject(obj), Modifier.fillMaxSize(), tint = color)
            } else {
                PixelSprite(Icons.Question, Modifier.fillMaxSize(0.7f), tint = GeoColors.SlotBorder)
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            OutlinedText(if (owned) obj.name else "???", GeoType.Heading, outlineWidth = 2.5.dp)
            Text(rarityLabel(obj.rarity).uppercase(), style = GeoType.Caption.copy(fontSize = 11.sp, letterSpacing = 1.5.sp), color = color)
        }
        if (owned) {
            PixelSprite(Icons.Check, Modifier.size(20.dp), tint = GeoColors.Success)
        } else {
            Text("+${obj.points} XP", style = GeoType.Label, color = GeoColors.Gold)
        }
    }
}
