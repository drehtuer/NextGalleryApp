package net.drehtuer.nextgallery.core.model

/** What the UI has to do next while images are moved to the system trash or restored. */
sealed interface TrashEffect {
    data class RequestConfirmation(val confirmation: PlatformConfirmation) : TrashEffect

    data class MovedToTrash(val count: Int) : TrashEffect

    data object Failed : TrashEffect
}
