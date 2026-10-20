package com.example.geoseek.engine

import com.example.geoseek.data.DailyProgress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyQuestsTest {

    private val daily = DailyProgress(finds = 4, rareFinds = 1, newCards = 2, hunts = 3)

    @Test
    fun progress_readsTheCounterForTheGoalType() {
        assertEquals(4, DailyQuest("", "", QuestGoal.FINDS, 10, 0).progress(daily))
        assertEquals(1, DailyQuest("", "", QuestGoal.RARE_FINDS, 10, 0).progress(daily))
        assertEquals(2, DailyQuest("", "", QuestGoal.NEW_CARDS, 10, 0).progress(daily))
        assertEquals(3, DailyQuest("", "", QuestGoal.HUNTS, 10, 0).progress(daily))
    }

    @Test
    fun progress_isCappedAtTheGoal() {
        val quest = DailyQuest("", "", QuestGoal.FINDS, 3, 0)
        assertEquals(3, quest.progress(daily))
        assertTrue(quest.isDone(daily))
    }

    @Test
    fun isDone_falseBelowTheGoal() {
        assertFalse(DailyQuest("", "", QuestGoal.RARE_FINDS, 2, 0).isDone(daily))
    }

    @Test
    fun questForDay_rotatesThroughEveryQuest() {
        val seen = (0 until DAILY_QUESTS.size).map { questForDay(it, binderComplete = false) }.toSet()
        assertEquals(DAILY_QUESTS.toSet(), seen)
        assertNotEquals(questForDay(2026292, false), questForDay(2026293, false))
    }

    @Test
    fun questForDay_neverAsksForNewCardsWhenTheBinderIsComplete() {
        repeat(DAILY_QUESTS.size * 2) { day ->
            assertNotEquals(QuestGoal.NEW_CARDS, questForDay(day, binderComplete = true).goalType)
        }
    }

    @Test
    fun questForDay_handlesNegativeDays() {
        questForDay(-1, binderComplete = false)
    }

    @Test
    fun everyQuest_hasAPositiveGoalAndReward() {
        DAILY_QUESTS.forEach {
            assertTrue(it.title, it.goal > 0)
            assertTrue(it.title, it.rewardXp > 0)
        }
    }
}
