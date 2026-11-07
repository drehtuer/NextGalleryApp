package net.drehtuer.nextgallery.core.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Adds the cache of server listings (Phase 8). */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `remote_cache` (
                `path` TEXT NOT NULL,
                `parent_path` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `is_directory` INTEGER NOT NULL,
                `size_bytes` INTEGER NOT NULL,
                `content_type` TEXT,
                `etag` TEXT,
                `file_id` INTEGER,
                `checksums` TEXT,
                `permissions` TEXT,
                `has_preview` INTEGER NOT NULL,
                `child_count` INTEGER,
                `last_modified_ms` INTEGER NOT NULL,
                `fetched_at_ms` INTEGER NOT NULL,
                PRIMARY KEY(`path`)
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_remote_cache_parent_path` ON `remote_cache` (`parent_path`)")
    }
}

/** Adds the cached upload status of local images (Phase 9). */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `upload_state` (
                `media_id` INTEGER NOT NULL,
                `remote_path` TEXT NOT NULL,
                `state` INTEGER NOT NULL,
                `checksum_verified` INTEGER NOT NULL,
                `local_size` INTEGER NOT NULL,
                `local_date_modified_s` INTEGER NOT NULL,
                `remote_etag` TEXT,
                `remote_folder_etag` TEXT,
                `checked_at_ms` INTEGER NOT NULL,
                PRIMARY KEY(`media_id`),
                FOREIGN KEY(`media_id`) REFERENCES `media`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
    }
}
