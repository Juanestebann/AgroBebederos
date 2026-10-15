package cl.ariztia.bebederos.viewmodel

import cl.ariztia.bebederos.data.model.UserRole
import cl.ariztia.bebederos.data.repository.DemoRepository
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FlushingCrudRulesTest {
    private val repository = DemoRepository()
    private val operator = repository.users.first { it.role == UserRole.OPERARIO }
    private val supervisor = repository.users.first { it.role == UserRole.SUPERVISOR }
    private val ownRecord = repository.initialFlushings.first {
        it.operatorName == operator.name && it.farmId == 1
    }

    @Test
    fun operatorCanManageOnlyOwnValidRecordInsideAssignedFarm() {
        assertTrue(canManageFlushing(operator, ownRecord, repository))
        assertFalse(
            canManageFlushing(
                operator,
                ownRecord.copy(operatorName = "Otra persona"),
                repository
            )
        )
        assertFalse(
            canManageFlushing(
                operator,
                ownRecord.copy(farmId = 2, shedId = 4, lineId = 7),
                repository
            )
        )
        assertFalse(
            canManageFlushing(
                operator,
                ownRecord.copy(shedId = 1),
                repository
            )
        )
    }

    @Test
    fun supervisorAndClosedSessionCannotManageFlushings() {
        assertFalse(canManageFlushing(supervisor, ownRecord, repository))
        assertFalse(canManageFlushing(null, ownRecord, repository))
    }

    @Test
    fun editableCopyPreservesEveryImmutableField() {
        val updated = ownRecord.withEditableFields(
            reason = "  Limpieza  ",
            observation = "  Actualizada  ",
            photoUri = "content://cl.ariztia.bebederos.fileprovider/flushing_images/nueva.jpg",
            beforeTemperatureC = 27.0,
            afterTemperatureC = 20.5
        )

        assertEquals(ownRecord.id, updated.id)
        assertEquals(ownRecord.farmId, updated.farmId)
        assertEquals(ownRecord.shedId, updated.shedId)
        assertEquals(ownRecord.lineId, updated.lineId)
        assertEquals(ownRecord.operatorName, updated.operatorName)
        assertEquals(ownRecord.date, updated.date)
        assertEquals(ownRecord.time, updated.time)
        assertEquals(ownRecord.completed, updated.completed)
        assertEquals(ownRecord.temperatureAtRegistrationC, updated.temperatureAtRegistrationC)
        assertEquals("Limpieza", updated.reason)
        assertEquals("Actualizada", updated.observation)
        assertEquals(27.0, updated.beforeTemperatureC)
        assertEquals(20.5, updated.afterTemperatureC)
    }

    @Test
    fun editValidationReusesRegistrationRules() {
        val valid = validateFlushingForm("Otro", "", "0", "50")
        assertTrue(valid.isValid)

        val invalid = validateFlushingForm(
            reason = "x".repeat(MAX_FLUSHING_REASON_LENGTH + 1),
            observation = "x".repeat(MAX_FLUSHING_OBSERVATION_LENGTH + 1),
            beforeTemperatureInput = "NaN",
            afterTemperatureInput = "50.1"
        )
        assertFalse(invalid.isValid)
        assertTrue(invalid.reasonError != null)
        assertTrue(invalid.observationError != null)
        assertTrue(invalid.beforeTemperature.error != null)
        assertTrue(invalid.afterTemperature.error != null)
    }

    @Test
    fun onlyPrivateFlushingFileProviderUrisAreOwned() {
        val packageName = "cl.ariztia.bebederos"
        assertTrue(
            isOwnedFlushingPhotoUri(
                "content://cl.ariztia.bebederos.fileprovider/flushing_images/flushing_1.jpg",
                packageName
            )
        )
        assertFalse(isOwnedFlushingPhotoUri("file:///data/private/photo.jpg", packageName))
        assertFalse(
            isOwnedFlushingPhotoUri(
                "content://otra.app.fileprovider/flushing_images/flushing_1.jpg",
                packageName
            )
        )
        assertFalse(
            isOwnedFlushingPhotoUri(
                "content://cl.ariztia.bebederos.fileprovider/camera_images/externa.jpg",
                packageName
            )
        )
        assertFalse(isOwnedFlushingPhotoUri("no es una uri", packageName))
    }

    @Test
    fun demoSeedRunsOnlyForANewUnmarkedEmptyDatabase() {
        assertTrue(
            shouldSeedDemoFlushings(
                seedAlreadyCompleted = false,
                databasePreviouslyExisted = false,
                storedRowCount = 0
            )
        )
        assertFalse(
            shouldSeedDemoFlushings(
                seedAlreadyCompleted = true,
                databasePreviouslyExisted = true,
                storedRowCount = 0
            )
        )
        assertFalse(
            shouldSeedDemoFlushings(
                seedAlreadyCompleted = false,
                databasePreviouslyExisted = true,
                storedRowCount = 0
            )
        )
        assertFalse(
            shouldSeedDemoFlushings(
                seedAlreadyCompleted = false,
                databasePreviouslyExisted = false,
                storedRowCount = 1
            )
        )
    }

    @Test
    fun removingPhotoIsAnEditableChangeWithoutTouchingHistory() {
        val withPhoto = ownRecord.copy(photoUri = "content://foto/original.jpg")
        val updated = withPhoto.withEditableFields(
            reason = withPhoto.reason,
            observation = withPhoto.observation,
            photoUri = null,
            beforeTemperatureC = 28.9,
            afterTemperatureC = 21.5
        )

        assertNull(updated.photoUri)
        assertEquals(withPhoto.id, updated.id)
        assertEquals(withPhoto.date, updated.date)
        assertEquals(withPhoto.time, updated.time)
    }
}
