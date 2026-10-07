package cl.ariztia.bebederos.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.ariztia.bebederos.data.model.TemperatureStatus
import cl.ariztia.bebederos.ui.theme.*

fun statusColors(status: TemperatureStatus): Pair<Color, Color> = when (status) {
    TemperatureStatus.NORMAL -> Normal to NormalBg
    TemperatureStatus.WARNING -> Warning to WarningBg
    TemperatureStatus.CRITICAL -> Critical to CriticalBg
}

@Composable
fun StatusBadge(status: TemperatureStatus) {
    val (color, background) = statusColors(status)
    val label = when (status) {
        TemperatureStatus.NORMAL -> "Normal"
        TemperatureStatus.WARNING -> "Advertencia"
        TemperatureStatus.CRITICAL -> "Crítico"
    }
    Row(
        modifier = Modifier.background(background, RoundedCornerShape(100.dp)).padding(horizontal = 9.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(Modifier.size(6.dp).background(color, CircleShape))
        Text(label, color = color, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(title: String, subtitle: String? = null, onBack: (() -> Unit)? = null) {
    TopAppBar(
        title = {
            Column {
                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                subtitle?.let { Text(it, fontSize = 11.sp, color = TextSecondary) }
            }
        },
        navigationIcon = {
            onBack?.let {
                IconButton(onClick = it) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = BrandRed) }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
    )
}

@Composable
fun OfflineBanner(visible: Boolean) {
    if (!visible) return
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            .background(Color(0xFFF2F2F2), RoundedCornerShape(12.dp))
            .border(1.dp, Border, RoundedCornerShape(12.dp)).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(Icons.Default.CloudOff, null, tint = TextSecondary, modifier = Modifier.size(18.dp))
        Text("Sin conexión", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Text("· Datos guardados a las 14:28", fontSize = 11.sp, color = TextSecondary)
    }
}

@Composable
fun SectionCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = CardDefaults.outlinedCardBorder()
    ) { Column(Modifier.padding(16.dp), content = content) }
}

@Composable
fun SummaryTile(value: String, label: String, color: Color, background: Color) {
    Column(
        Modifier.fillMaxWidth().background(background, RoundedCornerShape(12.dp)).padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, color = color, fontWeight = FontWeight.Bold, fontSize = 23.sp)
        Text(label, color = color, fontSize = 10.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun QuickAction(icon: ImageVector, label: String, tint: Color, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(40.dp).background(tint.copy(alpha = .10f), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = tint, modifier = Modifier.size(21.dp))
            }
            Spacer(Modifier.height(8.dp))
            Text(label, fontSize = 10.sp, lineHeight = 12.sp)
        }
    }
}
