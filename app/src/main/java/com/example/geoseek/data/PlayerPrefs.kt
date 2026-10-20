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

    fun exists(username: String) = prefs.contains(key(username, "hash"))

    fun load(username: String): Player? {
        val display = prefs.getString(key(username, "display"), null) ?: return null
        return Player(
            username = display,
            joinedAtMillis = prefs.getLong(key(username, "joined"), 0L),
            xp = prefs.getInt(key(username, "xp"), 0),
            huntsPlayed = prefs.getInt(key(username, "hunts"), 0),
            bestScore = prefs.getInt(key(username, "best"), 0),
            cards = prefs.getStringSet(key(username, "cards"), emptySet())!!.toSet(),
            findsToday = prefs.getInt(key(username, "finds.${today()}"), 0),
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
