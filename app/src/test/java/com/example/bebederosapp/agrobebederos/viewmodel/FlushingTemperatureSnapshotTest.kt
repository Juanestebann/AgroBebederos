package com.example.bebederosapp.agrobebederos.viewmodel

import cl.ariztia.bebederos.data.model.FlushingRecord
import cl.ariztia.bebederos.data.model.TemperatureStatus
import cl.ariztia.bebederos.data.model.UserRole
import cl.ariztia.bebederos.data.repository.DemoRepository
import cl.ariztia.bebederos.viewmodel.FlushingTimestamp
import cl.ariztia.bebederos.viewmodel.SelectionState
import cl.ariztia.bebederos.viewmodel.createFlushingRecord
import cl.ariztia.bebederos.viewmodel.validateWaterTemperatureInput
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FlushingTemperatureSnapshotTest {
    private val repository = DemoRepository()

    @Test
    fun requiredInputsAcceptCommaAndPointDecimals() {
        val comma = validateWaterTemperatureInput("28,9", "antes")
        val point = validateWaterTemperatureInput("21.5", "después")

        assertEquals(28.9, comma.value)
        assertEquals(21.5, point.value)
        assertTrue(comma.isValid)
        assertTrue(point.isValid)
    }

    @Test
    fun blankNonNumericAndNonFiniteInputsAreRejected() {
        listOf("", "   ", "texto", "NaN", "Infinity", "-Infinity").forEach { input ->
            val result = validateWaterTemperatureInput(input, "antes")

            assertFalse(result.isValid, input)
            assertNotNull(result.error, input)
        }
    }

    @Test
    fun waterTemperatureRangeIncludesLimitsAndRejectsOutsideValues() {
        assertTrue(validateWaterTemperatureInput("0", "antes").isValid)
        assertTrue(validateWaterTemperatureInput("50", "después").isValid)
        assertFalse(validateWaterTemperatureInput("-0.1", "antes").isValid)
        assertFalse(validateWaterTemperatureInput("50.1", "después").isValid)
    }

    @Test
    fun newRecordStoresExactManualMeasurementsWithoutReusingLegacyContext() {
        val selection = SelectionState(repository).apply {
            startSession(repository.users.first { it.role == UserRole.OPERARIO })
            selectLine(3)
        }

        val record = createFlushingRecord(
            location = assertNotNull(selection.validatedCurrent()),
            operatorName = "Juan Pérez",
            reason = "Temperatura alta",
            observation = "Medición manual",
            photoUri = null,
            timestamp = FlushingTimestamp("10/10/2026", "10:30"),
            beforeTemperatureC = 28.9,
            afterTemperatureC = 21.5
        )

        assertEquals(28.9, record.beforeTemperatureC)
        assertEquals(21.5, record.afterTemperatureC)
        assertEquals(TemperatureStatus.CRITICAL, record.beforeTemperatureStatus)
        assertEquals(TemperatureStatus.NORMAL, record.afterTemperatureStatus)
        assertNull(record.temperatureAtRegistrationC)
    }

    @Test
    fun afterTemperatureMayBeLowerEqualOrHigherThanBefore() {
        listOf(20.0, 25.0, 30.0).forEach { after ->
            val before = validateWaterTemperatureInput("25", "antes")
            val afterResult = validateWaterTemperatureInput(after.toString(), "después")

            assertTrue(before.isValid)
            assertTrue(afterResult.isValid)
        }
    }

    @Test
    fun historicalRecordsKeepLegacyContextAndMissingManualMeasurementsDistinct() {
        val contextual = historicalRecord(temperatureAtRegistrationC = 24.3)
        val migratedV1 = historicalRecord(temperatureAtRegistrationC = null)

        assertEquals(TemperatureStatus.WARNING, contextual.temperatureStatusAtRegistration)
        assertNull(contextual.beforeTemperatureC)
        assertNull(contextual.afterTemperatureC)
        assertNull(migratedV1.temperatureAtRegistrationC)
        assertNull(migratedV1.beforeTemperatureStatus)
        assertNull(migratedV1.afterTemperatureStatus)
    }

    private fun historicalRecord(temperatureAtRegistrationC: Double?) = FlushingRecord(
        farmId = 1,
        shedId = 1,
        lineId = 1,
        operatorName = "Operario histórico",
        date = "23/09/2026",
        time = "14:35",
        reason = "Limpieza",
        observation = "Registro previo",
        temperatureAtRegistrationC = temperatureAtRegistrationC
    )
}
