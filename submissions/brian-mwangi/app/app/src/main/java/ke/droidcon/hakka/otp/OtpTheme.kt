package ke.droidcon.hakka.otp

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** The `otp` palette from assets/colours.json, one set per system theme. */
@Immutable
data class OtpColors(
    val sheet: Color,
    val scrim: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val digitFilledBg: Color,
    val digitFilledText: Color,
    val digitFocusedBg: Color,
    val digitFocusedOutline: Color,
    val digitEmptyBg: Color,
    val digitEmptyOutline: Color,
    val digitPlaceholder: Color,
    val buttonBg: Color,
    val buttonText: Color,
    val buttonDisabledBg: Color,
    val link: Color,
    val success: Color,
    val error: Color,
)

val LightOtpColors = OtpColors(
    sheet = Color(0xFFFFFFFF),
    scrim = Color(0x99000000),
    textPrimary = Color(0xFF111111),
    textSecondary = Color(0xFF5F6368),
    digitFilledBg = Color(0xFF12361B),
    digitFilledText = Color(0xFFFFFFFF),
    digitFocusedBg = Color(0xFFFFFFFF),
    digitFocusedOutline = Color(0xFF111111),
    digitEmptyBg = Color(0xFFF1F1F1),
    digitEmptyOutline = Color(0xFFE0E0E0),
    digitPlaceholder = Color(0xFF9E9E9E),
    buttonBg = Color(0xFF12361B),
    buttonText = Color(0xFFFFFFFF),
    buttonDisabledBg = Color(0xFFC9D1CB),
    link = Color(0xFF12361B),
    success = Color(0xFF1B7F3A),
    error = Color(0xFFC62828),
)

val DarkOtpColors = OtpColors(
    sheet = Color(0xFF161A17),
    scrim = Color(0xCC000000),
    textPrimary = Color(0xFFF5F5F5),
    textSecondary = Color(0xFFB0B5B1),
    digitFilledBg = Color(0xFF2E7D45),
    digitFilledText = Color(0xFFFFFFFF),
    digitFocusedBg = Color(0xFF161A17),
    digitFocusedOutline = Color(0xFFF5F5F5),
    digitEmptyBg = Color(0xFF262B27),
    digitEmptyOutline = Color(0xFF353B36),
    digitPlaceholder = Color(0xFF6F7570),
    buttonBg = Color(0xFF2E7D45),
    buttonText = Color(0xFFFFFFFF),
    buttonDisabledBg = Color(0xFF2A302B),
    link = Color(0xFF7BC795),
    success = Color(0xFF7BC795),
    error = Color(0xFFEF9A9A),
)

val LocalOtpColors = staticCompositionLocalOf { LightOtpColors }

/** Follows the system light/dark setting. */
@Composable
fun OtpTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colors = if (darkTheme) DarkOtpColors else LightOtpColors
    MaterialTheme(colorScheme = if (darkTheme) darkColorScheme() else lightColorScheme()) {
        CompositionLocalProvider(LocalOtpColors provides colors, content = content)
    }
}
