---
name: Challenge
about: The hakkahakka26 challenge (opened live on stage)
title: Build the OTP verification screen
labels: challenge
---

Build this screen in your stack of choice with **one AI prompt**. Commit that prompt, verbatim, as `PROMPT.md`.

![Reference](https://raw.githubusercontent.com/buildthamani/hakka26/main/reference/otp-reference.png)

## Copy on screen (exact)

- Title: `Enter your code`
- Subtitle: `A 6-digit code has been sent to your phone.`
- Resend line: `Didn't receive the code? Resend` (during countdown: `Resend in 0:SS`)
- Button: `Submit`
- Success message: `You're in.`
- Error message: `That code didn't work. Try again.`

> If the reference image's copy differs from the list above, **the list wins**.

## Acceptance criteria

1. Six separate circular digit boxes; digits only; the numeric keyboard opens automatically.
2. Three box states: filled (dark green circle, white digit), focused (white circle, dark outline), empty (light grey, shows a placeholder, never a real `0`).
3. Typing a digit moves focus to the next box.
4. Backspace on an empty box moves back and clears the previous digit.
5. Pasting `Your code is 482913` fills the six boxes with `482913`.
6. `Submit` is disabled until all six digits are filled.
7. `123456` + Submit shows the success message.
8. Any other code shows the error message, clears all boxes and returns focus to the first box.
9. `Resend` is disabled for a 30 s countdown, then tappable; tapping it restarts the countdown.
10. Each box is announced to screen readers as "Digit N of 6"; the screen works in dark mode.

## Presentation

Shown as a bottom sheet over a dimmed background (blur is a bonus, not required), with the logo from `assets/`, and a full-width rounded `Submit` button at the bottom. Colours are in `assets/colours.json`.

## Test IDs

Use these so humans can verify quickly: `otp_digit_1` … `otp_digit_6`, `otp_submit`, `otp_resend`, `otp_status`.

## Submitting

Fork, copy a template from `templates/` into `submissions/<team-name>/app/`, and open a PR. See the [README](https://github.com/buildthamani/hakka26#readme).
