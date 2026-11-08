package net.drehtuer.nextgallery.buildlogic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DependencyFloorsTest {
    @Test
    fun raise_olderVersion_returnsTheFloor() {
        assertEquals("33.7.2-jre", DependencyFloors.raise("com.google.guava", "guava", "33.4.0-jre"))
    }

    @Test
    fun raise_newerVersion_keepsIt() {
        assertNull(DependencyFloors.raise("com.google.guava", "guava", "34.0.0-jre"))
    }

    @Test
    fun raise_moduleWithoutFloor_keepsIt() {
        assertNull(DependencyFloors.raise("com.example", "library", "1.0"))
    }

    @Test
    fun raise_withoutVersion_keepsIt() {
        assertNull(DependencyFloors.raise("com.google.guava", "guava", ""))
    }

    @Test
    fun isOlder_comparesPartsAsNumbers() {
        assertTrue(DependencyFloors.isOlder("1.9", "1.10"))
    }

    @Test
    fun isOlder_countsAMissingPartAsZero() {
        assertTrue(DependencyFloors.isOlder("2.0.6", "2.0.6.1"))
    }

    @Test
    fun isOlder_ignoresTheQualifier() {
        assertFalse(DependencyFloors.isOlder("33.7.2-android", "33.7.2-jre"))
    }
}
