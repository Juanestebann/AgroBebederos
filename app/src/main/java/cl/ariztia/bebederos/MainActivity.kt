package cl.ariztia.bebederos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import cl.ariztia.bebederos.ui.navigation.Routes
import cl.ariztia.bebederos.ui.screens.operario.AlertsOperarioScreen
import cl.ariztia.bebederos.ui.screens.operario.FlushingOkScreen
import cl.ariztia.bebederos.ui.screens.operario.FlushingScreen
import cl.ariztia.bebederos.ui.screens.operario.HomeOperarioScreen
import cl.ariztia.bebederos.ui.screens.operario.LineDetailScreen
import cl.ariztia.bebederos.ui.screens.operario.LinesScreen
import cl.ariztia.bebederos.ui.theme.BebederosTheme
import cl.ariztia.bebederos.viewmodel.AppViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            BebederosTheme {
                OperatorTestApp()
            }
        }
    }
}

@Composable
private fun OperatorTestApp() {

    val navController = rememberNavController()
    val appViewModel: AppViewModel = viewModel()

    /*
     * Login automático TEMPORAL para poder probar
     * registerFlushing(), porque AppViewModel exige
     * que exista un currentUser.
     */
    androidx.compose.runtime.LaunchedEffect(Unit) {
        appViewModel.updateEmail("operario@ariztia.cl")
        appViewModel.updatePassword("Demo123")
        appViewModel.login()
    }

    NavHost(
        navController = navController,
        startDestination = Routes.HomeOperario
    ) {

        composable(Routes.HomeOperario) {
            HomeOperarioScreen(
                viewModel = appViewModel,
                onNavigate = { route ->
                    navController.navigate(route)
                }
            )
        }

        composable(Routes.Lines) {
            LinesScreen(
                viewModel = appViewModel,
                supervisorMode = false,
                onNavigate = { route ->
                    navController.navigate(route)
                }
            )
        }

        composable(Routes.LineDetail) {
            LineDetailScreen(
                viewModel = appViewModel,
                supervisorMode = false,
                onBack = {
                    navController.popBackStack()
                },
                onNavigate = { route ->
                    navController.navigate(route)
                }
            )
        }

        composable(Routes.Flushing) {
            FlushingScreen(
                viewModel = appViewModel,
                onBack = {
                    navController.popBackStack()
                },
                onSuccess = {
                    navController.navigate(Routes.FlushingOk) {
                        popUpTo(Routes.Flushing) {
                            inclusive = true
                        }
                    }
                }
            )
        }

        composable(Routes.FlushingOk) {
            FlushingOkScreen(
                onHome = {
                    navController.navigate(Routes.HomeOperario) {
                        popUpTo(Routes.HomeOperario) {
                            inclusive = true
                        }
                    }
                }
            )
        }

        composable(Routes.AlertsOperario) {
            AlertsOperarioScreen(
                viewModel = appViewModel,
                onNavigate = { route ->
                    navController.navigate(route)
                }
            )
        }

        /*
         * El historial pertenece a la integración posterior.
         * Dejamos un destino temporal para que no crashee.
         */
        composable(Routes.History) {
            Text("Historial: pendiente de integración del Supervisor")
        }
    }
}