package cl.ariztia.bebederos.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

private data class NavItem(val label: String, val icon: ImageVector, val route: String)

@Composable
fun OperatorBottomBar(currentRoute: String, onNavigate: (String) -> Unit) {
    val items = listOf(
        NavItem("Inicio", Icons.Default.Home, Routes.HomeOperario),
        NavItem("Líneas", Icons.AutoMirrored.Filled.List, Routes.Lines),
        NavItem("Alertas", Icons.Default.Notifications, Routes.AlertsOperario),
        NavItem("Perfil", Icons.Default.Person, Routes.ProfileOperario)
    )
    NavigationBar { items.forEach { item ->
        NavigationBarItem(
            selected = currentRoute == item.route,
            onClick = { onNavigate(item.route) },
            icon = { Icon(item.icon, null) },
            label = { Text(item.label) }
        )
    } }
}

@Composable
fun SupervisorBottomBar(currentRoute: String, onNavigate: (String) -> Unit) {
    val items = listOf(
        NavItem("Inicio", Icons.Default.Home, Routes.HomeSupervisor),
        NavItem("Granjas", Icons.Default.Agriculture, Routes.Farms),
        NavItem("Historial", Icons.Default.History, Routes.History),
        NavItem("Alertas", Icons.Default.Notifications, Routes.AlertsSupervisor),
        NavItem("Perfil", Icons.Default.Person, Routes.ProfileSupervisor)
    )
    NavigationBar { items.forEach { item ->
        NavigationBarItem(
            selected = currentRoute == item.route,
            onClick = { onNavigate(item.route) },
            icon = { Icon(item.icon, null) },
            label = { Text(item.label) }
        )
    } }
}
