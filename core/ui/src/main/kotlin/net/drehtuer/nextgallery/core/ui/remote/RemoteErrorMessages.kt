package net.drehtuer.nextgallery.core.ui.remote

import androidx.annotation.StringRes
import net.drehtuer.nextgallery.core.common.AppError
import net.drehtuer.nextgallery.core.ui.R

/** What to tell the user about a failed remote operation. */
@StringRes
fun AppError.remoteMessage(): Int = when (this) {
    AppError.Auth -> R.string.core_ui_remote_error_auth

    AppError.NotLoggedIn -> R.string.core_ui_remote_error_not_logged_in

    AppError.NotFound -> R.string.core_ui_remote_error_not_found

    AppError.Forbidden, AppError.PermissionDenied -> R.string.core_ui_remote_error_forbidden

    AppError.Conflict -> R.string.core_ui_remote_error_conflict

    AppError.LocalNetworkBlocked -> R.string.core_ui_remote_error_local_network

    AppError.InvalidServerUrl -> R.string.core_ui_remote_error_invalid_url

    AppError.ServerMismatch -> R.string.core_ui_remote_error_server_mismatch

    AppError.LoginExpired -> R.string.core_ui_remote_error_login_expired

    AppError.InvalidName -> R.string.core_ui_remote_error_invalid_name

    is AppError.UntrustedCertificate -> R.string.core_ui_remote_error_certificate

    is AppError.Network, is AppError.Io -> R.string.core_ui_remote_error_network

    // Only the editor reports the last two; a remote operation cannot.
    is AppError.Server, is AppError.Unknown, is AppError.InsufficientMemory, is AppError.OriginalDamaged ->
        R.string.core_ui_remote_error_server
}
