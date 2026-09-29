package com.geoseek.domain.history

import kotlinx.coroutines.flow.Flow

interface RoundHistoryRepository {
    fun observeRecent(limit: Int): Flow<List<RoundRecord>>

    suspend fun get(id: Long): RoundRecord?

    /** Stores a finished round and returns its id. */
    suspend fun record(record: RoundRecord): Long
}
