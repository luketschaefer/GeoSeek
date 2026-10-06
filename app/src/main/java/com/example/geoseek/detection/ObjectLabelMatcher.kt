package com.example.geoseek.detection

import com.example.geoseek.models.GameObject
import com.example.geoseek.models.OBJECT_LIST
import com.example.geoseek.models.Rarity

data class LabelMatch(
    val gameObject: GameObject,
    val matchedLabel: String,
    val confidence: Float,
)

/**
 * Maps ML Kit vision labels to target [GameObject]s using canonical names,
 * alias dictionaries, and confidence thresholding.
 */
class ObjectLabelMatcher(
    private val defaultConfidenceThreshold: Float = 0.60f,
) {
    private val aliasMap: Map<String, List<String>> = mapOf(
        "Chair" to listOf("chair", "armchair", "folding chair", "office chair", "seat", "deckchair", "rocking chair"),
        "Table" to listOf("table", "desk", "coffee table", "dining table", "nightstand", "countertop"),
        "Couch" to listOf("couch", "sofa", "studio couch", "loveseat", "settee", "futon"),
        "Bed" to listOf("bed", "bedstead", "mattress", "bunk bed"),
        "Lamp" to listOf("lamp", "lighting", "table lamp", "lantern", "lampshade", "floor lamp"),
        "Plant" to listOf("plant", "houseplant", "potted plant", "flora", "botany", "succulent", "fern", "leaf"),
        "Flower" to listOf("flower", "blossom", "rose", "tulip", "petal", "bouquet"),
        "Tree" to listOf("tree", "woody plant", "shrub", "trunk", "conifer"),
        "Laptop" to listOf("laptop", "notebook computer", "laptop computer", "computer monitor", "personal computer", "netbook"),
        "Mobile Phone" to listOf("mobile phone", "cellphone", "cellular telephone", "smartphone", "telephone", "phone"),
        "Television" to listOf("television", "tv", "flat panel display", "television set", "media player"),
        "Keyboard" to listOf("keyboard", "computer keyboard", "typewriter"),
        "Mouse" to listOf("mouse", "computer mouse", "trackpad"),
        "Book" to listOf("book", "publication", "novel", "textbook", "hardcover"),
        "Bottle" to listOf("bottle", "water bottle", "plastic bottle", "glass bottle", "flask"),
        "Cup" to listOf("cup", "mug", "coffee cup", "drinkware", "teacup", "beverageware"),
        "Backpack" to listOf("backpack", "bag", "rucksack", "knapsack", "handbag", "school bag"),
        "Shoe" to listOf("shoe", "footwear", "sneakers", "boot", "athletic shoe", "sandal"),
        "Clock" to listOf("clock", "wall clock", "alarm clock", "watch"),
        "Bicycle" to listOf("bicycle", "bike", "cycle", "mountain bike"),
        "Car" to listOf("car", "automobile", "vehicle", "sedan", "motor vehicle"),
        "Dog" to listOf("dog", "canine", "puppy", "hound"),
        "Cat" to listOf("cat", "feline", "kitten"),
    )

    /**
     * Checks if any label in the frame matches the specified [target] object
     * with sufficient confidence.
     */
    fun findTargetMatch(
        target: GameObject,
        labels: List<RawLabel>,
        threshold: Float = defaultConfidenceThreshold,
    ): LabelMatch? {
        val matches = findMatches(labels, threshold)
        return matches.firstOrNull { (gameObject, _, _) ->
            gameObject.name.equals(target.name, ignoreCase = true)
        }
    }

    /**
     * Evaluates all frame labels against known targets and returns all valid matches
     * above the confidence threshold, sorted by confidence descending.
     */
    fun findMatches(
        labels: List<RawLabel>,
        threshold: Float = defaultConfidenceThreshold,
    ): List<LabelMatch> {
        return labels
            .filter { it.confidence >= threshold }
            .mapNotNull { label ->
                val matchedTarget = matchLabelToTarget(label.text)
                matchedTarget?.let { LabelMatch(it, label.text, label.confidence) }
            }
            .sortedByDescending { it.confidence }
    }

    /**
     * Maps a single label string to its target [GameObject], or null if no match exists.
     */
    private fun matchLabelToTarget(labelText: String): GameObject? {
        val normalized = labelText.trim().lowercase()
        for ((targetName, aliases) in aliasMap) {
            if (aliases.any { normalized.contains(it) || it.contains(normalized) }) {
                return GameObject(
                    name = targetName,
                    rarity = getRarityForTarget(targetName),
                    points = getPointsForTarget(targetName),
                )
            }
        }
        return null
    }

    private fun getRarityForTarget(targetName: String): Rarity {
        return OBJECT_LIST
            .firstOrNull { it.name.equals(targetName, ignoreCase = true) }?.rarity
            ?: Rarity.COMMON
    }

    private fun getPointsForTarget(targetName: String): Int {
        return OBJECT_LIST
            .firstOrNull { it.name.equals(targetName, ignoreCase = true) }?.points
            ?: 10
    }
}

/** Simple representation of label output from vision analyzers. */
data class RawLabel(
    val text: String,
    val confidence: Float,
)
