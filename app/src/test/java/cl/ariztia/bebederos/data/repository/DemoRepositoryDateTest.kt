package cl.ariztia.bebederos.data.repository

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DemoRepositoryDateTest {
    private val repository = DemoRepository()

    @Test
    fun everyLineKeepsTheExplicitDemoMeasurementDate() {
        assertTrue(repository.lines.isNotEmpty())
        assertTrue(repository.lines.all { it.measurementDate == DEMO_MEASUREMENT_DATE })
        assertEquals("23/09/2026", DEMO_MEASUREMENT_DATE)
    }

    @Test
    fun lineHistoryUsesTheSameMeasurementDateSource() {
        val line = requireNotNull(repository.line(3))
        val history = repository.historyForLine(line.id)

        assertTrue(history.isNotEmpty())
        assertTrue(history.all { it.date == line.measurementDate })
    }
}
