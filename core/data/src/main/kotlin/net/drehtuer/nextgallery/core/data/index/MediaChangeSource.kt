package net.drehtuer.nextgallery.core.data.index

import kotlinx.coroutines.flow.Flow

/** Port: emits whenever MediaStore reports a change to images. Implemented in `:core:media`. */
fun interface MediaChangeSource {
    fun changes(): Flow<Unit>
}
