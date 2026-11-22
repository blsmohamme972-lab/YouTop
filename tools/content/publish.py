#!/usr/bin/env python3
"""Publishes lessons to the `content` branch of this repository, which the app reads.

    python3 tools/content/publish.py <folder> [-m "what changed"] [--no-push] [--no-check] [--complete-only]
                                              [--redone <book>/<chapter>/<lesson> ...]
    python3 tools/content/publish.py --export <folder>

<folder> is a plain folder, anywhere outside the repository's tracked files:

    books/<book id>/book.json          chapters point at their files as "file": "chapters/<name>.json"
    books/<book id>/chapters/<name>.json
    books/<book id>/media/*            only the NEW pictures and audio; published ones stay as they are
    taxonomy.json                      optional: a newer tag system than the one inside the APK
    work/**                            optional: batch work files (stories, spots, scripts), kept as they are

Lessons refer to media as "gh:books/<book id>/media/<file name>".

What it does: runs validate.py, writes the files into a git worktree of `content` (.content/ in
the repository), giving book and chapter files a name that ends in the first 8 hex digits of
their SHA-256 (a changed file therefore has a new address, which is how the app notices it),
removes the versions they replace, rewrites catalog.json, commits, pushes, and finally reads
everything back from the public address.

--export writes what is published now as such a plain folder (without media), to start the next
batch from. Books that are not in <folder> stay published untouched.
"""
import argparse, hashlib, json, pathlib, re, shutil, subprocess, sys, time

ROOT = pathlib.Path(__file__).resolve().parents[2]
WORKTREE = ROOT / ".content"
BRANCH = "content"
HASHED = re.compile(r"(.+)-[0-9a-f]{8}\.json")
GH_REF = re.compile(r'"gh:([^"]+)"')


def base_url():
    for line in (ROOT / "gradle.properties").read_text().splitlines():
        if line.startswith("youtop.contentUrl="):
            return line.split("=", 1)[1].strip().rstrip("/") + "/"
    sys.exit("youtop.contentUrl is missing from gradle.properties")


def git(*args, cwd=ROOT, check=True, quiet=False):
    r = subprocess.run(["git", *args], cwd=cwd, text=True, capture_output=True)
    if check and r.returncode != 0:
        sys.exit(f"git {' '.join(args)} failed:\n{r.stdout}{r.stderr}")
    if not quiet and r.returncode != 0:
        print(r.stderr.strip())
    return r


def retry(what, attempt, tries=4):
    for i in range(tries):
        r = attempt()
        if r.returncode == 0:
            return r
        if i < tries - 1:
            time.sleep(2 ** (i + 1))
    sys.exit(f"{what} failed:\n{r.stdout}{r.stderr}")


def open_worktree():
    """Checks out the newest `content` into .content/ (creating the branch the very first time)."""
    remote = git("ls-remote", "--exit-code", "--heads", "origin", BRANCH, check=False, quiet=True).returncode == 0
    if remote:
        retry("git fetch", lambda: git("fetch", "--depth=1", "origin",
                                       f"+refs/heads/{BRANCH}:refs/remotes/origin/{BRANCH}", check=False, quiet=True))
    if not (WORKTREE / ".git").exists():
        git("worktree", "prune")
        if remote:
            git("worktree", "add", "-B", BRANCH, str(WORKTREE), f"origin/{BRANCH}")
        elif git("rev-parse", "--verify", "-q", f"refs/heads/{BRANCH}", check=False, quiet=True).returncode == 0:
            git("worktree", "add", str(WORKTREE), BRANCH)
        else:
            git("worktree", "add", "--orphan", "-b", BRANCH, str(WORKTREE))
    elif remote:
        if git("status", "--porcelain", cwd=WORKTREE).stdout.strip():
            sys.exit(f"{WORKTREE} has changes that were never committed. Look at them, then delete the folder and run again.")
        git("merge", "--ff-only", f"origin/{BRANCH}", cwd=WORKTREE)
    return WORKTREE


def digest(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()[:8]


def dump(obj) -> bytes:
    return (json.dumps(obj, ensure_ascii=False, indent=1) + "\n").encode("utf-8")


def load(path):
    return json.loads(path.read_text(encoding="utf-8"))


def write_hashed(folder, stem, data: bytes) -> str:
    """Writes <stem>-<hash>.json into folder, removes the versions it replaces, returns its name."""
    folder.mkdir(parents=True, exist_ok=True)
    name = f"{stem}-{digest(data)}.json"
    for old in folder.glob(f"{stem}-*.json"):
        m = HASHED.fullmatch(old.name)
        if m and m.group(1) == stem and old.name != name:
            old.unlink()
    (folder / name).write_bytes(data)
    return name


def published_ids(wt, book_id):
    """(chapter id, lesson id) of every lesson of the book as it is published now."""
    return set(published_lessons(wt, book_id))


def published_lessons(wt, book_id):
    """{(chapter id, lesson id): lesson} for the book as it is published now."""
    catalog = wt / "catalog.json"
    if not catalog.exists():
        return {}
    ids = {}
    for ref in load(catalog).get("books", []):
        if ref["id"] != book_id:
            continue
        book = load(wt / ref["file"].removeprefix("gh:"))
        for ch in book.get("chapters", []):
            lessons = load(wt / ch["file"].removeprefix("gh:"))["lessons"] if ch.get("file") else ch.get("lessons", [])
            ids.update({(ch["id"], lesson["id"]): lesson for lesson in lessons})
    return ids


def set_revisions(book_id, chapter_id, obj, published, redone, problems):
    """Gives each lesson its "rev" (see Lesson.rev in the app). A lesson never goes back to a lower
    rev than the published one; a lesson named in --redone gets one more, which tells the phone that
    holds it back for a redo to show it again, as a new lesson. True if anything was changed."""
    changed = False
    for lesson in obj.get("lessons", []):
        key = f"{book_id}/{chapter_id}/{lesson['id']}"
        old = published.get((chapter_id, lesson["id"]))
        prev = old.get("rev", 0) if old else 0
        rev = max(prev, lesson.get("rev", 0))
        if key in redone:
            redone.discard(key)
            strip = lambda d: {k: v for k, v in d.items() if k != "rev"}
            if old is None:
                problems.append(f"--redone {key}: this lesson is not published yet")
                continue
            if strip(old) == strip(lesson):
                problems.append(f"--redone {key}: the lesson is exactly as published; make the change first")
                continue
            rev = prev + 1
        if rev != lesson.get("rev", 0):
            lesson["rev"] = rev
            changed = True
    return changed


def publish_book(src, wt, problems, complete_only=False, redone=frozenset()):
    book = load(src / "book.json")
    book_id = book.get("id", "")
    if book_id != src.name:
        problems.append(f"{src}: the folder name must be the book id ('{book_id}')")
        return None
    dest = wt / "books" / book_id
    published = published_lessons(wt, book_id)
    before = set(published)

    # --complete-only: a lesson without its picture is held back (not published, or taken out of the
    # published book until it is complete); the phone keeps its progress under its id meanwhile.
    chapters, held, rewritten = {}, set(), set()
    for ch in book.get("chapters", []):
        plain = ch.get("file")
        if plain and not plain.startswith("gh:") and not HASHED.fullmatch(pathlib.Path(plain).name) and (src / plain).is_file():
            obj = json.loads((src / plain).read_bytes())
            if set_revisions(book_id, ch["id"], obj, published, redone, problems):
                rewritten.add(ch["id"])
            if complete_only:
                held |= {(ch["id"], lesson["id"]) for lesson in obj["lessons"] if not lesson.get("image")}
                obj["lessons"] = [lesson for lesson in obj["lessons"] if lesson.get("image")]
            chapters[ch["id"]] = obj
    if complete_only:
        book["chapters"] = [ch for ch in book.get("chapters", []) if ch["id"] not in chapters or chapters[ch["id"]]["lessons"]]
        wanted = {ref.rsplit("/", 1)[-1] for obj in [*chapters.values(), book] for ref in GH_REF.findall(json.dumps(obj, ensure_ascii=False))}

    # Media: new files only. A published file never changes under the same name, because the
    # phone keeps what it downloaded under that name.
    for f in sorted((src / "media").glob("*")) if (src / "media").is_dir() else []:
        if complete_only and f.name not in wanted:
            continue
        target = dest / "media" / f.name
        if not re.fullmatch(r"[\w.-]+", f.name):
            problems.append(f"{f}: use only letters, digits, dot, dash and underscore in media names")
        elif target.exists() and target.read_bytes() != f.read_bytes():
            problems.append(f"{f.name}: a different file is already published under this name. "
                            f"Give the new one a new name (for example {f.stem}-b{f.suffix}) and point the lesson at it.")
        else:
            target.parent.mkdir(parents=True, exist_ok=True)
            shutil.copyfile(f, target)

    now, texts = set(), []
    for ch in book.get("chapters", []):
        plain = ch.get("file")
        if plain is None:
            now |= {(ch["id"], lesson["id"]) for lesson in ch.get("lessons", [])}
            continue
        path = src / plain
        if plain.startswith("gh:") or HASHED.fullmatch(path.name) or not path.is_file():
            problems.append(f"{book_id} chapter '{ch['id']}': \"file\" must be a plain file in the folder, "
                            f"like chapters/q0001-0100.json (found '{plain}')")
            continue
        data = dump(chapters[ch["id"]]) if complete_only or ch["id"] in rewritten else path.read_bytes()
        texts.append(data.decode("utf-8"))
        now |= {(ch["id"], lesson["id"]) for lesson in json.loads(data)["lessons"]}
        ch["file"] = f"gh:books/{book_id}/chapters/" + write_hashed(dest / "chapters", path.stem, data)

    gone = sorted(before - now - held)
    if held:
        print(f"{book_id}: held back until they have their picture: {', '.join(sorted(l for _, l in held))}")
    if gone:
        problems.append(f"{book_id}: published lessons would disappear or move to another chapter, and the owner's "
                        f"progress with them: {', '.join('/'.join(g) for g in gone[:10])}{' …' if len(gone) > 10 else ''}")
    data = dump(book)
    texts.append(data.decode("utf-8"))
    for text in texts:
        for ref in GH_REF.findall(text):
            if not (wt / ref).is_file():
                problems.append(f"{book_id}: gh:{ref} is referred to but is neither published nor in the folder")
        if '"drive:' in text:
            print(f"note: {book_id} still refers to files in Google Drive (drive:...)")
    name = write_hashed(dest, "book", data)
    return {"id": book_id, "file": f"gh:books/{book_id}/{name}"}


def unused_media(wt):
    text = "".join(p.read_text(encoding="utf-8") for p in wt.glob("books/*/**/*.json"))
    used = set(GH_REF.findall(text))
    return [str(p.relative_to(wt)) for p in sorted(wt.glob("books/*/media/*")) if str(p.relative_to(wt).as_posix()) not in used]


def fetch(url):
    r = subprocess.run(["curl", "-fsSL", "--retry", "2", url], capture_output=True)
    return r.stdout if r.returncode == 0 else None


def check_public(wt):
    """Reads back from the public address everything the catalog leads to. The address is cached for about five minutes."""
    base = base_url()
    want = (wt / "catalog.json").read_bytes()
    print(f"Checking {base}catalog.json (new content can take about five minutes to appear) ...")
    deadline = time.time() + 8 * 60
    while fetch(base + "catalog.json") != want:
        if time.time() > deadline:
            sys.exit("The public address still serves the old catalog after 8 minutes. Check again later with curl.")
        time.sleep(20)
    paths = set(GH_REF.findall(want.decode("utf-8")))
    for path in sorted(paths):
        paths |= set(GH_REF.findall((wt / path).read_text(encoding="utf-8")))
    for path in sorted(paths):  # chapter files, then their media
        if path.endswith(".json"):
            paths |= set(GH_REF.findall((wt / path).read_text(encoding="utf-8")))
    bad = [p for p in sorted(paths) if fetch(base + p) != (wt / p).read_bytes()]
    if bad:
        sys.exit("Not served correctly from the public address:\n  " + "\n  ".join(bad))
    print(f"Public address OK: catalog and {len(paths)} files read back and identical.")


def export(folder):
    wt = open_worktree()
    catalog = wt / "catalog.json"
    if not catalog.exists():
        sys.exit("Nothing is published yet.")
    for ref in load(catalog)["books"]:
        book = load(wt / ref["file"].removeprefix("gh:"))
        out = folder / "books" / ref["id"]
        (out / "chapters").mkdir(parents=True, exist_ok=True)
        for ch in book.get("chapters", []):
            if ch.get("file"):
                src = wt / ch["file"].removeprefix("gh:")
                plain = HASHED.fullmatch(src.name).group(1) + ".json"
                shutil.copyfile(src, out / "chapters" / plain)
                ch["file"] = "chapters/" + plain
        (out / "book.json").write_bytes(dump(book))
        print(f"wrote {out}")


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("folder", type=pathlib.Path)
    ap.add_argument("-m", "--message", default="Publish content")
    ap.add_argument("--export", action="store_true", help="write the published content into <folder> instead of publishing")
    ap.add_argument("--no-push", action="store_true", help="commit in .content/ but do not push")
    ap.add_argument("--no-check", action="store_true", help="do not read the files back from the public address")
    ap.add_argument("--complete-only", action="store_true",
                    help="publish only lessons that have their picture; the others are held back (or taken out) until they do")
    ap.add_argument("--redone", nargs="+", default=[], metavar="BOOK/CHAPTER/LESSON",
                    help="lessons made again after the owner's Redo: their version goes up, so the phone, which holds "
                         "them back since the Redo, shows the new version in place of the old one (as a new lesson)")
    args = ap.parse_args()
    folder = args.folder.resolve()
    if args.export:
        return export(folder)

    books = sorted(p.parent for p in folder.glob("books/*/book.json"))
    if not books and not (folder / "work").is_dir() and not (folder / "taxonomy.json").is_file():
        sys.exit(f"Nothing to publish in {folder}: expected books/<book id>/book.json, taxonomy.json or work/")
    if books:
        check = subprocess.run([sys.executable, str(ROOT / "tools/content/validate.py"), *map(str, books)])
        if check.returncode != 0:
            sys.exit("Not published: fix what validate.py reports first.")

    wt = open_worktree()
    catalog_path = wt / "catalog.json"
    catalog = load(catalog_path) if catalog_path.exists() else {"books": []}
    problems = []
    redone = set(args.redone)
    for src in books:
        entry = publish_book(src, wt, problems, args.complete_only, redone)
        if entry:
            catalog["books"] = [entry if b["id"] == entry["id"] else b for b in catalog["books"]]
            if entry not in catalog["books"]:
                catalog["books"].append(entry)
    if (folder / "taxonomy.json").is_file():
        data = (folder / "taxonomy.json").read_bytes()
        json.loads(data)
        catalog["taxonomy"] = "gh:" + write_hashed(wt, "taxonomy", data)
    if (folder / "work").is_dir():
        shutil.copytree(folder / "work", wt / "work", dirs_exist_ok=True)
    for key in sorted(redone):
        problems.append(f"--redone {key}: no such lesson in the folder (book/chapter/lesson, as in the feedback issue)")
    if problems:
        git("reset", "--hard", cwd=wt, check=False, quiet=True)
        git("clean", "-fd", cwd=wt, check=False, quiet=True)
        sys.exit("Not published:\n  " + "\n  ".join(problems))
    catalog_path.write_bytes(dump(catalog))

    for path in unused_media(wt):
        print(f"note: no lesson uses {path} (left in place)")
    git("add", "-A", cwd=wt)
    if not git("status", "--porcelain", cwd=wt).stdout.strip():
        print("Nothing changed: what is in the folder is already published.")
    else:
        git("-c", "user.name=YouTop content", "-c", "user.email=noreply@anthropic.com",
            "commit", "-q", "-m", args.message, cwd=wt)
        print(git("show", "--stat", "--format=%h %s", "HEAD", cwd=wt).stdout.strip())
    if args.no_push:
        return print(f"Committed in {wt}, not pushed.")
    retry("git push", lambda: git("push", "-u", "origin", BRANCH, cwd=wt, check=False, quiet=True))
    print(f"Pushed to branch '{BRANCH}'.")
    if not args.no_check:
        check_public(wt)


if __name__ == "__main__":
    main()
