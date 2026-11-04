# hakkahakka26 rules

## Teams

- 20 teams, **maximum 3 people per team**.
- Any mobile stack. Starter templates are in `templates/`, but you may use your own as long as CI can build it (see the README for what CI runs).

## The one-prompt rule

- Each team gets **one AI prompt** to build the screen described in issue #1.
- Commit that prompt, verbatim, as `submissions/<team-name>/PROMPT.md`. It must be **600 characters or fewer** to earn the prompt point.
- You may edit the generated code by hand afterwards. The grader flags code that plainly couldn't have come from your prompt.
- The top 3 must show their AI chat history during their demo to prove the one-prompt rule.

## Time

- The build window is **30 minutes**, from the moment issue #1 opens on stage.
- The deadline is checked against the time a build starts. Pushes after the deadline are built but **not graded**. Your last graded result stands.

## Submitting

- Open a PR from your fork. It must only touch files inside one `submissions/<team-name>/` folder.
- Each team submits from one PR. Push more commits to the same PR to improve your score; the **latest graded** result counts, even if it is lower than an earlier one.
- PRs are never merged.

## Scoring

- Claude Sonnet grades every PR out of 25 against `grader/rubric.md` and posts the score as a PR comment. The live leaderboard is on GitHub Pages.
- **Winner = highest AI score.** Tie-break: the earliest graded run.
- The top 3 demo for 3 minutes each, showing the running app and their AI chat history.
- Prize: **KES 42,000**.

## Scores of 0

Your PR scores 0, with a comment explaining why, if:

- it touches files outside one `submissions/<team-name>/` folder,
- the team name breaks the naming rule,
- a required file is missing, or
- `TEAM.md` lists more than 3 people.

Fix it and push again; a fixed PR is graded normally.

## Disqualification

The organisers may disqualify a team for:

- using more than one AI prompt, or being unable to show the chat history when asked,
- trying to manipulate the grader (for example, instructions to the AI hidden in code, comments, the prompt or the screenshot),
- trying to attack or abuse the CI, the repository or other teams' submissions,
- submitting another team's work, or
- having more than 3 people work on the submission.

Flags raised by the grader (shown as ⚠ on the leaderboard) never change a score automatically. The organisers review them, and their decision is final.
