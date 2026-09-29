package com.geoseek.domain.hunt

import com.geoseek.domain.catalog.Catalog
import com.geoseek.domain.catalog.CatalogObject
import com.geoseek.domain.catalog.Rarity
import com.geoseek.domain.util.SeededRandom

/**
 * Picks distinct targets for a round from the environment's pool, weighted by rarity.
 * The same catalog + config (including seed) always yields the same targets in the same order.
 */
class RoundGenerator(
    private val catalog: Catalog,
    private val rarityWeights: Map<Rarity, Int> = DEFAULT_RARITY_WEIGHTS,
) {
    init {
        require(Rarity.entries.all { (rarityWeights[it] ?: 0) > 0 }) { "Every rarity needs a positive weight" }
    }

    fun generate(config: RoundConfig): Round {
        val pool = catalog.objectsIn(config.environment).toMutableList()
        require(pool.size >= config.targetCount) {
            "Environment ${config.environment} has ${pool.size} objects; round needs ${config.targetCount}"
        }
        val random = SeededRandom(config.seed)
        val targets = List(config.targetCount) { pool.removeAt(pickIndex(pool, random)) }
        return Round(config = config, targets = targets)
    }

    private fun pickIndex(
        pool: List<CatalogObject>,
        random: SeededRandom,
    ): Int {
        val total = pool.sumOf { weight(it) }
        var roll = random.nextInt(total)
        pool.forEachIndexed { index, obj ->
            roll -= weight(obj)
            if (roll < 0) return index
        }
        error("Unreachable: roll exceeded total weight")
    }

    private fun weight(obj: CatalogObject): Int = rarityWeights.getValue(obj.rarity)

    companion object {
        val DEFAULT_RARITY_WEIGHTS: Map<Rarity, Int> =
            mapOf(
                Rarity.COMMON to 50,
                Rarity.UNCOMMON to 25,
                Rarity.RARE to 15,
                Rarity.EPIC to 7,
                Rarity.LEGENDARY to 3,
            )
    }
}
