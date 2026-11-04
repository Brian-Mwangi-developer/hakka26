#!/usr/bin/env bash
set -euo pipefail

repo_root="$(git rev-parse --show-toplevel)"
cd "$repo_root"
if [ $# -gt 0 ]; then teams=("$@"); else teams=(dry-good dry-broken dry-inject); fi
stamp="$(date +%H%M%S)"

for team in "${teams[@]}"; do
  branch="dry/$team-$stamp"
  worktree="$(mktemp -d)"
  git worktree add -q -b "$branch" "$worktree" origin/main
  mkdir -p "$worktree/submissions/$team"
  rsync -a --exclude node_modules --exclude build --exclude .gradle --exclude .kotlin --exclude .cxx \
    --exclude local.properties --exclude .dart_tool "dry-run/$team/" "$worktree/submissions/$team/"
  git -C "$worktree" add "submissions/$team"
  git -C "$worktree" commit -q -m "Submit $team"
  git -C "$worktree" push -q -u origin "$branch"
  gh pr create --repo "$(gh repo view --json nameWithOwner -q .nameWithOwner)" --base main --head "$branch" \
    --title "Submission: $team" --body-file .github/pull_request_template.md
  git worktree remove --force "$worktree"
  echo "Opened PR for $team at $(date -u +%H:%M:%SZ)"
done
