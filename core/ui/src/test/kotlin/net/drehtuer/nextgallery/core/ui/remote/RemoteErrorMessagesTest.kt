package net.drehtuer.nextgallery.core.ui.remote

import net.drehtuer.nextgallery.core.common.AppError
import net.drehtuer.nextgallery.core.model.CertificateInfo
import net.drehtuer.nextgallery.core.ui.R
import org.junit.Assert.assertEquals
import org.junit.Test

class RemoteErrorMessagesTest {
    @Test
    fun remoteMessage_network_explainsTheConnection() {
        assertEquals(R.string.core_ui_remote_error_network, AppError.Network("x").remoteMessage())
    }

    @Test
    fun remoteMessage_io_explainsTheConnection() {
        assertEquals(R.string.core_ui_remote_error_network, AppError.Io("x").remoteMessage())
    }

    @Test
    fun remoteMessage_auth_saysTheSessionEnded() {
        assertEquals(R.string.core_ui_remote_error_auth, AppError.Auth.remoteMessage())
    }

    @Test
    fun remoteMessage_notLoggedIn() {
        assertEquals(R.string.core_ui_remote_error_not_logged_in, AppError.NotLoggedIn.remoteMessage())
    }

    @Test
    fun remoteMessage_notFound() {
        assertEquals(R.string.core_ui_remote_error_not_found, AppError.NotFound.remoteMessage())
    }

    @Test
    fun remoteMessage_forbidden() {
        assertEquals(R.string.core_ui_remote_error_forbidden, AppError.Forbidden.remoteMessage())
    }

    @Test
    fun remoteMessage_conflict() {
        assertEquals(R.string.core_ui_remote_error_conflict, AppError.Conflict.remoteMessage())
    }

    @Test
    fun remoteMessage_localNetworkBlocked_namesThePermission() {
        assertEquals(R.string.core_ui_remote_error_local_network, AppError.LocalNetworkBlocked.remoteMessage())
    }

    @Test
    fun remoteMessage_invalidUrl() {
        assertEquals(R.string.core_ui_remote_error_invalid_url, AppError.InvalidServerUrl.remoteMessage())
    }

    @Test
    fun remoteMessage_loginExpired() {
        assertEquals(R.string.core_ui_remote_error_login_expired, AppError.LoginExpired.remoteMessage())
    }

    @Test
    fun remoteMessage_invalidName() {
        assertEquals(R.string.core_ui_remote_error_invalid_name, AppError.InvalidName.remoteMessage())
    }

    @Test
    fun remoteMessage_untrustedCertificate() {
        val error = AppError.UntrustedCertificate(CertificateInfo("h", "s", "i", 1, 2, "AA"))

        assertEquals(R.string.core_ui_remote_error_certificate, error.remoteMessage())
    }

    @Test
    fun remoteMessage_server() {
        assertEquals(R.string.core_ui_remote_error_server, AppError.Server(500).remoteMessage())
    }

    @Test
    fun remoteMessage_serverMismatch_saysSo() {
        assertEquals(R.string.core_ui_remote_error_server_mismatch, AppError.ServerMismatch.remoteMessage())
    }
}
