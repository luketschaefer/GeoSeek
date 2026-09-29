package com.geoseek.domain.profile

import kotlinx.coroutines.flow.Flow

interface ProfileRepository {
    /** Emits the profile, creating a default one on first access. */
    fun observeProfile(): Flow<Profile>

    suspend fun setDisplayName(name: String)

    /** For XP not tied to a card (round completion, daily quest). Card XP goes through CollectionRepository. */
    suspend fun addXp(amount: Int)
}
