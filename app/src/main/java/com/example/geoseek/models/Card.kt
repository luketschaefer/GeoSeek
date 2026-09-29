// A trading card the player earns when they find an object; gives XP in Collector mode.
package com.example.geoseek.models

data class Card(
    val obj: GameObject,
    val xp: Int,
    // TODO: Add when it was found, image, etc.
)
