package net.drehtuer.nextgallery.buildlogic

import org.junit.Assert.assertEquals
import org.junit.Test

class NamespacesTest {
    @Test
    fun forProject_appModule_returnsRootNamespace() {
        assertEquals("net.drehtuer.nextgallery", Namespaces.forProject(":app"))
    }

    @Test
    fun forProject_nestedModule_appendsPathSegments() {
        assertEquals("net.drehtuer.nextgallery.core.database", Namespaces.forProject(":core:database"))
    }
}
