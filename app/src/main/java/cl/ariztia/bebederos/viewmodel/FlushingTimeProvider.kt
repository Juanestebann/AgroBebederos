package cl.ariztia.bebederos.viewmodel

import java.time.Clock
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Fecha y hora local capturadas en el instante real del registro de flushing. */
internal data class FlushingTimestamp(
    val date: String,
    val time: String
)

/** Encapsula el reloj para separar la hora real de flushing de las fechas del conjunto demo. */
internal class FlushingTimeProvider(
    private val clock: Clock = Clock.systemDefaultZone()
) {
    fun now(): FlushingTimestamp {
        val dateTime = ZonedDateTime.ofInstant(clock.instant(), clock.zone)
        return FlushingTimestamp(
            date = dateTime.format(DATE_FORMATTER),
            time = dateTime.format(TIME_FORMATTER)
        )
    }

    private companion object {
        val DATE_FORMATTER: DateTimeFormatter =
            DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.ROOT)
        val TIME_FORMATTER: DateTimeFormatter =
            DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT)
    }
}
