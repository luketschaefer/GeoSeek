// Each player's progress: XP, cards, hunts, and the daily quest.
package com.example.geoseek.data

import android.content.Context
import com.example.geoseek.managers.XPManager
import com.example.geoseek.models.GameObject

/**
 * Saves progress for a signed-in [Player] and returns the player as stored afterwards.
 * Local-only until a real backend exists.
 */
class ProgressStore internal constructor(private val store: PlayerPrefs) {

    constructor(context: Context) : this(PlayerPrefs.from(context))

    private val prefs get() = store.prefs
    private fun key(username: String, field: String) = store.key(username, field)

    /**
     * Records a finished hunt: the score counts toward the best score, the XP is banked,
     * every found object joins the collection, and each find counts toward the daily quest.
     */
    fun recordHunt(player: Player, score: Int, xpEarned: Int, found: List<GameObject>): Player {
        val name = player.username
        prefs.edit()
            .putInt(key(name, "hunts"), player.huntsPlayed + 1)
            .putInt(key(name, "best"), maxOf(player.bestScore, score))
            .putInt(key(name, "xp"), XPManager.addXpCapped(player.xp, xpEarned))
            .putStringSet(key(name, "cards"), player.cards + found.map { it.name })
            .putInt(key(name, "finds.${today()}"), player.findsToday + found.size)
            .apply()
        return store.load(name) ?: player
    }

    /** Adds a card. Optional [xpEarned] parameter allows custom game modes (e.g. CollectionMode) to award dynamic XP. */
    fun collect(player: Player, obj: GameObject, xpEarned: Int? = null): Player {
        val name = player.username
        val isNew = obj.name !in player.cards
        val addedXp = xpEarned ?: if (isNew) obj.points else 0
        prefs.edit()
            .putStringSet(key(name, "cards"), player.cards + obj.name)
            .putInt(key(name, "xp"), XPManager.addXpCapped(player.xp, addedXp))
            .putInt(key(name, "finds.${today()}"), player.findsToday + 1)
            .apply()
        return store.load(name) ?: player
    }

    fun claimQuest(player: Player, rewardXp: Int): Player {
        if (player.questClaimedToday) return player
        val name = player.username
        prefs.edit()
            .putInt(key(name, "xp"), XPManager.addXpCapped(player.xp, rewardXp))
            .putBoolean(key(name, "quest.${today()}"), true)
            .apply()
        return store.load(name) ?: player
    }
}
