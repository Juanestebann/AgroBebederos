package cl.ariztia.bebederos.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** Punto de acceso a los flushing persistidos en Room. */
@Dao
interface FlushingDao {
    /**
     * Observa todas las filas. Room vuelve a emitir la lista automáticamente después de insertar.
     */
    @Query("SELECT * FROM flushings ORDER BY id DESC")
    fun observeAll(): Flow<List<FlushingEntity>>

    /** Inserta fuera del hilo de UI y devuelve el ID generado. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: FlushingEntity): Long

    /** Permite sembrar datos demo solo cuando la tabla está realmente vacía. */
    @Query("SELECT COUNT(*) FROM flushings")
    suspend fun count(): Int
}
