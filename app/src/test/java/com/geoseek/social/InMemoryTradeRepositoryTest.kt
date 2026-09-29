package com.geoseek.social

import app.cash.turbine.test
import com.geoseek.domain.catalog.ObjectId
import com.geoseek.domain.social.TradeError
import com.geoseek.domain.social.TradeId
import com.geoseek.domain.social.TradeOffer
import com.geoseek.domain.social.TradeResult
import com.geoseek.domain.social.TradeStatus
import com.geoseek.domain.social.UserId
import com.geoseek.social.data.InMemoryTradeRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class InMemoryTradeRepositoryTest {
    private val alice = UserId("alice")
    private val bob = UserId("bob")
    private val carol = UserId("carol")
    private val repo = InMemoryTradeRepository(Clock.fixed(Instant.parse("2026-09-28T10:00:00Z"), ZoneOffset.UTC))

    private suspend fun proposeOffer(): TradeOffer =
        (repo.propose(alice, bob, listOf(ObjectId("cup")), listOf(ObjectId("bench"))) as TradeResult.Success).offer

    @Test
    fun `proposal is pending and visible to both participants only`() =
        runTest {
            val offer = proposeOffer()
            assertThat(offer.status).isEqualTo(TradeStatus.PENDING)
            repo.observeOffers(alice).test { assertThat(awaitItem()).containsExactly(offer) }
            repo.observeOffers(bob).test { assertThat(awaitItem()).containsExactly(offer) }
            repo.observeOffers(carol).test { assertThat(awaitItem()).isEmpty() }
        }

    @Test
    fun `recipient accepts or declines`() =
        runTest {
            val accepted = repo.respond(proposeOffer().id, bob, accept = true) as TradeResult.Success
            assertThat(accepted.offer.status).isEqualTo(TradeStatus.ACCEPTED)
            val declined = repo.respond(proposeOffer().id, bob, accept = false) as TradeResult.Success
            assertThat(declined.offer.status).isEqualTo(TradeStatus.DECLINED)
        }

    @Test
    fun `only the recipient can respond and only the sender can cancel`() =
        runTest {
            val offer = proposeOffer()
            assertThat(repo.respond(offer.id, alice, accept = true))
                .isEqualTo(TradeResult.Failure(TradeError.NOT_A_PARTICIPANT))
            assertThat(repo.cancel(offer.id, bob)).isEqualTo(TradeResult.Failure(TradeError.NOT_A_PARTICIPANT))
            assertThat(
                (repo.cancel(offer.id, alice) as TradeResult.Success).offer.status,
            ).isEqualTo(TradeStatus.CANCELLED)
        }

    @Test
    fun `finished offers cannot change`() =
        runTest {
            val offer = proposeOffer()
            repo.cancel(offer.id, alice)
            assertThat(
                repo.respond(offer.id, bob, accept = true),
            ).isEqualTo(TradeResult.Failure(TradeError.NOT_PENDING))
        }

    @Test
    fun `invalid proposals are rejected`() =
        runTest {
            assertThat(repo.propose(alice, bob, emptyList(), emptyList()))
                .isEqualTo(TradeResult.Failure(TradeError.EMPTY_TRADE))
            assertThat(repo.propose(alice, alice, listOf(ObjectId("cup")), emptyList()))
                .isEqualTo(TradeResult.Failure(TradeError.NOT_A_PARTICIPANT))
        }

    @Test
    fun `unknown offer is not found`() =
        runTest {
            assertThat(repo.cancel(TradeId("nope"), alice))
                .isEqualTo(TradeResult.Failure(TradeError.NOT_FOUND))
        }
}
