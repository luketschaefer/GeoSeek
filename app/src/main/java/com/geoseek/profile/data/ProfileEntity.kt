package com.geoseek.profile.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.geoseek.domain.profile.Profile

/** Single-row table: the local player. */
@Entity(tableName = "profile")
data class ProfileEntity(
    @PrimaryKey val id: Int = LOCAL_PROFILE_ID,
    val displayName: String,
    val totalXp: Long,
) {
    fun toDomain() = Profile(displayName = displayName, totalXp = totalXp)

    companion object {
        const val LOCAL_PROFILE_ID = 0
    }
}
