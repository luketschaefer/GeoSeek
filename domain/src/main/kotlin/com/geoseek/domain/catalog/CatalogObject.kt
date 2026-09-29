package com.geoseek.domain.catalog

@JvmInline
value class ObjectId(
    val value: String,
) {
    override fun toString(): String = value
}

/**
 * A findable real-world object. [acceptedLabels] are detector labels (ML Kit base model
 * labels today) that count as seeing this object when reported with at least [minConfidence].
 */
data class CatalogObject(
    val id: ObjectId,
    val name: String,
    val rarity: Rarity,
    val points: Int,
    val xp: Int,
    val environments: Set<Environment>,
    val acceptedLabels: Set<String>,
    val minConfidence: Float,
) {
    fun accepts(
        label: String,
        confidence: Float,
    ): Boolean = confidence >= minConfidence && acceptedLabels.any { it.equals(label, ignoreCase = true) }
}
