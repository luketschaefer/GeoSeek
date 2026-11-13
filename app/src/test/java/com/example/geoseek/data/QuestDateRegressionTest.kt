package com.example.geoseek.data

import android.content.Context
import com.example.geoseek.engine.questForDay
import com.example.geoseek.models.GameObject
import com.example.geoseek.models.Rarity
import java.util.TimeZone
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class QuestDateRegressionTest {
    private val app = RuntimeEnvironment.getApplication()
    private val prefs get() = app.getSharedPreferences("geoseek_auth", Context.MODE_PRIVATE)
    private lateinit var originalZone: TimeZone
    private lateinit var auth: AuthStore
    private lateinit var progress: ProgressStore
    private lateinit var player: Player
    private val chair = GameObject("Chair", Rarity.COMMON, 10)
    private val dog = GameObject("Dog", Rarity.EPIC, 75)

    @Before
    fun setUp() {
        originalZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("Etc/GMT+12"))
        prefs.edit().clear().commit()
        auth = AuthStore(app)
        progress = ProgressStore(app)
        player = (auth.signUp("Explorer", "secret", "secret") as AuthResult.Success).player
    }

    @After
    fun tearDown() {
        TimeZone.setDefault(originalZone)
        prefs.edit().clear().commit()
    }

    @Test
    fun timeZoneChangeDoesNotResetDailyProgress() {
        val before = progress.collect(player, chair, xpEarned = 0)
        TimeZone.setDefault(TimeZone.getTimeZone("Pacific/Kiritimati"))

        assertEquals(before.daily, AuthStore(app).currentPlayer()!!.daily)
    }

    @Test
    fun timeZoneChangeAndStoreRecreationDoNotReopenClaimedReward() {
        val finds = List(5) { chair } + List(2) { dog }
        val first = progress.recordHunt(player, 0, 0, finds)
        val done = progress.recordHunt(first, 0, 0, emptyList())
        val reward = questForDay(done.questDay, binderComplete = false).rewardXp
        val claimed = progress.claimQuest(done, reward)
        assertTrue(claimed.questClaimedToday)

        TimeZone.setDefault(TimeZone.getTimeZone("Pacific/Kiritimati"))
        val reloaded = AuthStore(app).currentPlayer()!!
        assertTrue("Changing time zone must not reopen a claimed reward", reloaded.questClaimedToday)
        assertEquals(claimed.xp, ProgressStore(app).claimQuest(reloaded, reward).xp)
    }
}
