package net.drehtuer.nextgallery.core.data.nextcloud

import net.drehtuer.nextgallery.core.common.AppError
import java.net.InetAddress
import java.net.UnknownHostException
import javax.inject.Inject

/**
 * On Android 17, connections to local addresses without `ACCESS_LOCAL_NETWORK` fail or time out (D39). This turns such
 * a failure into a clear error. It only runs after a failure, so looking up the name costs nothing in the normal case.
 */
class LocalNetworkPolicy(
    private val permission: LocalNetworkPermissionSource,
    private val resolve: (String) -> List<InetAddress>,
) {
    @Inject
    constructor(permission: LocalNetworkPermissionSource) : this(permission, { host ->
        InetAddress.getAllByName(host).toList()
    })

    fun refine(error: AppError, host: String): AppError = if (error is AppError.Network && !permission.isGranted() &&
        isLocal(host)
    ) {
        AppError.LocalNetworkBlocked
    } else {
        error
    }

    private fun isLocal(host: String): Boolean {
        val name = host.lowercase().trim('[', ']')
        val isSingleLabel = '.' !in name && ':' !in name
        return name.endsWith(".local") || isSingleLabel || addresses(name).any(::isLocalAddress)
    }

    private fun addresses(name: String): List<InetAddress> = try {
        resolve(name)
    } catch (_: UnknownHostException) {
        emptyList()
    }

    private fun isLocalAddress(address: InetAddress): Boolean {
        val bytes = address.address
        val first = bytes[0].toInt() and BYTE_MASK
        val second = bytes[1].toInt() and BYTE_MASK
        val isUniqueLocalV6 = bytes.size == IPV6_BYTES && first and ULA_MASK == ULA_PREFIX
        val isSharedV4 = bytes.size == IPV4_BYTES && first == CGNAT_FIRST && second in CGNAT_SECOND
        return address.isSiteLocalAddress || address.isLinkLocalAddress || address.isLoopbackAddress ||
            isUniqueLocalV6 || isSharedV4
    }

    private companion object {
        const val BYTE_MASK = 0xFF
        const val IPV4_BYTES = 4
        const val IPV6_BYTES = 16
        const val ULA_MASK = 0xFE
        const val ULA_PREFIX = 0xFC

        // 100.64.0.0/10, which VPNs such as Tailscale use.
        const val CGNAT_FIRST = 100
        val CGNAT_SECOND = 64..127
    }
}
