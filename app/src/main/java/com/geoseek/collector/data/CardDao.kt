package com.geoseek.collector.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface CardDao {
    @Query("SELECT * FROM cards ORDER BY firstAcquiredAtEpochMs DESC")
    fun observeAll(): Flow<List<CardEntity>>

    @Query("SELECT * FROM cards WHERE objectId = :objectId")
    fun observe(objectId: String): Flow<CardEntity?>

    @Query("SELECT * FROM cards WHERE objectId = :objectId")
    suspend fun get(objectId: String): CardEntity?

    @Upsert
    suspend fun upsert(card: CardEntity)
}
