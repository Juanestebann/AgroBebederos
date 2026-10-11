package cl.ariztia.bebederos.viewmodel

import cl.ariztia.bebederos.data.model.TemperatureRecord
import java.time.Clock
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle
import java.util.Locale

/** Opciones de filtro temporal aplicadas a las lecturas demostrativas. */
internal enum class TemperatureHistoryPeriod(
    val label: String,
    val duration: Duration?
) {
    ALL("Todo", null),
    LAST_24_HOURS("24h", Duration.ofHours(24)),
    LAST_7_DAYS("7d", Duration.ofDays(7))
}

/** Filtra solo registros con fecha válida dentro del periodo; no sustituye fechas por la actual. */
internal class TemperatureHistoryFilter(
    private val clock: Clock = Clock.systemDefaultZone()
) {
    fun filter(
        records: List<TemperatureRecord>,
        period: TemperatureHistoryPeriod
    ): List<TemperatureRecord> {
        val duration = period.duration ?: return records
        val now = clock.instant()
        val start = now.minus(duration)

        return records.filter { record ->
            val instant = record.toInstant(clock) ?: return@filter false
            !instant.isBefore(start) && !instant.isAfter(now)
        }
    }

    private fun TemperatureRecord.toInstant(clock: Clock) =
        runCatching {
            LocalDateTime.parse("$date $time", RECORD_FORMATTER)
                .atZone(clock.zone)
                .toInstant()
        }.getOrNull()

    private companion object {
        val RECORD_FORMATTER: DateTimeFormatter =
            DateTimeFormatter.ofPattern("dd/MM/uuuu HH:mm", Locale.ROOT)
                .withResolverStyle(ResolverStyle.STRICT)
    }
}
