package com.geoseek.profile.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.geoseek.domain.profile.Profile
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    @Query("SELECT * FROM profile WHERE id = 0")
    fun observe(): Flow<ProfileEntity?>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(profile: ProfileEntity)

    suspend fun ensureExists() = insertIfAbsent(ProfileEntity(displayName = Profile.DEFAULT_DISPLAY_NAME, totalXp = 0))

    @Query("UPDATE profile SET totalXp = totalXp + :amount WHERE id = 0")
    suspend fun addXp(amount: Long)

    @Query("UPDATE profile SET displayName = :name WHERE id = 0")
    suspend fun setDisplayName(name: String)
}
