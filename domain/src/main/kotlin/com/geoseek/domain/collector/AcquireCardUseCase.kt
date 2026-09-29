package com.geoseek.domain.collector

import com.geoseek.domain.catalog.Catalog
import com.geoseek.domain.catalog.ObjectId
import java.time.Clock

class AcquireCardUseCase(
    private val catalog: Catalog,
    private val collection: CollectionRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(id: ObjectId): AcquisitionResult {
        val obj = catalog.require(id)
        return collection.acquire(id = id, xpIfFirst = obj.xp, at = clock.instant())
    }
}
