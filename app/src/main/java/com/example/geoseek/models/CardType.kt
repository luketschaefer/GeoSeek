package com.example.geoseek.models

/**
 * Gameplay categories for collectible cards, with a balanced four-type strength cycle.
 */
enum class CardType(val displayLabel: String) {
    TECH("Tech"),
    NATURE("Nature"),
    HOUSEHOLD("Household"),
    MISC("Misc"),
    ;

    val strongAgainst: CardType
        get() = when (this) {
            TECH -> MISC
            NATURE -> TECH
            HOUSEHOLD -> NATURE
            MISC -> HOUSEHOLD
        }

    val weakAgainst: CardType
        get() = when (this) {
            TECH -> NATURE
            NATURE -> HOUSEHOLD
            HOUSEHOLD -> MISC
            MISC -> TECH
        }

    fun getDamageMultiplier(defenderType: CardType): Double = when (defenderType) {
        strongAgainst -> 1.5
        weakAgainst -> 0.75
        else -> 1.0
    }

    companion object {
        private val techLabels = setOf(
            "laptop", "mobile phone", "airpods", "television", "keyboard", "mouse",
            "phone", "smartphone", "cellphone", "tv", "earbuds",
        ).mapTo(mutableSetOf(), ::normalize)
        private val natureLabels = setOf(
            "plant", "flower", "tree", "dog", "cat", "houseplant", "rose", "puppy", "kitten",
        ).mapTo(mutableSetOf(), ::normalize)
        private val householdLabels = setOf(
            "chair", "table", "couch", "bed", "lamp", "sofa", "desk", "furniture",
        ).mapTo(mutableSetOf(), ::normalize)
        private val miscLabels = setOf(
            "book", "bottle", "cup", "backpack", "shoe", "clock", "bicycle", "car",
            "bike", "automobile", "bag", "mug", "watch",
        ).mapTo(mutableSetOf(), ::normalize)

        /** Maps canonical object names and common label variants; unknown labels default to [MISC]. */
        fun fromLabel(label: String): CardType {
            val normalized = normalize(label)
            return when {
                normalized in techLabels -> TECH
                normalized in natureLabels -> NATURE
                normalized in householdLabels -> HOUSEHOLD
                normalized in miscLabels -> MISC
                else -> MISC
            }
        }

        private fun normalize(label: String): String =
            label.lowercase().filter(Char::isLetterOrDigit)
    }
}
