package com.example.geoseek.data

import android.content.Context
import com.example.geoseek.engine.HuntRound
import com.example.geoseek.models.GameObject
import com.example.geoseek.models.Rarity
import org.json.JSONObject
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
class HuntHistoryStoreTest {
    private val app = RuntimeEnvironment.getApplication()
    private val prefs get() = app.getSharedPreferences("geoseek_auth", Context.MODE_PRIVATE)
    private lateinit var player: Player
    private lateinit var progress: ProgressStore
    private lateinit var history: HuntHistoryStore
    private val chair = GameObject("Chair", Rarity.COMMON, 10)
    private val dog = GameObject("Dog", Rarity.EPIC, 75)

    @Before
    fun setUp() {
        prefs.edit().clear().commit()
        player = (AuthStore(app).signUp("Explorer", "secret", "secret") as AuthResult.Success).player
        progress = ProgressStore(app)
        history = HuntHistoryStore(app)
    }

    @After
    fun tearDown() {
        prefs.edit().clear().commit()
    }

    private fun round(environment: String = "Indoors", finds: Int = 0, timeout: Boolean = false) =
        HuntRound(environment, listOf(chair, dog), 90).apply {
            listOf(chair, dog).take(finds).forEach { onObjectFound(it) }
            if (timeout) tickTime(90)
        }

    private fun save(round: HuntRound): Player = progress.recordHunt(player, round, round.finish())

    @Test
    fun accountWithoutHistoryLoadsEmptyWithoutChangingProgress() {
        val before = prefs.all.toMap()
        assertEquals(HuntHistory(), history.load(player.username))
        assertEquals(before, prefs.all)
    }

    @Test
    fun recordIncludesEnvironmentTimeFoundMissedScoreXpAndOutcome() {
        val hunt = round(finds = 1)
        val result = hunt.finish()
        val earliest = System.currentTimeMillis()
        save(hunt)
        val entry = history.load(player.username).records.single()
        assertEquals(hunt.id, entry.id)
        assertEquals("Indoors", entry.environment)
        assertTrue(entry.finishedAtMillis in earliest..System.currentTimeMillis())
        assertEquals(listOf("Chair", "Dog"), entry.targets)
        assertEquals(listOf("Chair"), entry.found)
        assertEquals(listOf("Dog"), entry.missed)
        assertEquals(result.totalScore, entry.score)
        assertEquals(result.totalXpEarned, entry.xpEarned)
        assertEquals(HuntOutcome.ENDED_EARLY, entry.outcome)
    }

    @Test
    fun completedAndTimedOutHuntsHaveDistinctOutcomes() {
        save(round(finds = 2, timeout = true))
        save(round(timeout = true))
        assertEquals(listOf(HuntOutcome.TIMED_OUT, HuntOutcome.COMPLETED), history.load(player.username).records.map { it.outcome })
        assertEquals(1, history.load(player.username).stats.completed)
        assertEquals(1, history.load(player.username).stats.timedOut)
    }

    @Test
    fun historyAndStatsSurviveNewStoreInstanceAndUsernameCase() {
        save(round(finds = 1))
        assertEquals(history.load("Explorer"), HuntHistoryStore(app).load("EXPLORER"))
    }

    @Test
    fun accountsDoNotShareRecordsOrStatistics() {
        save(round(finds = 2))
        val other = (AuthStore(app).signUp("Other", "secret", "secret") as AuthResult.Success).player
        assertEquals(HuntHistory(), history.load(other.username))
        assertEquals(1, history.load(player.username).records.size)
    }

    @Test
    fun recordsAreNewestFirstAndTrimToFiftyWhileStatsKeepAllHunts() {
        val first = round(finds = 2)
        val firstResult = first.finish()
        save(first)
        var lastId = ""
        repeat(50) {
            val hunt = round("Park")
            lastId = hunt.id
            save(hunt)
        }
        val saved = history.load(player.username)
        assertEquals(50, saved.records.size)
        assertEquals(lastId, saved.records.first().id)
        assertFalse(saved.records.any { it.id == first.id })
        assertEquals(51, saved.stats.recordedHunts)
        assertEquals(1, saved.stats.completed)
        assertEquals(50, saved.stats.endedEarly)
        assertEquals(firstResult.totalScore, saved.stats.personalBest)
        assertEquals(listOf("Park"), saved.stats.mostPlayed)
        assertEquals(firstResult.totalScore.toLong(), saved.stats.totalScore)
        val beforeReplay = AuthStore(app).currentPlayer()!!
        assertEquals(beforeReplay, save(first))
        assertEquals(saved, history.load(player.username))
    }

    @Test
    fun repeatedFinishCallbackDoesNotDuplicateHistoryStatsOrRewards() {
        val hunt = round(finds = 1)
        val result = hunt.finish()
        val once = progress.recordHunt(player, hunt, result)
        val saved = history.load(player.username)
        val twice = ProgressStore(app).recordHunt(player, hunt, result)
        assertEquals(once, twice)
        assertEquals(saved, history.load(player.username))
        assertEquals(result.totalXpEarned, twice.xp)
        assertEquals(1, twice.huntsPlayed)
    }

    @Test
    fun identicalResultsFromDifferentRoundsAreBothRecorded() {
        save(round())
        save(round())
        val saved = history.load(player.username)
        assertEquals(2, saved.records.map { it.id }.distinct().size)
        assertEquals(2, saved.stats.recordedHunts)
    }

    @Test
    fun readingHistoryNeverAwardsMoreXpCardsOrQuestProgress() {
        save(round(finds = 2))
        val before = prefs.all.toMap()
        repeat(3) { HuntHistoryStore(app).load(player.username) }
        assertEquals(before, prefs.all)
    }

    @Test
    fun statisticsIncludeAverageCompletionRateAndTiesForMostPlayed() {
        save(round("Park", finds = 2))
        save(round("Indoors", finds = 1))
        val stats = history.load(player.username).stats
        assertEquals(listOf("Indoors", "Park"), stats.mostPlayed)
        assertEquals(50L, stats.completionPercent)
        assertEquals((535L + 10L) / 2, stats.averageScore)
        assertEquals(250L, stats.totalXp)
    }

    @Test
    fun malformedSavedHistoryDoesNotCrashScreenLoading() {
        prefs.edit().putString("user.explorer.hunt_history", "broken json").apply()
        assertEquals(HuntHistory(), history.load(player.username))
    }

    @Test
    fun malformedRecordIsSkippedWithoutLosingOtherRecordsOrTotals() {
        save(round())
        save(round("Park"))
        val json = JSONObject(prefs.getString("user.explorer.hunt_history", null)!!)
        json.getJSONArray("records").getJSONObject(0).put("outcome", "unknown")
        prefs.edit().putString("user.explorer.hunt_history", json.toString()).apply()
        val loaded = history.load(player.username)
        assertEquals(1, loaded.records.size)
        assertEquals("Indoors", loaded.records.single().environment)
        assertEquals(2, loaded.stats.recordedHunts)
    }

    @Test
    fun scoreAndXpTotalsDoNotOverflowInt() {
        val record = HuntRecord.from(round(), round().finish()).copy(score = Int.MAX_VALUE, xpEarned = Int.MAX_VALUE)
        val stats = HuntStats().adding(record).adding(record)
        assertEquals(4_294_967_294L, stats.totalScore)
        assertEquals(4_294_967_294L, stats.totalXp)
    }
}
