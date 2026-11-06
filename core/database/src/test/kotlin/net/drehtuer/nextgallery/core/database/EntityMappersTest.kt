package net.drehtuer.nextgallery.core.database

import net.drehtuer.nextgallery.core.data.index.SyncState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class EntityMappersTest {
    @Test
    fun mediaEntity_toModelAndBack_isUnchanged() {
        assertEquals(mediaEntity(1), mediaEntity(1).toModel().toEntity())
    }

    @Test
    fun folderRow_toModel_takesFolderFieldsFromCover() {
        val folder = FolderRow(mediaEntity(1), itemCount = 3).toModel()

        assertEquals("Camera", folder.name)
    }

    @Test
    fun mediaEntity_toModel_buildsLocalDate() {
        assertEquals(LocalDate.of(2024, 3, 15), mediaEntity(1).toModel().takenLocalDate)
    }

    @Test
    fun mediaEntity_withoutCoordinates_hasNoLocation() {
        assertNull(mediaEntity(1).copy(latitude = null).toModel().location)
    }

    @Test
    fun syncState_toEntityAndBack_isUnchanged() {
        val state = SyncState("external_primary", 12, "v1", 5_000)

        assertEquals(state, state.toEntity().toModel())
    }
}
