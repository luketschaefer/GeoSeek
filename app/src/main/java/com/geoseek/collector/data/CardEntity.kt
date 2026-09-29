package com.geoseek.collector.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.geoseek.domain.catalog.ObjectId
import com.geoseek.domain.collector.Card
import java.time.Instant

@Entity(tableName = "cards")
data class CardEntity(
    @PrimaryKey val objectId: String,
    val firstAcquiredAtEpochMs: Long,
    val count: Int,
) {
    fun toDomain() = Card(ObjectId(objectId), Instant.ofEpochMilli(firstAcquiredAtEpochMs), count)
}
