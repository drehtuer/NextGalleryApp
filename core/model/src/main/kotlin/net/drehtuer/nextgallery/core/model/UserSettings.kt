package net.drehtuer.nextgallery.core.model

/**
 * Everything the user can set. To add a setting: a field here, its key in `SettingsDataSource` and a setter in
 * `SettingsRepository`.
 */
data class UserSettings(
    /** Only these folders are shown; a new installation has none (D55). */
    val addedFolders: List<AddedFolder> = emptyList(),
    val virtualFolders: List<VirtualFolder> = emptyList(),
    val gridColumns: Int = DEFAULT_GRID_COLUMNS,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val accentColor: AccentColor = AccentColor.BLUE,
    val remoteRoot: String = RemotePaths.ROOT,
    val tileUrl: String = DEFAULT_TILE_URL,
    /** Android's location prompt is shown once, the first time the map opens (D44). */
    val currentLocationAsked: Boolean = false,
) {
    companion object {
        const val DEFAULT_GRID_COLUMNS = 3

        /** The OpenStreetMap tile server (D23). */
        const val DEFAULT_TILE_URL = "https://tile.openstreetmap.org/{z}/{x}/{y}.png"

        /** The choices in Appearance (D51). */
        val GRID_COLUMN_OPTIONS = listOf(DEFAULT_GRID_COLUMNS, 4, 5)
    }
}
