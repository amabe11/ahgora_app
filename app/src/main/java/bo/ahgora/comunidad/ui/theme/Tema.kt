package bo.ahgora.comunidad.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Paleta de la Guía definitiva de Ahgora (azul petróleo, verde, menta). Contraste AA sobre fondo claro.
object Paleta {
    val Petroleo = Color(0xFF12303A)
    val Verde = Color(0xFF0E6B5C)
    val VerdeOscuro = Color(0xFF0B4A40)
    val Menta = Color(0xFFDCEFE9)
    val Fondo = Color(0xFFF4F7F5)
    val Tarjeta = Color(0xFFEAF1EE)
    val Texto = Color(0xFF14252B)
    val TextoSuave = Color(0xFF4A5A60)
    val Borde = Color(0xFFD5E0DC)
    val Ambar = Color(0xFF7A5100)
    val AmbarFondo = Color(0xFFFFF1CC)
}

private val Esquema = lightColorScheme(
    primary = Paleta.Verde,
    onPrimary = Color.White,
    primaryContainer = Paleta.Menta,
    onPrimaryContainer = Paleta.VerdeOscuro,
    secondary = Paleta.Petroleo,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD3E3E8),
    onSecondaryContainer = Paleta.Petroleo,
    tertiary = Paleta.Ambar,
    tertiaryContainer = Paleta.AmbarFondo,
    onTertiaryContainer = Color(0xFF4A3100),
    background = Paleta.Fondo,
    onBackground = Paleta.Texto,
    surface = Color.White,
    onSurface = Paleta.Texto,
    surfaceVariant = Paleta.Tarjeta,
    onSurfaceVariant = Paleta.TextoSuave,
    surfaceContainer = Paleta.Tarjeta,
    surfaceContainerLow = Color(0xFFF0F5F3),
    surfaceContainerHigh = Color(0xFFE3ECE8),
    outline = Color(0xFF6F8580),
    outlineVariant = Paleta.Borde,
    error = Color(0xFFB3261E),
)

private val Serif = FontFamily.Serif
private val base = Typography()
private val Tipografia = Typography(
    displaySmall = base.displaySmall.copy(fontFamily = Serif, fontWeight = FontWeight.Bold),
    headlineMedium = base.headlineMedium.copy(fontFamily = Serif, fontWeight = FontWeight.Bold),
    headlineSmall = base.headlineSmall.copy(fontFamily = Serif, fontWeight = FontWeight.Bold),
    titleLarge = base.titleLarge.copy(fontWeight = FontWeight.Bold),
    titleMedium = base.titleMedium.copy(fontWeight = FontWeight.Bold),
    bodyLarge = TextStyle(fontSize = 17.sp, lineHeight = 25.sp),
    bodyMedium = TextStyle(fontSize = 15.sp, lineHeight = 22.sp),
    labelLarge = base.labelLarge.copy(fontWeight = FontWeight.Bold),
)

@Composable
fun AhgoraTema(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Esquema, typography = Tipografia, content = content)
}
