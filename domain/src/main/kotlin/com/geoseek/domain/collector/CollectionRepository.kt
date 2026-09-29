package com.geoseek.domain.collector

import com.geoseek.domain.catalog.ObjectId
import kotlinx.coroutines.flow.Flow
import java.time.Instant

interface CollectionRepository {
    fun observeCards(): Flow<List<Card>>

    fun observeCard(id: ObjectId): Flow<Card?>

    /**
     * Atomically adds one copy of [id]. If this is the first acquisition ever, also adds
     * [xpIfFirst] to the local profile in the same transaction.
     */
    suspend fun acquire(
        id: ObjectId,
        xpIfFirst: Int,
        at: Instant,
    ): AcquisitionResult
}
