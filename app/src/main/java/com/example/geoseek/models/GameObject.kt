// Domain model for a findable real-world object.
package com.example.geoseek.models

/**
 * Rarity tier categorization for findable objects.
 */
enum class Rarity { COMMON, UNCOMMON, RARE, EPIC, LEGENDARY }

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
 */
data class GameObject(
    val name: String,
    val rarity: Rarity,
    val points: Int,
)
