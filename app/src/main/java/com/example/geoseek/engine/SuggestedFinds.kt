// Suggests which objects a player should look for next, based on what's missing from their binder.
package com.example.geoseek.engine

import com.example.geoseek.models.GameObject
import com.example.geoseek.models.Habitat
import com.example.geoseek.models.OBJECT_LIST

/** How many suggestions the home screen shows. */
const val SUGGESTED_FIND_COUNT = 3

/**
 * Up to [count] objects the player has no card for yet, easiest first: lowest rarity, then fewest
 * points, then catalog order. Objects that need the custom model are left out when the app doesn't
 * ship one (the stock labeler could never recognize them). Empty once the binder is complete.
 */
fun suggestedFinds(
    cards: Set<String>,
    customModelAvailable: Boolean,
    count: Int = SUGGESTED_FIND_COUNT,
    catalog: List<GameObject> = OBJECT_LIST,
): List<GameObject> = catalog
    .filter { it.name !in cards && (customModelAvailable || !it.needsCustomModel) }
    .sortedWith(compareBy({ it.rarity }, { it.points }))
    .take(count.coerceAtLeast(0))

/** Where to look for [obj], for the suggestion row: "Anywhere", or its habitats like "Indoors, Streets". */
fun whereToLook(obj: GameObject): String =
    if (obj.habitats.containsAll(Habitat.entries)) {
        "Anywhere"
    } else {
        Habitat.entries.filter { it in obj.habitats }
            .joinToString(", ") { it.name.lowercase().replaceFirstChar(Char::uppercase) }
    }
