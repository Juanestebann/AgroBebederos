package cl.ariztia.bebederos.data.repository

import cl.ariztia.bebederos.data.model.FlushingRecord
import cl.ariztia.bebederos.data.model.MAX_WATER_TEMPERATURE_C
import cl.ariztia.bebederos.data.model.MIN_WATER_TEMPERATURE_C
import cl.ariztia.bebederos.data.model.TemperatureSource
import cl.ariztia.bebederos.data.model.WaterLine

/**
 * Combina el catálogo demo con los flushing persistidos para construir la vista efectiva.
 *
 * Para cada línea se toma únicamente el flushing completado más reciente que pertenezca a la
 * misma jerarquía y tenga una temperatura manual posterior, finita y dentro de 0–50 °C. Si no
 * existe, se conserva intacta la lectura demo. El contexto automático heredado nunca participa.
 */
internal fun effectiveWaterLines(
    demoLines: List<WaterLine>,
    flushings: List<FlushingRecord>
): List<WaterLine> = demoLines.map { demoLine ->
    val latestManualMeasurement = sortFlushingsChronologically(
        flushings.filter { flushing ->
            flushing.completed &&
                flushing.farmId == demoLine.farmId &&
                flushing.shedId == demoLine.shedId &&
                flushing.lineId == demoLine.id &&
                flushing.hasValidManualAfterTemperature() &&
                flushing.parsedTimestamp() != null
        }
    ).firstOrNull()

    if (latestManualMeasurement == null) {
        demoLine
    } else {
        // La búsqueda anterior garantiza que el valor posterior existe y es válido.
        demoLine.copy(
            temperature = latestManualMeasurement.afterTemperatureC!!,
            measurementDate = latestManualMeasurement.date,
            updatedAt = latestManualMeasurement.time,
            temperatureSource = TemperatureSource.MANUAL_AFTER_FLUSHING
        )
    }
}

/** Indica si el registro puede reemplazar de forma segura la lectura efectiva de su línea. */
internal fun FlushingRecord.hasValidManualAfterTemperature(): Boolean =
    afterTemperatureC?.let { value ->
        value.isFinite() && value in MIN_WATER_TEMPERATURE_C..MAX_WATER_TEMPERATURE_C
    } == true
