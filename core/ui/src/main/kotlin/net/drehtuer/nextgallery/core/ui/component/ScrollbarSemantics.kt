package net.drehtuer.nextgallery.core.ui.component

import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription

/**
 * Makes a scrollbar thumb work without dragging: a screen reader treats it as a slider, whose value it reads out and
 * can change (ACTION_SET_PROGRESS and the scroll forward and backward actions). [positionText] replaces the percentage
 * it would otherwise read, for example with the date at that position.
 */
fun Modifier.scrollbarSemantics(
    description: String,
    fraction: () -> Float,
    positionText: () -> String?,
    onScrollTo: (Float) -> Unit,
): Modifier = semantics {
    contentDescription = description
    progressBarRangeInfo = ProgressBarRangeInfo(fraction(), 0f..1f)
    positionText()?.let { stateDescription = it }
    setProgress { target ->
        onScrollTo(target.coerceIn(0f, 1f))
        true
    }
}
