package net.drehtuer.nextgallery.feature.viewer

import android.text.format.Formatter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import net.drehtuer.nextgallery.core.data.map.label
import net.drehtuer.nextgallery.core.model.ImageDetails
import net.drehtuer.nextgallery.core.ui.theme.NextGalleryTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageInfoSheet(details: ImageDetails, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) { ImageInfoContent(details) }
}

/** Values the image does not have are left out. */
@Composable
internal fun ImageInfoContent(details: ImageDetails, modifier: Modifier = Modifier) {
    val dimensions = NextGalleryTheme.dimensions
    val date = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
        .withZone(ZoneId.systemDefault())
        .format(Instant.ofEpochMilli(details.dateTakenMillis))
    Column(
        modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(dimensions.spacingMedium),
        verticalArrangement = Arrangement.spacedBy(dimensions.spacingSmall),
    ) {
        InfoRow(R.string.feature_viewer_info_name, details.displayName)
        InfoRow(R.string.feature_viewer_info_date, date)
        InfoRow(R.string.feature_viewer_info_size, Formatter.formatFileSize(LocalContext.current, details.sizeBytes))
        InfoRow(
            R.string.feature_viewer_info_resolution,
            stringResource(R.string.feature_viewer_info_resolution_value, details.width, details.height),
        )
        InfoRow(R.string.feature_viewer_info_type, details.mimeType)
        InfoRow(R.string.feature_viewer_info_folder, details.relativePath)
        InfoRow(R.string.feature_viewer_info_location, locationText(details))
        InfoRow(R.string.feature_viewer_info_camera, details.camera)
        InfoRow(R.string.feature_viewer_info_exposure, details.exposureTime)
        InfoRow(R.string.feature_viewer_info_aperture, details.aperture)
        InfoRow(R.string.feature_viewer_info_iso, details.iso)
        InfoRow(R.string.feature_viewer_info_focal_length, details.focalLength)
    }
}

/** "near Desenzano del Garda, Italy · 45.62° N, 10.68° E"; only the coordinates without a town nearby (D45). */
@Composable
private fun locationText(details: ImageDetails): String? {
    val coordinates = details.location?.formatted()
    return details.place?.takeIf { coordinates != null }?.let {
        stringResource(R.string.feature_viewer_info_location_near, it.label(), coordinates.orEmpty())
    } ?: coordinates
}

@Composable
private fun InfoRow(label: Int, value: String?) {
    if (value == null) return
    Column {
        Text(stringResource(label), style = MaterialTheme.typography.labelMedium)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}
