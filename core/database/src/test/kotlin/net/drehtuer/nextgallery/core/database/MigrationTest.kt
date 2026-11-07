package net.drehtuer.nextgallery.core.database

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
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

    @Test
    fun migrate2To3_matchesExportedSchema() {
        helper.createDatabase(TEST_DB, 2).close()

        helper.runMigrationsAndValidate(TEST_DB, 3, true, MIGRATION_2_3).close()
    }

    @Test
    fun migrate2To3_keepsTheIndex() {
        helper.createDatabase(TEST_DB, 2).use { db -> insertMedia(db, id = 7) }

        helper.runMigrationsAndValidate(TEST_DB, 3, true, MIGRATION_2_3).use { db ->
            db.query("SELECT id FROM media").use { cursor ->
                cursor.moveToFirst()
                assertEquals(7, cursor.getLong(0))
            }
        }
    }

    @Test
    fun migrate2To3_uploadStateStartsEmpty() {
        helper.createDatabase(TEST_DB, 2).use { db -> insertMedia(db, id = 7) }

        helper.runMigrationsAndValidate(TEST_DB, 3, true, MIGRATION_2_3).use { db ->
            db.query("SELECT COUNT(*) FROM upload_state").use { cursor ->
                cursor.moveToFirst()
                assertEquals(0, cursor.getInt(0))
            }
        }
    }

    @Test
    fun migrate2To3_deletingAnImageDeletesItsUploadState() {
        helper.createDatabase(TEST_DB, 2).use { db -> insertMedia(db, id = 7) }

        helper.runMigrationsAndValidate(TEST_DB, 3, true, MIGRATION_2_3).use { db ->
            db.execSQL("PRAGMA foreign_keys = ON")
            db.execSQL(
                "INSERT INTO upload_state (media_id, remote_path, state, checksum_verified, local_size, " +
                    "local_date_modified_s, checked_at_ms) VALUES (7, '/P/a.jpg', 2, 0, 1, 1, 1)",
            )
            db.execSQL("DELETE FROM media WHERE id = 7")
            db.query("SELECT COUNT(*) FROM upload_state").use { cursor ->
                cursor.moveToFirst()
                assertEquals(0, cursor.getInt(0))
            }
        }
    }

    private fun insertMedia(db: SupportSQLiteDatabase, id: Long) {
        db.execSQL(
            "INSERT INTO media (id, volume_name, content_uri, bucket_id, bucket_name, relative_path, display_name, " +
                "mime_type, size_bytes, width, height, orientation, date_taken_ms, date_source, date_modified_s, " +
                "generation_modified, local_year, local_month, local_day, exif_scanned) VALUES " +
                "($id, 'v', 'content://$id', 1, 'B', 'B/', 'a.jpg', 'image/jpeg', 1, 1, 1, 0, 1, 'DATE_TAKEN', 1, " +
                "1, 2024, 3, 15, 0)",
        )
    }

    private companion object {
        const val TEST_DB = "migration-test.db"
    }
}
