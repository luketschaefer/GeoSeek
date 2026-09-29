package com.geoseek.domain.detection

import com.geoseek.domain.testObject
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test

class TargetMatcherTest {
    private val dog = testObject("dog", labels = setOf("Dog", "Pet"), minConfidence = 0.7f)
    private val cup = testObject("cup", labels = setOf("Cup"), minConfidence = 0.6f)
    private val targets = listOf(dog, cup)
    private val matcher = TargetMatcher(requiredConsecutiveFrames = 3)

    private fun frame(vararg detections: Pair<String, Float>) =
        matcher.onFrame(detections.map { Detection(it.first, it.second) }, targets)

    @Test
    fun `confirms after K consecutive frames, exactly once`() {
        assertThat(frame("Dog" to 0.9f).newlyConfirmed).isEmpty()
        assertThat(frame("Dog" to 0.9f).newlyConfirmed).isEmpty()
        val third = frame("Dog" to 0.9f)
        assertThat(third.newlyConfirmed).containsExactly(dog.id)
        assertThat(third.confirmed).containsExactly(dog.id)
        assertThat(frame("Dog" to 0.9f).newlyConfirmed).isEmpty()
    }

    @Test
    fun `a missed frame resets the streak`() {
        frame("Dog" to 0.9f)
        frame("Dog" to 0.9f)
        assertThat(frame().streaks[dog.id]).isEqualTo(0)
        frame("Dog" to 0.9f)
        assertThat(frame("Dog" to 0.9f).newlyConfirmed).isEmpty()
        assertThat(frame("Dog" to 0.9f).newlyConfirmed).containsExactly(dog.id)
    }

    @Test
    fun `below threshold does not count and resets`() {
        frame("Dog" to 0.9f)
        frame("Dog" to 0.9f)
        assertThat(frame("Dog" to 0.69f).streaks[dog.id]).isEqualTo(0)
    }

    @Test
    fun `threshold is inclusive and per object`() {
        repeat(2) { frame("Cup" to 0.6f) }
        assertThat(frame("Cup" to 0.6f).newlyConfirmed).containsExactly(cup.id)
    }

    @Test
    fun `any accepted label counts, case-insensitively, and can alternate`() {
        frame("Dog" to 0.8f)
        frame("pet" to 0.8f)
        assertThat(frame("DOG" to 0.8f).newlyConfirmed).containsExactly(dog.id)
    }

    @Test
    fun `targets progress independently in the same frames`() {
        frame("Dog" to 0.9f)
        frame("Dog" to 0.9f, "Cup" to 0.9f)
        val third = frame("Dog" to 0.9f, "Cup" to 0.9f)
        assertThat(third.newlyConfirmed).containsExactly(dog.id)
        assertThat(third.streaks[cup.id]).isEqualTo(2)
    }

    @Test
    fun `unrelated labels are ignored`() {
        repeat(5) { assertThat(frame("Cat" to 0.99f).newlyConfirmed).isEmpty() }
    }

    @Test
    fun `targets removed from the active list drop their streak`() {
        frame("Dog" to 0.9f)
        frame("Dog" to 0.9f)
        matcher.onFrame(emptyList(), listOf(cup)).also { assertThat(it.streaks).doesNotContainKey(dog.id) }
        assertThat(frame("Dog" to 0.9f).newlyConfirmed).isEmpty()
    }

    @Test
    fun `reset clears confirmations`() {
        repeat(3) { frame("Dog" to 0.9f) }
        matcher.reset()
        repeat(2) { frame("Dog" to 0.9f) }
        assertThat(frame("Dog" to 0.9f).newlyConfirmed).containsExactly(dog.id)
    }

    @Test
    fun `K of one confirms immediately`() {
        val instant = TargetMatcher(requiredConsecutiveFrames = 1)
        assertThat(instant.onFrame(listOf(Detection("Cup", 0.9f)), targets).newlyConfirmed).containsExactly(cup.id)
    }

    @Test
    fun `non-positive K is rejected`() {
        assertThrows(IllegalArgumentException::class.java) { TargetMatcher(0) }
    }
}
