package net.drehtuer.nextgallery.core.ui.thumbnail

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertLeftPositionInRootIsEqualTo
import androidx.compose.ui.test.assertTopPositionInRootIsEqualTo
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import net.drehtuer.nextgallery.core.model.UploadState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UploadBadgeTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun show(state: UploadState) = composeRule.setContent {
        Box(Modifier.size(100.dp)) { UploadBadge(state) }
    }

    @Test
    fun uploaded_saysSo() {
        show(UploadState.Uploaded(verifiedByChecksum = false))

        composeRule.onNodeWithContentDescription("On your Nextcloud").assertIsDisplayed()
    }

    @Test
    fun uploadedAndVerified_saysTheSame() {
        show(UploadState.Uploaded(verifiedByChecksum = true))

        composeRule.onNodeWithContentDescription("On your Nextcloud").assertIsDisplayed()
    }

    @Test
    fun notUploaded_saysSo() {
        show(UploadState.NotUploaded)

        composeRule.onNodeWithContentDescription("Not on your Nextcloud").assertIsDisplayed()
    }

    @Test
    fun unknown_saysSo() {
        show(UploadState.Unknown)

        composeRule.onNodeWithContentDescription("Not checked yet").assertIsDisplayed()
    }

    /** The icon is 16 dp, centred in the 24 dp disc, which is 6 dp from the bottom left of the 100 dp cell. */
    @Test
    fun sitsAtTheBottomLeft() {
        show(UploadState.Unknown)

        composeRule.onNodeWithContentDescription("Not checked yet")
            .assertLeftPositionInRootIsEqualTo(10.dp)
            .assertTopPositionInRootIsEqualTo(74.dp)
    }
}
