package cl.ariztia.bebederos.ui.screens.shared

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.ariztia.bebederos.data.model.DemoUser
import cl.ariztia.bebederos.data.model.UserRole
import cl.ariztia.bebederos.ui.components.AppTopBar
import cl.ariztia.bebederos.ui.navigation.*
import cl.ariztia.bebederos.ui.theme.*
import cl.ariztia.bebederos.viewmodel.AppViewModel

/** Texto de alcance derivado del usuario autenticado, nunca de un nombre fijo de pantalla. */
internal data class ProfileAccessInfo(val label: String, val value: String)

/** Presenta una granja para operario y acceso global para supervisor. */
internal fun profileAccessInfo(user: DemoUser): ProfileAccessInfo = when (user.role) {
    UserRole.OPERARIO -> ProfileAccessInfo("Granja asignada", user.farm)
    UserRole.SUPERVISOR -> ProfileAccessInfo("Acceso a granjas", "Todas las granjas")
}

/**
 * Perfil autenticado y preferencias persistidas.
 *
 * `collectAsState()` hace que cambios de sesión o DataStore recompongan los controles. Los diálogos
 * se guardan con `remember` porque son estado visual temporal. El callback [onLogout] permite que el
 * grafo limpie el back stack después de que el ViewModel borre el estado de sesión.
 */
@Composable
fun ProfileScreen(
    viewModel: AppViewModel,
    role: UserRole,
    onNavigate: (String) -> Unit,
    notificationsAvailable: Boolean,
    onRequestNotificationPermission: ((Boolean) -> Unit) -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onLogout: () -> Unit
) {
    val user by viewModel.currentUser.collectAsState()
    val offline by viewModel.offline.collectAsState()
    val notifications by viewModel.criticalNotifications.collectAsState()
    val isOperator = role == UserRole.OPERARIO
    val displayName = user?.name ?: "Usuario no disponible"
    val initials = displayName
        .split(" ")
        .take(2)
        .joinToString("") { it.take(1) }
    var openDialog by remember { mutableStateOf<ProfileDialog?>(null) }
    var showNotificationBlockedDialog by remember { mutableStateOf(false) }

    if (showNotificationBlockedDialog) {
        AlertDialog(
            onDismissRequest = { showNotificationBlockedDialog = false },
            icon = { Icon(Icons.Default.NotificationsOff, contentDescription = null) },
            title = { Text("Notificaciones no disponibles") },
            text = {
                Text(
                    "Android tiene bloqueados los avisos de AgroBebederos. " +
                            "Puedes habilitarlos desde los ajustes del sistema."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showNotificationBlockedDialog = false
                    onOpenNotificationSettings()
                }) {
                    Text("Abrir ajustes")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNotificationBlockedDialog = false }) {
                    Text("Cerrar")
                }
            }
        )
    }

    when (openDialog) {
        ProfileDialog.PERSONAL_DATA -> AlertDialog(
            onDismissRequest = { openDialog = null },
            icon = { Icon(Icons.Default.Person, contentDescription = null) },
            title = { Text("Mis datos") },
            text = {
                if (user == null) {
                    Text("No hay información de usuario disponible. Vuelve a iniciar sesión.")
                } else {
                    val activeUser = requireNotNull(user)
                    val accessInfo = profileAccessInfo(activeUser)
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        ProfileInfo("Nombre", activeUser.name)
                        ProfileInfo("Correo", activeUser.email)
                        ProfileInfo(
                            "Rol",
                            if (activeUser.role == UserRole.OPERARIO) "Operario" else "Supervisor"
                        )
                        ProfileInfo(accessInfo.label, accessInfo.value)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { openDialog = null }) { Text("Cerrar") }
            }
        )

        ProfileDialog.ABOUT -> AlertDialog(
            onDismissRequest = { openDialog = null },
            icon = { Icon(Icons.Default.Info, contentDescription = null) },
            title = { Text("Acerca de AgroBebederos") },
            text = {
                Text(
                    "AgroBebederos es una aplicación académica y demostrativa para consultar " +
                            "temperaturas, alertas e historiales de líneas de bebederos, y registrar " +
                            "flushing con evidencia fotográfica. Utiliza datos demo y almacenamiento " +
                            "local; no está conectada a un servicio remoto."
                )
            },
            confirmButton = {
                TextButton(onClick = { openDialog = null }) { Text("Cerrar") }
            }
        )

        null -> Unit
    }

    Scaffold(
        topBar = { AppTopBar("Mi perfil") },
        bottomBar = {
            if (isOperator) {
                OperatorBottomBar(Routes.ProfileOperario, onNavigate)
            } else {
                SupervisorBottomBar(Routes.ProfileSupervisor, onNavigate)
            }
        }
    ) { inner ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(inner)
                .background(Color.White)
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    Modifier
                        .size(82.dp)
                        .background(BrandRed, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        initials,
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(Modifier.height(8.dp))

                Text(
                    displayName,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    user?.role?.let {
                        if (it == UserRole.OPERARIO) "Operario" else "Supervisor"
                    } ?: "Sin sesión activa",
                    color = TextSecondary,
                    fontSize = 12.sp
                )

                Text(
                    user?.email ?: "Información no disponible",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }

            Column(
                Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ProfileRow(
                    Icons.Default.Person,
                    "Mis datos",
                    onClick = { openDialog = ProfileDialog.PERSONAL_DATA }
                )

                ProfileRow(
                    Icons.Default.Info,
                    "Acerca de la app",
                    onClick = { openDialog = ProfileDialog.ABOUT }
                )

                SettingCard(
                    Icons.Default.Notifications,
                    "Notificaciones",
                    when {
                        notifications && !notificationsAvailable ->
                            "Avisos bloqueados por Android. Toca para habilitarlos."
                        notifications -> "Avisos locales de alertas demostrativas; no provienen de sensores."
                        else -> "Avisos desactivados en la aplicación."
                    },
                    notifications && notificationsAvailable
                ) { enabled ->
                    if (!enabled) {
                        viewModel.setCriticalNotifications(false)
                    } else if (notificationsAvailable) {
                        viewModel.setCriticalNotifications(true)
                    } else {
                        onRequestNotificationPermission { granted ->
                            if (granted) {
                                viewModel.setCriticalNotifications(true)
                            } else {
                                showNotificationBlockedDialog = true
                            }
                        }
                    }
                }

                SettingCard(
                    Icons.Default.Storage,
                    "Detalle sobre origen de datos",
                    "Ampliar la leyenda de datos demo y registros locales persistidos.",
                    offline
                ) {
                    viewModel.setOffline(it)
                }

                Button(
                    onClick = onLogout,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CriticalBg,
                        contentColor = Critical
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Cerrar sesión")
                }
            }
        }
    }
}

@Composable
private fun ProfileRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Border),
        color = Color.White
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = BrandRed)
            Spacer(Modifier.width(12.dp))
            Text(
                label,
                modifier = Modifier.weight(1f),
                fontSize = 13.sp
            )
            Icon(
                Icons.Default.ChevronRight,
                null,
                tint = TextSecondary
            )
        }
    }
}

private enum class ProfileDialog {
    PERSONAL_DATA,
    ABOUT
}

@Composable
private fun ProfileInfo(label: String, value: String) {
    Column {
        Text(label, color = TextSecondary, fontSize = 11.sp)
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun SettingCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onChecked: (Boolean) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Border),
        color = Color.White
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = BrandRed)
            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    subtitle,
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }

            Switch(
                checked = checked,
                onCheckedChange = onChecked
            )
        }
    }
}
