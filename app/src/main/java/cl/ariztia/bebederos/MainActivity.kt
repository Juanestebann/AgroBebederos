package cl.ariztia.bebederos

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import cl.ariztia.bebederos.data.model.TemperatureStatus
import cl.ariztia.bebederos.ui.navigation.AppNavGraph
import cl.ariztia.bebederos.ui.theme.BebederosTheme
import cl.ariztia.bebederos.util.NotificationHelper
import cl.ariztia.bebederos.viewmodel.AppViewModel

class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        NotificationHelper.createChannel(this)

        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(
                Manifest.permission.POST_NOTIFICATIONS
            )
        }

        setContent {

            BebederosTheme {

                val navController = rememberNavController()

                val appViewModel: AppViewModel = viewModel()

                val currentUser by
                appViewModel.currentUser.collectAsState()

                val criticalNotificationsEnabled by
                appViewModel.criticalNotifications.collectAsState()

                LaunchedEffect(
                    currentUser,
                    criticalNotificationsEnabled
                ) {
                    if (
                        currentUser != null &&
                        criticalNotificationsEnabled
                    ) {
                        val criticalLine =
                            appViewModel.lines.firstOrNull { line ->
                                line.status == TemperatureStatus.CRITICAL
                            }

                        if (criticalLine != null) {
                            NotificationHelper.showCritical(
                                this@MainActivity,
                                criticalLine
                            )
                        }
                    }
                }

                AppNavGraph(
                    navController = navController,
                    viewModel = appViewModel
                )
            }
        }
    }
}