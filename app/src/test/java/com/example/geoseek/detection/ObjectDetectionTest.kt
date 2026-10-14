package com.example.geoseek.detection

import com.example.geoseek.models.GameObject
import com.example.geoseek.models.Rarity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ObjectDetectionTest {

    private val matcher = ObjectLabelMatcher(defaultConfidenceThreshold = 0.60f)
    private val sanitizer = LabelSanitizer()
    private val frameFilter = TemporalFrameFilter(requiredStreak = 3, maxGapMillis = 1500L)

    @Test
    fun testExactCanonicalMatch() {
        val chairTarget = GameObject("Chair", Rarity.COMMON, 10)
        val labels = listOf(RawLabel("Chair", 0.85f))

        val match = matcher.findTargetMatch(chairTarget, labels)
        assertNotNull(match)
        assertEquals("Chair", match?.gameObject?.name)
        assertEquals(0.85f, match?.confidence ?: 0f, 0.01f)
    }

    @Test
    fun testAliasMatchingForPlantAndLaptop() {
        val plantTarget = GameObject("Plant", Rarity.COMMON, 15)
        val houseplantLabels = listOf(RawLabel("Houseplant", 0.78f))

        val plantMatch = matcher.findTargetMatch(plantTarget, houseplantLabels)
        assertNotNull(plantMatch)
        assertEquals("Plant", plantMatch?.gameObject?.name)

        // Verify flowerpot & flower match plant target
        val flowerpotLabels = listOf(RawLabel("Flowerpot", 0.72f))
        val potMatch = matcher.findTargetMatch(plantTarget, flowerpotLabels)
        assertNotNull(potMatch)
        assertEquals("Plant", potMatch?.gameObject?.name)

        val laptopTarget = GameObject("Laptop", Rarity.UNCOMMON, 30)
        val notebookLabels = listOf(RawLabel("Notebook computer", 0.82f))

        val laptopMatch = matcher.findTargetMatch(laptopTarget, notebookLabels)
        assertNotNull(laptopMatch)
        assertEquals("Laptop", laptopMatch?.gameObject?.name)
    }

    @Test
    fun testLowConfidenceLabelIsIgnored() {
        val chairTarget = GameObject("Chair", Rarity.COMMON, 10)
        val lowConfLabels = listOf(RawLabel("Chair", 0.40f))

        val match = matcher.findTargetMatch(chairTarget, lowConfLabels, threshold = 0.60f)
        assertNull(match)
    }

    @Test
    fun testLabelSanitizerSuppressesGenericLabels() {
        assertFalse(sanitizer.isMeaningfulLabel("Product"))
        assertFalse(sanitizer.isMeaningfulLabel("Material"))
        assertFalse(sanitizer.isMeaningfulLabel("Rectangle"))
        assertTrue(sanitizer.isMeaningfulLabel("Armchair"))
        assertTrue(sanitizer.isMeaningfulLabel("Houseplant"))

        val labels = listOf(
            RawLabel("Product", 0.95f),
            RawLabel("Material", 0.90f),
            RawLabel("Armchair", 0.80f),
        )
        val visible = sanitizer.extractVisibleLabel(labels)
        assertEquals("Armchair", visible)
    }

    @Test
    fun testTemporalFrameFilterRequiresThreeConsecutiveStreakFrames() {
        // Frame 1: Match 1
        var result = frameFilter.processFrame(isMatch = true, confidence = 0.80f, currentTimeMillis = 1000L)
        assertFalse(result.isConfirmed)
        assertEquals(1, result.streakCount)

        // Frame 2: Match 2
        result = frameFilter.processFrame(isMatch = true, confidence = 0.82f, currentTimeMillis = 1200L)
        assertFalse(result.isConfirmed)
        assertEquals(2, result.streakCount)

        // Frame 3: Match 3 -> Confirmed!
        result = frameFilter.processFrame(isMatch = true, confidence = 0.84f, currentTimeMillis = 1400L)
        assertTrue(result.isConfirmed)
        assertEquals(3, result.streakCount)
        assertEquals(0.82f, result.averageConfidence, 0.01f)
    }

    @Test
    fun testTemporalFrameFilterResetsOnMissedFrame() {
        frameFilter.processFrame(isMatch = true, confidence = 0.80f, currentTimeMillis = 1000L)
        frameFilter.processFrame(isMatch = true, confidence = 0.82f, currentTimeMillis = 1200L)

        // Missed frame resets streak
        val (isConfirmed, streakCount, _) = frameFilter.processFrame(
            isMatch = false,
            confidence = 0f,
            currentTimeMillis = 1400L,
        )
        assertFalse(isConfirmed)
        assertEquals(0, streakCount)
    }

    @Test
    fun testTemporalFrameFilterResetsOnTimeGap() {
        frameFilter.processFrame(isMatch = true, confidence = 0.80f, currentTimeMillis = 1000L)

        // Gap of 2000ms (> 1500ms limit) resets streak back to 1
        val result = frameFilter.processFrame(isMatch = true, confidence = 0.80f, currentTimeMillis = 3000L)
        assertFalse(result.isConfirmed)
        assertEquals(1, result.streakCount)
    }

    /** Checks inclusive confidence threshold behavior at the exact boundary. */
    @Test
    fun testConfidenceThresholdBoundary() {
        val target = GameObject("Chair", Rarity.COMMON, 10)
        assertNull(matcher.findTargetMatch(target, listOf(RawLabel("Chair", 0.5999f))))
        assertNotNull(matcher.findTargetMatch(target, listOf(RawLabel("Chair", 0.60f))))
        assertNotNull(matcher.findTargetMatch(target, listOf(RawLabel("Chair", 0.6001f))))
    }

    /** Empty, unknown, and blank labels must not become catalog matches. */
    @Test
    fun testEmptyUnknownAndBlankLabelsHaveNoMatch() {
        assertTrue(matcher.findMatches(emptyList()).isEmpty())
        assertTrue(matcher.findMatches(listOf(RawLabel("Aurora", 0.99f))).isEmpty())
        assertTrue(matcher.findMatches(listOf(RawLabel("", 0.99f), RawLabel("   ", 0.99f))).isEmpty())
    }

    /** Specific hunts ignore higher-confidence labels for other supported objects. */
    @Test
    fun testTargetSelectionUsesHighestConfidenceMatchingAlias() {
        val target = GameObject("Chair", Rarity.COMMON, 10)
        val result = matcher.findTargetMatch(
            target,
            listOf(RawLabel("Plant", 0.99f), RawLabel("Chair", 0.70f), RawLabel("Armchair", 0.90f)),
        )
        assertEquals("Chair", result?.gameObject?.name)
        assertEquals("Armchair", result?.matchedLabel)
        assertEquals(0.90f, result?.confidence ?: 0f, 0.0001f)
    }

    /** Display filtering returns no label when every result is generic or empty. */
    @Test
    fun testSanitizerReturnsNullWithoutMeaningfulLabels() {
        assertNull(sanitizer.extractVisibleLabel(emptyList()))
        assertNull(sanitizer.extractVisibleLabel(
            listOf(RawLabel(" Product ", 0.99f), RawLabel("MATERIAL", 0.90f), RawLabel("", 1f)),
        ))
    }

    /** The maximum permitted gap remains part of the same streak. */
    @Test
    fun testTimeGapAtMaximumPreservesStreak() {
        frameFilter.processFrame(true, 0.60f, 1000L)
        val result = frameFilter.processFrame(true, 0.90f, 2500L)
        assertFalse(result.isConfirmed)
        assertEquals(2, result.streakCount)
        assertEquals(0.75f, result.averageConfidence, 0.0001f)
    }

    /** One millisecond beyond the maximum starts a new confidence window. */
    @Test
    fun testTimeGapJustBeyondMaximumResetsConfidence() {
        frameFilter.processFrame(true, 0.60f, 1000L)
        val result = frameFilter.processFrame(true, 0.90f, 2501L)
        assertFalse(result.isConfirmed)
        assertEquals(1, result.streakCount)
        assertEquals(0.90f, result.averageConfidence, 0.0001f)
    }

    /** Timestamp zero is a valid first frame, not an unset-time sentinel. */
    @Test
    fun testTimeGapAfterFrameAtTimestampZeroResetsStreak() {
        frameFilter.processFrame(true, 0.60f, 0L)
        val result = frameFilter.processFrame(true, 0.90f, 1501L)
        assertEquals(1, result.streakCount)
        assertEquals(0.90f, result.averageConfidence, 0.0001f)
    }

    /** Explicit reset discards a confirmed streak and its previous confidence. */
    @Test
    fun testExplicitResetClearsConfirmedStreak() {
        repeat(3) { frameFilter.processFrame(true, 0.90f, 1000L + it * 100L) }
        frameFilter.reset()
        val result = frameFilter.processFrame(true, 0.60f, 1400L)
        assertFalse(result.isConfirmed)
        assertEquals(1, result.streakCount)
        assertEquals(0.60f, result.averageConfidence, 0.0001f)
    }

    /** Configurable streak counts allow immediate or longer confirmation. */
    @Test
    fun testCustomRequiredStreak() {
        assertTrue(TemporalFrameFilter(requiredStreak = 1).processFrame(true, 0.80f, 1000L).isConfirmed)
        val filter = TemporalFrameFilter(requiredStreak = 4)
        repeat(3) { assertFalse(filter.processFrame(true, 0.80f, 1000L + it * 100L).isConfirmed) }
        assertTrue(filter.processFrame(true, 0.80f, 1300L).isConfirmed)
    }
}
