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
}
