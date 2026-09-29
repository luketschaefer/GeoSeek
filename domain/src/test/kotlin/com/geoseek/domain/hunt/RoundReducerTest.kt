package com.geoseek.domain.hunt

import com.geoseek.domain.catalog.Environment
import com.geoseek.domain.catalog.ObjectId
import com.geoseek.domain.testObject
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class RoundReducerTest {
    private val a = testObject("a")
    private val b = testObject("b")
    private val notStarted =
        Round(
            config = RoundConfig(Environment.PARK, seed = 0, targetCount = 2, duration = 1.minutes),
            targets = listOf(a, b),
        )
    private val running = RoundReducer.reduce(notStarted, RoundEvent.Start)

    private fun Round.on(vararg events: RoundEvent) = events.fold(this, RoundReducer::reduce)

    @Test
    fun `start moves not started to running`() {
        assertThat(running.status).isEqualTo(RoundStatus.RUNNING)
    }

    @Test
    fun `finding all targets wins and records find times`() {
        val round =
            running.on(
                RoundEvent.Tick(5.seconds),
                RoundEvent.TargetFound(a.id),
                RoundEvent.Tick(12.seconds),
                RoundEvent.TargetFound(b.id),
            )
        assertThat(round.status).isEqualTo(RoundStatus.WON)
        assertThat(round.found).containsExactly(a.id, 5.seconds, b.id, 12.seconds)
        assertThat(round.remaining).isEqualTo(48.seconds)
    }

    @Test
    fun `reaching duration is time up and clamps elapsed`() {
        val round = running.on(RoundEvent.TargetFound(a.id), RoundEvent.Tick(90.seconds))
        assertThat(round.status).isEqualTo(RoundStatus.TIME_UP)
        assertThat(round.elapsed).isEqualTo(1.minutes)
        assertThat(round.remaining).isEqualTo(0.seconds)
    }

    @Test
    fun `abandon from running and from not started`() {
        assertThat(running.on(RoundEvent.Abandon).status).isEqualTo(RoundStatus.ABANDONED)
        assertThat(notStarted.on(RoundEvent.Abandon).status).isEqualTo(RoundStatus.ABANDONED)
    }

    @Test
    fun `terminal states ignore all events`() {
        val terminals =
            listOf(
                running.on(RoundEvent.TargetFound(a.id), RoundEvent.TargetFound(b.id)),
                running.on(RoundEvent.Tick(1.minutes)),
                running.on(RoundEvent.Abandon),
            )
        val events =
            listOf(RoundEvent.Start, RoundEvent.Tick(2.minutes), RoundEvent.TargetFound(a.id), RoundEvent.Abandon)
        terminals.forEach { terminal ->
            events.forEach { event -> assertThat(RoundReducer.reduce(terminal, event)).isSameInstanceAs(terminal) }
        }
    }

    @Test
    fun `not started ignores ticks and finds`() {
        assertThat(notStarted.on(RoundEvent.Tick(5.seconds))).isSameInstanceAs(notStarted)
        assertThat(notStarted.on(RoundEvent.TargetFound(a.id))).isSameInstanceAs(notStarted)
    }

    @Test
    fun `start while running is ignored`() {
        assertThat(running.on(RoundEvent.Start)).isSameInstanceAs(running)
    }

    @Test
    fun `unknown and duplicate finds are ignored`() {
        val once = running.on(RoundEvent.TargetFound(a.id))
        assertThat(once.on(RoundEvent.TargetFound(a.id))).isSameInstanceAs(once)
        assertThat(once.on(RoundEvent.TargetFound(ObjectId("zzz")))).isSameInstanceAs(once)
    }

    @Test
    fun `stale or equal ticks are ignored`() {
        val at10 = running.on(RoundEvent.Tick(10.seconds))
        assertThat(at10.on(RoundEvent.Tick(3.seconds))).isSameInstanceAs(at10)
        assertThat(at10.on(RoundEvent.Tick(10.seconds))).isSameInstanceAs(at10)
    }
}
