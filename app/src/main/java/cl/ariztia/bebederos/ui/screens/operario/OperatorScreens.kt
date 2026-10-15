package cl.ariztia.bebederos.ui.screens.operario

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import cl.ariztia.bebederos.data.model.*
import cl.ariztia.bebederos.ui.components.*
import cl.ariztia.bebederos.ui.navigation.OperatorBottomBar
import cl.ariztia.bebederos.ui.navigation.Routes
import cl.ariztia.bebederos.ui.navigation.SupervisorBottomBar
import cl.ariztia.bebederos.ui.theme.*
import cl.ariztia.bebederos.viewmodel.AppViewModel
import cl.ariztia.bebederos.viewmodel.MAX_FLUSHING_OBSERVATION_LENGTH
import kotlinx.coroutines.delay
import java.io.File
import java.io.IOException

/** Dashboard del operario, limitado a la granja obtenida de su sesión actual. */
@Composable
fun HomeOperarioScreen(viewModel: AppViewModel, onNavigate: (String) -> Unit) {
    val offline by viewModel.offline.collectAsState()
    val user by viewModel.currentUser.collectAsState()
    val effectiveLines by viewModel.effectiveLines.collectAsState()
    val assignedFarm = user?.farm?.let { farmName ->
        viewModel.farms.firstOrNull { it.name == farmName }
    }
    val farmAlerts = assignedFarm
        ?.let { farm -> viewModel.alerts.filter { it.farmId == farm.id } }
        .orEmpty()
    val farmLines = assignedFarm
        ?.let { farm -> effectiveLines.filter { it.farmId == farm.id } }
        .orEmpty()
    val criticalLine = farmLines.firstOrNull { it.status == TemperatureStatus.CRITICAL }
    val criticalShed = criticalLine?.let { viewModel.shed(it.shedId) }
    val flushingLine = criticalLine ?: farmLines.firstOrNull()
    val counts = farmLines.groupingBy { it.status }.eachCount()

    Scaffold(bottomBar = { OperatorBottomBar(Routes.HomeOperario, onNavigate) }) { inner ->
        Column(Modifier.fillMaxSize().padding(inner).background(Color.White)) {
            Column(Modifier.fillMaxWidth().padding(20.dp)) {
                Text("Operario", color = BrandRed, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Text("Hola, ${user?.name ?: "Usuario"} 👋", fontSize = 21.sp, fontWeight = FontWeight.Bold)
                Text(
                    assignedFarm?.name ?: "Granja no disponible",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
            LocalDataBanner(offline, viewModel.lines.firstOrNull()?.measurementDate)
            if (
                assignedFarm != null && criticalLine != null && criticalShed != null
            ) {
                Card(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp).clickable {
                        viewModel.selectLine(criticalLine.id)
                        onNavigate(Routes.LineDetail)
                    },
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFFFCDD2)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, null, tint = Critical)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Estado efectivo crítico", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text(
                                "${criticalLine.name} · ${criticalShed.name} · ${assignedFarm.name}",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                            Text(
                                "${criticalLine.temperature} °C · ${effectiveMeasurementTimestamp(criticalLine)}",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                        StatusBadge(criticalLine.status)
                    }
                }
            } else {
                Card(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Border),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.CheckCircle,
                            null,
                            tint = Normal
                        )
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                "Sin líneas críticas",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            Text(
                                if (assignedFarm == null) {
                                    "No se pudo identificar la granja asignada."
                                } else {
                                    "No hay estados efectivos críticos en la granja asignada."
                                },
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                SectionCard {
                    Text(
                        "Estado general · ${assignedFarm?.name ?: "Granja no disponible"}",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.weight(1f)) { SummaryTile((counts[TemperatureStatus.NORMAL] ?: 0).toString(), "Normales", Normal, NormalBg) }
                        Box(Modifier.weight(1f)) { SummaryTile((counts[TemperatureStatus.WARNING] ?: 0).toString(), "Advertencia", Warning, WarningBg) }
                        Box(Modifier.weight(1f)) { SummaryTile((counts[TemperatureStatus.CRITICAL] ?: 0).toString(), "Críticas", Critical, CriticalBg) }
                    }
                }
                Text("Alertas demostrativas", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                farmAlerts.take(2).forEach { alert -> AlertCompact(viewModel, alert) { onNavigate(Routes.LineDetail) } }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.weight(1f)) { QuickAction(Icons.AutoMirrored.Filled.List, "Consultar líneas", BrandRed) { onNavigate(Routes.Lines) } }
                    Box(Modifier.weight(1f)) { QuickAction(Icons.Default.Notifications, "Ver alertas", Critical) { onNavigate(Routes.AlertsOperario) } }
                    Box(Modifier.weight(1f)) { QuickAction(Icons.Default.WaterDrop, "Registrar flushing", Normal) {
                        flushingLine?.let {
                            viewModel.selectLine(it.id)
                            onNavigate(Routes.Flushing)
                        }
                    } }
                }
            }
        }
    }
}

@Composable
private fun AlertCompact(viewModel: AppViewModel, alert: Alert, onClick: () -> Unit) {
    val location = viewModel.resolveAlertLocation(alert)
    val (color, bg) = statusColors(alert.status)
    val modifier = Modifier
        .fillMaxWidth()
        .background(bg, RoundedCornerShape(12.dp))
        .let { base ->
            if (location == null) base else base.clickable {
                if (viewModel.selectAlertLocation(alert)) onClick()
            }
        }
        .padding(12.dp)
    Row(
        modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(if (alert.status == TemperatureStatus.CRITICAL) Icons.Default.Warning else Icons.Default.Notifications, null, tint = color)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                location?.let { "${it.line.name} · ${it.shed.name}" }
                    ?: "Ubicación no disponible",
                color = color,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp
            )
            Text(
                "${alert.temperature} °C · ${demoAlertTimestamp(alert.date, alert.time)}",
                fontSize = 11.sp,
                color = TextSecondary
            )
        }
        StatusBadge(alert.status)
    }
}

/** Lista líneas efectivas para el alcance del rol y conserva la jerarquía al abrir un detalle. */
@Composable
fun LinesScreen(
    viewModel: AppViewModel,
    supervisorMode: Boolean,
    onNavigate: (String) -> Unit,
    onBack: (() -> Unit)? = null
) {
    val offline by viewModel.offline.collectAsState()
    val user by viewModel.currentUser.collectAsState()
    val effectiveLines by viewModel.effectiveLines.collectAsState()
    val selectedShed = viewModel.selectedShed()
    val selectedFarm = viewModel.selectedFarm()
    val operatorFarm = user
        ?.takeIf { it.role == UserRole.OPERARIO }
        ?.farm
        ?.let { farmName -> viewModel.farms.firstOrNull { it.name == farmName } }
    val farm = if (supervisorMode) selectedFarm else operatorFarm
    val data = if (supervisorMode) {
        effectiveLines.filter {
            it.farmId == selectedFarm.id && it.shedId == selectedShed.id
        }
    } else {
        operatorFarm?.let { farm -> effectiveLines.filter { it.farmId == farm.id } }.orEmpty()
    }

    Scaffold(
        topBar = {
            AppTopBar(
                "Líneas de bebederos",
                if (supervisorMode) {
                    "${selectedShed.name} · ${selectedFarm.name}"
                } else {
                    farm?.name ?: "Granja no disponible"
                },
                onBack = if (supervisorMode) onBack else null
            )
        },
        bottomBar = {
            if (supervisorMode) {
                SupervisorBottomBar(Routes.Lines, onNavigate)
            } else {
                OperatorBottomBar(Routes.Lines, onNavigate)
            }
        }
    ) { inner ->
        Column(Modifier.fillMaxSize().padding(inner).background(Color.White)) {
            LocalDataBanner(offline, viewModel.lines.firstOrNull()?.measurementDate)
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
                                Text(
                                    "${viewModel.shed(line.shedId)?.name ?: "Galpón"} · ${effectiveMeasurementTimestamp(line)}",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Consulta de una línea seleccionada, con acceso al historial y al formulario según el rol. */
@Composable
fun LineDetailScreen(viewModel: AppViewModel, supervisorMode: Boolean, onBack: () -> Unit, onNavigate: (String) -> Unit) {
    val effectiveLines by viewModel.effectiveLines.collectAsState()
    val line = viewModel.selectedLine()
    val farm = viewModel.farm(line.farmId)
    val shed = viewModel.shed(line.shedId)
    val (color, bg) = statusColors(line.status)
    val ringWidth by animateDpAsState(
        targetValue = when (line.status) {
            TemperatureStatus.NORMAL -> 3.dp
            TemperatureStatus.WARNING -> 5.dp
            TemperatureStatus.CRITICAL -> 7.dp
        },
        animationSpec = tween(durationMillis = 220),
        label = "lineTemperatureRingWidth"
    )
    Scaffold(
        topBar = { AppTopBar(line.name, "${shed?.name} · ${farm?.name}", onBack) },
        bottomBar = {
            if (supervisorMode) {
                SupervisorBottomBar(Routes.LineDetail, onNavigate)
            } else {
                OperatorBottomBar(Routes.LineDetail, onNavigate)
            }
        }
    ) { inner ->
        Column(Modifier.fillMaxSize().padding(inner).background(Color.White).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            SectionCard {
                Box(Modifier.size(150.dp).align(Alignment.CenterHorizontally).background(bg, CircleShape).border(ringWidth, color, CircleShape), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${line.temperature}°C", color = color, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                        Icon(Icons.Default.DeviceThermostat, null, tint = color)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Box(Modifier.align(Alignment.CenterHorizontally)) { StatusBadge(line.status) }
                Text(
                    effectiveMeasurementTimestamp(line),
                    color = TextSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
            SectionCard {
                InfoRow("Granja", farm?.name.orEmpty()); InfoRow("Galpón", shed?.name.orEmpty()); InfoRow("Línea", line.name); InfoRow("Estado", line.status.displayLabel)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = { onNavigate(Routes.History) }, modifier = Modifier.weight(1f)) { Icon(Icons.Default.History, null); Spacer(Modifier.width(6.dp)); Text("Historial de flushing") }
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

@Composable
private fun TemperatureInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    error: String?,
    enabled: Boolean
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        placeholder = { Text("Ej: 21,5") },
        singleLine = true,
        enabled = enabled,
        isError = error != null,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        leadingIcon = {
            Icon(
                if (error == null) Icons.Default.DeviceThermostat else Icons.Default.Error,
                contentDescription = if (error == null) null else "Error de temperatura"
            )
        },
        supportingText = error?.let { message ->
            { Text(message) }
        }
    )
}

/** URI compartible con la cámara y archivo privado que respalda esa URI. */
private data class ImageCaptureTarget(
    val uri: Uri,
    val file: File
)

/** Prepara un archivo privado para FileProvider; lo elimina si no puede construir la URI. */
private fun createImageCaptureTarget(context: Context): ImageCaptureTarget {
    val directory = File(context.filesDir, "flushing_images")
    if (!directory.exists() && !directory.mkdirs()) {
        throw IOException("No fue posible crear el directorio de fotografías")
    }

    val file = File.createTempFile("flushing_", ".jpg", directory)
    return try {
        ImageCaptureTarget(
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            ),
            file
        )
    } catch (error: Exception) {
        file.delete()
        throw error
    }
}

private fun hasCameraApplication(context: Context): Boolean =
    Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        .resolveActivity(context.packageManager) != null

private fun deletePhoto(path: String?) {
    path?.let { runCatching { File(it).delete() } }
}

/**
 * Formulario de flushing del operario.
 *
 * Los textos y rutas usan `rememberSaveable` para sobrevivir recreaciones como una rotación; el
 * desplegable usa `remember` porque solo importa mientras vive la composición. La UI recoge sesión,
 * línea efectiva y estado de guardado desde el ViewModel. No persiste directamente: entrega los
 * valores a [AppViewModel.registerFlushing] y espera un ID confirmado antes de navegar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlushingScreen(
    viewModel: AppViewModel,
    editMode: Boolean,
    onBack: () -> Unit,
    onSuccess: () -> Unit
) {
    val context = LocalContext.current
    val user by viewModel.currentUser.collectAsState()
    val effectiveLines by viewModel.effectiveLines.collectAsState()
    val editingRecord by viewModel.editingFlushing.collectAsState()
    val saveState by viewModel.flushingSave.collectAsState()
    val editLocation = editingRecord?.let(viewModel::resolveFlushingLocation)

    if (editMode && (editingRecord == null || editLocation == null)) {
        Scaffold(topBar = { AppTopBar("Editar flushing", onBack = onBack) }) { inner ->
            Box(
                Modifier.fillMaxSize().padding(inner).padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No se encontró un flushing autorizado para editar. Vuelve al historial.",
                    color = Critical,
                    textAlign = TextAlign.Center
                )
            }
        }
        return
    }

    val line = if (editMode) {
        viewModel.line(editingRecord!!.lineId) ?: editLocation!!.line
    } else {
        viewModel.selectedLine()
    }
    val shed = if (editMode) editLocation!!.shed else viewModel.selectedShed()
    val farm = if (editMode) editLocation!!.farm else viewModel.selectedFarm()
    val formKey = if (editMode) "edit-${editingRecord!!.id}" else "create-${line.id}"
    var reason by rememberSaveable(formKey) { mutableStateOf(editingRecord?.reason.orEmpty()) }
    var observation by rememberSaveable(formKey) {
        mutableStateOf(editingRecord?.observation.orEmpty())
    }
    var beforeTemperature by rememberSaveable(formKey) {
        mutableStateOf(editingRecord?.beforeTemperatureC?.toString().orEmpty())
    }
    var afterTemperature by rememberSaveable(formKey) {
        mutableStateOf(editingRecord?.afterTemperatureC?.toString().orEmpty())
    }
    var expanded by remember { mutableStateOf(false) }
    var photoUri by rememberSaveable(formKey) { mutableStateOf(editingRecord?.photoUri) }
    var photoPath by rememberSaveable(formKey) { mutableStateOf<String?>(null) }
    var pendingUri by rememberSaveable(formKey) { mutableStateOf<String?>(null) }
    var pendingPath by rememberSaveable(formKey) { mutableStateOf<String?>(null) }
    var cameraMessage by rememberSaveable(formKey) { mutableStateOf<String?>(null) }
    var cameraMessageIsError by rememberSaveable(formKey) { mutableStateOf(false) }
    val savedRecordId = saveState.savedRecordId
    val updatedRecordId = saveState.updatedRecordId
    // El callback recibe el resultado de otra actividad y conserva solo fotografías no vacías.
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val capturedPath = pendingPath
        val capturedUri = pendingUri
        pendingPath = null
        pendingUri = null

        val capturedFile = capturedPath?.let(::File)
        if (
            success &&
            capturedUri != null &&
            capturedFile?.isFile == true &&
            capturedFile.length() > 0
        ) {
            deletePhoto(photoPath)
            photoPath = capturedPath
            photoUri = capturedUri
            cameraMessage = null
        } else {
            deletePhoto(capturedPath)
            cameraMessage = if (success) {
                "La fotografía no se guardó correctamente. Inténtalo nuevamente."
            } else {
                "Captura cancelada. Puedes intentarlo nuevamente."
            }
            cameraMessageIsError = success
        }
    }

    fun startCameraCapture() {
        cameraMessage = null
        if (!hasCameraApplication(context)) {
            cameraMessage = "No hay una aplicación de cámara disponible en este dispositivo."
            cameraMessageIsError = true
            return
        }

        val target = try {
            createImageCaptureTarget(context)
        } catch (_: Exception) {
            cameraMessage = "No fue posible preparar el archivo para la fotografía."
            cameraMessageIsError = true
            return
        }

        pendingPath = target.file.absolutePath
        pendingUri = target.uri.toString()
        try {
            launcher.launch(target.uri)
        } catch (_: Exception) {
            deletePhoto(pendingPath)
            pendingPath = null
            pendingUri = null
            cameraMessage = "No fue posible abrir la cámara. Inténtalo nuevamente."
            cameraMessageIsError = true
        }
    }

    fun leaveForm() {
        deletePhoto(pendingPath)
        deletePhoto(photoPath)
        pendingPath = null
        pendingUri = null
        photoPath = null
        photoUri = null
        if (editMode) viewModel.cancelFlushingEdit()
        onBack()
    }
    // produceState crea una vista previa reactiva; la hora definitiva se toma al construir el registro.
    val timestampPreview by produceState(initialValue = viewModel.currentFlushingTimestamp()) {
        while (true) {
            value = viewModel.currentFlushingTimestamp()
            delay(1_000)
        }
    }
    LaunchedEffect(photoPath, photoUri) {
        if (
            photoPath != null &&
            photoUri != null &&
            photoPath?.let { File(it).isFile && File(it).length() > 0 } != true
        ) {
            photoPath = null
            photoUri = null
            cameraMessage = "La fotografía seleccionada ya no está disponible. Puedes tomar otra."
            cameraMessageIsError = true
        }
    }
    // Este efecto se ejecuta al recibir el ID de Room y evita navegar antes de confirmar el guardado.
    LaunchedEffect(savedRecordId) {
        if (!editMode) savedRecordId?.let { recordId ->
            // El archivo ya pertenece al registro guardado y no debe eliminarse al salir.
            photoPath = null
            photoUri = null
            beforeTemperature = ""
            afterTemperature = ""
            onSuccess()
            viewModel.consumeFlushingSuccess(recordId)
        }
    }
    LaunchedEffect(updatedRecordId) {
        if (editMode) updatedRecordId?.let { recordId ->
            // La nueva fotografía, si existe, ya quedó asociada al registro actualizado.
            photoPath = null
            photoUri = null
            onSuccess()
            viewModel.consumeFlushingUpdateSuccess(recordId)
        }
    }
    // Atrás se bloquea durante la escritura para no interrumpir el resultado ni duplicar reintentos.
    BackHandler {
        if (!saveState.isSaving && savedRecordId == null && updatedRecordId == null) leaveForm()
    }

    Scaffold(topBar = {
        AppTopBar(
            if (editMode) "Editar flushing" else "Registrar Flushing",
            onBack = {
                if (!saveState.isSaving && savedRecordId == null && updatedRecordId == null) {
                    leaveForm()
                }
            }
        )
    }) { inner ->
        Column(Modifier.fillMaxSize().padding(inner).background(Color.White).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            listOf(
                "Granja" to farm.name,
                "Galpón" to shed.name,
                "Línea" to line.name,
                (if (editMode) "Responsable original" else "Responsable automático") to
                        (editingRecord?.operatorName ?: user?.name ?: "Sesión no disponible"),
                (if (editMode) "Fecha original" else "Fecha (vista previa)") to
                        (editingRecord?.date ?: timestampPreview.date),
                (if (editMode) "Hora original" else "Hora (vista previa)") to
                        (editingRecord?.time ?: timestampPreview.time)
            ).forEach { (label, value) ->
                Text(label, fontSize = 12.sp, color = TextSecondary)
                Text(value, modifier = Modifier.fillMaxWidth().background(BrandRedLight, RoundedCornerShape(12.dp)).padding(12.dp), color = BrandRed, fontWeight = FontWeight.Medium, fontSize = 13.sp)
            }
            Text("Motivo del flushing *", fontSize = 12.sp, color = TextSecondary)
            Box {
                OutlinedTextField(
                    value = reason,
                    onValueChange = {},
                    readOnly = true,
                    isError = saveState.reasonError != null,
                    placeholder = { Text("Selecciona un motivo") },
                    leadingIcon = {
                        if (saveState.reasonError != null) {
                            Icon(Icons.Default.Error, contentDescription = "Error en el motivo")
                        }
                    },
                    trailingIcon = {
                        IconButton(onClick = { expanded = true }) {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = "Seleccionar motivo")
                        }
                    },
                    modifier = Modifier.fillMaxWidth().clickable { expanded = true }
                )
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    listOf("Temperatura alta", "Mantenimiento preventivo", "Limpieza", "Otro").forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                reason = option
                                viewModel.clearFlushingValidationErrors()
                                expanded = false
                            }
                        )
                    }
                }
            }
            saveState.reasonError?.let {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Error,
                        contentDescription = null,
                        tint = Critical,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(it, color = Critical, fontSize = 12.sp)
                }
            }
            SectionCard {
                Text(
                    "Mediciones manuales obligatorias",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
                Text(
                    "Ingresa las temperaturas medidas por el operario. La lectura actual es solo una referencia y no completa estos campos.",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
                Text(
                    "Referencia actual: ${line.temperature} °C · ${effectiveMeasurementTimestamp(line)}",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
                Spacer(Modifier.height(8.dp))
                TemperatureInputField(
                    value = beforeTemperature,
                    onValueChange = {
                        beforeTemperature = it
                        viewModel.clearFlushingValidationErrors()
                    },
                    label = "Temperatura antes del flushing (°C)",
                    error = saveState.beforeTemperatureError,
                    enabled = !saveState.isSaving
                )
                Spacer(Modifier.height(8.dp))
                TemperatureInputField(
                    value = afterTemperature,
                    onValueChange = {
                        afterTemperature = it
                        viewModel.clearFlushingValidationErrors()
                    },
                    label = "Temperatura después del flushing (°C)",
                    error = saveState.afterTemperatureError,
                    enabled = !saveState.isSaving
                )
            }
            SectionCard {
                Text("Evidencia fotográfica", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Text("Adjunta una fotografía del flushing realizado.", fontSize = 11.sp, color = TextSecondary)
                Spacer(Modifier.height(8.dp))
                if (photoUri == null) {
                    OutlinedButton(
                        onClick = ::startCameraCapture,
                        enabled = !saveState.isSaving,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.CameraAlt, null); Spacer(Modifier.width(6.dp)); Text("Tomar fotografía")
                    }
                } else {
                    Column {
                        FlushingPhoto(
                            uri = photoUri.orEmpty(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(
                                onClick = ::startCameraCapture,
                                enabled = !saveState.isSaving
                            ) { Text("Repetir") }
                            TextButton(
                                onClick = {
                                    deletePhoto(photoPath)
                                    photoPath = null
                                    photoUri = null
                                    cameraMessage = null
                                },
                                enabled = !saveState.isSaving
                            ) { Text("Eliminar", color = Critical) }
                        }
                    }
                }
                cameraMessage?.let {
                    Text(
                        it,
                        color = if (cameraMessageIsError) Critical else TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }
            OutlinedTextField(
                value = observation,
                onValueChange = {
                    if (it.length <= MAX_FLUSHING_OBSERVATION_LENGTH) {
                        observation = it
                        viewModel.clearFlushingValidationErrors()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Observación (opcional)") },
                placeholder = { Text("Ej: Flushing realizado por temperatura crítica...") },
                minLines = 3,
                isError = saveState.observationError != null,
                leadingIcon = {
                    if (saveState.observationError != null) {
                        Icon(Icons.Default.Error, contentDescription = "Error en la observación")
                    }
                },
                supportingText = {
                    Text(
                        saveState.observationError
                            ?: "${observation.length}/$MAX_FLUSHING_OBSERVATION_LENGTH caracteres"
                    )
                }
            )
            saveState.errorMessage?.let {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Error,
                        contentDescription = "Error al guardar",
                        tint = Critical,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(it, color = Critical, fontSize = 12.sp)
                }
            }
            Button(onClick = {
                if (editMode) {
                    viewModel.updateFlushing(
                        reason,
                        observation,
                        photoUri,
                        beforeTemperature,
                        afterTemperature
                    )
                } else {
                    viewModel.registerFlushing(
                        reason,
                        observation,
                        photoUri,
                        beforeTemperature,
                        afterTemperature
                    )
                }
            }, enabled = !saveState.isSaving && savedRecordId == null && updatedRecordId == null, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                if (saveState.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Guardando...")
                } else {
                    Text(
                        when {
                            saveState.errorMessage != null -> "Reintentar"
                            editMode -> "Guardar cambios"
                            else -> "Registrar flushing"
                        }
                    )
                }
            }
            if (editMode) {
                OutlinedButton(
                    onClick = ::leaveForm,
                    enabled = !saveState.isSaving && updatedRecordId == null,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cancelar")
                }
            }
        }
    }
}

/** Confirmación posterior a una inserción local ya validada por Room. */
@Composable
fun FlushingOkScreen(onHome: () -> Unit) {
    BackHandler(onBack = onHome)
    var contentVisible by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        contentVisible = true
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(AppBackground)
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = contentVisible,
            enter = fadeIn(tween(durationMillis = 220)) +
                    scaleIn(tween(durationMillis = 220), initialScale = 0.92f)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier
                        .size(96.dp)
                        .background(NormalBg, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Check,
                        null,
                        tint = Normal,
                        modifier = Modifier.size(48.dp)
                    )
                }
                Spacer(Modifier.height(18.dp))
                Text(
                    "Flushing registrado correctamente",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Text(
                    "Las mediciones antes y después y los datos de la intervención se guardaron localmente.",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 4.dp),
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(24.dp))
                Button(onClick = onHome) {
                    Text("Volver al inicio")
                }
            }
        }
    }
}

/** Alertas demo de la granja del operario; solo una jerarquía válida habilita el detalle. */
@Composable
fun AlertsOperarioScreen(viewModel: AppViewModel, onNavigate: (String) -> Unit) {
    val offline by viewModel.offline.collectAsState()
    val user by viewModel.currentUser.collectAsState()
    val assignedFarm = user?.farm?.let { farmName ->
        viewModel.farms.firstOrNull { it.name == farmName }
    }
    val alerts = assignedFarm
        ?.let { farm -> viewModel.alerts.filter { it.farmId == farm.id } }
        .orEmpty()
    Scaffold(topBar = { AppTopBar("Alertas demostrativas") }, bottomBar = { OperatorBottomBar(Routes.AlertsOperario, onNavigate) }) { inner ->
        Column(Modifier.fillMaxSize().padding(inner).background(Color.White)) {
            LocalDataBanner(offline, viewModel.lines.firstOrNull()?.measurementDate)
            LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(alerts.size) { i ->
                    val alert = alerts[i]
                    val location = viewModel.resolveAlertLocation(alert)
                    val (color, bg) = statusColors(alert.status)
                    Card(colors = CardDefaults.cardColors(containerColor = bg), border = BorderStroke(1.dp, color), shape = RoundedCornerShape(16.dp)) {
                        Column(Modifier.padding(14.dp)) {
                            Row {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        location?.let { "${it.line.name} · ${it.shed.name}" }
                                            ?: "Ubicación no disponible",
                                        color = color,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        location?.let { "${it.farm.name} · ${demoAlertTimestamp(alert.date, alert.time)}" }
                                            ?: "Referencia: granja ${alert.farmId} · galpón ${alert.shedId} · línea ${alert.lineId}",
                                        fontSize = 11.sp,
                                        color = if (location == null) Critical else TextSecondary
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("${alert.temperature}°C", color = color, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                                    StatusBadge(alert.status)
                                }
                            }
                            if (location != null) {
                                Spacer(Modifier.height(8.dp))
                                Button(
                                    onClick = {
                                        if (viewModel.selectAlertLocation(alert)) {
                                            onNavigate(Routes.LineDetail)
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = color)
                                ) {
                                    Text("Ver detalle")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
