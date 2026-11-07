package net.drehtuer.nextgallery

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Reads the file of the release build directly, because tests run against the debug build's resources (D19). */
class NetworkSecurityConfigTest {
    private val config = File("src/main/res/xml/network_security_config.xml").readText()

    @Test
    fun releaseConfig_disallowsCleartext() {
        assertTrue(config.contains("""cleartextTrafficPermitted="false""""))
    }

    @Test
    fun releaseConfig_hasNoExceptionForADomain() {
        assertFalse(config.contains("domain-config"))
    }

    @Test
    fun releaseConfig_trustsSystemAndUserCasOnly() {
        assertTrue(config.contains("""<certificates src="system" />"""))
        assertTrue(config.contains("""<certificates src="user" """))
    }

    @Test
    fun manifest_usesTheConfig() {
        val manifest = File("src/main/AndroidManifest.xml").readText()

        assertTrue(manifest.contains("""android:networkSecurityConfig="@xml/network_security_config""""))
    }
}
