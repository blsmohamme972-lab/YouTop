#!/usr/bin/env python3
"""The audit of recurring symbols (the owner's rule 5), and the check that batches use them.

    symbols.py audit            score every symbol in content/symbols.json, approve or reject it,
                                and write content/symbols.md (the readable list)
    symbols.py check batch.txt  every recurring concept named in a batch's 'keywords:' lines must be
                                drawn with its approved symbol (batch.py prompts/build run this too)

Numbers (the owner, 2026-11-22): each digit 0-9 and the decimal point has its own symbol (family "digits",
field "digit"), drawn small: a tattoo, a painting on a wall or a design on an object. A keyword
'number 5.5 (what it measures) -> object' must draw every digit's symbol, in the order of the digits
(5.5 = hamsa, golden drop, hamsa), and say where it is drawn. A number counts once toward the three
recurring symbols.

A symbol is scored 0-2 on five things: link (a sound-alike or a literal tie to the term), concrete,
unmistakable, drawable (an image generator gets it right) and sticky (remembered at first sight).
It is APPROVED when every score is at least 1 and the total is at least 8, and when its anchor words
(the words a prompt must contain to draw it) belong to no other concept, except inside one family.
Otherwise it is REJECTED and stays in the file, so that nobody proposes it again.

The scores are a judgement, written into the json by whoever proposes the symbol, with a reason in
'why' and 'notes'. The audit makes the judgement explicit, applies one rule to all of them, and
catches the mechanical mistakes (two concepts drawn alike, one term claimed by two symbols, a batch
that draws a known concept some other way, or with a symbol that was rejected).
"""
import argparse, datetime, functools, json, pathlib, re, sys

ROOT = pathlib.Path(__file__).resolve().parents[2]
SOURCE = ROOT / "content" / "symbols.json"
READABLE = ROOT / "content" / "symbols.md"
CRITERIA = ("link", "concrete", "unmistakable", "drawable", "sticky")
PASS_TOTAL = 8


def load():
    return json.loads(SOURCE.read_text(encoding="utf-8"))


LAYERS = ("sound", "meaning", "place")


def verdict(s):
    scores = s.get("scores", {})
    missing = [c for c in CRITERIA if not isinstance(scores.get(c), int) or not 0 <= scores[c] <= 2]
    if missing:
        return "rejected", f"no valid score for {', '.join(missing)}"
    total = sum(scores[c] for c in CRITERIA)
    low = [c for c in CRITERIA if scores[c] == 0]
    if low:
        return "rejected", f"scores 0 on {', '.join(low)}"
    if total < PASS_TOTAL:
        return "rejected", f"total {total}/10 is below {PASS_TOTAL}"
    if not s.get("terms"):
        return "rejected", "no terms: nothing would ever use it"
    # The owner (2026-11-20): every new symbol joins at least two layers in one object, as endocarditis does
    # (a sound-alike of the name + the meaning of the disease, + where it sits when that matters).
    layers = {k: v for k, v in (s.get("layers") or {}).items() if k in LAYERS and str(v).strip()}
    if not s.get("legacy") and len(layers) < 2:
        return "rejected", f"only {len(layers)} layer(s); a symbol must join at least two of {', '.join(LAYERS)}"
    return "approved", f"total {total}/10" + (f", {len(layers)} layers" if layers else "")


def audit(data):
    problems, report = [], []
    symbols = data["symbols"]
    for s in symbols:
        s["status"], s["reason"] = verdict(s)
    approved = [s for s in symbols if s["status"] == "approved"]
    # One term, one concept.
    seen = {}
    for s in approved:
        for t in s["terms"]:
            key = t.lower()
            if key in seen and seen[key] != s["id"]:
                problems.append(f"term '{t}' belongs to both '{seen[key]}' and '{s['id']}'")
            seen[key] = s["id"]
    # Two concepts must not be drawn alike (same anchor), unless they are one family on purpose.
    owner = {}
    for s in approved:
        for a in s.get("anchors", []):
            key = a.lower()
            other = owner.get(key)
            if other and not (other.get("family") and other.get("family") == s.get("family")):
                problems.append(f"'{s['id']}' and '{other['id']}' are both drawn with '{a}': two concepts would look alike")
            owner.setdefault(key, s)
        if not s.get("anchors"):
            problems.append(f"'{s['id']}' has no anchors: a prompt could not be checked against it")
    for s in symbols:
        report.append((s["status"], s["id"], s["reason"]))
    today = datetime.date.today().isoformat()
    for s in symbols:
        s["audited"] = today
    return problems, report


def write_readable(data):
    head = [
        "# Recurring symbols (the owner's rule 5)", "",
        "Written by `tools/content/symbols.py audit` from `content/symbols.json`; edit the json, not this file.", "",
        "A concept that comes back from lesson to lesson and from book to book is always drawn with the **same**",
        "symbol, so that each picture adds to the same memory. A symbol is scored 0-2 on five things: **link** (an",
        "English sound-alike, or a literal tie to the term), **concrete**, **unmistakable**, **drawable** (an image",
        "generator gets it right) and **sticky** (remembered at first sight). It is approved when every score is at",
        "least 1, the total is at least 8, and no other concept is drawn with the same thing. The owner asked for this",
        "audit to decide (2026-11-19). A new symbol is added to the json with its scores and a reason, then audited;",
        "a batch that draws a listed concept any other way is refused by `batch.py`.", "",
        "Since 2026-11-20 (the owner) every new symbol joins at least two **layers** in one object: **sound** (a",
        "sound-alike of the name), **meaning** (what the thing is or does) and, when it matters, **place** (where it",
        "sits in the picture). The model is endocarditis: playing CARDs (sound) sprouting broccoli, the vegetations",
        "(meaning), on the valve it infects (place). Symbols made before that are marked legacy.", "",
        "## Numbers: one symbol for each digit (the owner, 2026-11-22)", "",
        "Pictures carry no written numbers, so a number is drawn with these. Each digit's symbol sounds like the digit in",
        "Arabic (say its Arabic name in the story), holds the number (one horn, five fingers) and looks like the digit.",
        "In a picture a number is **small**: a tattoo on a hand or arm, a painting on a wall that belongs in the scene, or a",
        "design painted, carved or printed on an object, best on the thing whose value it gives; clear, in the picture's",
        "own style, never competing with the focal point (the owner, 2026-11-22). The digits stand side by side, the same",
        "size, left to right as a number is written; a repeated digit is drawn twice (55 = two hamsa hands); the golden",
        "drop is the decimal point (5.5 = hamsa, golden drop, hamsa). The story names the digits in order. Units are not",
        "drawn. In `keywords:` write `number 5.5 (what it measures) -> a small tattoo on ...: the objects in order`;",
        "`symbols.py check` makes sure every digit is drawn, in order, and that the keyword says where. A number counts",
        "as one of the lesson's four recurring symbols.", "",
        "| Digit | Drawn as | Say (Arabic) | Why it sticks |", "|---|---|---|---|"] + [
        f"| {s['digit']} | {s['symbol']} | {s.get('ar', '')} | {s['why']} |"
        for s in sorted((x for x in data["symbols"] if x.get("family") == "digits" and x["status"] == "approved"),
                        key=lambda x: ("." in x["digit"], x["digit"]))] + [
        "", "## Directions: a marker on the part, never a position (the owner, 2026-11-26)", "",
        "Image generators mirror pictures and guess what 'behind' or 'the left one' means, so the owner found sides and",
        "directions wrong again and again. A side or direction is therefore never carried by where a thing stands in the",
        "frame: the part itself wears its marker, small, wherever it falls in the picture. Most markers sound like the",
        "Arabic word (say their Arabic names in the story), all hold the meaning. `symbols.py check` refuses a keyword",
        "object, anchor, scene sentence or 'must' that names a direction (left, right, front, behind, upper, lower,",
        "medial, lateral, proximal, distal...) without its marker; say it with the marker or without the word.", "",
        "| Direction | Drawn as | Say (Arabic) | Why it sticks |", "|---|---|---|---|"] + [
        f"| {s['direction']} | {s['symbol']} | {s.get('ar', '')} | {s['why']} |"
        for s in (x for x in data["symbols"] if x.get("family") == "directions" and x["status"] == "approved")] + [
        "", "## Approved", "",
        "| Concept | Drawn as | Why | Score | First used |", "|---|---|---|---|---|"]
    rows, rejected = [], []
    for s in data["symbols"]:
        total = sum(s["scores"].get(c, 0) for c in CRITERIA)
        if s["status"] == "approved" and s.get("family") in ("digits", "directions"):
            continue
        if s["status"] == "approved":
            note = f" ({s['notes']})" if s.get("notes") else ""
            rows.append(f"| {s['concept']} | {s['symbol']}{note} | {s['why']} | {total}/10 | {s.get('firstUsed') or '-'} |")
        else:
            rejected.append(f"| {s['concept']} | {s['symbol']} | {s['reason']}. {s.get('notes', '')} |")
    body = head + rows + ["", "## Rejected (never use them)", "", "| Concept | Was drawn as | Why rejected |", "|---|---|---|"] + rejected
    READABLE.write_text("\n".join(body) + "\n", encoding="utf-8")


@functools.lru_cache(maxsize=None)
def _term_re(t):
    # Compiled once per term: with thousands of terms, re's own small cache would recompile on every call.
    return re.compile(r"(?<![\w-])" + re.escape(t) + r"(?![\w-])")


@functools.lru_cache(maxsize=None)
def _anchor_re(a):
    return re.compile(r"(?<!\w)" + re.escape(a) + r"(?:s|es)?(?!\w)")


def match(term, data):
    """The approved or rejected symbol a keyword term names, by its longest matching term."""
    term = re.sub(r"\([^)]*\)|\"[^\"]*\"", "", term).strip().lower()
    best, best_len = None, 0
    for s in data["symbols"]:
        for t in s.get("terms", []):
            exact = t.startswith("=")
            t = t.lstrip("=").lower()
            hit = term == t if exact else _term_re(t).search(term)
            if hit and len(t) > best_len:
                best, best_len = s, len(t)
    return best


def has_anchor(obj, s):
    obj = obj.lower()
    return any(_anchor_re(a.lower()).search(obj) for a in s.get("anchors", []))


NUMBER = re.compile(r"^(?:number|#)\s*(\d[\d,]*(?:\.\d+)?|\.\d+)$", re.I)


def number_term(term):
    """The digits of a number keyword ('number 5.5 (aortic size, cm)' -> '5.5'), or None."""
    m = NUMBER.match(re.sub(r"\([^)]*\)|\"[^\"]*\"", "", term).strip())
    return m.group(1).replace(",", "") if m else None


def digit_symbols(data):
    return {s["digit"]: s for s in data["symbols"] if s.get("family") == "digits" and s.get("digit") and s.get("status") == "approved"}


# Where a number is drawn (the owner, 2026-11-22): small, as a tattoo, a painting on a wall or a design on an object.
CARRIER = re.compile(r"\b(tattoo\w*|mural\w*|paint\w*|drawn|drawing\w*|graffiti|engrav\w*|carv\w*|etch\w*|"
                     r"embroider\w*|print\w*|stamp\w*|stencil\w*|mosaic\w*|inlaid|emblem\w*|badge\w*|sign|signs|"
                     r"signboard\w*|banner\w*|decal\w*|sticker\w*)\b", re.I)
COUNT_WORDS = {"two": 2, "twin": 2, "twins": 2, "pair": 2, "three": 3, "four": 4, "five": 5, "six": 6}


def number_problem(digits, obj, data):
    """None when the object draws every digit's symbol in the order of the digits, else what is missing.
    A digit that repeats (55, 100) is named once per digit, or once with a count ('two hamsa hands',
    'three golden whistles')."""
    table, obj, at = digit_symbols(data), obj.lower(), 0
    runs = []
    for d in digits:
        if runs and runs[-1][0] == d:
            runs[-1][1] += 1
        else:
            runs.append([d, 1])
    want = ", ".join(table[x]["anchors"][0] for x in digits if x in table)
    for d, k in runs:
        s = table.get(d)
        if s is None:
            return f"no approved symbol for '{d}'"
        while k > 0:
            hits = sorted((m for a in s["anchors"] for m in _anchor_re(a.lower()).finditer(obj, at)), key=lambda m: m.start())
            if not hits:
                return f"draw it as {want} (in this order); '{d}' ({s['anchors'][0]}) is missing or out of order"
            m = hits[0]
            before = re.findall(r"[a-z]+", obj[max(0, m.start() - 30):m.start()])
            count = next((COUNT_WORDS[w] for w in reversed(before[-3:]) if w in COUNT_WORDS), 1)
            k -= min(count, k)
            at = m.end()
    if not CARRIER.search(obj):
        return ("say where it is drawn, small: a tattoo on a hand or arm, a painting on a wall, or a design painted, "
                "carved or printed on an object in the scene (the owner, 2026-11-22)")
    return None


def check_items(items, data=None):
    """Problems and notes for the 'keywords:' lines of parsed batch items (see batch.py)."""
    data = data or load()
    problems, local = [], []
    for b in items:
        for part in (b.get("keywords") or "").split(";"):
            if not part.strip():
                continue
            pieces = re.split(r"->|→", part, maxsplit=1)
            if len(pieces) != 2:
                problems.append(f"question {b['n']}: keyword '{part.strip()}' is not written 'term -> object'")
                continue
            term, obj = pieces[0].strip(), pieces[1].strip()
            digits = number_term(term)
            if digits is not None:
                why = number_problem(digits, obj, data)
                if why:
                    problems.append(f"question {b['n']}: number {digits}: {why}")
                continue
            s = match(term, data)
            if s is None:
                local.append(f"question {b['n']}: '{term}' -> {obj[:60]}")
                continue
            if s.get("status") != "approved":
                problems.append(f"question {b['n']}: '{term}' uses '{s['id']}', which is {s.get('status', 'not audited')}; run symbols.py audit")
            elif not has_anchor(obj, s):
                problems.append(f"question {b['n']}: '{term}' is a recurring concept; draw it as {s['symbol']} "
                                f"(the object must name: {' or '.join(s['anchors'])})")
    return problems, local


# The owner (2026-11-22): every lesson carries at least four recurring symbols (three until the owner's rule 7, the
# same evening). The HART Hotel and its lobbies and engine rooms, the frame of every heart picture, do not count; each
# symbol must be named in 'keywords:' and drawn there.
MIN_PER_LESSON = 4
# The owner's rule 7 (2026-11-22): twelve symbols in each picture (at least 4 recurring, 5 one-off sound-alikes, up to 3
# others), all on the question stem and the text of the correct answer, around one anchor: the central image that joins
# what is asked to the correct answer, built on a recurring symbol.
TOTAL_PER_LESSON, ONE_OFF_PER_LESSON, CROWDED = 12, 5, 14
FRAME = {"hart-hotel", "atrium", "ventricle"}


def lesson_symbols(b, data=None):
    """The approved recurring symbols a lesson's 'keywords:' name and draw, the frame not counted."""
    data = data or load()
    found = []
    for part in (b.get("keywords") or "").split(";"):
        pieces = re.split(r"->|→", part, maxsplit=1)
        if len(pieces) != 2:
            continue
        digits = number_term(pieces[0].strip())
        if digits is not None:
            # A number is one recurring symbol, however many digits it has.
            if number_problem(digits, pieces[1], data) is None and "number" not in found:
                found.append("number")
            continue
        s = match(pieces[0].strip(), data)
        if s and s.get("status") == "approved" and s["id"] not in FRAME and has_anchor(pieces[1], s):
            # Numbers count once, and so do direction markers: they help other symbols, they are not concepts.
            sid = {"digits": "number", "directions": "direction"}.get(s.get("family"), s["id"])
            if sid not in found:
                found.append(sid)
    return found


def keyword_pairs(b):
    """The 'term -> object' pairs of a lesson's 'keywords:' line."""
    out = []
    for part in (b.get("keywords") or "").split(";"):
        pieces = re.split(r"->|→", part, maxsplit=1)
        if len(pieces) == 2 and pieces[0].strip() and pieces[1].strip():
            out.append((pieces[0].strip(), pieces[1].strip()))
    return out


def lesson_counts(b, data=None):
    """How many symbols a lesson's keywords name: all of them, the recurring ones (as the three-symbol rule counts
    them) and the one-offs (terms no symbol covers; numbers are not one-offs)."""
    data = data or load()
    pairs = keyword_pairs(b)
    one_off = sum(1 for t, _ in pairs if number_term(t) is None and match(t, data) is None)
    return {"total": len(pairs), "recurring": len(lesson_symbols(b, data)), "one_off": one_off}


def anchor_problem(b, data=None):
    """None when the lesson has an anchor built on a recurring symbol, else what is wrong."""
    data = data or load()
    text = (b.get("anchor") or "").strip()
    if not text:
        return "no 'anchor:' line: the one central image that joins what the question asks to its correct answer"
    pieces = re.split(r"->|→", text, maxsplit=1)
    if len(pieces) != 2 or not pieces[1].strip():
        return "write the anchor as 'anchor: what is asked -> the image that shows the correct answer'"
    if not any(s.get("status") == "approved" and s["id"] not in FRAME and s.get("family") not in ("digits", "directions") and has_anchor(pieces[1], s)
               for s in data["symbols"]):
        return "the anchor's image names no recurring symbol: draw what is asked as its recurring symbol (make one if it has none)"
    return None


DIGIT = re.compile(r"\d")

# The owner (2026-11-26): pictures kept getting sides and directions wrong (image generators mirror a picture, and guess
# what "behind" or "the left one" means). A direction is never carried by where a thing stands in the frame: the part
# wears its direction marker (family "directions" in symbols.json), small, wherever it falls in the picture.
DIRECTION_WORDS = {
    "right": r"\b(?:right|rt)\b",
    "left": r"\b(?:left|lt)\b",
    "anterior": r"\b(?:anterior|front|ventral)\b",
    "posterior": r"\b(?:posterior|behind|rear|dorsal)\b|\bback (?:wall|side|leaf|leaflet|surface|face)\b|\b(?:at|on|from) the back of\b",
    "superior": r"\b(?:superior|cranial)\b|\bupper\b(?! party wall| and lower party wall| glass lobb| lobb)",
    "inferior": r"\b(?:inferior|caudal)\b|\blower\b(?! party wall)",
    "medial": r"\bmedial\b",
    "lateral": r"\blateral\b",
    "proximal": r"\bproximal\b",
    "distal": r"\bdistal\b",
}
# Reading order is not a side of the body: numbers and routes run from left to right.
DIRECTION_EXEMPT = re.compile(r"\b(?:from )?left to right\b")


def direction_markers(data):
    return {s["direction"]: s for s in data["symbols"]
            if s.get("family") == "directions" and s.get("direction") and s.get("status") == "approved"}


def direction_problems(items, data=None):
    """Where a lesson's picture would show a side or direction by position alone (the owner, 2026-11-26): every
    keyword object, the anchor's image, each scene sentence and 'must' that names a direction must also name its
    marker (a white dove on the right-sided part, a golden bangle on the left-sided one...)."""
    data = data or load()
    marks, out = direction_markers(data), []
    for b in items:
        chunks = [(f"the object of '{t}'", o) for t, o in keyword_pairs(b)]
        pieces = re.split(r"->|→", b.get("anchor") or "", maxsplit=1)
        if len(pieces) == 2:
            chunks.append(("the anchor's image", pieces[1]))
        chunks += [(f"scene sentence {i}", s) for i, s in enumerate(re.split(r"(?<=[.!?])\s+", b.get("scene") or ""), 1)]
        if b.get("must"):
            chunks.append(("'must'", b["must"]))
        for where, text in chunks:
            low = DIRECTION_EXEMPT.sub(" ", re.sub(r"\([^)]*\)", "", text).lower())
            for d, rx in DIRECTION_WORDS.items():
                m = re.search(rx, low)
                if m and d in marks and not has_anchor(low, marks[d]):
                    out.append(f"question {b['n']}: '{m.group(0)}' in {where}: a direction is never shown by position (the owner, "
                               f"2026-11-26); put its marker on the part ({marks[d]['symbol']}; name: {marks[d]['anchors'][0]}), "
                               f"or say it without the word")
    return out


def written_numbers(b):
    """Where a lesson's picture would get written digits: numbers are drawn with the number symbols (rule 6)."""
    out = [f"the object of '{t}'" for t, o in keyword_pairs(b) if DIGIT.search(re.sub(r"\([^)]*\)|\"[^\"]*\"", "", o))]
    pieces = re.split(r"->|→", b.get("anchor") or "", maxsplit=1)
    if len(pieces) == 2 and DIGIT.search(re.sub(r"\([^)]*\)", "", pieces[1])):
        out.append("the anchor's image")
    return out


def rule7(items, data=None):
    """The owner's rule 7: an anchor, and twelve symbols with at least five one-offs (the four recurring ones are
    too_few's); and rule 6: no written digits in what is drawn. Returns (problems, warnings)."""
    data = data or load()
    problems, warnings = [], []
    for b in items:
        w = f"question {b['n']}"
        for where in written_numbers(b):
            problems.append(f"{w}: {where} has written digits; draw a number with the number symbols, small, as a tattoo, "
                            f"a painting on a wall or a design on an object (the owner's rule 6)")
        if DIGIT.search(b.get("words") or ""):
            warnings.append(f"{w}: 'words' paints digits; numbers are drawn with the number symbols now (rule 6)")
        why = anchor_problem(b, data)
        if why:
            problems.append(f"{w}: {why} (the owner's rule 7)")
        c = lesson_counts(b, data)
        if c["total"] < TOTAL_PER_LESSON:
            problems.append(f"{w}: {c['total']} symbols in 'keywords:'; the owner asks for {TOTAL_PER_LESSON} on the question and "
                            f"its correct answer (at least {MIN_PER_LESSON} recurring, {ONE_OFF_PER_LESSON} one-off; rule 7)")
        if c["one_off"] < ONE_OFF_PER_LESSON:
            problems.append(f"{w}: {c['one_off']} one-off symbol(s); the owner asks for {ONE_OFF_PER_LESSON}: sound-alikes for the terms "
                            f"of the stem and the correct answer that have no recurring symbol (rule 7)")
        if c["total"] > CROWDED:
            warnings.append(f"{w}: {c['total']} symbols; twelve is the owner's number, more crowds the picture")
    return problems, warnings


def too_few(items, data=None, minimum=MIN_PER_LESSON):
    """The lessons with fewer than `minimum` recurring symbols (the owner's rule, 2026-11-22)."""
    data = data or load()
    out = []
    for b in items:
        ids = lesson_symbols(b, data)
        if len(ids) < minimum:
            out.append(f"question {b['n']}: {len(ids)} recurring symbol(s) ({', '.join(ids) or 'none'}); every lesson needs "
                       f"at least {minimum}, named in 'keywords:' and drawn (the owner, 2026-11-22; the hotel frame does not count; "
                       f"recurring symbols may be added or improved, with the audit)")
    return out


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    sub = ap.add_subparsers(dest="cmd", required=True)
    sub.add_parser("audit")
    c = sub.add_parser("check")
    c.add_argument("batch", nargs="+")
    args = ap.parse_args()
    data = load()
    if args.cmd == "audit":
        problems, report = audit(data)
        for status, sid, reason in report:
            print(f"{status:9} {sid:32} {reason}")
        for p in problems:
            print("PROBLEM:", p)
        SOURCE.write_text(json.dumps(data, ensure_ascii=False, indent=1) + "\n", encoding="utf-8")
        write_readable(data)
        print(f"\n{sum(1 for s in data['symbols'] if s['status'] == 'approved')} approved, "
              f"{sum(1 for s in data['symbols'] if s['status'] != 'approved')} rejected; wrote {READABLE.relative_to(ROOT)}")
        sys.exit(1 if problems else 0)
    sys.path.insert(0, str(pathlib.Path(__file__).parent))
    import batch
    bad = 0
    for path in args.batch:
        _, items = batch.parse(path)
        problems, local = check_items(items, data)
        problems += too_few(items, data)
        more, warns = rule7(items, data)
        problems += more + direction_problems(items, data)
        for line in warns:
            print("warning:", line)
        for p in problems:
            print("PROBLEM:", p)
        print(f"{path}: {len(problems)} problem(s); {len(local)} keyword(s) not in the symbol list (fine if they are one-offs):")
        for l in local:
            print("   ", l)
        bad += len(problems)
    sys.exit(1 if bad else 0)


if __name__ == "__main__":
    main()
