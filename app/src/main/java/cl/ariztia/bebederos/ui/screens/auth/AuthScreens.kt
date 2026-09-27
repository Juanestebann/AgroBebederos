package cl.ariztia.bebederos.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.ariztia.bebederos.data.model.DemoUser
import cl.ariztia.bebederos.ui.theme.*
import cl.ariztia.bebederos.viewmodel.AppViewModel
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onDone: () -> Unit) {
    LaunchedEffect(Unit) { delay(1300); onDone() }

    Column(
        Modifier
            .fillMaxSize()
            .background(BrandRed)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = Color.White.copy(alpha = .14f)
        ) {
            Box(
                Modifier.size(112.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.WaterDrop,
                    null,
                    tint = Color.White,
                    modifier = Modifier.size(64.dp)
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        Text(
            "Ariztía",
            color = Color.White,
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            "CONTROL DE BEBEDEROS",
            color = Color.White.copy(alpha = .78f),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(Modifier.height(48.dp))

        Text(
            "Monitoreo de temperatura · Líneas de bebederos",
            color = Color.White.copy(alpha = .72f),
            fontSize = 12.sp
        )
    }
}

@Composable
fun LoginScreen(
    viewModel: AppViewModel,
    onLoggedIn: (DemoUser) -> Unit
) {
    val state by viewModel.login.collectAsState()
    var showPassword by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .background(AppBackground)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(top = 42.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Ariztía",
                color = BrandRed,
                fontSize = 25.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                "CONTROL DE BEBEDEROS",
                color = TextSecondary,
                fontSize = 11.sp
            )
        }

        Column(
            Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "Bienvenido",
                fontSize = 25.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                "Monitorea la temperatura de los bebederos desde tu celular.",
                color = TextSecondary,
                fontSize = 13.sp
            )

            Spacer(Modifier.height(24.dp))

            OutlinedTextField(
                value = state.email,
                onValueChange = viewModel::updateEmail,
                label = { Text("Correo electrónico") },
                placeholder = { Text("usuario@ariztia.com") },
                isError = state.emailError != null,
                supportingText = {
                    state.emailError?.let { Text(it) }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(Modifier.height(10.dp))

            OutlinedTextField(
                value = state.password,
                onValueChange = viewModel::updatePassword,
                label = { Text("Contraseña") },
                isError = state.passwordError != null,
                supportingText = {
                    state.passwordError?.let { Text(it) }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation =
                    if (showPassword)
                        VisualTransformation.None
                    else
                        PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(
                        onClick = {
                            showPassword = !showPassword
                        }
                    ) {
                        Icon(
                            if (showPassword)
                                Icons.Default.VisibilityOff
                            else
                                Icons.Default.Visibility,
                            null
                        )
                    }
                }
            )

            TextButton(onClick = {}) {
                Text("¿Olvidaste tu contraseña?")
            }

            Button(
                onClick = {
                    viewModel.login()?.let(onLoggedIn)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    "Iniciar sesión",
                    fontWeight = FontWeight.SemiBold
                )
            }

            state.credentialsError?.let {
                Spacer(Modifier.height(10.dp))

                Text(
                    it,
                    color = Critical,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            CriticalBg,
                            RoundedCornerShape(10.dp)
                        )
                        .padding(10.dp),
                    fontSize = 12.sp
                )
            }

            Spacer(Modifier.height(18.dp))

            Text(
                "Demo Operario: operario@ariztia.cl / Demo123",
                fontSize = 11.sp,
                color = TextSecondary
            )

            Text(
                "Demo Supervisor: supervisor@ariztia.cl / Demo123",
                fontSize = 11.sp,
                color = TextSecondary
            )
        }
    }
}