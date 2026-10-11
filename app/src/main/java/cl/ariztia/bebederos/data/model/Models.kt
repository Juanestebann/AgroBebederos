package cl.ariztia.bebederos.data.model

/** Roles disponibles en la sesión demostrativa local. */
enum class UserRole { OPERARIO, SUPERVISOR }

/** Clasificación térmica compartida por modelos, tarjetas, badges y gráficos. */
enum class TemperatureStatus { NORMAL, WARNING, CRITICAL }

/** Distingue una lectura del conjunto demo de una medición manual persistida. */
enum class TemperatureSource { DEMO, MANUAL_AFTER_FLUSHING }

/** Desde este valor una temperatura deja de ser normal. */
const val WARNING_TEMPERATURE_C = 23.0

/** Desde este valor una temperatura pasa a estado crítico. */
const val CRITICAL_TEMPERATURE_C = 27.0

/** Rango aceptado para mediciones manuales de temperatura del agua. */
const val MIN_WATER_TEMPERATURE_C = 0.0
const val MAX_WATER_TEMPERATURE_C = 50.0

/** Etiqueta en español que debe mostrarse en la interfaz en lugar del nombre del enum. */
val TemperatureStatus.displayLabel: String
    get() = when (this) {
        TemperatureStatus.NORMAL -> "Normal"
        TemperatureStatus.WARNING -> "Advertencia"
        TemperatureStatus.CRITICAL -> "Crítico"
    }

/** Usuario local de demostración; no representa una cuenta de un backend. */
data class DemoUser(
    val email: String,
    val password: String,
    val name: String,
    val role: UserRole,
    val farm: String
)

/** Granja del catálogo demostrativo y referencias declaradas a sus galpones. */
data class Farm(
    val id: Int,
    val name: String,
    val shedIds: List<Int>
)

/** Galpón asociado a una granja mediante [farmId]. */
data class Shed(
    val id: Int,
    val farmId: Int,
    val name: String,
    val lineIds: List<Int>
)

/**
 * Línea mostrada en consultas y dashboards.
 *
 * [temperatureSource] permite saber si la temperatura proviene del catálogo demo o de la
 * medición manual posterior del flushing más reciente. [status] siempre usa los umbrales
 * centrales de [temperatureStatusFor].
 */
data class WaterLine(
    val id: Int,
    val farmId: Int,
    val shedId: Int,
    val name: String,
    val temperature: Double,
    val measurementDate: String,
    val updatedAt: String,
    val temperatureSource: TemperatureSource = TemperatureSource.DEMO
) {
    val status: TemperatureStatus
        get() = temperatureStatusFor(temperature)
}

/** Punto histórico demostrativo de temperatura; no se obtiene de sensores en tiempo real. */
data class TemperatureRecord(
    val lineId: Int,
    val date: String,
    val time: String,
    val value: Double
) {
    val status: TemperatureStatus
        get() = temperatureStatusFor(value)
}

/** Alerta demostrativa cuya ubicación se valida antes de ofrecer navegación al detalle. */
data class Alert(
    val id: Int,
    val farmId: Int,
    val shedId: Int,
    val lineId: Int,
    val temperature: Double,
    val date: String,
    val time: String
) {
    val status: TemperatureStatus
        get() = temperatureStatusFor(temperature)
}

/**
 * Representación de dominio de un flushing almacenado localmente.
 *
 * [beforeTemperatureC] y [afterTemperatureC] son mediciones manuales incorporadas en la versión
 * 3 de Room. [temperatureAtRegistrationC] conserva únicamente el contexto automático heredado de
 * la versión 2: no equivale a una medición manual posterior ni actualiza la línea. Los tres campos
 * son anulables para poder leer registros creados por versiones anteriores sin inventar valores.
 * [photoUri] guarda una referencia al archivo local, no los bytes de la fotografía.
 */
data class FlushingRecord(
    val id: Long = 0,
    val farmId: Int,
    val shedId: Int,
    val lineId: Int,
    val operatorName: String,
    val date: String,
    val time: String,
    val reason: String,
    val observation: String,
    val photoUri: String? = null,
    val completed: Boolean = true,
    val temperatureAtRegistrationC: Double? = null,
    val beforeTemperatureC: Double? = null,
    val afterTemperatureC: Double? = null
) {
    val temperatureStatusAtRegistration: TemperatureStatus?
        get() = temperatureAtRegistrationC?.let(::temperatureStatusFor)

    val beforeTemperatureStatus: TemperatureStatus?
        get() = beforeTemperatureC?.let(::temperatureStatusFor)

    val afterTemperatureStatus: TemperatureStatus?
        get() = afterTemperatureC?.let(::temperatureStatusFor)
}

/**
 * Clasifica [value] con los únicos umbrales térmicos de la aplicación.
 *
 * @return normal bajo 23 °C, advertencia desde 23 °C y crítico desde 27 °C.
 */
fun temperatureStatusFor(value: Double): TemperatureStatus = when {
    value >= CRITICAL_TEMPERATURE_C -> TemperatureStatus.CRITICAL
    value >= WARNING_TEMPERATURE_C -> TemperatureStatus.WARNING
    else -> TemperatureStatus.NORMAL
}
