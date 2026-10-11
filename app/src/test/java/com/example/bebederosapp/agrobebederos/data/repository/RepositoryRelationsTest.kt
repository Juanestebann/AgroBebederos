package com.example.bebederosapp.agrobebederos.data.repository

import cl.ariztia.bebederos.data.model.FlushingRecord
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RepositoryRelationsTest {
    private val repository = DemoRepository()

    @Test
    fun shedsReturnedForEachFarmBelongToThatFarm() {
        repository.farms.forEach { farm ->
            val sheds = repository.shedsForFarm(farm.id)

            assertTrue(sheds.isNotEmpty())
            assertTrue(sheds.all { it.farmId == farm.id })
        }
    }

    @Test
    fun linesReturnedForEachShedMatchBothShedAndFarm() {
        repository.sheds.forEach { shed ->
            val lines = repository.linesForShed(shed.id)

            assertTrue(lines.isNotEmpty())
            assertTrue(lines.all { it.shedId == shed.id && it.farmId == shed.farmId })
        }
    }

    @Test
    fun farmLinesOnlyContainFullyResolvedLocations() {
        repository.farms.forEach { farm ->
            repository.linesForFarm(farm.id).forEach { line ->
                val location = assertNotNull(
                    repository.resolveLocation(farm.id, line.shedId, line.id)
                )

                assertEquals(farm.id, location.farm.id)
                assertEquals(farm.id, location.shed.farmId)
                assertEquals(location.shed.id, location.line.shedId)
            }
        }
    }

    @Test
    fun shedCountComesFromRepositoryRelationsInsteadOfCopiedIdMetadata() {
        val farm = repository.farms.first()
        val farmWithStaleMetadata = farm.copy(shedIds = listOf(999, 999, 999, 999))

        assertEquals(
            repository.sheds.count { it.farmId == farm.id },
            repository.shedCountForFarm(farmWithStaleMetadata)
        )
    }

    @Test
    fun demoAlertsResolveToACompleteHierarchy() {
        repository.alerts.forEach { alert ->
            val location = assertNotNull(repository.resolveLocation(alert))

            assertEquals(alert.farmId, location.farm.id)
            assertEquals(alert.shedId, location.shed.id)
            assertEquals(alert.lineId, location.line.id)
        }
    }

    @Test
    fun crossedOrMissingAlertReferencesDoNotResolve() {
        val valid = repository.alerts.first()

        assertNull(repository.resolveLocation(valid.copy(shedId = 4)))
        assertNull(repository.resolveLocation(valid.copy(lineId = 14)))
        assertNull(repository.resolveLocation(valid.copy(farmId = 999)))
    }

    @Test
    fun staleFlushingReferenceIsNotAssociatedWithAnotherLine() {
        val line = requireNotNull(repository.line(3))
        val valid = repository.initialFlushings.first { it.lineId == line.id }
        val stale = valid.copy(id = 999, farmId = 2, shedId = 4)

        assertNull(repository.resolveLocation(stale))
        assertEquals(
            listOf(valid),
            repository.flushingsForLine(listOf(stale, valid), line)
        )
    }

    @Test
    fun lineHistoryRequiresMatchingFarmShedAndLineIds() {
        val line = requireNotNull(repository.line(3))
        val valid = flushing(1, "10/10/2026", "10:30")
        val wrongFarm = valid.copy(id = 2, farmId = 2)
        val wrongShed = valid.copy(id = 3, shedId = 1)
        val wrongLine = valid.copy(id = 4, lineId = 4)

        assertEquals(
            listOf(valid),
            repository.flushingsForLine(
                listOf(wrongFarm, wrongShed, wrongLine, valid),
                line
            )
        )
    }

    @Test
    fun lineFlushingsAreSortedByDateAndTimeDescendingInsteadOfId() {
        val line = requireNotNull(repository.line(3))
        val records = listOf(
            flushing(100, "09/10/2026", "08:00"),
            flushing(1, "10/10/2026", "07:00"),
            flushing(2, "10/10/2026", "09:00")
        )

        assertEquals(
            listOf(2L, 1L, 100L),
            repository.flushingsForLine(records, line).map { it.id }
        )
    }

    @Test
    fun equalTimestampsUseDescendingIdAsStableTieBreaker() {
        val records = listOf(
            flushing(4, "10/10/2026", "10:30"),
            flushing(9, "10/10/2026", "10:30")
        )

        assertEquals(listOf(9L, 4L), sortFlushingsChronologically(records).map { it.id })
    }

    @Test
    fun invalidHistoricalTimestampsAreLastAndDoNotCrash() {
        val records = listOf(
            flushing(8, "fecha antigua", "sin hora"),
            flushing(2, "10/10/2026", "10:30"),
            flushing(9, "31/02/2026", "10:30")
        )

        assertEquals(listOf(2L, 9L, 8L), sortFlushingsChronologically(records).map { it.id })
    }

    private fun flushing(id: Long, date: String, time: String) = FlushingRecord(
        id = id,
        farmId = 1,
        shedId = 2,
        lineId = 3,
        operatorName = "Prueba",
        date = date,
        time = time,
        reason = "Otro",
        observation = "Orden"
    )
}
