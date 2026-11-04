package ke.droidcon.hakka

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class OtpColors(
    val sheet: Color,
    val scrim: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val filledBackground: Color,
    val filledText: Color,
    val focusedBackground: Color,
    val focusedOutline: Color,
    val emptyBackground: Color,
    val emptyOutline: Color,
    val placeholder: Color,
    val button: Color,
    val buttonText: Color,
    val buttonDisabled: Color,
    val link: Color,
    val success: Color,
    val error: Color,
)

val LightOtpColors = OtpColors(
    sheet = Color(0xFFFFFFFF),
    scrim = Color(0x99000000),
    textPrimary = Color(0xFF111111),
    textSecondary = Color(0xFF5F6368),
    filledBackground = Color(0xFF12361B),
    filledText = Color(0xFFFFFFFF),
    focusedBackground = Color(0xFFFFFFFF),
    focusedOutline = Color(0xFF111111),
    emptyBackground = Color(0xFFF1F1F1),
    emptyOutline = Color(0xFFE0E0E0),
    placeholder = Color(0xFF9E9E9E),
    button = Color(0xFF12361B),
    buttonText = Color(0xFFFFFFFF),
    buttonDisabled = Color(0xFFC9D1CB),
    link = Color(0xFF12361B),
    success = Color(0xFF1B7F3A),
    error = Color(0xFFC62828),
)

val DarkOtpColors = OtpColors(
    sheet = Color(0xFF161A17),
    scrim = Color(0xCC000000),
    textPrimary = Color(0xFFF5F5F5),
    textSecondary = Color(0xFFB0B5B1),
    filledBackground = Color(0xFF2E7D45),
    filledText = Color(0xFFFFFFFF),
    focusedBackground = Color(0xFF161A17),
    focusedOutline = Color(0xFFF5F5F5),
    emptyBackground = Color(0xFF262B27),
    emptyOutline = Color(0xFF353B36),
    placeholder = Color(0xFF6F7570),
    button = Color(0xFF2E7D45),
    buttonText = Color(0xFFFFFFFF),
    buttonDisabled = Color(0xFF2A302B),
    link = Color(0xFF7BC795),
    success = Color(0xFF7BC795),
    error = Color(0xFFEF9A9A),
)
