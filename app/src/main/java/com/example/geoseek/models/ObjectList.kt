// Catalog of findable real-world objects grouped by rarity and point value.
package com.example.geoseek.models

import com.example.geoseek.models.Habitat.INDOORS
import com.example.geoseek.models.Habitat.PARK
import com.example.geoseek.models.Habitat.STREETS

/**
 * Global catalog list of findable [GameObject] targets in GeoSeek.
 *
 * Integrates with:
 * - **Vision Analysis ([com.example.geoseek.detection.ObjectLabelMatcher])**: Provides targets and alias lookup pools.
 * - **UI ([com.example.geoseek.screens.HuntScreen])**: Populates target selection chips.
 * - **Hunt Targets ([com.example.geoseek.engine.pickHuntTargets])**: Filtered by [Habitat] when a hunt starts.
 */
val OBJECT_LIST = listOf(
    // Furniture & Living Space ("Chair" and "Table" also match benches and picnic tables outside)
    GameObject("Chair", Rarity.COMMON, 10, setOf(INDOORS, PARK, STREETS)),
    GameObject("Table", Rarity.COMMON, 10, setOf(INDOORS, PARK, STREETS)),
    GameObject("Couch", Rarity.UNCOMMON, 25, setOf(INDOORS)),
    GameObject("Bed", Rarity.UNCOMMON, 25, setOf(INDOORS)),
    GameObject("Lamp", Rarity.UNCOMMON, 20, setOf(INDOORS, STREETS)),

    // Plants & Nature
    GameObject("Plant", Rarity.COMMON, 15, setOf(INDOORS, PARK, STREETS)),
    GameObject("Flower", Rarity.UNCOMMON, 20, setOf(INDOORS, PARK, STREETS)),
    GameObject("Tree", Rarity.COMMON, 10, setOf(PARK, STREETS)),

    // Electronics & Workspace
    GameObject("Laptop", Rarity.UNCOMMON, 30, setOf(INDOORS)),
    GameObject("Mobile Phone", Rarity.COMMON, 15, setOf(INDOORS, PARK, STREETS)),
    // Our detection benchmark: needs the custom model (see docs/custom-model.md), with or without the case.
    GameObject("AirPods", Rarity.LEGENDARY, 100, setOf(INDOORS, PARK, STREETS), needsCustomModel = true),
    GameObject("Television", Rarity.RARE, 40, setOf(INDOORS)),
    GameObject("Keyboard", Rarity.UNCOMMON, 25, setOf(INDOORS)),
    GameObject("Mouse", Rarity.UNCOMMON, 20, setOf(INDOORS)),

    // Everyday Items
    GameObject("Book", Rarity.COMMON, 10, setOf(INDOORS, PARK)),
    GameObject("Bottle", Rarity.COMMON, 10, setOf(INDOORS, PARK, STREETS)),
    GameObject("Cup", Rarity.COMMON, 10, setOf(INDOORS, STREETS)),
    GameObject("Backpack", Rarity.UNCOMMON, 25, setOf(INDOORS, PARK, STREETS)),
    GameObject("Shoe", Rarity.COMMON, 15, setOf(INDOORS, PARK, STREETS)),
    GameObject("Clock", Rarity.RARE, 35, setOf(INDOORS, STREETS)),

    // Vehicles & Outdoor
    GameObject("Bicycle", Rarity.RARE, 50, setOf(PARK, STREETS)),
    GameObject("Car", Rarity.RARE, 45, setOf(STREETS)),

    // Pets & Animals
    GameObject("Dog", Rarity.EPIC, 75, setOf(INDOORS, PARK, STREETS)),
    GameObject("Cat", Rarity.EPIC, 75, setOf(INDOORS, STREETS)),
)
