package net.drehtuer.nextgallery.core.ui.component

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import net.drehtuer.nextgallery.core.model.edit.PreviewImage

/** Draws a rendered image; only this module and the adapter know that a `Bitmap` is inside (D25). */
@Composable
fun PreviewImageView(image: PreviewImage, contentDescription: String?, modifier: Modifier = Modifier) {
    val bitmap = remember(image) { (image.handle as Bitmap).asImageBitmap() }
    Image(bitmap, contentDescription, modifier, contentScale = ContentScale.FillBounds)
}
