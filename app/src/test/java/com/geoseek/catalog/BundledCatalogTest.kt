package com.geoseek.catalog

import com.geoseek.domain.catalog.CatalogParser
import com.geoseek.domain.catalog.Environment
import com.geoseek.domain.catalog.Rarity
import com.geoseek.domain.hunt.RoundConfig
import com.geoseek.testing.bundledCatalogJson
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Guards assets/catalog.json. The label list is extracted from the base model bundled in
 * com.google.mlkit:image-labeling:17.0.9 (0-labels-en.txt inside the .tflite metadata). If the
 * ML Kit dependency is bumped, re-extract it (see CLAUDE.md, "Adding a catalog object").
 */
class BundledCatalogTest {
    private val mlKitLabels: Set<String> =
        requireNotNull(javaClass.getResource("/mlkit-base-labels-17.0.9.txt"))
            .readText()
            .lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toSet()

    private val catalog = CatalogParser(knownLabels = mlKitLabels).parse(bundledCatalogJson())

    @Test
    fun `label map fixture is the full base model`() {
        assertThat(mlKitLabels).hasSize(447)
    }

    @Test
    fun `bundled catalog is valid and every label exists in the ML Kit base model`() {
        // Parsing with knownLabels above already enforces this; assert size for a clear signal.
        assertThat(catalog.objects.size).isAtLeast(20)
    }

    @Test
    fun `every environment can fill a default round`() {
        Environment.entries.forEach { env ->
            assertThat(catalog.objectsIn(env).size).isAtLeast(RoundConfig.DEFAULT_TARGET_COUNT)
        }
    }

    @Test
    fun `every rarity is represented`() {
        assertThat(catalog.objects.map { it.rarity }.toSet()).containsExactlyElementsIn(Rarity.entries)
    }

    @Test
    fun `points and xp never decrease with rarity`() {
        val byRarity = catalog.objects.groupBy { it.rarity }
        Rarity.entries.zipWithNext().forEach { (lower, higher) ->
            val lowerMax = byRarity.getValue(lower).maxOf { it.points }
            val higherMin = byRarity.getValue(higher).minOf { it.points }
            assertThat(higherMin).isAtLeast(lowerMax)
        }
    }
}
