// Chooses which objects a hunt asks for, based on where the player is hunting.
package com.example.geoseek.engine

import com.example.geoseek.models.GameObject
import com.example.geoseek.models.Habitat
import com.example.geoseek.models.OBJECT_LIST
import kotlin.random.Random

/**
 * Objects a hunt in [habitat] may ask for: those found there, minus any that need the custom
 * model when the app doesn't ship one (the stock labeler could never recognize them).
 */
fun huntPool(
    habitat: Habitat,
    customModelAvailable: Boolean,
    catalog: List<GameObject> = OBJECT_LIST,
): List<GameObject> = catalog.filter { habitat in it.habitats && (customModelAvailable || !it.needsCustomModel) }

/**
 * Picks up to [count] distinct targets for a hunt in [habitat]. Returns fewer if the habitat
 * doesn't have enough eligible objects.
 */
fun pickHuntTargets(
    habitat: Habitat,
    count: Int,
    customModelAvailable: Boolean,
    catalog: List<GameObject> = OBJECT_LIST,
    random: Random = Random.Default,
): List<GameObject> = huntPool(habitat, customModelAvailable, catalog).shuffled(random).take(count.coerceAtLeast(0))
