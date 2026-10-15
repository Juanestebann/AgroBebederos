package cl.ariztia.bebederos.ui.screens.supervisor

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.ariztia.bebederos.data.model.*
import cl.ariztia.bebederos.ui.components.*
import cl.ariztia.bebederos.ui.navigation.OperatorBottomBar
import cl.ariztia.bebederos.ui.navigation.Routes
import cl.ariztia.bebederos.ui.navigation.SupervisorBottomBar
import cl.ariztia.bebederos.ui.theme.*
import cl.ariztia.bebederos.viewmodel.AppViewModel
import cl.ariztia.bebederos.viewmodel.FlushingListUiState
import cl.ariztia.bebederos.viewmodel.records

/** Dashboard global del supervisor con conteos derivados y flushing observados desde Room. */
@Composable
fun HomeSupervisorScreen(viewModel: AppViewModel, onNavigate: (String) -> Unit) {
    val offline by viewModel.offline.collectAsState()
    val user by viewModel.currentUser.collectAsState()
    val flushingList by viewModel.flushingList.collectAsState()
    val effectiveLines by viewModel.effectiveLines.collectAsState()
    val relatedSheds = viewModel.farms.flatMap { viewModel.shedsForFarm(it.id) }
    val relatedLines = effectiveLines.filter { line ->
        viewModel.farms.any { it.id == line.farmId }
    }
    val statusCounts = relatedLines
        .groupingBy { it.status }
        .eachCount()

    Scaffold(
        bottomBar = {
            SupervisorBottomBar(
                Routes.HomeSupervisor,
                onNavigate
            )
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
                    .padding(20.dp)
            ) {
                Text(
                    "Supervisor",
                    color = BrandRed,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    user?.name ?: "Supervisor",
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    "Datos demostrativos · Todas las granjas",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }

            LocalDataBanner(offline, viewModel.lines.firstOrNull()?.measurementDate)

            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(Modifier.weight(1f)) {
                        SummaryTile(
                            viewModel.farms.size.toString(),
                            "Granjas",
                            BrandRed,
                            BrandRedLight
                        )
                    }

                    Box(Modifier.weight(1f)) {
                        SummaryTile(
                            relatedSheds.size.toString(),
                            "Galpones",
                            Color(0xFF6A1B9A),
                            Color(0xFFEDE7F6)
                        )
                    }

                    Box(Modifier.weight(1f)) {
                        SummaryTile(
                            relatedLines.size.toString(),
                            "Líneas",
                            Normal,
                            NormalBg
                        )
                    }
                }

                SectionCard {
                    Text(
                        "Resumen de líneas",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )

                    Spacer(Modifier.height(10.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(Modifier.weight(1f)) {
                            SummaryTile(
                                (statusCounts[TemperatureStatus.NORMAL] ?: 0).toString(),
                                "Normales",
                                Normal,
                                NormalBg
                            )
                        }

                        Box(Modifier.weight(1f)) {
                            SummaryTile(
                                (statusCounts[TemperatureStatus.WARNING] ?: 0).toString(),
                                "Advertencia",
                                Warning,
                                WarningBg
                            )
                        }

                        Box(Modifier.weight(1f)) {
                            SummaryTile(
                                (statusCounts[TemperatureStatus.CRITICAL] ?: 0).toString(),
                                "Críticas",
                                Critical,
                                CriticalBg
                            )
                        }
                    }
                }

                Text(
                    "Eventos críticos demostrativos",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )

                viewModel.alerts
                    .filter {
                        it.status == TemperatureStatus.CRITICAL
                    }
                    .forEach { alert ->

                        val location = viewModel.resolveAlertLocation(alert)

                        if (location == null) {
                            Text(
                                "No se pudo cargar la ubicación del evento crítico.",
                                color = Critical,
                                fontSize = 12.sp
                            )
                        } else Row(
                            Modifier
                                .fillMaxWidth()
                                .background(
                                    CriticalBg,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    if (viewModel.selectAlertLocation(alert)) {
                                        onNavigate(Routes.LineDetail)
                                    }
                                }
                                .padding(12.dp),
                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                null,
                                tint = Critical
                            )

                            Spacer(Modifier.width(8.dp))

                            Column {
                                Text(
                                    "${location.line.name} · ${location.shed.name} · ${location.farm.name}",
                                    color = Critical,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp
                                )

                                Text(
                                    "${alert.temperature}°C · ${demoAlertTimestamp(alert.date, alert.time)}",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    }

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.SpaceBetween,
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {
                    Text(
                        "Últimos flushing guardados",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )

                    TextButton(
                        onClick = {
                            onNavigate(
                                Routes.FlushingTrack
                            )
                        }
                    ) {
                        Text("Ver todos")
                    }
                }

                Text(
                    localRecordsDescription(),
                    color = TextSecondary,
                    fontSize = 11.sp
                )

                when (val state = flushingList) {
                    FlushingListUiState.Loading -> Text(
                        "Cargando registros de flushing…",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    FlushingListUiState.Error -> Text(
                        "No se pudieron cargar los registros de flushing.",
                        color = Critical,
                        fontSize = 12.sp
                    )
                    is FlushingListUiState.Ready -> {
                        if (state.records.isEmpty()) {
                            Text(
                                "No hay registros de flushing.",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        } else {
                            state.records.take(2).forEach { flushing ->
                                FlushingCard(viewModel, flushing)
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Entrada a la jerarquía de granjas; los conteos se derivan de relaciones validadas. */
@Composable
fun FarmsScreen(
    viewModel: AppViewModel,
    onNavigate: (String) -> Unit
) {
    val offline by
    viewModel.offline.collectAsState()
    val effectiveLines by viewModel.effectiveLines.collectAsState()

    Scaffold(
        topBar = {
            AppTopBar("Granjas")
        },
        bottomBar = {
            SupervisorBottomBar(
                Routes.Farms,
                onNavigate
            )
        }
    ) { inner ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(inner)
                .background(Color.White)
        ) {
            LocalDataBanner(offline, viewModel.lines.firstOrNull()?.measurementDate)

            LazyColumn(
                Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement =
                    Arrangement.spacedBy(12.dp)
            ) {
                items(
                    viewModel.farms.size
                ) { i ->
                    val farm =
                        viewModel.farms[i]

                    val lines = effectiveLines.filter { it.farmId == farm.id }

                    val counts =
                        lines
                            .groupingBy {
                                it.status
                            }
                            .eachCount()

                    Card(
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.selectFarm(
                                    farm.id
                                )

                                onNavigate(
                                    Routes.Sheds
                                )
                            },
                        shape =
                            RoundedCornerShape(
                                18.dp
                            ),
                        colors =
                            CardDefaults
                                .cardColors(
                                    containerColor =
                                        Color.White
                                ),
                        border =
                            BorderStroke(
                                1.dp,
                                Border
                            )
                    ) {
                        Column(
                            Modifier.padding(
                                16.dp
                            )
                        ) {
                            Row(
                                verticalAlignment =
                                    Alignment
                                        .CenterVertically
                            ) {
                                Icon(
                                    Icons.Default
                                        .Agriculture,
                                    null,
                                    tint = BrandRed
                                )

                                Spacer(
                                    Modifier.width(
                                        10.dp
                                    )
                                )

                                Column(
                                    Modifier.weight(
                                        1f
                                    )
                                ) {
                                    Text(
                                        farm.name,
                                        fontWeight =
                                            FontWeight
                                                .SemiBold
                                    )

                                    Text(
                                        "${viewModel.shedCountForFarm(farm)} galpones",
                                        fontSize =
                                            11.sp,
                                        color =
                                            TextSecondary
                                    )
                                }

                                Icon(
                                    Icons.Default
                                        .ChevronRight,
                                    null,
                                    tint =
                                        TextSecondary
                                )
                            }

                            Spacer(
                                Modifier.height(
                                    10.dp
                                )
                            )

                            Row(
                                horizontalArrangement =
                                    Arrangement
                                        .spacedBy(
                                            8.dp
                                        )
                            ) {
                                Box(
                                    Modifier.weight(
                                        1f
                                    )
                                ) {
                                    SummaryTile(
                                        (
                                                counts[
                                                    TemperatureStatus.NORMAL
                                                ] ?: 0
                                                ).toString(),
                                        "Normales",
                                        Normal,
                                        NormalBg
                                    )
                                }

                                Box(
                                    Modifier.weight(
                                        1f
                                    )
                                ) {
                                    SummaryTile(
                                        (
                                                counts[
                                                    TemperatureStatus.WARNING
                                                ] ?: 0
                                                ).toString(),
                                        "Advertencia",
                                        Warning,
                                        WarningBg
                                    )
                                }

                                Box(
                                    Modifier.weight(
                                        1f
                                    )
                                ) {
                                    SummaryTile(
                                        (
                                                counts[
                                                    TemperatureStatus.CRITICAL
                                                ] ?: 0
                                                ).toString(),
                                        "Críticas",
                                        Critical,
                                        CriticalBg
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Galpones de la granja seleccionada, sin mezclar relaciones de otras granjas. */
@Composable
fun ShedsScreen(
    viewModel: AppViewModel,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit
) {
    val offline by viewModel.offline.collectAsState()
    val effectiveLines by viewModel.effectiveLines.collectAsState()
    val farm =
        viewModel.selectedFarm()

    val sheds =
        viewModel.shedsForSelectedFarm()

    Scaffold(
        topBar = {
            AppTopBar(
                "Galpones",
                farm.name,
                onBack
            )
        },
        bottomBar = {
            SupervisorBottomBar(
                Routes.Farms,
                onNavigate
            )
        }
    ) { inner ->

        Column(
            Modifier
                .fillMaxSize()
                .padding(inner)
                .background(Color.White)
        ) {
            LocalDataBanner(offline, viewModel.lines.firstOrNull()?.measurementDate)

            LazyColumn(
                Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement =
                    Arrangement.spacedBy(12.dp)
            ) {
                items(sheds.size) { i ->

                    val shed =
                        sheds[i]

                    val lines = effectiveLines.filter {
                        it.farmId == farm.id && it.shedId == shed.id
                    }

                    val counts =
                        lines
                            .groupingBy {
                                it.status
                            }
                            .eachCount()

                    Card(
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.selectShed(
                                    shed.id
                                )

                                onNavigate(
                                    Routes.Lines
                                )
                            },
                        shape =
                            RoundedCornerShape(
                                18.dp
                            ),
                        colors =
                            CardDefaults.cardColors(
                                containerColor =
                                    Color.White
                            ),
                        border =
                            BorderStroke(
                                1.dp,
                                Border
                            )
                    ) {
                        Column(
                            Modifier.padding(
                                16.dp
                            )
                        ) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement =
                                    Arrangement
                                        .SpaceBetween
                            ) {
                                Text(
                                    shed.name,
                                    fontWeight =
                                        FontWeight
                                            .SemiBold
                                )

                                Text(
                                    "${lines.size} líneas",
                                    color =
                                        TextSecondary,
                                    fontSize =
                                        12.sp
                                )
                            }

                            Spacer(
                                Modifier.height(
                                    10.dp
                                )
                            )

                            Row(
                                horizontalArrangement =
                                    Arrangement.spacedBy(
                                        8.dp
                                    )
                            ) {
                                Box(
                                    Modifier.weight(
                                        1f
                                    )
                                ) {
                                    SummaryTile(
                                        (
                                                counts[
                                                    TemperatureStatus.NORMAL
                                                ] ?: 0
                                                ).toString(),
                                        "Normal",
                                        Normal,
                                        NormalBg
                                    )
                                }

                                Box(
                                    Modifier.weight(
                                        1f
                                    )
                                ) {
                                    SummaryTile(
                                        (
                                                counts[
                                                    TemperatureStatus.WARNING
                                                ] ?: 0
                                                ).toString(),
                                        "Advert.",
                                        Warning,
                                        WarningBg
                                    )
                                }

                                Box(
                                    Modifier.weight(
                                        1f
                                    )
                                ) {
                                    SummaryTile(
                                        (
                                                counts[
                                                    TemperatureStatus.CRITICAL
                                                ] ?: 0
                                                ).toString(),
                                        "Crítico",
                                        Critical,
                                        CriticalBg
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Historial de flushing de la línea seleccionada para cualquiera de los dos roles.
 *
 * `remember(flushingList, line)` recalcula el filtro cuando Room emite o cambia la selección. La
 * barra inferior depende del rol, mientras [onBack] conserva la jerarquía cuando existe un padre.
 */
@Composable
fun HistoryScreen(
    viewModel: AppViewModel,
    supervisorMode: Boolean,
    onNavigate: (String) -> Unit,
    onBack: (() -> Unit)? = null
) {
    val flushingList by viewModel.flushingList.collectAsState()
    val deleteState by viewModel.flushingDelete.collectAsState()
    val line = viewModel.selectedLine()
    val snackbarHostState = remember { SnackbarHostState() }
    var pendingDeletionId by remember { mutableStateOf<Long?>(null) }
    val lineFlushings = remember(flushingList, line) {
        viewModel.flushingsForLine(flushingList.records, line)
    }

    LaunchedEffect(deleteState.deletedRecordId) {
        deleteState.deletedRecordId?.let { deletedId ->
            pendingDeletionId = null
            viewModel.consumeFlushingDeleteSuccess(deletedId)
            snackbarHostState.showSnackbar("Flushing eliminado correctamente.")
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                "Historial de flushing",
                "${line.name} · ${viewModel.selectedShed().name}",
                onBack = onBack
            )
        },
        bottomBar = {
            if (supervisorMode) {
                SupervisorBottomBar(Routes.History, onNavigate)
            } else {
                OperatorBottomBar(Routes.History, onNavigate)
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { inner ->
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(inner)
                .background(Color.White)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SectionCard {
                    Text("Ubicación seleccionada", fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    Text(viewModel.selectedFarm().name, fontSize = 13.sp)
                    Text(
                        "${viewModel.selectedShed().name} · ${line.name}",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            item { LocalRecordsBanner() }

            when (flushingList) {
                FlushingListUiState.Loading -> item {
                    Column(
                        Modifier.fillParentMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(12.dp))
                        Text("Cargando historial de flushing…", color = TextSecondary)
                    }
                }
                FlushingListUiState.Error -> item {
                    Box(
                        Modifier.fillParentMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No se pudo cargar el historial de flushing.",
                            color = Critical
                        )
                    }
                }
                is FlushingListUiState.Ready -> {
                    if (lineFlushings.isEmpty()) {
                        item {
                            Box(
                                Modifier.fillParentMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Sin flushing registrados", color = TextSecondary)
                            }
                        }
                    } else {
                        items(lineFlushings, key = { it.id }) { flushing ->
                            val canManage = !supervisorMode &&
                                    viewModel.canCurrentUserManageFlushing(flushing)
                            FlushingCard(
                                viewModel = viewModel,
                                f = flushing,
                                showRegistrationDetails = true,
                                onEdit = if (canManage) {
                                    {
                                        if (viewModel.prepareFlushingEdit(flushing.id)) {
                                            onNavigate(Routes.FlushingEdit)
                                        }
                                    }
                                } else {
                                    null
                                },
                                onDelete = if (canManage) {
                                    {
                                        viewModel.clearFlushingDeleteState()
                                        pendingDeletionId = flushing.id
                                    }
                                } else {
                                    null
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    pendingDeletionId?.let { recordId ->
        val record = flushingList.records.firstOrNull { it.id == recordId }
        if (record != null) {
            val isDeleting = deleteState.deletingRecordId == recordId
            AlertDialog(
                onDismissRequest = {
                    if (!isDeleting) {
                        pendingDeletionId = null
                        viewModel.clearFlushingDeleteState()
                    }
                },
                icon = { Icon(Icons.Default.Delete, contentDescription = null) },
                title = { Text("Eliminar flushing") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "Se eliminará permanentemente el registro del ${record.date} a las ${record.time}."
                        )
                        Text("Motivo: ${record.reason}")
                        Text(
                            "La eliminación puede cambiar la temperatura efectiva mostrada para esta línea.",
                            color = TextSecondary
                        )
                        deleteState.errorMessage?.let {
                            Text(it, color = Critical, fontSize = 12.sp)
                        }
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            pendingDeletionId = null
                            viewModel.clearFlushingDeleteState()
                        },
                        enabled = !isDeleting
                    ) {
                        Text("Cancelar")
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { viewModel.deleteFlushing(recordId) },
                        enabled = !isDeleting,
                        colors = ButtonDefaults.buttonColors(containerColor = Critical)
                    ) {
                        if (isDeleting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Eliminar")
                        }
                    }
                }
            )
        } else if (deleteState.deletedRecordId == null && !deleteState.isDeleting) {
            LaunchedEffect(recordId) {
                pendingDeletionId = null
                viewModel.clearFlushingDeleteState()
                snackbarHostState.showSnackbar("El flushing ya no está disponible.")
            }
        }
    }
}

/** Seguimiento supervisor: distingue carga, error, lista vacía y registros disponibles. */
@Composable
fun FlushingTrackScreen(
    viewModel: AppViewModel,
    onNavigate: (String) -> Unit,
    onBack: () -> Unit
) {
    val flushingList by viewModel.flushingList.collectAsState()

    Scaffold(
        topBar = {
            AppTopBar("Seguimiento de flushing", onBack = onBack)
        },
        bottomBar = {
            SupervisorBottomBar(
                Routes.HomeSupervisor,
                onNavigate
            )
        }
    ) { inner ->

        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                LocalRecordsBanner()
            }

            when (val state = flushingList) {
                FlushingListUiState.Loading -> item {
                    Column(
                        Modifier.fillParentMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(12.dp))
                        Text("Cargando registros de flushing…", color = TextSecondary)
                    }
                }
                FlushingListUiState.Error -> item {
                    Box(
                        Modifier.fillParentMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No se pudieron cargar los registros de flushing.",
                            color = Critical
                        )
                    }
                }
                is FlushingListUiState.Ready -> {
                    if (state.records.isEmpty()) {
                        item {
                            Box(
                                Modifier.fillParentMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "No hay registros de flushing guardados.",
                                    color = TextSecondary
                                )
                            }
                        }
                    } else {
                        items(state.records, key = { it.id }) { flushing ->
                            FlushingCard(
                                viewModel,
                                flushing,
                                showRegistrationDetails = true
                            )
                        }
                    }
                }
            }
        }
    }
}
/**
 * Tarjeta reutilizada por dashboard, historial y seguimiento.
 *
 * La presentación distingue mediciones manuales antes/después, contexto automático heredado y
 * registros antiguos sin temperatura. Las referencias inválidas se muestran sin forzar nulls.
 */
@Composable
private fun FlushingCard(
    viewModel: AppViewModel,
    f: FlushingRecord,
    showRegistrationDetails: Boolean = false,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    val location = viewModel.resolveFlushingLocation(f)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        border = BorderStroke(
            1.dp,
            Border
        )
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {

                Icon(
                    if (f.completed) {
                        Icons.Default.CheckCircle
                    } else {
                        Icons.Default.Warning
                    },
                    null,
                    tint =
                        if (f.completed) {
                            Normal
                        } else {
                            Critical
                        }
                )

                Spacer(
                    Modifier.width(8.dp)
                )

                Column(
                    Modifier.weight(1f)
                ) {
                    Text(
                        location?.let { "${it.line.name} · ${it.shed.name}" }
                            ?: "Ubicación no disponible",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )

                    Text(
                        location?.farm?.name
                            ?: "Referencia guardada: granja ${f.farmId} · galpón ${f.shedId} · línea ${f.lineId}",
                        fontSize = 11.sp,
                        color = if (location == null) Critical else TextSecondary
                    )
                }

                Text(
                    if (f.completed) {
                        "Completado"
                    } else {
                        "Pendiente"
                    },
                    color =
                        if (f.completed) {
                            Normal
                        } else {
                            Critical
                        },
                    fontSize = 11.sp
                )
            }

            Spacer(
                Modifier.height(8.dp)
            )

            Text(
                "Operario: ${f.operatorName}",
                fontSize = 12.sp
            )

            Text(
                "${f.date} · ${f.time}",
                fontSize = 12.sp,
                color = TextSecondary
            )

            Text(
                "Motivo: ${f.reason}",
                fontSize = 12.sp,
                color = TextSecondary
            )

            if (f.observation.isNotBlank()) {
                Text(
                    "Observación: ${f.observation}",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            } else if (showRegistrationDetails) {
                Text(
                    "Observación: Sin observaciones",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }

            if (showRegistrationDetails) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Temperaturas del flushing",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(4.dp))
                if (f.beforeTemperatureC != null || f.afterTemperatureC != null) {
                    HistoricalTemperatureRow(
                        label = "Antes del flushing",
                        temperature = f.beforeTemperatureC
                    )
                    HistoricalTemperatureRow(
                        label = "Después del flushing",
                        temperature = f.afterTemperatureC
                    )
                } else if (f.temperatureAtRegistrationC != null) {
                    HistoricalTemperatureRow(
                        label = "Temperatura contextual automática heredada",
                        temperature = f.temperatureAtRegistrationC
                    )
                    Text(
                        "Esta lectura demostrativa no modifica el estado efectivo de la línea.",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                } else {
                    Text(
                        "Temperatura no capturada en este registro",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            } else {
                Spacer(Modifier.height(6.dp))
                CompactFlushingTemperatureSummary(f)
            }

            if (showRegistrationDetails && f.photoUri != null) {
                Spacer(Modifier.height(10.dp))
                Text(
                    "Fotografía del flushing",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(6.dp))
                FlushingPhoto(
                    uri = f.photoUri,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                )
            } else if (showRegistrationDetails) {
                Text(
                    "Fotografía: No adjuntada",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }

            if (onEdit != null || onDelete != null) {
                Spacer(Modifier.height(10.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    onEdit?.let {
                        OutlinedButton(onClick = it, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Edit, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Editar")
                        }
                    }
                    onDelete?.let {
                        OutlinedButton(onClick = it, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = Critical)
                            Spacer(Modifier.width(6.dp))
                            Text("Eliminar", color = Critical)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactFlushingTemperatureSummary(flushing: FlushingRecord) {
    val text = when {
        flushing.beforeTemperatureC != null || flushing.afterTemperatureC != null -> {
            val before = flushing.beforeTemperatureC?.let {
                "$it °C · ${flushing.beforeTemperatureStatus?.displayLabel}"
            } ?: "No capturada"
            val after = flushing.afterTemperatureC?.let {
                "$it °C · ${flushing.afterTemperatureStatus?.displayLabel}"
            } ?: "No capturada"
            "Antes: $before → Después: $after"
        }
        flushing.temperatureAtRegistrationC != null ->
            "Contexto automático heredado: ${flushing.temperatureAtRegistrationC} °C · " +
                    flushing.temperatureStatusAtRegistration?.displayLabel
        else -> "Sin temperatura registrada"
    }

    Text(
        text,
        fontSize = 11.sp,
        color = TextSecondary
    )
}

@Composable
private fun HistoricalTemperatureRow(label: String, temperature: Double?) {
    val validTemperature = temperature?.takeIf { it.isFinite() }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            if (validTemperature == null) "$label: No capturada" else "$label: $validTemperature °C",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (temperature != null && validTemperature == null) Critical else Color.Unspecified
        )
        validTemperature?.let { StatusBadge(temperatureStatusFor(it)) }
    }
}
/** Alertas demo globales; la acción de detalle solo existe para una ubicación completa válida. */
@Composable
fun AlertsSupervisorScreen(
    viewModel: AppViewModel,
    onNavigate: (String) -> Unit
) {
    val offline by viewModel.offline.collectAsState()

    Scaffold(
        topBar = {
            AppTopBar("Alertas demostrativas")
        },
        bottomBar = {
            SupervisorBottomBar(
                Routes.AlertsSupervisor,
                onNavigate
            )
        }
    ) { inner ->

        Column(
            Modifier
                .fillMaxSize()
                .padding(inner)
                .background(Color.White)
        ) {

            LocalDataBanner(offline, viewModel.lines.firstOrNull()?.measurementDate)

            LazyColumn(
                Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                items(viewModel.alerts.size) { i ->

                    val alert = viewModel.alerts[i]

                    val location = viewModel.resolveAlertLocation(alert)
                    val (color, bg) = statusColors(alert.status)

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = bg
                        ),
                        border = BorderStroke(
                            1.dp,
                            color
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            Modifier.padding(14.dp)
                        ) {

                            Row {

                                Column(
                                    Modifier.weight(1f)
                                ) {

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

                                Text(
                                    "${alert.temperature}°C",
                                    color = color,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp
                                )
                            }

                            if (location != null) {
                                Spacer(Modifier.height(8.dp))

                                OutlinedButton(
                                    onClick = {
                                        if (viewModel.selectAlertLocation(alert)) {
                                            onNavigate(Routes.LineDetail)
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
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
