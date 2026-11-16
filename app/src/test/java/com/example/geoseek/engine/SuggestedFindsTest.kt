package com.example.geoseek.engine

import com.example.geoseek.models.GameObject
import com.example.geoseek.models.Habitat
import com.example.geoseek.models.OBJECT_LIST
import com.example.geoseek.models.Rarity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SuggestedFindsTest {

    private val couch = GameObject("Couch", Rarity.UNCOMMON, 25, setOf(Habitat.INDOORS))
    private val tree = GameObject("Tree", Rarity.COMMON, 10, setOf(Habitat.PARK, Habitat.STREETS))
    private val plant = GameObject("Plant", Rarity.COMMON, 15)
    private val chair = GameObject("Chair", Rarity.COMMON, 10)
    private val car = GameObject("Car", Rarity.RARE, 45, setOf(Habitat.STREETS))
    private val airPods = GameObject("AirPods", Rarity.LEGENDARY, 100, needsCustomModel = true)
    private val catalog = listOf(couch, tree, plant, chair, car, airPods)

    @Test
    fun suggestedFinds_areEasiestFirst() {
        val suggested = suggestedFinds(emptySet(), customModelAvailable = true, count = 6, catalog = catalog)

        assertEquals(listOf(tree, chair, plant, couch, car, airPods), suggested)
    }

    @Test
    fun suggestedFinds_tiesKeepCatalogOrder() {
        val suggested = suggestedFinds(emptySet(), customModelAvailable = false, count = 2, catalog = catalog)

        assertEquals(listOf(tree, chair), suggested)
    }

    @Test
    fun suggestedFinds_leaveOutCardsThePlayerAlreadyHas() {
        val suggested = suggestedFinds(setOf("Tree", "Chair"), customModelAvailable = false, count = 2, catalog = catalog)

        assertEquals(listOf(plant, couch), suggested)
    }

    @Test
    fun suggestedFinds_leaveOutCustomModelObjectsWithoutTheModel() {
        val owned = setOf("Couch", "Tree", "Plant", "Chair", "Car")

        assertTrue(suggestedFinds(owned, customModelAvailable = false, catalog = catalog).isEmpty())
        assertEquals(listOf(airPods), suggestedFinds(owned, customModelAvailable = true, catalog = catalog))
    }

    @Test
    fun suggestedFinds_areEmptyOnceTheBinderIsComplete() {
        val everything = catalog.map { it.name }.toSet()

        assertTrue(suggestedFinds(everything, customModelAvailable = true, catalog = catalog).isEmpty())
    }

    @Test
    fun suggestedFinds_neverReturnMoreThanAsked() {
        assertEquals(1, suggestedFinds(emptySet(), customModelAvailable = true, count = 1, catalog = catalog).size)
        assertTrue(suggestedFinds(emptySet(), customModelAvailable = true, count = 0, catalog = catalog).isEmpty())
        assertTrue(suggestedFinds(emptySet(), customModelAvailable = true, count = -2, catalog = catalog).isEmpty())
        assertEquals(6, suggestedFinds(emptySet(), customModelAvailable = true, count = 50, catalog = catalog).size)
    }

    @Test
    fun suggestedFinds_onTheRealCatalogGiveThreeUnownedObjects() {
        val owned = setOf("Chair", "Table")
        val suggested = suggestedFinds(owned, customModelAvailable = false)

        assertEquals(SUGGESTED_FIND_COUNT, suggested.size)
        assertEquals(suggested.distinct(), suggested)
        suggested.forEach { obj ->
            assertFalse(obj.name, obj.name in owned)
            assertFalse(obj.name, obj.needsCustomModel)
            assertTrue(obj.name, obj in OBJECT_LIST)
        }
    }

    @Test
    fun whereToLook_namesTheHabitatsInOrder() {
        assertEquals("Indoors", whereToLook(couch))
        assertEquals("Park, Streets", whereToLook(tree))
        assertEquals("Indoors, Streets", whereToLook(GameObject("Cup", Rarity.COMMON, 10, setOf(Habitat.STREETS, Habitat.INDOORS))))
    }

    @Test
    fun whereToLook_saysAnywhereForObjectsFoundEverywhere() {
        assertEquals("Anywhere", whereToLook(chair))
    }
}
