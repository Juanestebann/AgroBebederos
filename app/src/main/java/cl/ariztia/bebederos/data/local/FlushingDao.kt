package cl.ariztia.bebederos.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
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

    /** Inserta el conjunto demo en una única operación Room. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<FlushingEntity>): List<Long>

    /** Recupera la versión persistida que debe usarse para autorizar una mutación. */
    @Query("SELECT * FROM flushings WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): FlushingEntity?

    /** Actualiza por clave primaria y devuelve el número real de filas afectadas. */
    @Update
    suspend fun update(entity: FlushingEntity): Int

    /** Elimina por clave primaria y devuelve el número real de filas afectadas. */
    @Delete
    suspend fun delete(entity: FlushingEntity): Int

    /** Permite sembrar datos demo solo cuando la tabla está realmente vacía. */
    @Query("SELECT COUNT(*) FROM flushings")
    suspend fun count(): Int

    /** Evita borrar un archivo que todavía está referenciado por otro registro. */
    @Query("SELECT COUNT(*) FROM flushings WHERE photoUri = :photoUri")
    suspend fun countPhotoReferences(photoUri: String): Int
}
