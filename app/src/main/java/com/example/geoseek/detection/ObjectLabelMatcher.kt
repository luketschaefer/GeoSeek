package com.example.geoseek.detection

import com.example.geoseek.models.GameObject
import com.example.geoseek.models.OBJECT_LIST
import com.example.geoseek.models.Rarity

/**
 * Represents a successfully matched label from ML Kit image analysis mapped to a target object.
 *
 * @property gameObject The target [GameObject] identified from the catalog.
 * @property matchedLabel The raw label string output by the ML Kit vision model.
 * @property confidence Confidence score (0.0f to 1.0f) reported by ML Kit.
 */
data class LabelMatch(
    val gameObject: GameObject,
    val matchedLabel: String,
    val confidence: Float,
)

/**
 * Maps raw ML Kit vision output labels to [GameObject]s using canonical names and alias dictionaries.
 *
 * Integrates with **Google ML Kit Image Labeling (`com.google.mlkit.vision.label`)** output by providing
 * synonym/alias mapping (e.g. mapping ML Kit's `"Houseplant"` to `Plant`, `"Notebook computer"` to `Laptop`,
 * `"Mug"` to `Cup`, `"Sofa"` to `Couch`, `"Smartphone"` to `Mobile Phone`) to improve detection accuracy.
 *
 * @param defaultConfidenceThreshold Default minimum confidence threshold for valid matches.
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
     * Searches raw ML Kit frame labels for a specific target [GameObject].
     *
     * @param target Desired target object to look for.
     * @param labels List of raw [RawLabel]s produced by the vision analyzer.
     * @param threshold Minimum required confidence score.
     * @return [LabelMatch] if found with sufficient confidence, or null otherwise.
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
     * Evaluates raw frame labels against the alias catalog and returns all matching targets sorted by confidence.
     *
     * @param labels List of raw [RawLabel]s produced by the vision analyzer.
     * @param threshold Minimum required confidence score.
     * @return List of valid [LabelMatch]es ordered highest-confidence first.
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
     * Resolves a single label string against the alias map to find its corresponding [GameObject].
     *
     * @param labelText Raw string from ML Kit labeler.
     * @return Corresponding [GameObject] if recognized, or null if unmapped.
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

    /**
     * Queries the catalog for the rarity tier of a target name.
     */
    private fun getRarityForTarget(targetName: String): Rarity {
        return OBJECT_LIST
            .firstOrNull { it.name.equals(targetName, ignoreCase = true) }?.rarity
            ?: Rarity.COMMON
    }

    /**
     * Queries the catalog for the base point value of a target name.
     */
    private fun getPointsForTarget(targetName: String): Int {
        return OBJECT_LIST
            .firstOrNull { it.name.equals(targetName, ignoreCase = true) }?.points
            ?: 10
    }
}

/**
 * Lightweight representation of a vision label text and confidence value.
 *
 * Decouples ML Kit model classes (`com.google.mlkit.vision.label.ImageLabel`) from domain models.
 *
 * @property text The label descriptor string.
 * @property confidence Confidence rating between 0.0f and 1.0f.
 */
data class RawLabel(
    val text: String,
    val confidence: Float,
)
