package net.drehtuer.nextgallery.core.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import net.drehtuer.nextgallery.core.model.AccentColor
import net.drehtuer.nextgallery.core.model.RemotePaths
import net.drehtuer.nextgallery.core.model.ThemeMode
import net.drehtuer.nextgallery.core.model.UserSettings
import java.io.File
import java.io.IOException

/** Maps [UserSettings] to Preferences DataStore. A missing or unknown value reads as its default. */
class SettingsDataSource(private val dataStore: DataStore<Preferences>) {
    val settings: Flow<UserSettings> = dataStore.data
        .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
        .map(::read)
        .distinctUntilChanged()

    suspend fun update(transform: (UserSettings) -> UserSettings) {
        dataStore.edit { preferences -> write(preferences, transform(read(preferences))) }
    }

    private fun read(preferences: Preferences) = UserSettings(
        folderRules = preferences[FOLDER_RULES]?.mapNotNull(FolderRuleCodec::decode)?.sortedBy { it.relativePath }
            ?: UserSettings.DEFAULT_FOLDER_RULES,
        excludePatterns = preferences[EXCLUDE_PATTERNS]?.lines()?.filter { it.isNotEmpty() }.orEmpty(),
        gridColumns = preferences[GRID_COLUMNS]?.takeIf { it in UserSettings.GRID_COLUMN_OPTIONS }
            ?: UserSettings.DEFAULT_GRID_COLUMNS,
        themeMode = ThemeMode.entries.find { it.name == preferences[THEME_MODE] } ?: ThemeMode.SYSTEM,
        accentColor = AccentColor.entries.find { it.name == preferences[ACCENT_COLOR] } ?: AccentColor.BLUE,
        remoteRoot = preferences[REMOTE_ROOT] ?: RemotePaths.ROOT,
        tileUrl = preferences[TILE_URL]?.takeIf(TileUrls::isValid) ?: UserSettings.DEFAULT_TILE_URL,
        currentLocationAsked = preferences[CURRENT_LOCATION_ASKED] ?: false,
    )

    private fun write(preferences: MutablePreferences, settings: UserSettings) {
        preferences[FOLDER_RULES] = settings.folderRules.map(FolderRuleCodec::encode).toSet()
        preferences[EXCLUDE_PATTERNS] = settings.excludePatterns.joinToString("\n")
        preferences[GRID_COLUMNS] = settings.gridColumns
        preferences[THEME_MODE] = settings.themeMode.name
        preferences[ACCENT_COLOR] = settings.accentColor.name
        preferences[REMOTE_ROOT] = settings.remoteRoot
        preferences[TILE_URL] = settings.tileUrl
        preferences[CURRENT_LOCATION_ASKED] = settings.currentLocationAsked
    }

    companion object {
        private val FOLDER_RULES = stringSetPreferencesKey("folder_rules")
        private val EXCLUDE_PATTERNS = stringPreferencesKey("exclude_patterns")
        private val GRID_COLUMNS = intPreferencesKey("grid_columns")
        private val THEME_MODE = stringPreferencesKey("theme_mode")
        private val ACCENT_COLOR = stringPreferencesKey("accent_color")
        private val REMOTE_ROOT = stringPreferencesKey("remote_root")
        private val TILE_URL = stringPreferencesKey("tile_url")
        private val CURRENT_LOCATION_ASKED = booleanPreferencesKey("current_location_asked")

        /** A corrupt file is replaced by the defaults, because nothing in it is worth stopping the app for. */
        fun create(file: File) = SettingsDataSource(
            PreferenceDataStoreFactory.create(
                corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
                produceFile = { file },
            ),
        )
    }
}
