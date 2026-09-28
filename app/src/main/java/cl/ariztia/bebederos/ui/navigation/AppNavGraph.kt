package cl.ariztia.bebederos.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import cl.ariztia.bebederos.data.model.UserRole
import cl.ariztia.bebederos.ui.screens.auth.*
import cl.ariztia.bebederos.ui.screens.operario.*
import cl.ariztia.bebederos.ui.screens.shared.ProfileScreen
import cl.ariztia.bebederos.ui.screens.supervisor.*
import cl.ariztia.bebederos.viewmodel.AppViewModel

@Composable
fun AppNavGraph(
    navController: NavHostController,
    viewModel: AppViewModel
) {
    val user by viewModel.currentUser.collectAsState()
    val isSupervisor = user?.role == UserRole.SUPERVISOR

    fun go(route: String) {
        navController.navigate(route) {
            launchSingleTop = true
        }
    }

    NavHost(
        navController = navController,
        startDestination = Routes.Splash
    ) {

        composable(Routes.Splash) {
            SplashScreen {
                navController.navigate(Routes.Login) {
                    popUpTo(Routes.Splash) {
                        inclusive = true
                    }
                }
            }
        }

        composable(Routes.Login) {
            LoginScreen(viewModel) { logged ->

                val destination =
                    if (logged.role == UserRole.SUPERVISOR) {
                        Routes.HomeSupervisor
                    } else {
                        Routes.HomeOperario
                    }

                navController.navigate(destination) {
                    popUpTo(Routes.Login) {
                        inclusive = true
                    }
                }
            }
        }

        composable(Routes.HomeOperario) {
            HomeOperarioScreen(
                viewModel,
                ::go
            )
        }

        composable(Routes.Lines) {
            LinesScreen(
                viewModel,
                supervisorMode = isSupervisor,
                onNavigate = ::go
            )
        }

        composable(Routes.LineDetail) {
            LineDetailScreen(
                viewModel,
                supervisorMode = isSupervisor,
                onBack = {
                    navController.popBackStack()
                },
                onNavigate = ::go
            )
        }

        composable(Routes.Flushing) {
            FlushingScreen(
                viewModel,
                onBack = {
                    navController.popBackStack()
                },
                onSuccess = {
                    go(Routes.FlushingOk)
                }
            )
        }

        composable(Routes.FlushingOk) {
            FlushingOkScreen {
                navController.navigate(
                    Routes.HomeOperario
                ) {
                    popUpTo(
                        Routes.HomeOperario
                    ) {
                        inclusive = false
                    }
                }
            }
        }

        composable(Routes.AlertsOperario) {
            AlertsOperarioScreen(
                viewModel,
                ::go
            )
        }

        composable(Routes.ProfileOperario) {
            ProfileScreen(
                viewModel,
                UserRole.OPERARIO,
                ::go
            ) {
                viewModel.logout()

                navController.navigate(
                    Routes.Login
                ) {
                    popUpTo(0)
                }
            }
        }

        composable(Routes.HomeSupervisor) {
            HomeSupervisorScreen(
                viewModel,
                ::go
            )
        }

        composable(Routes.Farms) {
            FarmsScreen(
                viewModel,
                ::go
            )
        }

        composable(Routes.Sheds) {
            ShedsScreen(
                viewModel,
                onBack = {
                    navController.popBackStack()
                },
                onNavigate = ::go
            )
        }

        composable(Routes.History) {
            HistoryScreen(
                viewModel,
                supervisorMode = isSupervisor,
                onNavigate = ::go
            )
        }

        composable(Routes.FlushingTrack) {
            FlushingTrackScreen(
                viewModel,
                ::go
            )
        }

        composable(Routes.AlertsSupervisor) {
            AlertsSupervisorScreen(
                viewModel,
                ::go
            )
        }

        composable(Routes.ProfileSupervisor) {
            ProfileScreen(
                viewModel,
                UserRole.SUPERVISOR,
                ::go
            ) {
                viewModel.logout()

                navController.navigate(
                    Routes.Login
                ) {
                    popUpTo(0)
                }
            }
        }
    }
}