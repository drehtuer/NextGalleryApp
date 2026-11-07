package net.drehtuer.nextgallery.core.data.upload

import kotlinx.coroutines.withContext
import net.drehtuer.nextgallery.core.common.AppResult
import net.drehtuer.nextgallery.core.common.DispatcherProvider
import net.drehtuer.nextgallery.core.common.runCatchingCancellable
import javax.inject.Inject

class LocalChecksums @Inject constructor(
    private val content: MediaContentSource,
    private val dispatchers: DispatcherProvider,
) {
    /** Null if the image can't be read: it may be gone, or the system may refuse its original bytes. */
    suspend fun digest(contentUri: String, algorithm: ChecksumAlgorithm): String? = withContext(dispatchers.io) {
        val result = runCatchingCancellable {
            content.open(contentUri)?.use { ChecksumCalculator.digest(algorithm, it) }
        }
        (result as? AppResult.Success)?.value
    }
}
