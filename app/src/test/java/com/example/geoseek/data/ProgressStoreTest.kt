package com.example.geoseek.data

import android.app.Application
import android.content.Context
import com.example.geoseek.models.GameObject
import com.example.geoseek.models.Rarity
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class ProgressStoreTest {
    private lateinit var application: Application
    private lateinit var progress: ProgressStore
    private lateinit var player: Player

    private val chair = GameObject("Chair", Rarity.COMMON, 10)
    private val dog = GameObject("Dog", Rarity.EPIC, 75)

    @Before
    fun setUp() {
        application = RuntimeEnvironment.getApplication()
        prefs().edit().clear().commit()
        val auth = AuthStore(application)
        player = (auth.signUp("Explorer", "secret", "secret") as AuthResult.Success).player
        progress = ProgressStore(application)
    }

    @After
    fun tearDown() {
        prefs().edit().clear().commit()
    }

    private fun prefs() = application.getSharedPreferences("geoseek_auth", Context.MODE_PRIVATE)

    @Test
    fun recordHunt_banksXpScoreCardsAndFinds() {
        val after = progress.recordHunt(player, score = 120, xpEarned = 40, found = listOf(chair, dog))
        assertEquals(1, after.huntsPlayed)
        assertEquals(120, after.bestScore)
        assertEquals(40, after.xp)
        assertEquals(setOf("Chair", "Dog"), after.cards)
        assertEquals(2, after.findsToday)
    }

    @Test
    fun recordHunt_keepsTheBestScore() {
        val first = progress.recordHunt(player, score = 120, xpEarned = 0, found = emptyList())
        val second = progress.recordHunt(first, score = 80, xpEarned = 0, found = emptyList())
        assertEquals(120, second.bestScore)
        assertEquals(2, second.huntsPlayed)
    }

    @Test
    fun collect_awardsPointsOnlyForNewCardsByDefault() {
        val once = progress.collect(player, chair)
        val twice = progress.collect(once, chair)
        assertEquals(10, once.xp)
        assertEquals(10, twice.xp)
        assertEquals(2, twice.findsToday)
    }

    @Test
    fun collect_usesTheModesXpWhenGiven() {
        assertEquals(33, progress.collect(player, chair, xpEarned = 33).xp)
    }

    @Test
    fun claimQuest_onlyPaysOncePerDay() {
        val claimed = progress.claimQuest(player, 50)
        assertTrue(claimed.questClaimedToday)
        assertEquals(50, progress.claimQuest(claimed, 50).xp)
        assertFalse(player.questClaimedToday)
    }

    @Test
    fun progress_survivesANewStoreInstance() {
        progress.recordHunt(player, score = 10, xpEarned = 5, found = listOf(chair))
        assertEquals(5, AuthStore(application).currentPlayer()!!.xp)
    }

    @Test
    fun duplicates_areCountedAndKeepTheirFirstFoundDate() {
        val first = progress.collect(player, chair)
        val firstFound = first.binder.getValue("Chair").firstFoundMillis
        assertTrue(firstFound > 0)

        val later = progress.recordHunt(first, score = 0, xpEarned = 0, found = listOf(chair, dog))
        assertEquals(CardRecord(2, firstFound), later.binder["Chair"])
        assertEquals(1, later.binder.getValue("Dog").count)
        assertEquals(3, later.totalCards)
    }

    @Test
    fun legacyCardsWithoutCounts_loadAsOneCopy() {
        prefs().edit().putStringSet("user.explorer.cards", setOf("Chair")).commit()
        assertEquals(CardRecord(1, 0L), AuthStore(application).currentPlayer()!!.binder["Chair"])
    }
}
