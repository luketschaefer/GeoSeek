package com.geoseek.domain.quests

import com.geoseek.domain.catalog.Catalog
import com.geoseek.domain.catalog.Environment
import com.geoseek.domain.catalog.Rarity
import com.geoseek.domain.util.SeededRandom
import java.time.LocalDate

/**
 * Deterministic daily quest: everyone with the same catalog gets the same quest for the same
 * calendar date. The app passes the device's *local* date, so the quest rolls over at local
 * midnight. Legendary objects are excluded to keep daily quests achievable.
 *
 * Note: the result depends on catalog contents, so players on different catalog versions can
 * see different quests. Move selection server-side once a backend exists.
 */
class DailyQuestSelector(
    private val catalog: Catalog,
) {
    fun questFor(date: LocalDate): DailyQuest {
        val random = SeededRandom(date.toEpochDay() xor SALT)
        val environments = Environment.entries.filter { env -> candidates(env).isNotEmpty() }
        check(environments.isNotEmpty()) { "No environment has a non-legendary object" }
        val environment = environments[random.nextInt(environments.size)]
        val pool = candidates(environment)
        val target = pool[random.nextInt(pool.size)]
        return DailyQuest(date = date, environment = environment, target = target, bonusXp = BASE_BONUS_XP + target.xp)
    }

    private fun candidates(environment: Environment) =
        catalog.objectsIn(environment).filter { it.rarity != Rarity.LEGENDARY }

    companion object {
        const val BASE_BONUS_XP = 50
        private const val SALT = 0x6E05EE4L
    }
}
