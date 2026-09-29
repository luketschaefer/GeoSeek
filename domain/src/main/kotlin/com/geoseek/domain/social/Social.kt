package com.geoseek.domain.social

import com.geoseek.domain.catalog.ObjectId
import java.time.Instant

/** Opaque remote user id. Maps 1:1 to a Firebase Auth uid once auth lands. */
@JvmInline
value class UserId(
    val value: String,
)

@JvmInline
value class TradeId(
    val value: String,
)

/** Public view of another player's profile. */
data class RemoteProfile(
    val userId: UserId,
    val displayName: String,
    val level: Int,
    val cardCount: Int,
)

enum class TradeStatus {
    PENDING,
    ACCEPTED,
    DECLINED,
    CANCELLED,
}

data class TradeOffer(
    val id: TradeId,
    val from: UserId,
    val to: UserId,
    val offered: List<ObjectId>,
    val requested: List<ObjectId>,
    val status: TradeStatus,
    val createdAt: Instant,
)

/** Expected trade failures are values, not exceptions. */
sealed interface TradeResult {
    data class Success(
        val offer: TradeOffer,
    ) : TradeResult

    data class Failure(
        val error: TradeError,
    ) : TradeResult
}

enum class TradeError {
    NOT_FOUND,
    NOT_A_PARTICIPANT,
    NOT_PENDING,
    EMPTY_TRADE,
    NETWORK,
}
