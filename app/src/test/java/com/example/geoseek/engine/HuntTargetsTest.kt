package com.example.geoseek.engine

import com.example.geoseek.models.GameObject
import com.example.geoseek.models.Habitat
import com.example.geoseek.models.OBJECT_LIST
import com.example.geoseek.models.Rarity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class HuntTargetsTest {

    private val couch = GameObject("Couch", Rarity.UNCOMMON, 25, setOf(Habitat.INDOORS))
    private val tree = GameObject("Tree", Rarity.COMMON, 10, setOf(Habitat.PARK, Habitat.STREETS))
    private val car = GameObject("Car", Rarity.RARE, 45, setOf(Habitat.STREETS))
    private val airPods = GameObject("AirPods", Rarity.LEGENDARY, 100, needsCustomModel = true)
    private val catalog = listOf(couch, tree, car, airPods)

    @Test
    fun huntPool_onlyIncludesObjectsFromThatHabitat() {
        assertEquals(listOf(couch), huntPool(Habitat.INDOORS, customModelAvailable = false, catalog))
        assertEquals(listOf(tree), huntPool(Habitat.PARK, customModelAvailable = false, catalog))
        assertEquals(listOf(tree, car), huntPool(Habitat.STREETS, customModelAvailable = false, catalog))
    }

    @Test
    fun huntPool_leavesOutCustomModelObjectsWithoutTheModel() {
        Habitat.entries.forEach { habitat ->
            assertFalse(airPods in huntPool(habitat, customModelAvailable = false, catalog))
            assertTrue(airPods in huntPool(habitat, customModelAvailable = true, catalog))
        }
    }

    @Test
    fun pickHuntTargets_returnsDistinctTargetsFromThePool() {
        repeat(50) { seed ->
            val targets = pickHuntTargets(Habitat.STREETS, 5, customModelAvailable = false, OBJECT_LIST, Random(seed))
            assertEquals(5, targets.size)
            assertEquals(targets.size, targets.toSet().size)
            assertTrue(targets.all { Habitat.STREETS in it.habitats && !it.needsCustomModel })
        }
    }

    @Test
    fun pickHuntTargets_returnsWholePoolWhenAskedForMore() {
        val targets = pickHuntTargets(Habitat.STREETS, 10, customModelAvailable = false, catalog, Random(1))
        assertEquals(setOf(tree, car), targets.toSet())
    }

    @Test
    fun pickHuntTargets_negativeCountGivesNoTargets() {
        assertTrue(pickHuntTargets(Habitat.INDOORS, -1, customModelAvailable = true, catalog).isEmpty())
    }

    @Test
    fun catalog_everyHabitatHasEnoughObjectsForTheLargestHunt() {
        // The Streets hunt asks for 5 targets, the most of any environment.
        Habitat.entries.forEach { habitat ->
            assertTrue(
                "$habitat has too few objects",
                huntPool(habitat, customModelAvailable = false).size >= 5,
            )
        }
    }

    @Test
    fun catalog_onlyAirPodsNeedTheCustomModel() {
        assertEquals(listOf("AirPods"), OBJECT_LIST.filter { it.needsCustomModel }.map { it.name })
    }
}
