package cl.ariztia.bebederos.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("bebederos_settings")

/**
 * Preferencias pequeñas persistidas con DataStore.
 *
 * Se almacenan la presentación ampliada del origen de datos y la habilitación de avisos locales;
 * los registros de flushing pertenecen a Room y no a este almacén.
 */
class SettingsStore(private val context: Context) {
    private val offlineKey = booleanPreferencesKey("offline_mode")
    private val notificationsKey = booleanPreferencesKey("critical_notifications")
    private val demoFlushingsSeededKey = booleanPreferencesKey("demo_flushings_seeded")

    /** Flujo que vuelve a emitir cuando cambia la preferencia visual de origen de datos. */
    val offline: Flow<Boolean> =
        context.dataStore.data.map { it[offlineKey] ?: false }

    /** Flujo de preferencia para avisos locales demostrativos. */
    val notifications: Flow<Boolean> =
        context.dataStore.data.map { it[notificationsKey] ?: true }

    suspend fun setOffline(value: Boolean) {
        context.dataStore.edit { it[offlineKey] = value }
    }

    suspend fun setNotifications(value: Boolean) {
        context.dataStore.edit { it[notificationsKey] = value }
    }

    /** Distingue el primer sembrado de una tabla vaciada intencionalmente por el usuario. */
    suspend fun wereDemoFlushingsSeeded(): Boolean =
        context.dataStore.data.first()[demoFlushingsSeededKey] ?: false

    suspend fun markDemoFlushingsSeeded() {
        context.dataStore.edit { it[demoFlushingsSeededKey] = true }
    }
}
