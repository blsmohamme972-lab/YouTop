---
name: youtop-lessons
description: Make or fix YouTop lessons from the owner's MCQ books (write stories, explanations, tags and image prompts for a range of questions; add the owner's pictures and spotlight spots; publish). Use whenever the owner asks for new lessons, "questions 41-50", pictures that arrived, or a correction to a published lesson.
---

# Making YouTop lessons, cheaply and well

Everything mechanical is done by `tools/content/batch.py`. You write only what needs a mind, in
one plain text file. Do not write per-batch Python scripts, do not retype questions, do not read
the published chapter file (it is large): ask the tool.

Read first, and only these: `docs/status.md` (what is done, what must not change) and the two
sections "The story is a story" and "Tags" of `docs/content-guide.md`. The rest of the guide is
reference for when something unusual comes up (a new book, recorded voice).

## 1. Get the questions (no prompt for the owner; never commit them)

Book facts are in `content/books/<book id>.json` (`masters` = Drive file ids of the question
files). Download the file that covers your range with the Drive connector's **download** tool
(not "read": it corrupts JSON); it saves to disk. Copy it to the scratchpad. Then:

    python3 tools/content/batch.py status shahzad-2015            # what exists, what lacks pictures/voice
    python3 tools/content/batch.py questions shahzad-2015 41 50 --master <file>   # read these
    python3 tools/content/batch.py quotes shahzad-2015            # quotations already used: never repeat
    python3 tools/content/batch.py new shahzad-2015 41 50 --master <file> -o <scratch>/batch.txt

## 2. Fill in `batch.txt` (the only creative step; work in batches of 10)

Per question (format at the top of `batch.py`):

- `title` / `titleAr`: the teaching point, short.
- `tags`: 1-2 topics, 1-2 `d:`, exactly one `q:`, exactly one `l:`, any `x:` — from
  `content/taxonomy.txt`. Tag what the question *tests*, not its chapter.
- `palette`, `scene`, optional `words`: the picture. One dominant focal point; few, large
  elements, each in its own area and each standing for one fact of the answer; a different
  setting and colour scheme every time; metaphors, no anatomy. `words` = at most three short
  labels or numbers painted in it.
- `story:` 8-11 short Arabic sentences, one per line. A real story in the manner of the book's
  author (people with names, an event, a turn), in eloquent, forceful classical Arabic. Every
  object of the scene appears and *does* something; numbers painted in the picture are said.
  **No medicine, no English, no "this means…"**. The last sentence leads into the quotation and
  ends with ":".
- `quote` + `source`: a **real** quotation of the book's author, in Arabic, from a named work,
  strong and well known, fitting the point, not in the `quotes` list. Never invent or adapt one;
  if unsure it is genuine, choose another.
- `explanation:` 4-8 English sentences, one per line, in the order of the story's objects: the
  teaching point, why the answer is right, the most tempting wrong option. Mainstream surgical
  knowledge only; leave out any number you are not sure of. If the book's key is debatable, keep
  it and say so in `note:`.

Quality bar: would the picture alone let the owner recall the answer a month later? If the
scene does not encode the *answer*, redo the scene before writing the story.

## 3. Build, publish, hand over

    python3 tools/content/batch.py build <scratch>/batch.txt --master <file>
    python3 tools/content/publish.py .content-work -m "Shahzad 2015: lessons 41-50"

`build` copies stems and options from the master file, keeps pictures/spots/recordings of
lessons that already exist, refuses to change a text that has a recorded voice, checks the
owner's rules and writes `image-prompts-<a>-<b>.json`. Fix every problem it prints; read its
warnings. Send the owner that prompts file (the file itself) — lessons work without pictures
until theirs arrive. Then update `docs/status.md` (short) and merge it to `main`.

## 4. When the owner's pictures arrive

They are in the Drive folder `imagesFolder` of the book's json, named by question number. List
it with the Drive connector (`parentId = '<folder id>'`) and download each (connector download,
or the public-link `curl` in the guide) into one scratch folder. Then:

    python3 tools/content/batch.py pictures shahzad-2015 <folder>

It writes the 1080-px WebP files, points the lessons at them and makes `<n>-grid.jpg` for each:
the picture with a grid in tenths. **Look at each grid picture once**: check it shows what the
story says (if not, fix the story in `batch.txt` and build again — unless the story is
recorded, then ask the owner for a new picture), and read the positions off the grid into a
spots file (format at the top of `batch.py`; one `x y r` per sentence, `-` when the sentence is
about nothing visible):

    python3 tools/content/batch.py spots <scratch>/spots.txt

Look at each `<n>-spots.jpg` it draws; move any circle that misses; publish as in step 3.

## Rules that are never broken

- Nothing is uploaded to Drive (every write prompts the owner). Publishing is `git push` only.
- Question files and PDFs never go into the repository; `batch.txt` (your own writing) does, under `work/`.
- Published ids never change. A published media file never changes under the same name
  (`pictures --replace b` publishes a corrected picture under a new name).
- Recorded text stays as recorded (`docs/status.md` lists which lessons).
