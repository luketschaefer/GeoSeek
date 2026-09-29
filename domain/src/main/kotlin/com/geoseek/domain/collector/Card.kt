package com.geoseek.domain.collector

import com.geoseek.domain.catalog.ObjectId
import java.time.Instant

/**
 * A collected card. [firstAcquiredAt] is never cleared, even if [count] later drops to 0 through
 * trading, so first-acquisition XP can only ever be awarded once per object.
 */
data class Card(
    val objectId: ObjectId,
    val firstAcquiredAt: Instant,
    val count: Int,
)

data class AcquisitionResult(
    val card: Card,
    val isFirstAcquisition: Boolean,
    val xpAwarded: Int,
)
