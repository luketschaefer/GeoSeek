package com.geoseek.hunt.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RoundHistoryDao {
    @Query("SELECT * FROM round_history ORDER BY startedAtEpochMs DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<RoundRecordEntity>>

    @Query("SELECT * FROM round_history WHERE id = :id")
    suspend fun get(id: Long): RoundRecordEntity?

    @Insert
    suspend fun insert(record: RoundRecordEntity): Long
}
