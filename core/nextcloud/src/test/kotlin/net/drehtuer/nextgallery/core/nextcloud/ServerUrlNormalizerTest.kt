package net.drehtuer.nextgallery.core.nextcloud

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ServerUrlNormalizerTest {
    private val normalizer = ServerUrlNormalizer(allowCleartext = false)

    @Test
    fun normalize_noScheme_addsHttps() {
        assertEquals("https://cloud.example.com", normalizer.normalize("cloud.example.com"))
    }

    @Test
    fun normalize_trailingSlash_isRemoved() {
        assertEquals("https://cloud.example.com", normalizer.normalize("https://cloud.example.com/"))
    }

    @Test
    fun normalize_indexPhp_isRemoved() {
        assertEquals("https://cloud.example.com", normalizer.normalize("https://cloud.example.com/index.php"))
    }

    @Test
    fun normalize_indexPhpWithSlash_isRemoved() {
        assertEquals("https://cloud.example.com", normalizer.normalize("https://cloud.example.com/index.php/"))
    }

    @Test
    fun normalize_subdirectoryInstall_keepsPath() {
        assertEquals("https://example.com/nextcloud", normalizer.normalize("example.com/nextcloud/index.php"))
    }

    @Test
    fun normalize_port_isKept() {
        assertEquals("https://cloud.example.com:8443", normalizer.normalize("cloud.example.com:8443"))
    }

    @Test
    fun normalize_surroundingSpaces_areRemoved() {
        assertEquals("https://cloud.example.com", normalizer.normalize("  cloud.example.com "))
    }

    @Test
    fun normalize_queryAndFragment_areRemoved() {
        assertEquals("https://cloud.example.com", normalizer.normalize("https://cloud.example.com/?a=1#b"))
    }

    @Test
    fun normalize_upperCaseHost_isLowered() {
        assertEquals("https://cloud.example.com", normalizer.normalize("HTTPS://Cloud.Example.COM"))
    }

    @Test
    fun normalize_httpWithoutCleartext_isRejected() {
        assertThrows(InvalidServerUrlException::class.java) { normalizer.normalize("http://cloud.example.com") }
    }

    @Test
    fun normalize_httpWithCleartext_isKept() {
        val debug = ServerUrlNormalizer(allowCleartext = true)

        assertEquals("http://10.0.2.2:8080", debug.normalize("http://10.0.2.2:8080"))
    }

    @Test
    fun normalize_otherScheme_isRejected() {
        assertThrows(InvalidServerUrlException::class.java) { normalizer.normalize("ftp://cloud.example.com") }
    }

    @Test
    fun normalize_empty_isRejected() {
        assertThrows(InvalidServerUrlException::class.java) { normalizer.normalize("   ") }
    }

    @Test
    fun normalize_notAUrl_isRejected() {
        assertThrows(InvalidServerUrlException::class.java) { normalizer.normalize("https://") }
    }

    @Test
    fun normalize_embeddedCredentials_areRejected() {
        assertThrows(InvalidServerUrlException::class.java) { normalizer.normalize("https://a:b@cloud.example.com") }
    }

    @Test
    fun isSameOrigin_sameSchemeHostAndPort_isTrue() {
        assertTrue(isSameOrigin("https://CLOUD.example.com:443/a", "https://cloud.example.com/b"))
    }

    @Test
    fun isSameOrigin_anotherHost_isFalse() {
        assertFalse(isSameOrigin("https://a.example.com", "https://b.example.com"))
    }

    @Test
    fun isSameOrigin_anotherPort_isFalse() {
        assertFalse(isSameOrigin("https://a.example.com", "https://a.example.com:8443"))
    }

    @Test
    fun isSameOrigin_anotherScheme_isFalse() {
        assertFalse(isSameOrigin("https://a.example.com", "http://a.example.com"))
    }

    @Test
    fun isSameOrigin_notAUrl_isFalse() {
        assertFalse(isSameOrigin("https://a.example.com", "nonsense"))
    }
}
