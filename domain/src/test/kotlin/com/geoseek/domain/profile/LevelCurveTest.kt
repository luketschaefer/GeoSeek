package com.geoseek.domain.profile

import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test

class LevelCurveTest {
    @Test
    fun `thresholds follow 50 n (n - 1)`() {
        assertThat((1..5).map(LevelCurve::totalXpForLevel)).containsExactly(0L, 100L, 300L, 600L, 1000L).inOrder()
    }

    @Test
    fun `progress at and around boundaries`() {
        assertThat(LevelCurve.progress(0)).isEqualTo(LevelProgress(1, 0, 0, 100))
        assertThat(LevelCurve.progress(99).level).isEqualTo(1)
        assertThat(LevelCurve.progress(100)).isEqualTo(LevelProgress(2, 100, 0, 200))
        assertThat(LevelCurve.progress(299)).isEqualTo(LevelProgress(2, 299, 199, 200))
        assertThat(LevelCurve.progress(300).level).isEqualTo(3)
    }

    @Test
    fun `level is monotonic and consistent with thresholds`() {
        var previous = 1
        for (xp in 0L..20_000L step 7) {
            val p = LevelCurve.progress(xp)
            assertThat(p.level).isAtLeast(previous)
            assertThat(LevelCurve.totalXpForLevel(p.level)).isAtMost(xp)
            assertThat(LevelCurve.totalXpForLevel(p.level + 1)).isGreaterThan(xp)
            previous = p.level
        }
    }

    @Test
    fun `huge xp does not overflow or drift`() {
        val xp = LevelCurve.totalXpForLevel(100_000)
        assertThat(LevelCurve.progress(xp).level).isEqualTo(100_000)
        assertThat(LevelCurve.progress(xp - 1).level).isEqualTo(99_999)
    }

    @Test
    fun `fraction is between 0 and 1`() {
        assertThat(LevelCurve.progress(150).fraction).isWithin(1e-6f).of(0.25f)
    }

    @Test
    fun `negative xp is rejected`() {
        assertThrows(IllegalArgumentException::class.java) { LevelCurve.progress(-1) }
    }
}
