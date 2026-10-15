package cl.ariztia.bebederos

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import cl.ariztia.bebederos.data.local.AppDatabase
import cl.ariztia.bebederos.data.local.FlushingEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FlushingDaoTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun prepare() {
        context.deleteDatabase(DATABASE_NAME)
    }

    @After
    fun cleanUp() {
        context.deleteDatabase(DATABASE_NAME)
    }

    @Test
    fun updateChangesOneRowAndObserveAllEmitsTheNewValue() = runBlocking {
        withDatabase { database ->
            val dao = database.flushingDao()
            val id = dao.insert(entity())
            val persisted = requireNotNull(dao.findById(id))

            assertEquals(1, dao.update(persisted.copy(reason = "Limpieza")))

            val emission = withTimeout(5_000) {
                dao.observeAll().first { rows -> rows.singleOrNull()?.reason == "Limpieza" }
            }
            assertEquals(id, emission.single().id)
            assertEquals("Original", emission.single().observation)
        }
    }

    @Test
    fun updateOfMissingIdReturnsZero() = runBlocking {
        withDatabase { database ->
            assertEquals(0, database.flushingDao().update(entity(id = 999)))
        }
    }

    @Test
    fun deleteRemovesOneRowAndObserveAllEmitsEmpty() = runBlocking {
        withDatabase { database ->
            val dao = database.flushingDao()
            val id = dao.insert(entity())
            val persisted = requireNotNull(dao.findById(id))

            assertEquals(1, dao.delete(persisted))

            val emission = withTimeout(5_000) {
                dao.observeAll().first { it.isEmpty() }
            }
            assertEquals(emptyList<FlushingEntity>(), emission)
        }
    }

    @Test
    fun deleteOfMissingIdReturnsZero() = runBlocking {
        withDatabase { database ->
            assertEquals(0, database.flushingDao().delete(entity(id = 999)))
        }
    }

    @Test
    fun updateAndDeleteRemainPersistedAfterDatabaseRestart() = runBlocking {
        val insertedId = withDatabase { database ->
            val dao = database.flushingDao()
            val id = dao.insert(entity())
            val persisted = requireNotNull(dao.findById(id))
            assertEquals(1, dao.update(persisted.copy(afterTemperatureC = 20.5)))
            id
        }

        withDatabase { database ->
            val dao = database.flushingDao()
            val persisted = requireNotNull(dao.findById(insertedId))
            assertEquals(20.5, persisted.afterTemperatureC ?: Double.NaN, 0.0)
            assertEquals(1, dao.delete(persisted))
        }

        withDatabase { database ->
            assertNull(database.flushingDao().findById(insertedId))
        }
    }

    @Test
    fun sharedPhotoReferenceRemainsCountedAfterDeletingOnlyOneRow() = runBlocking {
        withDatabase { database ->
            val dao = database.flushingDao()
            val sharedUri =
                "content://cl.ariztia.bebederos.fileprovider/flushing_images/shared.jpg"
            val firstId = dao.insert(entity().copy(photoUri = sharedUri))
            dao.insert(entity().copy(photoUri = sharedUri, time = "10:31"))

            assertEquals(2, dao.countPhotoReferences(sharedUri))
            assertEquals(1, dao.delete(requireNotNull(dao.findById(firstId))))
            assertEquals(1, dao.countPhotoReferences(sharedUri))
        }
    }

    private fun openDatabase(): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, DATABASE_NAME)
            .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3)
            .build()

    private suspend fun <T> withDatabase(block: suspend (AppDatabase) -> T): T {
        val database = openDatabase()
        return try {
            block(database)
        } finally {
            database.close()
        }
    }

    private fun entity(id: Long = 0) = FlushingEntity(
        id = id,
        farmId = 1,
        shedId = 2,
        lineId = 3,
        operatorName = "Juan Pérez",
        date = "14/10/2026",
        time = "10:30",
        reason = "Otro",
        observation = "Original",
        photoUri = null,
        completed = true,
        temperatureAtRegistrationC = null,
        beforeTemperatureC = 28.9,
        afterTemperatureC = 21.5
    )

    private companion object {
        const val DATABASE_NAME = "flushing-dao-test.db"
    }
}
