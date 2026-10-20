// The shared preferences file every player's account and progress live in, and how players are read from it.
package com.example.geoseek.data

import android.content.Context
import android.content.SharedPreferences

/**
 * Reads players from the app's one preferences file. [AuthStore] and [ProgressStore] write to it;
 * both go through here so they agree on key names.
 */
internal class PlayerPrefs(val prefs: SharedPreferences) {

    fun key(username: String, field: String) = "user.${username.lowercase()}.$field"

    fun cardKey(username: String, card: String, field: String) = key(username, "card.$card.$field")

    fun exists(username: String) = prefs.contains(key(username, "hash"))

    fun load(username: String): Player? {
        val display = prefs.getString(key(username, "display"), null) ?: return null
        return Player(
            username = display,
            joinedAtMillis = prefs.getLong(key(username, "joined"), 0L),
            xp = prefs.getInt(key(username, "xp"), 0),
            huntsPlayed = prefs.getInt(key(username, "hunts"), 0),
            bestScore = prefs.getInt(key(username, "best"), 0),
            binder = prefs.getStringSet(key(username, "cards"), emptySet())!!.associateWith { card ->
                CardRecord(
                    // Cards saved before counts existed have no count key; they were found at least once.
                    count = prefs.getInt(cardKey(username, card, "count"), 1).coerceAtLeast(1),
                    firstFoundMillis = prefs.getLong(cardKey(username, card, "first"), 0L),
                )
            },
            daily = DailyProgress(
                finds = prefs.getInt(key(username, "finds.${today()}"), 0),
                rareFinds = prefs.getInt(key(username, "rare.${today()}"), 0),
                newCards = prefs.getInt(key(username, "newcards.${today()}"), 0),
                hunts = prefs.getInt(key(username, "hunts.${today()}"), 0),
            ),
            questClaimedToday = prefs.getBoolean(key(username, "quest.${today()}"), false),
        )
    }

    companion object {
        // Kept from when accounts were the only thing stored, so existing players keep their progress.
        private const val FILE = "geoseek_auth"

        fun from(context: Context) =
            PlayerPrefs(context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE))
    }
}
