#!/usr/bin/env python3
"""Checks lesson files before they are published (tools/content/publish.py runs it first).

Usage:  python3 tools/content/validate.py <file-or-folder> [...]

Accepts book files ({"id", "title", "chapters": [...]}), chapter files ({"lessons": [...]}),
master question files ({"questions": [...]}) and image-prompt files ({"items": [...]}).
Exits with an error, listing every problem, if anything would break or mislead in the app.
"""
import json, pathlib, re, sys

sys.path.insert(0, str(pathlib.Path(__file__).parent))
from taxonomy import load

_, TAGS = load()
ID = re.compile(r"[a-z0-9][a-z0-9-]*")
MEDIA = re.compile(r"(gh:[\w-]+(/[\w.-]+)+|drive:[\w-]{10,}|asset:.+|https://.+)")
problems = []

def bad(where, msg):
    problems.append(f"{where}: {msg}")

def check_lesson(lesson, where, strict_tags=True):
    lid = lesson.get("id", "")
    where = f"{where} lesson '{lid}'"
    if not ID.fullmatch(lid):
        bad(where, "id must be lowercase letters, digits and dashes")
    if not lesson.get("title"):
        bad(where, "missing title")
    image = lesson.get("image")
    if image and not MEDIA.fullmatch(image):
        bad(where, f"image '{image}' is not gh:<path>, drive:<id>, asset:<path> or https://")
    tags = lesson.get("tags", [])
    for t in tags:
        if t not in TAGS:
            bad(where, f"unknown tag '{t}' (add it to content/taxonomy.txt first, or fix the spelling)")
    if strict_tags:
        topics = [t for t in tags if ":" not in t]
        if not 1 <= len(topics) <= 2:
            bad(where, "needs 1 or 2 topic tags")
        for group, lo, hi in (("d:", 1, 2), ("q:", 1, 1), ("l:", 1, 1)):
            n = sum(t.startswith(group) for t in tags)
            if not lo <= n <= hi:
                bad(where, f"needs {lo}-{hi} '{group}' tag(s), has {n}")
    tracks = lesson.get("tracks", [])
    if not tracks:
        bad(where, "no narration tracks")
    for track in tracks:
        kind = track.get("kind")
        if kind not in ("story", "explanation"):
            bad(where, f"track kind '{kind}' must be story or explanation")
        segments = track.get("segments", [])
        if not segments:
            bad(where, f"{kind} track has no segments")
        audio = track.get("audio")
        if audio and not MEDIA.fullmatch(audio):
            bad(where, f"audio '{audio}' is not a media reference")
        last = -1
        for i, seg in enumerate(segments):
            if not seg.get("text", "").strip():
                bad(where, f"{kind} segment {i} has no text")
            spot = seg.get("spot")
            if spot and not (0 <= spot.get("x", -1) <= 1 and 0 <= spot.get("y", -1) <= 1 and 0.03 <= spot.get("r", 0.15) <= 0.6):
                bad(where, f"{kind} segment {i} spot is outside the picture or has a bad radius")
            quote = seg.get("quote")
            if quote is not None and not (isinstance(quote, str) and quote.strip()):
                bad(where, f"{kind} segment {i} has an empty 'quote' (it is the quotation's source, e.g. author and work)")
            if quote and i != len(segments) - 1:
                bad(where, f"{kind} segment {i} is a quotation but not the last sentence of the track")
            if spot and not image:
                bad(where, f"{kind} segment {i} has a spot but the lesson has no image")
            if audio:
                t = seg.get("t")
                if t is None or t <= last and i > 0:
                    bad(where, f"{kind} segment {i} needs an increasing start time 't' because the track has audio")
                last = t if t is not None else last
    for i, q in enumerate(lesson.get("quiz", [])):
        options = q.get("options", [])
        if not q.get("q", "").strip() or len(options) < 2:
            bad(where, f"question {i} needs a stem and at least two options")
        if not isinstance(q.get("answer"), int) or not 0 <= q["answer"] < len(options):
            bad(where, f"question {i} answer index is out of range")
        if len(set(options)) != len(options):
            bad(where, f"question {i} has duplicate options")
        figure = q.get("image")
        if figure is not None and not (isinstance(figure, str) and MEDIA.fullmatch(figure)):
            bad(where, f"question {i} image '{figure}' is not gh:<path>, drive:<id of the PUBLIC copy>, asset:<path> or https://")

def check_lessons(lessons, where, strict):
    seen = set()
    for lesson in lessons:
        if lesson.get("id") in seen:
            bad(where, f"duplicate lesson id '{lesson.get('id')}'")
        seen.add(lesson.get("id"))
        check_lesson(lesson, where, strict)

def check_file(path):
    try:
        data = json.loads(path.read_text(encoding="utf-8"))
    except Exception as e:
        return bad(path.name, f"not valid JSON: {e}")
    if "chapters" in data:
        if not ID.fullmatch(data.get("id", "")) or not data.get("title"):
            bad(path.name, "book needs an id (lowercase, dashes) and a title")
        strict = data.get("id") != "starter"
        seen = set()
        for ch in data["chapters"]:
            cid = ch.get("id", "")
            if not ID.fullmatch(cid) or cid in seen:
                bad(path.name, f"chapter id '{cid}' is invalid or repeated")
            seen.add(cid)
            if ch.get("file") and ch.get("lessons"):
                bad(path.name, f"chapter '{cid}' has both a file and inline lessons")
            check_lessons(ch.get("lessons", []), f"{path.name} chapter '{cid}'", strict)
    elif "lessons" in data:
        check_lessons(data["lessons"], path.name, True)
    elif "questions" in data:
        # Master question file (YouTop private/books/<book id>/mcqs-*.json).
        expected = data.get("from")
        for q in data["questions"]:
            n = q.get("n")
            where = f"{path.name} question {n}"
            if not isinstance(n, int) or (expected is not None and n != expected):
                bad(where, f"numbers must run without gaps or repeats (expected {expected})")
            expected = (n + 1) if isinstance(n, int) else None
            options = q.get("options", [])
            if not q.get("stem", "").strip() or len(options) < 2:
                bad(where, "needs a stem and at least two options")
            if not isinstance(q.get("answer"), int) or not 0 <= q["answer"] < len(options):
                bad(where, "answer index is out of range")
            figure = q.get("figure")
            if figure and not MEDIA.fullmatch(figure):
                bad(where, f"figure '{figure}' is not a media reference")
            for c in q.get("corrections", []):
                if not all(c.get(k) for k in ("field", "was", "now", "why")):
                    bad(where, "each correction needs field, was, now and why")
        if data.get("to") is not None and expected is not None and expected - 1 != data["to"]:
            bad(path.name, f"last question is {expected - 1} but the file says it ends at {data['to']}")
    elif "items" in data:
        # Image prompts handed to the owner: {"items": [{"info": "1. ..."}, ...]} and nothing else.
        if set(data) != {"items"}:
            bad(path.name, "only the 'items' field is allowed")
        expected = None
        for i, item in enumerate(data["items"]):
            info = item.get("info", "") if isinstance(item, dict) else ""
            m = re.match(r"(\d+)\. \S", info)
            if not isinstance(item, dict) or set(item) != {"info"} or not m:
                bad(path.name, f"item {i} must be {{\"info\": \"<number>. <prompt>\"}}")
                continue
            n = int(m.group(1))
            if expected is not None and n != expected:
                bad(path.name, f"item {i} is numbered {n}, expected {expected} (no gaps or repeats)")
            expected = n + 1
    elif "books" in data:
        for b in data["books"]:
            if not b.get("id") or not b.get("file"):
                bad(path.name, "every catalog entry needs id and file")
    else:
        bad(path.name, "not a book, chapter or catalog file")

if __name__ == "__main__":
    if len(sys.argv) < 2:
        sys.exit(__doc__)
    files = []
    for arg in sys.argv[1:]:
        p = pathlib.Path(arg)
        files += sorted(p.rglob("*.json")) if p.is_dir() else [p]
    for f in files:
        check_file(f)
    if problems:
        print("\n".join(problems))
        sys.exit(f"\n{len(problems)} problem(s) in {len(files)} file(s). Nothing should be published until these are fixed.")
    print(f"OK: {len(files)} file(s), no problems.")
