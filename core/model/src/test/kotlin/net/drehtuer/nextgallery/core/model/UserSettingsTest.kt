package net.drehtuer.nextgallery.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class UserSettingsTest {
    @Test
    fun defaults_gridHasThreeColumns() {
        assertEquals(3, UserSettings().gridColumns)
    }

    @Test
    fun gridColumnOptions_areThreeFourAndFive() {
        assertEquals(listOf(3, 4, 5), UserSettings.GRID_COLUMN_OPTIONS)
    }

    @Test
    fun defaults_themeFollowsTheSystem() {
        assertEquals(ThemeMode.SYSTEM, UserSettings().themeMode)
    }

    @Test
    fun defaults_accentIsBlue() {
        assertEquals(AccentColor.BLUE, UserSettings().accentColor)
    }

    @Test
    fun defaults_noFolderIsAdded() {
        assertEquals(emptyList<AddedFolder>(), UserSettings().addedFolders)
    }

    @Test
    fun defaults_noVirtualFolderExists() {
        assertEquals(emptyList<VirtualFolder>(), UserSettings().virtualFolders)
    }
}
