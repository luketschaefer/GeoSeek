package com.geoseek.domain.util

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SeededRandomTest {
    @Test
    fun `matches SplitMix64 reference output`() {
        // Reference values for seed 0 from the SplitMix64 paper's reference implementation.
        val random = SeededRandom(0)
        assertThat(random.nextLongRaw()).isEqualTo(-0x1ddf57c684e23251L) // 0xE220A8397B1DCDAF
        assertThat(random.nextLongRaw()).isEqualTo(0x6E789E6AA1B965F4L)
    }

    @Test
    fun `same seed gives same sequence`() {
        val a = SeededRandom(42)
        val b = SeededRandom(42)
        repeat(100) { assertThat(a.nextInt(1000)).isEqualTo(b.nextInt(1000)) }
    }
}
