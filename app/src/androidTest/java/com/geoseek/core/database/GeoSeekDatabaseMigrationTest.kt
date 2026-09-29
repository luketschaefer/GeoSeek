package com.geoseek.core.database

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Template for migration tests. When bumping the DB version to N:
 * 1. Build once so app/schemas/.../N.json is generated, and commit it.
 * 2. Add MIGRATION_(N-1)_N to DatabaseModule.MIGRATIONS.
 * 3. Add a test that creates version N-1, inserts rows, then calls runMigrationsAndValidate.
 */
@RunWith(AndroidJUnit4::class)
class GeoSeekDatabaseMigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), GeoSeekDatabase::class.java)

    @Test
    fun version1SchemaMatchesExportedJson() {
        helper.createDatabase(TEST_DB, 1).close()
        helper.runMigrationsAndValidate(TEST_DB, 1, true, *DatabaseModule.MIGRATIONS)
    }

    private companion object {
        const val TEST_DB = "migration-test"
    }
}
