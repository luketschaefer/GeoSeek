package com.geoseek.core.database

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    /** All migrations, in order. Add new ones here (e.g. MIGRATION_1_2). */
    val MIGRATIONS = emptyArray<androidx.room.migration.Migration>()

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
    ): GeoSeekDatabase =
        Room
            .databaseBuilder(context, GeoSeekDatabase::class.java, GeoSeekDatabase.NAME)
            .addMigrations(*MIGRATIONS)
            .build()
}
