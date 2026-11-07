package net.drehtuer.nextgallery.core.data.settings

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** A library that ships in the app. [licences] holds SPDX identifiers such as `Apache-2.0`. */
data class LibraryLicence(val id: String, val name: String, val version: String, val licences: List<String>)

/** Reads the licence list that the build generates from the dependencies' POM files (ARCHITECTURE.md §11). */
object LicenceCatalog {
    private const val LIBRARIES = "libraries"
    private const val ID = "uniqueId"
    private const val NAME = "name"
    private const val VERSION = "artifactVersion"
    private const val LICENCES = "licenses"

    /** Sorted by name for reading. Data that can't be read gives an empty list: this is no reason to end the app. */
    fun parse(json: String): List<LibraryLicence> = try {
        Json.parseToJsonElement(json).jsonObject.getValue(LIBRARIES).jsonArray
            .map { it.jsonObject.toLibrary() }
            .sortedWith(compareBy({ it.name.lowercase() }, { it.id }))
    } catch (
        @Suppress("SwallowedException") _: SerializationException,
    ) {
        emptyList()
    } catch (
        @Suppress("SwallowedException") _: IllegalArgumentException,
    ) {
        emptyList()
    } catch (
        @Suppress("SwallowedException") _: NoSuchElementException,
    ) {
        emptyList()
    }

    private fun JsonObject.toLibrary() = LibraryLicence(
        id = getValue(ID).jsonPrimitive.content,
        name = getValue(NAME).jsonPrimitive.content,
        version = getValue(VERSION).jsonPrimitive.content,
        licences = getValue(LICENCES).jsonArray.map { it.jsonPrimitive.content },
    )
}
