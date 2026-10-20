package com.example.geoseek.screens

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.example.geoseek.data.AuthStore
import com.example.geoseek.data.CardRecord
import com.example.geoseek.data.DailyProgress
import com.example.geoseek.data.Player
import com.example.geoseek.data.today
import com.example.geoseek.engine.questForDay
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Drives real screens on the JVM: what the player sees and what their taps do. */
@RunWith(RobolectricTestRunner::class)
// A tall phone, so every screen fits without scrolling.
@Config(sdk = [34], qualifiers = "w411dp-h1800dp-xxhdpi")
class ScreensUiTest {
    @get:Rule
    val compose = createComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun prefs() = context.getSharedPreferences("geoseek_auth", Context.MODE_PRIVATE)

    @Before
    fun clearAccounts() {
        prefs().edit().clear().commit()
    }

    @After
    fun tearDown() {
        prefs().edit().clear().commit()
    }

    private fun player(
        binder: Map<String, CardRecord> = emptyMap(),
        daily: DailyProgress = DailyProgress(),
        questClaimedToday: Boolean = false,
    ) = Player("Tester", 0L, 0, 0, 0, binder, daily, questClaimedToday)

    private fun text(value: String) = compose.onAllNodesWithText(value).onFirst()

    private fun fillSignUp(name: String, password: String, confirm: String) {
        text("Create Explorer").performClick()
        compose.onNodeWithContentDescription("Explorer name").performTextInput(name)
        compose.onNodeWithContentDescription("Password").performTextInput(password)
        compose.onNodeWithContentDescription("Confirm password").performTextInput(confirm)
        text("Start Exploring").performClick()
    }

    @Test
    fun signUp_shortPasswordShowsTheErrorAndStaysOnTheForm() {
        var signedIn: Player? = null
        compose.setContent { AuthScreen(AuthStore(context), onAuthenticated = { signedIn = it }) }

        fillSignUp("Explorer", "short", "short")

        text("Password needs at least 6 characters").assertIsDisplayed()
        assertNull(signedIn)
    }

    @Test
    fun signUp_validDetailsSignTheExplorerIn() {
        var signedIn: Player? = null
        compose.setContent { AuthScreen(AuthStore(context), onAuthenticated = { signedIn = it }) }

        fillSignUp("Explorer", "secret1", "secret1")

        compose.runOnIdle { assertEquals("Explorer", signedIn?.username) }
    }

    @Test
    fun home_finishedQuestCanBeClaimedForItsReward() {
        val quest = questForDay(today(), binderComplete = false)
        var claimed: Int? = null
        compose.setContent {
            HomeScreen(player(daily = DailyProgress(10, 10, 10, 10)), onOpen = {}, onClaimQuest = { claimed = it })
        }

        text(quest.title).assertIsDisplayed()
        text("Claim ${quest.rewardXp} XP").performClick()

        compose.runOnIdle { assertEquals(quest.rewardXp, claimed) }
    }

    @Test
    fun home_claimedQuestSaysToComeBackTomorrow() {
        compose.setContent {
            HomeScreen(player(daily = DailyProgress(10, 10, 10, 10), questClaimedToday = true), onOpen = {}, onClaimQuest = {})
        }

        text("Claimed. New quest tomorrow.").assertIsDisplayed()
    }

    @Test
    fun collection_showsDuplicateCountsAndCompletion() {
        val binder = mapOf("Chair" to CardRecord(3, 0L), "Dog" to CardRecord(1, 0L))
        compose.setContent { CollectionScreen(player(binder = binder), onBack = {}) }

        text("x3").assertIsDisplayed()
        assertEquals(0, compose.onAllNodesWithText("x1").fetchSemanticsNodes().size)
    }
}
