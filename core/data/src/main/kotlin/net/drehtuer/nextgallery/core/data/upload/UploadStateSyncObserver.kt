package net.drehtuer.nextgallery.core.data.upload

import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import net.drehtuer.nextgallery.core.data.index.WorkScheduler
import net.drehtuer.nextgallery.core.data.nextcloud.AccountStore
import net.drehtuer.nextgallery.core.data.settings.SettingsRepository
import javax.inject.Inject

/**
 * Schedules the periodic check and asks for one right away after a login or a new remote root: otherwise the badges
 * would stay out of date for up to six hours.
 */
class UploadStateSyncObserver @Inject constructor(
    private val accounts: AccountStore,
    private val settings: SettingsRepository,
    private val workScheduler: WorkScheduler,
) {
    suspend fun observe() {
        workScheduler.scheduleUploadStateSync()
        combine(
            accounts.observeAccount().map { account -> account?.let { it.serverUrl to it.userId } },
            settings.observeSettings().map { it.remoteRoot },
        ) { account, remoteRoot -> account to remoteRoot }
            .distinctUntilChanged()
            .drop(1)
            .filter { (account, _) -> account != null }
            .collect { workScheduler.requestUploadStateSync() }
    }
}
