package com.example.geoseek.models

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class CardTypeTest {
    @Test
    fun strengthsAndWeaknessesFormABalancedCycle() {
        CardType.entries.forEach { type ->
            assertNotEquals(type, type.strongAgainst)
            assertNotEquals(type, type.weakAgainst)
            assertEquals(1, CardType.entries.count { it.strongAgainst == type })
            assertEquals(1, CardType.entries.count { it.weakAgainst == type })
        }
    }

    @Test
    fun damageMultiplierUsesStrengthWeaknessAndNeutralMatchups() {
        CardType.entries.forEach { type ->
            assertEquals(1.5, type.getDamageMultiplier(type.strongAgainst), 0.0)
            assertEquals(0.75, type.getDamageMultiplier(type.weakAgainst), 0.0)
            assertEquals(
                1.0,
                type.getDamageMultiplier(
                    CardType.entries.single {
                        it != type && it != type.strongAgainst && it != type.weakAgainst
                    },
                ),
                0.0,
            )
        }
    }

    @Test
    fun catalogObjectsAndCardsHaveDeterministicTypes() {
        val expectedTypes = mapOf(
            CardType.TECH to setOf("Laptop", "Mobile Phone", "AirPods", "Television", "Keyboard", "Mouse"),
            CardType.NATURE to setOf("Plant", "Flower", "Tree", "Dog", "Cat"),
            CardType.HOUSEHOLD to setOf("Chair", "Table", "Couch", "Bed", "Lamp"),
            CardType.MISC to setOf("Book", "Bottle", "Cup", "Backpack", "Shoe", "Clock", "Bicycle", "Car"),
        )
        assertEquals(
            expectedTypes.flatMap { (type, names) -> names.map { it to type } }.toMap(),
            OBJECT_LIST.associate { it.name to it.cardType },
        )
        val obj = OBJECT_LIST.single { it.name == "Chair" }
        assertEquals(CardType.HOUSEHOLD, Card(obj = obj, xpValue = 0).cardType)
        assertEquals(CardType.MISC, CardType.fromLabel("unrecognized object"))
        assertEquals(CardType.TECH, CardType.fromLabel(" Mobile-Phone "))
    }
}
