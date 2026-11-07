package net.drehtuer.nextgallery.core.data.upload

import net.drehtuer.nextgallery.core.data.fakes.mediaItem
import org.junit.Assert.assertEquals
import org.junit.Test

class RemoteTargetResolverTest {
    @Test
    fun remotePathFor_nestedFolder() {
        val item = mediaItem(1).copy(relativePath = "DCIM/Camera/Trip/", displayName = "a.jpg")

        assertEquals("/Photos/DCIM/Camera/Trip/a.jpg", RemoteTargetResolver.remotePathFor(item, "/Photos"))
    }

    @Test
    fun remotePathFor_rootAsRemoteRoot() {
        val item = mediaItem(1).copy(relativePath = "DCIM/", displayName = "a.jpg")

        assertEquals("/DCIM/a.jpg", RemoteTargetResolver.remotePathFor(item, "/"))
    }

    @Test
    fun remotePathFor_trailingSlashesInTheRoot() {
        val item = mediaItem(1).copy(relativePath = "DCIM/", displayName = "a.jpg")

        assertEquals("/Photos/DCIM/a.jpg", RemoteTargetResolver.remotePathFor(item, "/Photos//"))
    }

    @Test
    fun remotePathFor_specialCharactersStayDecoded() {
        val item = mediaItem(1).copy(relativePath = "Bilder & Fotos/", displayName = "Größe #1 100%.jpg")

        assertEquals("/P/Bilder & Fotos/Größe #1 100%.jpg", RemoteTargetResolver.remotePathFor(item, "/P"))
    }

    @Test
    fun remoteFolderFor_dropsTheTrailingSlash() {
        assertEquals("/Photos/DCIM/Camera", RemoteTargetResolver.remoteFolderFor("DCIM/Camera/", "/Photos"))
    }
}
