package com.geoseek.domain.social

import com.geoseek.domain.catalog.ObjectId
import kotlinx.coroutines.flow.Flow

/**
 * Trades between friends. Implementations: in-memory today; Firestore later (one document per
 * offer; acceptance must run as a server-side transaction that moves cards on both sides).
 */
interface TradeRepository {
    /** Offers where [user] is sender or recipient, newest first. */
    fun observeOffers(user: UserId): Flow<List<TradeOffer>>

    suspend fun propose(
        from: UserId,
        to: UserId,
        offered: List<ObjectId>,
        requested: List<ObjectId>,
    ): TradeResult

    /** Recipient accepts or declines a pending offer. */
    suspend fun respond(
        id: TradeId,
        responder: UserId,
        accept: Boolean,
    ): TradeResult

    /** Sender withdraws a pending offer. */
    suspend fun cancel(
        id: TradeId,
        requester: UserId,
    ): TradeResult
}
