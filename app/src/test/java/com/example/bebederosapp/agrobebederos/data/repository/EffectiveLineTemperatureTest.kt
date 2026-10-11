package com.example.bebederosapp.agrobebederos.data.repository

import cl.ariztia.bebederos.data.model.FlushingRecord
import cl.ariztia.bebederos.data.model.TemperatureSource
import cl.ariztia.bebederos.data.model.TemperatureStatus
import org.junit.Test
import kotlin.test.assertEquals

class EffectiveLineTemperatureTest {
    private val repository = DemoRepository()

    @Test
    fun latestValidManualAfterMeasurementWinsWithDeterministicTieBreak() {
        val records = listOf(
            flushing(10, "10/10/2026", "10:00", 25.0),
            flushing(11, "10/10/2026", "11:00", 22.0),
            flushing(12, "10/10/2026", "11:00", 21.5)
        )

        val line = effectiveWaterLines(repository.lines, records).first { it.id == 3 }

        assertEquals(21.5, line.temperature)
        assertEquals(TemperatureStatus.NORMAL, line.status)
        assertEquals(TemperatureSource.MANUAL_AFTER_FLUSHING, line.temperatureSource)
        assertEquals("10/10/2026", line.measurementDate)
        assertEquals("11:00", line.updatedAt)
    }

    @Test
    fun measurementsFromOtherHierarchyDoNotAffectLine() {
        val valid = flushing(1, "10/10/2026", "10:00", 21.5)
        val wrongFarm = valid.copy(id = 2, farmId = 2, afterTemperatureC = 40.0)
        val wrongShed = valid.copy(id = 3, shedId = 1, afterTemperatureC = 40.0)
        val wrongLine = valid.copy(id = 4, lineId = 4, afterTemperatureC = 40.0)

        val line = effectiveWaterLines(
            repository.lines,
            listOf(wrongFarm, wrongShed, wrongLine, valid)
        ).first { it.id == 3 }

        assertEquals(21.5, line.temperature)
    }

    @Test
    fun legacyInvalidPendingAndInvalidTimestampRecordsDoNotChangeDemoTemperature() {
        val legacy = flushing(1, "10/10/2026", "10:00", null)
            .copy(temperatureAtRegistrationC = 21.0)
        val nonFinite = flushing(2, "10/10/2026", "11:00", Double.NaN)
        val outsideRange = flushing(3, "10/10/2026", "12:00", 50.1)
        val pending = flushing(4, "10/10/2026", "13:00", 20.0).copy(completed = false)
        val invalidTimestamp = flushing(5, "sin fecha", "sin hora", 19.0)

        val line = effectiveWaterLines(
            repository.lines,
            listOf(legacy, nonFinite, outsideRange, pending, invalidTimestamp)
        ).first { it.id == 3 }

        assertEquals(28.9, line.temperature)
        assertEquals(TemperatureSource.DEMO, line.temperatureSource)
    }

    @Test
    fun effectiveCountsChangeOnlyForTheAffectedLineAndOriginalListIsUntouched() {
        val initialCounts = repository.lines.groupingBy { it.status }.eachCount()
        val updated = effectiveWaterLines(
            repository.lines,
            listOf(flushing(20, "10/10/2026", "14:00", 21.5))
        )
        val updatedCounts = updated.groupingBy { it.status }.eachCount()

        assertEquals(10, initialCounts[TemperatureStatus.NORMAL])
        assertEquals(3, initialCounts[TemperatureStatus.WARNING])
        assertEquals(1, initialCounts[TemperatureStatus.CRITICAL])
        assertEquals(11, updatedCounts[TemperatureStatus.NORMAL])
        assertEquals(3, updatedCounts[TemperatureStatus.WARNING])
        assertEquals(null, updatedCounts[TemperatureStatus.CRITICAL])
        assertEquals(28.9, repository.lines.first { it.id == 3 }.temperature)
        assertEquals(21.5, updated.first { it.id == 3 }.temperature)
        assertEquals(24.3, updated.first { it.id == 2 }.temperature)
    }

    @Test
    fun failedOrUnconfirmedInsertCannotChangeEffectiveTemperature() {
        val roomEmissionAfterFailure = emptyList<FlushingRecord>()

        val line = effectiveWaterLines(
            repository.lines,
            roomEmissionAfterFailure
        ).first { it.id == 3 }

        assertEquals(28.9, line.temperature)
        assertEquals(TemperatureStatus.CRITICAL, line.status)
        assertEquals(TemperatureSource.DEMO, line.temperatureSource)
    }

    private fun flushing(
        id: Long,
        date: String,
        time: String,
        afterTemperature: Double?
    ) = FlushingRecord(
        id = id,
        farmId = 1,
        shedId = 2,
        lineId = 3,
        operatorName = "Prueba",
        date = date,
        time = time,
        reason = "Otro",
        observation = "Efectiva",
        completed = true,
        beforeTemperatureC = 28.9,
        afterTemperatureC = afterTemperature
    )
}
