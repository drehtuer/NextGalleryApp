package net.drehtuer.nextgallery.core.model

/**
 * Everything the user can set. To add a setting: a field here, its key in `SettingsDataSource` and a setter in
 * `SettingsRepository`.
 */
data class UserSettings(
    val folderRules: List<FolderRule> = DEFAULT_FOLDER_RULES,
    val excludePatterns: List<String> = emptyList(),
    val gridColumns: Int = DEFAULT_GRID_COLUMNS,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val accentColor: AccentColor = AccentColor.BLUE,
    val remoteRoot: String = RemotePaths.ROOT,
) {
    companion object {
        const val DEFAULT_GRID_COLUMNS = 3

        /** The choices in Appearance (D51). */
        val GRID_COLUMN_OPTIONS = listOf(DEFAULT_GRID_COLUMNS, 4, 5)

        /** The rules of a new installation. They are ordinary rules, so the user can remove them. */
        val DEFAULT_FOLDER_RULES = listOf(
            FolderRule("Android/data", FolderRule.Type.EXCLUDE),
            FolderRule("Android/media/com.whatsapp/WhatsApp/Media/.Statuses", FolderRule.Type.EXCLUDE),
        )
    }
}
