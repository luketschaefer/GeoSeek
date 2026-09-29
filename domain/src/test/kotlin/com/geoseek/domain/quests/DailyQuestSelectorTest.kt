package com.geoseek.domain.quests

import com.geoseek.domain.catalog.Rarity
import com.geoseek.domain.testCatalog
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate

class DailyQuestSelectorTest {
    private val selector = DailyQuestSelector(testCatalog)

    @Test
    fun `same date gives same quest across instances`() {
        val date = LocalDate.of(2026, 9, 28)
        assertThat(DailyQuestSelector(testCatalog).questFor(date)).isEqualTo(selector.questFor(date))
    }

    @Test
    fun `golden output locks the algorithm`() {
        // Every player must see the same quest; changing this is a player-visible change.
        val quest = selector.questFor(LocalDate.of(2026, 9, 28))
        assertThat("${quest.environment}/${quest.target.id}").isEqualTo(GOLDEN_2026_09_28)
    }

    @Test
    fun `quests vary across dates`() {
        val start = LocalDate.of(2026, 1, 1)
        val targets = (0L until 60L).map { selector.questFor(start.plusDays(it)).target.id }.toSet()
        assertThat(targets.size).isGreaterThan(10)
    }

    @Test
    fun `never picks legendary and bonus includes target xp`() {
        val start = LocalDate.of(2026, 1, 1)
        (0L until 365L).map { selector.questFor(start.plusDays(it)) }.forEach { quest ->
            assertThat(quest.target.rarity).isNotEqualTo(Rarity.LEGENDARY)
            assertThat(quest.environment).isIn(quest.target.environments)
            assertThat(quest.bonusXp).isEqualTo(DailyQuestSelector.BASE_BONUS_XP + quest.target.xp)
        }
    }

    private companion object {
        const val GOLDEN_2026_09_28 = "PARK/park_2"
    }
}
