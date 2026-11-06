package net.drehtuer.nextgallery.feature.folders

import app.cash.turbine.test
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import net.drehtuer.nextgallery.core.data.media.FolderRepository
import net.drehtuer.nextgallery.core.model.Folder
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FolderListViewModelTest {
    private val folders = MutableSharedFlow<List<Folder>>()
    private val repository = mockk<FolderRepository> { every { observeFolders() } returns folders }

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun state_beforeFirstResult_isLoading() = runTest {
        FolderListViewModel(repository).state.test {
            assertEquals(FolderListUiState.Loading, awaitItem())
        }
    }

    @Test
    fun state_noFolders_isEmpty() = runTest {
        FolderListViewModel(repository).state.test {
            skipItems(1)

            folders.emit(emptyList())

            assertEquals(FolderListUiState.Empty, awaitItem())
        }
    }

    @Test
    fun state_withFolders_isContent() = runTest {
        FolderListViewModel(repository).state.test {
            skipItems(1)

            folders.emit(listOf(folder()))

            assertEquals(FolderListUiState.Content(listOf(folder())), awaitItem())
        }
    }
}
