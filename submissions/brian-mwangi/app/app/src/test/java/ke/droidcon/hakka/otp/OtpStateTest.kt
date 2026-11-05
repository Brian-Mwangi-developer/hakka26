package ke.droidcon.hakka.otp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class OtpStateTest {
    private fun typed(code: String): OtpState =
        code.foldIndexed(OtpState()) { index, state, char -> state.enter(index, char.toString()) }

    @Test
    fun startsEmptyWithFocusOnTheFirstBox() {
        val state = OtpState()

        assertEquals(List(OTP_LENGTH) { null }, state.digits)
        assertEquals(0, state.focusedIndex)
        assertFalse(state.isComplete)
    }

    @Test
    fun typingADigitFillsTheBoxAndMovesFocusToTheNext() {
        val state = OtpState().enter(0, "4")

        assertEquals('4', state.digits[0])
        assertEquals(1, state.focusedIndex)
    }

    @Test
    fun focusStaysOnTheLastBoxOnceItIsFilled() {
        assertEquals(OTP_LENGTH - 1, typed("123456").focusedIndex)
    }

    @Test
    fun nonDigitsAreRejected() {
        val empty = OtpState()

        assertSame(empty, empty.enter(0, "a"))
        assertSame(empty, empty.enter(0, " "))
        assertSame(empty, empty.enter(0, "٣")) // a non-ASCII digit
    }

    @Test
    fun typingIntoAFilledBoxReplacesItsDigit() {
        val state = typed("21").enter(0, "7")

        assertEquals("71", state.code)
        assertEquals(1, state.focusedIndex)
    }

    @Test
    fun backspaceOnAFilledBoxClearsIt() {
        val state = typed("215").backspace(2)

        assertEquals(listOf('2', '1', null, null, null, null), state.digits)
        assertEquals(2, state.focusedIndex)
    }

    @Test
    fun backspaceOnAnEmptyBoxMovesBackAndClearsThePreviousDigit() {
        val state = typed("215").backspace(3)

        assertEquals(listOf('2', '1', null, null, null, null), state.digits)
        assertEquals(2, state.focusedIndex)
    }

    @Test
    fun backspaceOnTheFirstEmptyBoxDoesNothing() {
        val empty = OtpState()

        assertSame(empty, empty.backspace(0))
    }

    @Test
    fun pastingAMessageExtractsTheSixDigits() {
        val state = OtpState().enter(0, "Your code is 482913")

        assertEquals("482913", state.code)
        assertTrue(state.isComplete)
    }

    @Test
    fun pastingIntoAnyBoxFillsAllSixFromTheStart() {
        val state = typed("99").enter(3, "Your code is 482913")

        assertEquals("482913", state.code)
    }

    @Test
    fun pastingFewerThanSixDigitsFillsForwardFromTheBox() {
        val state = typed("1").enter(1, "23")

        assertEquals("123", state.code)
        assertEquals(3, state.focusedIndex)
    }

    @Test
    fun addedTextIgnoresTheDigitAlreadyInTheBox() {
        assertEquals("7", addedText("2", "27"))
        assertEquals("Your code is 482913", addedText("5", "5Your code is 482913"))
        assertEquals("4", addedText("", "4"))
    }

    @Test
    fun submitDoesNothingUntilAllSixDigitsAreFilled() {
        val partial = typed("12345")

        assertFalse(partial.isComplete)
        assertSame(partial, partial.submit())
    }

    @Test
    fun theCorrectCodeSucceeds() {
        val state = typed("123456").submit()

        assertEquals(OtpStatus.Success, state.status)
        assertEquals("123456", state.code)
    }

    @Test
    fun aWrongCodeShowsTheErrorClearsEveryBoxAndFocusesTheFirst() {
        val state = typed("654321").submit()

        assertEquals(OtpStatus.Error, state.status)
        assertEquals(List(OTP_LENGTH) { null }, state.digits)
        assertEquals(0, state.focusedIndex)
    }

    @Test
    fun aWrongCodeDoesNotResetTheResendCountdown() {
        val state = typed("654321").copy(resendSecondsLeft = 12).submit()

        assertEquals(12, state.resendSecondsLeft)
    }

    @Test
    fun typingAfterAnErrorClearsTheMessage() {
        val state = typed("654321").submit().enter(0, "1")

        assertEquals(OtpStatus.Idle, state.status)
    }

    @Test
    fun theVerifierDecidesWhichCodeIsValid() {
        val state = typed("654321").submit(isValid = { it == "654321" })

        assertEquals(OtpStatus.Success, state.status)
    }

    @Test
    fun resendIsDisabledDuringTheThirtySecondCountdown() {
        val fresh = OtpState()

        assertEquals(RESEND_COUNTDOWN_SECONDS, fresh.resendSecondsLeft)
        assertFalse(fresh.canResend)
        assertSame(fresh, fresh.resend())
    }

    @Test
    fun resendBecomesAvailableAfterThirtyTicksAndRestartsTheCountdown() {
        val finished = (1..RESEND_COUNTDOWN_SECONDS).fold(OtpState()) { state, _ -> state.tick() }

        assertTrue(finished.canResend)
        assertSame(finished, finished.tick())
        assertEquals(RESEND_COUNTDOWN_SECONDS, finished.resend().resendSecondsLeft)
    }

    @Test
    fun countdownLabelUsesMinutesAndTwoDigitSeconds() {
        assertEquals("Resend in 0:30", resendCountdownLabel(30))
        assertEquals("Resend in 0:07", resendCountdownLabel(7))
    }
}
