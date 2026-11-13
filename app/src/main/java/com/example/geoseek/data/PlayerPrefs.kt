// The shared preferences file every player's account and progress live in, and how players are read from it.
package com.example.geoseek.data

import android.content.Context
import android.content.SharedPreferences
import java.util.Calendar

/**
 * Reads players from the app's one preferences file. [AuthStore] and [ProgressStore] write to it;
 * both go through here so they agree on key names.
 */
internal class PlayerPrefs(val prefs: SharedPreferences, private val clock: DailyQuestClock) {

    fun today(): Int = clock.today()

    fun key(username: String, field: String) = "user.${username.lowercase()}.$field"

    fun cardKey(username: String, card: String, field: String) = key(username, "card.$card.$field")

    fun exists(username: String) = prefs.contains(key(username, "hash"))

    fun load(username: String, day: Int = today()): Player? {
        val display = prefs.getString(key(username, "display"), null) ?: return null
        migrateDailyProgress(username, day)
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
                finds = prefs.getInt(key(username, "finds.$day"), 0),
                rareFinds = prefs.getInt(key(username, "rare.$day"), 0),
                newCards = prefs.getInt(key(username, "newcards.$day"), 0),
                hunts = prefs.getInt(key(username, "hunts.$day"), 0),
            ),
            questClaimedToday = prefs.getBoolean(key(username, "quest.$day"), false),
            questDay = day,
        )
    }

    /** Preserve today's legacy local-date progress when first switching this player to UTC. */
    private fun migrateDailyProgress(username: String, day: Int) {
        val migrated = key(username, "quest_clock_migrated")
        if (prefs.getBoolean(migrated, false)) return
        val legacyDay = Calendar.getInstance().let {
            it.get(Calendar.YEAR) * 1000 + it.get(Calendar.DAY_OF_YEAR)
        }
        prefs.edit().apply {
            if (legacyDay != day) {
                listOf("finds", "rare", "newcards", "hunts").forEach { field ->
                    putInt(
                        key(username, "$field.$day"),
                        maxOf(
                            prefs.getInt(key(username, "$field.$day"), 0),
                            prefs.getInt(key(username, "$field.$legacyDay"), 0),
                        ),
                    )
                    remove(key(username, "$field.$legacyDay"))
                }
                putBoolean(
                    key(username, "quest.$day"),
                    prefs.getBoolean(key(username, "quest.$day"), false) ||
                        prefs.getBoolean(key(username, "quest.$legacyDay"), false),
                )
                // Move rather than copy: tomorrow must not reload the same legacy progress.
                remove(key(username, "quest.$legacyDay"))
            }
            putBoolean(migrated, true)
            apply()
        }
    }

    companion object {
        // Kept from when accounts were the only thing stored, so existing players keep their progress.
        private const val FILE = "geoseek_auth"

        fun from(context: Context): PlayerPrefs {
            val app = context.applicationContext
            val prefs = app.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            return PlayerPrefs(prefs, DailyQuestClock.from(app, prefs))
        }
    }
}
