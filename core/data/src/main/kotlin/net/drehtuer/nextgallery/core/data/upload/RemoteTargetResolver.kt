package net.drehtuer.nextgallery.core.data.upload

import net.drehtuer.nextgallery.core.model.MediaItem
import net.drehtuer.nextgallery.core.model.RemotePaths

/** Where a local image is expected on the server: `<remoteRoot>/<local relative path>/<displayName>` (D13). */
object RemoteTargetResolver {
    fun remoteFolderFor(relativePath: String, remoteRoot: String): String = RemotePaths.join(remoteRoot, relativePath)

    fun remotePathFor(item: MediaItem, remoteRoot: String): String =
        RemotePaths.join(remoteFolderFor(item.relativePath, remoteRoot), item.displayName)
}
