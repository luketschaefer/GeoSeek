package com.example.geoseek.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.geoseek.data.HuntHistoryStore
import com.example.geoseek.data.HuntRecord
import com.example.geoseek.data.HuntStats
import com.example.geoseek.data.Player
import java.text.DateFormat
import java.util.Date

@Composable
fun HuntHistoryScreen(player: Player, onBack: () -> Unit) {
    val context = LocalContext.current
    val history = remember(context, player.username, player.huntsPlayed) {
        HuntHistoryStore(context).load(player.username)
    }
    var selectedId by rememberSaveable(player.username) { mutableStateOf<String?>(null) }
    var showStats by rememberSaveable(player.username) { mutableStateOf(false) }
    val selectedRecord = history.records.firstOrNull { it.id == selectedId }
    val accent = Scenes.Profile.accent

    if (selectedRecord != null) {
        BackHandler { selectedId = null }
        HuntDetails(selectedRecord, onBack = { selectedId = null })
        return
    }

    LazyColumn(
        Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ScreenHeader("Hunt History", "Your adventures, saved on this device.", accent, onBack, Icons.Hunt)
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PixelButton("History", { showStats = false },
                    Modifier.weight(1f).semantics { selected = !showStats }, color = if (!showStats) accent else GeoColors.TextMuted)
                PixelButton("Stats", { showStats = true },
                    Modifier.weight(1f).semantics { selected = showStats }, color = if (showStats) accent else GeoColors.TextMuted)
            }
        }
        if (showStats) {
            item { HuntStatistics(history.stats, player.bestScore) }
        } else {
            if (history.records.isEmpty()) {
                item {
                    Panel(Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedText("No hunts recorded yet", GeoType.Heading)
                            Text("Finish a Timed Hunt to see its results here.", style = GeoType.Body, color = GeoColors.TextSoft)
                        }
                    }
                }
            }
            items(history.records, key = { it.id }) { record ->
                Panel(Modifier.fillMaxWidth().testTag("hunt-${record.id}").clickable { selectedId = record.id }) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedText(record.environment, GeoType.Heading)
                        Text(formatHuntDate(record.finishedAtMillis), style = GeoType.Caption, color = GeoColors.TextSoft)
                        Text(record.outcome.label, style = GeoType.Label, color = accent)
                        Text("${record.score} points • ${record.xpEarned} XP • ${record.found.size}/${record.targets.size} found",
                            style = GeoType.Body, color = GeoColors.Gold)
                        Text("View results", style = GeoType.Caption, color = GeoColors.TextMuted)
                    }
                }
            }
        }
    }
}

@Composable
private fun HuntDetails(record: HuntRecord, onBack: () -> Unit) {
    val accent = Scenes.Profile.accent
    Column(
        Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ScreenHeader("Hunt details", record.environment, accent, onBack, Icons.Hunt)
        Panel(Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedText(record.outcome.label, GeoType.Heading, color = accent)
                Text(formatHuntDate(record.finishedAtMillis), style = GeoType.Body, color = GeoColors.TextSoft)
                Text("Score: ${record.score}", style = GeoType.Label, color = GeoColors.Gold)
                Text("XP earned: ${record.xpEarned}", style = GeoType.Label, color = GeoColors.Success)
            }
        }
        ObjectResults("Found objects", record.found, "No objects found")
        ObjectResults("Missed targets", record.missed, "No targets missed")
        PixelButton("Back to history", onBack, Modifier.fillMaxWidth(), color = accent, icon = Icons.Back)
    }
}

@Composable
private fun ObjectResults(title: String, objects: List<String>, emptyMessage: String) {
    Panel(Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedText(title, GeoType.Heading)
            if (objects.isEmpty()) Text(emptyMessage, style = GeoType.Body, color = GeoColors.TextSoft)
            objects.forEach { Text(it, style = GeoType.Body, color = GeoColors.TextSoft) }
        }
    }
}

@Composable
private fun HuntStatistics(stats: HuntStats, allTimeBest: Int) {
    val accent = Scenes.Profile.accent
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatSlot("Personal best", maxOf(allTimeBest, stats.personalBest).toString(), Icons.Trophy, GeoColors.Gold, Modifier.weight(1f))
            StatSlot("Recorded hunts", stats.recordedHunts.toString(), Icons.Hunt, accent, Modifier.weight(1f))
        }
        Panel(Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedText("Most played environment", GeoType.Heading)
                Text(stats.mostPlayed.joinToString(", ").ifEmpty { "No hunts recorded yet" },
                    style = GeoType.Body, color = GeoColors.Gold)
                if (stats.mostPlayed.size > 1) Text("Tied for most played", style = GeoType.Caption, color = GeoColors.TextSoft)
                stats.environmentCounts.toList().sortedWith(compareByDescending<Pair<String, Int>> { it.second }.thenBy { it.first })
                    .forEach { (environment, count) ->
                        Text("$environment: $count", style = GeoType.Body, color = GeoColors.TextSoft)
                    }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatSlot("Completed", stats.completed.toString(), Icons.Check, GeoColors.Success, Modifier.weight(1f))
            StatSlot("Completion rate", "${stats.completionPercent}%", Icons.Star, accent, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatSlot("Timed out", stats.timedOut.toString(), Icons.Clock, accent, Modifier.weight(1f))
            StatSlot("Ended early", stats.endedEarly.toString(), Icons.Back, accent, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatSlot("Average score", stats.averageScore.toString(), Icons.Trophy, GeoColors.Gold, Modifier.weight(1f))
            StatSlot("Hunt XP earned", stats.totalXp.toString(), Icons.Star, GeoColors.Success, Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
    }
}

private fun formatHuntDate(millis: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(millis))
