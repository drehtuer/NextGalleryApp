package net.drehtuer.nextgallery.core.data.map

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultMapPermissionsTest {
    @Test
    fun photoLocationsAllowed_granted_isTrue() {
        val permissions = DefaultMapPermissions { setOf("android.permission.ACCESS_MEDIA_LOCATION") }

        assertTrue(permissions.photoLocationsAllowed())
    }

    @Test
    fun photoLocationsAllowed_otherPermissionOnly_isFalse() {
        val permissions = DefaultMapPermissions { setOf("android.permission.ACCESS_COARSE_LOCATION") }

        assertFalse(permissions.photoLocationsAllowed())
    }

    @Test
    fun currentLocationAllowed_granted_isTrue() {
        val permissions = DefaultMapPermissions { setOf("android.permission.ACCESS_COARSE_LOCATION") }

        assertTrue(permissions.currentLocationAllowed())
    }

    @Test
    fun currentLocationAllowed_otherPermissionOnly_isFalse() {
        val permissions = DefaultMapPermissions { setOf("android.permission.ACCESS_MEDIA_LOCATION") }

        assertFalse(permissions.currentLocationAllowed())
    }
}
