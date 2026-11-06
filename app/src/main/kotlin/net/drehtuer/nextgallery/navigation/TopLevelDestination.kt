package net.drehtuer.nextgallery.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import net.drehtuer.nextgallery.R

/** Destinations of the bottom navigation bar. */
enum class TopLevelDestination(val route: Any, @param:StringRes val label: Int, @param:DrawableRes val icon: Int) {
    FOLDERS(FoldersRoute, R.string.destination_folders, R.drawable.ic_destination_folders),
    TIMELINE(TimelineRoute, R.string.destination_timeline, R.drawable.ic_destination_timeline),
    MAP(MapRoute, R.string.destination_map, R.drawable.ic_destination_map),
    NEXTCLOUD(NextcloudRoute, R.string.destination_nextcloud, R.drawable.ic_destination_nextcloud),
}
