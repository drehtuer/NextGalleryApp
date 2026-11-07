package net.drehtuer.nextgallery.core.ui.thumbnail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import net.drehtuer.nextgallery.core.model.UploadState
import net.drehtuer.nextgallery.core.ui.R
import net.drehtuer.nextgallery.core.ui.theme.NextGalleryOverlayColors
import net.drehtuer.nextgallery.core.ui.theme.NextGalleryTheme

/** For the badge slot of [ThumbnailCell]: at the bottom left, readable over any photo. */
@Composable
fun BoxScope.UploadBadge(state: UploadState, modifier: Modifier = Modifier) {
    val dimensions = NextGalleryTheme.dimensions
    val (icon, description) = when (state) {
        is UploadState.Uploaded -> R.drawable.core_ui_ic_cloud_check to R.string.core_ui_upload_state_uploaded
        UploadState.NotUploaded -> R.drawable.core_ui_ic_cloud to R.string.core_ui_upload_state_not_uploaded
        UploadState.Unknown -> R.drawable.core_ui_ic_cloud_question to R.string.core_ui_upload_state_unknown
    }
    Box(
        modifier
            .align(Alignment.BottomStart)
            .padding(dimensions.badgeInset)
            .size(dimensions.badgeSize)
            .background(NextGalleryOverlayColors.badgeBacking, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painterResource(icon),
            contentDescription = stringResource(description),
            modifier = Modifier.size(dimensions.badgeIconSize),
            tint = NextGalleryOverlayColors.badgeOn,
        )
    }
}
