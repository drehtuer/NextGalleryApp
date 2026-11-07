package net.drehtuer.nextgallery.core.data.nextcloud

import net.drehtuer.nextgallery.core.common.AppError
import org.junit.Assert.assertEquals
import org.junit.Test
import java.net.InetAddress
import java.net.UnknownHostException

class LocalNetworkPolicyTest {
    private val denied = object : LocalNetworkPermissionSource {
        override fun isGranted() = false
    }
    private val networkError = AppError.Network("timeout")

    private fun policy(
        permission: LocalNetworkPermissionSource = denied,
        resolve: (String) -> List<InetAddress> = { listOf(InetAddress.getByName(it)) },
    ) = LocalNetworkPolicy(permission, resolve)

    private fun publicName(): (String) -> List<InetAddress> = { listOf(InetAddress.getByName("93.184.216.34")) }

    @Test
    fun refine_privateIpv4WithoutPermission_isBlocked() {
        assertEquals(AppError.LocalNetworkBlocked, policy().refine(networkError, "192.168.1.5"))
    }

    @Test
    fun refine_localNameWithoutPermission_isBlocked() {
        assertEquals(AppError.LocalNetworkBlocked, policy(resolve = publicName()).refine(networkError, "nas.local"))
    }

    @Test
    fun refine_singleLabelNameWithoutPermission_isBlocked() {
        assertEquals(AppError.LocalNetworkBlocked, policy(resolve = publicName()).refine(networkError, "nas"))
    }

    @Test
    fun refine_nameResolvingToPrivateAddress_isBlocked() {
        val private = { _: String -> listOf(InetAddress.getByName("10.0.0.2")) }

        assertEquals(AppError.LocalNetworkBlocked, policy(resolve = private).refine(networkError, "cloud.home.arpa"))
    }

    @Test
    fun refine_uniqueLocalIpv6_isBlocked() {
        assertEquals(AppError.LocalNetworkBlocked, policy().refine(networkError, "[fd00::1]"))
    }

    @Test
    fun refine_vpnRange_isBlocked() {
        assertEquals(AppError.LocalNetworkBlocked, policy().refine(networkError, "100.100.1.1"))
    }

    @Test
    fun refine_publicAddress_staysNetworkError() {
        assertEquals(networkError, policy().refine(networkError, "93.184.216.34"))
    }

    @Test
    fun refine_unknownName_staysNetworkError() {
        val unknown = { _: String -> throw UnknownHostException() }

        assertEquals(networkError, policy(resolve = unknown).refine(networkError, "cloud.example.com"))
    }

    @Test
    fun refine_permissionGranted_staysNetworkError() {
        assertEquals(networkError, policy(permission = GrantedLocalNetwork).refine(networkError, "192.168.1.5"))
    }

    @Test
    fun refine_otherError_staysAsItIs() {
        assertEquals(AppError.Auth, policy().refine(AppError.Auth, "192.168.1.5"))
    }
}
