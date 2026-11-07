package net.drehtuer.nextgallery.feature.folders

import app.cash.turbine.test
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import net.drehtuer.nextgallery.core.data.media.FolderRepository
import net.drehtuer.nextgallery.core.data.settings.SettingsRepository
import net.drehtuer.nextgallery.core.model.AddedFolder
import net.drehtuer.nextgallery.core.model.Folder
import net.drehtuer.nextgallery.core.model.UserSettings
import net.drehtuer.nextgallery.core.model.VirtualFolderSummary
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FolderListViewModelTest {
    private val folders = MutableSharedFlow<List<Folder>>()
    private val virtualFolders = MutableStateFlow<List<VirtualFolderSummary>>(emptyList())
    private val settings = MutableStateFlow(UserSettings())
    private val repository = mockk<FolderRepository> {
        every { observeFolders() } returns folders
        every { observeVirtualFolders() } returns virtualFolders
    }
    private val settingsRepository = mockk<SettingsRepository> { every { observeSettings() } returns settings }

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = FolderListViewModel(repository, settingsRepository)

    @Test
    fun state_beforeFirstResult_isLoading() = runTest {
        viewModel().state.test {
            assertEquals(FolderListUiState.Loading, awaitItem())
        }
    }

    @Test
    fun state_noFolderAdded_isNoFolders() = runTest {
        viewModel().state.test {
            skipItems(1)

            folders.emit(emptyList())

            assertEquals(FolderListUiState.NoFolders, awaitItem())
        }
    }

    @Test
    fun state_folderAddedWithoutImages_isNoImages() = runTest {
        settings.value = UserSettings(addedFolders = listOf(AddedFolder("external_primary", "DCIM")))

        viewModel().state.test {
            skipItems(1)

            folders.emit(emptyList())

            assertEquals(FolderListUiState.NoImages, awaitItem())
        }
    }

    @Test
    fun state_withFolders_isContent() = runTest {
        viewModel().state.test {
            skipItems(1)

            folders.emit(listOf(folder()))

            assertEquals(FolderListUiState.Content(listOf(folder())), awaitItem())
        }
    }

    @Test
    fun state_leavesOutVirtualFoldersWithoutImages() = runTest {
        virtualFolders.value = listOf(virtualFolder(id = "empty", itemCount = 0), virtualFolder(id = "full"))

        viewModel().state.test {
            skipItems(1)

            folders.emit(listOf(folder()))

            assertEquals(
                FolderListUiState.Content(listOf(folder()), listOf(virtualFolder(id = "full"))),
                awaitItem(),
            )
        }
    }
}
