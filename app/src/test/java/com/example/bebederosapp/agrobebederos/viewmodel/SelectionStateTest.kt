package com.example.bebederosapp.agrobebederos.viewmodel

import cl.ariztia.bebederos.data.model.DemoUser
import cl.ariztia.bebederos.data.model.UserRole
import cl.ariztia.bebederos.data.repository.DemoRepository
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SelectionStateTest {
    private val repository = DemoRepository()
    private val operator = repository.users.first { it.role == UserRole.OPERARIO }
    private val supervisor = repository.users.first { it.role == UserRole.SUPERVISOR }

    @Test
    fun operatorSessionStartsInsideAssignedFarm() {
        val state = SelectionState(repository)

        assertTrue(state.startSession(operator))

        val selection = assertNotNull(state.validatedCurrent())
        assertEquals(operator.farm, selection.farm.name)
        assertEquals(selection.farm.id, selection.shed.farmId)
        assertEquals(selection.farm.id, selection.line.farmId)
        assertEquals(selection.shed.id, selection.line.shedId)
    }

    @Test
    fun supervisorSessionStartsWithAValidHierarchy() {
        val state = SelectionState(repository)

        assertTrue(state.startSession(supervisor))

        val selection = assertNotNull(state.validatedCurrent())
        assertEquals(selection.farm.id, selection.shed.farmId)
        assertEquals(selection.farm.id, selection.line.farmId)
        assertEquals(selection.shed.id, selection.line.shedId)
    }

    @Test
    fun selectingFarmUpdatesShedAndLine() {
        val state = SelectionState(repository)
        assertTrue(state.startSession(supervisor))

        state.selectFarm(2)

        assertEquals(2, state.current.farm.id)
        assertEquals(2, state.current.shed.farmId)
        assertEquals(state.current.shed.id, state.current.line.shedId)
        assertEquals(2, state.current.line.farmId)
    }

    @Test
    fun selectingShedUpdatesFarmAndLine() {
        val state = SelectionState(repository)
        assertTrue(state.startSession(supervisor))

        state.selectShed(5)

        assertEquals(5, state.current.shed.id)
        assertEquals(2, state.current.farm.id)
        assertEquals(5, state.current.line.shedId)
        assertEquals(2, state.current.line.farmId)
    }

    @Test
    fun selectingLineUpdatesShedAndFarm() {
        val state = SelectionState(repository)
        assertTrue(state.startSession(supervisor))

        state.selectLine(14)

        assertEquals(14, state.current.line.id)
        assertEquals(9, state.current.shed.id)
        assertEquals(3, state.current.farm.id)
    }

    @Test
    fun selectingUnknownIdsDoesNotChangeCurrentSelection() {
        val state = SelectionState(repository)
        assertTrue(state.startSession(supervisor))
        state.selectLine(5)
        val expected = state.current

        state.selectFarm(Int.MAX_VALUE)
        assertEquals(expected, state.current)

        state.selectShed(Int.MAX_VALUE)
        assertEquals(expected, state.current)

        state.selectLine(Int.MAX_VALUE)
        assertEquals(expected, state.current)
    }

    @Test
    fun logoutClearsThePreviousSessionSelection() {
        val state = SelectionState(repository)
        assertTrue(state.startSession(supervisor))
        state.selectLine(14)

        state.reset()

        assertNull(state.validatedCurrent())
        assertFalse(state.current.line.id == 14)
    }

    @Test
    fun newOperatorSessionDoesNotInheritTheSupervisorsLine() {
        val state = SelectionState(repository)
        assertTrue(state.startSession(supervisor))
        state.selectLine(14)

        state.reset()
        assertTrue(state.startSession(operator))

        val selection = assertNotNull(state.validatedCurrent())
        assertEquals(1, selection.farm.id)
        assertFalse(selection.line.id == 14)
    }

    @Test
    fun operatorCannotSelectLocationsFromAnotherFarm() {
        val state = SelectionState(repository)
        assertTrue(state.startSession(operator))
        val expected = state.current

        state.selectFarm(3)
        state.selectShed(9)
        state.selectLine(14)

        assertEquals(expected, state.current)
        assertNotNull(state.validatedCurrent())
    }

    @Test
    fun operatorWithUnknownFarmCannotStartASession() {
        val state = SelectionState(repository)
        val invalidOperator = DemoUser(
            email = "sin-granja@ariztia.cl",
            password = "Demo123",
            name = "Sin granja",
            role = UserRole.OPERARIO,
            farm = "Granja inexistente"
        )

        assertFalse(state.startSession(invalidOperator))
        assertNull(state.validatedCurrent())
    }

    @Test
    fun flushingIdsBelongToTheSelectedEntities() {
        val state = SelectionState(repository)
        assertTrue(state.startSession(operator))
        state.selectLine(5)
        val location = assertNotNull(state.validatedCurrent())

        val record = createFlushingRecord(
            location,
            operatorName = "Juan Pérez",
            reason = "Temperatura alta",
            observation = "Verificación",
            photoUri = null,
            timestamp = FlushingTimestamp("23/09/2026", "14:38"),
            beforeTemperatureC = 28.9,
            afterTemperatureC = 21.5
        )

        val farm = assertNotNull(repository.farm(record.farmId))
        val shed = assertNotNull(repository.shed(record.shedId))
        val line = assertNotNull(repository.line(record.lineId))

        assertEquals(1, record.farmId)
        assertEquals(3, record.shedId)
        assertEquals(5, record.lineId)
        assertEquals(farm.id, shed.farmId)
        assertEquals(farm.id, line.farmId)
        assertEquals(shed.id, line.shedId)
    }
}
