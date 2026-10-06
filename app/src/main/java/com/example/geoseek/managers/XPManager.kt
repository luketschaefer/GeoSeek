// Tracks player experience points (XP), leveling progress, and quest rewards.
package com.example.geoseek.managers

/**
 * Manages player profile experience points (XP) and level calculations.
 *
 * Integrates with:
 * - **Collection Mode ([com.example.geoseek.engine.modes.CollectionMode])**: Consumes XP awarded from trading card discoveries.
 * - **Player Profile UI**: Provides state properties for Jetpack Compose UI progress indicators.
 */
class XPManager {
    /** Total accumulated experience points. */
    var xp: Int = 0
        private set

    /** Derived player level calculated as `(xp / 100) + 1`. */
    val level: Int
        get() = (xp / 100) + 1

    /** Total cumulative XP required to achieve the next level. */
    val xpForNextLevel: Int
        get() = level * 100

    /** Fractional progress value (0.0f to 1.0f) toward the next level for Compose progress bars. */
    val progressToNextLevel: Float
        get() = (xp % 100) / 100f

    /**
     * Adds experience points to the player's profile.
     *
     * @param amount XP points to award.
     * @return True if this XP addition triggered a level-up milestone.
     */
    fun addXp(amount: Int): Boolean {
        if (amount <= 0) return false
        val previousLevel = level
        xp += amount
        return level > previousLevel
    }
}
