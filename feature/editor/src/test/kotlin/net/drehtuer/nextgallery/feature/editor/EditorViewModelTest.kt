package net.drehtuer.nextgallery.feature.editor

import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import net.drehtuer.nextgallery.core.common.AppError
import net.drehtuer.nextgallery.core.common.AppResult
import net.drehtuer.nextgallery.core.data.edit.EditRepository
import net.drehtuer.nextgallery.core.data.edit.EditSource
import net.drehtuer.nextgallery.core.data.edit.MemoryCheck
import net.drehtuer.nextgallery.core.data.edit.SaveMode
import net.drehtuer.nextgallery.core.data.edit.SaveOutcome
import net.drehtuer.nextgallery.core.model.MediaId
import net.drehtuer.nextgallery.core.model.PlatformConfirmation
import net.drehtuer.nextgallery.core.model.edit.AspectRatio
import net.drehtuer.nextgallery.core.model.edit.CropHandle
import net.drehtuer.nextgallery.core.model.edit.EditOperation
import net.drehtuer.nextgallery.core.model.edit.ImageSize
import net.drehtuer.nextgallery.core.model.edit.NormalizedDistance
import net.drehtuer.nextgallery.core.model.edit.NormalizedPoint
import net.drehtuer.nextgallery.core.model.edit.NormalizedRect
import net.drehtuer.nextgallery.core.model.edit.PreviewImage
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EditorViewModelTest {
    private val id = MediaId(1)
    private val base = PreviewImage(Any(), 2000, 1000)
    private val rendered = PreviewImage(Any(), 1000, 2000)
    private val source = EditSource(mediaItem(1), ImageSize(4000, 2000), base)
    private val confirmation = PlatformConfirmation(Any())
    private val rotate = EditOperation.Rotate(1)
    private val rightDrag = EditorEvent.CropDragged(CropHandle.LEFT, NormalizedDistance(0.5f, 0f))
    private val repository = mockk<EditRepository> {
        coEvery { open(id) } returns AppResult.Success(source)
        coEvery { renderPreview(source, any()) } returns AppResult.Success(rendered)
        every { checkMemory(source, any()) } returns MemoryCheck.Fits
    }

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = EditorViewModel(EditorRoute(1), repository)

    private fun EditorViewModel.operations() = state.value.history.operations

    // Opening

    @Test
    fun init_showsBaseImage() {
        assertSame(base, viewModel().state.value.preview)
    }

    @Test
    fun init_knowsFullSizeAndName() {
        val state = viewModel().state.value

        assertEquals(ImageSize(4000, 2000), state.imageSize)
        assertEquals(source.item.displayName, state.displayName)
    }

    @Test
    fun init_finishesLoading() {
        assertFalse(viewModel().state.value.isLoading)
    }

    @Test
    fun init_whileOpening_isLoading() {
        coEvery { repository.open(id) } coAnswers { CompletableDeferred<AppResult<EditSource>>().await() }

        assertTrue(viewModel().state.value.isLoading)
    }

    @Test
    fun init_failure_showsThatTheImageCannotBeEdited() {
        coEvery { repository.open(id) } returns AppResult.Failure(AppError.NotFound)

        val state = viewModel().state.value

        assertTrue(state.loadFailed)
        assertFalse(state.isLoading)
    }

    @Test
    fun init_hasNoChanges() {
        val state = viewModel().state.value

        assertEquals(listOf(false, false, false), listOf(state.canUndo, state.canRedo, state.hasChanges))
    }

    // Rotate

    @Test
    fun rotate_addsOperation() {
        val viewModel = viewModel()

        viewModel.onEvent(EditorEvent.Rotate)

        assertEquals(listOf<EditOperation>(rotate), viewModel.operations())
    }

    @Test
    fun rotate_rendersNewPreview() {
        val viewModel = viewModel()

        viewModel.onEvent(EditorEvent.Rotate)

        assertSame(rendered, viewModel.state.value.preview)
    }

    @Test
    fun rotate_updatesFullSize() {
        val viewModel = viewModel()

        viewModel.onEvent(EditorEvent.Rotate)

        assertEquals(ImageSize(2000, 4000), viewModel.state.value.imageSize)
    }

    @Test
    fun rotate_doesNotBecomeActiveTool() {
        val viewModel = viewModel()

        viewModel.onEvent(EditorEvent.Rotate)

        assertNull(viewModel.state.value.tool)
    }

    @Test
    fun rotate_whileTheImageIsNotOpen_keepsHistoryButRendersNothing() {
        coEvery { repository.open(id) } returns AppResult.Failure(AppError.NotFound)
        val viewModel = viewModel()

        viewModel.onEvent(EditorEvent.Rotate)

        coVerify(exactly = 0) { repository.renderPreview(any(), any()) }
    }

    @Test
    fun rotate_previewFails_showsMessageAndKeepsOldImage() {
        coEvery { repository.renderPreview(source, any()) } returns AppResult.Failure(AppError.Io("broken"))
        val viewModel = viewModel()

        viewModel.onEvent(EditorEvent.Rotate)

        assertEquals(EditorMessage.PreviewFailed, viewModel.state.value.message)
        assertSame(base, viewModel.state.value.preview)
    }

    @Test
    fun rotate_previewOutOfMemory_showsMemoryMessage() {
        coEvery { repository.renderPreview(source, any()) } returns
            AppResult.Failure(AppError.InsufficientMemory(480_000_000, 300_000_000))
        val viewModel = viewModel()

        viewModel.onEvent(EditorEvent.Rotate)

        assertEquals(EditorMessage.NotEnoughMemory(480, 300), viewModel.state.value.message)
    }

    @Test
    fun rotate_whilePreviousPreviewRenders_replacesThatRender() {
        val slow = CompletableDeferred<AppResult<PreviewImage>>()
        coEvery { repository.renderPreview(source, listOf(rotate)) } coAnswers { slow.await() }
        val viewModel = viewModel()

        viewModel.onEvent(EditorEvent.Rotate)
        viewModel.onEvent(EditorEvent.Rotate)
        slow.complete(AppResult.Success(PreviewImage(Any(), 1, 1)))

        assertSame(rendered, viewModel.state.value.preview)
    }

    // Crop

    @Test
    fun selectTool_crop_becomesActive() {
        val viewModel = viewModel()

        viewModel.onEvent(EditorEvent.SelectTool(EditorTool.CROP))

        assertEquals(EditorTool.CROP, viewModel.state.value.tool)
    }

    @Test
    fun selectTool_activeTool_closesIt() {
        val viewModel = viewModel()
        viewModel.onEvent(EditorEvent.SelectTool(EditorTool.DRAW))

        viewModel.onEvent(EditorEvent.SelectTool(EditorTool.DRAW))

        assertNull(viewModel.state.value.tool)
    }

    @Test
    fun cropDragged_movesFrameWithoutAddingAnOperation() {
        val viewModel = viewModel()
        viewModel.onEvent(EditorEvent.SelectTool(EditorTool.CROP))

        viewModel.onEvent(rightDrag)

        assertEquals(0.5f, viewModel.state.value.cropRect.left)
        assertTrue(viewModel.operations().isEmpty())
    }

    @Test
    fun cropDragged_countsAsAChange() {
        val viewModel = viewModel()

        viewModel.onEvent(rightDrag)

        assertTrue(viewModel.state.value.hasChanges)
    }

    @Test
    fun selectTool_otherTool_appliesTheCrop() {
        val viewModel = viewModel()
        viewModel.onEvent(EditorEvent.SelectTool(EditorTool.CROP))
        viewModel.onEvent(rightDrag)

        viewModel.onEvent(EditorEvent.SelectTool(EditorTool.DRAW))

        assertEquals(
            listOf<EditOperation>(EditOperation.Crop(NormalizedRect(0.5f, 0f, 1f, 1f))),
            viewModel.operations(),
        )
    }

    @Test
    fun selectTool_afterCrop_resetsFrameAndRenders() {
        val viewModel = viewModel()
        viewModel.onEvent(EditorEvent.SelectTool(EditorTool.CROP))
        viewModel.onEvent(rightDrag)

        viewModel.onEvent(EditorEvent.SelectTool(EditorTool.CROP))

        assertEquals(NormalizedRect.FULL, viewModel.state.value.cropRect)
        assertSame(rendered, viewModel.state.value.preview)
    }

    @Test
    fun selectTool_cropUnchanged_addsNoOperation() {
        val viewModel = viewModel()
        viewModel.onEvent(EditorEvent.SelectTool(EditorTool.CROP))

        viewModel.onEvent(EditorEvent.SelectTool(EditorTool.DRAW))

        assertTrue(viewModel.operations().isEmpty())
    }

    @Test
    fun aspectChanged_ratio_fitsTheFrame() {
        val viewModel = viewModel()

        viewModel.onEvent(EditorEvent.AspectChanged(AspectRatio.SQUARE))

        // A square on the 4000 × 2000 image: half the width, centred.
        assertEquals(NormalizedRect(0.25f, 0f, 0.75f, 1f), viewModel.state.value.cropRect)
        assertEquals(AspectRatio.SQUARE, viewModel.state.value.aspect)
    }

    @Test
    fun aspectChanged_free_keepsTheFrame() {
        val viewModel = viewModel()
        viewModel.onEvent(EditorEvent.AspectChanged(AspectRatio.SQUARE))

        viewModel.onEvent(EditorEvent.AspectChanged(AspectRatio.FREE))

        assertEquals(NormalizedRect(0.25f, 0f, 0.75f, 1f), viewModel.state.value.cropRect)
    }

    @Test
    fun aspectChanged_imageNotOpen_doesNothing() {
        coEvery { repository.open(id) } returns AppResult.Failure(AppError.NotFound)
        val viewModel = viewModel()

        viewModel.onEvent(EditorEvent.AspectChanged(AspectRatio.SQUARE))

        assertEquals(AspectRatio.FREE, viewModel.state.value.aspect)
    }

    @Test
    fun cropDragged_imageNotOpen_doesNothing() {
        coEvery { repository.open(id) } returns AppResult.Failure(AppError.NotFound)
        val viewModel = viewModel()

        viewModel.onEvent(rightDrag)

        assertEquals(NormalizedRect.FULL, viewModel.state.value.cropRect)
    }

    @Test
    fun cropDragged_withRatio_keepsTheRatio() {
        val viewModel = viewModel()
        viewModel.onEvent(EditorEvent.AspectChanged(AspectRatio.SQUARE))

        viewModel.onEvent(EditorEvent.CropDragged(CropHandle.RIGHT, NormalizedDistance(-0.1f, 0f)))

        val rect = viewModel.state.value.cropRect
        assertEquals(rect.width * 4000, rect.height * 2000, 1f)
    }

    @Test
    fun rotate_duringCrop_appliesTheCropFirst() {
        val viewModel = viewModel()
        viewModel.onEvent(rightDrag)

        viewModel.onEvent(EditorEvent.Rotate)

        assertEquals(
            listOf(EditOperation.Crop(NormalizedRect(0.5f, 0f, 1f, 1f)), rotate),
            viewModel.operations(),
        )
    }

    // Scale

    @Test
    fun showScaleDialog_showsIt() {
        val viewModel = viewModel()

        viewModel.onEvent(EditorEvent.ShowScaleDialog)

        assertTrue(viewModel.state.value.isScaleDialogShown)
    }

    @Test
    fun showScaleDialog_duringCrop_appliesTheCropSoTheDialogShowsTheCroppedSize() {
        val viewModel = viewModel()
        viewModel.onEvent(rightDrag)

        viewModel.onEvent(EditorEvent.ShowScaleDialog)

        assertEquals(ImageSize(2000, 2000), viewModel.state.value.imageSize)
    }

    @Test
    fun hideScaleDialog_hidesIt() {
        val viewModel = viewModel()
        viewModel.onEvent(EditorEvent.ShowScaleDialog)

        viewModel.onEvent(EditorEvent.HideScaleDialog)

        assertFalse(viewModel.state.value.isScaleDialogShown)
    }

    @Test
    fun applyScale_fits_addsOperationAndClosesDialog() {
        val viewModel = viewModel()
        viewModel.onEvent(EditorEvent.ShowScaleDialog)

        viewModel.onEvent(EditorEvent.ApplyScale(ImageSize(400, 200)))

        assertEquals(listOf<EditOperation>(EditOperation.Scale(400, 200)), viewModel.operations())
        assertFalse(viewModel.state.value.isScaleDialogShown)
    }

    @Test
    fun applyScale_fits_updatesFullSize() {
        val viewModel = viewModel()

        viewModel.onEvent(EditorEvent.ApplyScale(ImageSize(400, 200)))

        assertEquals(ImageSize(400, 200), viewModel.state.value.imageSize)
    }

    @Test
    fun applyScale_checksTheMemoryWithTheNewOperation() {
        val viewModel = viewModel()
        viewModel.onEvent(EditorEvent.Rotate)

        viewModel.onEvent(EditorEvent.ApplyScale(ImageSize(400, 800)))

        verify { repository.checkMemory(source, listOf(rotate, EditOperation.Scale(400, 800))) }
    }

    @Test
    fun applyScale_refused_leavesImageUnchanged() {
        every { repository.checkMemory(source, any()) } returns MemoryCheck.Refused(480_000_000, 300_000_000)
        val viewModel = viewModel()

        viewModel.onEvent(EditorEvent.ApplyScale(ImageSize(12000, 6000)))

        assertTrue(viewModel.operations().isEmpty())
        assertEquals(ImageSize(4000, 2000), viewModel.state.value.imageSize)
        coVerify(exactly = 0) { repository.renderPreview(any(), any()) }
    }

    @Test
    fun applyScale_refused_showsBothNumbers() {
        every { repository.checkMemory(source, any()) } returns MemoryCheck.Refused(480_000_000, 300_000_000)
        val viewModel = viewModel()

        viewModel.onEvent(EditorEvent.ApplyScale(ImageSize(12000, 6000)))

        assertEquals(EditorMessage.NotEnoughMemory(480, 300), viewModel.state.value.message)
    }

    @Test
    fun applyScale_imageNotOpen_doesNothing() {
        coEvery { repository.open(id) } returns AppResult.Failure(AppError.NotFound)
        val viewModel = viewModel()

        viewModel.onEvent(EditorEvent.ApplyScale(ImageSize(400, 200)))

        assertTrue(viewModel.operations().isEmpty())
    }

    // Draw

    @Test
    fun penColorChanged_updatesColor() {
        val viewModel = viewModel()

        viewModel.onEvent(EditorEvent.PenColorChanged(5))

        assertEquals(5, viewModel.state.value.penColor)
    }

    @Test
    fun penWidthChanged_updatesWidth() {
        val viewModel = viewModel()

        viewModel.onEvent(EditorEvent.PenWidthChanged(20))

        assertEquals(20, viewModel.state.value.penWidth)
    }

    @Test
    fun strokeDrawn_addsOperationWithColorAndRelativeWidth() {
        val viewModel = viewModel()
        val points = listOf(NormalizedPoint(0.1f, 0.2f))
        viewModel.onEvent(EditorEvent.PenColorChanged(5))
        viewModel.onEvent(EditorEvent.PenWidthChanged(20))

        viewModel.onEvent(EditorEvent.StrokeDrawn(points))

        // 20 px on the 2000 px long preview.
        assertEquals(listOf<EditOperation>(EditOperation.PenStroke(points, 5, 0.01f)), viewModel.operations())
    }

    @Test
    fun strokeDrawn_noPoints_addsNothing() {
        val viewModel = viewModel()

        viewModel.onEvent(EditorEvent.StrokeDrawn(emptyList()))

        assertTrue(viewModel.operations().isEmpty())
    }

    @Test
    fun penWidthFraction_withoutPreview_usesLargestPreviewSide() {
        assertEquals(8f / 2048f, EditorUiState().penWidthFraction)
    }

    // Undo and redo

    @Test
    fun undo_removesLastOperation() {
        val viewModel = viewModel()
        viewModel.onEvent(EditorEvent.Rotate)

        viewModel.onEvent(EditorEvent.Undo)

        assertTrue(viewModel.operations().isEmpty())
    }

    @Test
    fun undo_lastOperation_showsBaseWithoutRendering() {
        val viewModel = viewModel()
        viewModel.onEvent(EditorEvent.Rotate)

        viewModel.onEvent(EditorEvent.Undo)

        assertSame(base, viewModel.state.value.preview)
        coVerify(exactly = 1) { repository.renderPreview(source, any()) }
    }

    @Test
    fun undo_restoresFullSize() {
        val viewModel = viewModel()
        viewModel.onEvent(EditorEvent.Rotate)

        viewModel.onEvent(EditorEvent.Undo)

        assertEquals(ImageSize(4000, 2000), viewModel.state.value.imageSize)
    }

    @Test
    fun undo_withEarlierOperation_rendersWhatRemains() {
        val viewModel = viewModel()
        viewModel.onEvent(EditorEvent.Rotate)
        viewModel.onEvent(EditorEvent.Rotate)

        viewModel.onEvent(EditorEvent.Undo)

        coVerify(exactly = 2) { repository.renderPreview(source, listOf(rotate)) }
    }

    @Test
    fun undo_nothingToUndo_doesNothing() {
        val viewModel = viewModel()

        viewModel.onEvent(EditorEvent.Undo)

        assertFalse(viewModel.state.value.canRedo)
    }

    @Test
    fun undo_pendingCrop_resetsOnlyTheFrame() {
        val viewModel = viewModel()
        viewModel.onEvent(EditorEvent.Rotate)
        viewModel.onEvent(rightDrag)

        viewModel.onEvent(EditorEvent.Undo)

        assertEquals(NormalizedRect.FULL, viewModel.state.value.cropRect)
        assertEquals(listOf<EditOperation>(rotate), viewModel.operations())
    }

    @Test
    fun redo_bringsOperationBack() {
        val viewModel = viewModel()
        viewModel.onEvent(EditorEvent.Rotate)
        viewModel.onEvent(EditorEvent.Undo)

        viewModel.onEvent(EditorEvent.Redo)

        assertEquals(listOf<EditOperation>(rotate), viewModel.operations())
        assertSame(rendered, viewModel.state.value.preview)
    }

    @Test
    fun redo_nothingToRedo_doesNothing() {
        val viewModel = viewModel()

        viewModel.onEvent(EditorEvent.Redo)

        assertTrue(viewModel.operations().isEmpty())
    }

    @Test
    fun undo_enablesRedo() {
        val viewModel = viewModel()
        viewModel.onEvent(EditorEvent.Rotate)

        viewModel.onEvent(EditorEvent.Undo)

        assertTrue(viewModel.state.value.canRedo)
    }

    // Saving

    @Test
    fun save_newCopy_passesOperationsAndMode() = runTest {
        coEvery { repository.save(id, any(), any()) } returns SaveOutcome.SavedCopy(MediaId(9), 7, true)
        val viewModel = viewModel()
        viewModel.onEvent(EditorEvent.Rotate)

        viewModel.onEvent(EditorEvent.Save(SaveMode.NEW_COPY))

        coVerify { repository.save(id, listOf(rotate), SaveMode.NEW_COPY) }
    }

    @Test
    fun save_pendingCrop_isPartOfTheSave() = runTest {
        coEvery { repository.save(id, any(), any()) } returns SaveOutcome.SavedCopy(MediaId(9), 7, true)
        val viewModel = viewModel()
        viewModel.onEvent(rightDrag)

        viewModel.onEvent(EditorEvent.Save(SaveMode.NEW_COPY))

        coVerify {
            repository.save(id, listOf(EditOperation.Crop(NormalizedRect(0.5f, 0f, 1f, 1f))), SaveMode.NEW_COPY)
        }
    }

    @Test
    fun save_whileRunning_isSaving() {
        coEvery { repository.save(id, any(), any()) } coAnswers {
            CompletableDeferred<SaveOutcome>().await()
        }
        val viewModel = viewModel()
        viewModel.onEvent(EditorEvent.Rotate)

        viewModel.onEvent(EditorEvent.Save(SaveMode.NEW_COPY))

        assertTrue(viewModel.state.value.isSaving)
    }

    @Test
    fun onEvent_whileSaving_isIgnored() {
        coEvery { repository.save(id, any(), any()) } coAnswers {
            CompletableDeferred<SaveOutcome>().await()
        }
        val viewModel = viewModel()
        viewModel.onEvent(EditorEvent.Rotate)
        viewModel.onEvent(EditorEvent.Save(SaveMode.NEW_COPY))

        viewModel.onEvent(EditorEvent.Rotate)
        viewModel.onEvent(EditorEvent.Close)

        assertEquals(listOf<EditOperation>(rotate), viewModel.operations())
        assertFalse(viewModel.state.value.isDiscardDialogShown)
    }

    @Test
    fun save_copyInIndex_opensTheCopy() = runTest {
        coEvery { repository.save(id, any(), any()) } returns SaveOutcome.SavedCopy(MediaId(9), 7, true)
        val viewModel = viewModel()

        viewModel.effects.test {
            viewModel.onEvent(EditorEvent.Save(SaveMode.NEW_COPY))

            assertEquals(EditorEffect.OpenCopy(MediaId(9), 7), awaitItem())
        }
    }

    @Test
    fun save_copyNotYetInIndex_closesTheEditor() = runTest {
        coEvery { repository.save(id, any(), any()) } returns SaveOutcome.SavedCopy(MediaId(9), 7, false)
        val viewModel = viewModel()

        viewModel.effects.test {
            viewModel.onEvent(EditorEvent.Save(SaveMode.NEW_COPY))

            assertEquals(EditorEffect.Close, awaitItem())
        }
    }

    @Test
    fun save_refused_leavesImageAndShowsBothNumbers() {
        coEvery { repository.save(id, any(), any()) } returns
            SaveOutcome.Failed(AppError.InsufficientMemory(480_000_000, 300_000_000))
        val viewModel = viewModel()
        viewModel.onEvent(EditorEvent.Rotate)

        viewModel.onEvent(EditorEvent.Save(SaveMode.NEW_COPY))

        val state = viewModel.state.value
        assertEquals(EditorMessage.NotEnoughMemory(480, 300), state.message)
        assertEquals(listOf<EditOperation>(rotate), state.history.operations)
        assertFalse(state.isSaving)
    }

    @Test
    fun save_failed_showsMessage() {
        coEvery { repository.save(id, any(), any()) } returns SaveOutcome.Failed(AppError.Io("disk full"))
        val viewModel = viewModel()

        viewModel.onEvent(EditorEvent.Save(SaveMode.NEW_COPY))

        assertEquals(EditorMessage.SaveFailed, viewModel.state.value.message)
    }

    @Test
    fun dismissMessage_removesIt() {
        coEvery { repository.save(id, any(), any()) } returns SaveOutcome.Failed(AppError.Io("disk full"))
        val viewModel = viewModel()
        viewModel.onEvent(EditorEvent.Save(SaveMode.NEW_COPY))

        viewModel.onEvent(EditorEvent.DismissMessage)

        assertNull(viewModel.state.value.message)
    }

    @Test
    fun save_overwrite_requestsConfirmation() = runTest {
        coEvery { repository.save(id, any(), SaveMode.OVERWRITE) } returns SaveOutcome.NeedsConfirmation(confirmation)
        val viewModel = viewModel()

        viewModel.effects.test {
            viewModel.onEvent(EditorEvent.Save(SaveMode.OVERWRITE))

            assertEquals(EditorEffect.RequestConfirmation(confirmation), awaitItem())
        }
    }

    @Test
    fun confirmationResult_confirmed_commitsTheSameOperations() {
        coEvery { repository.save(id, any(), SaveMode.OVERWRITE) } returns SaveOutcome.NeedsConfirmation(confirmation)
        coEvery { repository.commitOverwrite(id, any()) } returns SaveOutcome.Overwritten
        val viewModel = viewModel()
        viewModel.onEvent(EditorEvent.Rotate)
        viewModel.onEvent(EditorEvent.Save(SaveMode.OVERWRITE))

        viewModel.onEvent(EditorEvent.ConfirmationResult(true))

        coVerify { repository.commitOverwrite(id, listOf(rotate)) }
    }

    @Test
    fun confirmationResult_confirmedAndWritten_reportsOverwrite() = runTest {
        coEvery { repository.save(id, any(), SaveMode.OVERWRITE) } returns SaveOutcome.NeedsConfirmation(confirmation)
        coEvery { repository.commitOverwrite(id, any()) } returns SaveOutcome.Overwritten
        val viewModel = viewModel()

        viewModel.effects.test {
            viewModel.onEvent(EditorEvent.Save(SaveMode.OVERWRITE))
            awaitItem()

            viewModel.onEvent(EditorEvent.ConfirmationResult(true))

            assertEquals(EditorEffect.Overwritten, awaitItem())
        }
    }

    @Test
    fun confirmationResult_declined_writesNothingAndGoesBackToEditing() {
        coEvery { repository.save(id, any(), SaveMode.OVERWRITE) } returns SaveOutcome.NeedsConfirmation(confirmation)
        val viewModel = viewModel()
        viewModel.onEvent(EditorEvent.Rotate)
        viewModel.onEvent(EditorEvent.Save(SaveMode.OVERWRITE))

        viewModel.onEvent(EditorEvent.ConfirmationResult(false))

        coVerify(exactly = 0) { repository.commitOverwrite(any(), any()) }
        assertFalse(viewModel.state.value.isSaving)
        assertEquals(listOf<EditOperation>(rotate), viewModel.operations())
    }

    @Test
    fun confirmationResult_withoutSaving_isIgnored() {
        val viewModel = viewModel()

        viewModel.onEvent(EditorEvent.ConfirmationResult(true))

        coVerify(exactly = 0) { repository.commitOverwrite(any(), any()) }
    }

    @Test
    fun confirmationResult_commitRefused_showsBothNumbers() {
        coEvery { repository.save(id, any(), SaveMode.OVERWRITE) } returns SaveOutcome.NeedsConfirmation(confirmation)
        coEvery { repository.commitOverwrite(id, any()) } returns
            SaveOutcome.Failed(AppError.InsufficientMemory(480_000_000, 300_000_000))
        val viewModel = viewModel()
        viewModel.onEvent(EditorEvent.Save(SaveMode.OVERWRITE))

        viewModel.onEvent(EditorEvent.ConfirmationResult(true))

        assertEquals(EditorMessage.NotEnoughMemory(480, 300), viewModel.state.value.message)
    }

    // Closing

    @Test
    fun close_noChanges_closesTheEditor() = runTest {
        val viewModel = viewModel()

        viewModel.effects.test {
            viewModel.onEvent(EditorEvent.Close)

            assertEquals(EditorEffect.Close, awaitItem())
        }
    }

    @Test
    fun close_withChanges_asksFirst() {
        val viewModel = viewModel()
        viewModel.onEvent(EditorEvent.Rotate)

        viewModel.onEvent(EditorEvent.Close)

        assertTrue(viewModel.state.value.isDiscardDialogShown)
    }

    @Test
    fun close_allChangesUndone_closesTheEditor() = runTest {
        val viewModel = viewModel()
        viewModel.onEvent(EditorEvent.Rotate)
        viewModel.onEvent(EditorEvent.Undo)

        viewModel.effects.test {
            viewModel.onEvent(EditorEvent.Close)

            assertEquals(EditorEffect.Close, awaitItem())
        }
    }

    @Test
    fun keepEditing_hidesTheQuestion() {
        val viewModel = viewModel()
        viewModel.onEvent(EditorEvent.Rotate)
        viewModel.onEvent(EditorEvent.Close)

        viewModel.onEvent(EditorEvent.KeepEditing)

        assertFalse(viewModel.state.value.isDiscardDialogShown)
    }

    @Test
    fun discard_closesTheEditor() = runTest {
        val viewModel = viewModel()
        viewModel.onEvent(EditorEvent.Rotate)

        viewModel.effects.test {
            viewModel.onEvent(EditorEvent.Discard)

            assertEquals(EditorEffect.Close, awaitItem())
        }
    }
}
