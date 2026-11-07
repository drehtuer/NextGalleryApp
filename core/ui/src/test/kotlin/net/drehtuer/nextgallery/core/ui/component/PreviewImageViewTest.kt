package net.drehtuer.nextgallery.core.ui.component

import android.graphics.Bitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.test.ext.junit.runners.AndroidJUnit4
import net.drehtuer.nextgallery.core.model.edit.PreviewImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PreviewImageViewTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun showsTheImage() {
        val bitmap = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888)

        composeRule.setContent { PreviewImageView(PreviewImage(bitmap, 4, 4), "photo") }

        composeRule.onNodeWithContentDescription("photo").assertIsDisplayed()
    }
}
