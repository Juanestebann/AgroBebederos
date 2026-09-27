package cl.ariztia.bebederos.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import cl.ariztia.bebederos.data.model.FlushingRecord

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
    val completed: Boolean
)

fun FlushingEntity.toModel() = FlushingRecord(
    id,
    farmId,
    shedId,
    lineId,
    operatorName,
    date,
    time,
    reason,
    observation,
    photoUri,
    completed
)

fun FlushingRecord.toEntity() = FlushingEntity(
    id,
    farmId,
    shedId,
    lineId,
    operatorName,
    date,
    time,
    reason,
    observation,
    photoUri,
    completed
)