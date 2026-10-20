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
        get() = levelFor(xp)

    /** Total cumulative XP required to achieve the next level. */
    val xpForNextLevel: Int
        get() = level * XP_PER_LEVEL

    /** Fractional progress value (0.0f to 1.0f) toward the next level for Compose progress bars. */
    val progressToNextLevel: Float
        get() = xpIntoLevel(xp) / XP_PER_LEVEL.toFloat()

    /**
     * Adds experience points to the player's profile.
     *
     * @param amount XP points to award.
     * @return True if this XP addition triggered a level-up milestone.
     */
    fun addXp(amount: Int): Boolean {
        if (amount <= 0) return false
        val previousLevel = level
        xp = addXpCapped(xp, amount)
        return level > previousLevel
    }

    /** The level curve, shared with anything that only has a stored XP total (e.g. a saved player). */
    companion object {
        /** XP needed to go from one level to the next. */
        const val XP_PER_LEVEL = 100

        /** The level reached with [xp] total experience. */
        fun levelFor(xp: Int): Int = (xp.coerceAtLeast(0) / XP_PER_LEVEL) + 1

        /** XP earned past the start of the current level. */
        fun xpIntoLevel(xp: Int): Int = xp.coerceAtLeast(0) % XP_PER_LEVEL

        /** [xp] plus [amount], capped at Int.MAX_VALUE instead of overflowing. Negative amounts are ignored. */
        fun addXpCapped(xp: Int, amount: Int): Int =
            (xp.toLong() + amount.coerceAtLeast(0)).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    }
}
