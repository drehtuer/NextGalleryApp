package net.drehtuer.nextgallery

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import dagger.Lazy
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import net.drehtuer.nextgallery.core.data.nextcloud.RemoteShareCache
import net.drehtuer.nextgallery.core.data.settings.RuleChangeObserver
import net.drehtuer.nextgallery.core.data.upload.UploadStateSyncObserver
import javax.inject.Inject

@HiltAndroidApp
class NextGalleryApplication :
    Application(),
    Configuration.Provider,
    SingletonImageLoader.Factory {
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var imageLoader: ImageLoader

    /** Lazy: it needs WorkManager, which needs [workerFactory], so it must not be built during injection. */
    @Inject
    lateinit var ruleChangeObserver: Lazy<RuleChangeObserver>

    @Inject
    lateinit var remoteShareCache: Lazy<RemoteShareCache>

    @Inject
    lateinit var uploadStateSyncObserver: Lazy<UploadStateSyncObserver>

    /** Not tied to an activity, so a rule change still reaches the index if the user leaves the app right after it. */
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch { ruleChangeObserver.get().observe() }
        applicationScope.launch { uploadStateSyncObserver.get().observe() }
        // Shared server images only live until the next share or app start (D48).
        applicationScope.launch(Dispatchers.IO) { remoteShareCache.get().clear() }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun newImageLoader(context: PlatformContext): ImageLoader = imageLoader
}
