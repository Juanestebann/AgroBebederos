package cl.ariztia.bebederos.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("bebederos_settings")

class SettingsStore(private val context: Context) {
    private val offlineKey = booleanPreferencesKey("offline_mode")
    private val notificationsKey = booleanPreferencesKey("critical_notifications")

    val offline: Flow<Boolean> =
        context.dataStore.data.map { it[offlineKey] ?: false }

    val notifications: Flow<Boolean> =
        context.dataStore.data.map { it[notificationsKey] ?: true }

    suspend fun setOffline(value: Boolean) {
        context.dataStore.edit { it[offlineKey] = value }
    }

    suspend fun setNotifications(value: Boolean) {
        context.dataStore.edit { it[notificationsKey] = value }
    }
}