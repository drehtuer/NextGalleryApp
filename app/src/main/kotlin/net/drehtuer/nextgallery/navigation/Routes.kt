package net.drehtuer.nextgallery.navigation

import kotlinx.serialization.Serializable

@Serializable
data object FoldersRoute

@Serializable
data object TimelineRoute

@Serializable
data object MapRoute

@Serializable
data object NextcloudRoute

/** The settings screens: a graph of their own, because they belong to no tab and hide the bottom navigation. */
@Serializable
data object SettingsGraphRoute
