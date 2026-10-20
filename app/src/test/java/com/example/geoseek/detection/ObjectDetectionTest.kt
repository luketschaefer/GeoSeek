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

    private val cup = GameObject("Cup", Rarity.COMMON, 10)
    private val bottle = GameObject("Bottle", Rarity.COMMON, 10)
    private val mug = TrackedBox(trackingId = 1, box = Box(100f, 100f, 200f, 200f))

    /** Feeds [frames] frames 100 ms apart starting at [start] and returns the last state. */
    private fun HoldTracker.feed(frames: Int, seen: TrackedBox?, match: LabelMatch?, start: Long = 1_000L): HoldState {
        var state = HoldState()
        repeat(frames) { state = update(seen, listOfNotNull(match), start + it * 100L) }
        return state
    }

    @Test
    fun testHoldConfirmsOnlyAfterHoldingForTwoSeconds() {
        val tracker = HoldTracker(holdMillis = 2_000L)
        val match = LabelMatch(cup, "Mug", 0.85f)

        val early = tracker.feed(15, mug, match)
        assertFalse(early.confirmed)
        assertEquals(0.7f, early.progress, 0.01f)
        assertEquals(0.85f, early.confidence, 0.01f)

        val done = tracker.feed(6, mug, match, start = 2_500L)
        assertTrue(done.confirmed)
        assertEquals("Cup", done.candidate?.name)
    }

    @Test
    fun testHoldPausesWhileConfidenceIsLow() {
        val tracker = HoldTracker(holdMillis = 2_000L, minConfidence = 0.60f)
        val state = tracker.feed(30, mug, LabelMatch(cup, "Mug", 0.55f))
        assertFalse(state.confirmed)
        assertEquals(0f, state.progress, 0.001f)
        assertEquals(0.55f, state.confidence, 0.01f)
    }

    @Test
    fun testHoldSticksWithCandidateThenHandsOverWhenItFades() {
        val tracker = HoldTracker(holdMillis = 2_000L)
        val cupReading = LabelMatch(cup, "Mug", 0.85f)
        val bottleReading = LabelMatch(bottle, "Bottle", 0.9f)
        tracker.feed(10, mug, cupReading)

        // A second label in the same reading doesn't steal the hold while the candidate is still there.
        val both = tracker.update(mug, listOf(bottleReading, cupReading), 2_000L)
        assertEquals("Cup", both.candidate?.name)
        assertTrue(both.progress > 0.4f)

        // Readings without the candidate pause the hold and fade it out, then the new object starts from zero.
        val missed = tracker.update(mug, listOf(bottleReading), 2_100L)
        assertEquals("Cup", missed.candidate?.name)
        assertEquals(both.progress, missed.progress, 0.001f)
        var state = missed
        var t = 2_200L
        while (state.candidate?.name == "Cup") {
            state = tracker.update(mug, listOf(bottleReading), t)
            t += 100L
        }
        assertNull(state.candidate)
        state = tracker.update(mug, listOf(bottleReading), t)
        assertEquals("Bottle", state.candidate?.name)
        assertTrue(state.progress <= 0.05f)
    }

    @Test
    fun testFramesWithoutLabelReadingKeepTheHoldGoing() {
        val tracker = HoldTracker(holdMillis = 2_000L)
        tracker.update(mug, listOf(LabelMatch(cup, "Mug", 0.85f)), 1_000L)
        var state = HoldState()
        for (i in 1..20) state = tracker.update(mug, emptyList(), 1_000L + i * 100L, labeled = false)
        assertTrue(state.confirmed)
        assertEquals(0.85f, state.confidence, 0.001f)
    }

    @Test
    fun testUprightBoxMapsBackToBuffer() {
        // A 640x480 buffer rotated 90 degrees is a 480x640 upright image.
        val upright = Box(10f, 20f, 110f, 220f)
        assertEquals(Box(20f, 370f, 220f, 470f), upright.uprightToBuffer(90, 640, 480))
        assertEquals(Box(530f, 260f, 630f, 460f), upright.uprightToBuffer(180, 640, 480))
        assertEquals(Box(420f, 10f, 620f, 110f), upright.uprightToBuffer(270, 640, 480))
        assertEquals(upright, upright.uprightToBuffer(0, 640, 480))
    }

    @Test
    fun testAliasesMatchWholeWordsOnly() {
        assertTrue(matcher.findMatches(listOf(RawLabel("Cupboard", 0.9f), RawLabel("Street", 0.9f))).isEmpty())
        assertTrue(matcher.findMatches(listOf(RawLabel("Desktop computer", 0.9f), RawLabel("Vehicle", 0.9f))).isEmpty())
        assertEquals("Cup", matcher.findMatches(listOf(RawLabel("Coffee cup", 0.9f))).single().gameObject.name)
        // Two labels for the same object collapse into its best one.
        val merged = matcher.findMatches(listOf(RawLabel("Mug", 0.7f), RawLabel("Cup", 0.8f)))
        assertEquals(1, merged.size)
        assertEquals(0.8f, merged.single().confidence, 0.001f)
    }

    @Test
    fun testHoldSurvivesBriefDropoutButResetsWhenLost() {
        val tracker = HoldTracker(holdMillis = 2_000L, lostGraceMillis = 500L)
        val match = LabelMatch(cup, "Mug", 0.85f)
        tracker.feed(10, mug, match)

        val blip = tracker.update(null, emptyList(), 2_000L)
        assertEquals(mug, blip.lock)
        assertTrue(blip.progress > 0f)

        val lost = tracker.update(null, emptyList(), 2_600L)
        assertNull(lost.lock)
        assertEquals(0f, lost.progress, 0.001f)
    }

    @Test
    fun testHoldRestartsOnDifferentObject() {
        val tracker = HoldTracker(holdMillis = 2_000L)
        val match = LabelMatch(cup, "Mug", 0.85f)
        tracker.feed(10, mug, match)
        val other = TrackedBox(trackingId = 2, box = Box(300f, 300f, 400f, 400f))
        val state = tracker.update(other, listOf(match), 2_000L)
        assertEquals(other, state.lock)
        assertTrue(state.progress < 0.1f)
    }

    @Test
    fun testChooseLockPrefersObjectUnderCenter() {
        val big = TrackedBox(1, Box(0f, 0f, 400f, 400f))
        val small = TrackedBox(2, Box(150f, 150f, 250f, 250f))
        val far = TrackedBox(3, Box(350f, 0f, 400f, 50f))
        assertEquals(small, chooseLock(listOf(big, small, far), 200f, 200f, previous = null))
        // An existing lock that still covers the center is kept, so the outline doesn't flip between objects.
        assertEquals(big, chooseLock(listOf(big, small, far), 200f, 200f, previous = big))
        assertEquals(far, chooseLock(listOf(far), 200f, 200f, previous = null))
        assertNull(chooseLock(emptyList(), 200f, 200f, previous = null))
    }

    @Test
    fun testAirPodsMatchWithOrWithoutCase() {
        for (label in listOf("AirPods", "AirPods case", "Earbuds")) {
            assertEquals(label, "AirPods", matcher.findMatches(listOf(RawLabel(label, 0.9f))).single().gameObject.name)
        }
        // The custom model's catch-all class must never count as a find.
        assertTrue(matcher.findMatches(listOf(RawLabel("Background", 0.99f))).isEmpty())
    }

    @Test
    fun testCustomLabelsParseTeachableMachineFormat() {
        assertEquals(
            listOf("AirPods", "AirPods case", "Background"),
            CustomClassifier.parseLabels("0 AirPods\n1 AirPods case\n\n2 Background\n"),
        )
        assertEquals(listOf("AirPods"), CustomClassifier.parseLabels("AirPods"))
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
