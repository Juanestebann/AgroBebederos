package cl.ariztia.bebederos

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import cl.ariztia.bebederos.data.model.TemperatureStatus
import cl.ariztia.bebederos.ui.navigation.AppNavGraph
import cl.ariztia.bebederos.ui.theme.BebederosTheme
import cl.ariztia.bebederos.util.NotificationHelper
import cl.ariztia.bebederos.viewmodel.AppViewModel

/**
 * Punto de entrada Android que instala el árbol Compose y conecta servicios del sistema.
 *
 * La actividad crea un único [AppViewModel], observa sus `StateFlow` como estado Compose y entrega
 * callbacks de permisos/ajustes al grafo. Una nueva emisión provoca recomposición de los lectores;
 * no se recrea manualmente cada pantalla.
 */
class MainActivity : ComponentActivity() {

    private val notificationAvailability = mutableStateOf(false)
    private var pendingNotificationPermissionResult: ((Boolean) -> Unit)? = null

    private val notificationPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) {
            refreshNotificationAvailability()
            pendingNotificationPermissionResult?.invoke(notificationAvailability.value)
            pendingNotificationPermissionResult = null
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        NotificationHelper.createChannel(this)
        refreshNotificationAvailability()

        setContent {

            BebederosTheme {

                val navController = rememberNavController()

                val appViewModel: AppViewModel = viewModel()

                // collectAsState convierte el flujo de sesión en estado observable por Compose.
                val currentUser by
                appViewModel.currentUser.collectAsState()

                val criticalNotificationsEnabled by
                appViewModel.criticalNotifications.collectAsState()
                val notificationsAvailable = notificationAvailability.value

                // El efecto se reinicia solo si cambia alguna clave y publica un aviso local demo.
                LaunchedEffect(
                    currentUser,
                    criticalNotificationsEnabled,
                    notificationsAvailable
                ) {
                    if (
                        currentUser != null &&
                        criticalNotificationsEnabled &&
                        notificationsAvailable
                    ) {
                        val criticalLine =
                            appViewModel.lines.firstOrNull { line ->
                                line.status == TemperatureStatus.CRITICAL
                            }

                        if (criticalLine != null) {
                            NotificationHelper.showCriticalOnce(
                                this@MainActivity,
                                criticalLine
                            )
                        }
                    }
                }

                AppNavGraph(
                    navController = navController,
                    viewModel = appViewModel,
                    notificationsAvailable = notificationsAvailable,
                    onRequestNotificationPermission = ::requestNotificationPermission,
                    onOpenNotificationSettings = ::openNotificationSettings
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshNotificationAvailability()
    }

    private fun refreshNotificationAvailability() {
        notificationAvailability.value =
            NotificationHelper.canPostCriticalNotifications(this)
    }

    private fun requestNotificationPermission(onResult: (Boolean) -> Unit) {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            pendingNotificationPermissionResult = onResult
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            refreshNotificationAvailability()
            onResult(notificationAvailability.value)
        }
    }

    private fun openNotificationSettings() {
        startActivity(
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            }
        )
    }
}
