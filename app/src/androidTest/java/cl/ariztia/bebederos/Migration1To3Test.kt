package cl.ariztia.bebederos
import cl.ariztia.bebederos.data.local.AppDatabase
import cl.ariztia.bebederos.data.local.FlushingEntity
import android.content.Context
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Migration1To3Test {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Before
    fun prepare() {
        databaseNames.forEach(context::deleteDatabase)
    }

    @After
    fun cleanUp() {
        databaseNames.forEach(context::deleteDatabase)
    }

    @Test
    fun migrate2To3PreservesContextualAndNullRowsAndAddsManualColumns() {
        helper.createDatabase(V2_DATABASE, 2).apply {
            insertV2Row(41, 24.3, "content://flushing_images/contextual.jpg")
            insertV2Row(42, null, "content://flushing_images/legacy-null.jpg")
            close()
        }

        helper.runMigrationsAndValidate(
            V2_DATABASE,
            3,
            true,
            AppDatabase.MIGRATION_2_3
        ).use { migrated ->
            assertDatabaseVersion(migrated, 3)
            migrated.query("SELECT * FROM flushings ORDER BY id").use { cursor ->
                assertEquals(2, cursor.count)
                assertTrue(cursor.moveToFirst())
                assertEquals(41L, cursor.getLong(cursor.getColumnIndexOrThrow("id")))
                assertEquals(24.3, cursor.getDouble(cursor.getColumnIndexOrThrow("temperatureAtRegistrationC")), 0.0)
                assertEquals(
                    "content://flushing_images/contextual.jpg",
                    cursor.getString(cursor.getColumnIndexOrThrow("photoUri"))
                )
                assertTrue(cursor.isNull(cursor.getColumnIndexOrThrow("beforeTemperatureC")))
                assertTrue(cursor.isNull(cursor.getColumnIndexOrThrow("afterTemperatureC")))

                assertTrue(cursor.moveToNext())
                assertEquals(42L, cursor.getLong(cursor.getColumnIndexOrThrow("id")))
                assertTrue(cursor.isNull(cursor.getColumnIndexOrThrow("temperatureAtRegistrationC")))
                assertTrue(cursor.isNull(cursor.getColumnIndexOrThrow("beforeTemperatureC")))
                assertTrue(cursor.isNull(cursor.getColumnIndexOrThrow("afterTemperatureC")))
                assertEquals(
                    "content://flushing_images/legacy-null.jpg",
                    cursor.getString(cursor.getColumnIndexOrThrow("photoUri"))
                )
            }
        }
    }

    @Test
    fun migrate1Through2To3PreservesCompleteLegacyRow() {
        helper.createDatabase(V1_DATABASE, 1).apply {
            execSQL(
                """
                INSERT INTO flushings (
                    id, farmId, shedId, lineId, operatorName, date, time,
                    reason, observation, photoUri, completed
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent(),
                arrayOf<Any>(
                    51, 1, 2, 3, "Operario histórico", "23/09/2026", "14:35",
                    "Temperatura alta", "Registro v1",
                    "content://flushing_images/historica-v1.jpg", 1
                )
            )
            close()
        }

        helper.runMigrationsAndValidate(
            V1_DATABASE,
            3,
            true,
            AppDatabase.MIGRATION_1_2,
            AppDatabase.MIGRATION_2_3
        ).use { migrated ->
            assertDatabaseVersion(migrated, 3)
            migrated.query("SELECT * FROM flushings WHERE id = 51").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("Operario histórico", cursor.getString(cursor.getColumnIndexOrThrow("operatorName")))
                assertEquals("Registro v1", cursor.getString(cursor.getColumnIndexOrThrow("observation")))
                assertEquals(
                    "content://flushing_images/historica-v1.jpg",
                    cursor.getString(cursor.getColumnIndexOrThrow("photoUri"))
                )
                assertTrue(cursor.isNull(cursor.getColumnIndexOrThrow("temperatureAtRegistrationC")))
                assertTrue(cursor.isNull(cursor.getColumnIndexOrThrow("beforeTemperatureC")))
                assertTrue(cursor.isNull(cursor.getColumnIndexOrThrow("afterTemperatureC")))
            }
        }
    }

    @Test
    fun manualMeasurementsEmitReactivelyAndSurviveDatabaseRestart() = runBlocking {
        helper.createDatabase(RESTART_DATABASE, 2).apply {
            insertV2Row(61, 24.3, "content://flushing_images/preserved.jpg")
            close()
        }
        helper.runMigrationsAndValidate(
            RESTART_DATABASE,
            3,
            true,
            AppDatabase.MIGRATION_2_3
        ).close()

        val firstOpen = openMigratedDatabase(RESTART_DATABASE)
        val insertedId: Long
        try {
            val updatedRows = async(start = CoroutineStart.UNDISPATCHED) {
                firstOpen.flushingDao().observeAll().first { it.size == 2 }
            }
            insertedId = firstOpen.flushingDao().insert(
                FlushingEntity(
                    farmId = 1,
                    shedId = 2,
                    lineId = 3,
                    operatorName = "Juan Pérez",
                    date = "10/10/2026",
                    time = "21:30",
                    reason = "Otro",
                    observation = "ETAPA 2.3",
                    photoUri = null,
                    completed = true,
                    temperatureAtRegistrationC = null,
                    beforeTemperatureC = 28.9,
                    afterTemperatureC = 21.5
                )
            )

            val rows = withTimeout(5_000) { updatedRows.await() }
            val inserted = rows.first { it.id == insertedId }
            assertEquals(28.9, inserted.beforeTemperatureC ?: Double.NaN, 0.0)
            assertEquals(21.5, inserted.afterTemperatureC ?: Double.NaN, 0.0)
            assertNull(inserted.temperatureAtRegistrationC)
        } finally {
            firstOpen.close()
        }

        val reopened = openMigratedDatabase(RESTART_DATABASE)
        try {
            val rows = reopened.flushingDao().observeAll().first()
            assertEquals(2, rows.size)
            val persisted = rows.first { it.id == insertedId }
            assertEquals(28.9, persisted.beforeTemperatureC ?: Double.NaN, 0.0)
            assertEquals(21.5, persisted.afterTemperatureC ?: Double.NaN, 0.0)
            val legacy = rows.first { it.id == 61L }
            assertEquals(24.3, legacy.temperatureAtRegistrationC ?: Double.NaN, 0.0)
            assertEquals("content://flushing_images/preserved.jpg", legacy.photoUri)
        } finally {
            reopened.close()
        }
    }

    private fun openMigratedDatabase(name: String): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, name)
            .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3)
            .build()

    private fun SupportSQLiteDatabase.insertV2Row(
        id: Long,
        contextualTemperature: Double?,
        photoUri: String
    ) {
        execSQL(
            """
            INSERT INTO flushings (
                id, farmId, shedId, lineId, operatorName, date, time,
                reason, observation, photoUri, completed, temperatureAtRegistrationC
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            arrayOf<Any?>(
                id, 1, 2, 3, "Operario histórico", "23/09/2026", "14:35",
                "Temperatura alta", "Registro v2", photoUri, 1, contextualTemperature
            )
        )
    }

    private fun assertDatabaseVersion(database: SupportSQLiteDatabase, expected: Int) {
        database.query("PRAGMA user_version").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(expected, cursor.getInt(0))
        }
    }

    private companion object {
        const val V1_DATABASE = "migration-stage-2-3-v1.db"
        const val V2_DATABASE = "migration-stage-2-3-v2.db"
        const val RESTART_DATABASE = "migration-stage-2-3-restart.db"
        val databaseNames = listOf(V1_DATABASE, V2_DATABASE, RESTART_DATABASE)
    }
}
