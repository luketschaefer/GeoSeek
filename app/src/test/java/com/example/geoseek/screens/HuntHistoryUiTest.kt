package com.example.geoseek.screens

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.example.geoseek.data.AuthResult
import com.example.geoseek.data.AuthStore
import com.example.geoseek.data.HuntHistoryStore
import com.example.geoseek.data.HuntOutcome
import com.example.geoseek.data.Player
import com.example.geoseek.data.ProgressStore
import com.example.geoseek.engine.HuntRound
import com.example.geoseek.models.GameObject
import com.example.geoseek.models.Rarity
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h1800dp-xxhdpi")
class HuntHistoryUiTest {
    @get:Rule
    val compose = createComposeRule()
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val prefs get() = context.getSharedPreferences("geoseek_auth", Context.MODE_PRIVATE)
    private lateinit var player: Player

    private fun text(value: String) = compose.onAllNodesWithText(value).onFirst()

    @Before
    fun setUp() {
        prefs.edit().clear().commit()
        player = (AuthStore(context).signUp("Explorer", "secret", "secret") as AuthResult.Success).player
    }

    @After
    fun tearDown() {
        prefs.edit().clear().commit()
    }

    @Test
    fun profileProvidesHistoryEntryPoint() {
        var opened = false
        compose.setContent { ProfileScreen(player, onBack = {}, onSignOut = {}, onHuntHistory = { opened = true }) }
        text("Hunt History").performScrollTo().performClick()
        compose.runOnIdle { assertTrue(opened) }
    }

    @Test
    fun emptyHistoryAndStatsAreUsableForExistingAccounts() {
        compose.setContent { HuntHistoryScreen(player.copy(bestScore = 500), onBack = {}) }
        text("No hunts recorded yet").assertIsDisplayed()
        text("Stats").performClick()
        text("PERSONAL BEST").assertIsDisplayed()
        text("500").assertIsDisplayed()
        text("RECORDED HUNTS").assertIsDisplayed()
        text("0%").assertIsDisplayed()
    }

    @Test
    fun listDetailsStatsAndBackNavigationDoNotChangeProgress() {
        val chair = GameObject("Chair", Rarity.COMMON, 10)
        val dog = GameObject("Dog", Rarity.EPIC, 75)
        val round = HuntRound("Indoors", listOf(chair, dog), 90)
        round.onObjectFound(chair)
        player = ProgressStore(context).recordHunt(player, round, round.finish())
        val before = prefs.all.toMap()
        var activity: ComponentActivity? = null
        var returnedToProfile = false
        compose.setContent {
            activity = LocalActivity.current as? ComponentActivity
            BackHandler { returnedToProfile = true }
            HuntHistoryScreen(player, onBack = { returnedToProfile = true })
        }
        compose.onNodeWithTag("hunt-${round.id}").performClick()
        text("Hunt details").assertIsDisplayed()
        text("Chair").assertIsDisplayed()
        text("Dog").assertIsDisplayed()
        text("XP earned: 30").assertIsDisplayed()

        compose.runOnIdle { activity!!.onBackPressedDispatcher.onBackPressed() }
        compose.onNodeWithTag("hunt-${round.id}").assertIsDisplayed()
        assertFalse(returnedToProfile)
        compose.onNodeWithTag("hunt-${round.id}").performClick()
        text("Back to history").performScrollTo().performClick()
        text("Stats").performClick()
        text("Most played environment").assertIsDisplayed()
        text("Indoors: 1").assertIsDisplayed()
        text("History").performClick()
        compose.onNodeWithTag("hunt-${round.id}").assertIsDisplayed()
        compose.runOnIdle { activity!!.onBackPressedDispatcher.onBackPressed() }
        assertTrue(returnedToProfile)
        assertEquals(before, prefs.all)
    }

    @Test
    fun endingActualHuntScreenCreatesOneHistoryRecord() {
        var callbacks = 0
        compose.setContent {
            HuntScreen(player, onBack = {}, onImmersiveChange = {}, onHuntFinished = { round, result ->
                callbacks++
                player = ProgressStore(context).recordHunt(player, round, result)
            })
        }
        text("Indoors").performClick()
        text("Start Timed Hunt").performScrollTo().performClick()
        compose.mainClock.advanceTimeBy(4_000L)
        text("End Hunt").performClick()
        compose.runOnIdle {
            assertEquals(1, callbacks)
            val entry = HuntHistoryStore(context).load(player.username).records.single()
            assertEquals("Indoors", entry.environment)
            assertEquals(HuntOutcome.ENDED_EARLY, entry.outcome)
        }
    }

    @Test
    fun leavingCountdownDoesNotRecordAHunt() {
        var activity: ComponentActivity? = null
        var callbacks = 0
        compose.setContent {
            activity = LocalActivity.current as? ComponentActivity
            HuntScreen(player, onBack = {}, onImmersiveChange = {}, onHuntFinished = { _, _ -> callbacks++ })
        }
        text("Start Timed Hunt").performScrollTo().performClick()
        compose.runOnIdle { activity!!.onBackPressedDispatcher.onBackPressed() }
        compose.mainClock.advanceTimeBy(4_000L)
        text("Start Timed Hunt").assertIsDisplayed()
        assertEquals(0, callbacks)
        assertTrue(HuntHistoryStore(context).load(player.username).records.isEmpty())
    }
}
