package net.drehtuer.nextgallery.core.data.settings

import kotlinx.coroutines.flow.Flow
import net.drehtuer.nextgallery.core.common.AppResult
import net.drehtuer.nextgallery.core.common.RelativePaths
import net.drehtuer.nextgallery.core.common.runCatchingCancellable
import net.drehtuer.nextgallery.core.model.AccentColor
import net.drehtuer.nextgallery.core.model.FolderRule
import net.drehtuer.nextgallery.core.model.RemotePaths
import net.drehtuer.nextgallery.core.model.ThemeMode
import net.drehtuer.nextgallery.core.model.UserSettings
import javax.inject.Inject

interface SettingsRepository {
    fun observeSettings(): Flow<UserSettings>

    /** A folder has at most one rule: this replaces an existing rule for the same path. */
    suspend fun addRule(rule: FolderRule): AppResult<Unit>

    suspend fun removeRule(relativePath: String): AppResult<Unit>

    suspend fun setExcludePatterns(patterns: List<String>): AppResult<Unit>

    /** [columns] must be one of [UserSettings.GRID_COLUMN_OPTIONS]. */
    suspend fun setGridColumns(columns: Int): AppResult<Unit>

    suspend fun setThemeMode(mode: ThemeMode): AppResult<Unit>

    suspend fun setAccentColor(color: AccentColor): AppResult<Unit>

    /** The server folder the local folders map to (D13, used for the upload status in Phase 9). */
    suspend fun setRemoteRoot(path: String): AppResult<Unit>
}

class DefaultSettingsRepository @Inject constructor(private val dataSource: SettingsDataSource) : SettingsRepository {
    override fun observeSettings(): Flow<UserSettings> = dataSource.settings

    override suspend fun addRule(rule: FolderRule): AppResult<Unit> = update { settings ->
        val normalized = rule.copy(relativePath = RelativePaths.normalize(rule.relativePath))
        settings.copy(folderRules = settings.folderRules.withoutPath(rule.relativePath) + normalized)
    }

    override suspend fun removeRule(relativePath: String): AppResult<Unit> = update { settings ->
        settings.copy(folderRules = settings.folderRules.withoutPath(relativePath))
    }

    override suspend fun setExcludePatterns(patterns: List<String>): AppResult<Unit> =
        update { it.copy(excludePatterns = patterns) }

    override suspend fun setGridColumns(columns: Int): AppResult<Unit> {
        require(columns in UserSettings.GRID_COLUMN_OPTIONS) { "Unsupported column count: $columns" }
        return update { it.copy(gridColumns = columns) }
    }

    override suspend fun setThemeMode(mode: ThemeMode): AppResult<Unit> = update { it.copy(themeMode = mode) }

    override suspend fun setAccentColor(color: AccentColor): AppResult<Unit> = update { it.copy(accentColor = color) }

    override suspend fun setRemoteRoot(path: String): AppResult<Unit> =
        update { it.copy(remoteRoot = RemotePaths.normalize(path)) }

    private suspend fun update(transform: (UserSettings) -> UserSettings): AppResult<Unit> =
        runCatchingCancellable { dataSource.update(transform) }

    private fun List<FolderRule>.withoutPath(path: String) = filterNot {
        RelativePaths.isUnder(it.relativePath, path) && RelativePaths.isUnder(path, it.relativePath)
    }
}
