package net.drehtuer.nextgallery.core.nextcloud

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody

/** The bodies of the PROPFIND requests. Only what the app shows or compares is asked for. */
object PropfindRequestBuilder {
    private val XML = "application/xml; charset=utf-8".toMediaType()

    private val FILE_PROPERTIES = listOf(
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
    )

    private val TRASH_PROPERTIES = listOf(
        "d:resourcetype",
        "d:getcontentlength",
        "oc:fileid",
        "nc:trashbin-filename",
        "nc:trashbin-original-location",
        "nc:trashbin-deletion-time",
    )

    fun files(): String = body(FILE_PROPERTIES)

    fun trash(): String = body(TRASH_PROPERTIES)

    internal fun String.asXmlBody(): RequestBody = toRequestBody(XML)

    private fun body(properties: List<String>): String = buildString {
        append("""<?xml version="1.0" encoding="UTF-8"?>""")
        append("""<d:propfind xmlns:d="DAV:" xmlns:oc="http://owncloud.org/ns" xmlns:nc="http://nextcloud.org/ns">""")
        append("<d:prop>")
        properties.forEach { append("<$it/>") }
        append("</d:prop></d:propfind>")
    }
}
