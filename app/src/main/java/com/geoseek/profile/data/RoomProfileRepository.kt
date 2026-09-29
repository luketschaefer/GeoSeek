package com.geoseek.profile.data

import androidx.room.withTransaction
import com.geoseek.core.database.GeoSeekDatabase
import com.geoseek.domain.profile.Profile
import com.geoseek.domain.profile.ProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject

class RoomProfileRepository
    @Inject
    constructor(
        private val db: GeoSeekDatabase,
    ) : ProfileRepository {
        private val dao = db.profileDao()

        override fun observeProfile(): Flow<Profile> =
            dao
                .observe()
                .onStart { dao.ensureExists() }
                .filterNotNull()
                .map { it.toDomain() }

        override suspend fun setDisplayName(name: String) {
            require(name.isNotBlank()) { "Display name must not be blank" }
            db.withTransaction {
                dao.ensureExists()
                dao.setDisplayName(name.trim())
            }
        }

        override suspend fun addXp(amount: Int) {
            require(amount >= 0) { "XP amount must be >= 0, was $amount" }
            db.withTransaction {
                dao.ensureExists()
                dao.addXp(amount.toLong())
            }
        }
    }
