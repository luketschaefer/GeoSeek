package com.geoseek.hunt

import com.geoseek.detection.FakeDetector
import com.geoseek.domain.catalog.CatalogParser
import com.geoseek.domain.catalog.ObjectId
import com.geoseek.domain.detection.Detection
import com.geoseek.testing.bundledCatalogJson
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** End-to-end seam test without a camera: FakeDetector → TargetMatcher → overlay state. */
class DetectionDebugTrackerTest {
    private val cup = CatalogParser().parse(bundledCatalogJson()).require(ObjectId(HuntViewModel.DEBUG_TARGET_ID))
    private val seen = listOf(Detection("Tableware", 0.95f), Detection("Cup", 0.9f))
    private val missed = listOf(Detection("Table", 0.9f))

    @Test
    fun `matches after required consecutive frames from the detector`() {
        val tracker = DetectionDebugTracker(cup, requiredFrames = 3)
        val detector = FakeDetector(script = listOf(seen, seen, missed, seen, seen, seen))
        val states = List(6) { tracker.onFrame(detector.nextFrame()) }

        assertThat(states.map { it.streak }).containsExactly(1, 2, 0, 1, 2, 3).inOrder()
        assertThat(states.map { it.matched }).containsExactly(false, false, false, false, false, true).inOrder()
        assertThat(states.last().framesAnalyzed).isEqualTo(6)
    }

    @Test
    fun `labels are sorted by confidence and flag the ones that count`() {
        val state = DetectionDebugTracker(cup).onFrame(seen)
        assertThat(state.labels.map { it.label }).containsExactly("Tableware", "Cup").inOrder()
        assertThat(state.labels.map { it.countsForTarget }).containsExactly(false, true).inOrder()
    }

    @Test
    fun `stays matched once confirmed`() {
        val tracker = DetectionDebugTracker(cup, requiredFrames = 1)
        tracker.onFrame(seen)
        assertThat(tracker.onFrame(missed).matched).isTrue()
    }
}
