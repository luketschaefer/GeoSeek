package com.geoseek.hunt.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.geoseek.domain.catalog.Environment
import com.geoseek.domain.history.RoundRecord
import com.geoseek.domain.hunt.RoundStatus
import java.time.Instant
import kotlin.time.Duration.Companion.milliseconds

/** Enums are stored by name; renaming an enum constant therefore requires a migration. */
@Entity(tableName = "round_history")
data class RoundRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val environment: String,
    val seed: Long,
    val startedAtEpochMs: Long,
    val elapsedMs: Long,
    val status: String,
    val score: Int,
    val targetsFound: Int,
    val targetsTotal: Int,
) {
    fun toDomain() =
        RoundRecord(
            id = id,
            environment = Environment.valueOf(environment),
            seed = seed,
            startedAt = Instant.ofEpochMilli(startedAtEpochMs),
            elapsed = elapsedMs.milliseconds,
            status = RoundStatus.valueOf(status),
            score = score,
            targetsFound = targetsFound,
            targetsTotal = targetsTotal,
        )

    companion object {
        fun from(record: RoundRecord) =
            RoundRecordEntity(
                id = record.id,
                environment = record.environment.name,
                seed = record.seed,
                startedAtEpochMs = record.startedAt.toEpochMilli(),
                elapsedMs = record.elapsed.inWholeMilliseconds,
                status = record.status.name,
                score = record.score,
                targetsFound = record.targetsFound,
                targetsTotal = record.targetsTotal,
            )
    }
}
