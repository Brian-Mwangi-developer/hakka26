package ke.droidcon.hakka.otp

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private const val PLACEHOLDER = "–"

/**
 * Six circular digit boxes. Owns no state: it renders [state] and reports each
 * keystroke as a transition for the owner to apply to its latest [OtpState].
 * Passing transitions rather than new states matters: keystrokes can arrive
 * faster than recomposition, and a stale snapshot would drop digits.
 */
@Composable
fun OtpInput(
    state: OtpState,
    onUpdate: ((OtpState) -> OtpState) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusRequesters = remember { List(OTP_LENGTH) { FocusRequester() } }
    val keyboard = LocalSoftwareKeyboardController.current

    // Autofocus on first show, then follow the state: next box after typing,
    // previous box after backspace, first box after a wrong code.
    LaunchedEffect(state.focusedIndex) {
        focusRequesters[state.focusedIndex].requestFocus()
        keyboard?.show()
    }

    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        for (index in 0 until OTP_LENGTH) {
            DigitBox(
                index = index,
                digit = state.digits[index],
                focused = state.focusedIndex == index,
                focusRequester = focusRequesters[index],
                onEdit = { fieldText -> onUpdate { it.edit(index, fieldText) } },
                onBackspace = { onUpdate { it.backspace(it.focusedIndex) } },
                onFocused = { onUpdate { it.focus(index) } },
                onDone = onDone,
            )
        }
    }
}

@Composable
private fun DigitBox(
    index: Int,
    digit: Char?,
    focused: Boolean,
    focusRequester: FocusRequester,
    onEdit: (String) -> Unit,
    onBackspace: () -> Unit,
    onFocused: () -> Unit,
    onDone: () -> Unit,
) {
    val colors = LocalOtpColors.current
    val text = digit?.toString().orEmpty()
    val (background, outline, outlineWidth) = when {
        digit != null -> Triple(colors.digitFilledBg, colors.digitFilledBg, 1.dp)
        focused -> Triple(colors.digitFocusedBg, colors.digitFocusedOutline, 2.dp)
        else -> Triple(colors.digitEmptyBg, colors.digitEmptyOutline, 1.dp)
    }

    BasicTextField(
        value = TextFieldValue(text, selection = TextRange(text.length)),
        onValueChange = { value -> onEdit(value.text) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        textStyle = TextStyle(
            color = colors.digitFilledText,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        ),
        cursorBrush = SolidColor(colors.digitFocusedOutline),
        modifier = Modifier
            .size(48.dp)
            .focusRequester(focusRequester)
            .onFocusChanged { if (it.isFocused) onFocused() }
            // Soft and hardware keyboards send backspace on an empty field as a key event, not a text change.
            .onPreviewKeyEvent { event ->
                val isBackspaceOnEmpty = event.type == KeyEventType.KeyDown && event.key == Key.Backspace && digit == null
                if (isBackspaceOnEmpty) onBackspace()
                isBackspaceOnEmpty
            }
            .testTag("otp_digit_${index + 1}")
            .semantics {
                contentDescription = "Digit ${index + 1} of $OTP_LENGTH"
                stateDescription = text.ifEmpty { "Empty" }
            }
            .background(background, CircleShape)
            .border(outlineWidth, outline, CircleShape),
        decorationBox = { innerTextField ->
            Box(contentAlignment = Alignment.Center) {
                if (digit == null && !focused) {
                    Text(PLACEHOLDER, color = colors.digitPlaceholder, fontSize = 20.sp)
                }
                innerTextField()
            }
        },
    )
}
