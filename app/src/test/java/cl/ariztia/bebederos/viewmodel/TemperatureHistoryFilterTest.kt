package cl.ariztia.bebederos.viewmodel

import cl.ariztia.bebederos.data.model.TemperatureRecord
import cl.ariztia.bebederos.data.repository.DemoRepository
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TemperatureHistoryFilterTest {
    private val repository = DemoRepository()

    @Test
    fun line3KeepsItsOriginalHistory() {
        val records = repository.historyForLine(3)

        assertEquals(9, records.size)
        assertTrue(records.all { it.lineId == 3 })
        assertEquals("08:00", records.first().time)
        assertEquals("16:00", records.last().time)
    }

    @Test
    fun lineWithoutMeasurementsHasEmptyHistory() {
        assertTrue(repository.historyForLine(5).isEmpty())
    }

    @Test
    fun historyIsNotReusedBetweenLines() {
        val line3 = repository.historyForLine(3)
        val line5 = repository.historyForLine(5)

        assertTrue(line3.isNotEmpty())
        assertTrue(line5.isEmpty())
    }

    @Test
    fun last24HoursIncludesOnlyRecordsInsideTheInterval() {
        val filter = filterAt("2026-09-23T16:00:00Z")
        val records = listOf(
            record("22/09/2026", "15:59", 1.0),
            record("22/09/2026", "16:00", 2.0),
            record("23/09/2026", "15:00", 3.0),
            record("23/09/2026", "16:00", 4.0),
            record("23/09/2026", "16:01", 5.0)
        )

        val result = filter.filter(records, TemperatureHistoryPeriod.LAST_24_HOURS)

        assertEquals(listOf(2.0, 3.0, 4.0), result.map { it.value })
    }

    @Test
    fun last7DaysIncludesOnlyRecordsInsideTheInterval() {
        val filter = filterAt("2026-09-23T16:00:00Z")
        val records = listOf(
            record("16/09/2026", "15:59", 1.0),
            record("16/09/2026", "16:00", 2.0),
            record("20/09/2026", "12:00", 3.0),
            record("23/09/2026", "16:00", 4.0),
            record("23/09/2026", "16:01", 5.0)
        )

        val result = filter.filter(records, TemperatureHistoryPeriod.LAST_7_DAYS)

        assertEquals(listOf(2.0, 3.0, 4.0), result.map { it.value })
    }

    @Test
    fun futureMeasurementsAreExcluded() {
        val filter = filterAt("2026-09-23T16:00:00Z")

        val result = filter.filter(
            listOf(record("23/09/2026", "16:01")),
            TemperatureHistoryPeriod.LAST_7_DAYS
        )

        assertTrue(result.isEmpty())
    }

    @Test
    fun measurementsBeforeTheRequestedIntervalAreExcluded() {
        val filter = filterAt("2026-09-23T16:00:00Z")

        val result = filter.filter(
            listOf(record("22/09/2026", "15:59")),
            TemperatureHistoryPeriod.LAST_24_HOURS
        )

        assertTrue(result.isEmpty())
    }

    @Test
    fun intervalBoundariesUseTheClockZoneAcrossDayChanges() {
        val instant = Instant.parse("2026-10-08T02:30:00Z")
        val filter = TemperatureHistoryFilter(
            Clock.fixed(instant, ZoneOffset.ofHours(-3))
        )
        val records = listOf(
            record("06/10/2026", "23:29", 1.0),
            record("06/10/2026", "23:30", 2.0),
            record("07/10/2026", "23:30", 3.0),
            record("08/10/2026", "00:00", 4.0)
        )

        val result = filter.filter(records, TemperatureHistoryPeriod.LAST_24_HOURS)

        assertEquals(listOf(2.0, 3.0), result.map { it.value })
    }

    @Test
    fun oldDemoMeasurementsProduceAnEmptyFilteredState() {
        val filter = filterAt("2026-10-08T00:30:00Z")

        val result = filter.filter(
            repository.historyForLine(3),
            TemperatureHistoryPeriod.LAST_7_DAYS
        )

        assertTrue(result.isEmpty())
    }

    private fun filterAt(
        instant: String,
        zoneId: String = "UTC"
    ): TemperatureHistoryFilter = TemperatureHistoryFilter(
        Clock.fixed(Instant.parse(instant), ZoneId.of(zoneId))
    )

    private fun record(
        date: String,
        time: String,
        value: Double = 20.0
    ) = TemperatureRecord(3, date, time, value)
}
