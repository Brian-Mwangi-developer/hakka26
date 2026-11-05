# hakkahakka26

A 30-minute, AI-graded mobile hackathon at **droidcon Kenya 2026**.

**One prompt. One screen. 30 minutes. KES 42,000.**

- The challenge is [issue #1](../../issues/1): build an OTP verification screen.
- Leaderboard: https://buildthamani.github.io/hakka26/
- Full rules: [RULES.md](RULES.md)

## How it works

1. Form a team of **up to 3 people**.
2. When issue #1 opens on stage, you have **30 minutes**.
3. Give your AI tool **one prompt** to build the screen, then commit that prompt verbatim as `PROMPT.md`.
4. Open a PR. CI builds your app and runs your tests, then **Claude Sonnet grades it out of 25** and comments the score on your PR.
5. The leaderboard updates live. Highest score wins; the top 3 demo for 3 minutes each.

## How to submit

1. **Fork** this repository.
2. Pick a template and copy it into your team folder:
   ```sh
   cp -r templates/android-compose submissions/<team-name>/app
   ```
   Templates: `android-compose`, `compose-multiplatform`, `flutter`, `react-native` (bare), `expo`, `ios-swiftui`. Each template already contains the logo and `colours.json` from `assets/`.
3. Add these files:
   ```
   submissions/<team-name>/
   ├── PROMPT.md          # your single prompt, verbatim; 600 characters or fewer
   ├── TEAM.md            # up to 3 names + GitHub handles
   ├── screenshot.png     # the screen in the "partially filled" state, like the reference
   ├── demo.gif or .mp4   # 10 MB or less, for humans (the grader can't watch video)
   └── app/               # the template + your changes
   ```
   `<team-name>` is lowercase, uses only `a-z`, `0-9` and `-`, and is 3–30 characters long.
4. Open a PR against `main`. Fill in the checklist.
5. Push more commits to the same PR to improve your score.

`TEAM.md` example:

```md
- Jane Doe (@janedoe)
- John Otieno (@jotieno)
```

## What CI runs

CI detects your stack from `app/`:

| Detected by | Stack | Command |
|---|---|---|
| `pubspec.yaml` | flutter | `flutter build apk --debug && flutter test` |
| `package.json` containing `react-native` (bare or Expo) | react-native | `npm ci`, then `npx expo prebuild --platform android` if there's no `android/` folder, then `npx jest && (cd android && ./gradlew assembleDebug)` |
| `*.xcodeproj` or `Package.swift`, no Gradle | ios | `xcodebuild build test -scheme App …` on an iPhone simulator, no signing |
| Gradle with `kotlin("multiplatform")` | kmp | `./gradlew assembleDebug testDebugUnitTest` |
| Other Gradle | android | `./gradlew assembleDebug testDebugUnitTest` |

Each job has a 15-minute limit. Keep the template's build setup intact and you'll be fine.

## Scoring (out of 25)

| Area | Points | Decided by |
|---|---|---|
| Acceptance criteria | 10 (1 each) | The grader, from your code (and your screenshot for AC2) |
| Visual match | 6 | The grader compares `screenshot.png` with the reference: layout 2, digit states and colours 2, typography and copy 2 |
| Code quality | 4 | The grader: structure, state handling, no hard-coding to pass checks, sensible naming |
| Builds in CI | 2 | CI |
| Tests exist and pass | 2 | CI |
| Prompt ≤ 600 characters | 1 | A script counts the characters in `PROMPT.md` |

- If your build fails, you can still earn code-quality points and up to 5 acceptance-criteria points.
- An invalid submission scores 0, with a comment explaining why.
- The copy list in issue #1 wins over the copy in the reference image.

## FAQ

**Can I edit the code after the prompt?** Yes. But the grader flags code that plainly couldn't have come from your prompt, and the top 3 must show their chat history.

**Can I use any AI tool?** Yes, any tool, as long as you send it one prompt.

**Can I push more than once?** Yes. Your latest graded push counts, even if it scores lower than an earlier one.

**My PR shows "build_failed". What now?** Open the "Build" check on your PR, read the log, fix it and push again.

**What happens after the deadline?** Pushes are built but not graded. Your last graded score stands.

**How are ties broken?** The earliest graded run wins.

**Can I hide instructions for the grader in my code?** No. The grader is told to ignore them and flag them, and it's grounds for disqualification.
