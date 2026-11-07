package net.drehtuer.nextgallery.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.drehtuer.nextgallery.core.model.AccentColor
import net.drehtuer.nextgallery.core.model.ThemeMode

/** Spacing and sizes from DESIGN.md. */
@Immutable
data class NextGalleryDimensions(
    val spacingSmall: Dp = 8.dp,
    val spacingMedium: Dp = 16.dp,
    val spacingLarge: Dp = 24.dp,
    val gridGap: Dp = 2.dp,
    val gridColumns: Int = 3,
    val folderCellMinSize: Dp = 160.dp,
    val selectionBorderWidth: Dp = 3.dp,
    val selectionRingSize: Dp = 24.dp,
    val selectionRingInset: Dp = 6.dp,
    val selectionRingStroke: Dp = 2.dp,
    val selectionIconSize: Dp = 16.dp,
    val scrollbarThumbWidth: Dp = 6.dp,
    val scrollbarThumbHeight: Dp = 48.dp,
    val scrollbarTouchWidth: Dp = 48.dp,
    val badgeSize: Dp = 24.dp,
    val badgeInset: Dp = 6.dp,
    val badgeIconSize: Dp = 16.dp,
    val clusterMarkerSize: Dp = 60.dp,
    val clusterMarkerBorder: Dp = 3.dp,
    val clusterPillHeight: Dp = 22.dp,
    val clusterSheetHeight: Dp = 600.dp,
    val clusterSheetColumns: Int = 4,
    val mapZoomButtonSize: Dp = 48.dp,
    val mapLocationButtonSize: Dp = 56.dp,
    val locationDotSize: Dp = 20.dp,
    val locationDotRing: Dp = 3.dp,
    val locationHaloSize: Dp = 48.dp,
    val dateRailInset: Dp = 18.dp,
    val dateRailTrackWidth: Dp = 2.dp,
    val dateRailLabelPadding: Dp = 4.dp,
    val dateRailLabelOpacity: Float = 0.88f,
    val iconTileSize: Dp = 72.dp,
    val iconTileIconSize: Dp = 36.dp,
    val accentSwatchSize: Dp = 52.dp,
    val editorToolPillWidth: Dp = 64.dp,
    val editorToolPillHeight: Dp = 32.dp,
    val cropFrameWidth: Dp = 2.dp,
    val cropThirdsWidth: Dp = 1.dp,
    val cropHandleWidth: Dp = 5.dp,
    val cropHandleLength: Dp = 20.dp,
    val cropTouchTolerance: Dp = 24.dp,
    val penSwatchSize: Dp = 36.dp,
    val breadcrumbHeight: Dp = 32.dp,
    val contextMenuWidth: Dp = 232.dp,
)

private val LocalNextGalleryDimensions = staticCompositionLocalOf { NextGalleryDimensions() }

private val LocalNextGalleryMapColors = staticCompositionLocalOf { nextGalleryMapColors(darkTheme = false) }

private val NextGalleryTypography = Typography(
    headlineMedium = textStyle(size = 32, line = 40, FontWeight.Normal),
    headlineSmall = textStyle(size = 24, line = 32, FontWeight.Normal),
    titleLarge = textStyle(size = 22, line = 28, FontWeight.Normal),
    titleMedium = textStyle(size = 18, line = 26, FontWeight.Medium),
    bodyLarge = textStyle(size = 16, line = 24, FontWeight.Normal),
    bodyMedium = textStyle(size = 14, line = 20, FontWeight.Normal),
    bodySmall = textStyle(size = 13, line = 18, FontWeight.Normal),
    labelLarge = textStyle(size = 14, line = 20, FontWeight.Medium),
    labelMedium = textStyle(size = 12, line = 16, FontWeight.Medium),
)

private val NextGalleryShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

private fun textStyle(size: Int, line: Int, weight: FontWeight) =
    TextStyle(fontFamily = FontFamily.Default, fontSize = size.sp, lineHeight = line.sp, fontWeight = weight)

/** Counts and times line up with tabular figures. */
val TextStyle.tabularFigures: TextStyle
    get() = copy(fontFeatureSettings = "tnum")

object NextGalleryTheme {
    val dimensions: NextGalleryDimensions
        @Composable
        @ReadOnlyComposable
        get() = LocalNextGalleryDimensions.current

    val mapColors: NextGalleryMapColors
        @Composable
        @ReadOnlyComposable
        get() = LocalNextGalleryMapColors.current
}

/** App theme with the values from DESIGN.md (D22). Features must only use values from this theme. */
@Composable
fun NextGalleryTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    accent: AccentColor = AccentColor.BLUE,
    gridColumns: Int = NextGalleryDimensions().gridColumns,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalNextGalleryMapColors provides nextGalleryMapColors(darkTheme)) {
        AppliedTheme(
            nextGalleryColorScheme(accent, darkTheme),
            NextGalleryDimensions(gridColumns = gridColumns),
            content,
        )
    }
}

/** Whether this mode shows the dark theme right now. */
@Composable
@ReadOnlyComposable
fun ThemeMode.isDark(): Boolean = when (this) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}

/** For the viewer and the editor, which stay black in every theme. */
@Composable
fun NextGalleryViewerTheme(content: @Composable () -> Unit) {
    AppliedTheme(viewerColorScheme(), NextGalleryDimensions(), content)
}

@Composable
private fun AppliedTheme(
    colorScheme: androidx.compose.material3.ColorScheme,
    dimensions: NextGalleryDimensions,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalNextGalleryDimensions provides dimensions) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = NextGalleryTypography,
            shapes = NextGalleryShapes,
            content = content,
        )
    }
}
