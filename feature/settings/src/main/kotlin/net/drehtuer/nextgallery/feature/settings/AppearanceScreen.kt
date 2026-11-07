package net.drehtuer.nextgallery.feature.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import net.drehtuer.nextgallery.core.model.AccentColor
import net.drehtuer.nextgallery.core.model.ThemeMode
import net.drehtuer.nextgallery.core.model.UserSettings
import net.drehtuer.nextgallery.core.ui.theme.NextGalleryTheme
import net.drehtuer.nextgallery.core.ui.theme.isDark
import net.drehtuer.nextgallery.core.ui.theme.nextGalleryColorScheme
import net.drehtuer.nextgallery.core.ui.thumbnail.LARGE_THUMBNAIL_PX
import net.drehtuer.nextgallery.core.ui.thumbnail.ThumbnailCell
import net.drehtuer.nextgallery.core.ui.R as CoreUiR

internal const val COLUMN_PREVIEW_CELL_TAG = "column-preview-cell"
internal const val ACCENT_CHECK_TAG = "accent-check"
private const val COLUMN_PREVIEW_ROWS = 2

@Composable
fun AppearanceScreen(
    state: AppearanceUiState,
    onEvent: (AppearanceEvent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsScaffold(stringResource(R.string.feature_settings_appearance), onBack, modifier) { contentModifier ->
        SettingsColumn(contentModifier) {
            SectionTitle(stringResource(R.string.feature_settings_theme))
            SegmentedChoice(
                options = ThemeMode.entries,
                selected = state.themeMode,
                onSelect = { onEvent(AppearanceEvent.ThemeModeSelected(it)) },
                label = { stringResource(it.label) },
            )
            SettingsNote(stringResource(R.string.feature_settings_theme_note))

            SectionTitle(stringResource(R.string.feature_settings_grid_columns))
            SegmentedChoice(
                options = UserSettings.GRID_COLUMN_OPTIONS,
                selected = state.gridColumns,
                onSelect = { onEvent(AppearanceEvent.GridColumnsSelected(it)) },
                label = { it.toString() },
            )
            ColumnPreview(state.gridColumns)

            SectionTitle(stringResource(R.string.feature_settings_accent))
            AccentSwatches(state, onEvent)
            SettingsNote(stringResource(R.string.feature_settings_accent_note, stringResource(state.accentColor.label)))
        }
    }
}

/** Two rows of flat cells at the chosen column count. */
@Composable
private fun ColumnPreview(columns: Int) {
    val gap = NextGalleryTheme.dimensions.gridGap
    Column(verticalArrangement = Arrangement.spacedBy(gap)) {
        repeat(COLUMN_PREVIEW_ROWS) {
            Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                repeat(columns) {
                    ThumbnailCell(
                        thumbnail = null,
                        contentDescription = null,
                        sizePx = LARGE_THUMBNAIL_PX,
                        modifier = Modifier.weight(1f).testTag(COLUMN_PREVIEW_CELL_TAG),
                    )
                }
            }
        }
    }
}

@Composable
private fun AccentSwatches(state: AppearanceUiState, onEvent: (AppearanceEvent) -> Unit) {
    val dark = state.themeMode.isDark()
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        AccentColor.entries.forEach { accent ->
            val scheme = nextGalleryColorScheme(accent, dark)
            AccentSwatch(
                name = stringResource(accent.label),
                selected = accent == state.accentColor,
                fill = scheme.primary,
                onFill = scheme.onPrimary,
                onClick = { onEvent(AppearanceEvent.AccentSelected(accent)) },
            )
        }
    }
}

@Composable
private fun AccentSwatch(
    name: String,
    selected: Boolean,
    fill: androidx.compose.ui.graphics.Color,
    onFill: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
) {
    val dimensions = NextGalleryTheme.dimensions
    val ring = if (selected) {
        Modifier.border(dimensions.selectionBorderWidth, MaterialTheme.colorScheme.onSurface, CircleShape)
    } else {
        Modifier
    }
    Box(
        modifier = Modifier
            .size(dimensions.accentSwatchSize)
            .clip(CircleShape)
            .background(fill)
            .then(ring)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .semantics { contentDescription = name },
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Icon(
                painterResource(CoreUiR.drawable.core_ui_ic_check),
                contentDescription = null,
                modifier = Modifier.testTag(ACCENT_CHECK_TAG),
                tint = onFill,
            )
        }
    }
}

@get:StringRes
private val ThemeMode.label: Int
    get() = when (this) {
        ThemeMode.SYSTEM -> R.string.feature_settings_theme_system
        ThemeMode.LIGHT -> R.string.feature_settings_theme_light
        ThemeMode.DARK -> R.string.feature_settings_theme_dark
    }

@get:StringRes
private val AccentColor.label: Int
    get() = when (this) {
        AccentColor.BLUE -> R.string.feature_settings_accent_blue
        AccentColor.TEAL -> R.string.feature_settings_accent_teal
        AccentColor.PLUM -> R.string.feature_settings_accent_plum
        AccentColor.MOSS -> R.string.feature_settings_accent_moss
        AccentColor.CORAL -> R.string.feature_settings_accent_coral
    }
