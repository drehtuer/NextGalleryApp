package net.drehtuer.nextgallery.feature.settings

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable

/**
 * [returnAfterSignIn]: opened from the Nextcloud tab, which continues once the user is signed in.
 * [signInAgain]: the session ended, so the form is shown although there is an account.
 */
@Serializable
data class NextcloudAccountRoute(val returnAfterSignIn: Boolean = false, val signInAgain: Boolean = false)

@Serializable
data object TrustedCertificatesRoute

fun NavGraphBuilder.nextcloudAccountScreen(onBack: () -> Unit, onOpenCertificates: () -> Unit) {
    composable<NextcloudAccountRoute> { entry ->
        val route = entry.toRoute<NextcloudAccountRoute>()
        val viewModel = hiltViewModel<NextcloudSettingsViewModel, NextcloudSettingsViewModel.Factory>(
            creationCallback = { factory -> factory.create(route.signInAgain) },
        )
        val state by viewModel.state.collectAsStateWithLifecycle()
        val context = LocalContext.current
        val permissionRequest = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
            viewModel.onEvent(NextcloudSettingsEvent.LocalNetworkRequestFinished)
        }
        LifecycleResumeEffect(viewModel) {
            viewModel.onEvent(NextcloudSettingsEvent.Resumed)
            onPauseOrDispose { viewModel.onEvent(NextcloudSettingsEvent.Paused) }
        }
        LaunchedEffect(viewModel) {
            viewModel.effects.collect { effect ->
                when (effect) {
                    is NextcloudSettingsEffect.OpenBrowser -> context.openLoginPage(effect.url)
                    is NextcloudSettingsEffect.RequestPermission -> permissionRequest.launch(effect.permission)
                    NextcloudSettingsEffect.SignedIn -> if (route.returnAfterSignIn) onBack()
                }
            }
        }
        NextcloudSettingsScreen(state, viewModel::onEvent, onBack, onOpenCertificates)
    }
}

fun NavGraphBuilder.trustedCertificatesScreen(onBack: () -> Unit) {
    composable<TrustedCertificatesRoute> {
        val viewModel = hiltViewModel<TrustedCertificatesViewModel>()
        val certificates by viewModel.state.collectAsStateWithLifecycle()
        TrustedCertificatesScreen(certificates, viewModel::remove, onBack)
    }
}

/** The login page opens in the default browser (D54), which shows its own certificate warning (D32). */
internal fun loginPageIntent(url: String): Intent = Intent(Intent.ACTION_VIEW, url.toUri())
    .addCategory(Intent.CATEGORY_BROWSABLE)
    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

private fun Context.openLoginPage(url: String) {
    try {
        startActivity(loginPageIntent(url))
    } catch (_: ActivityNotFoundException) {
        // No browser at all: the flow simply runs into its timeout; the user sees the waiting state and can cancel.
    }
}
