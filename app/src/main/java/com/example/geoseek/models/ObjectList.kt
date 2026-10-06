// Catalog of findable real-world objects grouped by rarity and point value.
package com.example.geoseek.models

val OBJECT_LIST = listOf(
    // Furniture & Living Space
    GameObject("Chair", Rarity.COMMON, 10),
    GameObject("Table", Rarity.COMMON, 10),
    GameObject("Couch", Rarity.UNCOMMON, 25),
    GameObject("Bed", Rarity.UNCOMMON, 25),
    GameObject("Lamp", Rarity.UNCOMMON, 20),

    // Plants & Nature
    GameObject("Plant", Rarity.COMMON, 15),
    GameObject("Flower", Rarity.UNCOMMON, 20),
    GameObject("Tree", Rarity.COMMON, 10),

    // Electronics & Workspace
    GameObject("Laptop", Rarity.UNCOMMON, 30),
    GameObject("Mobile Phone", Rarity.COMMON, 15),
    GameObject("Television", Rarity.RARE, 40),
    GameObject("Keyboard", Rarity.UNCOMMON, 25),
    GameObject("Mouse", Rarity.UNCOMMON, 20),

    // Everyday Items
    GameObject("Book", Rarity.COMMON, 10),
    GameObject("Bottle", Rarity.COMMON, 10),
    GameObject("Cup", Rarity.COMMON, 10),
    GameObject("Backpack", Rarity.UNCOMMON, 25),
    GameObject("Shoe", Rarity.COMMON, 15),
    GameObject("Clock", Rarity.RARE, 35),

    // Vehicles & Outdoor
    GameObject("Bicycle", Rarity.RARE, 50),
    GameObject("Car", Rarity.RARE, 45),

    // Pets & Animals
    GameObject("Dog", Rarity.EPIC, 75),
    GameObject("Cat", Rarity.EPIC, 75),
)
