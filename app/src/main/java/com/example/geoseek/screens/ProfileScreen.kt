// Profile screen: the player's explorer, level, stats, and signing out.
package com.example.geoseek.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.geoseek.models.OBJECT_LIST
import java.text.DateFormat
import java.util.Date

@Composable
fun ProfileScreen(player: Player, onBack: () -> Unit, onSignOut: () -> Unit) {
    val accent = Scenes.Profile.accent
    val level = player.levelInfo
    var confirming by remember { mutableStateOf(false) }
    BackHandler(enabled = confirming) { confirming = false }
    val bob = rememberInfiniteTransition(label = "bob")
    val hop by bob.animateFloat(0f, 1f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "hop")

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            ScreenHeader("Profile", "Your explorer and how far they've come.", accent, onBack, Icons.Profile)
            Spacer(Modifier.height(18.dp))

            Panel(Modifier.fillMaxWidth().entrance(0)) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(120.dp).slot(border = accent).padding(14.dp), contentAlignment = Alignment.Center) {
                        PixelSprite(
                            explorerSprite(player.username),
                            Modifier.fillMaxSize().graphicsLayer { translationY = -hop * 4.dp.toPx() },
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    OutlinedText(player.username, GeoType.Display, outlineWidth = 4.dp)
                    if (player.joinedAtMillis > 0) {
                        Text(
                            "Exploring since ${DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(player.joinedAtMillis))}",
                            style = GeoType.Caption, color = GeoColors.TextSoft,
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedText("Level ${level.level}", GeoType.Heading, color = GeoColors.Gold, outlineWidth = 2.5.dp)
                        Spacer(Modifier.weight(1f))
                        Text("${level.intoLevel} / ${level.needed} XP", style = GeoType.Caption, color = GeoColors.TextSoft)
                    }
                    Spacer(Modifier.height(8.dp))
                    PixelBar(level.progress, GeoColors.Success, Modifier.fillMaxWidth())
                }
            }

            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth().entrance(1), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatSlot("Total XP", "${player.xp}", Icons.Star, GeoColors.Success, Modifier.weight(1f))
                StatSlot("Cards", "${player.cards.size}/${OBJECT_LIST.size}", Icons.Cards, Scenes.Collection.accent, Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth().entrance(2), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatSlot("Hunts", "${player.huntsPlayed}", Icons.Hunt, Scenes.Hunt.accent, Modifier.weight(1f))
                StatSlot("Best score", "${player.bestScore}", Icons.Trophy, GeoColors.Gold, Modifier.weight(1f))
            }

            Spacer(Modifier.height(28.dp))
            PixelButton("Sign Out", { confirming = true }, Modifier.fillMaxWidth().entrance(3), color = GeoColors.Danger, icon = Icons.Door)
            Spacer(Modifier.height(24.dp))
        }

        AnimatedVisibility(confirming, enter = fadeIn(tween(180)), exit = fadeOut(tween(180))) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color(0xB303051A))
                    .clickable(remember { MutableInteractionSource() }, indication = null) { confirming = false }
                    .padding(28.dp),
                contentAlignment = Alignment.Center,
            ) {
                Panel(
                    Modifier
                        .widthIn(max = 400.dp)
                        .fillMaxWidth()
                        .clickable(remember { MutableInteractionSource() }, indication = null) {},
                    fill = Color(0xFA111644),
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        OutlinedText("Sign out?", GeoType.Title, outlineWidth = 3.dp)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Your progress stays on this device. Sign back in any time.",
                            style = GeoType.Body, color = GeoColors.TextSoft, textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(18.dp))
                        PixelButton("Sign Out", onSignOut, Modifier.fillMaxWidth(), color = GeoColors.Danger)
                        Spacer(Modifier.height(6.dp))
                        MenuItem("Stay", { confirming = false }, style = GeoType.Heading)
                    }
                }
            }
        }
    }
}
