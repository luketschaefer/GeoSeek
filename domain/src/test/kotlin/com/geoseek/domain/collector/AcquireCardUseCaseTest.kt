package com.geoseek.domain.collector

import com.geoseek.domain.catalog.ObjectId
import com.geoseek.domain.testCatalog
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class AcquireCardUseCaseTest {
    private val now = Instant.parse("2026-09-28T12:00:00Z")
    private val repo = FakeCollectionRepository()
    private val acquire = AcquireCardUseCase(testCatalog, repo, Clock.fixed(now, ZoneOffset.UTC))

    @Test
    fun `passes catalog xp and clock time to repository`() =
        runTest {
            val id = ObjectId("park_3")
            val result = acquire(id)
            assertThat(result.isFirstAcquisition).isTrue()
            assertThat(result.xpAwarded).isEqualTo(testCatalog.require(id).xp)
            assertThat(result.card.firstAcquiredAt).isEqualTo(now)
        }

    @Test
    fun `second acquisition awards no xp`() =
        runTest {
            val id = ObjectId("park_3")
            acquire(id)
            val second = acquire(id)
            assertThat(second.isFirstAcquisition).isFalse()
            assertThat(second.xpAwarded).isEqualTo(0)
            assertThat(second.card.count).isEqualTo(2)
        }

    private class FakeCollectionRepository : CollectionRepository {
        private val cards = MutableStateFlow<Map<ObjectId, Card>>(emptyMap())

        override fun observeCards(): Flow<List<Card>> = cards.map { it.values.toList() }

        override fun observeCard(id: ObjectId): Flow<Card?> = cards.map { it[id] }

        override suspend fun acquire(
            id: ObjectId,
            xpIfFirst: Int,
            at: Instant,
        ): AcquisitionResult {
            val existing = cards.value[id]
            val card = existing?.copy(count = existing.count + 1) ?: Card(id, at, 1)
            cards.value += id to card
            return AcquisitionResult(card, existing == null, if (existing == null) xpIfFirst else 0)
        }
    }
}
