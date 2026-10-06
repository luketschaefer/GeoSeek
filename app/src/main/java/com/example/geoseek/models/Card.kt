// Collectible trading card model earned during object discovery.
package com.example.geoseek.models

import java.util.UUID

/**
 * Represents a collectible trading card earned when finding a real-world object.
 *
 * Integrates with:
 * - **Collection Mode ([com.example.geoseek.engine.modes.CollectionMode])**: Populates the player's card binder.
 * - **XP System ([com.example.geoseek.managers.XPManager])**: Carries experience points ([xpValue]) awarded upon discovery.
 *
 * @property id Unique identifier for the card instance.
 * @property obj Associated [GameObject] represented by this card.
 * @property xpValue Experience points granted when this card is collected.
 * @property dateCollectedMillis Epoch timestamp in milliseconds when the card was collected.
 * @property isNewDiscovery True if this card represents a first-time discovery for the player's binder.
 */
data class Card(
    val id: String = UUID.randomUUID().toString(),
    val obj: GameObject,
    val xpValue: Int,
    val dateCollectedMillis: Long = System.currentTimeMillis(),
    val isNewDiscovery: Boolean = true,
)
