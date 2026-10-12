package cl.ariztia.bebederos.data.model

import cl.ariztia.bebederos.data.model.CRITICAL_TEMPERATURE_C
import cl.ariztia.bebederos.data.model.TemperatureStatus
import cl.ariztia.bebederos.data.model.WARNING_TEMPERATURE_C
import cl.ariztia.bebederos.data.model.temperatureStatusFor
import org.junit.Test
import kotlin.test.assertEquals

class TemperaturePresentationTest {
    @Test
    fun thermalStatesUseSpanishLabels() {
        assertEquals("Normal", TemperatureStatus.NORMAL.displayLabel)
        assertEquals("Advertencia", TemperatureStatus.WARNING.displayLabel)
        assertEquals("Crítico", TemperatureStatus.CRITICAL.displayLabel)
    }

    @Test
    fun classificationChangesAtExistingThresholds() {
        assertEquals(
            TemperatureStatus.NORMAL,
            temperatureStatusFor(WARNING_TEMPERATURE_C - 0.1)
        )
        assertEquals(
            TemperatureStatus.WARNING,
            temperatureStatusFor(WARNING_TEMPERATURE_C)
        )
        assertEquals(
            TemperatureStatus.WARNING,
            temperatureStatusFor(CRITICAL_TEMPERATURE_C - 0.1)
        )
        assertEquals(
            TemperatureStatus.CRITICAL,
            temperatureStatusFor(CRITICAL_TEMPERATURE_C)
        )
    }

    @Test
    fun sharedThresholdsKeepTheEstablishedGraphLimits() {
        assertEquals(23.0, WARNING_TEMPERATURE_C)
        assertEquals(27.0, CRITICAL_TEMPERATURE_C)
    }
}
