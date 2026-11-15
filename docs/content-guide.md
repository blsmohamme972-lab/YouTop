# Making and publishing lessons

For whoever turns the owner's MCQ books into lessons. Read `CLAUDE.md` first.

The library is planned to pass **5,000 questions** in cardiac, thoracic and vascular surgery.
Everything below is shaped by that size: lessons are published in chapter files of about 100,
the app loads only a light index, and every lesson is tagged so the owner can study by topic.

## Where things live (Drive account gpmo592@gmail.com)

| Folder | Shared? | Holds |
|---|---|---|
| **YouTop books** (`1r5301GkHo5hZItw4gSwTUzrBD6HFcHUI`) | Anyone with the link, viewer. The app reads it. | `catalog-<n>.json`, one folder per book with `book.json`, `chapters/*.json`, pictures, audio |
| **YouTop private** (`1QQ4CyHwy6VjuYfYRobermLpT5BqLQVqb`) | Never | `youtop-signing.txt`, `START-HERE.txt`, `source books/` (the PDFs), `books/<book id>/` (question files, style, quotation log), `status-<n>.txt` |

Source PDFs are copyrighted books: keep them in **YouTop private/source books**, never in
"YouTop books" and never in the GitHub repository (it is public). If the owner drops a PDF into
"YouTop books", move it (`update_file` with `parentId`) and tell them.

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

Where it lives: **YouTop private / books / `<book id>` /** (private: these are the book's
questions word for word). Create `books` and the book's folder if they do not exist.

    YouTop private/books/<book id>/
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
          "figure": "drive:<file id>",         // only if the stem needs it
          "corrections": [ { "field": "answer", "was": "B", "now": "C", "why": "…" } ]
        } ]
    }

Reading and writing these files: upload with `disableConversionToGoogleType: true`. To read one
back exactly, use the Drive connector's **download** tool (base64) — its "read" tool reformats
text and corrupts JSON. After saving, download the file again and compare it with what you made.
When step 1 is finished for a book, check the count against the book and write the next
`status-<n>.txt`.

### Step 2 — the book's style

Every book has one style chosen by the owner, used for all its pictures and stories (for example:
scenes inspired by the works of Fyodor Dostoevsky, with his quotations). Record it in
`book-<n>.txt` with the book's title, id, source PDF file id and number of questions. Ask the
owner for the style if they have not given one for this book. Never mix styles between books.

### Step 3 — the three things per question

Built from the **stem and the correct answer** (plus the book's explanation, or sound general
knowledge when there is none):

1. **Image prompt** — handed to the owner, who has the pictures made in ChatGPT. Not saved in Drive.
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

### Step 4 — quotations

The Arabic story ends with a **real** quotation from the book's author, chosen to fit that
question's point, and shown on screen while the end of the story is read.

- Use only quotations you can attribute to a specific work, and name the work. Much of what
  circulates online under famous names is misattributed. Never invent or "adapt" a quotation.
- Vary them: do not repeat a quotation within a chapter. Keep `quotes-<n>.json` (quotation, work,
  the questions that use it) so later sessions know what has been used.
- If no genuine quotation fits a question well, say so to the owner rather than forcing one.

### Step 5 — pictures arrive, camera script, publish

The owner uploads the pictures, named by question number, into the book's pictures folder. Check
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
      "image": "drive:<file id>",
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

- Write the story after you have **looked at the picture**, not only from its prompt: the story
  must match what the picture really shows.

A worked set is in "YouTop private/books/shahzad-2015/work/stories-v2.py".

### A question that needs its figure

When the stem refers to a figure ("the forest plot shown below"), copy the figure from the
private `figures/` folder into the book's public folder and point to it from the question:
`"image": "drive:<public file id>"`. The app shows it above the options (tap to enlarge).

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
built in; to show new tags before the next APK, upload `app/src/main/assets/taxonomy.json` to
"YouTop books" and put its file id in the next catalog as `"taxonomy": "<id>"`.

## Pictures and spotlight

- Portrait 9:16, one per lesson, clear and uncluttered, showing the things the story and the
  explanation mention, each in its own area so the camera can point at them. No text in the image.
- The owner has pictures made (by ChatGPT) from the prompts you write and uploads them as
  `<question number>.png` into the book's `images` folder (about 3 MB each). Shrink each to a
  1080-pixel-wide WebP (about 300 KB), upload it to the book folder as `<lesson id>.webp`, and
  point the lesson's `image` at that copy. Leave the owner's originals where they are.
- The app shows the picture whole, never cropped or zoomed (the owner asked for that), with a
  spotlight on the `spot` of the sentence being spoken. Any proportion works; 9:16 fills most.
- Then **look at every picture** (download it and view it) and set each segment's `spot`: centre
  `x`,`y` as fractions of width/height from the top-left, radius `r` as a fraction of the width.
  Verify by drawing the circles on the picture, as `tools/starter/art.mjs --check` does for the
  starter lessons. A spot that points at the wrong thing is worse than no spot; leave `spot` out
  when a sentence is not about anything visible.
- A lesson may be published before its picture exists (no `image`, no `spot`); add them later.

## Publishing

The Drive connector creates files; it cannot change their contents. Files are never edited,
only replaced.

1. Write lessons into chapter files of about 100: `{"lessons": [ … ]}`. Work in a scratch folder
   outside the repository.
2. `python3 tools/content/validate.py <folder>` until it prints OK. It checks ids, tags, answer
   indexes, spots and audio timing.
3. Upload each new or changed chapter file to `<book folder>/chapters/` (always
   `disableConversionToGoogleType: true`, or JSON becomes a Google Doc). Note the file ids.
4. Upload a new `book.json` for the book:

       { "id": "kaplan", "title": "…", "cover": "drive:<id>",
         "chapters": [ { "id": "valves-1", "title": "Valves 1", "file": "<chapter file id>" }, … ] }

   Unchanged chapters keep their old file id; only changed ones get the new id.
5. Upload `catalog-<n+1>.json` to "YouTop books" listing **every** book:
   `{ "books": [ { "id": "kaplan", "file": "<book.json id>" }, … ] }` (plus `"taxonomy"` if used).
6. Check as the app does (these are public links, plain `curl` works):
   - `https://drive.google.com/embeddedfolderview?id=1r5301GkHo5hZItw4gSwTUzrBD6HFcHUI` lists the new catalog;
   - `https://drive.usercontent.google.com/download?id=<id>&export=download&confirm=t` returns
     the JSON for the catalog, the book and one chapter, and a picture for one image.
7. Trash the files that were replaced (old catalog, old `book.json`, old chapter versions).
8. Upload `status-<n+1>.txt` to "YouTop private": which book, which questions are done, what is
   waiting (pictures, audio), anything odd. The next agent starts from the highest-numbered one.

Rules that protect the owner's progress on the phone (stored as `book/chapter/lesson`):
never change a published book id, chapter id or lesson id, and never move a lesson to another
chapter. To fix a lesson, publish a new version of its chapter file with the same ids.

## Audio (later)

Not used yet; the phone's own voice reads the text. When a voice model is chosen: synthesise each
segment separately, join them, upload the file, set the track's `audio` and every segment's `t`
(start in milliseconds). The validator insists on `t` when `audio` is present.
