package cl.ariztia.bebederos.ui.screens.shared

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import cl.ariztia.bebederos.data.model.UserRole
import cl.ariztia.bebederos.ui.components.AppTopBar
import cl.ariztia.bebederos.ui.navigation.*
import cl.ariztia.bebederos.ui.theme.*
import cl.ariztia.bebederos.viewmodel.AppViewModel

@Composable
fun ProfileScreen(
    viewModel: AppViewModel,
    role: UserRole,
    onNavigate: (String) -> Unit,
    onLogout: () -> Unit
) {
    val user by viewModel.currentUser.collectAsState()
    val offline by viewModel.offline.collectAsState()
    val notifications by viewModel.criticalNotifications.collectAsState()
    val isOperator = role == UserRole.OPERARIO
    val initials = (user?.name ?: if (isOperator) "Juan Pérez" else "Carlos Morales")
        .split(" ")
        .take(2)
        .joinToString("") { it.take(1) }

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
                    user?.name ?: if (isOperator) "Juan Pérez" else "Carlos Morales",
                    fontWeight = FontWeight.Bold
                )

                Text(
                    if (isOperator) "Operario" else "Supervisor",
                    color = TextSecondary,
                    fontSize = 12.sp
                )

                Text(
                    user?.email ?: if (isOperator)
                        "j.perez@ariztia.cl"
                    else
                        "c.morales@ariztia.cl",
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
                    "Mis datos"
                )

                ProfileRow(
                    Icons.Default.Info,
                    "Acerca de la app"
                )

                SettingCard(
                    Icons.Default.Notifications,
                    "Notificaciones",
                    "Alertas de temperatura crítica",
                    notifications
                ) {
                    viewModel.setCriticalNotifications(it)
                }

                SettingCard(
                    Icons.Default.CloudOff,
                    "Modo sin conexión",
                    "Guardar datos recientes en el dispositivo.",
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
    label: String
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
