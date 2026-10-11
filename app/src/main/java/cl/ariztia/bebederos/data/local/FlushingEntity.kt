package cl.ariztia.bebederos.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import cl.ariztia.bebederos.data.model.FlushingRecord

/**
 * Fila Room de un flushing.
 *
 * Las temperaturas son anulables por compatibilidad con versiones antiguas. [photoUri] referencia
 * un archivo privado conservado por la aplicación; la imagen no se guarda dentro de la base.
 */
@Entity(tableName = "flushings")
data class FlushingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val farmId: Int,
    val shedId: Int,
    val lineId: Int,
    val operatorName: String,
    val date: String,
    val time: String,
    val reason: String,
    val observation: String,
    val photoUri: String?,
    val completed: Boolean,
    val temperatureAtRegistrationC: Double? = null,
    val beforeTemperatureC: Double? = null,
    val afterTemperatureC: Double? = null
)

/** Traduce la representación persistida al modelo que consumen ViewModel y UI. */
fun FlushingEntity.toModel() = FlushingRecord(
    id = id,
    farmId = farmId,
    shedId = shedId,
    lineId = lineId,
    operatorName = operatorName,
    date = date,
    time = time,
    reason = reason,
    observation = observation,
    photoUri = photoUri,
    completed = completed,
    temperatureAtRegistrationC = temperatureAtRegistrationC,
    beforeTemperatureC = beforeTemperatureC,
    afterTemperatureC = afterTemperatureC
)

/** Traduce un registro validado a la fila que inserta Room. */
fun FlushingRecord.toEntity() = FlushingEntity(
    id = id,
    farmId = farmId,
    shedId = shedId,
    lineId = lineId,
    operatorName = operatorName,
    date = date,
    time = time,
    reason = reason,
    observation = observation,
    photoUri = photoUri,
    completed = completed,
    temperatureAtRegistrationC = temperatureAtRegistrationC,
    beforeTemperatureC = beforeTemperatureC,
    afterTemperatureC = afterTemperatureC
)
