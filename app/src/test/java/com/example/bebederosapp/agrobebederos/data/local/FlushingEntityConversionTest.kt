package com.example.bebederosapp.agrobebederos.data.local

import cl.ariztia.bebederos.data.model.FlushingRecord
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FlushingEntityConversionTest {
    @Test
    fun legacyContextAndManualTemperaturesSurviveModelEntityRoundTrip() {
        val record = sampleRecord(
            contextualTemperature = 23.1,
            beforeTemperature = 28.9,
            afterTemperature = 21.5
        )

        assertEquals(record, record.toEntity().toModel())
    }

    @Test
    fun legacyNullTemperatureSurvivesModelEntityRoundTrip() {
        val restored = sampleRecord(
            contextualTemperature = null,
            beforeTemperature = null,
            afterTemperature = null
        ).toEntity().toModel()

        assertNull(restored.temperatureAtRegistrationC)
        assertNull(restored.temperatureStatusAtRegistration)
        assertNull(restored.beforeTemperatureC)
        assertNull(restored.afterTemperatureC)
    }

    private fun sampleRecord(
        contextualTemperature: Double?,
        beforeTemperature: Double?,
        afterTemperature: Double?
    ) = FlushingRecord(
        id = 17,
        farmId = 1,
        shedId = 3,
        lineId = 5,
        operatorName = "Juan Pérez",
        date = "10/10/2026",
        time = "10:30",
        reason = "Otro",
        observation = "Conversión",
        photoUri = "content://prueba/foto.jpg",
        completed = true,
        temperatureAtRegistrationC = contextualTemperature,
        beforeTemperatureC = beforeTemperature,
        afterTemperatureC = afterTemperature
    )
}
