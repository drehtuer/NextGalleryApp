package net.drehtuer.nextgallery.core.data.settings

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import net.drehtuer.nextgallery.core.common.AppResult
import net.drehtuer.nextgallery.core.model.AddedFolder
import net.drehtuer.nextgallery.core.model.UserSettings
import net.drehtuer.nextgallery.core.model.VirtualFolder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class DefaultFolderSettingsRepositoryTest {
    private var stored = UserSettings()
    private val dataSource = mockk<SettingsDataSource> {
        coEvery { update(any()) } coAnswers { stored = firstArg<(UserSettings) -> UserSettings>()(stored) }
    }
    private val repository = DefaultFolderSettingsRepository(dataSource)

    @Test
    fun addFolder_storesNormalizedPath() = runTest {
        repository.addFolder(AddedFolder("external_primary", "DCIM/Camera/"))

        assertEquals(listOf(AddedFolder("external_primary", "DCIM/Camera")), stored.addedFolders)
    }

    @Test
    fun addFolder_alreadyAddedInOtherCase_changesNothing() = runTest {
        val added = AddedFolder("external_primary", "dcim")
        stored = stored.copy(addedFolders = listOf(added))

        repository.addFolder(AddedFolder("external_primary", "DCIM"))

        assertEquals(listOf(added), stored.addedFolders)
    }

    @Test
    fun addFolder_samePathOnOtherVolume_isAdded() = runTest {
        stored = stored.copy(addedFolders = listOf(AddedFolder("external_primary", "DCIM")))

        repository.addFolder(AddedFolder("1234-abcd", "DCIM"))

        assertEquals(2, stored.addedFolders.size)
    }

    @Test
    fun addFolder_writeFails_returnsFailure() = runTest {
        coEvery { dataSource.update(any()) } throws IOException("disk full")

        assertTrue(repository.addFolder(AddedFolder("external_primary", "DCIM")) is AppResult.Failure)
    }

    @Test
    fun removeFolder_removesOnlyThatFolder() = runTest {
        val other = AddedFolder("external_primary", "DCIM/Camera")
        stored = stored.copy(addedFolders = listOf(AddedFolder("external_primary", "DCIM"), other))

        repository.removeFolder(AddedFolder("external_primary", "DCIM"))

        assertEquals(listOf(other), stored.addedFolders)
    }

    @Test
    fun createVirtualFolder_storesNameAndMembers() = runTest {
        repository.createVirtualFolder("All", listOf(7, 8))

        assertEquals("All" to listOf(7L, 8L), stored.virtualFolders.single().let { it.name to it.bucketIds })
    }

    @Test
    fun createVirtualFolder_twoWithTheSameName_haveDifferentIds() = runTest {
        repository.createVirtualFolder("All", listOf(7))
        repository.createVirtualFolder("All", listOf(7))

        assertEquals(2, stored.virtualFolders.map { it.id }.toSet().size)
    }

    @Test
    fun updateVirtualFolder_replacesTheOneWithThatId() = runTest {
        val other = VirtualFolder("v2", "Other", listOf(9))
        stored = stored.copy(virtualFolders = listOf(VirtualFolder("v1", "Old", listOf(7)), other))

        repository.updateVirtualFolder(VirtualFolder("v1", "New", listOf(8)))

        assertEquals(listOf(VirtualFolder("v1", "New", listOf(8)), other), stored.virtualFolders)
    }

    @Test
    fun deleteVirtualFolder_removesOnlyThatOne() = runTest {
        val other = VirtualFolder("v2", "Other", listOf(9))
        stored = stored.copy(virtualFolders = listOf(VirtualFolder("v1", "Old", listOf(7)), other))

        repository.deleteVirtualFolder("v1")

        assertEquals(listOf(other), stored.virtualFolders)
    }
}
