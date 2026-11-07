package net.drehtuer.nextgallery.core.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import app.cash.turbine.test
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import net.drehtuer.nextgallery.core.model.AccentColor
import net.drehtuer.nextgallery.core.model.AddedFolder
import net.drehtuer.nextgallery.core.model.ThemeMode
import net.drehtuer.nextgallery.core.model.UserSettings
import net.drehtuer.nextgallery.core.model.VirtualFolder
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.IOException

class SettingsDataSourceTest {
    @get:Rule
    val folder = TemporaryFolder()

    private fun file(): File = File(folder.root, "settings.preferences_pb")

    private fun dataSource() = SettingsDataSource.create(file())

    @Test
    fun settings_noFile_readsDefaults() = runTest {
        assertEquals(UserSettings(), dataSource().settings.first())
    }

    @Test
    fun update_tileUrl_isReadBack() = runTest {
        val source = dataSource()
        val url = "https://tiles.example.org/{z}/{x}/{y}.png"

        source.update { it.copy(tileUrl = url) }

        assertEquals(url, source.settings.first().tileUrl)
    }

    @Test
    fun settings_invalidStoredTileUrl_readsDefault() = runTest {
        val source = dataSource()

        source.update { it.copy(tileUrl = "http://not-allowed.example.org") }

        assertEquals(UserSettings.DEFAULT_TILE_URL, source.settings.first().tileUrl)
    }

    @Test
    fun update_currentLocationAsked_isReadBack() = runTest {
        val source = dataSource()

        source.update { it.copy(currentLocationAsked = true) }

        assertEquals(true, source.settings.first().currentLocationAsked)
    }

    @Test
    fun settings_corruptFile_readsDefaults() = runTest {
        file().writeText("this is not a preferences file")

        assertEquals(UserSettings(), dataSource().settings.first())
    }

    @Test
    fun settings_readFailure_readsDefaults() = runTest {
        val dataStore = mockk<DataStore<Preferences>> {
            every { data } returns flow { throw IOException("disk") }
        }

        assertEquals(UserSettings(), SettingsDataSource(dataStore).settings.first())
    }

    @Test(expected = IllegalStateException::class)
    fun settings_otherFailure_isNotHidden() = runTest {
        val dataStore = mockk<DataStore<Preferences>> {
            every { data } returns flow { error("bug") }
        }

        SettingsDataSource(dataStore).settings.first()
    }

    @Test
    fun update_addedFolders_areReadBackSortedByPath() = runTest {
        val folders = listOf(AddedFolder("external_primary", "Pictures"), AddedFolder("external_primary", "DCIM"))
        val dataSource = dataSource()

        dataSource.update { it.copy(addedFolders = folders) }

        assertEquals(folders.reversed(), dataSource.settings.first().addedFolders)
    }

    @Test
    fun update_virtualFolders_areReadBackSortedByName() = runTest {
        val folders = listOf(VirtualFolder("v1", "Trips", listOf(7, 8)), VirtualFolder("v2", "All", listOf(9)))
        val dataSource = dataSource()

        dataSource.update { it.copy(virtualFolders = folders) }

        assertEquals(folders.reversed(), dataSource.settings.first().virtualFolders)
    }

    @Test
    fun update_gridColumns_isReadBack() = runTest {
        val dataSource = dataSource()

        dataSource.update { it.copy(gridColumns = 5) }

        assertEquals(5, dataSource.settings.first().gridColumns)
    }

    @Test
    fun update_unsupportedGridColumns_readsDefault() = runTest {
        val dataSource = dataSource()

        dataSource.update { it.copy(gridColumns = 9) }

        assertEquals(UserSettings.DEFAULT_GRID_COLUMNS, dataSource.settings.first().gridColumns)
    }

    @Test
    fun update_themeMode_isReadBack() = runTest {
        val dataSource = dataSource()

        dataSource.update { it.copy(themeMode = ThemeMode.DARK) }

        assertEquals(ThemeMode.DARK, dataSource.settings.first().themeMode)
    }

    @Test
    fun update_remoteRoot_isReadBack() = runTest {
        val dataSource = dataSource()

        dataSource.update { it.copy(remoteRoot = "/Photos") }

        assertEquals("/Photos", dataSource.settings.first().remoteRoot)
    }

    @Test
    fun update_accentColor_isReadBack() = runTest {
        val dataSource = dataSource()

        dataSource.update { it.copy(accentColor = AccentColor.CORAL) }

        assertEquals(AccentColor.CORAL, dataSource.settings.first().accentColor)
    }

    @Test
    fun settings_afterUpdate_emitsNewValue() = runTest {
        val dataSource = dataSource()

        dataSource.settings.test {
            assertEquals(ThemeMode.SYSTEM, awaitItem().themeMode)

            dataSource.update { it.copy(themeMode = ThemeMode.LIGHT) }

            assertEquals(ThemeMode.LIGHT, awaitItem().themeMode)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
