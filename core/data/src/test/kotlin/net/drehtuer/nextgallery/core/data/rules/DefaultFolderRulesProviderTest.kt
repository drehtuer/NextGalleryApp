package net.drehtuer.nextgallery.core.data.rules

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class DefaultFolderRulesProviderTest {
    @Test
    fun current_returnsDefaultRules() = runTest {
        assertEquals(DefaultFolderRulesProvider.DEFAULT_RULES, DefaultFolderRulesProvider().current())
    }
}
