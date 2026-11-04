#!/usr/bin/env python3
import argparse
import base64
import io
import json
import os
import re
import subprocess
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
import zipfile
from dataclasses import dataclass, field
from datetime import datetime, timezone
from pathlib import Path
from typing import Any, Optional

REPO_ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(REPO_ROOT / "scripts"))

from validate_submission import TEAM_NAME, SubmissionFiles, Validation, team_folders, validate

MODEL = "claude-sonnet-5-5"
PRICE_PER_MTOK = {"input": 2.00, "output": 10.00, "cache_write": 2.50, "cache_read": 0.20}
COMMENT_MARKER = "<!-- hakkahakka26-grade -->"
SCORES_BRANCH = "scores"
LEADERBOARD_PATH = "leaderboard.json"
CONFIG_PATH = "config.json"
MAX_DIFF_CHARS = 240_000
MAX_LOG_CHARS = 8_000
MAX_SCREENSHOT_EDGE = 1600
MAX_IMAGE_PIXELS = 40_000_000
PROMPT_LIMIT = 600
BUILD_FAILED_AC_CAP = 5
SOURCE_EXTENSIONS = {".kt", ".kts", ".dart", ".ts", ".tsx", ".js", ".jsx", ".swift", ".java", ".xml", ".md"}
SKIPPED_NAMES = {
    "package-lock.json", "yarn.lock", "pnpm-lock.yaml", "pubspec.lock", "Podfile.lock",
    "Package.resolved", "gradle.lockfile", "gradlew", "gradlew.bat", "project.pbxproj",
}
SKIPPED_DIRS = {"build", ".gradle", "node_modules", ".dart_tool", "Pods", "DerivedData", "generated", ".kotlin", ".idea"}
BINARY_EXTENSIONS = {
    ".png", ".jpg", ".jpeg", ".gif", ".webp", ".mp4", ".mov", ".jar", ".apk", ".aab", ".ttf", ".otf",
    ".ico", ".zip", ".so", ".a", ".keystore", ".jks", ".pdf", ".svg",
}
SUBMISSION_TAG = re.compile(r"</?\s*submission\b", re.IGNORECASE)

OUTPUT_SCHEMA = {
    "type": "object",
    "properties": {
        "acceptance": {
            "type": "array",
            "items": {
                "type": "object",
                "properties": {
                    "id": {"type": "integer", "enum": list(range(1, 11))},
                    "pass": {"type": "boolean"},
                    "reason": {"type": "string"},
                },
                "required": ["id", "pass", "reason"],
                "additionalProperties": False,
            },
        },
        "visual": {
            "type": "object",
            "properties": {
                "layout": {"type": "integer", "enum": [0, 1, 2]},
                "states": {"type": "integer", "enum": [0, 1, 2]},
                "type_copy": {"type": "integer", "enum": [0, 1, 2]},
                "reason": {"type": "string"},
            },
            "required": ["layout", "states", "type_copy", "reason"],
            "additionalProperties": False,
        },
        "code_quality": {
            "type": "object",
            "properties": {
                "score": {"type": "integer", "enum": [0, 1, 2, 3, 4]},
                "reason": {"type": "string"},
            },
            "required": ["score", "reason"],
            "additionalProperties": False,
        },
        "flags": {
            "type": "array",
            "items": {
                "type": "string",
                "enum": ["possible_prompt_injection", "prompt_code_mismatch", "screenshot_mismatch", "other"],
            },
        },
        "summary": {"type": "string"},
    },
    "required": ["acceptance", "visual", "code_quality", "flags", "summary"],
    "additionalProperties": False,
}


class GitHub:
    def __init__(self, repo: str, token: str):
        self.repo = repo
        self.token = token

    def request(self, method: str, path: str, body: Any = None, accept: str = "application/vnd.github+json") -> Any:
        url = path if path.startswith("https://") else f"https://api.github.com{path}"
        data = json.dumps(body).encode() if body is not None else None
        request = urllib.request.Request(url, data=data, method=method)
        request.add_header("Authorization", f"Bearer {self.token}")
        request.add_header("Accept", accept)
        request.add_header("X-GitHub-Api-Version", "2022-11-28")
        if data is not None:
            request.add_header("Content-Type", "application/json")
        with urllib.request.urlopen(request, timeout=60) as response:
            payload = response.read()
        if accept.endswith("raw") or accept.endswith("diff"):
            return payload
        return json.loads(payload) if payload else None

    def get(self, path: str, **kwargs: Any) -> Any:
        return self.request("GET", path, **kwargs)

    def paginate(self, path: str, limit: int = 3000) -> list[Any]:
        items: list[Any] = []
        page = 1
        separator = "&" if "?" in path else "?"
        while len(items) < limit:
            batch = self.get(f"{path}{separator}per_page=100&page={page}")
            if not batch:
                break
            items.extend(batch)
            if len(batch) < 100:
                break
            page += 1
        return items

    def contents(self, path: str, ref: str) -> Any:
        quoted = urllib.parse.quote(path)
        return self.get(f"/repos/{self.repo}/contents/{quoted}?ref={urllib.parse.quote(ref)}")

    def raw(self, path: str, ref: str) -> bytes:
        quoted = urllib.parse.quote(path)
        return self.get(
            f"/repos/{self.repo}/contents/{quoted}?ref={urllib.parse.quote(ref)}",
            accept="application/vnd.github.raw",
        )


@dataclass
class BuildResult:
    stack: str = "unknown"
    build_ok: bool = False
    tests_ok: bool = False
    tests_found: bool = False
    build_log_tail: str = ""
    test_log_tail: str = ""


@dataclass
class Score:
    total: int = 0
    breakdown: dict[str, int] = field(default_factory=dict)
    acceptance: list[dict[str, Any]] = field(default_factory=list)
    visual: dict[str, Any] = field(default_factory=dict)
    code_quality: dict[str, Any] = field(default_factory=dict)
    flags: list[str] = field(default_factory=list)
    summary: str = ""
    prompt_chars: int = 0


def log(message: str) -> None:
    print(message, file=sys.stderr, flush=True)


def now_iso() -> str:
    return datetime.now(timezone.utc).replace(microsecond=0).isoformat().replace("+00:00", "Z")


def parse_time(value: str) -> datetime:
    parsed = datetime.fromisoformat(value.strip().replace("Z", "+00:00"))
    if parsed.tzinfo is None:
        raise ValueError(f"Timestamp {value!r} has no timezone; use e.g. 2026-11-14T15:30:00+03:00")
    return parsed


def parse_result_json(raw: str) -> tuple[int, str, BuildResult]:
    data = json.loads(raw)
    if not isinstance(data, dict):
        raise ValueError("result.json is not an object")
    pr = data.get("pr_number")
    sha = data.get("head_sha")
    if not isinstance(pr, int) or isinstance(pr, bool) or pr <= 0:
        raise ValueError("result.json has no valid pr_number")
    if not isinstance(sha, str) or not re.fullmatch(r"[0-9a-f]{40}", sha):
        raise ValueError("result.json has no valid head_sha")

    def boolean(key: str) -> bool:
        return data.get(key) is True

    def text(key: str, limit: int) -> str:
        value = data.get(key)
        return value[-limit:] if isinstance(value, str) else ""

    stack = data.get("stack") if data.get("stack") in {"android", "kmp", "flutter", "react-native", "ios"} else "unknown"
    return pr, sha, BuildResult(
        stack=stack,
        build_ok=boolean("build_ok"),
        tests_ok=boolean("tests_ok"),
        tests_found=boolean("tests_found"),
        build_log_tail=text("build_log_tail", MAX_LOG_CHARS),
        test_log_tail=text("test_log_tail", MAX_LOG_CHARS),
    )


def api_submission_files(github: GitHub, ref: str, folder: str) -> SubmissionFiles:
    listing_cache: dict[str, dict[str, Any]] = {}

    def listing(directory: str) -> dict[str, Any]:
        if directory not in listing_cache:
            try:
                entries = github.contents(directory, ref)
                listing_cache[directory] = {e["name"]: e for e in entries} if isinstance(entries, list) else {}
            except urllib.error.HTTPError:
                listing_cache[directory] = {}
        return listing_cache[directory]

    def entry(path: str) -> Optional[dict[str, Any]]:
        directory, _, name = path.rpartition("/")
        return listing(directory).get(name)

    def read_bytes(path: str) -> Optional[bytes]:
        found = entry(path)
        if not found or found.get("type") != "file":
            return None
        return github.raw(path, ref)

    return SubmissionFiles(
        exists=lambda path: (entry(path) or {}).get("type") == "file",
        is_dir=lambda path: (entry(path) or {}).get("type") == "dir",
        size=lambda path: (entry(path) or {}).get("size"),
        read_bytes=read_bytes,
    )


def template_blob_shas() -> set[str]:
    try:
        output = subprocess.run(
            ["git", "ls-tree", "-r", "HEAD", "templates"],
            cwd=REPO_ROOT, capture_output=True, text=True, check=True,
        ).stdout
    except (subprocess.CalledProcessError, FileNotFoundError):
        return set()
    return {line.split()[2] for line in output.splitlines() if len(line.split()) >= 3}


def is_skipped(path: str) -> Optional[str]:
    parts = path.split("/")
    name = parts[-1]
    suffix = Path(name).suffix.lower()
    if name in SKIPPED_NAMES:
        return "lock or generated file"
    if any(part in SKIPPED_DIRS for part in parts[:-1]):
        return "build or generated folder"
    if suffix in BINARY_EXTENSIONS:
        return "binary"
    return None


def build_diff(files: list[dict[str, Any]], template_shas: set[str]) -> tuple[str, list[str]]:
    notes: list[str] = []
    unchanged = skipped = 0
    chunks: list[tuple[int, str]] = []
    for item in files:
        path = item.get("filename", "")
        if item.get("sha") in template_shas and item.get("status") == "added":
            unchanged += 1
            continue
        if is_skipped(path) or "patch" not in item:
            skipped += 1
            continue
        priority = 0 if Path(path).suffix.lower() in SOURCE_EXTENSIONS else 1
        chunks.append((priority, f"--- {path} ({item.get('status')})\n{item['patch']}\n"))

    chunks.sort(key=lambda chunk: chunk[0])
    diff = ""
    omitted = 0
    for _, chunk in chunks:
        if len(diff) + len(chunk) > MAX_DIFF_CHARS:
            omitted += 1
            continue
        diff += chunk
    if unchanged:
        notes.append(f"{unchanged} files identical to a starter template were left out.")
    if skipped:
        notes.append(f"{skipped} lock, generated, binary or oversized files were left out.")
    if omitted:
        notes.append(f"TRUNCATED: {omitted} changed files were left out to stay within about 60k tokens.")
    return diff, notes


def neutralise(text: str) -> str:
    return SUBMISSION_TAG.sub(lambda match: match.group(0).replace("<", "&lt;"), text)


def png_block(data: bytes) -> dict[str, Any]:
    from PIL import Image

    Image.MAX_IMAGE_PIXELS = MAX_IMAGE_PIXELS
    with Image.open(io.BytesIO(data)) as image:
        image = image.convert("RGBA") if image.mode not in ("RGB", "RGBA") else image.copy()
        image.thumbnail((MAX_SCREENSHOT_EDGE, MAX_SCREENSHOT_EDGE))
        buffer = io.BytesIO()
        image.save(buffer, format="PNG", optimize=True)
    return {
        "type": "image",
        "source": {"type": "base64", "media_type": "image/png", "data": base64.b64encode(buffer.getvalue()).decode()},
    }


def build_messages(
    prompt_text: str, prompt_chars: int, diff: str, diff_notes: list[str], screenshot: Optional[bytes], build: BuildResult
) -> tuple[str, list[dict[str, Any]]]:
    system = (REPO_ROOT / "grader/system_prompt.md").read_text()
    challenge = (REPO_ROOT / "docs/issue-1.md").read_text()
    rubric = (REPO_ROOT / "grader/rubric.md").read_text()
    reference = (REPO_ROOT / "reference/otp-reference.png").read_bytes()

    reference_block = png_block(reference)
    reference_block["cache_control"] = {"type": "ephemeral"}
    content: list[dict[str, Any]] = [
        {"type": "text", "text": f"<challenge>\n{challenge}\n</challenge>\n\n<rubric>\n{rubric}\n</rubric>\n\n<reference_image>"},
        reference_block,
        {"type": "text", "text": "</reference_image>"},
    ]

    screenshot_block: Optional[dict[str, Any]] = None
    screenshot_note = "missing"
    if screenshot:
        try:
            screenshot_block = png_block(screenshot)
            screenshot_note = "attached below"
        except Exception as error:
            screenshot_note = f"unreadable ({type(error).__name__})"

    facts = (
        f"Facts computed by the CI script (trusted):\n"
        f"- stack: {build.stack}\n- build_ok: {build.build_ok}\n- tests_ok: {build.tests_ok}\n"
        f"- tests_found: {build.tests_found}\n- PROMPT.md length: {prompt_chars} characters\n"
        f"- screenshot: {screenshot_note}\n"
        + "".join(f"- diff note: {note}\n" for note in diff_notes)
    )
    content.append({"type": "text", "text": facts})
    content.append({"type": "text", "text": "<submission>\n<screenshot>"})
    if screenshot_block:
        content.append(screenshot_block)
    content.append({
        "type": "text",
        "text": (
            "</screenshot>\n"
            f"<prompt_md>\n{neutralise(prompt_text)}\n</prompt_md>\n"
            f"<build_log_tail>\n{neutralise(build.build_log_tail)}\n</build_log_tail>\n"
            f"<test_log_tail>\n{neutralise(build.test_log_tail)}\n</test_log_tail>\n"
            f"<diff>\n{neutralise(diff)}\n</diff>\n"
            "</submission>\n\n"
            "Grade this submission against the rubric. Return all 10 acceptance criteria, ids 1 to 10."
        ),
    })
    return system, [{"role": "user", "content": content}]


def call_model(system: str, messages: list[dict[str, Any]]) -> tuple[dict[str, Any], dict[str, Any]]:
    import anthropic

    client = anthropic.Anthropic(max_retries=2, timeout=300)
    started = time.monotonic()
    response = client.beta.messages.create(
        model=MODEL,
        max_tokens=16000,
        system=system,
        messages=messages,
        output_config={"effort": "high", "format": {"type": "json_schema", "schema": OUTPUT_SCHEMA}},
        betas=["server-side-fallback-2026-07-01"],
        extra_body={"fallbacks": "default"},
    )
    elapsed = time.monotonic() - started
    if response.stop_reason == "refusal":
        raise RuntimeError("The model declined to grade this submission.")
    if response.stop_reason == "max_tokens":
        raise RuntimeError("The model ran out of output tokens before finishing the grade.")
    text = next(block.text for block in response.content if block.type == "text")
    usage = response.usage
    tokens = {
        "input": usage.input_tokens or 0,
        "output": usage.output_tokens or 0,
        "cache_write": getattr(usage, "cache_creation_input_tokens", 0) or 0,
        "cache_read": getattr(usage, "cache_read_input_tokens", 0) or 0,
    }
    cost = sum(tokens[key] * PRICE_PER_MTOK[key] / 1_000_000 for key in tokens)
    meta = {"model": response.model, "tokens": tokens, "cost_usd": round(cost, 4), "seconds": round(elapsed, 1)}
    return json.loads(text), meta


def score_submission(model_output: dict[str, Any], build: BuildResult, prompt_chars: int, valid: bool) -> Score:
    by_id: dict[int, dict[str, Any]] = {}
    for item in model_output.get("acceptance", []):
        if isinstance(item, dict) and isinstance(item.get("id"), int) and 1 <= item["id"] <= 10 and item["id"] not in by_id:
            by_id[item["id"]] = {"id": item["id"], "pass": item.get("pass") is True, "reason": str(item.get("reason", ""))}
    acceptance = [by_id.get(i, {"id": i, "pass": False, "reason": "Not graded by the model."}) for i in range(1, 11)]

    def clamp(value: Any, low: int, high: int) -> int:
        return max(low, min(high, value)) if isinstance(value, int) and not isinstance(value, bool) else low

    visual = model_output.get("visual", {}) or {}
    code = model_output.get("code_quality", {}) or {}
    ac_points = sum(1 for item in acceptance if item["pass"])
    if not build.build_ok:
        ac_points = min(ac_points, BUILD_FAILED_AC_CAP)
    breakdown = {
        "ac": ac_points,
        "visual": clamp(visual.get("layout"), 0, 2) + clamp(visual.get("states"), 0, 2) + clamp(visual.get("type_copy"), 0, 2),
        "code": clamp(code.get("score"), 0, 4),
        "build": 2 if build.build_ok else 0,
        "tests": 2 if build.tests_ok and build.tests_found else 0,
        "prompt": 1 if 0 < prompt_chars <= PROMPT_LIMIT else 0,
    }
    total = sum(breakdown.values()) if valid else 0
    flags = sorted({flag for flag in model_output.get("flags", []) if isinstance(flag, str)})
    return Score(
        total=total,
        breakdown=breakdown,
        acceptance=acceptance,
        visual={
            "layout": clamp(visual.get("layout"), 0, 2),
            "states": clamp(visual.get("states"), 0, 2),
            "type_copy": clamp(visual.get("type_copy"), 0, 2),
            "reason": str(visual.get("reason", "")),
        },
        code_quality={"score": breakdown["code"], "reason": str(code.get("reason", ""))},
        flags=flags,
        summary=str(model_output.get("summary", "")),
        prompt_chars=prompt_chars,
    )


def one_line(text: str) -> str:
    return " ".join(text.split()).replace("|", "\\|")[:300]


def score_comment(team: str, head_sha: str, build: BuildResult, score: Score) -> str:
    b = score.breakdown
    rows = [
        f"| Acceptance criteria | {b['ac']}/10 | {'Build failed, so capped at 5. ' if not build.build_ok else ''}See below. |",
        f"| Visual match | {b['visual']}/6 | Layout {score.visual['layout']}/2, states {score.visual['states']}/2, type and copy {score.visual['type_copy']}/2: {one_line(score.visual['reason'])} |",
        f"| Code quality | {b['code']}/4 | {one_line(score.code_quality['reason'])} |",
        f"| Builds in CI | {b['build']}/2 | {'Build passed.' if build.build_ok else 'Build failed; open the Build check for the log.'} |",
        f"| Tests exist and pass | {b['tests']}/2 | {'Tests found and passed.' if b['tests'] else ('No tests found.' if not build.tests_found else 'Tests failed.')} |",
        f"| Prompt ≤ {PROMPT_LIMIT} characters | {b['prompt']}/1 | `PROMPT.md` is {score.prompt_chars} characters. |",
    ]
    ac_rows = [f"| {item['id']} | {'✅' if item['pass'] else '❌'} | {one_line(item['reason'])} |" for item in score.acceptance]
    flags = f"\n⚠ Flagged for organiser review: {', '.join(f'`{flag}`' for flag in score.flags)}. Flags don't change the score.\n" if score.flags else ""
    return (
        f"{COMMENT_MARKER}\n## hakkahakka26 score: **{score.total}/25**\n\n"
        f"Team `{team}` · stack `{build.stack}` · commit `{head_sha[:7]}`\n\n"
        f"{one_line(score.summary)}\n{flags}\n"
        "| Area | Points | Reason |\n|---|---|---|\n" + "\n".join(rows) + "\n\n"
        "<details><summary>Acceptance criteria</summary>\n\n"
        "| AC | Result | Reason |\n|---|---|---|\n" + "\n".join(ac_rows) + "\n\n</details>\n\n"
        "Push again to re-grade. Your latest graded push counts.\n"
    )


def message_comment(title: str, body: str) -> str:
    return f"{COMMENT_MARKER}\n## hakkahakka26: {title}\n\n{body}\n"


def upsert_comment(github: GitHub, pr: int, body: str) -> None:
    allowed = {"github-actions[bot]"}
    try:
        allowed.add(github.get("/user")["login"])
    except urllib.error.HTTPError:
        pass
    comments = github.paginate(f"/repos/{github.repo}/issues/{pr}/comments")
    mine = [c for c in comments if c.get("user", {}).get("login") in allowed and COMMENT_MARKER in (c.get("body") or "")]
    if mine:
        github.request("PATCH", f"/repos/{github.repo}/issues/comments/{mine[-1]['id']}", {"body": body})
    else:
        github.request("POST", f"/repos/{github.repo}/issues/{pr}/comments", {"body": body})


def read_json_file(github: GitHub, path: str) -> tuple[Any, Optional[str]]:
    try:
        meta = github.contents(path, SCORES_BRANCH)
    except urllib.error.HTTPError as error:
        if error.code == 404:
            return None, None
        raise
    return json.loads(base64.b64decode(meta["content"]).decode()), meta["sha"]


def write_json_file(github: GitHub, path: str, data: Any, sha: Optional[str], message: str) -> None:
    body: dict[str, Any] = {
        "message": message,
        "content": base64.b64encode((json.dumps(data, indent=2) + "\n").encode()).decode(),
        "branch": SCORES_BRANCH,
    }
    if sha:
        body["sha"] = sha
    github.request("PUT", f"/repos/{github.repo}/contents/{path}", body)


def update_leaderboard(github: GitHub, team_key: str, changes: dict[str, Any], keep_score: bool) -> None:
    for attempt in range(6):
        board, sha = read_json_file(github, LEADERBOARD_PATH)
        board = board if isinstance(board, list) else []
        existing = next((entry for entry in board if entry.get("team") == team_key), None)
        entry = dict(existing or {"team": team_key, "score": None, "breakdown": None, "graded_at": None, "flags": []})
        if keep_score:
            entry.update({k: v for k, v in changes.items() if k in ("status", "stack", "pr", "updated_at")})
        else:
            entry.update(changes)
        board = [e for e in board if e.get("team") != team_key] + [entry]
        board.sort(key=lambda e: (-(e.get("score") or -1), e.get("graded_at") or "~", e.get("team", "")))
        try:
            write_json_file(github, LEADERBOARD_PATH, board, sha, f"{team_key}: {entry.get('status')}")
            return
        except urllib.error.HTTPError as error:
            if error.code not in (409, 422) or attempt == 5:
                raise
            log(f"Leaderboard write conflict, retrying ({attempt + 1})")
            time.sleep(1 + attempt)


def sync_config(github: GitHub, deadline: Optional[str]) -> None:
    if not deadline:
        return
    config, sha = read_json_file(github, CONFIG_PATH)
    config = config if isinstance(config, dict) else {}
    if config.get("deadline") == deadline:
        return
    config["deadline"] = deadline
    try:
        write_json_file(github, CONFIG_PATH, config, sha, "Sync deadline from HACK_DEADLINE")
    except urllib.error.HTTPError as error:
        log(f"Couldn't sync config.json ({error.code}); the leaderboard countdown may be stale.")


def team_key_for(validation: Validation, pr: int) -> str:
    return validation.team if TEAM_NAME.match(validation.team) else f"pr-{pr}"


def find_pr_by_head(github: GitHub, head_sha: str) -> Optional[dict[str, Any]]:
    for pr in github.paginate(f"/repos/{github.repo}/pulls?state=open", limit=500):
        if pr.get("head", {}).get("sha") == head_sha:
            return pr
    return None


def mark_building(github: GitHub, head_sha: str) -> int:
    pr = find_pr_by_head(github, head_sha)
    if not pr:
        log("No open PR has this head commit; nothing to mark.")
        return 0
    files = [f["filename"] for f in github.paginate(f"/repos/{github.repo}/pulls/{pr['number']}/files")]
    teams = {path.split("/")[1] for path in files if path.startswith("submissions/") and path.count("/") >= 2}
    team = next(iter(teams)) if len(teams) == 1 and TEAM_NAME.match(next(iter(teams))) else f"pr-{pr['number']}"
    update_leaderboard(github, team, {"status": "building", "pr": pr["number"], "updated_at": now_iso()}, keep_score=True)
    log(f"Marked {team} (PR #{pr['number']}) as building.")
    return 0


def latest_build_result(github: GitHub, head_sha: str) -> Optional[str]:
    runs = github.get(f"/repos/{github.repo}/actions/workflows/build.yml/runs?head_sha={head_sha}&per_page=5")
    for run in runs.get("workflow_runs", []):
        artifacts = github.get(f"/repos/{github.repo}/actions/runs/{run['id']}/artifacts")
        artifact = next((a for a in artifacts.get("artifacts", []) if a["name"] == "result"), None)
        if not artifact:
            continue
        archive = github.request("GET", artifact["archive_download_url"], accept="application/vnd.github.raw")
        with zipfile.ZipFile(io.BytesIO(archive)) as bundle:
            return bundle.read("result.json").decode()
    return None


def grade(
    github: GitHub,
    pr_number: int,
    run_head_sha: Optional[str],
    result_raw: Optional[str],
    received_at: datetime,
    deadline: Optional[str],
    publish: bool,
) -> int:
    pr = github.get(f"/repos/{github.repo}/pulls/{pr_number}")
    head_sha = pr["head"]["sha"]
    if pr.get("base", {}).get("repo", {}).get("full_name") != github.repo:
        log("PR doesn't target this repository; refusing to grade.")
        return 1
    if run_head_sha and head_sha != run_head_sha:
        log(f"PR #{pr_number} head is {head_sha}, but the build ran on {run_head_sha}; a newer push will be graded instead.")
        return 0

    build = BuildResult()
    if result_raw:
        result_pr, result_sha, build = parse_result_json(result_raw)
        if result_pr != pr_number or result_sha != head_sha:
            log("result.json doesn't match this PR's head commit; ignoring its build results.")
            build = BuildResult()

    files = github.paginate(f"/repos/{github.repo}/pulls/{pr_number}/files")
    changed = [f["filename"] for f in files] + [f["previous_filename"] for f in files if f.get("previous_filename")]
    teams, _ = team_folders(changed)
    folder = f"submissions/{next(iter(teams))}" if len(teams) == 1 else "submissions/__none__"
    validation = validate(changed, api_submission_files(github, head_sha, folder))
    team = team_key_for(validation, pr_number)
    log(f"PR #{pr_number} team={team} valid={validation.valid} stack={build.stack} build_ok={build.build_ok}")

    def publish_status(changes: dict[str, Any], keep_score: bool = False) -> None:
        if publish:
            update_leaderboard(github, team, {"pr": pr_number, "stack": build.stack, "updated_at": now_iso(), **changes}, keep_score)

    def comment(body: str) -> None:
        print(body)
        if publish:
            upsert_comment(github, pr_number, body)

    if publish:
        sync_config(github, deadline)

    if deadline and received_at > parse_time(deadline):
        comment(message_comment("not graded", "Received after the deadline: built, not graded."))
        publish_status({"status": "late"}, keep_score=True)
        return 0

    if not validation.valid:
        errors = "\n".join(f"- {error}" for error in validation.errors)
        comment(message_comment(
            "score 0/25 (invalid submission)",
            f"This PR scores 0 because it doesn't follow the submission format:\n\n{errors}\n\nFix it and push again.",
        ))
        publish_status({
            "status": "invalid", "score": 0, "graded_at": now_iso(), "flags": [],
            "breakdown": {"ac": 0, "visual": 0, "code": 0, "build": 0, "tests": 0, "prompt": 0},
        })
        return 0

    publish_status({"status": "grading"}, keep_score=True)

    reader = api_submission_files(github, head_sha, validation.folder)
    prompt_text = (reader.read_bytes(f"{validation.folder}/PROMPT.md") or b"").decode("utf-8", errors="replace")
    prompt_chars = len(prompt_text.strip())
    screenshot = reader.read_bytes(f"{validation.folder}/screenshot.png")
    diff, notes = build_diff(files, template_blob_shas())
    system, messages = build_messages(prompt_text, prompt_chars, diff, notes, screenshot, build)

    try:
        model_output, meta = call_model(system, messages)
    except Exception as error:
        log(f"Grading failed: {type(error).__name__}: {error}")
        comment(message_comment("grading failed", "The grader couldn't score this push. The organisers have been notified; push again or wait for a re-run."))
        publish_status({"status": "error"}, keep_score=True)
        return 1

    score = score_submission(model_output, build, prompt_chars, validation.valid)
    log(f"Model {meta['model']} took {meta['seconds']}s, tokens {meta['tokens']}, cost ${meta['cost_usd']}")
    summary_path = os.environ.get("GITHUB_STEP_SUMMARY")
    if summary_path:
        with open(summary_path, "a", encoding="utf-8") as handle:
            handle.write(f"PR #{pr_number} `{team}`: **{score.total}/25** · {meta['model']} · {meta['seconds']}s · "
                         f"tokens {meta['tokens']} · ${meta['cost_usd']}\n")

    comment(score_comment(team, head_sha, build, score))
    publish_status({
        "status": "scored" if build.build_ok else "build_failed",
        "score": score.total,
        "breakdown": score.breakdown,
        "graded_at": now_iso(),
        "flags": score.flags,
    })
    return 0


def main() -> int:
    parser = argparse.ArgumentParser(description="Grade a hakkahakka26 PR with Claude.")
    parser.add_argument("--pr", type=int, help="PR number to grade (local backup mode).")
    parser.add_argument("--result", type=Path, help="result.json from the Build workflow.")
    parser.add_argument("--run-head-sha", help="Head SHA the Build workflow ran on.")
    parser.add_argument("--received-at", help="When the push was received (ISO-8601); defaults to now.")
    parser.add_argument("--mark-building", metavar="HEAD_SHA", help="Only mark the PR with this head commit as building.")
    parser.add_argument("--dry-run", action="store_true", help="Print the score; don't comment or touch the leaderboard.")
    parser.add_argument("--repo", default=os.environ.get("GITHUB_REPOSITORY", "MamboBryan/hakka26"))
    args = parser.parse_args()

    token = os.environ.get("GITHUB_TOKEN") or os.environ.get("GH_TOKEN")
    if not token:
        parser.error("Set GITHUB_TOKEN (or GH_TOKEN), e.g. GITHUB_TOKEN=$(gh auth token).")
    github = GitHub(args.repo, token)

    if args.mark_building:
        return mark_building(github, args.mark_building)

    result_raw = args.result.read_text() if args.result and args.result.exists() else None
    pr_number = args.pr
    if pr_number is None:
        if not result_raw:
            log("No result.json (the build was probably cancelled by a newer push); nothing to grade.")
            return 0
        try:
            pr_number = parse_result_json(result_raw)[0]
        except (ValueError, json.JSONDecodeError) as error:
            log(f"Unusable result.json: {error}")
            return 0
    elif result_raw is None:
        head = github.get(f"/repos/{github.repo}/pulls/{pr_number}")["head"]["sha"]
        result_raw = latest_build_result(github, head)
        if result_raw is None:
            log("No Build result found for this PR's head commit; grading with build and tests marked as failed.")

    received_at = parse_time(args.received_at) if args.received_at else datetime.now(timezone.utc)
    deadline = os.environ.get("HACK_DEADLINE") or None
    return grade(github, pr_number, args.run_head_sha, result_raw, received_at, deadline, publish=not args.dry_run)


if __name__ == "__main__":
    sys.exit(main())
