package com.geoseek.quests

import com.geoseek.domain.quests.DailyQuest
import com.geoseek.domain.quests.DailyQuestSelector
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/** Today's quest for the device's local date (rolls over at local midnight). */
class TodaysQuestProvider
    @Inject
    constructor(
        private val selector: DailyQuestSelector,
        private val clock: Clock,
    ) {
        fun today(): DailyQuest = selector.questFor(LocalDate.now(clock))
    }
