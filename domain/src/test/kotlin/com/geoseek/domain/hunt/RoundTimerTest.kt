package com.geoseek.domain.hunt

import app.cash.turbine.test
import com.geoseek.domain.catalog.Environment
import com.geoseek.domain.testObject
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class RoundTimerTest {
    @Test
    fun `emits each second then completes at duration`() =
        runTest {
            RoundTimer(1.seconds).ticks(3.seconds).test {
                assertThat(awaitItem()).isEqualTo(1.seconds)
                assertThat(awaitItem()).isEqualTo(2.seconds)
                assertThat(awaitItem()).isEqualTo(3.seconds)
                awaitComplete()
            }
            assertThat(currentTime).isEqualTo(3_000)
        }

    @Test
    fun `last tick is shortened to land exactly on duration`() =
        runTest {
            val ticks = RoundTimer(1.seconds).ticks(2500.milliseconds).toList()
            assertThat(ticks).containsExactly(1.seconds, 2.seconds, 2500.milliseconds).inOrder()
            assertThat(currentTime).isEqualTo(2_500)
        }

    @Test
    fun `nothing is emitted before the first interval passes`() =
        runTest {
            val seen = mutableListOf<Duration>()
            val job = launch { RoundTimer(1.seconds).ticks(10.seconds).collect { seen += it } }
            advanceTimeBy(999)
            runCurrent()
            assertThat(seen).isEmpty()
            advanceTimeBy(1)
            runCurrent()
            assertThat(seen).containsExactly(1.seconds)
            job.cancel()
        }

    @Test
    fun `resumes from startAt`() =
        runTest {
            val ticks = RoundTimer(1.seconds).ticks(5.seconds, startAt = 3.seconds).toList()
            assertThat(ticks).containsExactly(4.seconds, 5.seconds).inOrder()
        }

    @Test
    fun `startAt at or past duration completes immediately`() =
        runTest {
            assertThat(RoundTimer().ticks(5.seconds, startAt = 9.seconds).toList()).isEmpty()
        }

    @Test
    fun `driving the reducer with the timer ends in time up`() =
        runTest {
            val start =
                Round(
                    config =
                        RoundConfig(
                            environment = Environment.PARK,
                            seed = 0,
                            targetCount = 1,
                            duration = 3.seconds,
                        ),
                    targets = listOf(testObject("a")),
                ).let { RoundReducer.reduce(it, RoundEvent.Start) }
            var round = start
            RoundTimer().ticks(start.config.duration).collect { elapsed ->
                round = RoundReducer.reduce(round, RoundEvent.Tick(elapsed))
            }
            assertThat(round.status).isEqualTo(RoundStatus.TIME_UP)
        }
}
