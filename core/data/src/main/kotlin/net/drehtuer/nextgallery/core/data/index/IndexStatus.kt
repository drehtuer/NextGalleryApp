package net.drehtuer.nextgallery.core.data.index

import net.drehtuer.nextgallery.core.common.AppError

sealed interface IndexStatus {
    data object Idle : IndexStatus

    data object Running : IndexStatus

    data class Failed(val error: AppError) : IndexStatus
}
