package net.drehtuer.nextgallery.buildlogic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.time.Instant

class ApkNamesTest {
    @get:Rule
    val folder = TemporaryFolder()

    @Test
    fun release_signed_isNamedAfterTheVersion() {
        assertEquals("NextGallery-1.2.3.apk", ApkNames.release("1.2.3", signed = true))
    }

    @Test
    fun release_unsigned_saysSo() {
        assertEquals("NextGallery-1.2.3-unsigned.apk", ApkNames.release("1.2.3", signed = false))
    }

    @Test
    fun debug_addsTheBuildTimeInUtc() {
        val builtAt = Instant.parse("2026-11-07T22:15:00Z")

        assertEquals("NextGallery-debug-1.2.3-20261107-221500.apk", ApkNames.debug("1.2.3", builtAt))
    }

    @Test
    fun copy_writesTheApkUnderTheGivenName() {
        val apk = folder.newFile("app-release.apk").apply { writeText("apk") }
        val directory = File(folder.root, "out")

        ApkNames.copy(apk, directory, "NextGallery-1.2.3.apk")

        assertEquals("apk", File(directory, "NextGallery-1.2.3.apk").readText())
    }

    @Test
    fun copy_removesApksOfEarlierBuilds() {
        val apk = folder.newFile("app-debug.apk")
        val directory = folder.newFolder("out")
        val earlier = File(directory, "NextGallery-debug-1.2.3-20261107-221500.apk").apply { writeText("old") }

        ApkNames.copy(apk, directory, "NextGallery-debug-1.2.3-20261107-221600.apk")

        assertFalse(earlier.exists())
    }
}
