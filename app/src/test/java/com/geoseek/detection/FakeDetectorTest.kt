package com.geoseek.detection

import com.geoseek.domain.detection.Detection
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test

class FakeDetectorTest {
    private val cup = listOf(Detection("Cup", 0.9f))
    private val dog = listOf(Detection("Dog", 0.8f))

    @Test
    fun `plays script then falls back to emitted value`() {
        val fake = FakeDetector(script = listOf(cup, dog))
        assertThat(fake.nextFrame()).isEqualTo(cup)
        assertThat(fake.nextFrame()).isEqualTo(dog)
        assertThat(fake.nextFrame()).isEmpty()
        fake.emit(dog)
        assertThat(fake.nextFrame()).isEqualTo(dog)
    }

    @Test
    fun `loops when asked`() {
        val fake = FakeDetector(script = listOf(cup, dog), loop = true)
        assertThat((1..5).map { fake.nextFrame() }).containsExactly(cup, dog, cup, dog, cup).inOrder()
    }

    @Test
    fun `closed detector rejects frames`() {
        val fake = FakeDetector()
        fake.close()
        assertThrows(IllegalStateException::class.java) { fake.nextFrame() }
    }
}
