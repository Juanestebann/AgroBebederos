package com.example.bebederosapp.agrobebederos.viewmodel

import cl.ariztia.bebederos.data.local.toEntity
import cl.ariztia.bebederos.data.model.UserRole
import cl.ariztia.bebederos.data.repository.DemoRepository
import cl.ariztia.bebederos.viewmodel.FlushingTimeProvider
import cl.ariztia.bebederos.viewmodel.FlushingTimestamp
import cl.ariztia.bebederos.viewmodel.SelectionState
import cl.ariztia.bebederos.viewmodel.createFlushingRecord
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class FlushingTimeProviderTest {
    @Test
    fun fixedClockGeneratesExpectedDate() {
        val provider = providerAt("2026-10-07T18:42:30Z", "America/Santiago")

        assertEquals("07/10/2026", provider.now().date)
    }

    @Test
    fun fixedClockGeneratesExpectedTimeFromTheSameInstant() {
        val provider = providerAt("2026-10-07T18:42:30Z", "America/Santiago")

        assertEquals("15:42", provider.now().time)
    }

    @Test
    fun timestampUsesRequiredFormats() {
        val timestamp = providerAt("2026-01-02T03:04:00Z", "UTC").now()

        assertTrue(timestamp.date.matches(Regex("\\d{2}/\\d{2}/\\d{4}")))
        assertTrue(timestamp.time.matches(Regex("\\d{2}:\\d{2}")))
        assertEquals("02/01/2026", timestamp.date)
        assertEquals("03:04", timestamp.time)
    }

    @Test
    fun generatedTimestampIsPreservedInEntitySentToRoom() {
        val repository = DemoRepository()
        val selection = SelectionState(repository).apply {
            startSession(repository.users.first { it.role == UserRole.OPERARIO })
            selectLine(5)
        }
        val timestamp = providerAt("2026-10-07T18:42:30Z", "America/Santiago").now()

        val entity = createFlushingRecord(
            location = assertNotNull(selection.validatedCurrent()),
            operatorName = "Juan Pérez",
            reason = "Temperatura alta",
            observation = "Verificación",
            photoUri = null,
            timestamp = timestamp,
            beforeTemperatureC = 28.9,
            afterTemperatureC = 21.5
        ).toEntity()

        assertEquals(timestamp.date, entity.date)
        assertEquals(timestamp.time, entity.time)
    }

    @Test
    fun timestampChangesDateAcrossMidnight() {
        val beforeMidnight = providerAt("2026-10-07T23:59:00Z", "UTC").now()
        val afterMidnight = providerAt("2026-10-08T00:01:00Z", "UTC").now()

        assertEquals("07/10/2026", beforeMidnight.date)
        assertEquals("23:59", beforeMidnight.time)
        assertEquals("08/10/2026", afterMidnight.date)
        assertEquals("00:01", afterMidnight.time)
    }

    @Test
    fun dateAndTimeConsistentlyUseTheClockZone() {
        val instant = Instant.parse("2026-10-08T02:30:00Z")
        val utc = FlushingTimeProvider(Clock.fixed(instant, ZoneOffset.UTC)).now()
        val santiago = FlushingTimeProvider(
            Clock.fixed(instant, ZoneOffset.ofHours(-3))
        ).now()

        assertEquals(FlushingTimestamp("08/10/2026", "02:30"), utc)
        assertEquals(FlushingTimestamp("07/10/2026", "23:30"), santiago)
    }

    private fun providerAt(instant: String, zoneId: String): FlushingTimeProvider =
        FlushingTimeProvider(Clock.fixed(Instant.parse(instant), ZoneId.of(zoneId)))
}
