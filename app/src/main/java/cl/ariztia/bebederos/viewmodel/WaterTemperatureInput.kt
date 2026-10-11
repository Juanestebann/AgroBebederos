package cl.ariztia.bebederos.viewmodel

import cl.ariztia.bebederos.data.model.MAX_WATER_TEMPERATURE_C
import cl.ariztia.bebederos.data.model.MIN_WATER_TEMPERATURE_C

/** Resultado analizado de un campo térmico, con valor o mensaje de error excluyentes. */
internal data class WaterTemperatureInput(
    val value: Double? = null,
    val error: String? = null
) {
    val isValid: Boolean
        get() = value != null && error == null
}

/**
 * Acepta coma o punto decimal y exige un número finito dentro del rango compartido 0–50 °C.
 */
internal fun validateWaterTemperatureInput(
    input: String,
    fieldName: String
): WaterTemperatureInput {
    val normalized = input.trim().replace(',', '.')
    if (normalized.isBlank()) {
        return WaterTemperatureInput(error = "Ingresa la temperatura $fieldName del flushing.")
    }

    val value = normalized.toDoubleOrNull()
    if (value == null || !value.isFinite()) {
        return WaterTemperatureInput(
            error = "Ingresa una temperatura $fieldName válida usando coma o punto decimal."
        )
    }

    if (value !in MIN_WATER_TEMPERATURE_C..MAX_WATER_TEMPERATURE_C) {
        return WaterTemperatureInput(
            error = "La temperatura $fieldName debe estar entre " +
                "${MIN_WATER_TEMPERATURE_C.toInt()} y ${MAX_WATER_TEMPERATURE_C.toInt()} °C."
        )
    }

    return WaterTemperatureInput(value = value)
}
