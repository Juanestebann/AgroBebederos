package cl.ariztia.bebederos.ui.components

import cl.ariztia.bebederos.data.model.TemperatureSource
import cl.ariztia.bebederos.data.model.WaterLine

/** Describe datos demo sin prometer sensores, actualidad ni sincronización remota. */
fun demoDataDescription(measurementDate: String?): String {
    val dateSuffix = measurementDate
        ?.takeIf { it.isNotBlank() }
        ?.let { " del $it" }
        .orEmpty()

    return "Temperaturas, alertas y ubicaciones demostrativas$dateSuffix. " +
        "Sin sensores ni sincronización remota."
}

/** Describe exclusivamente los registros y archivos persistidos en el dispositivo. */
fun localRecordsDescription(): String =
    "Flushing y fotografías guardados localmente en este dispositivo."

fun combinedDataOriginDescription(measurementDate: String?): String =
    "${demoDataDescription(measurementDate)} ${localRecordsDescription()}"

fun compactDataOriginDescription(measurementDate: String?): String {
    val datedLabel = measurementDate
        ?.takeIf { it.isNotBlank() }
        ?.let { "Lecturas demo del $it" }
        ?: "Lecturas demo"

    return "$datedLabel · Flushing y fotografías locales."
}

fun demoMeasurementTimestamp(date: String, time: String): String =
    "Lectura demo · $date · $time"

/** Etiqueta una línea según el origen real de su temperatura efectiva. */
fun effectiveMeasurementTimestamp(line: WaterLine): String =
    if (line.temperatureSource == TemperatureSource.MANUAL_AFTER_FLUSHING) {
        "Medición manual posterior · ${line.measurementDate} · ${line.updatedAt}"
    } else {
        demoMeasurementTimestamp(line.measurementDate, line.updatedAt)
    }

fun demoAlertTimestamp(date: String, time: String): String =
    "Alerta demo · $date · $time"

fun demoNotificationText(
    lineName: String,
    temperature: Double,
    date: String,
    time: String
): String = "Aviso local demo · $lineName: $temperature °C · $date $time."
