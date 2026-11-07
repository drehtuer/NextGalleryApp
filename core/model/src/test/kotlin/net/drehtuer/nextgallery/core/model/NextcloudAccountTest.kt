package net.drehtuer.nextgallery.core.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NextcloudAccountTest {
    private val account = NextcloudAccount("https://cloud.example.com", "anna", "anna-id", "s3cret-app-password")

    @Test
    fun toString_doesNotContainTheAppPassword() {
        assertFalse(account.toString().contains("s3cret-app-password"))
    }

    @Test
    fun toString_marksThePasswordAsRedacted() {
        assertTrue(account.toString().contains("appPassword=<redacted>"))
    }
}
