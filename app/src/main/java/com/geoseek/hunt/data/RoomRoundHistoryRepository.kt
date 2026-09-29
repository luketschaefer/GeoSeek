package com.geoseek.hunt.data

import com.geoseek.core.database.GeoSeekDatabase
import com.geoseek.domain.history.RoundHistoryRepository
import com.geoseek.domain.history.RoundRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class RoomRoundHistoryRepository
    @Inject
    constructor(
        db: GeoSeekDatabase,
    ) : RoundHistoryRepository {
        private val dao = db.roundHistoryDao()

        override fun observeRecent(limit: Int): Flow<List<RoundRecord>> =
            dao.observeRecent(limit).map { list -> list.map { it.toDomain() } }

        override suspend fun get(id: Long): RoundRecord? = dao.get(id)?.toDomain()

        override suspend fun record(record: RoundRecord): Long = dao.insert(RoundRecordEntity.from(record.copy(id = 0)))
    }
