package com.example.geoseek.data

import android.content.Context
import com.example.geoseek.engine.questForDay
import com.example.geoseek.models.GameObject
import com.example.geoseek.models.Rarity
import java.time.Instant
import java.util.Calendar
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
class QuestClockProgressTest {
    private val app = RuntimeEnvironment.getApplication()
    private val prefs get() = app.getSharedPreferences("geoseek_auth", Context.MODE_PRIVATE)
    private var wall = Instant.parse("2026-11-10T12:00:00Z").toEpochMilli()
    private var elapsed = 10_000L
    private var boot = 1
    private var advanceAfterRead = false
    private lateinit var auth: AuthStore
    private lateinit var progress: ProgressStore
    private lateinit var player: Player
    private val chair = GameObject("Chair", Rarity.COMMON, 10)
    private val dog = GameObject("Dog", Rarity.EPIC, 75)
    private val plant = GameObject("Plant", Rarity.COMMON, 15)

    private fun store() = PlayerPrefs(prefs, DailyQuestClock(prefs, { wall }, {
        elapsed.also {
            if (advanceAfterRead) {
                elapsed += 2_000L
                advanceAfterRead = false
            }
        }
    }, { boot }))

    @Before
    fun setUp() {
        prefs.edit().clear().commit()
        auth = AuthStore(store())
        progress = ProgressStore(store())
        player = (auth.signUp("Explorer", "secret", "secret") as AuthResult.Success).player
    }

    @After
    fun tearDown() {
        prefs.edit().clear().commit()
    }

    private fun complete(current: Player, newCard: GameObject? = null): Player {
        val found = List(5) { chair } + List(2) { dog } + listOfNotNull(newCard)
        val first = progress.recordHunt(current, 0, 0, found)
        return progress.recordHunt(first, 0, 0, emptyList())
    }

    private fun reward(current: Player) = questForDay(current.questDay, binderComplete = false).rewardXp

    @Test
    fun wallClockAdvanceAndStoreRecreationDoNotAllowAnotherReward() {
        val done = complete(player)
        val claimed = progress.claimQuest(done, reward(done))
        wall += 10 * DAY
        val reloaded = AuthStore(store()).currentPlayer()!!

        assertEquals(claimed.questDay, reloaded.questDay)
        assertEquals(claimed.daily, reloaded.daily)
        assertTrue(reloaded.questClaimedToday)
        assertEquals(claimed.xp, ProgressStore(store()).claimQuest(reloaded, reward(done)).xp)
    }

    @Test
    fun rollingWallClockBackDoesNotRestoreAnExpiredUnclaimedReward() {
        val done = complete(player)
        elapsed += DAY
        val nextDay = auth.currentPlayer()!!
        wall -= DAY
        val after = progress.claimQuest(done, reward(done))

        assertEquals(nextDay.questDay, after.questDay)
        assertEquals(DailyProgress(), after.daily)
        assertEquals(0, after.xp)
        assertFalse(after.questClaimedToday)
    }

    @Test
    fun genuineNextDayResetsProgressAndAllowsExactlyOneNewReward() {
        val done = complete(player)
        val claimed = progress.claimQuest(done, reward(done))
        elapsed += DAY
        val nextDay = auth.currentPlayer()!!
        assertEquals(2026315, nextDay.questDay)
        assertEquals(DailyProgress(), nextDay.daily)
        assertFalse(nextDay.questClaimedToday)

        val nextDone = complete(nextDay, newCard = plant)
        val nextClaimed = progress.claimQuest(nextDone, reward(nextDone))
        assertTrue(nextClaimed.questClaimedToday)
        assertEquals(claimed.xp + reward(nextDone), nextClaimed.xp)
        assertEquals(nextClaimed.xp, progress.claimQuest(nextDone, reward(nextDone)).xp)
    }

    @Test
    fun stalePreviousDayClaimIsRejectedEvenWhenCurrentQuestIsComplete() {
        val oldDone = complete(player)
        elapsed += DAY
        val newDone = complete(auth.currentPlayer()!!, newCard = plant)
        val after = progress.claimQuest(oldDone, reward(oldDone))
        assertEquals(0, after.xp)
        assertFalse(after.questClaimedToday)
        assertTrue(progress.claimQuest(newDone, reward(newDone)).questClaimedToday)
    }

    @Test
    fun incompleteQuestCannotBeClaimed() {
        val after = progress.claimQuest(player, reward(player))
        assertEquals(0, after.xp)
        assertFalse(after.questClaimedToday)
    }

    @Test
    fun staleOrIncorrectRewardAmountCannotBeClaimed() {
        val done = complete(player)
        val after = progress.claimQuest(done, reward(done) + 1)
        assertEquals(0, after.xp)
        assertFalse(after.questClaimedToday)
        assertEquals(reward(done), progress.claimQuest(done, reward(done)).xp)
    }

    @Test
    fun collectingFromOldSnapshotCountsOnlyTowardCurrentDay() {
        val old = progress.collect(player, chair, xpEarned = 0)
        elapsed += DAY
        val after = progress.collect(old, dog, xpEarned = 0)
        assertEquals(2026315, after.questDay)
        assertEquals(DailyProgress(finds = 1, rareFinds = 1, newCards = 1), after.daily)
    }

    @Test
    fun huntFromOldSnapshotCountsOnlyTowardCurrentDay() {
        val old = progress.recordHunt(player, 0, 0, listOf(chair))
        elapsed += DAY
        val after = progress.recordHunt(old, 0, 0, listOf(dog))
        assertEquals(2026315, after.questDay)
        assertEquals(DailyProgress(finds = 1, rareFinds = 1, newCards = 1, hunts = 1), after.daily)
        assertEquals(2, after.huntsPlayed)
    }

    @Test
    fun rebootWithEditedDateDoesNotReopenReward() {
        val done = complete(player)
        val claimed = progress.claimQuest(done, reward(done))
        boot++
        elapsed += DAY
        wall += 10 * DAY
        val reloaded = AuthStore(store()).currentPlayer()!!
        assertEquals(claimed.questDay, reloaded.questDay)
        assertTrue(reloaded.questClaimedToday)
        assertEquals(claimed.xp, ProgressStore(store()).claimQuest(reloaded, reward(done)).xp)
    }

    @Test
    fun accountsShareQuestTimeButHaveSeparateClaimFlags() {
        val done = complete(player)
        progress.claimQuest(done, reward(done))
        wall += 10 * DAY
        val other = (auth.signUp("Other", "secret", "secret") as AuthResult.Success).player
        assertEquals(done.questDay, other.questDay)
        assertFalse(other.questClaimedToday)
        assertEquals(DailyProgress(), other.daily)
    }

    @Test
    fun oneSaveUsesOneDayEvenIfMidnightPassesDuringIt() {
        // Reach 23:59:59 on the offline clock, then cross midnight after its next sample.
        elapsed += DAY / 2 - 1_000L
        advanceAfterRead = true
        val after = progress.recordHunt(player, 0, 0, listOf(chair))
        assertEquals(2026314, after.questDay)
        assertEquals(DailyProgress(finds = 1, newCards = 1, hunts = 1), after.daily)
        val nextDay = auth.currentPlayer()!!
        assertEquals(2026315, nextDay.questDay)
        assertEquals(DailyProgress(), nextDay.daily)
    }

    @Test
    fun legacyLocalDateProgressAndClaimAreMigratedOnlyOnce() {
        // Keep the destination distinct from the real legacy date regardless of when this test runs.
        prefs.edit().clear().commit()
        wall = System.currentTimeMillis() - 3 * DAY
        val day = store().today()
        val legacyDay = Calendar.getInstance().let {
            it.get(Calendar.YEAR) * 1000 + it.get(Calendar.DAY_OF_YEAR)
        }
        prefs.edit()
            .putString("user.legacy.display", "Legacy")
            .putInt("user.legacy.xp", 150)
            .putStringSet("user.legacy.cards", setOf("Chair"))
            .putInt("user.legacy.finds.$legacyDay", 5)
            .putInt("user.legacy.finds.$day", 2)
            .putBoolean("user.legacy.quest.$legacyDay", true)
            .apply()
        val migrated = store().load("Legacy")!!
        assertEquals(5, migrated.daily.finds)
        assertTrue(migrated.questClaimedToday)
        assertEquals(150, migrated.xp)
        assertEquals(setOf("Chair"), migrated.cards)
        assertFalse(prefs.contains("user.legacy.finds.$legacyDay"))
        assertFalse(prefs.contains("user.legacy.quest.$legacyDay"))

        elapsed += DAY
        assertEquals(DailyProgress(), store().load("Legacy")!!.daily)
        assertFalse(store().load("Legacy")!!.questClaimedToday)
    }

    companion object {
        private const val DAY = 86_400_000L
    }
}
