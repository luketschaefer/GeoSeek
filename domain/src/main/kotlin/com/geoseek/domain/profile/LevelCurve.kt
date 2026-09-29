package com.geoseek.domain.profile

import kotlin.math.floor
import kotlin.math.sqrt

data class LevelProgress(
    val level: Int,
    val totalXp: Long,
    /** XP earned since reaching [level]. */
    val xpIntoLevel: Long,
    /** XP needed to go from [level] to level + 1. */
    val xpForNextLevel: Long,
) {
    val fraction: Float get() = xpIntoLevel.toFloat() / xpForNextLevel
}

/**
 * Total XP required to reach level n is `50 * n * (n - 1)`: L1 = 0, L2 = 100, L3 = 300, L4 = 600.
 * Each level costs 100 XP more than the previous one.
 */
object LevelCurve {
    private const val STEP = 50L

    fun totalXpForLevel(level: Int): Long {
        require(level >= 1) { "level must be >= 1" }
        return STEP * level * (level - 1)
    }

    fun progress(totalXp: Long): LevelProgress {
        require(totalXp >= 0) { "totalXp must be >= 0, was $totalXp" }
        // Solve 50n(n-1) <= xp for the largest n, then correct for floating-point error.
        var level = floor((1 + sqrt(1 + 4.0 * totalXp / STEP)) / 2).toInt().coerceAtLeast(1)
        while (totalXpForLevel(level + 1) <= totalXp) level++
        while (level > 1 && totalXpForLevel(level) > totalXp) level--
        val base = totalXpForLevel(level)
        return LevelProgress(
            level = level,
            totalXp = totalXp,
            xpIntoLevel = totalXp - base,
            xpForNextLevel = totalXpForLevel(level + 1) - base,
        )
    }
}
