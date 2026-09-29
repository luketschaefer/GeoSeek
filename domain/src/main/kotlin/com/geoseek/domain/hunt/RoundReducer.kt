package com.geoseek.domain.hunt

/**
 * Round state machine:
 *
 * ```
 * NOT_STARTED --Start--> RUNNING --all targets found--> WON
 *      |                    |------elapsed >= duration--> TIME_UP
 *      +------Abandon-------+------Abandon--------------> ABANDONED
 * ```
 *
 * Events that are not valid in the current state return the same instance unchanged, so callers
 * can dispatch freely (e.g. a late detector result after time is up) without guarding.
 */
object RoundReducer {
    fun reduce(
        round: Round,
        event: RoundEvent,
    ): Round =
        when (event) {
            RoundEvent.Start -> start(round)
            is RoundEvent.Tick -> tick(round, event)
            is RoundEvent.TargetFound -> targetFound(round, event)
            RoundEvent.Abandon -> abandon(round)
        }

    private fun start(round: Round): Round =
        if (round.status == RoundStatus.NOT_STARTED) round.copy(status = RoundStatus.RUNNING) else round

    private fun tick(
        round: Round,
        event: RoundEvent.Tick,
    ): Round {
        if (round.status != RoundStatus.RUNNING || event.elapsed <= round.elapsed) return round
        return if (event.elapsed >= round.config.duration) {
            round.copy(elapsed = round.config.duration, status = RoundStatus.TIME_UP)
        } else {
            round.copy(elapsed = event.elapsed)
        }
    }

    private fun targetFound(
        round: Round,
        event: RoundEvent.TargetFound,
    ): Round {
        if (round.status != RoundStatus.RUNNING) return round
        if (round.targets.none { it.id == event.id } || round.isFound(event.id)) return round
        val found = round.found + (event.id to round.elapsed)
        val status = if (found.size == round.targets.size) RoundStatus.WON else RoundStatus.RUNNING
        return round.copy(found = found, status = status)
    }

    private fun abandon(round: Round): Round =
        if (round.status.isTerminal) round else round.copy(status = RoundStatus.ABANDONED)
}
