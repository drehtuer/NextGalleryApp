package net.drehtuer.nextgallery

import android.os.StrictMode
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DebugStrictModeTest {
    private val threadPolicyBefore = StrictMode.getThreadPolicy()
    private val vmPolicyBefore = StrictMode.getVmPolicy()

    @After
    fun restore() {
        StrictMode.setThreadPolicy(threadPolicyBefore)
        StrictMode.setVmPolicy(vmPolicyBefore)
    }

    @Test
    fun enableStrictMode_setsAThreadPolicy() {
        StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.LAX)

        enableStrictMode()

        assertNotEquals(StrictMode.ThreadPolicy.LAX.toString(), StrictMode.getThreadPolicy().toString())
    }

    @Test
    fun enableStrictMode_setsAVmPolicy() {
        StrictMode.setVmPolicy(StrictMode.VmPolicy.LAX)

        enableStrictMode()

        assertNotEquals(StrictMode.VmPolicy.LAX.toString(), StrictMode.getVmPolicy().toString())
    }
}
