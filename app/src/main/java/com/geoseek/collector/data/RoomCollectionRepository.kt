package com.geoseek.collector.data

import androidx.room.withTransaction
import com.geoseek.core.database.GeoSeekDatabase
import com.geoseek.domain.catalog.ObjectId
import com.geoseek.domain.collector.AcquisitionResult
import com.geoseek.domain.collector.Card
import com.geoseek.domain.collector.CollectionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject

class RoomCollectionRepository
    @Inject
    constructor(
        private val db: GeoSeekDatabase,
    ) : CollectionRepository {
        private val cards = db.cardDao()
        private val profile = db.profileDao()

        override fun observeCards(): Flow<List<Card>> = cards.observeAll().map { list -> list.map { it.toDomain() } }

        override fun observeCard(id: ObjectId): Flow<Card?> = cards.observe(id.value).map { it?.toDomain() }

        override suspend fun acquire(
            id: ObjectId,
            xpIfFirst: Int,
            at: Instant,
        ): AcquisitionResult =
            db.withTransaction {
                val existing = cards.get(id.value)
                val updated =
                    existing?.copy(count = existing.count + 1)
                        ?: CardEntity(objectId = id.value, firstAcquiredAtEpochMs = at.toEpochMilli(), count = 1)
                cards.upsert(updated)
                val first = existing == null
                if (first) {
                    profile.ensureExists()
                    profile.addXp(xpIfFirst.toLong())
                }
                AcquisitionResult(
                    card = updated.toDomain(),
                    isFirstAcquisition = first,
                    xpAwarded = if (first) xpIfFirst else 0,
                )
            }
    }
