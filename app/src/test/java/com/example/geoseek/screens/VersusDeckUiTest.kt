package com.example.geoseek.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import com.example.geoseek.data.CardRecord
import com.example.geoseek.data.DailyProgress
import com.example.geoseek.data.Player
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h1800dp-xxhdpi")
class VersusDeckUiTest {
    @get:Rule
    val compose = createComposeRule()

    private fun player(vararg cardNames: String) = Player(
        username = "Versus tester",
        joinedAtMillis = 0L,
        xp = 0,
        huntsPlayed = 0,
        bestScore = 0,
        binder = cardNames.associateWith { CardRecord(1, 0L) },
        daily = DailyProgress(),
        questClaimedToday = false,
    )

    private fun text(value: String) = compose.onAllNodesWithText(value).onFirst()

    @Test
    fun rarityToggleControlsOpponentGenerationMode() {
        compose.setContent {
            VersusScreen(player("Chair"), onBack = {}, onFindCards = {})
        }

        val rarityToggle = compose.onNodeWithContentDescription("Match Opponent Rarity")
        rarityToggle.assertIsOn()
        rarityToggle.performClick()
        rarityToggle.assertIsOff()

        text("Chair").performClick()
        text("Choose both cards below").assertIsNotEnabled()
        text("Chair").performClick()
        text("Start Battle").performClick()

        text("Local practice • Random-rarity rival").assertIsDisplayed()
    }

    @Test
    fun duplicateDeckCanSwapAndSwapDisablesAfterBenchKnockout() {
        compose.setContent {
            VersusScreen(player("AirPods"), onBack = {}, onFindCards = {})
        }

        text("Choose both cards below").assertIsNotEnabled()
        text("AirPods").performClick()
        text("AirPods").performClick()
        text("Start Battle").performClick()

        text("Local practice • Rarity-matched rival").assertIsDisplayed()
        text("Tech").assertIsDisplayed()
        text("YOUR BENCH").assertIsDisplayed()
        text("OPPONENT BENCH").assertIsDisplayed()
        text("Swap").performClick()
        compose.onAllNodesWithText("Swapped to AirPods!", substring = true).onFirst().assertIsDisplayed()

        repeat(3) { text("Attack").performClick() }

        text("KO").assertIsDisplayed()
        text("Swap").assertIsNotEnabled()
    }

    @Test
    fun swappingToDifferentCardUpdatesCombatSummary() {
        compose.setContent {
            VersusScreen(player("Chair", "Plant"), onBack = {}, onFindCards = {})
        }

        text("Chair").performClick()
        text("Plant").performClick()
        text("Start Battle").performClick()
        text("Swap").performClick()

        compose.onAllNodesWithText("Swapped to Plant!", substring = true).onFirst().assertIsDisplayed()
    }

    @Test
    fun emptyCollectionShowsPromptToFindCards() {
        compose.setContent {
            VersusScreen(player(), onBack = {}, onFindCards = {})
        }

        text("Find an object in Free Look to unlock a card. You can use that card in both deck slots.")
            .assertIsDisplayed()
        text("Find cards").assertIsDisplayed()
    }
}
