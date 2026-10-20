// A signed-in explorer's profile and progress, as loaded from on-device storage.
package com.example.geoseek.data

import com.example.geoseek.managers.XPManager
import java.util.Calendar

data class Player(
    val username: String,
    val joinedAtMillis: Long,
    val xp: Int,
    val huntsPlayed: Int,
    val bestScore: Int,
    val cards: Set<String>,
    val findsToday: Int,
    val questClaimedToday: Boolean,
)

data class LevelInfo(val level: Int, val intoLevel: Int, val needed: Int) {
    val progress: Float get() = intoLevel / needed.toFloat()
}

/** Level progress, using XPManager's level curve. */
val Player.levelInfo: LevelInfo
    get() = LevelInfo(XPManager.levelFor(xp), XPManager.xpIntoLevel(xp), XPManager.XP_PER_LEVEL)

/** Today's date as a number, used to key daily progress. */
fun today(): Int = Calendar.getInstance().let { it.get(Calendar.YEAR) * 1000 + it.get(Calendar.DAY_OF_YEAR) }
