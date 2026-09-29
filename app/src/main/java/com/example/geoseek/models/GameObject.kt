// A findable real-world object, with its rarity and point value.
package com.example.geoseek.models

enum class Rarity { COMMON, UNCOMMON, RARE, EPIC, LEGENDARY }

data class GameObject(
    val name: String,
    val rarity: Rarity,
    val points: Int,
)
