package com.geoseek.domain.util

import kotlin.random.Random

/**
 * SplitMix64 PRNG. Used instead of `Random(seed)` because Kotlin only guarantees
 * `Random(seed)` sequences within one runtime version; daily quests must be identical
 * for every player regardless of which app build they run.
 */
class SeededRandom(
    seed: Long,
) : Random() {
    private var state: Long = seed

    fun nextLongRaw(): Long {
        state += GOLDEN_GAMMA
        var z = state
        z = (z xor (z ushr SHIFT_1)) * MIX_1
        z = (z xor (z ushr SHIFT_2)) * MIX_2
        return z xor (z ushr SHIFT_3)
    }

    override fun nextBits(bitCount: Int): Int {
        if (bitCount == 0) return 0
        return (nextLongRaw() ushr (Long.SIZE_BITS - bitCount)).toInt()
    }

    private companion object {
        const val GOLDEN_GAMMA = -0x61c8864680b583ebL
        const val MIX_1 = -0x40a7b892e31b1a47L
        const val MIX_2 = -0x6b2fb644ecceee15L
        const val SHIFT_1 = 30
        const val SHIFT_2 = 27
        const val SHIFT_3 = 31
    }
}
