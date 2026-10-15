package cl.ariztia.bebederos.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import cl.ariztia.bebederos.data.model.UserRole
import cl.ariztia.bebederos.ui.screens.auth.*
import cl.ariztia.bebederos.ui.screens.operario.*
import cl.ariztia.bebederos.ui.screens.shared.ProfileScreen
import cl.ariztia.bebederos.ui.screens.supervisor.*
import cl.ariztia.bebederos.ui.theme.AppBackground
import cl.ariztia.bebederos.viewmodel.AppViewModel

/**
 * Grafo raíz de navegación y frontera de autorización por rol.
 *
 * Splash y Login quedan fuera de las barras. Cada pantalla autenticada crea como máximo un
 * `Scaffold`; el grafo solo compone el destino actual y protege rutas incompatibles. Las lambdas
 * `go`, `openSecondary` y `onBack` centralizan navegación, `launchSingleTop` evita duplicados y
 * `popUpTo(0)` elimina pantallas protegidas después del logout.
 */
@Composable
fun AppNavGraph(
    navController: NavHostController,
    viewModel: AppViewModel,
    notificationsAvailable: Boolean,
    onRequestNotificationPermission: ((Boolean) -> Unit) -> Unit,
    onOpenNotificationSettings: () -> Unit
) {
    val user by viewModel.currentUser.collectAsState()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route
    val isSupervisor = user?.role == UserRole.SUPERVISOR
    val homeRoute = if (isSupervisor) Routes.HomeSupervisor else Routes.HomeOperario
    val bottomRoutes = if (isSupervisor) {
        setOf(
            Routes.HomeSupervisor,
            Routes.Farms,
            Routes.History,
            Routes.AlertsSupervisor,
            Routes.ProfileSupervisor
        )
    } else {
        setOf(
            Routes.HomeOperario,
            Routes.Lines,
            Routes.AlertsOperario,
            Routes.ProfileOperario
        )
    }
    val allowedRoutes = if (isSupervisor) {
        setOf(
            Routes.HomeSupervisor,
            Routes.Farms,
            Routes.Sheds,
            Routes.Lines,
            Routes.LineDetail,
            Routes.History,
            Routes.FlushingTrack,
            Routes.AlertsSupervisor,
            Routes.ProfileSupervisor
        )
    } else {
        setOf(
            Routes.HomeOperario,
            Routes.Lines,
            Routes.LineDetail,
            Routes.History,
            Routes.Flushing,
            Routes.FlushingEdit,
            Routes.FlushingOk,
            Routes.AlertsOperario,
            Routes.ProfileOperario
        )
    }

    // Reacciona a cambios de sesión o destino sin ejecutar navegación durante la composición.
    LaunchedEffect(user, currentRoute) {
        if (currentRoute != null && currentRoute != Routes.Splash && currentRoute != Routes.Login) {
            when {
                user == null -> navController.navigate(Routes.Login) {
                    popUpTo(0)
                    launchSingleTop = true
                }

                currentRoute !in allowedRoutes -> navController.navigate(homeRoute) {
                    popUpTo(0)
                    launchSingleTop = true
                }
            }
        }
    }

    // Las pestañas vuelven a la raíz del rol para no acumular copias en el back stack.
    fun go(route: String) {
        if (route == currentRoute) return
        if (route == Routes.Flushing) {
            viewModel.prepareFlushingRegistration()
        }
        navController.navigate(route) {
            launchSingleTop = true
            if (route in bottomRoutes) {
                popUpTo(homeRoute) {
                    inclusive = false
                }
            }
        }
    }

    // Los destinos jerárquicos conservan su padre para que la flecha Atrás siga siendo útil.
    fun openSecondary(route: String) {
        if (route == Routes.Flushing) {
            viewModel.prepareFlushingRegistration()
        }
        navController.navigate(route) {
            launchSingleTop = true
        }
    }

    Box(Modifier.fillMaxSize()) {
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
                    onNavigate = ::go,
                    onBack = {
                        navController.popBackStack()
                    }
                )
            }

            composable(Routes.LineDetail) {
                LineDetailScreen(
                    viewModel,
                    supervisorMode = isSupervisor,
                    onBack = {
                        navController.popBackStack()
                    },
                    onNavigate = ::openSecondary
                )
            }

            composable(Routes.Flushing) {
                FlushingScreen(
                    viewModel,
                    editMode = false,
                    onBack = {
                        navController.popBackStack()
                    },
                    onSuccess = {
                        navController.navigate(Routes.FlushingOk) {
                            popUpTo(Routes.Flushing) {
                                inclusive = true
                            }
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable(Routes.FlushingEdit) {
                FlushingScreen(
                    viewModel,
                    editMode = true,
                    onBack = {
                        navController.popBackStack()
                    },
                    onSuccess = {
                        navController.popBackStack()
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
                        launchSingleTop = true
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
                    ::go,
                    notificationsAvailable,
                    onRequestNotificationPermission,
                    onOpenNotificationSettings
                ) {
                    viewModel.logout()

                    navController.navigate(
                        Routes.Login
                    ) {
                        popUpTo(0)
                        launchSingleTop = true
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
                val hasDetailParent =
                    navController.previousBackStackEntry?.destination?.route == Routes.LineDetail

                HistoryScreen(
                    viewModel,
                    supervisorMode = isSupervisor,
                    onNavigate = ::go,
                    onBack = if (hasDetailParent) {
                        { navController.popBackStack() }
                    } else {
                        null
                    }
                )
            }

            composable(Routes.FlushingTrack) {
                FlushingTrackScreen(
                    viewModel,
                    onNavigate = ::go,
                    onBack = {
                        navController.popBackStack()
                    }
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
                    ::go,
                    notificationsAvailable,
                    onRequestNotificationPermission,
                    onOpenNotificationSettings
                ) {
                    viewModel.logout()

                    navController.navigate(
                        Routes.Login
                    ) {
                        popUpTo(0)
                        launchSingleTop = true
                    }
                }
            }
        }

        if (
            user == null &&
            currentRoute != null &&
            currentRoute != Routes.Splash &&
            currentRoute != Routes.Login
        ) {
            Box(
                Modifier
                    .matchParentSize()
                    .background(AppBackground)
            )
        }
    }
}
