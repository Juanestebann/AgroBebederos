package cl.ariztia.bebederos.ui.screens.operario

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import cl.ariztia.bebederos.data.model.*
import cl.ariztia.bebederos.ui.components.*
import cl.ariztia.bebederos.ui.navigation.OperatorBottomBar
import cl.ariztia.bebederos.ui.navigation.Routes
import cl.ariztia.bebederos.ui.theme.*
import cl.ariztia.bebederos.viewmodel.AppViewModel
import java.io.File

@Composable
fun HomeOperarioScreen(viewModel: AppViewModel, onNavigate: (String) -> Unit) {
    val offline by viewModel.offline.collectAsState()
    val notifications by viewModel.criticalNotifications.collectAsState()
    val user by viewModel.currentUser.collectAsState()
    val farmLines = viewModel.linesForFarm(1)
    val counts = farmLines.groupingBy { it.status }.eachCount()

    Scaffold(bottomBar = { OperatorBottomBar(Routes.HomeOperario, onNavigate) }) { inner ->
        Column(Modifier.fillMaxSize().padding(inner).background(Color.White)) {
            Column(Modifier.fillMaxWidth().padding(20.dp)) {
                Text("Operario", color = BrandRed, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Text("Hola, ${user?.name ?: "Juan Pérez"} 👋", fontSize = 21.sp, fontWeight = FontWeight.Bold)
                Text("Granja El Sauce · Turno mañana", fontSize = 12.sp, color = TextSecondary)
            }
            OfflineBanner(offline)
            if (notifications) {
                Card(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp).clickable {
                        viewModel.selectFarm(1); viewModel.selectShed(2); viewModel.selectLine(3); onNavigate(Routes.LineDetail)
                    },
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFFFCDD2)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, null, tint = Critical)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Temperatura crítica", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text("Línea 3 · Galpón B · 28.9 °C", color = TextSecondary, fontSize = 11.sp)
                        }
                        Text("Ver", color = BrandRed, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                SectionCard {
                    Text("Estado general · Granja El Sauce", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.weight(1f)) { SummaryTile((counts[TemperatureStatus.NORMAL] ?: 0).toString(), "Normales", Normal, NormalBg) }
                        Box(Modifier.weight(1f)) { SummaryTile((counts[TemperatureStatus.WARNING] ?: 0).toString(), "Advertencia", Warning, WarningBg) }
                        Box(Modifier.weight(1f)) { SummaryTile((counts[TemperatureStatus.CRITICAL] ?: 0).toString(), "Críticas", Critical, CriticalBg) }
                    }
                }
                Text("Alertas activas", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                viewModel.alerts.take(2).forEach { alert -> AlertCompact(viewModel, alert) { onNavigate(Routes.LineDetail) } }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.weight(1f)) { QuickAction(Icons.Default.List, "Consultar líneas", BrandRed) { onNavigate(Routes.Lines) } }
                    Box(Modifier.weight(1f)) { QuickAction(Icons.Default.Notifications, "Ver alertas", Critical) { onNavigate(Routes.AlertsOperario) } }
                    Box(Modifier.weight(1f)) { QuickAction(Icons.Default.WaterDrop, "Registrar flushing", Normal) {
                        viewModel.selectFarm(1); viewModel.selectShed(2); viewModel.selectLine(3); onNavigate(Routes.Flushing)
                    } }
                }
            }
        }
    }
}

@Composable
private fun AlertCompact(viewModel: AppViewModel, alert: Alert, onClick: () -> Unit) {
    val line = viewModel.line(alert.lineId) ?: return
    val shed = viewModel.shed(alert.shedId) ?: return
    val (color, bg) = statusColors(alert.status)
    Row(
        Modifier.fillMaxWidth().background(bg, RoundedCornerShape(12.dp)).clickable {
            viewModel.selectFarm(alert.farmId); viewModel.selectShed(alert.shedId); viewModel.selectLine(alert.lineId); onClick()
        }.padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(if (alert.status == TemperatureStatus.CRITICAL) Icons.Default.Warning else Icons.Default.Notifications, null, tint = color)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text("${line.name} · ${shed.name}", color = color, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
            Text("${alert.temperature} °C · ${alert.time}", fontSize = 11.sp, color = TextSecondary)
        }
        StatusBadge(alert.status)
    }
}

@Composable
fun LinesScreen(viewModel: AppViewModel, supervisorMode: Boolean, onNavigate: (String) -> Unit) {
    val offline by viewModel.offline.collectAsState()
    val shed = viewModel.selectedShed()
    val farm = viewModel.selectedFarm()
    val data = if (supervisorMode) viewModel.linesForSelectedShed() else viewModel.linesForFarm(1)

    Scaffold(
        topBar = { AppTopBar("Líneas de bebederos", if (supervisorMode) "${shed.name} · ${farm.name}" else "Granja El Sauce") },
        bottomBar = { if (!supervisorMode) OperatorBottomBar(Routes.Lines, onNavigate) }
    ) { inner ->
        Column(Modifier.fillMaxSize().padding(inner).background(Color.White)) {
            OfflineBanner(offline)
            LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(data.size) { index ->
                    val line = data[index]
                    val (color, _) = statusColors(line.status)
                    Card(
                        Modifier.fillMaxWidth().clickable {
                            viewModel.selectFarm(line.farmId); viewModel.selectShed(line.shedId); viewModel.selectLine(line.id); onNavigate(Routes.LineDetail)
                        },
                        shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, Border)
                    ) {
                        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.width(4.dp).height(62.dp).background(color, RoundedCornerShape(4.dp)))
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(line.name, fontWeight = FontWeight.SemiBold)
                                    StatusBadge(line.status)
                                }
                                Spacer(Modifier.height(6.dp))
                                Text("${line.temperature}°C", color = color, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                                Text("${viewModel.shed(line.shedId)?.name ?: "Galpón"} · Act. ${line.updatedAt}", fontSize = 11.sp, color = TextSecondary)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LineDetailScreen(viewModel: AppViewModel, supervisorMode: Boolean, onBack: () -> Unit, onNavigate: (String) -> Unit) {
    val line = viewModel.selectedLine()
    val farm = viewModel.farm(line.farmId)
    val shed = viewModel.shed(line.shedId)
    val (color, bg) = statusColors(line.status)
    Scaffold(
        topBar = { AppTopBar(line.name, "${shed?.name} · ${farm?.name}", onBack) },
        bottomBar = { if (!supervisorMode) OperatorBottomBar(Routes.Lines, onNavigate) }
    ) { inner ->
        Column(Modifier.fillMaxSize().padding(inner).background(Color.White).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            SectionCard {
                Box(Modifier.size(150.dp).align(Alignment.CenterHorizontally).background(bg, CircleShape).border(5.dp, color, CircleShape), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${line.temperature}°C", color = color, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                        Icon(Icons.Default.DeviceThermostat, null, tint = color)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Box(Modifier.align(Alignment.CenterHorizontally)) { StatusBadge(line.status) }
                Text("Última actualización: 23/09/2026 · ${line.updatedAt}", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
            }
            SectionCard {
                InfoRow("Granja", farm?.name.orEmpty()); InfoRow("Galpón", shed?.name.orEmpty()); InfoRow("Línea", line.name); InfoRow("Estado", line.status.name)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = { onNavigate(Routes.History) }, modifier = Modifier.weight(1f)) { Icon(Icons.Default.History, null); Spacer(Modifier.width(6.dp)); Text("Ver historial") }
                if (!supervisorMode) Button(onClick = { onNavigate(Routes.Flushing) }, modifier = Modifier.weight(1f)) { Icon(Icons.Default.WaterDrop, null); Spacer(Modifier.width(6.dp)); Text("Registrar flushing") }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 12.sp, color = TextSecondary)
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

private fun createImageUri(context: Context): Uri {
    val dir = File(context.cacheDir, "images").apply { mkdirs() }
    val file = File(dir, "flushing_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlushingScreen(viewModel: AppViewModel, onBack: () -> Unit, onSuccess: () -> Unit) {
    val context = LocalContext.current
    val line = viewModel.selectedLine(); val shed = viewModel.selectedShed(); val farm = viewModel.selectedFarm()
    var reason by remember { mutableStateOf("") }
    var observation by remember { mutableStateOf("") }
    var reasonError by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    var pendingUri by remember { mutableStateOf<Uri?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success -> if (success) photoUri = pendingUri }

    Scaffold(topBar = { AppTopBar("Registrar Flushing", onBack = onBack) }) { inner ->
        Column(Modifier.fillMaxSize().padding(inner).background(Color.White).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            listOf("Granja" to farm.name, "Galpón" to shed.name, "Línea" to line.name, "Fecha" to "23/09/2026", "Hora" to "14:38").forEach { (label, value) ->
                Text(label, fontSize = 12.sp, color = TextSecondary)
                Text(value, modifier = Modifier.fillMaxWidth().background(BrandRedLight, RoundedCornerShape(12.dp)).padding(12.dp), color = BrandRed, fontWeight = FontWeight.Medium, fontSize = 13.sp)
            }
            Text("Motivo del flushing *", fontSize = 12.sp, color = TextSecondary)
            Box {
                OutlinedTextField(
                    value = reason,
                    onValueChange = {},
                    readOnly = true,
                    isError = reasonError,
                    placeholder = { Text("Selecciona un motivo") },
                    trailingIcon = { IconButton(onClick = { expanded = true }) { Icon(Icons.Default.ArrowDropDown, null) } },
                    modifier = Modifier.fillMaxWidth().clickable { expanded = true }
                )
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    listOf("Temperatura alta", "Mantenimiento preventivo", "Limpieza", "Otro").forEach { option ->
                        DropdownMenuItem(text = { Text(option) }, onClick = { reason = option; reasonError = false; expanded = false })
                    }
                }
            }
            if (reasonError) Text("Selecciona un motivo.", color = Critical, fontSize = 12.sp)
            SectionCard {
                Text("Evidencia fotográfica", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Text("Adjunta una fotografía del flushing realizado.", fontSize = 11.sp, color = TextSecondary)
                Spacer(Modifier.height(8.dp))
                if (photoUri == null) {
                    OutlinedButton(onClick = { pendingUri = createImageUri(context); launcher.launch(pendingUri) }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.CameraAlt, null); Spacer(Modifier.width(6.dp)); Text("Tomar fotografía")
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, null, tint = Normal)
                        Spacer(Modifier.width(8.dp)); Text("Evidencia adjuntada", color = Normal, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        TextButton(onClick = { pendingUri = createImageUri(context); launcher.launch(pendingUri) }) { Text("Repetir") }
                        TextButton(onClick = { photoUri = null }) { Text("Eliminar", color = Critical) }
                    }
                }
            }
            OutlinedTextField(observation, { observation = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Observación") }, placeholder = { Text("Ej: Flushing realizado por temperatura crítica...") }, minLines = 3)
            Button(onClick = {
                if (reason.isBlank()) reasonError = true else viewModel.registerFlushing(reason, observation, photoUri?.toString(), onSuccess)
            }, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("Registrar flushing") }
        }
    }
}

@Composable
fun FlushingOkScreen(onHome: () -> Unit) {
    Column(Modifier.fillMaxSize().background(AppBackground).padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box(Modifier.size(96.dp).background(NormalBg, CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Default.Check, null, tint = Normal, modifier = Modifier.size(48.dp)) }
        Spacer(Modifier.height(18.dp)); Text("Flushing registrado correctamente", fontWeight = FontWeight.Bold, fontSize = 18.sp); Text("El registro se guardó correctamente.", color = TextSecondary, fontSize = 13.sp)
        Spacer(Modifier.height(24.dp)); Button(onClick = onHome) { Text("Volver al inicio") }
    }
}

@Composable
fun AlertsOperarioScreen(viewModel: AppViewModel, onNavigate: (String) -> Unit) {
    val offline by viewModel.offline.collectAsState()
    Scaffold(topBar = { AppTopBar("Alertas") }, bottomBar = { OperatorBottomBar(Routes.AlertsOperario, onNavigate) }) { inner ->
        Column(Modifier.fillMaxSize().padding(inner).background(Color.White)) {
            OfflineBanner(offline)
            LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(viewModel.alerts.size) { i ->
                    val alert = viewModel.alerts[i]; val line = viewModel.line(alert.lineId)!!; val shed = viewModel.shed(alert.shedId)!!; val farm = viewModel.farm(alert.farmId)!!; val (color, bg) = statusColors(alert.status)
                    Card(colors = CardDefaults.cardColors(containerColor = bg), border = BorderStroke(1.dp, color), shape = RoundedCornerShape(16.dp)) {
                        Column(Modifier.padding(14.dp)) {
                            Row { Column(Modifier.weight(1f)) { Text("${line.name} · ${shed.name}", color = color, fontWeight = FontWeight.SemiBold); Text("${farm.name} · ${alert.date} ${alert.time}", fontSize = 11.sp, color = TextSecondary) }; Column(horizontalAlignment = Alignment.End) { Text("${alert.temperature}°C", color = color, fontWeight = FontWeight.Bold, fontSize = 20.sp); StatusBadge(alert.status) } }
                            Spacer(Modifier.height(8.dp))
                            Button(onClick = { viewModel.selectFarm(alert.farmId); viewModel.selectShed(alert.shedId); viewModel.selectLine(alert.lineId); onNavigate(Routes.LineDetail) }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = color)) { Text("Ver detalle") }
                        }
                    }
                }
            }
        }
    }
}
