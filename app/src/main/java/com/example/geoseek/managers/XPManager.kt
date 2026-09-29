// Tracks the player's XP and level, earned from cards and the daily quest.
package com.example.geoseek.managers

class XPManager {
    var xp: Int = 0
        private set

    val level: Int
        get() = 1 // TODO: Derive level from xp

    fun addXp(amount: Int) {
        // TODO: Add XP (from new cards and the daily quest bonus)
    }

    // TODO: Daily quest: pick one quest per day and award bonus XP when completed
}
