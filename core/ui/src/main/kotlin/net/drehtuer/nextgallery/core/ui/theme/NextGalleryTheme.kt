package net.drehtuer.nextgallery.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Spacing and sizes. Placeholder values until the design from Claude Design is delivered (D22). */
@Immutable
data class NextGalleryDimensions(
    val spacingSmall: Dp = 8.dp,
    val spacingMedium: Dp = 16.dp,
    val gridGap: Dp = 2.dp,
    val mediaCellMinSize: Dp = 96.dp,
    val folderCellMinSize: Dp = 160.dp,
    val selectionBorderWidth: Dp = 3.dp,
    val scrollbarThumbWidth: Dp = 6.dp,
    val scrollbarThumbHeight: Dp = 48.dp,
    val scrollbarTouchWidth: Dp = 48.dp,
)

private val LocalNextGalleryDimensions = staticCompositionLocalOf { NextGalleryDimensions() }

object NextGalleryTheme {
    val dimensions: NextGalleryDimensions
        @Composable
        @ReadOnlyComposable
        get() = LocalNextGalleryDimensions.current
}

/**
 * App theme. Colours, typography, shapes and dimensions are placeholders until the design from
 * Claude Design is delivered (D22); features must only use values from this theme.
 */
@Composable
fun NextGalleryTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalNextGalleryDimensions provides NextGalleryDimensions()) {
        MaterialTheme(
            colorScheme = if (darkTheme) darkColorScheme() else lightColorScheme(),
            content = content,
        )
    }
}
