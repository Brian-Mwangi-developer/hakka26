#!/usr/bin/env python3
import argparse
import json
import os
import re
import sys
from dataclasses import dataclass, field
from pathlib import Path
from typing import Callable, Iterable, Optional

TEAM_NAME = re.compile(r"^[a-z0-9-]{3,30}$")
SUBMISSIONS = "submissions/"
MAX_PEOPLE = 3
MAX_DEMO_BYTES = 10 * 1024 * 1024
PNG_MAGIC = b"\x89PNG\r\n\x1a\n"
BULLET = re.compile(r"^\s*(?:[-*+]|\d+[.)])\s+\S")


@dataclass
class Validation:
    valid: bool = False
    team: str = ""
    folder: str = ""
    errors: list[str] = field(default_factory=list)


@dataclass
class SubmissionFiles:
    exists: Callable[[str], bool]
    is_dir: Callable[[str], bool]
    size: Callable[[str], Optional[int]]
    read_bytes: Callable[[str], Optional[bytes]]


def count_people(team_md: str) -> int:
    lines = [line for line in team_md.splitlines() if line.strip()]
    bullets = [line for line in lines if BULLET.match(line)]
    if bullets:
        return len(bullets)
    return len([line for line in lines if not line.lstrip().startswith("#")])


def team_folders(changed_files: Iterable[str]) -> tuple[set[str], list[str]]:
    teams: set[str] = set()
    outside: list[str] = []
    for path in changed_files:
        parts = path.split("/")
        if path.startswith(SUBMISSIONS) and len(parts) >= 3 and parts[1]:
            teams.add(parts[1])
        else:
            outside.append(path)
    return teams, outside


def validate(changed_files: list[str], files: SubmissionFiles) -> Validation:
    result = Validation()
    if not changed_files:
        result.errors.append("The PR doesn't change any files.")
        return result

    teams, outside = team_folders(changed_files)
    if outside:
        shown = ", ".join(sorted(outside)[:5])
        more = f" and {len(outside) - 5} more" if len(outside) > 5 else ""
        result.errors.append(f"The PR touches files outside `submissions/<team-name>/`: {shown}{more}.")
    if len(teams) > 1:
        result.errors.append(f"The PR touches more than one team folder: {', '.join(sorted(teams))}.")
    if len(teams) != 1:
        if not teams:
            result.errors.append("The PR doesn't add a `submissions/<team-name>/` folder.")
        return result

    team = teams.pop()
    folder = f"{SUBMISSIONS}{team}"
    result.team = team
    result.folder = folder

    if not TEAM_NAME.match(team):
        result.errors.append(
            f"Team name `{team}` must be lowercase, use only a-z, 0-9 and -, and be 3–30 characters long."
        )

    for name in ("PROMPT.md", "TEAM.md", "screenshot.png"):
        if not files.exists(f"{folder}/{name}"):
            result.errors.append(f"`{folder}/{name}` is missing.")

    demos = [name for name in ("demo.gif", "demo.mp4") if files.exists(f"{folder}/{name}")]
    if not demos:
        result.errors.append(f"`{folder}/demo.gif` or `{folder}/demo.mp4` is missing.")
    for name in demos:
        size = files.size(f"{folder}/{name}")
        if size is not None and size > MAX_DEMO_BYTES:
            result.errors.append(f"`{name}` is {size / 1024 / 1024:.1f} MB; the limit is 10 MB.")

    if not files.is_dir(f"{folder}/app"):
        result.errors.append(f"`{folder}/app/` is missing.")

    screenshot = files.read_bytes(f"{folder}/screenshot.png") if files.exists(f"{folder}/screenshot.png") else None
    if screenshot is not None and not screenshot.startswith(PNG_MAGIC):
        result.errors.append("`screenshot.png` isn't a PNG image.")

    team_md = files.read_bytes(f"{folder}/TEAM.md") if files.exists(f"{folder}/TEAM.md") else None
    if team_md is not None:
        people = count_people(team_md.decode("utf-8", errors="replace"))
        if people > MAX_PEOPLE:
            result.errors.append(f"`TEAM.md` lists {people} people; the maximum is {MAX_PEOPLE}.")
        if people == 0:
            result.errors.append("`TEAM.md` doesn't list anyone.")

    result.valid = not result.errors
    return result


def local_files(root: Path) -> SubmissionFiles:
    def resolve(path: str) -> Path:
        return root / path

    def size(path: str) -> Optional[int]:
        target = resolve(path)
        return target.stat().st_size if target.is_file() else None

    def read_bytes(path: str) -> Optional[bytes]:
        target = resolve(path)
        return target.read_bytes() if target.is_file() and not target.is_symlink() else None

    return SubmissionFiles(
        exists=lambda path: resolve(path).is_file() and not resolve(path).is_symlink(),
        is_dir=lambda path: resolve(path).is_dir() and not resolve(path).is_symlink(),
        size=size,
        read_bytes=read_bytes,
    )


def write_github_output(result: Validation) -> None:
    output = os.environ.get("GITHUB_OUTPUT")
    if not output:
        return
    with open(output, "a", encoding="utf-8") as handle:
        handle.write(f"valid={str(result.valid).lower()}\n")
        handle.write(f"team={result.team if TEAM_NAME.match(result.team) else ''}\n")
        handle.write(f"folder={result.folder if TEAM_NAME.match(result.team) else ''}\n")


def main() -> int:
    parser = argparse.ArgumentParser(description="Validate a hakkahakka26 submission PR.")
    parser.add_argument("--changed-files", required=True, type=Path, help="File with one changed path per line.")
    parser.add_argument("--root", default=".", type=Path, help="Checkout root.")
    parser.add_argument("--json-out", type=Path, help="Where to write the validation result as JSON.")
    args = parser.parse_args()

    changed = [line.strip() for line in args.changed_files.read_text().splitlines() if line.strip()]
    result = validate(changed, local_files(args.root))

    payload = json.dumps(result.__dict__, indent=2)
    print(payload)
    if args.json_out:
        args.json_out.write_text(payload)
    write_github_output(result)
    return 0


if __name__ == "__main__":
    sys.exit(main())
