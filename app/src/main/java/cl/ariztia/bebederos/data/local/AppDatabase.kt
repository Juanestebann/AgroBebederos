package cl.ariztia.bebederos.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [FlushingEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun flushingDao(): FlushingDao
}