package app.tenvia.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Brand colors from brand/README.md. Always dark: easier on eyes at night and
// dark pixels use less power on OLED screens.
val Night = Color(0xFF101B17)
val Forest = Color(0xFF17231E)
val ForestLight = Color(0xFF24342B)
val Amber = Color(0xFFD4A14A)
val Paper = Color(0xFFF6F2E9)
val Muted = Color(0xFFB9C0AE)
val Warning = Color(0xFFE8A07A)

private val colors = darkColorScheme(
    primary = Amber,
    onPrimary = Forest,
    background = Night,
    onBackground = Paper,
    surface = Forest,
    onSurface = Paper,
    surfaceVariant = ForestLight,
    onSurfaceVariant = Muted,
    outline = Muted,
)

// Large, plain type so it can be read under stress, in sunlight and with poor eyesight.
private val type = Typography(
    headlineLarge = TextStyle(fontSize = 34.sp, lineHeight = 40.sp, fontWeight = FontWeight.Bold),
    headlineSmall = TextStyle(fontSize = 26.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold),
    titleLarge = TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 20.sp, lineHeight = 28.sp),
    bodyMedium = TextStyle(fontSize = 17.sp, lineHeight = 24.sp),
    labelLarge = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
)

@Composable
fun TenviaTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colors, typography = type, content = content)
}
