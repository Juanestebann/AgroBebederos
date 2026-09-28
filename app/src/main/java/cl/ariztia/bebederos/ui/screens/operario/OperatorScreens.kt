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
                Text("Hola, ${user?.name ?: "Juan P茅rez"} 馃憢", fontSize = 21.sp, fontWeight = FontWeight.Bold)
                Text("Granja El Sauce 路 Turno ma帽ana", fontSize = 12.sp, color = TextSecondary)
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
                            Text("Temperatura cr铆tica", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text("L铆nea 3 路 Galp贸n B 路 28.9 掳C", color = TextSecondary, fontSize = 11.sp)
                        }
                        Text("Ver", color = BrandRed, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                SectionCard {
                    Text("Estado general 路 Granja El Sauce", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.weight(1f)) { SummaryTile((counts[TemperatureStatus.NORMAL] ?: 0).toString(), "Normales", Normal, NormalBg) }
                        Box(Modifier.weight(1f)) { SummaryTile((counts[TemperatureStatus.WARNING] ?: 0).toString(), "Advertencia", Warning, WarningBg) }
                        Box(Modifier.weight(1f)) { SummaryTile((counts[TemperatureStatus.CRITICAL] ?: 0).toString(), "Cr铆ticas", Critical, CriticalBg) }
                    }
                }
                Text("Alertas activas", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                viewModel.alerts.take(2).forEach { alert -> AlertCompact(viewModel, alert) { onNavigate(Routes.LineDetail) } }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.weight(1f)) { QuickAction(Icons.Default.List, "Consultar l铆neas", BrandRed) { onNavigate(Routes.Lines) } }
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
            Text("${line.name} 路 ${shed.name}", color = color, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
            Text("${alert.temperature} 掳C 路 ${alert.time}", fontSize = 11.sp, color = TextSecondary)
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
        topBar = { AppTopBar("L铆neas de bebederos", if (supervisorMode) "${shed.name} 路 ${farm.name}" else "Granja El Sauce") },
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
                                Text("${line.temperature}掳C", color = color, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                                Text("${viewModel.shed(line.shedId)?.name ?: "Galp贸n"} 路 Act. ${line.updatedAt}", fontSize = 11.sp, color = TextSecondary)
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
        topBar = { AppTopBar(line.name, "${shed?.name} 路 ${farm?.name}", onBack) },
        bottomBar = { if (!supervisorMode) OperatorBottomBar(Routes.Lines, onNavigate) }
    ) { inner ->
        Column(Modifier.fillMaxSize().padding(inner).background(Color.White).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            SectionCard {
                Box(Modifier.size(150.dp).align(Alignment.CenterHorizontally).background(bg, CircleShape).border(5.dp, color, CircleShape), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${line.temperature}掳C", color = color, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                        Icon(Icons.Default.DeviceThermostat, null, tint = color)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Box(Modifier.align(Alignment.CenterHorizontally)) { StatusBadge(line.status) }
                Text("脷ltima actualizaci贸n: 23/09/2026 路 ${line.updatedAt}", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
            }
            SectionCard {
                InfoRow("Granja", farm?.name.orEmpty()); InfoRow("Galp贸n", shed?.name.orEmpty()); InfoRow("L铆nea", line.name); InfoRow("Estado", line.status.name)
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