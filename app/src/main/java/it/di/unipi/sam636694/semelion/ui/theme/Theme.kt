package it.di.unipi.sam636694.semelion.ui.theme

import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

data class MatchSummaryCardTheme(
    val containerColor: Color,
    val borderStroke: BorderStroke?,
    val nameColor: Color,
    val statusText: String,
    val statusColor: Color,
    val timeLabelColor: Color,
    val timeValueColor: Color,
    val dividerColor: Color,
    val statLabelColor: Color,
    val statValueColor: Color
)

val WinnerTheme = MatchSummaryCardTheme(
    containerColor  = CardLight,
    borderStroke    = BorderStroke(2.dp, GreenPrimary),
    nameColor       = Color.Black,
    statusText      = "WINNER",
    statusColor     = GreenPrimary,
    timeLabelColor  = TextMuted,
    timeValueColor  = TextDark,
    dividerColor    = Color(0xFFDDEEDD),
    statLabelColor  = TextMuted,
    statValueColor  = TextDark
)

val LoserTheme = MatchSummaryCardTheme(
    containerColor  = CardDark,
    borderStroke    = null,
    nameColor       = Color.White,
    statusText      = "DEFEATED",
    statusColor     = Color(0xFFFF6B6B),
    timeLabelColor  = Color(0xFF8FA88F),
    timeValueColor  = Color.White,
    dividerColor    = Color(0xFF3D4D3D),
    statLabelColor  = Color(0xFF8FA88F),
    statValueColor  = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40

    /* Other default colors to override
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    */
)

@Composable
fun SemelionTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

val TextPrimary   = Color(0xFF111111)
val TextSecondary = Color(0xFF888888)