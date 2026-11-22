#!/usr/bin/env python3
"""The audit of recurring symbols (the owner's rule 5), and the check that batches use them.

    symbols.py audit            score every symbol in content/symbols.json, approve or reject it,
                                and write content/symbols.md (the readable list)
    symbols.py check batch.txt  every recurring concept named in a batch's 'keywords:' lines must be
                                drawn with its approved symbol (batch.py prompts/build run this too)

Numbers (the owner, 2026-11-22): each digit 0-9 and the decimal point has its own symbol (family "digits",
field "digit"). A keyword 'number 5.5 (what it measures) -> object' must draw every digit's symbol, in the
order of the digits (5.5 = hamsa, golden drop, hamsa). A number counts once toward the three recurring symbols.

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
        "To make a number: the story names the digits in order; the picture draws the first digit biggest and each next one",
        "smaller; a repeated digit is drawn twice (55 = two hamsa hands); the golden drop is the decimal point",
        "(5.5 = hamsa, golden drop, hamsa); the number sits on the symbol of what it measures; units are not drawn.",
        "In `keywords:` write `number 5.5 (what it measures) -> the objects`; `symbols.py check` makes sure every digit",
        "is drawn in order. A number counts as one of the lesson's three recurring symbols.", "",
        "| Digit | Drawn as | Say (Arabic) | Why it sticks |", "|---|---|---|---|"] + [
        f"| {s['digit']} | {s['symbol']} | {s.get('ar', '')} | {s['why']} |"
        for s in sorted((x for x in data["symbols"] if x.get("family") == "digits" and x["status"] == "approved"),
                        key=lambda x: ("." in x["digit"], x["digit"]))] + [
        "", "## Approved", "",
        "| Concept | Drawn as | Why | Score | First used |", "|---|---|---|---|---|"]
    rows, rejected = [], []
    for s in data["symbols"]:
        total = sum(s["scores"].get(c, 0) for c in CRITERIA)
        if s["status"] == "approved" and s.get("family") == "digits":
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


# The owner (2026-11-22): every lesson carries at least three recurring symbols. The HART Hotel and its lobbies and
# engine rooms, the frame of every heart picture, do not count; each symbol must be named in 'keywords:' and drawn there.
MIN_PER_LESSON = 3
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
            sid = "number" if s.get("family") == "digits" else s["id"]
            if sid not in found:
                found.append(sid)
    return found


def too_few(items, data=None, minimum=MIN_PER_LESSON):
    """The lessons with fewer than `minimum` recurring symbols (the owner's rule, 2026-11-22)."""
    data = data or load()
    out = []
    for b in items:
        ids = lesson_symbols(b, data)
        if len(ids) < minimum:
            out.append(f"question {b['n']}: {len(ids)} recurring symbol(s) ({', '.join(ids) or 'none'}); every lesson needs "
                       f"at least {minimum}, named in 'keywords:' and drawn (the owner, 2026-11-22; the hotel frame does not count)")
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
        for p in problems:
            print("PROBLEM:", p)
        print(f"{path}: {len(problems)} problem(s); {len(local)} keyword(s) not in the symbol list (fine if they are one-offs):")
        for l in local:
            print("   ", l)
        bad += len(problems)
    sys.exit(1 if bad else 0)


if __name__ == "__main__":
    main()
