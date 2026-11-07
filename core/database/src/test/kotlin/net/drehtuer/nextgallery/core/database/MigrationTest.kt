package net.drehtuer.nextgallery.core.database

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Base for migration tests: every schema version is created from the exported JSON in `schemas/`. */
@RunWith(AndroidJUnit4::class)
class MigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), NextGalleryDatabase::class.java)

    @Test
    fun createDatabase_version1_matchesExportedSchema() {
        helper.createDatabase(TEST_DB, 1).close()
    }

    @Test
    fun migrate1To2_matchesExportedSchema() {
        helper.createDatabase(TEST_DB, 1).close()

        helper.runMigrationsAndValidate(TEST_DB, 2, true, MIGRATION_1_2).close()
    }

    @Test
    fun migrate1To2_keepsTheIndex() {
        helper.createDatabase(TEST_DB, 1).use { db ->
            db.execSQL(
                "INSERT INTO sync_state (volume_name, last_generation, media_store_version, last_full_scan_ms) " +
                    "VALUES ('external_primary', 12, 'v1', 5000)",
            )
        }

        helper.runMigrationsAndValidate(TEST_DB, 2, true, MIGRATION_1_2).use { db ->
            db.query("SELECT last_generation FROM sync_state").use { cursor ->
                cursor.moveToFirst()
                assertEquals(12, cursor.getLong(0))
            }
        }
    }

    @Test
    fun migrate1To2_remoteCacheStartsEmpty() {
        helper.createDatabase(TEST_DB, 1).close()

        helper.runMigrationsAndValidate(TEST_DB, 2, true, MIGRATION_1_2).use { db ->
            db.query("SELECT COUNT(*) FROM remote_cache").use { cursor ->
                cursor.moveToFirst()
                assertEquals(0, cursor.getInt(0))
            }
        }
    }

    private companion object {
        const val TEST_DB = "migration-test.db"
    }
}
