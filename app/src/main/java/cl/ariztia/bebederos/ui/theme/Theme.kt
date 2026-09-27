package cl.ariztia.bebederos.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val BrandRed = Color(0xFFE30613)
val BrandRedDark = Color(0xFFB5121B)
val BrandRedLight = Color(0xFFFDE8EA)
val AppBackground = Color(0xFFF7F7F7)
val SurfaceWhite = Color(0xFFFFFFFF)
val TextPrimary = Color(0xFF1F1F1F)
val TextSecondary = Color(0xFF606060)
val Border = Color(0xFFE4E4E4)
val Normal = Color(0xFF2E7D32)
val NormalBg = Color(0xFFE8F5E9)
val Warning = Color(0xFFF57C00)
val WarningBg = Color(0xFFFFF3E0)
val Critical = Color(0xFFC62828)
val CriticalBg = Color(0xFFFFEBEE)

private val Colors = lightColorScheme(
    primary = BrandRed,
    onPrimary = Color.White,
    secondary = BrandRedDark,
    background = AppBackground,
    surface = SurfaceWhite,
    onSurface = TextPrimary,
    error = Critical
)

@Composable
fun BebederosTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Colors, content = content)
}