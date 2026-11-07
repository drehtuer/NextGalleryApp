package net.drehtuer.nextgallery.core.model

/** A folder MediaStore reports images in, whether or not the folder rules let it into the index. */
data class DeviceFolder(val name: String, val relativePath: String, val itemCount: Int)
