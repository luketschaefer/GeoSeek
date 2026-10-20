// The daily quest rotation: one quest a day, each tracking a different kind of progress.
package com.example.geoseek.engine

import com.example.geoseek.data.DailyProgress

/** What a daily quest counts. */
enum class QuestGoal(val unit: String) {
    FINDS("found"),
    RARE_FINDS("rare+ found"),
    NEW_CARDS("new cards"),
    HUNTS("hunts"),
}

data class DailyQuest(val title: String, val detail: String, val goalType: QuestGoal, val goal: Int, val rewardXp: Int) {
    /** How far [daily] is toward this quest, capped at the goal. */
    fun progress(daily: DailyProgress): Int = when (goalType) {
        QuestGoal.FINDS -> daily.finds
        QuestGoal.RARE_FINDS -> daily.rareFinds
        QuestGoal.NEW_CARDS -> daily.newCards
        QuestGoal.HUNTS -> daily.hunts
    }.coerceIn(0, goal)

    fun isDone(daily: DailyProgress): Boolean = progress(daily) >= goal
}

val DAILY_QUESTS = listOf(
    DailyQuest("Morning Forager", "Find 3 objects today", QuestGoal.FINDS, 3, 50),
    DailyQuest("Treasure Seeker", "Find 1 Rare or better object today", QuestGoal.RARE_FINDS, 1, 70),
    DailyQuest("Keen Eye", "Find 5 objects today", QuestGoal.FINDS, 5, 80),
    DailyQuest("Collector", "Add 1 new card to your binder today", QuestGoal.NEW_CARDS, 1, 60),
    DailyQuest("Racer", "Finish 2 hunts today", QuestGoal.HUNTS, 2, 60),
    DailyQuest("Wanderer", "Find 4 objects today", QuestGoal.FINDS, 4, 65),
    DailyQuest("Big Game", "Find 2 Rare or better objects today", QuestGoal.RARE_FINDS, 2, 100),
    DailyQuest("Quick Look", "Find 2 objects today", QuestGoal.FINDS, 2, 35),
)

/**
 * The quest for [day] (see [com.example.geoseek.data.today]). Players who already own every card
 * never get a new-card quest, since they couldn't finish it.
 */
fun questForDay(day: Int, binderComplete: Boolean, quests: List<DailyQuest> = DAILY_QUESTS): DailyQuest {
    val eligible = if (binderComplete) quests.filter { it.goalType != QuestGoal.NEW_CARDS } else quests
    return eligible[day.mod(eligible.size)]
}
