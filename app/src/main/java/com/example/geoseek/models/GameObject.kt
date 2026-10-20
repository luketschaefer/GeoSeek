// Domain model for a findable real-world object.
package com.example.geoseek.models

/**
 * Rarity tier categorization for findable objects.
 */
enum class Rarity { COMMON, UNCOMMON, RARE, EPIC, LEGENDARY }

/**
 * The kinds of places a hunt can take place in. Each [GameObject] lists the habitats it's
 * realistically found in, so a hunt only asks for objects the player can find there.
 */
enum class Habitat { INDOORS, PARK, STREETS }

/**
 * Represents a findable real-world object in GeoSeek.
 *
 * Integrates with:
 * - **Vision Detector ([com.example.geoseek.detection.ObjectDetector])**: Matched against live vision labels.
 * - **Game Engine ([com.example.geoseek.engine.GameMode])**: Determines base point scoring and card rewards.
 *
 * @property name Display name of the object (e.g. "Chair", "Plant", "Laptop").
 * @property rarity Rarity tier classification influencing score multipliers.
 * @property points Base point value awarded when found.
 * @property habitats Where the object is realistically found; hunts in other habitats never ask for it.
 * @property needsCustomModel True if only the bundled custom classifier can recognize it (see docs/custom-model.md).
 */
data class GameObject(
    val name: String,
    val rarity: Rarity,
    val points: Int,
    val habitats: Set<Habitat> = Habitat.entries.toSet(),
    val needsCustomModel: Boolean = false,
)
