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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.ariztia.bebederos.data.model.*
import cl.ariztia.bebederos.ui.components.*
import cl.ariztia.bebederos.ui.navigation.Routes
import cl.ariztia.bebederos.ui.navigation.SupervisorBottomBar
import cl.ariztia.bebederos.ui.theme.*
import cl.ariztia.bebederos.viewmodel.AppViewModel

@Composable
fun HomeSupervisorScreen(viewModel: AppViewModel, onNavigate: (String) -> Unit) {
    val offline by viewModel.offline.collectAsState()
    val user by viewModel.currentUser.collectAsState()
    val flushings by viewModel.flushings.collectAsState()

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
                    user?.name ?: "Carlos Morales",
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    "Vista general · Todas las granjas",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }

            OfflineBanner(offline)

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
                            "3",
                            "Granjas",
                            BrandRed,
                            BrandRedLight
                        )
                    }

                    Box(Modifier.weight(1f)) {
                        SummaryTile(
                            "9",
                            "Galpones",
                            Color(0xFF6A1B9A),
                            Color(0xFFEDE7F6)
                        )
                    }

                    Box(Modifier.weight(1f)) {
                        SummaryTile(
                            "14",
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
                                "10",
                                "Normales",
                                Normal,
                                NormalBg
                            )
                        }

                        Box(Modifier.weight(1f)) {
                            SummaryTile(
                                "3",
                                "Advertencia",
                                Warning,
                                WarningBg
                            )
                        }

                        Box(Modifier.weight(1f)) {
                            SummaryTile(
                                "1",
                                "Críticas",
                                Critical,
                                CriticalBg
                            )
                        }
                    }
                }

                Text(
                    "Eventos críticos",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )

                viewModel.alerts
                    .filter {
                        it.status == TemperatureStatus.CRITICAL
                    }
                    .forEach { alert ->

                        val line =
                            viewModel.line(alert.lineId)!!

                        val shed =
                            viewModel.shed(alert.shedId)!!

                        val farm =
                            viewModel.farm(alert.farmId)!!

                        Row(
                            Modifier
                                .fillMaxWidth()
                                .background(
                                    CriticalBg,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    viewModel.selectFarm(
                                        alert.farmId
                                    )

                                    viewModel.selectShed(
                                        alert.shedId
                                    )

                                    viewModel.selectLine(
                                        alert.lineId
                                    )

                                    onNavigate(
                                        Routes.LineDetail
                                    )
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
                                    "${line.name} · ${shed.name} · ${farm.name}",
                                    color = Critical,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp
                                )

                                Text(
                                    "${alert.temperature}°C · ${alert.time}",
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
                        "Últimos flushing",
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

                flushings
                    .take(2)
                    .forEach { f ->
                        FlushingCard(
                            viewModel,
                            f
                        )
                    }
            }
        }
    }
}

@Composable
fun FarmsScreen(
    viewModel: AppViewModel,
    onNavigate: (String) -> Unit
) {
    val offline by
    viewModel.offline.collectAsState()

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
            OfflineBanner(offline)

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

                    val lines =
                        viewModel.linesForFarm(
                            farm.id
                        )

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
                                        "${farm.shedIds.size} galpones",
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

@Composable
fun ShedsScreen(
    viewModel: AppViewModel,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit
) {
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

        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(16.dp),
            verticalArrangement =
                Arrangement.spacedBy(12.dp)
        ) {
            items(sheds.size) { i ->

                val shed =
                    sheds[i]

                val lines =
                    viewModel.lines.filter {
                        it.shedId == shed.id
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

@Composable
fun HistoryScreen(
    viewModel: AppViewModel,
    supervisorMode: Boolean,
    onNavigate: (String) -> Unit
) {
    val offline by viewModel.offline.collectAsState()
    var period by remember { mutableStateOf("24h") }

    val line = viewModel.selectedLine()
    val records = viewModel.historyForSelectedLine()

    Scaffold(
        topBar = {
            AppTopBar(
                "Historial de temperatura",
                "${line.name} · ${viewModel.selectedShed().name}"
            )
        },
        bottomBar = {
            if (supervisorMode) {
                SupervisorBottomBar(
                    Routes.History,
                    onNavigate
                )
            }
        }
    ) { inner ->

        Column(
            Modifier
                .fillMaxSize()
                .padding(inner)
                .background(Color.White)
        ) {

            OfflineBanner(offline)

            Column(
                Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {

                SectionCard {

                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Text(
                            "Temperatura",
                            fontWeight = FontWeight.SemiBold
                        )

                        Row {
                            listOf(
                                "24h",
                                "7d"
                            ).forEach { p ->

                                FilterChip(
                                    selected = period == p,
                                    onClick = {
                                        period = p
                                    },
                                    label = {
                                        Text(p)
                                    }
                                )

                                Spacer(
                                    Modifier.width(6.dp)
                                )
                            }
                        }
                    }

                    Spacer(
                        Modifier.height(14.dp)
                    )

                    TemperatureChart(records)
                }

                SectionCard {

                    Text(
                        "Registros",
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(
                        Modifier.height(8.dp)
                    )

                    records
                        .takeLast(5)
                        .reversed()
                        .forEach { record ->

                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {

                                Text(
                                    "${record.date} · ${record.time}",
                                    modifier = Modifier.weight(1f),
                                    fontSize = 12.sp
                                )

                                Text(
                                    "${record.value}°C",
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(
                                    Modifier.width(8.dp)
                                )

                                StatusBadge(
                                    record.status
                                )
                            }
                        }
                }
            }
        }
    }
}

@Composable
private fun TemperatureChart(
    records: List<TemperatureRecord>
) {
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(160.dp)
    ) {

        if (records.size < 2) {
            return@Canvas
        }

        val minY = 16f
        val maxY = 32f

        val stepX =
            size.width / (records.size - 1)

        val path = Path()

        records.forEachIndexed { index, record ->

            val x =
                index * stepX

            val normalized =
                (
                        (record.value.toFloat() - minY) /
                                (maxY - minY)
                        ).coerceIn(
                        0f,
                        1f
                    )

            val y =
                size.height -
                        normalized * size.height

            if (index == 0) {
                path.moveTo(
                    x,
                    y
                )
            } else {
                path.lineTo(
                    x,
                    y
                )
            }
        }

        val warningY =
            size.height -
                    (
                            (23f - minY) /
                                    (maxY - minY)
                            ) * size.height

        val criticalY =
            size.height -
                    (
                            (27f - minY) /
                                    (maxY - minY)
                            ) * size.height

        drawLine(
            color = Warning,
            start = Offset(
                0f,
                warningY
            ),
            end = Offset(
                size.width,
                warningY
            ),
            strokeWidth = 2f
        )

        drawLine(
            color = Critical,
            start = Offset(
                0f,
                criticalY
            ),
            end = Offset(
                size.width,
                criticalY
            ),
            strokeWidth = 2f
        )

        drawPath(
            path = path,
            color = BrandRed,
            style = Stroke(
                width = 5f
            )
        )
    }
}

@Composable
private fun FlushingCard(
    viewModel: AppViewModel,
    flushing: FlushingRecord
) {
    val line = viewModel.line(flushing.lineId)
    val shed = viewModel.shed(flushing.shedId)
    val farm = viewModel.farm(flushing.farmId)

    Card(
        modifier = Modifier.fillMaxWidth(),
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
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector =
                        if (flushing.completed) {
                            Icons.Default.CheckCircle
                        } else {
                            Icons.Default.Warning
                        },
                    contentDescription = null,
                    tint =
                        if (flushing.completed) {
                            Normal
                        } else {
                            Critical
                        }
                )

                Spacer(
                    Modifier.width(8.dp)
                )

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "${line?.name ?: "Línea"} · ${shed?.name ?: "Galpón"}",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )

                    Text(
                        text = farm?.name ?: "Granja",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }

                Text(
                    text =
                        if (flushing.completed) {
                            "Completado"
                        } else {
                            "Pendiente"
                        },
                    color =
                        if (flushing.completed) {
                            Normal
                        } else {
                            Critical
                        },
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(
                Modifier.height(8.dp)
            )

            Text(
                text = "Operario: ${flushing.operatorName}",
                fontSize = 12.sp
            )

            Text(
                text = "${flushing.date} · ${flushing.time}",
                fontSize = 12.sp,
                color = TextSecondary
            )

            Text(
                text = "Motivo: ${flushing.reason}",
                fontSize = 12.sp,
                color = TextSecondary
            )
        }
    }
}