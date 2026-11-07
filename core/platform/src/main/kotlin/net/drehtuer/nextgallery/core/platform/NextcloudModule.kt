package net.drehtuer.nextgallery.core.platform

import android.content.Context
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.drehtuer.nextgallery.core.data.nextcloud.AccountStore
import net.drehtuer.nextgallery.core.data.nextcloud.ConnectivitySource
import net.drehtuer.nextgallery.core.data.nextcloud.DataStoreTrustedCertificateStore
import net.drehtuer.nextgallery.core.data.nextcloud.DefaultAccountStore
import net.drehtuer.nextgallery.core.data.nextcloud.DefaultNextcloudAccountRepository
import net.drehtuer.nextgallery.core.data.nextcloud.DefaultRemoteBrowseRepository
import net.drehtuer.nextgallery.core.data.nextcloud.DefaultRemoteDownloads
import net.drehtuer.nextgallery.core.data.nextcloud.DefaultRemoteFileOperations
import net.drehtuer.nextgallery.core.data.nextcloud.DefaultRemoteShareRepository
import net.drehtuer.nextgallery.core.data.nextcloud.DefaultTrustedCertificateRepository
import net.drehtuer.nextgallery.core.data.nextcloud.LocalNetworkPermissionSource
import net.drehtuer.nextgallery.core.data.nextcloud.NextcloudAccountRepository
import net.drehtuer.nextgallery.core.data.nextcloud.RemoteBrowseRepository
import net.drehtuer.nextgallery.core.data.nextcloud.RemoteCacheStore
import net.drehtuer.nextgallery.core.data.nextcloud.RemoteDownloads
import net.drehtuer.nextgallery.core.data.nextcloud.RemoteFileOperations
import net.drehtuer.nextgallery.core.data.nextcloud.RemoteShareCache
import net.drehtuer.nextgallery.core.data.nextcloud.RemoteShareRepository
import net.drehtuer.nextgallery.core.data.nextcloud.SecretCipher
import net.drehtuer.nextgallery.core.data.nextcloud.StoredCertificatePins
import net.drehtuer.nextgallery.core.data.nextcloud.StoredCredentialsProvider
import net.drehtuer.nextgallery.core.data.nextcloud.TrustedCertificateRepository
import net.drehtuer.nextgallery.core.data.nextcloud.TrustedCertificateStore
import net.drehtuer.nextgallery.core.database.RoomRemoteCacheStore
import net.drehtuer.nextgallery.core.nextcloud.CertificatePinSource
import net.drehtuer.nextgallery.core.nextcloud.ConnectionEvictor
import net.drehtuer.nextgallery.core.nextcloud.CredentialsProvider
import net.drehtuer.nextgallery.core.nextcloud.DownloadProgressTracker
import net.drehtuer.nextgallery.core.nextcloud.LoginFlowV2Client
import net.drehtuer.nextgallery.core.nextcloud.NextcloudHttpConfig
import net.drehtuer.nextgallery.core.nextcloud.NextcloudHttpModule
import net.drehtuer.nextgallery.core.nextcloud.OcsClient
import net.drehtuer.nextgallery.core.nextcloud.ServerUrlNormalizer
import net.drehtuer.nextgallery.core.nextcloud.WebDavClient
import java.io.File
import javax.inject.Singleton

/** The Nextcloud client and repositories. `:app` provides [NextcloudHttpConfig], because it knows the build type. */
@Module
@InstallIn(SingletonComponent::class)
abstract class NextcloudModule {
    @Binds
    abstract fun bindSecretCipher(cipher: KeystoreSecretCipher): SecretCipher

    @Binds
    abstract fun bindConnectivity(source: AndroidConnectivitySource): ConnectivitySource

    @Binds
    abstract fun bindLocalNetwork(source: AndroidLocalNetworkPermissionSource): LocalNetworkPermissionSource

    @Binds
    abstract fun bindShareCache(cache: FileProviderShareCache): RemoteShareCache

    @Binds
    abstract fun bindRemoteCacheStore(store: RoomRemoteCacheStore): RemoteCacheStore

    @Binds
    abstract fun bindCredentials(provider: StoredCredentialsProvider): CredentialsProvider

    @Binds
    abstract fun bindPins(pins: StoredCertificatePins): CertificatePinSource

    companion object {
        /** One instance per file: DataStore does not allow two in a process. */
        @Provides
        @Singleton
        fun provideAccountStore(@ApplicationContext context: Context, cipher: SecretCipher): AccountStore =
            DefaultAccountStore.create(File(context.filesDir, "datastore/credentials.preferences_pb"), cipher)

        @Provides
        @Singleton
        fun provideCertificateStore(@ApplicationContext context: Context): TrustedCertificateStore =
            DataStoreTrustedCertificateStore.create(File(context.filesDir, "datastore/trusted_certs.preferences_pb"))

        @Provides
        @Singleton
        fun provideHttpModule(
            config: NextcloudHttpConfig,
            credentials: CredentialsProvider,
            pins: CertificatePinSource,
        ): NextcloudHttpModule = NextcloudHttpModule(config, credentials, pins)

        @Provides
        fun provideConnectionEvictor(module: NextcloudHttpModule): ConnectionEvictor = module

        @Provides
        fun provideDownloadTracker(module: NextcloudHttpModule): DownloadProgressTracker = module.downloadProgress

        @Provides
        fun provideWebDavClient(module: NextcloudHttpModule) = WebDavClient(module.authenticated, module.noRedirects)

        @Provides
        fun provideLoginFlowClient(module: NextcloudHttpModule, config: NextcloudHttpConfig) =
            LoginFlowV2Client(module.base, config.allowCleartext)

        @Provides
        fun provideOcsClient(module: NextcloudHttpModule) = OcsClient(module.unauthenticatedNoRedirects)

        @Provides
        fun provideNormalizer(config: NextcloudHttpConfig) = ServerUrlNormalizer(config.allowCleartext)
    }
}

/** Binds the Nextcloud repositories to their default implementations. */
@Module
@InstallIn(SingletonComponent::class)
interface NextcloudRepositoryModule {
    @Binds
    fun bindAccountRepository(repository: DefaultNextcloudAccountRepository): NextcloudAccountRepository

    @Binds
    fun bindBrowseRepository(repository: DefaultRemoteBrowseRepository): RemoteBrowseRepository

    @Binds
    fun bindFileOperations(operations: DefaultRemoteFileOperations): RemoteFileOperations

    @Binds
    fun bindShareRepository(repository: DefaultRemoteShareRepository): RemoteShareRepository

    @Binds
    fun bindDownloads(downloads: DefaultRemoteDownloads): RemoteDownloads

    @Binds
    fun bindCertificates(repository: DefaultTrustedCertificateRepository): TrustedCertificateRepository
}
