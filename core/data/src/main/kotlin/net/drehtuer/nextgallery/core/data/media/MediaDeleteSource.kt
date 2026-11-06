package net.drehtuer.nextgallery.core.data.media

import net.drehtuer.nextgallery.core.model.PlatformConfirmation

/** Port: system requests that change images the app does not own. Implemented in `:core:media`. */
interface MediaDeleteSource {
    suspend fun trashRequest(contentUris: List<String>): PlatformConfirmation

    suspend fun restoreRequest(contentUris: List<String>): PlatformConfirmation

    suspend fun deleteRequest(contentUris: List<String>): PlatformConfirmation
}
