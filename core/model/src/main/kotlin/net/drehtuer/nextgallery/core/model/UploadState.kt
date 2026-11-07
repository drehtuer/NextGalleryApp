package net.drehtuer.nextgallery.core.model

/** Whether a local image is on the server, as far as the last check knows (D13). */
sealed interface UploadState {
    /** Not checked yet, or the image changed since the last check. */
    data object Unknown : UploadState

    data object NotUploaded : UploadState

    /** The same name and size are on the server; [verifiedByChecksum] if the content was compared as well. */
    data class Uploaded(val verifiedByChecksum: Boolean) : UploadState
}
