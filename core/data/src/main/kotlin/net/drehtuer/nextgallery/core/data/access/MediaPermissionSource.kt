package net.drehtuer.nextgallery.core.data.access

/** Port: currently granted Android permissions (as permission name strings). Implemented in `:core:media`. */
fun interface MediaPermissionSource {
    fun grantedPermissions(): Set<String>
}
