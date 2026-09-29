package com.geoseek.domain.history

import com.geoseek.domain.catalog.Environment
import com.geoseek.domain.hunt.RoundStatus
import java.time.Instant
import kotlin.time.Duration

/** A finished round as stored in history. [id] is 0 until persisted. */
data class RoundRecord(
    val id: Long = 0,
    val environment: Environment,
    val seed: Long,
    val startedAt: Instant,
    val elapsed: Duration,
    val status: RoundStatus,
    val score: Int,
    val targetsFound: Int,
    val targetsTotal: Int,
)
