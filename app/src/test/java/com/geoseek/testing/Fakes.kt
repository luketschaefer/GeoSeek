package com.geoseek.testing

import com.geoseek.domain.catalog.ObjectId
import com.geoseek.domain.collector.AcquisitionResult
import com.geoseek.domain.collector.Card
import com.geoseek.domain.collector.CollectionRepository
import com.geoseek.domain.profile.Profile
import com.geoseek.domain.profile.ProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.io.File
import java.time.Instant

class FakeProfileRepository(
    initial: Profile = Profile(Profile.DEFAULT_DISPLAY_NAME, 0),
) : ProfileRepository {
    val profile = MutableStateFlow(initial)

    override fun observeProfile(): Flow<Profile> = profile

    override suspend fun setDisplayName(name: String) = profile.update { it.copy(displayName = name) }

    override suspend fun addXp(amount: Int) = profile.update { it.copy(totalXp = it.totalXp + amount) }
}

class FakeCollectionRepository : CollectionRepository {
    val cards = MutableStateFlow<Map<ObjectId, Card>>(emptyMap())

    override fun observeCards(): Flow<List<Card>> = cards.map { it.values.toList() }

    override fun observeCard(id: ObjectId): Flow<Card?> = cards.map { it[id] }

    override suspend fun acquire(
        id: ObjectId,
        xpIfFirst: Int,
        at: Instant,
    ): AcquisitionResult {
        val existing = cards.value[id]
        val card = existing?.copy(count = existing.count + 1) ?: Card(id, at, 1)
        cards.update { it + (id to card) }
        return AcquisitionResult(card, existing == null, if (existing == null) xpIfFirst else 0)
    }
}

/** The real bundled catalog. Unit tests run with the module directory as working directory. */
fun bundledCatalogJson(): String = File("src/main/assets/catalog.json").readText()
