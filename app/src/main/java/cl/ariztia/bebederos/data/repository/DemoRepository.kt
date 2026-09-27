package cl.ariztia.bebederos.data.repository

import cl.ariztia.bebederos.data.model.*

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
        WaterLine(1, 1, 1, "Línea 1", 19.8, "14:32"),
        WaterLine(2, 1, 1, "Línea 2", 24.3, "14:30"),
        WaterLine(3, 1, 2, "Línea 3", 28.9, "14:28"),
        WaterLine(4, 1, 2, "Línea 4", 20.5, "14:31"),
        WaterLine(5, 1, 3, "Línea 5", 23.1, "14:29"),
        WaterLine(6, 2, 4, "Línea 1", 21.2, "14:25"),
        WaterLine(7, 2, 4, "Línea 2", 22.4, "14:22"),
        WaterLine(8, 2, 5, "Línea 3", 20.8, "14:19"),
        WaterLine(9, 2, 5, "Línea 4", 24.1, "14:18"),
        WaterLine(10, 2, 6, "Línea 5", 21.0, "14:17"),
        WaterLine(11, 2, 6, "Línea 6", 20.2, "14:16"),
        WaterLine(12, 2, 7, "Línea 7", 21.4, "14:15"),
        WaterLine(13, 3, 8, "Línea 1", 20.7, "14:14"),
        WaterLine(14, 3, 9, "Línea 2", 21.1, "14:13")
    )

    val alerts = listOf(
        Alert(1, 1, 2, 3, 28.9, "23/09/2026", "14:28"),
        Alert(2, 1, 1, 2, 24.3, "23/09/2026", "14:15"),
        Alert(3, 1, 3, 5, 23.1, "23/09/2026", "13:50")
    )

    val history = listOf(
        TemperatureRecord(3, "23/09/2026", "08:00", 19.5),
        TemperatureRecord(3, "23/09/2026", "09:00", 20.1),
        TemperatureRecord(3, "23/09/2026", "10:00", 21.8),
        TemperatureRecord(3, "23/09/2026", "11:00", 23.4),
        TemperatureRecord(3, "23/09/2026", "12:00", 25.2),
        TemperatureRecord(3, "23/09/2026", "13:00", 27.0),
        TemperatureRecord(3, "23/09/2026", "14:00", 28.9),
        TemperatureRecord(3, "23/09/2026", "15:00", 27.5),
        TemperatureRecord(3, "23/09/2026", "16:00", 25.8)
    )

    val initialFlushings = listOf(
        FlushingRecord(1, 1, 2, 3, "Juan Pérez", "23/09/2026", "14:35", "Temperatura alta", "Flushing realizado", completed = true),
        FlushingRecord(2, 2, 4, 7, "María López", "23/09/2026", "13:10", "Mantenimiento preventivo", "Sin novedades", completed = true),
        FlushingRecord(3, 1, 3, 5, "Carlos Rivas", "22/09/2026", "17:45", "Limpieza", "Pendiente de revisión", completed = false)
    )

    fun authenticate(email: String, password: String): DemoUser? =
        users.find { it.email.equals(email.trim(), ignoreCase = true) && it.password == password }

    fun farm(id: Int) = farms.firstOrNull { it.id == id }
    fun shed(id: Int) = sheds.firstOrNull { it.id == id }
    fun line(id: Int) = lines.firstOrNull { it.id == id }
    fun shedsForFarm(farmId: Int) = sheds.filter { it.farmId == farmId }
    fun linesForShed(shedId: Int) = lines.filter { it.shedId == shedId }
    fun linesForFarm(farmId: Int) = lines.filter { it.farmId == farmId }
    fun historyForLine(lineId: Int) = history.filter { it.lineId == lineId }.ifEmpty { history.map { it.copy(lineId = lineId) } }
}