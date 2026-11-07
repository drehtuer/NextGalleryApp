package net.drehtuer.nextgallery.feature.editor

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import net.drehtuer.nextgallery.core.model.edit.ImageSize
import net.drehtuer.nextgallery.core.model.edit.ScaleMath
import net.drehtuer.nextgallery.core.ui.theme.NextGalleryTheme

@Composable
fun ScaleDialog(original: ImageSize, onApply: (ImageSize) -> Unit, onDismiss: () -> Unit) {
    var input by remember(original) { mutableStateOf(ScaleInput.initial(original)) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.feature_editor_scale_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(NextGalleryTheme.dimensions.spacingSmall)) {
                NumberField(R.string.feature_editor_scale_percent, input.percent) { input = input.withPercent(it) }
                NumberField(R.string.feature_editor_scale_width, input.width) { input = input.withWidth(it) }
                NumberField(R.string.feature_editor_scale_height, input.height) { input = input.withHeight(it) }
                Text(summary(input))
            }
        },
        confirmButton = {
            TextButton(onClick = { input.target?.let(onApply) }, enabled = input.canApply) {
                Text(stringResource(R.string.feature_editor_scale_apply))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.feature_editor_cancel)) }
        },
    )
}

@Composable
private fun NumberField(@StringRes label: Int, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(stringResource(label)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
    )
}

@Composable
private fun summary(input: ScaleInput): String {
    val original = input.original
    val megapixels = ScaleMath.megapixels(original)
    return input.target?.let { target ->
        stringResource(
            R.string.feature_editor_scale_summary,
            original.width,
            original.height,
            megapixels,
            ScaleMath.megapixels(target),
        )
    } ?: stringResource(R.string.feature_editor_scale_summary_no_target, original.width, original.height, megapixels)
}
