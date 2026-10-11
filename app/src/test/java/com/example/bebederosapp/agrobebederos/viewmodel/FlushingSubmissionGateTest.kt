package com.example.bebederosapp.agrobebederos.viewmodel

import cl.ariztia.bebederos.viewmodel.FlushingSubmissionGate
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FlushingSubmissionGateTest {
    @Test
    fun duplicateSubmissionRemainsBlockedUntilExplicitUnlock() {
        val gate = FlushingSubmissionGate()

        assertTrue(gate.tryLock())
        assertFalse(gate.tryLock())
        assertFalse(gate.tryLock())

        gate.unlock()

        assertTrue(gate.tryLock())
    }
}
