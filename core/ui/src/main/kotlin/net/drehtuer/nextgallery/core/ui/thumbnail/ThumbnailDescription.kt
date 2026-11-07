package net.drehtuer.nextgallery.core.ui.thumbnail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import net.drehtuer.nextgallery.core.model.MediaItem
import net.drehtuer.nextgallery.core.ui.R
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** What a screen reader says for a thumbnail: the file name and the date, with English month names (D34). */
@Composable
fun thumbnailDescription(item: MediaItem): String {
    val pattern = stringResource(R.string.core_ui_thumbnail_date_pattern)
    val date = remember(pattern, item.dateTakenMillis) {
        DateTimeFormatter.ofPattern(pattern, Locale.ENGLISH)
            .format(Instant.ofEpochMilli(item.dateTakenMillis).atZone(ZoneId.systemDefault()))
    }
    return stringResource(R.string.core_ui_thumbnail_description, item.displayName, date)
}
