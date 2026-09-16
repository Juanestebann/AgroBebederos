package com.example.bebederosapp.agrobebederos.ui.theme

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.bebederosapp.agrobebederos.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {

    // Scaffold entrega una estructura general para la pantalla.
    Scaffold(

        // Barra superior de la aplicación.
        topBar = {
            TopAppBar(
                title = {
                    Text("AgroBebederos")
                }
            )
        }

    ) { innerPadding ->

        // Column ordena los elementos de arriba hacia abajo.
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp),

            // Agrega un espacio uniforme de 20 dp entre los elementos.
            verticalArrangement = Arrangement.spacedBy(20.dp),

            // Centra horizontalmente todos los elementos.
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // Título de bienvenida.
            Text(
                text = "Monitoreo de temperatura",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary
            )

            // Nuevo elemento visual de texto.
            Text(
                text = "Control de líneas de bebederos",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground
            )

            // Botón que utilizará una acción más adelante.
            Button(
                onClick = {
                    // Acción futura.
                },

                // Usa el color principal definido por MaterialTheme.
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("Ver líneas")
            }

            // Imagen almacenada dentro de res/drawable.
            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "Imagen de gatito",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentScale = ContentScale.Fit
            )
        }
    }
}


// Vista previa de la pantalla sin ejecutar el emulador.
@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    HomeScreen()
}