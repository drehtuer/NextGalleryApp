package net.drehtuer.nextgallery.core.database

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
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

    private companion object {
        const val TEST_DB = "migration-test.db"
    }
}
