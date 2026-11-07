package net.drehtuer.nextgallery

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.core.net.toUri
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import net.drehtuer.nextgallery.core.ui.theme.NextGalleryTheme
import net.drehtuer.nextgallery.core.ui.theme.isDark
import net.drehtuer.nextgallery.feature.viewer.ExternalImageScreen
import net.drehtuer.nextgallery.navigation.externalImageUri

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val externalImage = intent.externalImageUri()
        if (externalImage != null) {
            setContent { ExternalImageScreen(externalImage) }
            return
        }
        setContent {
            val state by viewModel.state.collectAsStateWithLifecycle()
            val accessRequest =
                rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
                    viewModel.onEvent(MainEvent.AccessRequestFinished)
                }
            LifecycleResumeEffect(Unit) {
                viewModel.onEvent(MainEvent.Resumed)
                onPauseOrDispose {}
            }
            val darkTheme = state.themeMode.isDark()
            DisposableEffect(darkTheme) {
                applySystemBarStyle(darkTheme)
                onDispose {}
            }
            NextGalleryTheme(darkTheme, state.accentColor, state.gridColumns) {
                NextGalleryAppContent(
                    state = state,
                    onRequestAccess = { accessRequest.launch(viewModel.requestedPermissions.toTypedArray()) },
                    onOpenSettings = ::openAppSettings,
                )
            }
        }
    }

    /** The system bars follow the app's theme, which can differ from the system's. */
    private fun applySystemBarStyle(darkTheme: Boolean) {
        val style = if (darkTheme) {
            SystemBarStyle.dark(Color.TRANSPARENT)
        } else {
            SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        }
        enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
    }

    private fun openAppSettings() {
        startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:$packageName".toUri()))
    }
}
