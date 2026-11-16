# Making and publishing lessons

For whoever turns the owner's MCQ books into lessons. Read `CLAUDE.md` first.

**The short path is the `youtop-lessons` skill** (`.claude/skills/youtop-lessons/SKILL.md`) with
`tools/content/batch.py`: you fill in one text file per batch (titles, tags, scene, story,
quotation, explanation) and the tool does the rest — questions copied from the master file, lessons
and image prompts built, pictures converted, spots applied and drawn, the rules below checked,
texts with a recorded voice protected. This guide is the reference behind it: the reasons, the
formats, and what to do for a new book or a recording. The older batches' own scripts
(`work/shahzad-2015/batch03/batch03.py` …) are history; do not copy that way of working.

The library is planned to pass **5,000 questions** in cardiac, thoracic and vascular surgery.
Everything below is shaped by that size: lessons are published in chapter files of about 100,
the app loads only a light index, and every lesson is tagged so the owner can study by topic.

## Where things live

| Place | Who can read it | Holds |
|---|---|---|
| Branch **`content`** of this GitHub repository | Everyone. The app reads it. | `catalog.json`, `books/<book id>/` (book file, chapter files, `media/` with pictures, figures and recorded narration), `work/` (work files of each batch) |
| Drive folder **YouTop books** (`1r5301GkHo5hZItw4gSwTUzrBD6HFcHUI`, account gpmo592@gmail.com) | Anyone with the link | The owner's original pictures: `<Book name>/images/<question number>.png`. Also the lessons as they were published before version 1.7; the app reads those only when GitHub cannot be reached. |
| Drive folder **YouTop private** (`1QQ4CyHwy6VjuYfYRobermLpT5BqLQVqb`) | Never shared | `youtop-signing.txt`, `source books/` (the PDFs), `books/<book id>/` (question files, style, quotation log and work files made before 1.7), old `status-<n>.txt` |

**Nothing is uploaded to Drive any more.** Every upload through the Drive connector shows the
owner an "Allow once" prompt (about thirty for a batch of ten lessons) and that cannot be switched
off. Reading and downloading from Drive, `curl` on public links and `git push` do not prompt. So
lessons are published by `git push` to the `content` branch, and Drive is only read. The owner
knows that this makes the lessons' questions publicly readable and accepted it; do not reopen it.

Source PDFs are copyrighted books: they stay in **YouTop private/source books**, never in
"YouTop books" and never in the GitHub repository (it is public). If the owner drops a PDF into
"YouTop books", tell them (moving it is a Drive write and will show them a prompt).

## The pipeline the owner agreed (follow it in this order)

Each book goes through these steps. Do not skip ahead: later steps are built from the files of
earlier ones, and those files are what the next session or the next agent starts from.

### Step 1 — the question file (the book's master copy)

Before anything else, turn the source PDF into a question file and save it. Every later step
reads the questions from this file, never from the PDF again.

- **Copy the source exactly**: stem, every option, the printed answer, and the book's own
  explanation if it has one. Do not reword, shorten, reorder or "improve" anything.
- **The only allowed change is an obvious error** (a clear typo, a broken character from the PDF,
  an answer key that is wrong beyond dispute). Make the fix, and record it in `corrections` with
  the original text, so nothing is changed silently. If it is merely debatable, leave it as printed.
- Keep **only the pictures needed to answer a question** and referred to in its stem. Save each as
  `figures/<question number>.png` in the book's private folder and point to it with `figure`.
- Number questions as the book does. If the book restarts numbering in each chapter, number them
  1…N through the whole book in book order and keep the printed number in `printed`.

Where it lives: for `shahzad-2015`, all 200 questions are already in **YouTop private / books /
shahzad-2015 /** in Drive; read them from there. For a new book, keep these files under
`work/<book id>/` on the `content` branch (put them in the `work/` folder you give
`publish.py`), since nothing is uploaded to Drive any more. The layout is the same:

    <book id>/
      book-<n>.txt                 the book's facts and style (see step 2); highest n is current
      mcqs-0001-0100-<n>.json      questions 1 to 100; highest n is current
      mcqs-0101-0200-<n>.json      …one file per 100 questions
      figures/17.png               pictures that questions need
      quotes-<n>.json              quotations already used (see step 4)

    {
      "book": "kaplan", "from": 1, "to": 100,
      "questions": [
        { "n": 17, "printed": "2.5", "chapter": "Aortic valve", "page": 88,
          "stem": "…exactly as printed…",
          "options": ["…", "…", "…", "…"],
          "answer": 2,                         // index into options, 0-based, after any correction
          "explanation": "…the book's own explanation, exactly, or empty…",
          "figure": "gh:books/kaplan/media/figure-q0017.png",   // only if the stem needs it
          "corrections": [ { "field": "answer", "was": "B", "now": "C", "why": "…" } ]
        } ]
    }

Reading the files that are in Drive: use the Drive connector's **download** tool (base64) — its
"read" tool reformats text and corrupts JSON.
When step 1 is finished for a book, check the count against the book and update `docs/status.md`.

### Step 2 — the book's style

Every book has one style chosen by the owner, used for all its pictures and stories (for example:
scenes inspired by the works of Fyodor Dostoevsky, with his quotations). Record it in
`book-<n>.txt` with the book's title, id, source PDF file id and number of questions. Ask the
owner for the style if they have not given one for this book. Never mix styles between books.

### Step 3 — the three things per question

Built from the **stem and the correct answer** (plus the book's explanation, or sound general
knowledge when there is none):

1. **Image prompt** — handed to the owner, who has the pictures made in ChatGPT.
2. **Arabic story** — saved in the lesson (step 5). It ends with a real quotation (step 4).
3. **English explanation** — saved in the lesson.

The image prompts are delivered as **one JSON file per book (or per batch), and nothing else in
the file**: no markdown, no code fences, no commentary. Exactly this structure:

    { "items": [ { "info": "1. …" }, { "info": "2. …" }, { "info": "3. …" } ] }

- Each `info` starts with the question's number, a dot and a space. Never skip or repeat a number.
  The number is the question's number in the book; a later batch continues the numbering.
- One item per question. No other fields.

Each `info` is a complete prompt written to this frame (the owner's wording):

1. Generate one highly memorable image from the medical source text.
2. Vertical 9:16 aspect ratio.
3. Use strong, creative everyday-life metaphors, similes and visual analogies to represent every
   important medical fact, in a scene from history that suits the book's style (for a Dostoevsky
   book: a scene inspired by his works).
4. Connect all symbols through physical interactions, cause-and-effect actions and one coherent
   story in the same scene.
5. Make every object visually meaningful, distinctive and easy to recall.
6. No literal anatomy, no unrelated objects, and no written text, except the few important words
   (keep them to one to three short words; image generators misspell longer text).
7. Where to send the picture: the link of the book's pictures folder in "YouTop books".
8. Name the picture with the question's number.

Describe the scene and what happens in it concretely; a vague prompt gives a forgettable picture.

The owner found the first twenty pictures not good enough (they were all brown, snowy, crowded
with small things). Since lesson 21 every prompt also asks for: one dominant focal point drawn
larger than life; only the few elements described, each big and in its own area with empty space
between; a bold colour scheme named for that picture; and a different setting each time (dawn at
a gate, a forge at dusk, a summer fair, a monastery at evening…). The frame is built into
`tools/content/batch.py` (`image_prompt`), with the book's style from `content/books/<book id>.json`.

### Step 4 — quotations

The Arabic story ends with a **real** quotation from the book's author, chosen to fit that
question's point, and shown on screen while the end of the story is read.

- Use only quotations you can attribute to a specific work, and name the work. Much of what
  circulates online under famous names is misattributed. Never invent or "adapt" a quotation.
- Vary them: do not repeat a quotation within a book. The quotations already used are in the
  published chapter files themselves (the last story segment of each lesson, with its `quote`);
  list them from there before choosing new ones.
- If no genuine quotation fits a question well, say so to the owner rather than forcing one.

### Step 5 — pictures arrive, camera script, publish

The owner drops the pictures, named by question number, into the book's `images` folder in
"YouTop books" in Drive (that is the owner's own upload; you only read it). Check
each picture against its prompt (a picture uploaded under the wrong number would appear on the
wrong question), then write the camera script and publish, as described below.

### Keeping quality when there are thousands

- Work in batches of 20 to 30 questions, not a whole book at once.
- For a new book, show the owner the first 10 finished questions and continue only after they agree.
- Run `tools/content/validate.py` on every batch, and compare each published question with the
  question file.
- Medical accuracy comes first: leave out any detail you are not sure of.

### Keeping books apart

One private folder and one public folder per book; a fixed book id that is part of every lesson
id (`kaplan-q0142`); the book's own pictures folder link inside its prompts; its own style file,
question files and quotation log. Nothing is shared between books except the tag system.

## One question becomes one lesson

    {
      "id": "kaplan-q0142",                      // never changes once published
      "title": "Aortic stenosis: when to operate",
      "titleAr": "تضيّق الصمام الأبهري: متى نجري الجراحة",
      "image": "gh:books/kaplan/media/kaplan-q0142.webp",
      "tags": ["cardiac/aortic-valve", "d:indications", "q:vignette", "l:2", "x:high-yield"],
      "tracks": [
        { "kind": "story", "lang": "ar", "segments": [ { "text": "…", "spot": { "x": 0.4, "y": 0.3, "r": 0.15 } } ] },
        { "kind": "explanation", "lang": "en", "segments": [ … ] }
      ],
      "quiz": [ { "q": "<stem as printed>", "options": ["…"], "answer": 2,
                  "explain": "…", "lang": "en", "source": "Kaplan p. 88, Q142" } ]
    }

How to write it:

- **Source of truth is the stem plus the correct answer.** They decide what the picture shows and
  what the lesson teaches. Use the book's explanation when there is one, otherwise sound,
  mainstream surgical knowledge. Do not invent numbers, trial names or guideline classes; if
  unsure, leave the detail out.
- **Answer key.** If the printed key is wrong beyond reasonable dispute, set `answer` to the right
  option, say so in `explain` ("The book's key says B; …"), and add the tag `x:key-corrected`.
  If it is merely debatable, keep the book's key and mention the debate in `explain`.
- **Story (Arabic, 8–11 short sentences)**: see "The story is a story" below. It is the owner's
  rule and the first set of stories was rejected for breaking it.
  **Explanation (English, 4–8 sentences)**: all of the science: the teaching point, why the right
  answer is right, and the most tempting wrong answer.
- **Question**: stem and options exactly as printed (fix only obvious typos). Options that depend
  on order ("all of the above") are detected and not shuffled.
- `id`: `<book short name>-q<4-digit number>`. `source`: book, page, question number.
- Each sentence is one `segment`. Keep sentences short; they are also the on-screen captions.

### The story is a story (the owner's rule)

The owner rejected the first stories because they were explanations dressed as scenes ("the
first monk sawed off the whole branch: this is a lobectomy"). What they asked for instead:

- **A real story**, in the manner of the book's author: people with names, a place, something
  that happens, a turn at the end. For a Dostoevsky book: poor students, clerks, monks, a
  feverish narrator, guilt, pity, a sudden confession.
- **Its job is to make the picture stay in memory.** Every important thing in the picture appears
  in the story and *does* something there: it has life. The numbers painted in the picture
  (8, 10, 12; 3:1; 1995) are said in the story, as things the characters count or read.
- **No medical explanation in the story.** No "this is the ...", no anatomical or disease names,
  no English terms. The link between the object and the fact is made by the English explanation
  that follows, sentence by sentence, with the camera on the same objects.
- **It ends with the real quotation** (step 4), in Arabic, as its own last segment with the
  `quote` field set to where it is from; the sentence before it leads into it ("…and said:").
  The app shows the quotation centred with its source underneath.

      { "text": "ثم طرقت جدار الكوخ برفق، كمن يطرق صدر إنسان، وقالت:", "spot": { … } },
      { "text": "«الإنسان سرّ. …»", "quote": "فيودور دوستويفسكي، رسالة إلى أخيه ميخائيل، 1839" }

- **Style the owner asked for (2026-11-15, after reading lessons 1-20):** eloquent classical
  Arabic with force in it — strong verbs, rhythm, parallel clauses, images that strike, speech
  that sounds like speech — not plain modern prose; and **stronger quotations**: the lines of the
  author that everyone remembers, not mild ones. Still exactly as real: a named work, never
  adapted. `work/shahzad-2015/batch03/batch03.py` on `content` is the worked set in this style.
- Write the story after you have **looked at the picture**, not only from its prompt: the story
  must match what the picture really shows.

Worked sets are in "YouTop private/books/shahzad-2015/work/" in Drive (read `README-work.txt`
there); work files of later batches are under `work/` on the `content` branch.

### A question that needs its figure

When the stem refers to a figure ("the forest plot shown below"), put the figure in the book's
`media/` folder as `figure-q<number>.png` and point to it from the question:
`"image": "gh:books/<book id>/media/figure-q0010.png"`. The app shows it above the options (tap
to enlarge).

## Tags (required on every lesson)

The system is defined in `content/taxonomy.txt`. It is small on purpose (about 55 tags: the owner
asked for no more than 60). Every lesson gets:

| Group | How many | Meaning | Examples |
|---|---|---|---|
| topic (no prefix) | 1, at most 2 | What it is about: specialty/area. | `cardiac/aortic-valve`, `vascular/aorta` |
| `d:` | 1, at most 2 | Kind of knowledge tested | `d:indications`, `d:anatomy`, `d:complications` |
| `q:` | exactly 1 | Style of question | `q:vignette`, `q:recall`, `q:next-step` |
| `l:` | exactly 1 | Level: 1 core, 2 intermediate, 3 advanced | `l:2` |
| `x:` | any | Flags | `x:high-yield`, `x:trap`, `x:guideline`, `x:key-corrected` |

The owner filters the feed by these tags and the app shows results per tag, so wrong or lazy
tags directly hurt their studying. Tag by what the *question tests*, not by the book chapter it
happens to sit in. A question about heparin reversal inside a valve chapter is `cardiac/cpb`.

Changing the system (only if the owner asks; they want it to stay small): edit `content/taxonomy.txt`, run `python3 tools/content/taxonomy.py`,
commit. **Never rename or delete an id that published lessons use.** The app has the tag system
built in; to show new tags before the next APK, copy `app/src/main/assets/taxonomy.json` to
`taxonomy.json` at the top of the folder you give `publish.py`.

## Pictures and spotlight

- Portrait 9:16, one per lesson, clear and uncluttered, showing the things the story and the
  explanation mention, each in its own area so the camera can point at them. No text in the image.
- The owner has pictures made (by ChatGPT) from the prompts you write and drops them into the
  book's `images` folder in Drive (about 3 MB each), named by question number, sometimes without
  ".png". For "Shahzad 2015" that folder is `1Rh8ghw3oe5_5yYzaTvH5iFcpJQTnrRxZ`. List it and
  download each picture by public link, with no prompt:
  `curl -sL "https://drive.google.com/embeddedfolderview?id=<folder id>"` shows names and ids,
  `curl -sL "https://drive.usercontent.google.com/download?id=<file id>&export=download&confirm=t"`
  gets a file. Shrink each to a 1080-pixel-wide WebP (about 300 KB; Pillow:
  `im.resize((1080, round(h * 1080 / w)), Image.LANCZOS).save(out, "WEBP", quality=82)`), save it
  as `media/<lesson id>.webp` and point the lesson's `image` at
  `gh:books/<book id>/media/<lesson id>.webp`. Leave the owner's originals where they are.
- A published media file never changes under the same name (the phone keeps what it downloaded
  under that name, and `publish.py` refuses). A corrected picture gets a new name, for example
  `shahzad-q0007-b.webp`, and the lesson points at the new name.
- The app shows the picture whole, never cropped or zoomed (the owner asked for that), with a
  spotlight on the `spot` of the sentence being spoken. Any proportion works; 9:16 fills most.
- Then **look at every picture** (download it and view it) and set each segment's `spot`: centre
  `x`,`y` as fractions of width/height from the top-left, radius `r` as a fraction of the width.
  Verify by drawing the circles on the picture, as `tools/starter/art.mjs --check` does for the
  starter lessons. A spot that points at the wrong thing is worse than no spot; leave `spot` out
  when a sentence is not about anything visible.
- **Marks** (since app 2.2): a lesson with a picture also carries `"marks": [{"spot": {…}, "what":
  "…", "means": "…"}]`, the numbered circles of the "All circles" table (number = place in the
  list, from 1; English only), and its question carries `"marks": [1]` (circles shown while it is
  asked; never the answer) and `"answerMarks": [3]` (circles shown once answered). Written with
  `batch.py marks` (format at the top of `batch.py`; the skill, step 5). All three are optional:
  a lesson without them simply shows no circles in its question.
- A lesson may be published before its picture exists (no `image`, no `spot`); add them later.

## Publishing

Lessons are files on the `content` branch of this repository, served to the app from
`https://raw.githubusercontent.com/blsmohamme972-lab/YouTop/content/<path>`:

    catalog.json                                  {"books":[{"id":"…","file":"gh:books/<id>/book-<hash>.json"}]}
    books/<id>/book-<hash>.json                   chapters point at "gh:books/<id>/chapters/<name>-<hash>.json"
    books/<id>/chapters/<name>-<hash>.json        {"lessons":[ … ]}, about 100 lessons
    books/<id>/media/…                            <lesson id>.webp, figure-q<n>.png, <lesson id>-story-ar.m4a
    work/…                                        work files of each batch

`<hash>` is the first 8 hex digits of the file's SHA-256, so a changed book or chapter has a new
address; that is how the app notices an update. `catalog.json` keeps its name and the app fetches
it on every sync. You never write these names yourself: `tools/content/publish.py` does.

1. Get what is published now as a plain folder, outside the repository's tracked files:

       python3 tools/content/publish.py --export /some/scratch/folder

   It writes `books/<id>/book.json` (chapters as `"file": "chapters/<name>.json"`) and
   `books/<id>/chapters/<name>.json`. Media are not copied; published media stay where they are.
2. Change the chapter files there (add lessons, fix lessons), put **new** pictures and audio in
   `books/<id>/media/`, and the batch's work files (stories, spots, scripts, image prompts) in
   `work/<book id>/<batch>/`. A new book is a new `books/<id>/` folder with its `book.json`:

       { "id": "kaplan", "title": "…", "cover": "gh:books/kaplan/media/kaplan-q0001.webp",
         "chapters": [ { "id": "valves-1", "title": "Valves 1", "file": "chapters/valves-1.json" }, … ] }

3. Publish:

       python3 tools/content/publish.py /some/scratch/folder -m "Shahzad 2015: lessons 21-30"

   It runs `validate.py` (ids, tags, answer indexes, spots, audio timing), refuses if a published
   lesson would disappear or a media file is missing or would change under the same name, writes
   the hashed files, removes the versions they replace, rewrites `catalog.json`, commits, pushes
   to `content`, and then reads everything back from the public address and compares. The public
   address is cached for about five minutes, so that last check can take that long; the owner's
   phone sees the change at its next sync after that (it syncs on opening and every 15 minutes).
4. Update `docs/status.md` on `main`: which book, which questions are done, what is waiting
   (pictures, audio), anything odd. The next agent starts from it.

The starter book is not in the catalog; it is bundled in the APK. Do not edit the `content`
branch by hand, and never upload lessons to Drive.

Rules that protect the owner's progress on the phone (stored as `book/chapter/lesson`):
never change a published book id, chapter id or lesson id, and never move a lesson to another
chapter. To fix a lesson, publish a new version of its chapter file with the same ids.

## Audio (later)

Not used yet; the phone's own voice reads the text. When a voice model is chosen: synthesise each
segment separately, join them, put the file in `media/`, set the track's `audio` and every segment's `t`
(start in milliseconds). The validator insists on `t` when `audio` is present.
