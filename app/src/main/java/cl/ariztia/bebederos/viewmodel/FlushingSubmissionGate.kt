package cl.ariztia.bebederos.viewmodel

/** Bloqueo temporal en memoria que rechaza una segunda pulsación mientras se guarda. */
internal class FlushingSubmissionGate {
    private var locked = false

    /** @return `true` solo para el primer intento hasta que se invoque [unlock]. */
    fun tryLock(): Boolean {
        if (locked) return false
        locked = true
        return true
    }

    fun unlock() {
        locked = false
    }
}
