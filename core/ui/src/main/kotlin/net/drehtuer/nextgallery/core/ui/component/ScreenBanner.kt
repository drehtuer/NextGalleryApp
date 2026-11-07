package net.drehtuer.nextgallery.core.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf

/** The banner `:app` wants shown under the top bar of a screen, e.g. for limited photo access. */
val LocalScreenBanner = compositionLocalOf<@Composable () -> Unit> { {} }

@Composable
fun ScreenBanner() {
    LocalScreenBanner.current()
}
