package com.geoseek.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.geoseek.collector.data.CardDao
import com.geoseek.collector.data.CardEntity
import com.geoseek.hunt.data.RoundHistoryDao
import com.geoseek.hunt.data.RoundRecordEntity
import com.geoseek.profile.data.ProfileDao
import com.geoseek.profile.data.ProfileEntity

/**
 * Single app database. Migration policy (see CLAUDE.md):
 * - Bump [version] for every schema change and commit the generated JSON in app/schemas/.
 * - Add a Migration (or an AutoMigration spec) plus a MigrationTestHelper test.
 * - Never use fallbackToDestructiveMigration: collections are player progress.
 */
@Database(
    entities = [CardEntity::class, ProfileEntity::class, RoundRecordEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class GeoSeekDatabase : RoomDatabase() {
    abstract fun cardDao(): CardDao

    abstract fun profileDao(): ProfileDao

    abstract fun roundHistoryDao(): RoundHistoryDao

    companion object {
        const val NAME = "geoseek.db"
    }
}
