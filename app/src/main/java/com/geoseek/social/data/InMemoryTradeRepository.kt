package com.geoseek.social.data

import com.geoseek.domain.catalog.ObjectId
import com.geoseek.domain.social.TradeError
import com.geoseek.domain.social.TradeId
import com.geoseek.domain.social.TradeOffer
import com.geoseek.domain.social.TradeRepository
import com.geoseek.domain.social.TradeResult
import com.geoseek.domain.social.TradeStatus
import com.geoseek.domain.social.UserId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.time.Clock
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Process-local trades implementing the full offer lifecycle, so UI can be built before Firestore.
 * Does not move cards between collections; the Firestore version will do that server-side.
 */
@Singleton
class InMemoryTradeRepository
    @Inject
    constructor(
        private val clock: Clock,
    ) : TradeRepository {
        private val offers = MutableStateFlow<Map<TradeId, TradeOffer>>(emptyMap())

        override fun observeOffers(user: UserId): Flow<List<TradeOffer>> =
            offers.map { all ->
                all.values.filter { it.from == user || it.to == user }.sortedByDescending { it.createdAt }
            }

        override suspend fun propose(
            from: UserId,
            to: UserId,
            offered: List<ObjectId>,
            requested: List<ObjectId>,
        ): TradeResult {
            if (offered.isEmpty() && requested.isEmpty()) return TradeResult.Failure(TradeError.EMPTY_TRADE)
            if (from == to) return TradeResult.Failure(TradeError.NOT_A_PARTICIPANT)
            val offer =
                TradeOffer(
                    id = TradeId(UUID.randomUUID().toString()),
                    from = from,
                    to = to,
                    offered = offered,
                    requested = requested,
                    status = TradeStatus.PENDING,
                    createdAt = clock.instant(),
                )
            offers.update { it + (offer.id to offer) }
            return TradeResult.Success(offer)
        }

        override suspend fun respond(
            id: TradeId,
            responder: UserId,
            accept: Boolean,
        ): TradeResult =
            transition(id) { offer ->
                if (offer.to != responder) return@transition TradeResult.Failure(TradeError.NOT_A_PARTICIPANT)
                TradeResult.Success(offer.copy(status = if (accept) TradeStatus.ACCEPTED else TradeStatus.DECLINED))
            }

        override suspend fun cancel(
            id: TradeId,
            requester: UserId,
        ): TradeResult =
            transition(id) { offer ->
                if (offer.from != requester) return@transition TradeResult.Failure(TradeError.NOT_A_PARTICIPANT)
                TradeResult.Success(offer.copy(status = TradeStatus.CANCELLED))
            }

        /** Applies [change] to a pending offer atomically. */
        private fun transition(
            id: TradeId,
            change: (TradeOffer) -> TradeResult,
        ): TradeResult {
            var result: TradeResult = TradeResult.Failure(TradeError.NOT_FOUND)
            offers.update { all ->
                val offer = all[id]
                result =
                    when {
                        offer == null -> TradeResult.Failure(TradeError.NOT_FOUND)
                        offer.status != TradeStatus.PENDING -> TradeResult.Failure(TradeError.NOT_PENDING)
                        else -> change(offer)
                    }
                (result as? TradeResult.Success)?.let { all + (id to it.offer) } ?: all
            }
            return result
        }
    }
