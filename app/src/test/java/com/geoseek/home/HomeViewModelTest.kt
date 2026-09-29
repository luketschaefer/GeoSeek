package com.geoseek.home

import com.geoseek.domain.catalog.CatalogParser
import com.geoseek.domain.profile.Profile
import com.geoseek.domain.quests.DailyQuestSelector
import com.geoseek.quests.TodaysQuestProvider
import com.geoseek.testing.FakeProfileRepository
import com.geoseek.testing.MainDispatcherRule
import com.geoseek.testing.bundledCatalogJson
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class HomeViewModelTest {
    @get:Rule
    val mainRule = MainDispatcherRule()

    private val catalog = CatalogParser().parse(bundledCatalogJson())
    private val selector = DailyQuestSelector(catalog)
    private val profiles = FakeProfileRepository(Profile("Ada", totalXp = 150))

    @Test
    fun `shows profile level and today's quest for the local date`() =
        runTest {
            // 23:30 in Berlin on the 28th is already the 29th in UTC: the local date must win.
            val zone = ZoneId.of("Europe/Berlin")
            val clock = Clock.fixed(Instant.parse("2026-09-28T21:30:00Z"), zone)
            val vm = HomeViewModel(profiles, TodaysQuestProvider(selector, clock))
            backgroundScope.launch { vm.uiState.collect {} }

            val state = vm.uiState.first { !it.isLoading }
            assertThat(state.displayName).isEqualTo("Ada")
            assertThat(state.level).isEqualTo(2)
            assertThat(state.levelFraction).isWithin(1e-6f).of(0.25f)
            val expected = selector.questFor(LocalDate.of(2026, 9, 28))
            assertThat(state.quest?.targetName).isEqualTo(expected.target.name)
            assertThat(state.quest?.bonusXp).isEqualTo(expected.bonusXp)
        }

    @Test
    fun `updates when xp changes`() =
        runTest {
            val vm = HomeViewModel(profiles, TodaysQuestProvider(selector, Clock.systemUTC()))
            backgroundScope.launch { vm.uiState.collect {} }
            profiles.addXp(150)
            assertThat(vm.uiState.first { it.level == 3 }.levelFraction).isEqualTo(0f)
        }
}
