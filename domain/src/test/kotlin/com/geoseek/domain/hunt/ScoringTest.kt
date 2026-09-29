package com.geoseek.domain.hunt

import com.geoseek.domain.catalog.Environment
import com.geoseek.domain.testObject
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class ScoringTest {
    private val a = testObject("a", points = 10)
    private val b = testObject("b", points = 50)
    private val running =
        RoundReducer.reduce(
            Round(RoundConfig(Environment.PARK, seed = 0, targetCount = 2, duration = 1.minutes), listOf(a, b)),
            RoundEvent.Start,
        )

    private fun Round.on(vararg events: RoundEvent) = events.fold(this, RoundReducer::reduce)

    @Test
    fun `won round gets points, completion bonus and time bonus`() {
        val won = running.on(RoundEvent.TargetFound(a.id), RoundEvent.Tick(20.seconds), RoundEvent.TargetFound(b.id))
        assertThat(
            Scoring.score(won),
        ).isEqualTo(ScoreBreakdown(targetPoints = 60, completionBonus = 30, timeBonus = 80))
        assertThat(Scoring.score(won).total).isEqualTo(170)
    }

    @Test
    fun `time up keeps found points only`() {
        val timeUp = running.on(RoundEvent.TargetFound(b.id), RoundEvent.Tick(1.minutes))
        assertThat(Scoring.score(timeUp)).isEqualTo(ScoreBreakdown(50, 0, 0))
    }

    @Test
    fun `abandoned scores zero even with finds`() {
        assertThat(
            Scoring.score(running.on(RoundEvent.TargetFound(b.id), RoundEvent.Abandon)),
        ).isEqualTo(ScoreBreakdown.ZERO)
    }

    @Test
    fun `time bonus uses whole remaining seconds`() {
        val won =
            running.on(
                RoundEvent.Tick(59_500.milliseconds),
                RoundEvent.TargetFound(a.id),
                RoundEvent.TargetFound(b.id),
            )
        assertThat(Scoring.score(won).timeBonus).isEqualTo(0)
    }

    @Test
    fun `running round shows partial points`() {
        assertThat(Scoring.score(running.on(RoundEvent.TargetFound(a.id))).total).isEqualTo(10)
    }
}
