package net.drehtuer.nextgallery.core.platform

import dagger.Lazy
import net.drehtuer.nextgallery.core.nextcloud.NextcloudHttpConfig
import net.drehtuer.nextgallery.core.nextcloud.NextcloudHttpModule
import org.junit.Assert.assertNotNull
import org.junit.Test

/** Building the HTTP module is slow, so providing a client must not do it; the first request does. */
class NextcloudModuleTest {
    private val notYet = Lazy<NextcloudHttpModule> { error("The HTTP module was built too early") }

    @Test
    fun provideWebDavClient_doesNotBuildTheHttpModule() {
        assertNotNull(NextcloudModule.provideWebDavClient(notYet))
    }

    @Test
    fun provideLoginFlowClient_doesNotBuildTheHttpModule() {
        assertNotNull(
            NextcloudModule.provideLoginFlowClient(notYet, NextcloudHttpConfig("test", allowCleartext = false)),
        )
    }

    @Test
    fun provideOcsClient_doesNotBuildTheHttpModule() {
        assertNotNull(NextcloudModule.provideOcsClient(notYet))
    }

    @Test
    fun provideConnectionEvictor_doesNotBuildTheHttpModule() {
        assertNotNull(NextcloudModule.provideConnectionEvictor(notYet))
    }
}
