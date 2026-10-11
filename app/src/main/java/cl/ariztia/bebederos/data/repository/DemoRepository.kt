package cl.ariztia.bebederos.data.repository

import cl.ariztia.bebederos.data.model.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.ResolverStyle

/** Fecha histórica explícita del conjunto de lecturas demostrativas. */
const val DEMO_MEASUREMENT_DATE = "23/09/2026"

/** Resultado de validar conjuntamente una granja, un galpón y una línea. */
data class ResolvedLocation(
    val farm: Farm,
    val shed: Shed,
    val line: WaterLine
)

/**
 * Catálogo local de usuarios, ubicaciones, temperaturas y alertas demostrativas.
 *
 * Esta clase no consulta sensores, API ni backend. Los flushing iniciales solo sirven para poblar
 * una base vacía; después, Room es la fuente persistente de los registros de intervención.
 */
class DemoRepository {
    val users = listOf(
        DemoUser("operario@ariztia.cl", "Demo123", "Juan Pérez", UserRole.OPERARIO, "Granja El Sauce"),
        DemoUser("supervisor@ariztia.cl", "Demo123", "Carlos Morales", UserRole.SUPERVISOR, "Granja El Sauce")
    )

    val farms = listOf(
        Farm(1, "Granja El Sauce", listOf(1, 2, 3)),
        Farm(2, "Granja Los Maitenes", listOf(4, 5, 6, 7)),
        Farm(3, "Granja Santa Rosa", listOf(8, 9))
    )

    val sheds = listOf(
        Shed(1, 1, "Galpón A", listOf(1, 2)),
        Shed(2, 1, "Galpón B", listOf(3, 4)),
        Shed(3, 1, "Galpón C", listOf(5)),
        Shed(4, 2, "Galpón A", listOf(6, 7)),
        Shed(5, 2, "Galpón B", listOf(8, 9)),
        Shed(6, 2, "Galpón C", listOf(10, 11)),
        Shed(7, 2, "Galpón D", listOf(12)),
        Shed(8, 3, "Galpón A", listOf(13)),
        Shed(9, 3, "Galpón B", listOf(14))
    )

    val lines = listOf(
        WaterLine(1, 1, 1, "Línea 1", 19.8, DEMO_MEASUREMENT_DATE, "14:32"),
        WaterLine(2, 1, 1, "Línea 2", 24.3, DEMO_MEASUREMENT_DATE, "14:30"),
        WaterLine(3, 1, 2, "Línea 3", 28.9, DEMO_MEASUREMENT_DATE, "14:28"),
        WaterLine(4, 1, 2, "Línea 4", 20.5, DEMO_MEASUREMENT_DATE, "14:31"),
        WaterLine(5, 1, 3, "Línea 5", 23.1, DEMO_MEASUREMENT_DATE, "14:29"),
        WaterLine(6, 2, 4, "Línea 1", 21.2, DEMO_MEASUREMENT_DATE, "14:25"),
        WaterLine(7, 2, 4, "Línea 2", 22.4, DEMO_MEASUREMENT_DATE, "14:22"),
        WaterLine(8, 2, 5, "Línea 3", 20.8, DEMO_MEASUREMENT_DATE, "14:19"),
        WaterLine(9, 2, 5, "Línea 4", 24.1, DEMO_MEASUREMENT_DATE, "14:18"),
        WaterLine(10, 2, 6, "Línea 5", 21.0, DEMO_MEASUREMENT_DATE, "14:17"),
        WaterLine(11, 2, 6, "Línea 6", 20.2, DEMO_MEASUREMENT_DATE, "14:16"),
        WaterLine(12, 2, 7, "Línea 7", 21.4, DEMO_MEASUREMENT_DATE, "14:15"),
        WaterLine(13, 3, 8, "Línea 1", 20.7, DEMO_MEASUREMENT_DATE, "14:14"),
        WaterLine(14, 3, 9, "Línea 2", 21.1, DEMO_MEASUREMENT_DATE, "14:13")
    )

    val alerts = listOf(
        Alert(1, 1, 2, 3, 28.9, DEMO_MEASUREMENT_DATE, "14:28"),
        Alert(2, 1, 1, 2, 24.3, DEMO_MEASUREMENT_DATE, "14:15"),
        Alert(3, 1, 3, 5, 23.1, DEMO_MEASUREMENT_DATE, "13:50")
    )

    val history = listOf(
        TemperatureRecord(3, DEMO_MEASUREMENT_DATE, "08:00", 19.5),
        TemperatureRecord(3, DEMO_MEASUREMENT_DATE, "09:00", 20.1),
        TemperatureRecord(3, DEMO_MEASUREMENT_DATE, "10:00", 21.8),
        TemperatureRecord(3, DEMO_MEASUREMENT_DATE, "11:00", 23.4),
        TemperatureRecord(3, DEMO_MEASUREMENT_DATE, "12:00", 25.2),
        TemperatureRecord(3, DEMO_MEASUREMENT_DATE, "13:00", 27.0),
        TemperatureRecord(3, DEMO_MEASUREMENT_DATE, "14:00", 28.9),
        TemperatureRecord(3, DEMO_MEASUREMENT_DATE, "15:00", 27.5),
        TemperatureRecord(3, DEMO_MEASUREMENT_DATE, "16:00", 25.8)
    )

    val initialFlushings = listOf(
        FlushingRecord(1, 1, 2, 3, "Juan Pérez", "23/09/2026", "14:35", "Temperatura alta", "Flushing realizado", completed = true),
        FlushingRecord(2, 2, 4, 7, "María López", "23/09/2026", "13:10", "Mantenimiento preventivo", "Sin novedades", completed = true),
        FlushingRecord(3, 1, 3, 5, "Carlos Rivas", "22/09/2026", "17:45", "Limpieza", "Pendiente de revisión", completed = false)
    )

    /** Valida credenciales contra los usuarios demo en memoria. */
    fun authenticate(email: String, password: String): DemoUser? =
        users.find { it.email.equals(email.trim(), ignoreCase = true) && it.password == password }

    fun farm(id: Int) = farms.firstOrNull { it.id == id }
    fun shed(id: Int) = sheds.firstOrNull { it.id == id }
    fun line(id: Int) = lines.firstOrNull { it.id == id }

    /** Deriva los galpones desde la relación [Shed.farmId], no desde el conteo declarado. */
    fun shedsForFarm(farmId: Int): List<Shed> {
        if (farm(farmId) == null) return emptyList()
        return sheds.filter { it.farmId == farmId }
    }

    fun shedCountForFarm(farm: Farm): Int = shedsForFarm(farm.id).size

    /** Devuelve solo líneas que coinciden simultáneamente con el galpón y su granja. */
    fun linesForShed(shedId: Int): List<WaterLine> {
        val shed = shed(shedId) ?: return emptyList()
        if (farm(shed.farmId) == null) return emptyList()
        return lines.filter { line ->
            line.shedId == shed.id && line.farmId == shed.farmId
        }
    }

    fun linesForFarm(farmId: Int): List<WaterLine> =
        shedsForFarm(farmId).flatMap { linesForShed(it.id) }

    /**
     * Resuelve la jerarquía completa o retorna `null` si algún ID falta o contradice a su padre.
     * Esta validación evita enlazar alertas y registros antiguos con una línea incompatible.
     */
    fun resolveLocation(
        farmId: Int,
        shedId: Int,
        lineId: Int
    ): ResolvedLocation? {
        val farm = farm(farmId) ?: return null
        val shed = shed(shedId)
            ?.takeIf { it.farmId == farm.id }
            ?: return null
        val line = line(lineId)
            ?.takeIf { it.farmId == farm.id && it.shedId == shed.id }
            ?: return null
        return ResolvedLocation(farm, shed, line)
    }

    fun resolveLocation(alert: Alert): ResolvedLocation? =
        resolveLocation(alert.farmId, alert.shedId, alert.lineId)

    fun resolveLocation(flushing: FlushingRecord): ResolvedLocation? =
        resolveLocation(flushing.farmId, flushing.shedId, flushing.lineId)

    /** Filtra por la jerarquía completa validada y ordena los flushing del más reciente al más antiguo. */
    fun flushingsForLine(
        records: List<FlushingRecord>,
        line: WaterLine
    ): List<FlushingRecord> = sortFlushingsChronologically(
        records.filter { flushing ->
            flushing.farmId == line.farmId &&
                    flushing.shedId == line.shedId &&
                    flushing.lineId == line.id &&
                    resolveLocation(flushing)?.line?.id == line.id
        }
    )

    fun historyForLine(lineId: Int) = history.filter { it.lineId == lineId }
}

private val flushingTimestampFormatter = DateTimeFormatter
    .ofPattern("dd/MM/uuuu HH:mm")
    .withResolverStyle(ResolverStyle.STRICT)

/** Convierte fecha y hora persistidas; un dato histórico mal formado se trata de forma segura. */
internal fun FlushingRecord.parsedTimestamp(): LocalDateTime? = try {
    LocalDateTime.parse("$date $time", flushingTimestampFormatter)
} catch (_: DateTimeParseException) {
    null
}

/**
 * Ordena primero los registros con fecha válida y usa el ID como desempate o respaldo heredado.
 */
internal fun sortFlushingsChronologically(
    records: List<FlushingRecord>
): List<FlushingRecord> = records.sortedWith { first, second ->
    val firstTimestamp = first.parsedTimestamp()
    val secondTimestamp = second.parsedTimestamp()

    when {
        firstTimestamp != null && secondTimestamp != null -> {
            val timestampOrder = secondTimestamp.compareTo(firstTimestamp)
            if (timestampOrder != 0) timestampOrder else second.id.compareTo(first.id)
        }
        firstTimestamp != null -> -1
        secondTimestamp != null -> 1
        else -> second.id.compareTo(first.id)
    }
}
