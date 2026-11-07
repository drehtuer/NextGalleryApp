package net.drehtuer.nextgallery.buildlogic

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ForbiddenDependenciesTest {
    @Test
    fun isForbidden_playServices_isTrue() {
        assertTrue(ForbiddenDependencies.isForbidden("com.google.android.gms"))
    }

    @Test
    fun isForbidden_firebase_isTrue() {
        assertTrue(ForbiddenDependencies.isForbidden("com.google.firebase"))
    }

    @Test
    fun isForbidden_subgroup_isTrue() {
        assertTrue(ForbiddenDependencies.isForbidden("com.google.android.gms.foo"))
    }

    @Test
    fun isForbidden_tink_isFalse() {
        assertFalse(ForbiddenDependencies.isForbidden("com.google.crypto.tink"))
    }

    @Test
    fun isForbidden_similarName_isFalse() {
        assertFalse(ForbiddenDependencies.isForbidden("com.google.firebaseish"))
    }

    @Test
    fun isShipped_releaseRuntimeClasspath_isTrue() {
        assertTrue(ForbiddenDependencies.isShipped("releaseRuntimeClasspath"))
    }

    @Test
    fun isShipped_testRuntimeClasspath_isFalse() {
        assertFalse(ForbiddenDependencies.isShipped("debugUnitTestRuntimeClasspath"))
    }

    @Test
    fun isShipped_compileClasspath_isFalse() {
        assertFalse(ForbiddenDependencies.isShipped("releaseCompileClasspath"))
    }
}
