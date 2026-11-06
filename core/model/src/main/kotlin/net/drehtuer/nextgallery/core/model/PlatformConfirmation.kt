package net.drehtuer.nextgallery.core.model

/**
 * Opaque handle for a system confirmation dialog. The Android adapter wraps an `IntentSender` in it and
 * `:core:ui` launches it; everything in between only passes it on (D25).
 */
class PlatformConfirmation(val handle: Any)
