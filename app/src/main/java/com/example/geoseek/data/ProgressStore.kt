// Each player's progress: XP, cards, hunts, and the daily quest.
package com.example.geoseek.data

import android.content.Context
import android.content.SharedPreferences
import com.example.geoseek.managers.XPManager
import com.example.geoseek.models.GameObject
import com.example.geoseek.models.Rarity

/**
 * Saves progress for a signed-in [Player] and returns the player as stored afterwards.
 * Local-only until a real backend exists.
 */
class ProgressStore internal constructor(private val store: PlayerPrefs) {

    constructor(context: Context) : this(PlayerPrefs.from(context))

    private val prefs get() = store.prefs
    private fun key(username: String, field: String) = store.key(username, field)

    /**
     * [player] as currently stored. Screens can hold a copy that's out of date (loaded before another save, or
     * before midnight rolled the daily progress over), and building on it would overwrite newer progress.
     */
    private fun fresh(player: Player): Player = store.load(player.username) ?: player

    /** Counts [found] toward today's finds, rare finds, and new cards for the daily quest. */
    private fun SharedPreferences.Editor.addDailyFinds(player: Player, found: List<GameObject>) = apply {
        val name = player.username
        val day = today()
        putInt(key(name, "finds.$day"), player.daily.finds + found.size)
        putInt(key(name, "rare.$day"), player.daily.rareFinds + found.count { it.rarity >= Rarity.RARE })
        putInt(key(name, "newcards.$day"), player.daily.newCards + found.map { it.name }.distinct().count { it !in player.cards })
    }

    /** Adds one copy of each of [found] to [player]'s binder, dating any first finds [now]. */
    private fun SharedPreferences.Editor.addCards(player: Player, found: List<GameObject>, now: Long) = apply {
        val name = player.username
        putStringSet(key(name, "cards"), player.cards + found.map { it.name })
        found.groupingBy { it.name }.eachCount().forEach { (card, copies) ->
            val existing = player.binder[card]
            putInt(store.cardKey(name, card, "count"), (existing?.count ?: 0) + copies)
            if (existing == null) putLong(store.cardKey(name, card, "first"), now)
        }
    }

    /**
     * Records a finished hunt: the score counts toward the best score, the XP is banked,
     * every found object adds a card (or a duplicate) to the collection, and each find counts toward the daily quest.
     */
    fun recordHunt(stale: Player, score: Int, xpEarned: Int, found: List<GameObject>): Player {
        val player = fresh(stale)
        val name = player.username
        prefs.edit()
            .putInt(key(name, "hunts"), player.huntsPlayed + 1)
            .putInt(key(name, "best"), maxOf(player.bestScore, score))
            .putInt(key(name, "xp"), XPManager.addXpCapped(player.xp, xpEarned))
            .addCards(player, found, System.currentTimeMillis())
            .addDailyFinds(player, found)
            .putInt(key(name, "hunts.${today()}"), player.daily.hunts + 1)
            .apply()
        return store.load(name) ?: player
    }

    /** Adds a card. Optional [xpEarned] parameter allows custom game modes (e.g. CollectionMode) to award dynamic XP. */
    fun collect(stale: Player, obj: GameObject, xpEarned: Int? = null): Player {
        val player = fresh(stale)
        val name = player.username
        val isNew = obj.name !in player.cards
        val addedXp = xpEarned ?: if (isNew) obj.points else 0
        prefs.edit()
            .addCards(player, listOf(obj), System.currentTimeMillis())
            .addDailyFinds(player, listOf(obj))
            .putInt(key(name, "xp"), XPManager.addXpCapped(player.xp, addedXp))
            .apply()
        return store.load(name) ?: player
    }

    fun claimQuest(stale: Player, rewardXp: Int): Player {
        val player = fresh(stale)
        if (player.questClaimedToday) return player
        val name = player.username
        prefs.edit()
            .putInt(key(name, "xp"), XPManager.addXpCapped(player.xp, rewardXp))
            .putBoolean(key(name, "quest.${today()}"), true)
            .apply()
        return store.load(name) ?: player
    }
}
