package cl.ariztia.bebederos.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FlushingDao {
    @Query("SELECT * FROM flushings ORDER BY id DESC")
    fun observeAll(): Flow<List<FlushingEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: FlushingEntity): Long

    @Query("SELECT COUNT(*) FROM flushings")
    suspend fun count(): Int
}