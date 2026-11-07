package net.drehtuer.nextgallery.feature.editor

import android.graphics.Bitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeRight
import androidx.test.ext.junit.runners.AndroidJUnit4
import net.drehtuer.nextgallery.core.data.edit.SaveMode
import net.drehtuer.nextgallery.core.model.edit.AspectRatio
import net.drehtuer.nextgallery.core.model.edit.CropHandle
import net.drehtuer.nextgallery.core.model.edit.EditHistory
import net.drehtuer.nextgallery.core.model.edit.EditOperation
import net.drehtuer.nextgallery.core.model.edit.ImageSize
import net.drehtuer.nextgallery.core.model.edit.NormalizedRect
import net.drehtuer.nextgallery.core.model.edit.PreviewImage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class EditorScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val events = mutableListOf<EditorEvent>()
    private val preview = PreviewImage(Bitmap.createBitmap(100, 50, Bitmap.Config.ARGB_8888), 100, 50)
    private val ready = EditorUiState(
        displayName = "IMG_2041.jpg",
        isLoading = false,
        preview = preview,
        imageSize = ImageSize(4000, 3000),
    )
    private val edited = ready.copy(history = EditHistory().push(EditOperation.Rotate(1)))

    private fun show(state: EditorUiState) {
        composeRule.setContent { EditorScreen(state, onEvent = { events += it }) }
    }

    private fun click(text: String) = composeRule.onNodeWithText(text).performClick()

    private fun clickIcon(description: String) = composeRule.onNodeWithContentDescription(description).performClick()

    @Test
    fun loading_showsNoTools() {
        show(EditorUiState())

        composeRule.onNodeWithText("Rotate").assertDoesNotExist()
    }

    @Test
    fun loadFailed_tellsThatTheImageCannotBeEdited() {
        show(EditorUiState(isLoading = false, loadFailed = true))

        composeRule.onNodeWithText("This image can't be edited").assertIsDisplayed()
    }

    @Test
    fun ready_showsTheImage() {
        show(ready)

        composeRule.onNodeWithContentDescription("IMG_2041.jpg").assertIsDisplayed()
    }

    @Test
    fun ready_showsAllFourTools() {
        show(ready)

        listOf("Rotate", "Crop", "Scale", "Draw").forEach { composeRule.onNodeWithText(it).assertIsDisplayed() }
    }

    @Test
    fun undoRedo_noHistory_areDisabled() {
        show(ready)

        composeRule.onNodeWithContentDescription("Undo").assertIsNotEnabled()
        composeRule.onNodeWithContentDescription("Redo").assertIsNotEnabled()
    }

    @Test
    fun undo_withHistory_sendsEvent() {
        show(edited)

        clickIcon("Undo")

        assertEquals(listOf<EditorEvent>(EditorEvent.Undo), events)
    }

    @Test
    fun redo_afterUndo_sendsEvent() {
        show(ready.copy(history = edited.history.undo()))

        clickIcon("Redo")

        assertEquals(listOf<EditorEvent>(EditorEvent.Redo), events)
    }

    @Test
    fun close_sendsEvent() {
        show(ready)

        clickIcon("Close")

        assertEquals(listOf<EditorEvent>(EditorEvent.Close), events)
    }

    @Test
    fun save_noChanges_isDisabled() {
        show(ready)

        composeRule.onNodeWithText("Save").assertIsNotEnabled()
    }

    @Test
    fun save_withChanges_isEnabled() {
        show(edited)

        composeRule.onNodeWithText("Save").assertIsEnabled()
    }

    @Test
    fun save_opensMenuWithBothModes() {
        show(edited)

        click("Save")

        composeRule.onNodeWithText("Save as copy").assertIsDisplayed()
        composeRule.onNodeWithText("Overwrite original").assertIsDisplayed()
    }

    @Test
    fun saveAsCopy_sendsNewCopyMode() {
        show(edited)
        click("Save")

        click("Save as copy")

        assertEquals(listOf<EditorEvent>(EditorEvent.Save(SaveMode.NEW_COPY)), events)
    }

    @Test
    fun overwriteOriginal_sendsOverwriteMode() {
        show(edited)
        click("Save")

        click("Overwrite original")

        assertEquals(listOf<EditorEvent>(EditorEvent.Save(SaveMode.OVERWRITE)), events)
    }

    @Test
    fun saving_disablesButtons() {
        show(edited.copy(isSaving = true))

        composeRule.onNodeWithText("Save").assertIsNotEnabled()
        composeRule.onNodeWithContentDescription("Close").assertIsNotEnabled()
        composeRule.onNodeWithContentDescription("Undo").assertIsNotEnabled()
    }

    @Test
    fun rotate_sendsEvent() {
        show(ready)

        click("Rotate")

        assertEquals(listOf<EditorEvent>(EditorEvent.Rotate), events)
    }

    @Test
    fun scale_sendsEvent() {
        show(ready)

        click("Scale")

        assertEquals(listOf<EditorEvent>(EditorEvent.ShowScaleDialog), events)
    }

    @Test
    fun crop_sendsEvent() {
        show(ready)

        click("Crop")

        assertEquals(listOf<EditorEvent>(EditorEvent.SelectTool(EditorTool.CROP)), events)
    }

    @Test
    fun draw_sendsEvent() {
        show(ready)

        click("Draw")

        assertEquals(listOf<EditorEvent>(EditorEvent.SelectTool(EditorTool.DRAW)), events)
    }

    @Test
    fun activeTool_isSelected() {
        show(ready.copy(tool = EditorTool.CROP))

        composeRule.onNodeWithText("Crop", useUnmergedTree = true).assertExists()
        composeRule.onNode(androidx.compose.ui.test.hasText("Crop") and androidx.compose.ui.test.isSelectable())
            .assertIsSelected()
        composeRule.onNode(androidx.compose.ui.test.hasText("Draw") and androidx.compose.ui.test.isSelectable())
            .assertIsNotSelected()
    }

    // Crop tool

    @Test
    fun cropTool_showsAspectChips() {
        show(ready.copy(tool = EditorTool.CROP))

        listOf("Free", "Original", "1:1", "4:3", "16:9").forEach {
            composeRule.onNodeWithText(it).assertIsDisplayed()
        }
    }

    @Test
    fun aspectChip_sendsEvent() {
        show(ready.copy(tool = EditorTool.CROP))

        click("16:9")

        assertEquals(listOf<EditorEvent>(EditorEvent.AspectChanged(AspectRatio.R16_9)), events)
    }

    @Test
    fun selectedAspectChip_isSelected() {
        show(ready.copy(tool = EditorTool.CROP, aspect = AspectRatio.SQUARE))

        composeRule.onNodeWithText("1:1").assertIsSelected()
    }

    @Test
    fun cropOverlay_dragInside_movesTheFrame() {
        val frame = NormalizedRect(0.2f, 0.2f, 0.8f, 0.8f)
        show(ready.copy(tool = EditorTool.CROP, cropRect = frame))

        composeRule.onNodeWithTag(CROP_OVERLAY_TAG).performTouchInput {
            swipeRight(startX = centerX, endX = centerX + width / 8f)
        }

        val drag = events.filterIsInstance<EditorEvent.CropDragged>()
        assertTrue(drag.isNotEmpty())
        assertTrue(drag.all { it.handle == CropHandle.INSIDE })
        assertTrue(drag.sumOf { it.distance.x.toDouble() } > 0.0)
    }

    @Test
    fun cropOverlay_dragFromCorner_movesTheCorner() {
        show(ready.copy(tool = EditorTool.CROP))

        composeRule.onNodeWithTag(CROP_OVERLAY_TAG).performTouchInput {
            swipeRight(startX = 0f, endX = width / 2f, durationMillis = 200)
        }

        assertEquals(CropHandle.LEFT, events.filterIsInstance<EditorEvent.CropDragged>().first().handle)
    }

    @Test
    fun cropOverlay_otherTool_isNotShown() {
        show(ready)

        composeRule.onNodeWithTag(CROP_OVERLAY_TAG).assertDoesNotExist()
    }

    // Draw tool

    @Test
    fun drawTool_showsSwatchesAndWidth() {
        show(ready.copy(tool = EditorTool.DRAW))

        listOf("White", "Black", "Coral", "Orange", "Green", "Blue").forEach {
            composeRule.onNodeWithContentDescription(it).assertIsDisplayed()
        }
        composeRule.onNodeWithText("8 px").assertIsDisplayed()
    }

    @Test
    fun swatch_sendsColor() {
        show(ready.copy(tool = EditorTool.DRAW))

        clickIcon("Black")

        assertEquals(listOf<EditorEvent>(EditorEvent.PenColorChanged(0xFF000000.toInt())), events)
    }

    @Test
    fun swatch_ofCurrentColor_isSelected() {
        show(ready.copy(tool = EditorTool.DRAW))

        composeRule.onNodeWithContentDescription("White").assertIsSelected()
    }

    @Test
    fun widthSlider_sendsWidth() {
        show(ready.copy(tool = EditorTool.DRAW))

        composeRule.onNode(
            androidx.compose.ui.test.hasProgressBarRangeInfo(
                androidx.compose.ui.semantics.ProgressBarRangeInfo(8f, 1f..64f),
            ),
        ).performTouchInput { swipeRight(startX = centerX, endX = right) }

        assertTrue(events.filterIsInstance<EditorEvent.PenWidthChanged>().last().width > 8)
    }

    @Test
    fun penCanvas_drag_sendsPointsOnRelease() {
        show(ready.copy(tool = EditorTool.DRAW))

        composeRule.onNodeWithTag(PEN_CANVAS_TAG).performTouchInput {
            down(center)
            moveBy(androidx.compose.ui.geometry.Offset(10f, 0f))
            moveBy(androidx.compose.ui.geometry.Offset(10f, 0f))
            up()
        }

        val stroke = events.filterIsInstance<EditorEvent.StrokeDrawn>().single()
        assertTrue(stroke.points.size >= 2)
        assertTrue(stroke.points.first().x < stroke.points.last().x)
    }

    @Test
    fun penCanvas_tap_sendsOnePoint() {
        show(ready.copy(tool = EditorTool.DRAW))

        composeRule.onNodeWithTag(PEN_CANVAS_TAG).performTouchInput {
            down(center)
            up()
        }

        assertEquals(1, events.filterIsInstance<EditorEvent.StrokeDrawn>().single().points.size)
    }

    @Test
    fun penCanvas_dragOutOfTheImage_keepsPointsInside() {
        show(ready.copy(tool = EditorTool.DRAW))

        composeRule.onNodeWithTag(PEN_CANVAS_TAG).performTouchInput {
            down(center)
            moveBy(androidx.compose.ui.geometry.Offset(100_000f, 0f))
            up()
        }

        assertEquals(1f, events.filterIsInstance<EditorEvent.StrokeDrawn>().single().points.last().x)
    }

    // Dialogs

    @Test
    fun scaleDialog_showsTitleAndSummaryWithCurrentSize() {
        show(ready.copy(isScaleDialogShown = true))

        composeRule.onNodeWithText("Scale image").assertIsDisplayed()
        composeRule.onNodeWithText("Proportions are kept. Original: 4000 × 3000 (12 MP), new: 12 MP.")
            .assertIsDisplayed()
    }

    @Test
    fun scaleDialog_changingPercent_updatesFieldsAndSummary() {
        show(ready.copy(isScaleDialogShown = true))

        composeRule.onNodeWithText("Percent").performTextReplacement("50")

        composeRule.onNodeWithText("2000").assertIsDisplayed()
        composeRule.onNodeWithText("1500").assertIsDisplayed()
        composeRule.onNodeWithText("Proportions are kept. Original: 4000 × 3000 (12 MP), new: 3 MP.")
            .assertIsDisplayed()
    }

    @Test
    fun scaleDialog_changingWidth_updatesHeight() {
        show(ready.copy(isScaleDialogShown = true))

        composeRule.onNodeWithText("Width").performTextReplacement("2000")

        composeRule.onNodeWithText("1500").assertIsDisplayed()
    }

    @Test
    fun scaleDialog_changingHeight_updatesWidth() {
        show(ready.copy(isScaleDialogShown = true))

        composeRule.onNodeWithText("Height").performTextReplacement("1500")

        composeRule.onNodeWithText("2000").assertIsDisplayed()
    }

    @Test
    fun scaleDialog_emptyField_disablesScaleAndShortensSummary() {
        show(ready.copy(isScaleDialogShown = true))

        composeRule.onNodeWithText("Percent").performTextReplacement("")

        composeRule.onNodeWithText("Proportions are kept. Original: 4000 × 3000 (12 MP).").assertIsDisplayed()
        scaleButton().assertIsNotEnabled()
    }

    @Test
    fun scaleDialog_scale_sendsTarget() {
        show(ready.copy(isScaleDialogShown = true))
        composeRule.onNodeWithText("Percent").performTextReplacement("50")

        scaleButton().performClick()

        assertEquals(listOf<EditorEvent>(EditorEvent.ApplyScale(ImageSize(2000, 1500))), events)
    }

    @Test
    fun scaleDialog_cancel_sendsHide() {
        show(ready.copy(isScaleDialogShown = true))

        click("Cancel")

        assertEquals(listOf<EditorEvent>(EditorEvent.HideScaleDialog), events)
    }

    private fun scaleButton() = composeRule.onNode(
        androidx.compose.ui.test.hasText("Scale") and
            androidx.compose.ui.test.hasClickAction() and
            androidx.compose.ui.test.isSelectable().not(),
    )

    @Test
    fun discardDialog_namesTheImage() {
        show(edited.copy(isDiscardDialogShown = true))

        composeRule.onNodeWithText("Discard your changes?").assertIsDisplayed()
        composeRule.onNodeWithText("Your changes to IMG_2041.jpg haven't been saved and will be lost.")
            .assertIsDisplayed()
    }

    @Test
    fun discardDialog_keepEditing_sendsEvent() {
        show(edited.copy(isDiscardDialogShown = true))

        click("Keep editing")

        assertEquals(listOf<EditorEvent>(EditorEvent.KeepEditing), events)
    }

    @Test
    fun discardDialog_discard_sendsEvent() {
        show(edited.copy(isDiscardDialogShown = true))

        click("Discard")

        assertEquals(listOf<EditorEvent>(EditorEvent.Discard), events)
    }

    @Test
    fun memoryMessage_hasExactWordingWithBothNumbers() {
        show(ready.copy(message = EditorMessage.NotEnoughMemory(480, 300)))

        composeRule.onNodeWithText(
            "This image needs about 480 MB of memory, but only 300 MB are available. Close other apps and try again.",
        ).assertIsDisplayed()
    }

    @Test
    fun saveFailedMessage_isShown() {
        show(ready.copy(message = EditorMessage.SaveFailed))

        composeRule.onNodeWithText("The image could not be saved. Your photos were not changed.").assertIsDisplayed()
    }

    @Test
    fun previewFailedMessage_isShown() {
        show(ready.copy(message = EditorMessage.PreviewFailed))

        composeRule.onNodeWithText("The preview could not be updated.").assertIsDisplayed()
    }

    @Test
    fun message_ok_sendsDismiss() {
        show(ready.copy(message = EditorMessage.SaveFailed))

        click("OK")

        assertEquals(listOf<EditorEvent>(EditorEvent.DismissMessage), events)
    }
}
