package cl.ariztia.bebederos.data.model

enum class UserRole { OPERARIO, SUPERVISOR }
enum class TemperatureStatus { NORMAL, WARNING, CRITICAL }

data class DemoUser(
    val email: String,
    val password: String,
    val name: String,
    val role: UserRole,
    val farm: String
)

data class Farm(
    val id: Int,
    val name: String,
    val shedIds: List<Int>
)

data class Shed(
    val id: Int,
    val farmId: Int,
    val name: String,
    val lineIds: List<Int>
)

data class WaterLine(
    val id: Int,
    val farmId: Int,
    val shedId: Int,
    val name: String,
    val temperature: Double,
    val updatedAt: String
) {
    val status: TemperatureStatus
        get() = temperatureStatusFor(temperature)
}

data class TemperatureRecord(
    val lineId: Int,
    val date: String,
    val time: String,
    val value: Double
) {
    val status: TemperatureStatus
        get() = temperatureStatusFor(value)
}

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
    val completed: Boolean = true
)

fun temperatureStatusFor(value: Double): TemperatureStatus = when {
    value >= 27.0 -> TemperatureStatus.CRITICAL
    value >= 23.0 -> TemperatureStatus.WARNING
    else -> TemperatureStatus.NORMAL
}