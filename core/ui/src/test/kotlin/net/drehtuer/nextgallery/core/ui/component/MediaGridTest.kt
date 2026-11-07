package net.drehtuer.nextgallery.core.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.unit.dp
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.flowOf
import net.drehtuer.nextgallery.core.model.DateSource
import net.drehtuer.nextgallery.core.model.MediaId
import net.drehtuer.nextgallery.core.model.MediaItem
import net.drehtuer.nextgallery.core.model.UploadState
import net.drehtuer.nextgallery.core.ui.thumbnail.UploadBadge
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class MediaGridTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun show(
        count: Int,
        selectedIds: Set<MediaId> = emptySet(),
        onItemClick: (MediaItem) -> Unit = {},
        onItemLongClick: (MediaItem) -> Unit = {},
        onState: (LazyGridState) -> Unit = {},
    ) {
        val pages = flowOf(PagingData.from(List(count) { item(it + 1L) }))
        composeRule.setContent {
            val items = pages.collectAsLazyPagingItems()
            val state = rememberLazyGridState()
            onState(state)
            Box(Modifier.size(400.dp)) {
                MediaGrid(items, selectedIds, onItemClick, onItemLongClick, state = state)
                FastScrollbar(state, items.itemCount)
            }
        }
    }

    @Test
    fun mediaGrid_showsTheBadgeOfAnItem() {
        val pages = flowOf(PagingData.from(listOf(item(1))))
        composeRule.setContent {
            MediaGrid(
                pages.collectAsLazyPagingItems(),
                emptySet(),
                {},
                {},
                cellBadge = { UploadBadge(UploadState.NotUploaded) },
            )
        }

        composeRule.onNodeWithContentDescription("Not on your Nextcloud", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun mediaGrid_rendersItems() {
        show(count = 2)

        composeRule.onNodeWithContentDescription("IMG_2.jpg", substring = true).assertIsDisplayed()
    }

    @Test
    fun mediaGrid_itemClicked_reportsItem() {
        var clicked: MediaItem? = null
        show(count = 1, onItemClick = { clicked = it })

        composeRule.onNodeWithContentDescription("IMG_1.jpg", substring = true).performClick()

        assertEquals(MediaId(1), clicked?.id)
    }

    @Test
    fun mediaGrid_itemLongClicked_reportsItem() {
        var clicked: MediaItem? = null
        show(count = 1, onItemLongClick = { clicked = it })

        composeRule.onNodeWithContentDescription("IMG_1.jpg", substring = true).performTouchInput { longClick() }

        assertEquals(MediaId(1), clicked?.id)
    }

    @Test
    fun mediaGrid_selectedItem_isMarkedSelected() {
        show(count = 1, selectedIds = setOf(MediaId(1)))

        composeRule.onNodeWithContentDescription("IMG_1.jpg", substring = true).assertIsSelected()
    }

    @Test
    fun mediaGridPlaceholder_isShown() {
        composeRule.setContent { MediaGridPlaceholder() }

        composeRule.onNodeWithTag(MEDIA_GRID_PLACEHOLDER_TAG).assertIsDisplayed()
    }

    @Test
    fun mediaGrid_columns_setsCellWidth() {
        val pages = flowOf(PagingData.from(listOf(item(1))))
        composeRule.setContent {
            Box(Modifier.size(302.dp)) {
                MediaGrid(pages.collectAsLazyPagingItems(), emptySet(), {}, {}, columns = 2)
            }
        }

        composeRule.onNodeWithContentDescription("IMG_1.jpg", substring = true).assertWidthIsEqualTo(150.dp)
    }

    @Test
    fun fastScrollbar_allItemsVisible_isHidden() {
        show(count = 1)

        composeRule.onNodeWithTag(FAST_SCROLLBAR_THUMB_TAG).assertDoesNotExist()
    }

    @Test
    fun fastScrollbar_dragged_scrollsGrid() {
        lateinit var state: LazyGridState
        show(count = 2_000, onState = { state = it })

        composeRule.onNodeWithTag(FAST_SCROLLBAR_THUMB_TAG).performTouchInput { swipeDown(endY = centerY + 300f) }
        composeRule.waitForIdle()

        assertTrue(state.firstVisibleItemIndex > 0)
    }

    private fun item(id: Long) = MediaItem(
        id = MediaId(id),
        volumeName = "external_primary",
        contentUri = "content://media/$id",
        bucketId = 7,
        bucketName = "Camera",
        relativePath = "DCIM/Camera/",
        displayName = "IMG_$id.jpg",
        mimeType = "image/jpeg",
        sizeBytes = 1,
        width = 1,
        height = 1,
        orientation = 0,
        dateTakenMillis = 1,
        dateSource = DateSource.DATE_TAKEN,
        takenLocalDate = LocalDate.of(2024, 3, 15),
        dateModifiedSeconds = 1,
        generationModified = 1,
        location = null,
    )
}
