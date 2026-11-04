package ke.droidcon.hakka

import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun OtpScreen(initialState: OtpState = OtpState()) {
    val colors = if (isSystemInDarkTheme()) DarkOtpColors else LightOtpColors
    var state by remember { mutableStateOf(initialState) }
    var resendRound by rememberSaveable { mutableStateOf(0) }

    LaunchedEffect(resendRound) {
        while (state.resendSecondsLeft > 0) {
            delay(1_000)
            state = OtpReducer.tick(state)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .semantics { testTagsAsResourceId = true },
    ) {
        BehindSheet(colors)
        Box(Modifier.fillMaxSize().background(colors.scrim))
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(0.86f)
                .background(colors.sheet, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(R.drawable.thamani_logo),
                contentDescription = "Thamani logo",
                modifier = Modifier.size(56.dp),
            )
            Spacer(Modifier.height(20.dp))
            Text("Enter your code", color = colors.textPrimary, fontSize = 26.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(8.dp))
            Text(
                "A 6-digit code has been sent to your phone.",
                color = colors.textSecondary,
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(28.dp))
            OtpInput(state = state, colors = colors, onStateChange = { state = it })
            Spacer(Modifier.height(24.dp))
            ResendLine(state, colors) {
                state = OtpReducer.resend(state)
                resendRound++
            }
            Spacer(Modifier.height(16.dp))
            StatusMessage(state.status, colors)
            Spacer(Modifier.weight(1f))
            Button(
                onClick = { state = OtpReducer.submit(state) },
                enabled = state.isComplete,
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.button,
                    contentColor = colors.buttonText,
                    disabledContainerColor = colors.buttonDisabled,
                    disabledContentColor = colors.buttonText,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("otp_submit"),
            ) {
                Text("Submit", fontSize = 16.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun BehindSheet(colors: OtpColors) {
    val blur = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Modifier.blur(12.dp) else Modifier
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.sheet)
            .then(blur)
            .padding(top = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Welcome back", color = colors.textPrimary, fontSize = 28.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun OtpInput(state: OtpState, colors: OtpColors, onStateChange: (OtpState) -> Unit) {
    val focusRequesters = remember { List(OTP_LENGTH) { FocusRequester() } }
    val keyboard = LocalSoftwareKeyboardController.current

    LaunchedEffect(state.focusedIndex, state.status) {
        focusRequesters[state.focusedIndex].requestFocus()
        keyboard?.show()
    }

    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        repeat(OTP_LENGTH) { index ->
            DigitBox(
                index = index,
                digit = state.digits[index],
                focused = state.focusedIndex == index,
                colors = colors,
                focusRequester = focusRequesters[index],
                onInput = { onStateChange(OtpReducer.input(state, index, it)) },
                onBackspace = { onStateChange(OtpReducer.backspace(state, index)) },
                onFocused = { if (state.focusedIndex != index) onStateChange(state.copy(focusedIndex = index)) },
            )
        }
    }
}

@Composable
private fun DigitBox(
    index: Int,
    digit: String,
    focused: Boolean,
    colors: OtpColors,
    focusRequester: FocusRequester,
    onInput: (String) -> Unit,
    onBackspace: () -> Unit,
    onFocused: () -> Unit,
) {
    val background = when {
        digit.isNotEmpty() -> colors.filledBackground
        focused -> colors.focusedBackground
        else -> colors.emptyBackground
    }
    val outline = when {
        digit.isNotEmpty() -> colors.filledBackground
        focused -> colors.focusedOutline
        else -> colors.emptyOutline
    }

    BasicTextField(
        value = TextFieldValue(digit, selection = TextRange(digit.length)),
        onValueChange = { value ->
            when {
                value.text.isEmpty() && digit.isNotEmpty() -> onBackspace()
                value.text != digit -> onInput(value.text.removePrefix(digit).ifEmpty { value.text })
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        textStyle = TextStyle(
            color = colors.filledText,
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
        ),
        cursorBrush = SolidColor(colors.focusedOutline),
        modifier = Modifier
            .size(46.dp)
            .focusRequester(focusRequester)
            .onFocusChanged { if (it.isFocused) onFocused() }
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && event.key == Key.Backspace && digit.isEmpty()) {
                    onBackspace()
                    true
                } else {
                    false
                }
            }
            .testTag("otp_digit_${index + 1}")
            .semantics { contentDescription = "Digit ${index + 1} of $OTP_LENGTH" }
            .background(background, CircleShape)
            .border(if (focused && digit.isEmpty()) 2.dp else 1.dp, outline, CircleShape),
        decorationBox = { inner ->
            Box(contentAlignment = Alignment.Center) {
                if (digit.isEmpty()) {
                    Text("–", color = colors.placeholder, fontSize = 20.sp)
                }
                inner()
            }
        },
    )
}

@Composable
private fun ResendLine(state: OtpState, colors: OtpColors, onResend: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Didn't receive the code? ", color = colors.textSecondary, fontSize = 14.sp)
        TextButton(
            onClick = onResend,
            enabled = state.canResend,
            modifier = Modifier.testTag("otp_resend"),
        ) {
            val label = if (state.canResend) {
                "Resend"
            } else {
                "Resend in 0:%02d".format(state.resendSecondsLeft)
            }
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(color = if (state.canResend) colors.link else colors.textSecondary)) { append(label) }
                },
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun StatusMessage(status: OtpStatus, colors: OtpColors) {
    val (text, color) = when (status) {
        OtpStatus.Idle -> "" to colors.textSecondary
        OtpStatus.Success -> "You're in." to colors.success
        OtpStatus.Error -> "That code didn't work. Try again." to colors.error
    }
    Text(
        text,
        color = color,
        fontSize = 15.sp,
        modifier = Modifier
            .testTag("otp_status")
            .semantics { liveRegion = LiveRegionMode.Polite },
    )
}
