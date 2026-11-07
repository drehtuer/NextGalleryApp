package net.drehtuer.nextgallery.core.nextcloud

import org.junit.Assert.assertTrue
import org.junit.Test

class PropfindRequestBuilderTest {
    @Test
    fun files_asksForEveryPropertyTheAppUses() {
        val body = PropfindRequestBuilder.files()

        listOf(
            "d:resourcetype",
            "d:getcontentlength",
            "d:getcontenttype",
            "d:getetag",
            "d:getlastmodified",
            "oc:fileid",
            "oc:checksums",
            "oc:permissions",
            "nc:has-preview",
            "nc:contained-file-count",
        ).forEach { assertTrue(it, body.contains("<$it/>")) }
    }

    @Test
    fun files_declaresTheNamespaces() {
        val body = PropfindRequestBuilder.files()

        assertTrue(body.contains("""xmlns:d="DAV:""""))
        assertTrue(body.contains("""xmlns:oc="http://owncloud.org/ns""""))
        assertTrue(body.contains("""xmlns:nc="http://nextcloud.org/ns""""))
    }

    @Test
    fun trash_asksForTheOriginalLocation() {
        assertTrue(PropfindRequestBuilder.trash().contains("<nc:trashbin-original-location/>"))
    }
}
