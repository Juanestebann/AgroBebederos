package cl.ariztia.bebederos.ui.components

import cl.ariztia.bebederos.ui.components.compactDataOriginDescription
import cl.ariztia.bebederos.ui.components.demoAlertTimestamp
import cl.ariztia.bebederos.ui.components.demoMeasurementTimestamp
import cl.ariztia.bebederos.ui.components.demoNotificationText
import cl.ariztia.bebederos.ui.components.localRecordsDescription
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DataOriginPresentationTest {

    @Test
    fun demoDescriptionUsesTheProvidedHistoricalDate() {
        val message = demoDataDescription("23/09/2026")

        assertTrue(message.contains("demostrativas"))
        assertTrue(message.contains("23/09/2026"))
        assertTrue(message.contains("Sin sensores ni sincronización remota"))
    }

    @Test
    fun compactDescriptionSeparatesDemoReadingsFromLocalRecords() {
        assertEquals(
            "Lecturas demo del 23/09/2026 · Flushing y fotografías locales.",
            compactDataOriginDescription("23/09/2026")
        )
    }

    @Test
    fun localRecordsAreNotPresentedAsDemoData() {
        val message = localRecordsDescription()

        assertTrue(message.contains("guardados localmente"))
        assertFalse(message.contains("demo", ignoreCase = true))
        assertFalse(message.contains("demostrativ", ignoreCase = true))
    }

    @Test
    fun demoTimestampsKeepTheirStoredDateAndTime() {
        assertEquals(
            "Lectura demo · 23/09/2026 · 14:29",
            demoMeasurementTimestamp("23/09/2026", "14:29")
        )
        assertEquals(
            "Alerta demo · 23/09/2026 · 13:50",
            demoAlertTimestamp("23/09/2026", "13:50")
        )
    }

    @Test
    fun notificationTextIdentifiesLocalDemoAndStoredTimestamp() {
        val message = demoNotificationText(
            lineName = "Línea 5",
            temperature = 23.1,
            date = "23/09/2026",
            time = "14:29"
        )

        assertTrue(message.startsWith("Aviso local demo"))
        assertTrue(message.contains("23/09/2026 14:29"))
        assertFalse(message.contains("Act."))
    }
}
