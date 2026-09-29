package com.geoseek.domain.hunt

import com.geoseek.domain.catalog.Environment
import com.geoseek.domain.catalog.Rarity
import com.geoseek.domain.testCatalog
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test

class RoundGeneratorTest {
    private val generator = RoundGenerator(testCatalog)

    @Test
    fun `same seed gives same targets in same order`() {
        val config = RoundConfig(Environment.PARK, seed = 1234L)
        assertThat(generator.generate(config).targets).isEqualTo(generator.generate(config).targets)
    }

    @Test
    fun `golden output locks the algorithm`() {
        // If this fails, a change altered which targets a seed produces. That is a
        // player-visible change (shared seeds, replays): update deliberately.
        val ids =
            generator
                .generate(
                    RoundConfig(Environment.PARK, seed = 42L, targetCount = 3),
                ).targets
                .map { it.id.value }
        assertThat(ids).containsExactly(*GOLDEN_SEED_42).inOrder()
    }

    @Test
    fun `targets are distinct and from the environment`() {
        repeat(200) { seed ->
            val round = generator.generate(RoundConfig(Environment.KITCHEN, seed = seed.toLong(), targetCount = 6))
            assertThat(round.targets.map { it.id }).containsNoDuplicates()
            assertThat(round.targets.all { Environment.KITCHEN in it.environments }).isTrue()
        }
    }

    @Test
    fun `new round is not started`() {
        val round = generator.generate(RoundConfig(Environment.STREET, seed = 7L))
        assertThat(round.status).isEqualTo(RoundStatus.NOT_STARTED)
        assertThat(round.targets).hasSize(RoundConfig.DEFAULT_TARGET_COUNT)
    }

    @Test
    fun `pool smaller than target count throws`() {
        assertThrows(IllegalArgumentException::class.java) {
            generator.generate(RoundConfig(Environment.PARK, seed = 1L, targetCount = 7))
        }
    }

    @Test
    fun `common objects are picked far more often than legendary`() {
        val counts =
            (0 until 2000)
                .flatMap { seed ->
                    generator.generate(RoundConfig(Environment.PARK, seed = seed.toLong(), targetCount = 1)).targets
                }.groupingBy { it.rarity }
                .eachCount()
        // Weights: two commons (50 each) vs one legendary (3) → expected ratio ~33x.
        assertThat(counts.getValue(Rarity.COMMON)).isGreaterThan(counts.getValue(Rarity.LEGENDARY) * 10)
    }

    @Test
    fun `missing rarity weight is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            RoundGenerator(testCatalog, mapOf(Rarity.COMMON to 1))
        }
    }

    private companion object {
        val GOLDEN_SEED_42 = arrayOf("park_1", "park_extra", "park_0")
    }
}
