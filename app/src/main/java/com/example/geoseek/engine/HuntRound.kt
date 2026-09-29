// Runs a single hunt: holds the timer, the targets to find, and the score.
package com.example.geoseek.engine

import com.example.geoseek.models.GameObject

class HuntRound(
    val environment: String,
    val targets: List<GameObject>,
    val durationSeconds: Int,
) {
    var secondsLeft: Int = durationSeconds
    var score: Int = 0
    val found = mutableListOf<GameObject>()

    fun start() {
        // TODO: Start the countdown timer
    }

    fun onObjectFound(obj: GameObject) {
        // TODO: If obj is a target and not found yet, add it to found and add its points to score
    }

    fun isOver(): Boolean {
        // TODO: Round ends when time runs out or all targets are found
        return false
    }
}
