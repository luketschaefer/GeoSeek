package com.geoseek.domain.quests

import com.geoseek.domain.catalog.CatalogObject
import com.geoseek.domain.catalog.Environment
import java.time.LocalDate

data class DailyQuest(
    val date: LocalDate,
    val environment: Environment,
    val target: CatalogObject,
    val bonusXp: Int,
)
