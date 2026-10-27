package com.example.geoseek.engine

import com.example.geoseek.models.GameObject

/** Local practice rules. Battles never consume collected cards or change account progress. */
data class DuelStats(val health: Int, val attack: Int)

fun GameObject.duelStats(): DuelStats = DuelStats(
    health = 60 + rarity.ordinal * 15,
    attack = 12 + rarity.ordinal * 3 + points.coerceIn(0, 100) / 10,
)

data class DuelState(
    val playerHealth: Int,
    val rivalHealth: Int,
    val recoveries: Int = 2,
    val turn: Int = 1,
) {
    val finished: Boolean get() = playerHealth <= 0 || rivalHealth <= 0

    /** Player acts first; a defeated rival cannot counterattack. Recovery restores 20 HP. */
    fun play(player: DuelStats, rival: DuelStats, recover: Boolean = false): DuelState {
        if (finished || (recover && recoveries == 0)) return this
        val nextRivalHealth = if (recover) rivalHealth else (rivalHealth - player.attack).coerceAtLeast(0)
        val healed = if (recover) (playerHealth + 20).coerceAtMost(player.health) else playerHealth
        return copy(
            playerHealth = if (nextRivalHealth > 0) (healed - rival.attack).coerceAtLeast(0) else healed,
            rivalHealth = nextRivalHealth,
            recoveries = recoveries - if (recover) 1 else 0,
            turn = turn + 1,
        )
    }
}
