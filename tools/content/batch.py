#!/usr/bin/env python3
"""Turns a batch of written lessons into published lessons, doing every mechanical step.

The writer (a person or an AI) only writes what needs a mind: titles, tags, the picture's scene,
the Arabic story, the quotation and the English explanation, in one plain text file. This tool
copies the questions from the master file (they are never retyped), builds the lessons and the
image prompts, checks them against the owner's rules, and prepares the folder for publish.py.

    batch.py questions <book> <from> <to> --master <file> [...]   the questions, compactly, to read
    batch.py quotes    <book> [--folder F]                        quotations already used (do not repeat)
    batch.py new       <book> <from> <to> --master <file> -o batch.txt     an empty batch file to fill in
    batch.py prompts   batch.txt [-o prompts.json]                the image prompts alone, BEFORE the stories exist
    batch.py build     batch.txt --master <file> [--folder F] [--force]    lessons + image prompts into F
    batch.py pictures  <book> <folder of the owner's pictures> [--folder F]  N.png -> media/<id>.webp
    batch.py spots     spots.txt [--folder F] [--pictures <folder>]        spotlight positions, drawn to check
    batch.py marks     <book> <from> <to> --draft marks.txt [--folder F]   a marks file to fill in, from the spots
    batch.py marks     marks.txt [--folder F]                     the numbered circles and their table, drawn to check
    batch.py status    <book> [--folder F]                        what each lesson still lacks

<book> is a book id with a file content/books/<book>.json. --master files are the book's question
files (Drive: "YouTop private/books/<book>", ids in that json); read them with the Drive
connector's download tool, never commit them. F is the scratch folder for publish.py (default
.content-work/, git-ignored); it is filled from what is published the first time. When a step is
done:  python3 tools/content/publish.py F -m "what changed"

The batch file (UTF-8 text). A line "== 41" starts question 41; then "key: value" lines.
"story:" and "explanation:" are followed by one sentence per line.

    book: shahzad-2015
    style: ...                            (optional: this batch's picture style, in place of the book's;
                                           the owner wants a new style every hundred lessons, 2026-11-26)
    theme: ...                            (optional: this batch's setting and quotation source)
    == 41
    title: The cardiac cycle: isovolumetric contraction
    titleAr: الدورة القلبية: الانقباض متساوي الحجم
    tags: cardiac/foundations d:physiology q:recall l:1
    palette: cold blue dawn with one blaze of gold
    words: 8, 10, 12                      (optional: the few short labels painted in the picture)
    must: the clerk holds up exactly two fingers; the left door leaf is the larger one
                                          (optional: the few details the story depends on; the prompt
                                           repeats them at its end as things that must be exactly so)
    keywords: atropine -> a gold TROPHY ("a-TRO-pine"); asystole -> a nun, "a SISTER", lying still
                                          (the owner's rule 2: every abstract term of the answer as a
                                           concrete sound-alike object; recurring ones from content/symbols.md;
                                           rule 7: twelve, on the stem and the correct answer: at least 4
                                           recurring, 5 one-off, up to 3 others; numbers per rule 6)
    anchor: what stops the heart -> the nun lying still on her flat white line, the gold trophy beside her
                                          (rule 7: what the question asks -> the one central image that shows
                                           its correct answer, built on at least one recurring symbol)
    scene: dawn at the only gate of a small hill fortress. ...
    story:
    كان لقلعة التلّ بابٌ واحد ...
    ... 8 to 11 sentences; the last one leads into the quotation ("...وقال:")
    quote: «...»
    source: الجريمة والعقاب
    explanation:
    Isovolumetric contraction begins when ...
    ... 4 to 8 sentences
    note: The book's key is B; ...        (optional: added to what is shown after answering)
    key: C                                (optional: corrects the book's answer key, as option letters,
                                           "A C" for several; ONLY for a clear, undisputed medical error
                                           (the owner's rule, 2026-11-19). Needs the tag x:key-corrected
                                           and a 'note:' that says what the book's key was and why.)

The spots file: per question, one position per sentence, "x y r" as hundredths of the picture's
width/height (r of the width), "-" for a sentence about nothing visible. The quotation takes none.

    book: shahzad-2015
    == 41
    story: 44 20 30 | 15 50 25 | - | 78 30 22 | ...
    explanation: 78 30 22 | 92 27 10 | ...

The marks file: per question, the numbered circles of "all circles" and their table (English only).
One line per circle: "<number>: x y r | what is in the picture | what it stands for"; numbers run
1, 2, 3... "question:" lists the circles shown while the question is asked (they must not give the
answer away), "answer:" the circles shown once it is answered. Text is never touched by this step.

    book: shahzad-2015
    == 41
    1: 44 20 30 | The gatekeeper at the only gate | Mitral valve: closes as the ventricle contracts
    2: 15 50 25 | The bell that rings once | First heart sound
    question: 1
    answer: 2
"""
import argparse, json, pathlib, re, shutil, subprocess, sys, unicodedata

ROOT = pathlib.Path(__file__).resolve().parents[2]
DEFAULT_FOLDER = ROOT / ".content-work"
LATIN = re.compile(r"[A-Za-z]")
KEYS = {"title", "titleAr", "tags", "palette", "words", "must", "keywords", "anchor", "scene", "story", "quote", "source", "explanation", "note", "key", "option", "asks"}
MULTI = {"story", "explanation", "asks"}


def die(msg):
    sys.exit(f"batch.py: {msg}")


def book_config(book):
    path = ROOT / "content" / "books" / f"{book}.json"
    if not path.is_file():
        die(f"no {path.relative_to(ROOT)}: describe the book there first (copy shahzad-2015.json)")
    return json.loads(path.read_text(encoding="utf-8"))


def batch_config(head):
    """The book's config, with the batch file's own 'style:' (and 'theme:') lines in place of the book's: the owner
    (2026-11-26) wants a new picture style and quotation source for every hundred lessons, chosen by the agent."""
    cfg = book_config(head["book"])
    return dict(cfg, **{k: head[k] for k in ("style", "theme") if head.get(k)})


def from_extraction(record):
    """One line of an extraction .jsonl ({"number", "stem", "options": [{"label", "text"}], "answer": "ACD"})
    as a master question. Several letters become "answers" (choose all that apply). A matching key
    ("A-1. B-1,2") cannot be asked as options yet: the question is returned with "unsupported" set."""
    labels = [o["label"].strip().upper() for o in record["options"]]
    q = {"n": record["number"], "stem": record["stem"], "options": [o["text"] for o in record["options"]]}
    key = str(record.get("answer") or "").upper()
    letters = re.findall(r"[A-Z]", key)
    if not letters or re.search(r"\d", key) or any(x not in labels for x in letters):
        q.update(answer=0, unsupported=f"answer key '{record.get('answer')}' is not a set of option letters")
        return q
    picks = sorted({labels.index(x) for x in letters})
    q["answer"] = picks[0]
    if len(picks) > 1:
        q["answers"] = picks
    if record.get("note"):
        q["explanation"] = record["note"]
    if record.get("images"):
        # The stem shows a figure: build then wants media/figure-qNNNN.png (several images joined into one).
        q["figure"] = ", ".join(record["images"])
    return q


def load_master(paths):
    questions = {}
    for p in paths or []:
        text = pathlib.Path(p).read_text(encoding="utf-8")
        if str(p).endswith(".jsonl"):
            records = [from_extraction(json.loads(line)) for line in text.splitlines() if line.strip()]
            for i, q in enumerate(records, 1):
                # A book numbered by chapter ("47.3") is numbered 1, 2, 3... in the order of the file;
                # its own number is kept as "ref" and shown with the question.
                if not isinstance(q["n"], int):
                    q["ref"], q["n"] = str(q["n"]), i
        else:
            records = json.loads(text)["questions"]
        for q in records:
            questions[q["n"]] = q
    if not questions:
        die("give the book's question file(s) with --master (from Drive, see content/books/<book>.json)")
    return questions


def lesson_id(cfg, n):
    return f"{cfg['short']}-q{n:04d}"


def chapter_of(cfg, n):
    size = cfg.get("chapterSize", 100)
    lo = (n - 1) // size * size + 1
    return f"q{lo:04d}-{lo + size - 1:04d}"


def ensure_folder(folder, cfg):
    """The scratch folder holds the book as published; export it the first time."""
    book_dir = folder / "books" / cfg["id"]
    started = (folder / "books").is_dir() and any((folder / "books").iterdir())
    if not (book_dir / "book.json").is_file() and not started:
        # Only an empty folder is filled from what is published: exporting into a folder that already holds
        # work would overwrite other books' unpublished lessons with their published copies.
        catalog = subprocess.run(["git", "ls-remote", "--exit-code", "--heads", "origin", "content"], cwd=ROOT, capture_output=True)
        if catalog.returncode == 0:
            subprocess.run([sys.executable, str(ROOT / "tools/content/publish.py"), "--export", str(folder)], check=True)
    if not (book_dir / "book.json").is_file():
        (book_dir / "chapters").mkdir(parents=True, exist_ok=True)
        (book_dir / "book.json").write_text(json.dumps({"id": cfg["id"], "title": cfg["sourceLabel"], "chapters": []}, ensure_ascii=False, indent=1), encoding="utf-8")
    return book_dir


def read_chapter(book_dir, cfg, name, create=False):
    path = book_dir / "chapters" / f"{name}.json"
    if path.is_file():
        return path, json.loads(path.read_text(encoding="utf-8"))
    if not create:
        return path, {"lessons": []}
    book = json.loads((book_dir / "book.json").read_text(encoding="utf-8"))
    if not any(c["id"] == name for c in book["chapters"]):
        lo, hi = int(name[1:5]), int(name[6:10])
        book["chapters"].append({"id": name, "title": f"Questions {lo}–{hi}", "file": f"chapters/{name}.json"})
        (book_dir / "book.json").write_text(json.dumps(book, ensure_ascii=False, indent=1) + "\n", encoding="utf-8")
    return path, {"lessons": []}


def all_lessons(book_dir):
    for path in sorted((book_dir / "chapters").glob("*.json")):
        for lesson in json.loads(path.read_text(encoding="utf-8"))["lessons"]:
            yield lesson


def normal(text):
    """Arabic text without vowel marks, quotation marks and spacing, for comparing quotations."""
    text = "".join(c for c in unicodedata.normalize("NFKD", text) if not unicodedata.combining(c))
    return re.sub(r"[\W_]+", "", text)


def used_quotes(book_dir, skip_ids=()):
    out = {}
    for lesson in all_lessons(book_dir):
        if lesson["id"] in skip_ids:
            continue
        for track in lesson.get("tracks", []):
            for seg in track.get("segments", []):
                if seg.get("quote"):
                    out.setdefault(normal(seg["text"]), []).append((lesson["id"], seg["text"], seg["quote"]))
    return out


# ---- the batch file -------------------------------------------------------------

def parse(path, multi=MULTI, keys=KEYS):
    head, items, cur, field = {}, [], None, None
    for n, raw in enumerate(pathlib.Path(path).read_text(encoding="utf-8").splitlines(), 1):
        line = raw.strip()
        if not line or line.startswith("#"):
            continue
        m = re.fullmatch(r"==\s*(\d+)\s*=*", line)
        if m:
            cur = {"n": int(m.group(1))}
            items.append(cur)
            field = None
            continue
        m = re.match(r"([A-Za-z]+):\s*(.*)$", line)
        if m and (m.group(1) in keys or cur is None):
            key, value = m.group(1), m.group(2).strip()
            if cur is None:
                head[key] = value
            elif key in multi:
                cur[key] = [value] if value else []
                field = key
            else:
                if key in cur:
                    die(f"{path}:{n}: '{key}' is given twice for question {cur['n']}")
                cur[key] = value
                field = key if key == "scene" else None
            continue
        if cur is None or field is None:
            die(f"{path}:{n}: this line belongs to no field: {line[:60]}")
        if field in multi:
            cur[field].append(line)
        else:
            cur[field] += " " + line
    if "book" not in head:
        die(f"{path}: the first line must be 'book: <book id>'")
    return head, items


def image_prompt(cfg, b):
    """The owner's frame, with the five rules of 2026-11-19 (see the youtop-lessons skill)."""
    words = b.get("words", "").strip()
    labels = (f"No written text anywhere except exactly these short labels, large and clearly legible, spelled exactly: {words}. "
              if words else "No written text, letters or numbers anywhere in the image. ")
    # Only the objects go into the prompt ("term -> object (why)" keeps the right-hand side, without the why):
    # medical words in a prompt tempt the generator to draw anatomy or write text.
    import symbols

    def object_text(part):
        part = re.sub(r"\([^)]*\)|\"[^\"]*\"|“[^”]*”", "", part)
        part = re.sub(r"\s+", " ", re.sub(r"\s*,\s*,", ",", part)).strip().strip(",").strip().rstrip(".")
        # Capitals mark the sound-alike for the writer; in a prompt they tempt the generator to letter the word.
        return re.sub(r"\b\w*[A-Z]{2,}\w*\b", lambda m: m.group(0).lower(), part)

    objects, numbers = [], []
    for part in b.get("keywords", "").split(";"):
        pieces = re.split(r"->|→", part)
        is_number = len(pieces) > 1 and symbols.number_term(pieces[0].strip()) is not None
        part = object_text(pieces[-1])
        if part:
            (numbers if is_number else objects).append(part)
    keywords = "; ".join(objects)
    anchor = object_text(re.split(r"->|→", b["anchor"])[-1]) if b.get("anchor") else ""
    # Numbers are small drawings, never big objects and never written digits (the owner, 2026-11-22).
    number_text = ("Numbers appear only as small drawings of little characters, never as written digits: " + "; ".join(numbers) +
                   ". Each is a small, neat drawing in the picture's own style and colours, its little characters crisp, simple and "
                   "easy to tell apart, the same size, side by side from left to right in the order given; it takes only a small "
                   "part of the picture and never competes with the focal point. ") if numbers else ""
    # Sides and directions are shown by small markers on the parts, never by position (the owner, 2026-11-26):
    # image generators mirror pictures, so the prompt tells them the markers, not the frame, carry the sides.
    drawn = " ".join([b.get("scene", ""), keywords, anchor]).lower()
    markers = [s["anchors"][0] for s in symbols.direction_markers(symbols.load()).values() if symbols.has_anchor(drawn, s)]
    marker_text = ("Some parts carry a small marker (" + ", ".join(markers) + "): draw each marker small, clear and exactly on "
                   "the part it belongs to, wherever that part falls in the frame; the markers, not the positions, show the "
                   "sides, so never move or mirror a marker to another part. ") if markers else ""
    folder = cfg.get("imagesFolder")
    where = (f"Save the image with the file name {b['n']} and upload it to: https://drive.google.com/drive/folders/{folder}"
             if folder else f"Save the image with the file name {b['n']}.")
    return (f"{b['n']}. Generate one highly memorable image. Vertical 9:16 aspect ratio. "
            f"Style: {cfg['style']}. "
            f"Colour scheme: {b['palette']}. " +
            (("Composition: ONE dominant focal point that the eye finds at once, drawn boldly and larger than life, in the "
              f"centre and in the strongest light: {anchor}. It carries the main message. Around it only the elements described "
              "below, each clearly recognisable for what it is, in its own place in the frame and linked to the focal point, "
              "smaller than it but never tiny or muddled. ") if anchor else
             ("Composition: ONE dominant focal point that the eye finds at once, drawn boldly and larger than life, in the "
              "centre and in the strongest light: it carries the main message. Only the few elements described below, each of "
              "them big, clearly separated from the others and placed in its own area of the frame, with calm empty space between "
              "them. ")) +
            "Nothing decorative: no crowd, no extra people, no props or background details that are not described. "
            "Faces full of feeling. "
            "Scene: " + b["scene"].rstrip() + " " +
            (f"These objects carry the meaning, so each must be {'' if anchor else 'big, '}unmistakable and clearly recognisable "
             f"for what it is: {keywords}. " if keywords else "") +
            "Every object must be visually meaningful, distinctive and easy to recall, and all of them must be linked by "
            "physical interaction and cause-and-effect inside this one scene. No literal anatomy, no organs, no medical "
            "equipment, no unrelated objects. " + number_text + marker_text + labels +
            (f"These details matter most and must be painted exactly as stated: {b['must'].strip().rstrip('.')}. " if b.get("must") else "") +
            where)


def lint(cfg, b, q, used, problems, warnings):
    w = f"question {b['n']}"
    for key in ("title", "titleAr", "tags", "palette", "scene", "story", "quote", "source", "explanation"):
        if not b.get(key):
            problems.append(f"{w}: '{key}' is missing")
    if q is None:
        problems.append(f"{w}: not in the master question file(s) given")
    elif q.get("unsupported") and not b.get("key") and not b.get("asks"):
        problems.append(f"{w}: {q['unsupported']}; the app cannot ask it yet")
    if b.get("key") and q is not None:
        right = key_override(b, q)
        if any(not 0 <= i < len(q["options"]) for i in right):
            problems.append(f"{w}: 'key: {b['key']}' names an option the question does not have")
        if "x:key-corrected" not in (b.get("tags") or ""):
            problems.append(f"{w}: a corrected key needs the tag x:key-corrected")
        if not b.get("note"):
            problems.append(f"{w}: a corrected key needs a 'note:' saying what the book's key was and why it is wrong")
    for i, line in enumerate(b.get("asks") or [], 1):
        a = parse_ask(line)
        if a is None:
            problems.append(f"{w}: asks line {i} is not 'question | option | option ... = letters'")
    if b.get("asks") and not b.get("note"):
        problems.append(f"{w}: questions written in 'asks:' need a 'note:' saying how they come from the book's question and key")
    if b.get("option") and q is not None:
        fixed = option_override(b, q)
        if fixed is None:
            problems.append(f"{w}: write the corrected option as 'option: C = the corrected text'")
        if "x:key-corrected" not in (b.get("tags") or ""):
            problems.append(f"{w}: a corrected option needs the tag x:key-corrected")
        if not b.get("note"):
            problems.append(f"{w}: a corrected option needs a 'note:' saying what the book printed and why it is wrong")
    if cfg.get("soundAlikes") and not b.get("keywords"):
        warnings.append(f"{w}: no 'keywords' (the owner's rule 2: every abstract term of the answer as a concrete sound-alike object)")
    story, expl = b.get("story") or [], b.get("explanation") or []
    if story and not 8 <= len(story) <= 11:
        warnings.append(f"{w}: the story has {len(story)} sentences (the guide asks for 8 to 11)")
    if expl and not 4 <= len(expl) <= 8:
        warnings.append(f"{w}: the explanation has {len(expl)} sentences (the guide asks for 4 to 8)")
    for i, s in enumerate(story, 1):
        if LATIN.search(s):
            # Allowed only for the labels painted in the picture ("words"), which the story says aloud.
            painted = (b.get("words") or "").lower()
            if not all(x.lower() in painted for x in re.findall(r"[A-Za-z]+", s)):
                warnings.append(f"{w}: story sentence {i} has Latin letters; the story takes no English terms, only labels painted in the picture")
        if len(s) > 190:
            warnings.append(f"{w}: story sentence {i} is {len(s)} characters; it is also the caption, keep it short")
    for i, s in enumerate(expl, 1):
        if len(s) > 260:
            warnings.append(f"{w}: explanation sentence {i} is {len(s)} characters; split it")
    if story and not story[-1].rstrip().endswith((":", "：")):
        warnings.append(f"{w}: the last story sentence should lead into the quotation and end with ':'")
    quote = b.get("quote", "")
    if quote:
        if LATIN.search(quote):
            problems.append(f"{w}: the quotation must be in Arabic")
        # A quotation is used once per book, unless the book allows a few repeats far apart
        # ("quoteRepeats": {"max": 2, "gap": 20}: the owner chose this for Johns Hopkins 2016, 2026-11-21; raised to
        # "max": 3 on 2026-11-22, when the owner left the choice to the agent: "make anything strong and consistent").
        rule = cfg.get("quoteRepeats") or {}
        most, gap = rule.get("max", 1), rule.get("gap", 0)
        hits = used.get(normal(quote), [])
        if len(hits) >= most:
            problems.append(f"{w}: this quotation is already used in {', '.join(h[0] for h in hits)}")
        else:
            near = [h[0] for h in hits if abs(int(h[0][-4:]) - b["n"]) < gap]
            if near:
                problems.append(f"{w}: this quotation is used in {', '.join(near)}, fewer than {gap} lessons away")
        used.setdefault(normal(quote), []).append((lesson_id(cfg, b["n"]), quote, ""))
    if b.get("must") and len(b["must"]) > 260:
        warnings.append(f"{w}: 'must' is long; name only the two or three details the story cannot do without")
    if b.get("words") and len(b["words"]) > 40:
        warnings.append(f"{w}: 'words' is long; image generators misspell more than a few short labels")
    if q is not None and expl and not b.get("asks"):
        answer = (option_override(b, q) or q["options"])[q["answer"]]
        key_words = [x for x in re.findall(r"[A-Za-z]{5,}", answer.lower())]
        text = " ".join(expl).lower()
        if key_words and not any(x in text for x in key_words):
            warnings.append(f"{w}: the explanation never mentions the correct answer ('{answer[:50]}')")


def key_override(b, q):
    """The corrected key of a 'key:' line, as option indexes, or None when the book's key stands."""
    letters = re.findall(r"[A-Za-z]", b.get("key") or "")
    if not letters:
        return None
    return sorted({ord(x.upper()) - ord("A") for x in letters})


def parse_ask(line):
    """One line of 'asks:': "question | option | option ... = A,C" -> {"q", "options", "right"}, or None."""
    m = re.fullmatch(r"(.+?)\s*=\s*([A-Za-z][A-Za-z ,]*)", line.strip())
    if not m:
        return None
    parts = [x.strip() for x in m.group(1).split("|")]
    stem, options = parts[0], [x for x in parts[1:] if x]
    right = sorted({ord(x.upper()) - ord("A") for x in re.findall(r"[A-Za-z]", m.group(2))})
    if not stem or len(options) < 2 or not right or any(not 0 <= i < len(options) for i in right):
        return None
    return {"q": stem, "options": options, "right": right}


def option_override(b, q):
    """The options with a clear misprint corrected ('option: C = text'), or None when there is none or it is unreadable."""
    m = re.fullmatch(r"\s*([A-Za-z])\s*=\s*(.+?)\s*", b.get("option") or "")
    if not m or not 0 <= ord(m.group(1).upper()) - ord("A") < len(q["options"]):
        return None
    options = list(q["options"])
    options[ord(m.group(1).upper()) - ord("A")] = m.group(2)
    return options


def build_lesson(cfg, b, q, book_dir, old, force, problems, warnings):
    n = b["n"]
    lid = lesson_id(cfg, n)
    fix = lambda s: [s := s.replace(a, c) for a, c in cfg.get("typos", {}).items()][-1] if cfg.get("typos") else s
    source = b["source"] if cfg["author"] in b["source"] else f"{cfg['author']}، {b['source']}"
    story = [{"text": t} for t in b["story"]] + [{"text": b["quote"], "quote": source}]
    explain = " ".join(b["explanation"]) + (" " + b["note"] if b.get("note") else "")
    right = key_override(b, q) or sorted(set(q.get("answers") or [q["answer"]]))
    quiz = {"q": fix(q["stem"]), "options": [fix(o) for o in (option_override(b, q) or q["options"])], "answer": right[0],
            "explain": explain, "lang": "en", "source": f"{cfg['sourceLabel']}, Q{q.get('ref', n)}"}
    if len(right) > 1:
        quiz["answers"] = right  # "choose all that apply": the app ticks and checks them together (since 2.5)
    quizzes = [quiz]
    if b.get("asks"):
        # A question the app cannot ask as printed (a matching key) is asked as several questions in one lesson.
        quizzes = []
        for a in filter(None, map(parse_ask, b["asks"])):
            one = {"q": fix(a["q"]), "options": [fix(o) for o in a["options"]], "answer": a["right"][0],
                   "explain": explain, "lang": "en", "source": f"{cfg['sourceLabel']}, Q{q.get('ref', n)}"}
            if len(a["right"]) > 1:
                one["answers"] = a["right"]
            quizzes.append(one)
    if q.get("figure"):
        name = f"figure-q{n:04d}.png"
        published = subprocess.run(["git", "cat-file", "-e", f"origin/content:books/{cfg['id']}/media/{name}"], cwd=ROOT, capture_output=True).returncode == 0
        if published or (book_dir / "media" / name).is_file():
            quiz["image"] = f"gh:books/{cfg['id']}/media/{name}"
        else:
            problems.append(f"question {n}: its stem needs a figure. Download {q['figure']} from Drive and save it as "
                            f"{(book_dir / 'media' / name)}")
    lesson = {"id": lid, "title": b["title"], "titleAr": b["titleAr"], "tags": b["tags"].replace(",", " ").split(),
              "tracks": [{"kind": "story", "lang": "ar", "segments": story},
                         {"kind": "explanation", "lang": "en", "segments": [{"text": t} for t in b["explanation"]]}],
              "quiz": quizzes}
    if old:
        # Keep what later steps added (picture, spots, recorded voice) wherever the text is unchanged.
        if old.get("image"):
            lesson["image"] = old["image"]
        if old.get("marks"):
            # The numbered circles belong to the picture, not to the wording.
            lesson["marks"] = old["marks"]
            for k in ("marks", "answerMarks"):
                if old.get("quiz") and k in old["quiz"][0]:
                    for one in quizzes:
                        one[k] = old["quiz"][0][k]
        for new_track in lesson["tracks"]:
            old_track = next((t for t in old.get("tracks", []) if t.get("kind") == new_track["kind"]), None)
            if not old_track:
                continue
            same = [s["text"] for s in old_track["segments"]] == [s["text"] for s in new_track["segments"]]
            if old_track.get("audio"):
                if same:
                    new_track["audio"] = old_track["audio"]
                elif not force:
                    problems.append(f"question {n}: the {new_track['kind']} has a recorded voice; changing its text would "
                                    f"break the recording. Keep the text, or pass --force and ask for a new recording.")
                else:
                    warnings.append(f"question {n}: the recorded {new_track['kind']} voice was dropped (text changed)")
            for i, seg in enumerate(new_track["segments"]):
                if i < len(old_track["segments"]) and old_track["segments"][i]["text"] == seg["text"]:
                    for k in ("spot", "t"):
                        if k in old_track["segments"][i] and (k != "t" or "audio" in new_track):
                            seg[k] = old_track["segments"][i][k]
                elif "spot" in (old_track["segments"][i] if i < len(old_track["segments"]) else {}):
                    warnings.append(f"question {n}: {new_track['kind']} sentence {i + 1} changed; its spot was dropped, set it again")
    return lesson


def cmd_build(args):
    head, items = parse(args.batch)
    cfg = batch_config(head)
    if cfg.get("soundAlikes"):
        symbol_check(items, few_is_problem=False)
    master = load_master(args.master)
    folder = pathlib.Path(args.folder).resolve()
    book_dir = ensure_folder(folder, cfg)
    if not items:
        die("the batch file has no questions")
    numbers = [b["n"] for b in items]
    if len(set(numbers)) != len(numbers):
        die("a question number is repeated in the batch file")
    ids = {lesson_id(cfg, n) for n in numbers}
    used = used_quotes(book_dir, ids)
    existing = {l["id"]: l for l in all_lessons(book_dir)}
    problems, warnings, lessons = [], [], {}
    for b in items:
        q = master.get(b["n"])
        lint(cfg, b, q, used, problems, warnings)
        if q is not None and all(b.get(k) for k in ("title", "titleAr", "tags", "story", "quote", "source", "explanation")):
            lessons[b["n"]] = build_lesson(cfg, b, q, book_dir, existing.get(lesson_id(cfg, b["n"])), args.force, problems, warnings)
    for line in warnings:
        print("warning:", line)
    if problems:
        die("nothing written:\n  " + "\n  ".join(problems))
    by_chapter = {}
    for n, lesson in lessons.items():
        by_chapter.setdefault(chapter_of(cfg, n), []).append(lesson)
    for name, new in by_chapter.items():
        path, chapter = read_chapter(book_dir, cfg, name, create=True)
        new_ids = {l["id"] for l in new}
        chapter["lessons"] = sorted([l for l in chapter["lessons"] if l["id"] not in new_ids] + new, key=lambda l: l["id"])
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(json.dumps(chapter, ensure_ascii=False) + "\n", encoding="utf-8")
    label = f"{min(numbers):04d}-{max(numbers):04d}"
    work = folder / "work" / cfg["id"] / f"batch-{label}"
    work.mkdir(parents=True, exist_ok=True)
    if pathlib.Path(args.batch).resolve() != (work / "batch.txt").resolve():
        shutil.copyfile(args.batch, work / "batch.txt")
    prompts = work / f"image-prompts-{min(numbers)}-{max(numbers)}.json"
    with_scene = [b for b in items if b.get("scene") and b.get("palette")]
    prompts.write_text(json.dumps({"items": [{"info": image_prompt(cfg, b)} for b in with_scene]}, ensure_ascii=False, indent=1) + "\n", encoding="utf-8")
    check = subprocess.run([sys.executable, str(ROOT / "tools/content/validate.py"), str(book_dir), str(prompts)])
    if check.returncode != 0:
        die("validate.py found problems (above); fix the batch file and build again")
    print(f"\n{len(lessons)} lesson(s) built into {book_dir}")
    print(f"Image prompts for the owner (send them this file): {prompts}")
    print(f"Publish:  python3 tools/content/publish.py {folder} -m \"{cfg['sourceLabel']}: lessons {min(numbers)}-{max(numbers)}\"")


def symbol_check(items, few_is_problem=True):
    """The owner's rule 5: a recurring concept is drawn with its audited symbol (tools/content/symbols.py), and every
    lesson carries at least four recurring symbols; rule 7: an anchor and twelve symbols, five of them one-offs (the
    owner, 2026-11-22). The prompts step refuses a lesson that falls short; the build only warns, so that lessons
    drawn before these rules can still be rebuilt."""
    import symbols
    problems, local = symbols.check_items(items)
    seven, crowded = symbols.rule7(items)
    # Sides and directions are carried by their markers, never by position (the owner, 2026-11-26).
    few = symbols.too_few(items) + seven + symbols.direction_problems(items)
    for line in crowded:
        print("warning:", line)
    if local:
        print(f"{len(local)} keyword(s) are not recurring symbols (fine for one-offs; add one to content/symbols.json if it will come back):")
        for line in local:
            print("   ", line)
    if few_is_problem:
        problems += few
    else:
        # The build only warns when a concept is drawn otherwise: its picture may have been drawn before the concept had
        # a symbol (since 2026-11-26 a hundred new symbols are made with every hundred lessons). The prompts step refuses.
        older = [p for p in problems if "is a recurring concept; draw it as" in p]
        problems = [p for p in problems if p not in older]
        for line in few + older:
            print("warning:", line)
    if problems:
        die("recurring symbols:\n  " + "\n  ".join(problems))


def cmd_prompts(args):
    """The image prompts from 'palette', 'scene' and 'words' alone, so the owner can start on the pictures at once."""
    head, items = parse(args.batch)
    cfg = batch_config(head)
    missing = [str(b["n"]) for b in items if not (b.get("scene") and b.get("palette"))]
    if missing or not items:
        die("these questions have no 'scene' and 'palette' yet: " + (", ".join(missing) or "none in the file"))
    symbol_check(items)
    numbers = [b["n"] for b in items]
    out = pathlib.Path(args.output or pathlib.Path(args.batch).with_name(f"image-prompts-{min(numbers)}-{max(numbers)}.json"))
    out.write_text(json.dumps({"items": [{"info": image_prompt(cfg, b)} for b in items]}, ensure_ascii=False, indent=1) + "\n", encoding="utf-8")
    if subprocess.run([sys.executable, str(ROOT / "tools/content/validate.py"), str(out)]).returncode != 0:
        die("validate.py found problems (above)")
    print(f"Send the owner this file now, and each prompt as text: {out}")


def cmd_questions(args):
    master = load_master(args.master)
    for n in range(args.first, args.last + 1):
        q = master.get(n)
        if not q:
            print(f"== {n}: NOT IN THE FILES GIVEN\n")
            continue
        print(f"== {n}" + (f"   (book number {q['ref']})" if q.get("ref") else "") + ("   [needs its figure: " + q["figure"] + "]" if q.get("figure") else ""))
        print(q["stem"])
        right = set(q.get("answers") or [q["answer"]])
        for i, o in enumerate(q["options"]):
            print(("  * " if i in right else "  - ") + o)
        if q.get("explanation"):
            print("Book: " + q["explanation"])
        for c in q.get("corrections", []):
            print(f"Corrected {c['field']}: was {c['was']}, now {c['now']} ({c['why']})")
        print()


def cmd_new(args):
    cfg = book_config(args.book)
    master = load_master(args.master)
    lines = [f"book: {cfg['id']}", "# One block per question. See the top of tools/content/batch.py for the format,",
             "# and .claude/skills/youtop-lessons/SKILL.md for how to write each part.", ""]
    for n in range(args.first, args.last + 1):
        q = master.get(n)
        if not q:
            die(f"question {n} is not in the master file(s) given")
        lines += [f"== {n}"] + ([f"# book number {q['ref']}"] if q.get("ref") else []) + ["# " + q["stem"].replace("\n", " "), "# answer: " + q["options"][q["answer"]]]
        if q.get("figure"):
            lines.append("# this question has a figure (it is shown above the options)")
        lines += ["title: ", "titleAr: ", "tags: ", "palette: ", "words: ", "keywords: ", "anchor: ", "scene: ", "story:", "quote: ", "source: ", "explanation:", ""]
    pathlib.Path(args.output).write_text("\n".join(lines), encoding="utf-8")
    print(f"wrote {args.output}: fill it in, then  python3 tools/content/batch.py build {args.output} --master ...")


def cmd_quotes(args):
    cfg = book_config(args.book)
    book_dir = ensure_folder(pathlib.Path(args.folder).resolve(), cfg)
    for lid, text, source in sorted(e for entries in used_quotes(book_dir).values() for e in entries):
        print(f"{lid[-4:]}  {source.replace(cfg['author'] + '، ', '')}: {text}")


def cmd_status(args):
    cfg = book_config(args.book)
    book_dir = ensure_folder(pathlib.Path(args.folder).resolve(), cfg)
    done = {}
    for lesson in all_lessons(book_dir):
        tracks = {t["kind"]: t for t in lesson.get("tracks", [])}
        lacks = []
        if not lesson.get("image"):
            lacks.append("picture")
        elif not any(s.get("spot") for t in tracks.values() for s in t["segments"]):
            lacks.append("spots")
        elif not lesson.get("marks"):
            lacks.append("marks")
        for kind in ("story", "explanation"):
            if kind in tracks and not tracks[kind].get("audio"):
                lacks.append(f"{kind} voice (phone voice is used)")
        done[int(lesson["id"][-4:])] = lacks
    total = cfg.get("questions", max(done, default=0))
    missing = [n for n in range(1, total + 1) if n not in done]
    def ranges(ns):
        out, ns = [], sorted(ns)
        for n in ns:
            if out and n == out[-1][1] + 1:
                out[-1][1] = n
            else:
                out.append([n, n])
        return ", ".join(f"{a}-{b}" if a != b else str(a) for a, b in out) or "none"
    print(f"{cfg['id']}: {len(done)} of {total} questions are lessons.")
    print("Not written yet:", ranges(missing))
    for what in ("picture", "spots", "marks", "story voice (phone voice is used)", "explanation voice (phone voice is used)"):
        print(f"Without {what}:", ranges([n for n, lacks in done.items() if what in lacks]))


def cmd_pictures(args):
    from PIL import Image
    cfg = book_config(args.book)
    folder = pathlib.Path(args.folder).resolve()
    book_dir = ensure_folder(folder, cfg)
    media = book_dir / "media"
    media.mkdir(parents=True, exist_ok=True)
    check = folder / "check" / cfg["id"]
    check.mkdir(parents=True, exist_ok=True)
    found = {}
    for f in sorted(pathlib.Path(args.source).iterdir()):
        m = re.fullmatch(r"(\d+)(\.\w+)?", f.name)
        if m and f.is_file():
            found[int(m.group(1))] = f
    if not found:
        die(f"no pictures named by question number (41.png, 42, ...) in {args.source}")
    done = []
    for path in sorted((book_dir / "chapters").glob("*.json")):
        chapter = json.loads(path.read_text(encoding="utf-8"))
        changed = False
        for lesson in chapter["lessons"]:
            n = int(lesson["id"][-4:])
            if n not in found:
                continue
            name = f"{lesson['id']}.webp"
            ref = f"gh:books/{cfg['id']}/media/{name}"
            if lesson.get("image") and lesson["image"] != ref and not args.replace:
                print(f"question {n}: already has {lesson['image']}; left alone (use --replace NAME-b to publish a corrected picture)")
                continue
            if args.replace:
                name = f"{lesson['id']}-{args.replace}.webp"
                ref = f"gh:books/{cfg['id']}/media/{name}"
            im = Image.open(found[n]).convert("RGB")
            w, h = im.size
            im = im.resize((1080, round(h * 1080 / w)), Image.LANCZOS)
            if lesson.get("image") != ref or not (media / name).exists():
                im.save(media / name, "WEBP", quality=82)
            lesson["image"] = ref
            grid(im).save(check / f"{n}-grid.jpg", quality=80)
            done.append(n)
            changed = True
        if changed:
            path.write_text(json.dumps(chapter, ensure_ascii=False) + "\n", encoding="utf-8")
    left = sorted(set(found) - set(done))
    print(f"{len(done)} picture(s) set: {done}")
    if left:
        print(f"no lesson (or already has a picture) for: {left}")
    print(f"Look at each {check}/<n>-grid.jpg (the grid is in tenths: read x and y off it), check the picture against its story,\n"
          f"then write the spots file and run:  python3 tools/content/batch.py spots spots.txt --folder {folder}")


def grid(im):
    """The picture with a labelled grid in tenths, so spot positions can be read off instead of guessed."""
    from PIL import ImageDraw, ImageFont
    im = im.copy().resize((540, round(im.height * 540 / im.width)))
    d = ImageDraw.Draw(im, "RGBA")
    font = ImageFont.load_default()
    for i in range(1, 10):
        x, y = im.width * i / 10, im.height * i / 10
        d.line([(x, 0), (x, im.height)], fill=(255, 255, 255, 110), width=1)
        d.line([(0, y), (im.width, y)], fill=(255, 255, 255, 110), width=1)
        for pos, text in (((x + 2, 2), f"{i * 10}"), ((2, y + 2), f"{i * 10}")):
            d.rectangle([pos, (pos[0] + 14, pos[1] + 11)], fill=(0, 0, 0, 170))
            d.text(pos, text, fill=(255, 255, 0, 255), font=font)
    return im


def cmd_spots(args):
    head, items = parse(args.spots, multi=set(), keys={"story", "explanation"})
    cfg = batch_config(head)
    folder = pathlib.Path(args.folder).resolve()
    book_dir = ensure_folder(folder, cfg)
    wanted = {lesson_id(cfg, b["n"]): b for b in items}
    problems, drawn = [], []
    for path in sorted((book_dir / "chapters").glob("*.json")):
        chapter = json.loads(path.read_text(encoding="utf-8"))
        changed = False
        for lesson in chapter["lessons"]:
            b = wanted.pop(lesson["id"], None)
            if not b:
                continue
            if not lesson.get("image"):
                problems.append(f"question {b['n']}: the lesson has no picture yet (run 'pictures' first)")
                continue
            for track in lesson["tracks"]:
                if track["kind"] not in b:
                    continue
                segs = [s for s in track["segments"] if not s.get("quote")]
                spots = [s.strip() for s in b[track["kind"]].split("|")]
                if len(spots) != len(segs):
                    problems.append(f"question {b['n']}: the {track['kind']} has {len(segs)} sentences (without the quotation) but {len(spots)} spots")
                    continue
                for seg, sp in zip(segs, spots):
                    seg.pop("spot", None)
                    if sp in ("-", ""):
                        continue
                    try:
                        x, y, r = (float(v) / 100 for v in sp.replace(",", " ").split())
                    except ValueError:
                        problems.append(f"question {b['n']}: '{sp}' is not 'x y r' in hundredths, or '-'")
                        continue
                    if not (0 <= x <= 1 and 0 <= y <= 1 and 0.03 <= r <= 0.6):
                        problems.append(f"question {b['n']}: spot '{sp}' is outside the picture or its radius is not 3 to 60")
                        continue
                    seg["spot"] = {"x": round(x, 3), "y": round(y, 3), "r": round(r, 3)}
            changed = True
            drawn.append(lesson)
        if changed and not problems:
            path.write_text(json.dumps(chapter, ensure_ascii=False) + "\n", encoding="utf-8")
    for lid in wanted:
        problems.append(f"{lid}: no such lesson in {book_dir}")
    if problems:
        die("nothing written:\n  " + "\n  ".join(problems))
    out = folder / "check" / cfg["id"]
    out.mkdir(parents=True, exist_ok=True)
    sheets = draw_spots(cfg, book_dir, drawn, out)
    print(f"spots set for {len(drawn)} lesson(s).")
    if sheets:
        print(f"Now LOOK at {out}/<n>-spots.jpg: every circle (S1.. story, E1.. explanation) must sit on what its sentence is about.")
    else:
        print("The pictures are not on this machine, so the circles could not be drawn. (They are in the folder after 'pictures'.)")


# The colours of the numbered circles; the app uses the same ones (ui/Marks.kt, MarkColors).
MARK_COLOURS = [(255, 210, 63), (76, 201, 240), (255, 107, 107), (122, 229, 130), (199, 125, 255),
                (255, 159, 28), (46, 196, 182), (244, 114, 182), (184, 242, 230), (224, 224, 224)]
MARK_WHAT, MARK_MEANS, MARK_MAX = 48, 110, 10


def parse_marks(path):
    book, items, cur = None, [], None
    for n, raw in enumerate(pathlib.Path(path).read_text(encoding="utf-8").splitlines(), 1):
        line = raw.strip()
        if not line or line.startswith("#"):
            continue
        m = re.fullmatch(r"==\s*(\d+)\s*=*", line)
        if m:
            cur = {"n": int(m.group(1)), "marks": [], "question": [], "answer": []}
            items.append(cur)
            continue
        key, _, value = line.partition(":")
        key, value = key.strip(), value.strip()
        if cur is None:
            if key == "book":
                book = value
                continue
            die(f"{path}:{n}: the first line must be 'book: <book id>'")
        if key in ("question", "answer"):
            try:
                cur[key] = [int(v) for v in value.replace(",", " ").split()]
            except ValueError:
                die(f"{path}:{n}: '{key}:' takes circle numbers")
        elif key.isdigit():
            parts = [x.strip() for x in value.split("|")]
            if int(key) != len(cur["marks"]) + 1 or len(parts) != 3:
                die(f"{path}:{n}: expected '{len(cur['marks']) + 1}: x y r | what | means'")
            cur["marks"].append(parts)
        else:
            die(f"{path}:{n}: not understood: {line[:60]}")
    if not book:
        die(f"{path}: the first line must be 'book: <book id>'")
    return book, items


def cmd_marks(args):
    if args.draft:
        return marks_draft(args)
    book, items = parse_marks(args.what[0])
    cfg = book_config(book)
    folder = pathlib.Path(args.folder).resolve()
    book_dir = ensure_folder(folder, cfg)
    wanted = {lesson_id(cfg, b["n"]): b for b in items}
    problems, drawn, todo = [], [], []
    for path in sorted((book_dir / "chapters").glob("*.json")):
        chapter = json.loads(path.read_text(encoding="utf-8"))
        changed = False
        for lesson in chapter["lessons"]:
            b = wanted.pop(lesson["id"], None)
            if not b:
                continue
            w = f"question {b['n']}"
            if not lesson.get("image"):
                problems.append(f"{w}: the lesson has no picture yet")
                continue
            marks = []
            for i, (sp, what, means) in enumerate(b["marks"], 1):
                try:
                    x, y, r = (float(v) / 100 for v in sp.replace(",", " ").split())
                except ValueError:
                    problems.append(f"{w}: circle {i}: '{sp}' is not 'x y r' in hundredths")
                    continue
                if not (0 <= x <= 1 and 0 <= y <= 1 and 0.03 <= r <= 0.6):
                    problems.append(f"{w}: circle {i} is outside the picture or its radius is not 3 to 60")
                if not what or not means:
                    problems.append(f"{w}: circle {i} needs both 'what' and 'means'")
                if re.search(r"[\u0600-\u06FF]", what + means):
                    problems.append(f"{w}: circle {i}: the table is in English only")
                if len(what) > MARK_WHAT or len(means) > MARK_MEANS:
                    problems.append(f"{w}: circle {i}: 'what' is {len(what)} characters (at most {MARK_WHAT}), 'means' {len(means)} (at most {MARK_MEANS}); the table must stay small")
                marks.append({"spot": {"x": round(x, 3), "y": round(y, 3), "r": round(r, 3)}, "what": what, "means": means})
            if not 2 <= len(marks) <= MARK_MAX:
                problems.append(f"{w}: {len(marks)} circles; give 2 to {MARK_MAX}")
            for key in ("question", "answer"):
                if any(not 1 <= v <= len(marks) for v in b[key]):
                    problems.append(f"{w}: '{key}:' names a circle that does not exist")
            if not b["answer"]:
                problems.append(f"{w}: 'answer:' must name at least one circle")
            if set(b["question"]) & set(b["answer"]):
                problems.append(f"{w}: a circle is in both 'question:' and 'answer:'; the question's circles must not give the answer away")
            if not lesson.get("quiz"):
                problems.append(f"{w}: the lesson has no question")
                continue
            lesson["marks"] = marks
            for one in lesson["quiz"]:  # a lesson with several questions shows the same circles for each
                one["marks"] = b["question"]
                one["answerMarks"] = b["answer"]
            changed = True
            drawn.append(lesson)
        if changed:
            todo.append((path, chapter))
    for lid in wanted:
        problems.append(f"{lid}: no such lesson in {book_dir}")
    if problems:
        die("nothing written:\n  " + "\n  ".join(problems))
    for path, chapter in todo:
        path.write_text(json.dumps(chapter, ensure_ascii=False) + "\n", encoding="utf-8")
    out = folder / "check" / cfg["id"]
    out.mkdir(parents=True, exist_ok=True)
    count = draw_marks(cfg, book_dir, drawn, out)
    print(f"marks set for {len(drawn)} lesson(s).")
    if count:
        print(f"Now LOOK at {out}/<n>-marks.jpg: every numbered circle must sit on what its row says. "
              "A ring drawn twice is a circle of the question (Q) or of the answer (A).")


def marks_draft(args):
    """A marks file to start from: one line per distinct spot, with the sentences spoken over it."""
    if len(args.what) != 3:
        die("usage: batch.py marks <book> <from> <to> --draft marks.txt")
    cfg = book_config(args.what[0])
    first, last = int(args.what[1]), int(args.what[2])
    folder = pathlib.Path(args.folder).resolve()
    book_dir = ensure_folder(folder, cfg)
    out = folder / "check" / cfg["id"]
    out.mkdir(parents=True, exist_ok=True)
    lines = [f"book: {cfg['id']}",
             "# Per circle:  <number>: x y r | what is in the picture | what it stands for   (English only)",
             "# The '#' lines under a circle are the sentences spoken over it (S story, E explanation); delete them or leave them.", ""]
    drafted = []
    for lesson in all_lessons(book_dir):
        n = int(lesson["id"][-4:])
        if not first <= n <= last or not lesson.get("image"):
            continue
        spots = {}
        for track, letter in zip(lesson["tracks"], "SE"):
            for i, seg in enumerate(track["segments"], 1):
                sp = seg.get("spot")
                if sp:
                    spots.setdefault((sp["x"], sp["y"], sp.get("r", 0.15)), []).append(f"{letter}{i} {seg['text']}")
        q = lesson["quiz"][0] if lesson.get("quiz") else None
        lines.append(f"== {n}")
        if q:
            lines += ["# Q: " + q["q"].replace("\n", " "), "# ANSWER: " + q["options"][q["answer"]],
                      "# other options: " + " / ".join(o for i, o in enumerate(q["options"]) if i != q["answer"])]
        marks = []
        for i, ((x, y, r), said) in enumerate(spots.items(), 1):
            lines.append(f"{i}: {round(x * 100)} {round(y * 100)} {round(r * 100)} | WHAT | MEANS")
            lines += [f"#    {t}" for t in said]
            marks.append({"spot": {"x": x, "y": y, "r": r}})
        lines += ["question: ", "answer: ", ""]
        drafted.append(dict(lesson, marks=marks))
    pathlib.Path(args.draft).write_text("\n".join(lines), encoding="utf-8")
    draw_marks(cfg, book_dir, drafted, out, suffix="draft")
    print(f"wrote {args.draft} for {len(drafted)} lesson(s); the pictures with these circles are {out}/<n>-draft.jpg")


def lesson_picture(cfg, book_dir, lesson, out):
    from PIL import Image
    name = lesson["image"].rsplit("/", 1)[-1]
    src = book_dir / "media" / name
    if not src.is_file():
        src = out / name
        if not src.is_file():
            blob = subprocess.run(["git", "cat-file", "-p", f"origin/content:books/{cfg['id']}/media/{name}"], cwd=ROOT, capture_output=True)
            if blob.returncode != 0:
                return None
            src.write_bytes(blob.stdout)
    im = Image.open(src).convert("RGB")
    return im.resize((720, round(im.height * 720 / im.width)))


def draw_marks(cfg, book_dir, lessons, out, suffix="marks"):
    try:
        from PIL import ImageDraw, ImageFont
    except ImportError:
        return 0
    try:
        font = ImageFont.truetype("DejaVuSans-Bold.ttf", 22)
    except OSError:
        font = ImageFont.load_default()
    count = 0
    for lesson in lessons:
        im = lesson_picture(cfg, book_dir, lesson, out)
        if im is None:
            continue
        d = ImageDraw.Draw(im, "RGBA")
        quiz = lesson["quiz"][0] if lesson.get("quiz") else {}
        for i, mark in enumerate(lesson["marks"], 1):
            sp, colour = mark["spot"], MARK_COLOURS[(i - 1) % len(MARK_COLOURS)]
            x, y, r = sp["x"] * im.width, sp["y"] * im.height, sp["r"] * im.width
            d.ellipse([x - r, y - r, x + r, y + r], outline=colour + (255,), width=4)
            role = "Q" if i in quiz.get("marks", []) else "A" if i in quiz.get("answerMarks", []) else ""
            if role:
                d.ellipse([x - r - 7, y - r - 7, x + r + 7, y + r + 7], outline=colour + (255,), width=2)
            # The number sits on the ring (upper left), as in the app, so it does not hide what is circled.
            bx, by = x - r * 0.707, y - r * 0.707
            bx, by = min(max(bx, 18), im.width - 18), min(max(by, 18), im.height - 18)
            d.ellipse([bx - 17, by - 17, bx + 17, by + 17], fill=colour + (255,), outline=(0, 0, 0, 255), width=2)
            label = f"{i}{role}"
            tw = d.textlength(label, font=font)
            d.text((bx - tw / 2, by - 13), label, fill=(0, 0, 0, 255), font=font)
        im.save(out / f"{int(lesson['id'][-4:])}-{suffix}.jpg", quality=82)
        count += 1
    return count


def draw_spots(cfg, book_dir, lessons, out):
    try:
        from PIL import Image, ImageDraw, ImageFont
    except ImportError:
        return 0
    count = 0
    for lesson in lessons:
        name = lesson["image"].rsplit("/", 1)[-1]
        src = book_dir / "media" / name
        if not src.is_file():
            blob = subprocess.run(["git", "cat-file", "-p", f"origin/content:books/{cfg['id']}/media/{name}"], cwd=ROOT, capture_output=True)
            if blob.returncode != 0:
                continue
            src = out / name
            src.write_bytes(blob.stdout)
        im = Image.open(src).convert("RGB")
        im = im.resize((540, round(im.height * 540 / im.width)))
        d = ImageDraw.Draw(im, "RGBA")
        font = ImageFont.load_default()
        for track, letter, colour in zip(lesson["tracks"], "SE", ((255, 220, 0), (0, 255, 255))):
            for i, seg in enumerate(track["segments"], 1):
                sp = seg.get("spot")
                if not sp:
                    continue
                x, y, r = sp["x"] * im.width, sp["y"] * im.height, sp["r"] * im.width
                d.ellipse([x - r, y - r, x + r, y + r], outline=colour + (255,), width=2)
                d.rectangle([x - 9, y - 6, x + 11, y + 7], fill=(0, 0, 0, 190))
                d.text((x - 7, y - 5), f"{letter}{i}", fill=colour + (255,), font=font)
        im.save(out / f"{int(lesson['id'][-4:])}-spots.jpg", quality=80)
        count += 1
    return count


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    sub = ap.add_subparsers(dest="cmd", required=True)
    def add(name, fn, *positional, master=False, folder=True):
        p = sub.add_parser(name)
        for arg, kind in positional:
            p.add_argument(arg, type=kind)
        if master:
            p.add_argument("--master", nargs="+")
        if folder:
            p.add_argument("--folder", default=str(DEFAULT_FOLDER))
        p.set_defaults(fn=fn)
        return p
    add("questions", cmd_questions, ("book", str), ("first", int), ("last", int), master=True, folder=False)
    add("quotes", cmd_quotes, ("book", str))
    add("new", cmd_new, ("book", str), ("first", int), ("last", int), master=True, folder=False).add_argument("-o", "--output", required=True)
    add("prompts", cmd_prompts, ("batch", str), folder=False).add_argument("-o", "--output")
    add("build", cmd_build, ("batch", str), master=True).add_argument("--force", action="store_true", help="allow changing a text that has a recorded voice")
    add("pictures", cmd_pictures, ("book", str), ("source", str)).add_argument("--replace", metavar="SUFFIX", help="publish corrected pictures under a new name, e.g. --replace b")
    add("spots", cmd_spots, ("spots", str))
    m = add("marks", cmd_marks)
    m.add_argument("what", nargs="+", help="a marks file, or with --draft: <book> <from> <to>")
    m.add_argument("--draft", metavar="FILE", help="write a marks file to fill in, from the lessons' spots")
    add("status", cmd_status, ("book", str))
    args = ap.parse_args()
    args.fn(args)


if __name__ == "__main__":
    main()
