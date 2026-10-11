package com.example.bebederosapp.agrobebederos.viewmodel

import cl.ariztia.bebederos.data.model.FlushingRecord
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class FlushingListUiStateTest {
    @Test
    fun initialLoadingAndLoadedEmptyAreDifferentStates() {
        val loading: FlushingListUiState = FlushingListUiState.Loading
        val loadedEmpty = flushingListUiState(emptyList())

        assertNotEquals(loading, loadedEmpty)
        assertIs<FlushingListUiState.Ready>(loadedEmpty)
        assertTrue(loadedEmpty.records.isEmpty())
    }

    @Test
    fun readyStatePreservesRecordsForAutomaticRoomUpdates() {
        val record = FlushingRecord(
            id = 42,
            farmId = 1,
            shedId = 2,
            lineId = 3,
            operatorName = "Operario",
            date = "23/09/2026",
            time = "14:35",
            reason = "Temperatura alta",
            observation = "Sin novedades"
        )

        val state = assertIs<FlushingListUiState.Ready>(
            flushingListUiState(listOf(record))
        )

        assertEquals(listOf(record), state.records)
    }
}
