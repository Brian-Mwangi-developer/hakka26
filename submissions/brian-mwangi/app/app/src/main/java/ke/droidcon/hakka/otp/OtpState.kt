package ke.droidcon.hakka.otp

const val OTP_LENGTH = 6
const val RESEND_COUNTDOWN_SECONDS = 30

/** The code the demo backend accepts. A real app would verify on the server instead. */
const val DEMO_VALID_CODE = "123456"

enum class OtpStatus { Idle, Success, Error }

/**
 * Everything the OTP screen shows, in one immutable value.
 * Every user action is a pure function from one state to the next, so the
 * whole flow can be unit-tested without a device.
 */
data class OtpState(
    val digits: List<Char?> = List(OTP_LENGTH) { null },
    val focusedIndex: Int = 0,
    val status: OtpStatus = OtpStatus.Idle,
    val resendSecondsLeft: Int = RESEND_COUNTDOWN_SECONDS,
) {
    val code: String get() = digits.filterNotNull().joinToString("")
    val isComplete: Boolean get() = digits.all { it != null }
    val canResend: Boolean get() = resendSecondsLeft == 0

    /** Text typed or pasted into box [index]. Non-digits are dropped. */
    fun enter(index: Int, text: String): OtpState {
        val incoming = text.filter(::isAsciiDigit)
        if (incoming.isEmpty()) return this
        if (incoming.length >= OTP_LENGTH) return paste(incoming)

        val next = digits.toMutableList()
        var cursor = index
        for (digit in incoming) {
            if (cursor >= OTP_LENGTH) break
            next[cursor++] = digit
        }
        return copy(
            digits = next,
            focusedIndex = cursor.coerceAtMost(OTP_LENGTH - 1),
            status = OtpStatus.Idle,
        )
    }

    /** Fills all six boxes from the first six digits in [text], e.g. "Your code is 482913". */
    fun paste(text: String): OtpState {
        val pasted = text.filter(::isAsciiDigit).take(OTP_LENGTH)
        if (pasted.isEmpty()) return this
        return copy(
            digits = List(OTP_LENGTH) { pasted.getOrNull(it) },
            focusedIndex = pasted.length.coerceAtMost(OTP_LENGTH - 1),
            status = OtpStatus.Idle,
        )
    }

    /** Backspace in box [index]: clears it, or, if it is already empty, moves back and clears the previous box. */
    fun backspace(index: Int): OtpState {
        val next = digits.toMutableList()
        val target = when {
            next[index] != null -> index
            index > 0 -> index - 1
            else -> return this
        }
        next[target] = null
        return copy(digits = next, focusedIndex = target, status = OtpStatus.Idle)
    }

    fun focus(index: Int): OtpState =
        if (index == focusedIndex || index !in 0 until OTP_LENGTH) this else copy(focusedIndex = index)

    /** Checks the code. A wrong code clears every box and sends focus back to the first one. */
    fun submit(isValid: (String) -> Boolean = { it == DEMO_VALID_CODE }): OtpState = when {
        !isComplete -> this
        isValid(code) -> copy(status = OtpStatus.Success)
        else -> OtpState(status = OtpStatus.Error, resendSecondsLeft = resendSecondsLeft)
    }

    /** One second of the resend countdown has passed. */
    fun tick(): OtpState =
        if (resendSecondsLeft == 0) this else copy(resendSecondsLeft = resendSecondsLeft - 1)

    /** Resending is only possible once the countdown has finished, and restarts it. */
    fun resend(): OtpState =
        if (canResend) copy(resendSecondsLeft = RESEND_COUNTDOWN_SECONDS) else this
}

/** "Resend in 0:07" while counting down. */
fun resendCountdownLabel(secondsLeft: Int): String =
    "Resend in ${secondsLeft / 60}:${(secondsLeft % 60).toString().padStart(2, '0')}"

/**
 * A text field holding [previous] reports [current]; returns only what the user added.
 * The cursor sits after the existing digit, so new input arrives after it.
 */
fun addedText(previous: String, current: String): String = when {
    previous.isEmpty() -> current
    current.startsWith(previous) -> current.removePrefix(previous)
    current.endsWith(previous) -> current.removeSuffix(previous)
    else -> current
}

private fun isAsciiDigit(char: Char): Boolean = char in '0'..'9'
