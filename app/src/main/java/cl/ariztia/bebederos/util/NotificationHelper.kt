package cl.ariztia.bebederos.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import cl.ariztia.bebederos.data.model.WaterLine

object NotificationHelper {
    private const val CHANNEL_ID = "critical_temperature"

    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Alertas críticas de temperatura",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Avisos cuando una línea alcanza temperatura crítica"
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun showCritical(context: Context, line: WaterLine) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle("Temperatura crítica")
            .setContentText("${line.name}: ${line.temperature} °C. Requiere revisión inmediata.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(line.id, notification)
        } catch (_: SecurityException) {
            // Android 13+ requiere permiso POST_NOTIFICATIONS concedido por el usuario.
        }
    }
}
