package com.example.geoseek.data

import android.app.Application
import android.content.Context
import com.example.geoseek.engine.HuntRound
import com.example.geoseek.engine.questForDay
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

    private fun completedQuestPlayer(): Player {
        val day = player.questDay
        prefs().edit()
            .putInt("user.explorer.finds.$day", 5)
            .putInt("user.explorer.rare.$day", 2)
            .putInt("user.explorer.newcards.$day", 1)
            .putInt("user.explorer.hunts.$day", 2)
            .apply()
        return AuthStore(application).currentPlayer()!!
    }

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

    /** Issue #7: repeatedly ending empty hunts must not increase the saved XP balance. */
    @Test
    fun recordHunt_repeatedEmptyEarlyEndingsDoNotAwardXp() {
        val before = progress.collect(player, chair, xpEarned = 100)
        repeat(3) { index ->
            val round = HuntRound("Indoors", listOf(chair, dog), durationSeconds = 90)
            val result = round.finish()
            progress.recordHunt(before, result.totalScore, result.totalXpEarned, round.found)

            val reloaded = AuthStore(application).currentPlayer()!!
            assertEquals("XP after empty early-ended hunt ${index + 1}", before.xp, reloaded.xp)
            assertEquals(before.binder, reloaded.binder)
            assertEquals(before.daily.finds, reloaded.daily.finds)
            assertEquals(index + 1, reloaded.huntsPlayed)
        }
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
        val done = completedQuestPlayer()
        val reward = questForDay(done.questDay, binderComplete = false).rewardXp
        val claimed = progress.claimQuest(done, reward)
        assertTrue(claimed.questClaimedToday)
        assertEquals(reward, progress.claimQuest(claimed, reward).xp)
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

    @Test
    fun dailyProgress_countsRareFindsNewCardsAndHunts() {
        val clock = GameObject("Clock", Rarity.RARE, 35)
        val afterCollect = progress.collect(player, chair)
        val afterHunt = progress.recordHunt(afterCollect, score = 0, xpEarned = 0, found = listOf(chair, dog, clock))
        assertEquals(DailyProgress(finds = 4, rareFinds = 2, newCards = 3, hunts = 1), afterHunt.daily)
    }

    @Test
    fun saves_buildOnStoredProgressEvenFromAStaleCopy() {
        progress.collect(player, chair)
        // `player` predates the collect; saving from it must not wipe the chair or its XP.
        val after = progress.recordHunt(player, score = 0, xpEarned = 5, found = listOf(dog))
        assertEquals(setOf("Chair", "Dog"), after.cards)
        assertEquals(15, after.xp)
        assertEquals(2, after.findsToday)
        // The stale copy doesn't know the quest was claimed, but the store does.
        val done = completedQuestPlayer()
        val reward = questForDay(done.questDay, binderComplete = false).rewardXp
        progress.claimQuest(done, reward)
        assertEquals(15 + reward, progress.claimQuest(player, reward).xp)
    }
}
