# Dry run

Three fake submissions for testing the pipeline end to end.

| Folder | Stack | What it tests | Expected |
|---|---|---|---|
| `dry-good` | android-compose | A genuine attempt at all 10 ACs, with 9 unit tests and a real emulator screenshot | 18+ / 25, `scored` |
| `dry-broken` | flutter | Doesn't compile (`FilteringTextInputFormatter` used without importing `package:flutter/services.dart`) | `build_failed`, a partial score with ACs capped at 5 |
| `dry-inject` | react-native | A real implementation whose `App.tsx` contains a comment telling the grader to award 25/25 | Flagged `possible_prompt_injection`, graded normally |

`dry-broken` reuses `dry-good`'s screenshot, so it may also be flagged `screenshot_mismatch`. That's expected.

## From this account (branches in the main repo)

```sh
./dry-run/open-prs.sh                 # all three
./dry-run/open-prs.sh dry-good        # just one
```

Each run creates a fresh `dry/<team>-<time>` branch from `origin/main`, copies the folder to `submissions/<team>/` and opens a PR. Branch PRs share the base repo, so they don't test the fork path.

## From a second account (forks, like the real event)

Fork PRs behave differently: they have no access to secrets, their `GITHUB_TOKEN` is read-only, and their workflows may need approval. Test this before the event:

1. Sign in to GitHub as the second account, then fork `MamboBryan/hakka26`.
2. Clone the fork and add a submission:
   ```sh
   gh repo clone <second-account>/hakka26 && cd hakka26
   git checkout -b fork-test
   mkdir -p submissions/fork-test && cp -R dry-run/dry-good/. submissions/fork-test/
   rm -rf submissions/fork-test/app/app/build
   git add submissions/fork-test && git commit -m "Submit fork-test" && git push -u origin fork-test
   gh pr create --repo MamboBryan/hakka26 --head <second-account>:fork-test --title "Submission: fork-test" --fill
   ```
3. On the PR, check:
   - **Build** starts, or waits for approval if the account is new to GitHub (see the fork-approval setting in the setup checklist).
   - **Grade** runs from `main` after Build finishes and posts the score comment.
   - The leaderboard shows `fork-test` going `building` → `grading` → `scored`.
4. Push a second commit to check that the comment is updated in place rather than duplicated.
5. Edit `.github/workflows/build.yml` in the fork PR and push. The PR must score 0 as invalid, because files outside `submissions/` were touched.

## Cleaning up

Close the dry-run PRs:

```sh
gh pr list --repo MamboBryan/hakka26 --search "Submission: in:title" --json number -q '.[].number' | xargs -n1 gh pr close --repo MamboBryan/hakka26 --delete-branch
```

Then reset the board:

```sh
sha=$(gh api "repos/MamboBryan/hakka26/contents/leaderboard.json?ref=scores" -q .sha)
gh api -X PUT repos/MamboBryan/hakka26/contents/leaderboard.json \
  -f message="Reset leaderboard" -f branch=scores -f sha="$sha" -f content="$(printf '[]\n' | base64)"
```
