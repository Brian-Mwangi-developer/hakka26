package ke.droidcon.hakka

const val OTP_LENGTH = 6
const val RESEND_SECONDS = 30
const val VALID_CODE = "123456"

enum class OtpStatus { Idle, Success, Error }

data class OtpState(
    val digits: List<String> = List(OTP_LENGTH) { "" },
    val focusedIndex: Int = 0,
    val status: OtpStatus = OtpStatus.Idle,
    val resendSecondsLeft: Int = RESEND_SECONDS,
) {
    val code: String get() = digits.joinToString("")
    val isComplete: Boolean get() = digits.all { it.isNotEmpty() }
    val canResend: Boolean get() = resendSecondsLeft == 0
}

object OtpReducer {
    fun input(state: OtpState, index: Int, text: String): OtpState {
        val numbers = text.filter(Char::isDigit)
        if (numbers.isEmpty()) return state
        if (numbers.length >= OTP_LENGTH || numbers.length != text.length) {
            return paste(state, numbers)
        }
        val digits = state.digits.toMutableList()
        numbers.take(OTP_LENGTH - index).forEachIndexed { offset, char -> digits[index + offset] = char.toString() }
        return state.copy(
            digits = digits,
            focusedIndex = (index + numbers.length).coerceAtMost(OTP_LENGTH - 1),
            status = OtpStatus.Idle,
        )
    }

    fun paste(state: OtpState, text: String): OtpState {
        val numbers = text.filter(Char::isDigit).take(OTP_LENGTH)
        if (numbers.isEmpty()) return state
        val digits = List(OTP_LENGTH) { i -> numbers.getOrNull(i)?.toString() ?: "" }
        return state.copy(
            digits = digits,
            focusedIndex = (numbers.length).coerceAtMost(OTP_LENGTH - 1),
            status = OtpStatus.Idle,
        )
    }

    fun backspace(state: OtpState, index: Int): OtpState {
        val digits = state.digits.toMutableList()
        if (digits[index].isNotEmpty()) {
            digits[index] = ""
            return state.copy(digits = digits, focusedIndex = index, status = OtpStatus.Idle)
        }
        if (index == 0) return state
        digits[index - 1] = ""
        return state.copy(digits = digits, focusedIndex = index - 1, status = OtpStatus.Idle)
    }

    fun submit(state: OtpState): OtpState {
        if (!state.isComplete) return state
        return if (state.code == VALID_CODE) {
            state.copy(status = OtpStatus.Success)
        } else {
            state.copy(digits = List(OTP_LENGTH) { "" }, focusedIndex = 0, status = OtpStatus.Error)
        }
    }

    fun tick(state: OtpState): OtpState =
        state.copy(resendSecondsLeft = (state.resendSecondsLeft - 1).coerceAtLeast(0))

    fun resend(state: OtpState): OtpState =
        if (state.canResend) state.copy(resendSecondsLeft = RESEND_SECONDS) else state
}
