package net.drehtuer.nextgallery.core.data.settings

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import net.drehtuer.nextgallery.core.common.AppResult
import net.drehtuer.nextgallery.core.model.AccentColor
import net.drehtuer.nextgallery.core.model.FolderRule
import net.drehtuer.nextgallery.core.model.FolderRule.Type.EXCLUDE
import net.drehtuer.nextgallery.core.model.FolderRule.Type.INCLUDE
import net.drehtuer.nextgallery.core.model.ThemeMode
import net.drehtuer.nextgallery.core.model.UserSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class DefaultSettingsRepositoryTest {
    private var stored = UserSettings(folderRules = emptyList())
    private val dataSource = mockk<SettingsDataSource> {
        coEvery { update(any()) } coAnswers { stored = firstArg<(UserSettings) -> UserSettings>()(stored) }
    }
    private val repository = DefaultSettingsRepository(dataSource)

    @Test
    fun setRemoteRoot_storesNormalizedPath() = runTest {
        repository.setRemoteRoot("Photos/InstantUpload/")

        assertEquals("/Photos/InstantUpload", stored.remoteRoot)
    }

    @Test
    fun setTileUrl_validUrl_isStored() = runTest {
        val url = "https://tiles.example.org/{z}/{x}/{y}.png"

        repository.setTileUrl(url)

        assertEquals(url, stored.tileUrl)
    }

    @Test(expected = IllegalArgumentException::class)
    fun setTileUrl_invalidUrl_throws() = runTest {
        repository.setTileUrl("http://tiles.example.org/{z}/{x}/{y}.png")
    }

    @Test
    fun setCurrentLocationAsked_isStored() = runTest {
        repository.setCurrentLocationAsked()

        assertTrue(stored.currentLocationAsked)
    }

    @Test
    fun observeSettings_comesFromDataSource() {
        val settings = flowOf(UserSettings())
        every { dataSource.settings } returns settings

        assertEquals(settings, repository.observeSettings())
    }

    @Test
    fun addRule_storesNormalizedPath() = runTest {
        repository.addRule(FolderRule("DCIM/Camera/", INCLUDE))

        assertEquals(listOf(FolderRule("DCIM/Camera", INCLUDE)), stored.folderRules)
    }

    @Test
    fun addRule_samePathInOtherCase_replacesRule() = runTest {
        stored = stored.copy(folderRules = listOf(FolderRule("dcim", INCLUDE)))

        repository.addRule(FolderRule("DCIM/", EXCLUDE))

        assertEquals(listOf(FolderRule("DCIM", EXCLUDE)), stored.folderRules)
    }

    @Test
    fun addRule_writeFails_returnsFailure() = runTest {
        coEvery { dataSource.update(any()) } throws IOException("disk")

        assertTrue(repository.addRule(FolderRule("DCIM", INCLUDE)) is AppResult.Failure)
    }

    @Test
    fun removeRule_removesOnlyThatPath() = runTest {
        val other = FolderRule("DCIM/Camera", EXCLUDE)
        stored = stored.copy(folderRules = listOf(FolderRule("DCIM", INCLUDE), other))

        repository.removeRule("DCIM/")

        assertEquals(listOf(other), stored.folderRules)
    }

    @Test
    fun setExcludePatterns_storesPatterns() = runTest {
        repository.setExcludePatterns(listOf("Old*"))

        assertEquals(listOf("Old*"), stored.excludePatterns)
    }

    @Test
    fun setGridColumns_storesColumns() = runTest {
        repository.setGridColumns(4)

        assertEquals(4, stored.gridColumns)
    }

    @Test(expected = IllegalArgumentException::class)
    fun setGridColumns_unsupportedCount_isRejected() = runTest {
        repository.setGridColumns(2)
    }

    @Test
    fun setThemeMode_storesMode() = runTest {
        repository.setThemeMode(ThemeMode.DARK)

        assertEquals(ThemeMode.DARK, stored.themeMode)
    }

    @Test
    fun setAccentColor_storesColor() = runTest {
        repository.setAccentColor(AccentColor.TEAL)

        assertEquals(AccentColor.TEAL, stored.accentColor)
    }
}
