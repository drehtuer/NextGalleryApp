package net.drehtuer.nextgallery.feature.settings

import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LoginPageIntentTest {
    private val intent = loginPageIntent("https://cloud.example.com/login")

    @Test
    fun loginPageIntent_opensTheAddress() {
        assertEquals("https://cloud.example.com/login", intent.dataString)
    }

    @Test
    fun loginPageIntent_isAPlainViewForTheDefaultBrowser() {
        assertEquals(Intent.ACTION_VIEW, intent.action)
    }

    @Test
    fun loginPageIntent_namesNoBrowser() {
        assertEquals(null, intent.`package`)
    }

    @Test
    fun loginPageIntent_isBrowsable() {
        assertTrue(intent.hasCategory(Intent.CATEGORY_BROWSABLE))
    }

    @Test
    fun loginPageIntent_opensInItsOwnTask() {
        assertTrue(intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
    }
}
