package ke.droidcon.hakka.otp

import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ke.droidcon.hakka.R
import kotlinx.coroutines.delay

/** Holds the screen's single [OtpState] and drives the real one-second resend countdown. */
@Composable
fun OtpScreen(
    initialState: OtpState = OtpState(),
    isValidCode: (String) -> Boolean = { it == DEMO_VALID_CODE },
) {
    var state by remember { mutableStateOf(initialState) }
    val keyboard = LocalSoftwareKeyboardController.current

    LaunchedEffect(state.resendSecondsLeft) {
        if (state.resendSecondsLeft > 0) {
            delay(1_000)
            state = state.tick()
        }
    }
    LaunchedEffect(state.status) {
        if (state.status == OtpStatus.Success) keyboard?.hide()
    }

    OtpSheet(
        state = state,
        onStateChange = { state = it },
        onSubmit = { state = state.submit(isValidCode) },
        onResend = { state = state.resend() },
    )
}

/** The bottom sheet over a dimmed, blurred page. Stateless. */
@Composable
fun OtpSheet(
    state: OtpState,
    onStateChange: (OtpState) -> Unit,
    onSubmit: () -> Unit,
    onResend: () -> Unit,
) {
    val colors = LocalOtpColors.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .semantics { testTagsAsResourceId = true },
    ) {
        PageBehindSheet()
        Box(Modifier.fillMaxSize().background(colors.scrim))
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(0.86f)
                .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                .background(colors.sheet)
                .navigationBarsPadding()
                .imePadding()
                .padding(start = 24.dp, end = 24.dp, top = 36.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(R.drawable.thamani_logo),
                contentDescription = "Thamani logo",
                modifier = Modifier.size(64.dp),
            )
            Spacer(Modifier.height(24.dp))
            Text(
                text = "Enter your code",
                color = colors.textPrimary,
                fontSize = 26.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.semantics { heading() },
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = "A 6-digit code has been sent to your phone.",
                color = colors.textSecondary,
                fontSize = 15.sp,
                lineHeight = 21.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(32.dp))
            OtpInput(state = state, onStateChange = onStateChange, onDone = onSubmit)
            Spacer(Modifier.height(28.dp))
            ResendLine(secondsLeft = state.resendSecondsLeft, canResend = state.canResend, onResend = onResend)
            Spacer(Modifier.height(16.dp))
            StatusMessage(state.status)
            Spacer(Modifier.weight(1f))
            SubmitButton(enabled = state.isComplete, onClick = onSubmit)
        }
    }
}

@Composable
private fun ResendLine(secondsLeft: Int, canResend: Boolean, onResend: () -> Unit) {
    val colors = LocalOtpColors.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Didn't receive the code? ", color = colors.textSecondary, fontSize = 14.sp)
        Text(
            text = if (canResend) "Resend" else resendCountdownLabel(secondsLeft),
            color = if (canResend) colors.link else colors.textSecondary,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .testTag("otp_resend")
                .clip(RoundedCornerShape(8.dp))
                .clickable(enabled = canResend, role = Role.Button, onClick = onResend)
                .padding(horizontal = 4.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun StatusMessage(status: OtpStatus) {
    val colors = LocalOtpColors.current
    val (text, color) = when (status) {
        OtpStatus.Idle -> "" to colors.textSecondary
        OtpStatus.Success -> "You're in." to colors.success
        OtpStatus.Error -> "That code didn't work. Try again." to colors.error
    }
    Text(
        text = text,
        color = color,
        fontSize = 15.sp,
        fontWeight = FontWeight.Medium,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .heightIn(min = 22.dp)
            .testTag("otp_status")
            .semantics { liveRegion = LiveRegionMode.Polite },
    )
}

@Composable
private fun SubmitButton(enabled: Boolean, onClick: () -> Unit) {
    val colors = LocalOtpColors.current
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = colors.buttonBg,
            contentColor = colors.buttonText,
            disabledContainerColor = colors.buttonDisabledBg,
            disabledContentColor = colors.buttonText,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .testTag("otp_submit"),
    ) {
        Text("Submit", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** A stand-in for the page the sheet opens over; blurred where the platform supports it (API 31+). */
@Composable
private fun PageBehindSheet() {
    val colors = LocalOtpColors.current
    val blur = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Modifier.blur(10.dp) else Modifier
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.sheet)
            .then(blur)
            .statusBarsPadding()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Welcome back", color = colors.textPrimary, fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
        repeat(3) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.digitEmptyBg),
            )
        }
    }
}

private val previewState = OtpState(digits = listOf('2', '1', '5', null, null, null), focusedIndex = 3, resendSecondsLeft = 25)

@Preview(name = "Light", widthDp = 390, heightDp = 844)
@Composable
private fun OtpSheetLightPreview() {
    OtpTheme(darkTheme = false) { OtpSheet(previewState, {}, {}, {}) }
}

@Preview(name = "Dark", widthDp = 390, heightDp = 844)
@Composable
private fun OtpSheetDarkPreview() {
    OtpTheme(darkTheme = true) { OtpSheet(previewState, {}, {}, {}) }
}
