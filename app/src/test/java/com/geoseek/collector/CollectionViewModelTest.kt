package com.geoseek.collector

import com.geoseek.domain.catalog.CatalogParser
import com.geoseek.domain.catalog.ObjectId
import com.geoseek.testing.FakeCollectionRepository
import com.geoseek.testing.MainDispatcherRule
import com.geoseek.testing.bundledCatalogJson
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import java.time.Instant

class CollectionViewModelTest {
    @get:Rule
    val mainRule = MainDispatcherRule()

    private val catalog = CatalogParser().parse(bundledCatalogJson())
    private val repo = FakeCollectionRepository()

    @Test
    fun `lists whole catalog sorted by rarity with owned counts`() =
        runTest {
            repo.acquire(ObjectId("cup"), 5, Instant.EPOCH)
            repo.acquire(ObjectId("cup"), 5, Instant.EPOCH)
            val vm = CollectionViewModel(catalog, repo)
            backgroundScope.launch { vm.uiState.collect {} }

            val state = vm.uiState.first { !it.isLoading }
            assertThat(state.items).hasSize(catalog.objects.size)
            assertThat(state.items.map { it.rarity.ordinal }).isInOrder()
            assertThat(state.items.single { it.objectId == "cup" }.count).isEqualTo(2)
            assertThat(state.collectedCount).isEqualTo(1)
        }
}
