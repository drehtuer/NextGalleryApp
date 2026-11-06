package net.drehtuer.nextgallery.navigation

import android.content.ContentResolver
import android.content.Intent

/** The image another app asked to open, or null if the app was started normally. */
fun Intent.externalImageUri(): String? = data
    ?.takeIf { action == Intent.ACTION_VIEW && it.scheme == ContentResolver.SCHEME_CONTENT }
    ?.takeIf { type?.startsWith("image/") == true }
    ?.toString()
