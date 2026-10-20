// Main menu: the rocking logo, the player's level, today's quest, and a title-screen menu into each mode.
package com.example.geoseek.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Calendar
import com.example.geoseek.data.Player
import com.example.geoseek.data.levelInfo
import com.example.geoseek.data.today
import com.example.geoseek.engine.questForDay
import com.example.geoseek.models.OBJECT_LIST



@Composable
fun HomeScreen(player: Player, onOpen: (Destination) -> Unit, onClaimQuest: (Int) -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(28.dp))
        GeoLogo(fontSize = 52f)
        OutlinedText(greeting(player.username), GeoType.Heading, Modifier.padding(top = 4.dp), color = GeoColors.TextSoft)
        Spacer(Modifier.height(20.dp))
        PlayerBadge(player, Modifier.widthIn(max = 460.dp).fillMaxWidth().entrance(0))
        Spacer(Modifier.height(36.dp))

        // Same-width rows so the icons line up in a column, like a title-screen menu.
        Column(Modifier.width(IntrinsicSize.Max), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            MenuItem("Hunt", { onOpen(Destination.Hunt) }, Modifier.fillMaxWidth().entrance(1), Icons.Hunt, Scenes.Hunt.accent, "Timed Hunt: Race the clock for targets")
            MenuItem("Free Look", { onOpen(Destination.Explore) }, Modifier.fillMaxWidth().entrance(2), Icons.Explore, Scenes.Explore.accent, "Collection Mode: Discover new cards")
            MenuItem("Collection", { onOpen(Destination.Collection) }, Modifier.fillMaxWidth().entrance(3), Icons.Cards, Scenes.Collection.accent, "${player.cards.size} / ${OBJECT_LIST.size} cards collected")
            MenuItem("Profile", { onOpen(Destination.Profile) }, Modifier.fillMaxWidth().entrance(4), Icons.Profile, Scenes.Profile.accent, "Level ${player.levelInfo.level} Explorer • ${player.xp} XP")
        }

        Spacer(Modifier.height(36.dp))
        QuestPanel(player, onClaimQuest, Modifier.widthIn(max = 460.dp).fillMaxWidth().entrance(5))
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun PlayerBadge(player: Player, modifier: Modifier = Modifier) {
    val level = player.levelInfo
    Row(modifier.pixelPanel().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
        PixelSprite(explorerSprite(player.username), Modifier.size(48.dp).slot().padding(6.dp))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedText(player.username, GeoType.Heading, Modifier.weight(1f), outlineWidth = 2.5.dp, maxLines = 1)
                OutlinedText("Lv ${level.level}", GeoType.Heading, color = GeoColors.Gold, outlineWidth = 2.5.dp)
            }
            Spacer(Modifier.height(8.dp))
            PixelBar(level.progress, GeoColors.Success, Modifier.fillMaxWidth())
            Spacer(Modifier.height(4.dp))
            Text("${level.intoLevel} / ${level.needed} XP", style = GeoType.Caption, color = GeoColors.TextSoft)
        }
    }
}

@Composable
private fun QuestPanel(player: Player, onClaim: (Int) -> Unit, modifier: Modifier = Modifier) {
    val quest = questForDay(today(), binderComplete = OBJECT_LIST.all { it.name in player.cards })
    val progress = quest.progress(player.daily)
    val done = quest.isDone(player.daily)
    Panel(modifier) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PixelSprite(Icons.Star, Modifier.size(18.dp), tint = GeoColors.Gold)
                Spacer(Modifier.width(8.dp))
                Text("DAILY QUEST", style = GeoType.Caption.copy(letterSpacing = 2.sp), color = GeoColors.Gold)
                Spacer(Modifier.weight(1f))
                Text("+${quest.rewardXp} XP", style = GeoType.Label, color = GeoColors.TextSoft)
            }
            Spacer(Modifier.height(8.dp))
            OutlinedText(quest.title, GeoType.Title, outlineWidth = 2.5.dp)
            Text(quest.detail, style = GeoType.Body, color = GeoColors.TextSoft)
            Spacer(Modifier.height(12.dp))
            PixelBar(progress / quest.goal.toFloat(), GeoColors.Gold, Modifier.fillMaxWidth(), segments = quest.goal * 4)
            Spacer(Modifier.height(6.dp))
            Text("$progress / ${quest.goal} ${quest.goalType.unit}", style = GeoType.Caption, color = GeoColors.TextSoft)
            when {
                player.questClaimedToday -> {
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PixelSprite(Icons.Check, Modifier.size(16.dp), tint = GeoColors.Success)
                        Spacer(Modifier.width(8.dp))
                        OutlinedText("Claimed. New quest tomorrow.", GeoType.Label, color = GeoColors.Success, outlineWidth = 2.dp)
                    }
                }
                done -> {
                    Spacer(Modifier.height(12.dp))
                    PixelButton("Claim ${quest.rewardXp} XP", { onClaim(quest.rewardXp) }, Modifier.fillMaxWidth(), color = GeoColors.Gold, icon = Icons.Star)
                }
            }
        }
    }
}

private fun greeting(name: String): String = when (Calendar.getInstance()[Calendar.HOUR_OF_DAY]) {
    in 5..11 -> "Good morning, $name"
    in 12..17 -> "Good afternoon, $name"
    else -> "Good evening, $name"
}
