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
 * Maps raw ML Kit vision output labels to [GameObject]s using canonical names and expanded alias dictionaries.
 *
 * Integrates with **Google ML Kit Image Labeling (`com.google.mlkit.vision.label`)** output by providing
 * synonym/alias mapping to improve detection accuracy and prevent misclassifications (e.g. accepting flowerpot for plant).
 *
 * @param defaultConfidenceThreshold Default minimum confidence threshold for valid matches.
 */
class ObjectLabelMatcher(
    private val defaultConfidenceThreshold: Float = 0.50f,
) {
    private val aliasMap: Map<String, List<String>> = mapOf(
        "Chair" to listOf("chair", "armchair", "folding chair", "office chair", "seat", "deckchair", "rocking chair", "bench", "stool", "recliner", "swivel chair"),
        "Table" to listOf("table", "desk", "coffee table", "dining table", "nightstand", "countertop", "stand", "workbench"),
        "Couch" to listOf("couch", "sofa", "studio couch", "loveseat", "settee", "futon", "armchair"),
        "Bed" to listOf("bed", "bedstead", "mattress", "bunk bed", "duvet", "pillow"),
        "Lamp" to listOf("lamp", "lighting", "table lamp", "lantern", "lampshade", "floor lamp", "light fixture", "light"),
        "Plant" to listOf("plant", "houseplant", "potted plant", "flora", "botany", "succulent", "fern", "leaf", "flowerpot", "planter", "vase", "flower", "blossom", "petal", "greenery", "vegetation"),
        "Flower" to listOf("flower", "blossom", "rose", "tulip", "petal", "bouquet", "flowerpot", "plant", "potted plant", "houseplant", "flora"),
        "Tree" to listOf("tree", "woody plant", "shrub", "trunk", "conifer", "evergreen", "palm", "branch"),
        "Laptop" to listOf("laptop", "notebook computer", "laptop computer", "computer monitor", "netbook", "screen", "display", "macbook"),
        "Mobile Phone" to listOf("mobile phone", "cellphone", "cellular telephone", "smartphone", "telephone", "phone", "iphone", "android", "handset"),
        // Only the custom model emits these (docs/custom-model.md); its "Background" class deliberately matches nothing.
        "AirPods" to listOf("airpods", "airpod", "airpods case", "earbuds", "earbud"),
        "Television" to listOf("television", "tv", "flat panel display", "television set", "media player", "screen", "monitor"),
        "Keyboard" to listOf("keyboard", "computer keyboard", "typewriter", "keypad"),
        "Mouse" to listOf("mouse", "computer mouse", "trackpad"),
        "Book" to listOf("book", "publication", "novel", "textbook", "hardcover", "paperback", "magazine", "journal"),
        "Bottle" to listOf("bottle", "water bottle", "plastic bottle", "glass bottle", "flask", "thermos", "container"),
        "Cup" to listOf("cup", "mug", "coffee cup", "drinkware", "teacup", "beverageware", "glass", "tumbler"),
        "Backpack" to listOf("backpack", "bag", "rucksack", "knapsack", "handbag", "school bag", "satchel", "tote"),
        "Shoe" to listOf("shoe", "footwear", "sneakers", "boot", "athletic shoe", "sandal", "slipper"),
        "Clock" to listOf("clock", "wall clock", "alarm clock", "watch", "timepiece"),
        "Bicycle" to listOf("bicycle", "bike", "cycle", "mountain bike", "road bike"),
        "Car" to listOf("car", "automobile", "sedan", "motor vehicle", "suv", "truck"),
        "Dog" to listOf("dog", "canine", "puppy", "hound", "pooch"),
        "Cat" to listOf("cat", "feline", "kitten"),
    )

    /**
     * Searches raw ML Kit frame labels for a specific target [GameObject].
     *
     * Evaluates raw labels directly against the target's aliases first to prevent cross-category rejections.
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
        val aliases = aliasMap[target.name] ?: listOf(target.name.lowercase())
        val bestLabelMatch = labels
            .filter { it.confidence >= threshold }
            .filter { raw -> aliases.any { alias -> matchesAlias(raw.text, alias) } }
            .maxByOrNull { it.confidence }
            ?: return null
        return LabelMatch(target, bestLabelMatch.text, bestLabelMatch.confidence)
    }

    /**
     * Evaluates raw frame labels against the alias catalog and returns all matching targets sorted by confidence.
     * Several labels for the same object collapse into that object's best one.
     *
     * A label shared by several objects (ML Kit's "Flower" is an alias of both Flower and Plant) matches all of
     * them, so a pool without one still finds the other. Ties rank the object the label names exactly first.
     *
     * @param labels List of raw [RawLabel]s produced by the vision analyzer.
     * @param threshold Minimum required confidence score.
     * @return List of valid [LabelMatch]es ordered highest-confidence first.
     */
    fun findMatches(
        labels: List<RawLabel>,
        threshold: Float = defaultConfidenceThreshold,
    ): List<LabelMatch> {
        val catalogOrder = aliasMap.keys.toList()
        return labels
            .filter { it.confidence >= threshold }
            .flatMap { label ->
                matchLabelToTargets(label.text).map { LabelMatch(it, label.text, label.confidence) }
            }
            .groupBy { it.gameObject.name }
            .map { (_, sameObject) -> sameObject.maxBy { it.confidence } }
            .sortedWith(
                compareByDescending<LabelMatch> { it.confidence }
                    .thenByDescending { words(it.matchedLabel) == words(it.gameObject.name) }
                    .thenBy { catalogOrder.indexOf(it.gameObject.name) },
            )
    }

    /**
     * Resolves a single label string against the alias map to every [GameObject] it could mean. When one object's
     * alias covers more of the label than another's ("Glass bottle" is a bottle, not a glass), only the more
     * specific objects are kept.
     *
     * @param labelText Raw string from ML Kit labeler.
     * @return Corresponding [GameObject]s, or empty if unmapped.
     */
    private fun matchLabelToTargets(labelText: String): List<GameObject> {
        val bySpecificity = aliasMap.mapNotNull { (targetName, aliases) ->
            aliases.filter { matchesAlias(labelText, it) }
                .maxOfOrNull { words(it).size }
                ?.let { targetName to it }
        }
        val best = bySpecificity.maxOfOrNull { it.second } ?: return emptyList()
        return bySpecificity.filter { it.second == best }.map { (targetName, _) ->
            OBJECT_LIST.firstOrNull { it.name.equals(targetName, ignoreCase = true) }
                ?: GameObject(name = targetName, rarity = getRarityForTarget(targetName), points = getPointsForTarget(targetName))
        }
    }

    /**
     * Checks whether a label contains an alias as whole words, so "Coffee cup" matches "cup" but "Cupboard" doesn't.
     */
    private fun matchesAlias(label: String, alias: String): Boolean {
        val labelWords = words(label)
        val aliasWords = words(alias)
        if (labelWords.isEmpty() || aliasWords.isEmpty() || aliasWords.size > labelWords.size) return false
        return (0..labelWords.size - aliasWords.size).any { start ->
            aliasWords.indices.all { labelWords[start + it] == aliasWords[it] }
        }
    }

    private fun words(text: String): List<String> =
        text.lowercase().split(Regex("[^a-z0-9]+")).filter { it.isNotEmpty() }

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
