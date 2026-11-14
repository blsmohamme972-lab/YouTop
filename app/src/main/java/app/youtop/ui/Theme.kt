package app.youtop.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import app.youtop.R

// The look of the app: a warm near-black, one amber accent, and two typefaces.
// Lesson pictures are painted scenes, so the colours around them stay dark and warm.

/** Page background. */
val Ink = Color(0xFF0C0B0A)
/** Cards and sheets on the page. */
val Surface1 = Color(0xFF171513)
/** Raised or pressed things on a card. */
val Surface2 = Color(0xFF24211D)
/** Text and icons on the amber accent. */
val OnAccent = Color(0xFF1C1505)
val Faint = Color.White.copy(alpha = 0.62f)
val Good = Color(0xFF4ADE80)
val Bad = Color(0xFFF87171)

/** Everything except stories: one clean family that covers Arabic and Latin, so mixed text matches. */
val Ui = FontFamily(
    Font(R.font.plex_regular, FontWeight.Normal),
    Font(R.font.plex_medium, FontWeight.Medium),
    Font(R.font.plex_semibold, FontWeight.SemiBold),
)

/** Stories and quotations: a classical book face. */
val Literary = FontFamily(
    Font(R.font.amiri_regular, FontWeight.Normal),
    Font(R.font.amiri_bold, FontWeight.Bold),
)

private val Type = Typography().run {
    copy(
        displayLarge = displayLarge.copy(fontFamily = Ui), displayMedium = displayMedium.copy(fontFamily = Ui),
        displaySmall = displaySmall.copy(fontFamily = Ui), headlineLarge = headlineLarge.copy(fontFamily = Ui),
        headlineMedium = headlineMedium.copy(fontFamily = Ui), headlineSmall = headlineSmall.copy(fontFamily = Ui),
        titleLarge = titleLarge.copy(fontFamily = Ui), titleMedium = titleMedium.copy(fontFamily = Ui),
        titleSmall = titleSmall.copy(fontFamily = Ui), bodyLarge = bodyLarge.copy(fontFamily = Ui),
        bodyMedium = bodyMedium.copy(fontFamily = Ui), bodySmall = bodySmall.copy(fontFamily = Ui),
        labelLarge = labelLarge.copy(fontFamily = Ui), labelMedium = labelMedium.copy(fontFamily = Ui),
        labelSmall = labelSmall.copy(fontFamily = Ui),
    )
}

private val Scheme = darkColorScheme(
    primary = Accent, onPrimary = OnAccent,
    secondary = Accent, onSecondary = OnAccent,
    tertiary = Accent, onTertiary = OnAccent,
    primaryContainer = Surface2, onPrimaryContainer = Color.White,
    background = Ink, onBackground = Color.White,
    surface = Surface1, onSurface = Color.White,
    surfaceVariant = Surface2, onSurfaceVariant = Color.White.copy(alpha = 0.72f),
    surfaceContainer = Surface1, surfaceContainerHigh = Surface2, surfaceContainerHighest = Surface2,
    surfaceContainerLow = Surface1, surfaceContainerLowest = Ink,
    outline = Color.White.copy(alpha = 0.32f), outlineVariant = Color.White.copy(alpha = 0.14f),
    error = Bad,
)

@Composable
fun YouTopTheme(content: @Composable () -> Unit) = MaterialTheme(colorScheme = Scheme, typography = Type, content = content)
