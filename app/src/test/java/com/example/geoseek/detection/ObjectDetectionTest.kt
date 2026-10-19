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

    // Manual mutation demonstrations. Each test checks the production result and
    // shows a specific faulty replacement producing a different result.
    // Mutants stay local to these tests; production code is never swapped.

    /** M1: confidence >= threshold mutated to confidence > threshold. */
    @Test
    fun mutationM1StrictConfidenceThresholdIsKilled() {
        val threshold = 0.60f
        val label = RawLabel("Chair", threshold)
        val target = GameObject("Chair", Rarity.COMMON, 10)

        assertNotNull(matcher.findTargetMatch(target, listOf(label), threshold))
        val mutantAcceptsLabel = label.confidence > threshold
        assertFalse("M1 incorrectly rejects confidence exactly at the threshold", mutantAcceptsLabel)
    }

    /** M2: currentStreak >= requiredStreak mutated to currentStreak > requiredStreak. */
    @Test
    fun mutationM2StrictStreakThresholdIsKilled() {
        val requiredStreak = 3
        val filter = TemporalFrameFilter(requiredStreak = requiredStreak)
        repeat(2) { filter.processFrame(true, 0.80f, 1000L + it * 100L) }
        val result = filter.processFrame(true, 0.80f, 1200L)

        assertTrue(result.isConfirmed)
        val mutantConfirmed = result.streakCount > requiredStreak
        assertFalse("M2 incorrectly requires a fourth frame", mutantConfirmed)
    }

    /** M3: gap > maxGapMillis mutated to gap >= maxGapMillis. */
    @Test
    fun mutationM3InclusiveGapResetIsKilled() {
        val maxGapMillis = 1500L
        val filter = TemporalFrameFilter(maxGapMillis = maxGapMillis)
        filter.processFrame(true, 0.60f, 1000L)
        val result = filter.processFrame(true, 0.90f, 2500L)

        assertEquals(2, result.streakCount)
        val mutantStreak = if (2500L - 1000L >= maxGapMillis) 1 else 2
        assertEquals("M3 incorrectly resets at the permitted gap", 1, mutantStreak)
        assertTrue(result.streakCount != mutantStreak)
    }

    /** M4: normalized.length > 2 mutated to normalized.length >= 2. */
    @Test
    fun mutationM4InclusiveMinimumLabelLengthIsKilled() {
        val normalized = "TV".trim().lowercase()

        assertFalse(sanitizer.isMeaningfulLabel(normalized))
        // "tv" is not suppressed, so only the length condition decides this case.
        val mutantMeaningful = normalized.length >= 2
        assertTrue("M4 incorrectly accepts a two-character label", mutantMeaningful)
    }

    /** M5: confidenceSum / currentStreak mutated to confidenceSum * currentStreak. */
    @Test
    fun mutationM5AverageDivisionReplacedByMultiplicationIsKilled() {
        val filter = TemporalFrameFilter()
        filter.processFrame(true, 0.60f, 1000L)
        val result = filter.processFrame(true, 0.90f, 1100L)

        assertEquals(0.75f, result.averageConfidence, 0.0001f)
        val mutantAverage = (0.60f + 0.90f) * 2
        assertEquals(3f, mutantAverage, 0.0001f)
        assertTrue(kotlin.math.abs(result.averageConfidence - mutantAverage) > 0.0001f)
    }

    // Assignment: boundary value tests for TemporalFrameFilter.processFrame.

    /** B1: 2 matching frames compared with a required streak of three. */
    @Test
    fun B1StreakOf2Frames() {
        val filter = TemporalFrameFilter(requiredStreak = 3, maxGapMillis = 1500L)
        var result = FilterResult(false, 0, 0f)
        repeat(2) { index ->
            result = filter.processFrame(true, 0.80f, 1000L + index * 100L)
        }
        assertEquals(2, result.streakCount)
        assertEquals(false, result.isConfirmed)
        assertEquals(0.80f, result.averageConfidence, 0.0001f)
    }

    /** B2: 3 matching frames compared with a required streak of three. */
    @Test
    fun B2StreakOf3Frames() {
        val filter = TemporalFrameFilter(requiredStreak = 3, maxGapMillis = 1500L)
        var result = FilterResult(false, 0, 0f)
        repeat(3) { index ->
            result = filter.processFrame(true, 0.80f, 1000L + index * 100L)
        }
        assertEquals(3, result.streakCount)
        assertEquals(true, result.isConfirmed)
        assertEquals(0.80f, result.averageConfidence, 0.0001f)
    }

    /** B3: 4 matching frames compared with a required streak of three. */
    @Test
    fun B3StreakOf4Frames() {
        val filter = TemporalFrameFilter(requiredStreak = 3, maxGapMillis = 1500L)
        var result = FilterResult(false, 0, 0f)
        repeat(4) { index ->
            result = filter.processFrame(true, 0.80f, 1000L + index * 100L)
        }
        assertEquals(4, result.streakCount)
        assertEquals(true, result.isConfirmed)
        assertEquals(0.80f, result.averageConfidence, 0.0001f)
    }

    /** B4: 1499 ms gap compared with the 1500 ms maximum. */
    @Test
    fun B4GapOf1499Milliseconds() {
        val filter = TemporalFrameFilter(requiredStreak = 3, maxGapMillis = 1500L)
        filter.processFrame(true, 0.60f, 1000L)
        val result = filter.processFrame(true, 0.90f, 2499L)
        assertFalse(result.isConfirmed)
        assertEquals(2, result.streakCount)
        assertEquals(0.75f, result.averageConfidence, 0.0001f)
    }

    /** B5: 1500 ms gap compared with the 1500 ms maximum. */
    @Test
    fun B5GapOf1500Milliseconds() {
        val filter = TemporalFrameFilter(requiredStreak = 3, maxGapMillis = 1500L)
        filter.processFrame(true, 0.60f, 1000L)
        val result = filter.processFrame(true, 0.90f, 2500L)
        assertFalse(result.isConfirmed)
        assertEquals(2, result.streakCount)
        assertEquals(0.75f, result.averageConfidence, 0.0001f)
    }

    /** B6: 1501 ms gap compared with the 1500 ms maximum. */
    @Test
    fun B6GapOf1501Milliseconds() {
        val filter = TemporalFrameFilter(requiredStreak = 3, maxGapMillis = 1500L)
        filter.processFrame(true, 0.60f, 1000L)
        val result = filter.processFrame(true, 0.90f, 2501L)
        assertFalse(result.isConfirmed)
        assertEquals(1, result.streakCount)
        assertEquals(0.90f, result.averageConfidence, 0.0001f)
    }

    // Assignment: equivalence partitions for LabelSanitizer.isMeaningfulLabel.
    // P1: normalized length <= 2; P2: longer and suppressed; P3: longer and unsuppressed.

    /** Empty input belongs to the short-label partition. */
    @Test
    fun assignmentP1EmptyLabel() {
        val sanitizer = LabelSanitizer()
        assertEquals(false, sanitizer.isMeaningfulLabel(""))
    }

    /** Whitespace normalizes to the short-label partition. */
    @Test
    fun assignmentP1WhitespaceLabel() {
        val sanitizer = LabelSanitizer()
        assertEquals(false, sanitizer.isMeaningfulLabel("   "))
    }

    /** Two characters remain in the rejected short-label partition. */
    @Test
    fun assignmentP1TwoCharactersLabel() {
        val sanitizer = LabelSanitizer()
        assertEquals(false, sanitizer.isMeaningfulLabel("TV"))
    }

    /** A long generic label belongs to the suppressed partition. */
    @Test
    fun assignmentP2SuppressedLabel() {
        val sanitizer = LabelSanitizer()
        assertEquals(false, sanitizer.isMeaningfulLabel("Product"))
    }

    /** Normalization preserves suppressed partition membership. */
    @Test
    fun assignmentP2NormalizedSuppressedLabel() {
        val sanitizer = LabelSanitizer()
        assertEquals(false, sanitizer.isMeaningfulLabel(" PRODUCT "))
    }

    /** A long unsuppressed label belongs to the accepted partition. */
    @Test
    fun assignmentP3ObjectLabel() {
        val sanitizer = LabelSanitizer()
        assertEquals(true, sanitizer.isMeaningfulLabel("Chair"))
    }

    /** Normalization preserves accepted partition membership. */
    @Test
    fun assignmentP3NormalizedObjectLabel() {
        val sanitizer = LabelSanitizer()
        assertEquals(true, sanitizer.isMeaningfulLabel(" cHaIr "))
    }

    /** An unsuppressed name need not exist in the game catalog. */
    @Test
    fun assignmentP3NonCatalogLabel() {
        val sanitizer = LabelSanitizer()
        assertEquals(true, sanitizer.isMeaningfulLabel("Aurora"))
    }

}
