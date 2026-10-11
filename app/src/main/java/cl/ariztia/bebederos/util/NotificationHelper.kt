package cl.ariztia.bebederos.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import cl.ariztia.bebederos.MainActivity
import cl.ariztia.bebederos.data.model.TemperatureStatus
import cl.ariztia.bebederos.data.model.WaterLine
import cl.ariztia.bebederos.ui.components.demoNotificationText

/**
 * Gestiona avisos Android locales basados en líneas demostrativas críticas.
 *
 * No escucha sensores ni ejecuta monitoreo remoto. El conjunto en memoria evita repetir el mismo
 * aviso durante el proceso actual; la preferencia de DataStore y el permiso de Android se validan
 * antes de publicar.
 */
object NotificationHelper {
    private const val CHANNEL_ID = "critical_temperature"
    private val notifiedCriticalLines = mutableSetOf<Int>()

    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Avisos locales de datos demo",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Avisos locales basados en los datos demostrativos de la aplicación"
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    /** Comprueba permiso global, habilitación de la app y disponibilidad del canal. */
    fun canPostCriticalNotifications(context: Context): Boolean {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }

        val notificationManager = NotificationManagerCompat.from(context)
        if (!notificationManager.areNotificationsEnabled()) return false

        val channel = context
            .getSystemService(NotificationManager::class.java)
            .getNotificationChannel(CHANNEL_ID)

        return channel == null || channel.importance != NotificationManager.IMPORTANCE_NONE
    }

    /** Publica como máximo una vez por línea crítica en la ejecución actual. */
    @Synchronized
    fun showCriticalOnce(context: Context, line: WaterLine): Boolean {
        if (
            line.status != TemperatureStatus.CRITICAL ||
            line.id in notifiedCriticalLines ||
            !canPostCriticalNotifications(context)
        ) {
            return false
        }

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val contentIntent = PendingIntent.getActivity(
            context,
            line.id,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle("Alerta demostrativa de temperatura")
            .setContentText(
                demoNotificationText(
                    line.name,
                    line.temperature,
                    line.measurementDate,
                    line.updatedAt
                )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .build()

        return try {
            NotificationManagerCompat.from(context).notify(line.id, notification)
            notifiedCriticalLines += line.id
            true
        } catch (_: SecurityException) {
            false
        }
    }
}
