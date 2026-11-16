#!/usr/bin/env python3
"""Makes the recorded voice of lessons: a woman narrates, each character speaks with its own voice,
and every sentence's start time is written into the lesson, so the circle moves exactly with it.

    voice.py fill  voice.txt batch.txt            put the story lines and the quotation into batch.txt
    voice.py make  voice.txt [--folder F] [--only 41 42] [--no-explanation]
                                                  the audio into F/books/<id>/media and 't' into the lessons
    voice.py check voice.txt [--folder F] [--only 41 42]
                                                  "listens" to the audio made: a speech recogniser on this machine
                                                  writes down what it hears, sentence by sentence, and every
                                                  sentence that does not match its text is listed (needs
                                                  `pip install faster-whisper`; nothing is sent anywhere)

Needs the `edge-tts` package (a virtualenv is fine: <venv>/bin/python tools/content/voice.py ...) and
ffmpeg. It sends the lesson text to Microsoft's speech service; the owner allowed that on
2026-11-16. Voices, speeds and pauses: content/voices.json. Publish afterwards with publish.py.

The voice file (UTF-8). Under "story:" ONE LINE PER SENTENCE of the lesson (the quotation last), each
made of parts "role: text" joined by " || ". The text is FULLY VOWELLED Arabic, with a sukun where
the voice should pause (end of sentence, before a comma): voices guess wrongly without it. Roles:
N the narrator, Q the quotation (narrator, slower), or a character from voices.json (man, man2,
young, old-man, woman, old-woman); add "!" for a shout and "~" for a whisper (old-man!, young~).

    book: shahzad-2015
    say: azygos=AZ-ih-gus; NETT=N E T T      (optional: how the English voice should say a word)
    == 41
    story:
    N: فِي فَجْرٍ أَزْرَقْ، اِنْتَظَرَ الحَدَّادُ الإِشَارَةْ.
    young!: شُدُّوا الحِبَالْ! || N: صَاحَ الصَّبِيَّانْ.
    Q: ...

What the learner reads is made from these lines by `fill`: the vowels are taken off (shadda and
tanwin fath stay) and a character's words are put between « ». `make` refuses a lesson whose
published sentences differ from the lines here, so text and voice cannot drift apart. The English
explanation is read from the lesson itself, one voice.
"""
import argparse, array, asyncio, hashlib, json, math, pathlib, re, subprocess, sys, wave

ROOT = pathlib.Path(__file__).resolve().parents[2]
RATE = 44100
STRIP = re.compile("[ٌٍَُِْٰ]")   # all vowel marks but shadda and tanwin fath


def die(msg):
    sys.exit(f"voice.py: {msg}")


def shown(text):
    """The sentence as the learner reads it: without the vowel marks the voice needed."""
    text = STRIP.sub("", text)
    text = re.sub("(\\bال.)\u0651", "\\1", text)     # no shadda on the letter after "al-": it is not written
    return re.sub(r"\s+", " ", text).strip()


def parse(path):
    book, say, lessons, cur, mode = None, {}, [], None, None
    for n, raw in enumerate(pathlib.Path(path).read_text(encoding="utf-8").splitlines(), 1):
        line = raw.strip()
        if not line or line.startswith("#"):
            continue
        m = re.fullmatch(r"==\s*(\d+)\s*=*", line)
        if m:
            cur = {"n": int(m.group(1)), "story": []}
            lessons.append(cur)
            mode = None
        elif cur is None and line.startswith("book:"):
            book = line[5:].strip()
        elif cur is None and line.startswith("say:"):
            for pair in line[4:].split(";"):
                if "=" in pair:
                    a, b = pair.split("=", 1)
                    say[a.strip()] = b.strip()
        elif line == "story:":
            mode = "story"
        elif cur is not None and mode == "story":
            parts = []
            for part in line.split("||"):
                role, sep, text = part.strip().partition(":")
                if not sep or not text.strip() or " " in role.strip():
                    die(f"{path}:{n}: each part is 'role: text' (parts joined by ' || ')")
                parts.append((role.strip(), text.strip()))
            cur["story"].append(parts)
        else:
            die(f"{path}:{n}: not understood: {line[:50]}")
    if not book:
        die(f"{path}: the first line must be 'book: <book id>'")
    return book, say, lessons


def display(parts):
    """A sentence's parts as one caption: a character's words between « »."""
    out = []
    for role, text in parts:
        t = shown(text)
        out.append(t if role in ("N", "Q") else f"«{t}»")
    return " ".join(out)


def cmd_fill(args):
    book, _, lessons = parse(args.voice)
    by = {l["n"]: l for l in lessons}
    out, cur, done, filling = [], None, 0, False
    for raw in pathlib.Path(args.batch).read_text(encoding="utf-8").splitlines():
        line = raw.strip()
        m = re.fullmatch(r"==\s*(\d+)\s*=*", line)
        if m:
            cur = by.get(int(m.group(1)))
        if cur and filling and line and not re.match(r"[A-Za-z]+:", line) and not line.startswith(("#", "==")):
            continue                       # an old story line: replaced by the lines from the voice file
        filling = False
        if line.startswith("quote:") and cur:
            said = [p for p in cur["story"] if p[0][0] == "Q"]
            if said:
                raw = f"quote: «{display(said[0])}»"
        out.append(raw)
        if line == "story:" and cur:
            out += [display(p) for p in cur["story"] if p[0][0] != "Q"]
            done += 1
            filling = True
    pathlib.Path(args.batch).write_text("\n".join(out) + "\n", encoding="utf-8")
    print(f"story lines and quotations written for {done} lesson(s).")


# ---- sound ---------------------------------------------------------------------

def speaker(cfg, role, n):
    """Voice and manner of a role in lesson n."""
    narrator = cfg["narrators"][n % len(cfg["narrators"])]
    mood = role[-1] if role[-1] in cfg["moods"] else ""
    name = role[:-1] if mood else role
    if name == "N":
        spec = dict(cfg["narration"], voice=narrator)
    elif name == "Q":
        spec = dict(cfg["quotation"], voice=narrator)
    elif name in cfg["roles"]:
        spec = dict(cfg["roles"][name])
        if spec.pop("female", False):
            # A woman in the story never sounds like the lesson's narrator.
            others = [v for v in cfg["narrators"] if v != narrator]
            spec["voice"] = others[(n // len(cfg["narrators"])) % len(others)]
    else:
        die(f"lesson {n}: unknown role '{role}' (see content/voices.json)")
    if mood:
        m = cfg["moods"][mood]
        spec["rate"] = f"{int(spec.get('rate', '+0%')[:-1]) + m['rate']:+d}%"
        spec["pitch"] = f"{int(spec.get('pitch', '+0Hz')[:-2]) + m['pitch']:+d}Hz"
        spec["volume"] = m["volume"]
    return spec


def clip(cache, text, spec):
    """One spoken part as 16-bit mono samples: silence trimmed at both ends, levelled."""
    import edge_tts
    key = hashlib.sha256(json.dumps([text, spec], ensure_ascii=False, sort_keys=True).encode()).hexdigest()[:20]
    wav = cache / f"{key}.wav"
    if not wav.is_file():
        mp3 = cache / f"{key}.mp3"
        args = {k: spec[k] for k in ("voice", "rate", "pitch", "volume") if k in spec}
        err = None
        for attempt in range(5):
            try:
                asyncio.run(edge_tts.Communicate(text, **args).save(str(mp3)))
                err = None
                break
            except Exception as e:      # the service refuses now and then; wait and ask again
                err = e
                import time
                time.sleep(3 + 4 * attempt)
        if err:
            die(f"the speech service gave no sound for [{spec['voice']}] {text[:40]}: {err}")
        trim = "silenceremove=start_periods=1:start_threshold=-50dB,areverse,silenceremove=start_periods=1:start_threshold=-50dB,areverse"
        r = subprocess.run(["ffmpeg", "-y", "-v", "error", "-i", str(mp3), "-af", f"aresample={RATE},{trim}", "-ac", "1", "-c:a", "pcm_s16le", str(wav)], capture_output=True, text=True)
        if r.returncode:
            die(r.stderr[:300])
        mp3.unlink()
    with wave.open(str(wav)) as w:
        samples = array.array("h", w.readframes(w.getnframes()))
    if not samples:
        die(f"empty sound for: {text[:40]}")
    # Every voice comes at its own loudness: bring each part to the same level (a whisper stays lower).
    rms = math.sqrt(sum(s * s for s in samples) / len(samples)) or 1.0
    target = 3300 * (0.6 if spec.get("volume", "").startswith("-") else 1.15 if spec.get("volume") else 1.0)
    gain = min(target / rms, 30000 / max(1, max(abs(s) for s in samples)))
    return array.array("h", (int(s * gain) for s in samples))


def build(cache, out, segments):
    """segments: [[(text, spec, pause_after_ms), ...], ...] -> writes out (m4a), returns each segment's start in ms."""
    pcm, starts = array.array("h"), []
    for parts in segments:
        starts.append(round(len(pcm) * 1000 / RATE))
        for text, spec, pause in parts:
            before = spec.get("before", 0)
            if before and pcm:
                pcm.extend([0] * int(RATE * before / 1000))
                starts[-1] = round(len(pcm) * 1000 / RATE) if parts[0][0] == text else starts[-1]
            pcm.extend(clip(cache, text, spec))
            pcm.extend([0] * int(RATE * pause / 1000))
    wav = cache / "joined.wav"
    with wave.open(str(wav), "w") as w:
        w.setnchannels(1); w.setsampwidth(2); w.setframerate(RATE); w.writeframes(pcm.tobytes())
    out.parent.mkdir(parents=True, exist_ok=True)
    r = subprocess.run(["ffmpeg", "-y", "-v", "error", "-i", str(wav), "-af", "highpass=f=70,alimiter=limit=0.89", "-c:a", "aac", "-b:a", "96k", str(out)], capture_output=True, text=True)
    wav.unlink()
    if r.returncode:
        die(r.stderr[:300])
    return starts, round(len(pcm) / RATE, 1)


def cmd_make(args):
    book, say, lessons = parse(args.voice)
    cfg = json.loads((ROOT / "content" / "voices.json").read_text(encoding="utf-8"))
    bookcfg = json.loads((ROOT / "content" / "books" / f"{book}.json").read_text(encoding="utf-8"))
    folder = pathlib.Path(args.folder).resolve()
    book_dir = folder / "books" / book
    cache = folder / "voice-cache"
    cache.mkdir(parents=True, exist_ok=True)
    wanted = {f"{bookcfg['short']}-q{l['n']:04d}": l for l in lessons if not args.only or l["n"] in args.only}
    pause = cfg["pause"]
    for path in sorted((book_dir / "chapters").glob("*.json")):
        chapter = json.loads(path.read_text(encoding="utf-8"))
        changed = False
        for lesson in chapter["lessons"]:
            v = wanted.pop(lesson["id"], None)
            if not v:
                continue
            n = v["n"]
            for track in lesson["tracks"]:
                segs = track["segments"]
                if track["kind"] == "story":
                    if len(v["story"]) != len(segs):
                        die(f"lesson {n}: the story has {len(segs)} sentences but the voice file {len(v['story'])} lines")
                    plan = []
                    for i, (parts, seg) in enumerate(zip(v["story"], segs)):
                        if display(parts).strip("«»") != seg["text"].strip("«»"):
                            die(f"lesson {n}, sentence {i + 1}: the voice line and the lesson differ:\n  voice : {display(parts)}\n  lesson: {seg['text']}")
                        plan.append([(text, speaker(cfg, role, n), pause["segment"] if j == len(parts) - 1 else pause["part"])
                                     for j, (role, text) in enumerate(parts)])
                    name = f"{lesson['id']}-story-ar.m4a"
                elif track["kind"] == "explanation" and not args.no_explanation:
                    ex = cfg["explanation"]
                    spec = {"voice": ex["voices"][n % len(ex["voices"])], "rate": ex["rate"], "pitch": ex["pitch"]}
                    def spoken(t):
                        for a, b in say.items():
                            t = re.sub(rf"\b{re.escape(a)}\b", b, t)
                        return t
                    plan = [[(spoken(seg["text"]), spec, pause["segment"])] for seg in segs]
                    name = f"{lesson['id']}-explanation-en.m4a"
                else:
                    continue
                # A published media file never changes under the same name: a remake gets a new name.
                published = subprocess.run(["git", "cat-file", "-e", f"origin/content:books/{book}/media/{name}"], cwd=ROOT, capture_output=True).returncode == 0
                if published:
                    stem, k = name[:-4], 2
                    while subprocess.run(["git", "cat-file", "-e", f"origin/content:books/{book}/media/{stem}-v{k}.m4a"], cwd=ROOT, capture_output=True).returncode == 0:
                        k += 1
                    name = f"{stem}-v{k}.m4a"
                starts, seconds = build(cache, book_dir / "media" / name, plan)
                track["audio"] = f"gh:books/{book}/media/{name}"
                for seg, t in zip(segs, starts):
                    seg["t"] = t
                print(f"lesson {n}: {track['kind']} {seconds} s -> {name}")
            changed = True
        if changed:
            path.write_text(json.dumps(chapter, ensure_ascii=False) + "\n", encoding="utf-8")
    for lid in wanted:
        print(f"warning: {lid} is not a lesson in {book_dir} (build it first)")


# ---- listening back ---------------------------------------------------------------

def plain(text, arabic):
    """Text reduced to what a recogniser can be compared on: no vowels, no punctuation, one spelling."""
    if arabic:
        text = re.sub("[\u064b-\u0652\u0670\u0640]", "", text)
        text = re.sub("[\u0623\u0625\u0622\u0671]", "\u0627", text).replace("\u0649", "\u064a").replace("\u0629", "\u0647")
        text = text.replace("\u0624", "\u0648").replace("\u0626", "\u064a")
    return re.sub(r"[\W_]+", " ", text.lower()).strip()


def cmd_check(args):
    import difflib
    import numpy as np
    from faster_whisper import WhisperModel
    book, say, lessons = parse(args.voice)
    bookcfg = json.loads((ROOT / "content" / "books" / f"{book}.json").read_text(encoding="utf-8"))
    book_dir = pathlib.Path(args.folder).resolve() / "books" / book
    wanted = {f"{bookcfg['short']}-q{l['n']:04d}" for l in lessons if not args.only or l["n"] in args.only}
    model = WhisperModel(args.model, device="cpu", compute_type="int8")
    doubtful = 0
    for path in sorted((book_dir / "chapters").glob("*.json")):
        for lesson in json.loads(path.read_text(encoding="utf-8"))["lessons"]:
            if lesson["id"] not in wanted:
                continue
            for track in lesson["tracks"]:
                if not track.get("audio") or any("t" not in s for s in track["segments"]):
                    continue
                name = track["audio"].rsplit("/", 1)[-1]
                src = book_dir / "media" / name
                if not src.is_file():       # not made here: take the published file
                    src = book_dir.parent.parent / "voice-cache" / name
                    src.parent.mkdir(parents=True, exist_ok=True)
                    src.write_bytes(subprocess.run(["git", "cat-file", "-p", f"origin/content:books/{book}/media/{name}"], cwd=ROOT, capture_output=True).stdout)
                raw = subprocess.run(["ffmpeg", "-v", "error", "-i", str(src), "-f", "f32le", "-ac", "1", "-ar", "16000", "pipe:1"], capture_output=True).stdout
                if not raw:
                    print(f"{lesson['id']} {track['kind']}: the audio file {name} could not be read")
                    doubtful += 1
                    continue
                arabic = track["lang"].startswith("ar")
                heard, _ = model.transcribe(np.frombuffer(raw, np.float32), language=track["lang"][:2], word_timestamps=True, beam_size=5)
                words = [(w.start * 1000, w.word) for seg in heard for w in seg.words]
                starts = [s["t"] for s in track["segments"]] + [10 ** 9]
                print(f"{lesson['id']} {track['kind']} ({name})")
                for i, seg in enumerate(track["segments"]):
                    got = " ".join(w for t, w in words if starts[i] - 250 <= t < starts[i + 1] - 250)
                    score = difflib.SequenceMatcher(None, plain(seg["text"], arabic), plain(got, arabic)).ratio()
                    if score < args.threshold:
                        doubtful += 1
                        print(f"  sentence {i + 1}: {score:.2f}  LISTEN TO THIS ONE\n      text : {seg['text']}\n      heard: {got.strip()}")
                    else:
                        print(f"  sentence {i + 1}: {score:.2f}")
    print(f"\n{doubtful} sentence(s) to look at." if doubtful else "\nEvery sentence was heard as written (a wrong vowel can still slip through: the owner's ear decides).")


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    sub = ap.add_subparsers(dest="cmd", required=True)
    p = sub.add_parser("fill"); p.add_argument("voice"); p.add_argument("batch"); p.set_defaults(fn=cmd_fill)
    p = sub.add_parser("make"); p.add_argument("voice"); p.add_argument("--folder", default=str(ROOT / ".content-work"))
    p.add_argument("--only", type=int, nargs="+"); p.add_argument("--no-explanation", action="store_true"); p.set_defaults(fn=cmd_make)
    p = sub.add_parser("check"); p.add_argument("voice"); p.add_argument("--folder", default=str(ROOT / ".content-work"))
    p.add_argument("--only", type=int, nargs="+"); p.add_argument("--model", default="small"); p.add_argument("--threshold", type=float, default=0.7)
    p.set_defaults(fn=cmd_check)
    args = ap.parse_args()
    args.fn(args)


if __name__ == "__main__":
    main()
