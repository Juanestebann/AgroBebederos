package cl.ariztia.bebederos.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Base Room local; los flushing son datos persistentes y no se sincronizan con un servidor. */
@Database(
    entities = [FlushingEntity::class],
    version = 3,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun flushingDao(): FlushingDao

    companion object {
        /**
         * Añade el contexto térmico automático heredado como nullable para conservar filas previas.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE `flushings` ADD COLUMN `temperatureAtRegistrationC` REAL"
                )
            }
        }

        /**
         * Añade las mediciones manuales antes/después como nullable para conservar filas previas.
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE `flushings` ADD COLUMN `beforeTemperatureC` REAL"
                )
                db.execSQL(
                    "ALTER TABLE `flushings` ADD COLUMN `afterTemperatureC` REAL"
                )
            }
        }
    }
}
