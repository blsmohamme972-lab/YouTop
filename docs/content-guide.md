# Making and publishing lessons

For whoever turns the owner's MCQ books into lessons. Read `CLAUDE.md` first.

The library is planned to pass **5,000 questions** in cardiac, thoracic and vascular surgery.
Everything below is shaped by that size: lessons are published in chapter files of about 100,
the app loads only a light index, and every lesson is tagged so the owner can study by topic.

## Where things live (Drive account gpmo592@gmail.com)

| Folder | Shared? | Holds |
|---|---|---|
| **YouTop books** (`1r5301GkHo5hZItw4gSwTUzrBD6HFcHUI`) | Anyone with the link, viewer. The app reads it. | `catalog-<n>.json`, one folder per book with `book.json`, `chapters/*.json`, pictures, audio |
| **YouTop private** (`1QQ4CyHwy6VjuYfYRobermLpT5BqLQVqb`) | Never | `youtop-signing.txt`, `START-HERE.txt`, `source books/` (the PDFs), `status-<n>.txt` |

Source PDFs are copyrighted books: keep them in **YouTop private/source books**, never in
"YouTop books" and never in the GitHub repository (it is public). If the owner drops a PDF into
"YouTop books", move it (`update_file` with `parentId`) and tell them.

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
- **Story (Arabic, 5–9 short sentences)**: a small scene or patient that makes the point
  memorable, in clear Modern Standard Arabic, with the key English term kept in Latin letters
  where a surgeon would say it that way. **Explanation (English, 4–8 sentences)**: the teaching
  point, why the right answer is right, and the most tempting wrong answer.
- **Question**: stem and options exactly as printed (fix only obvious typos). Options that depend
  on order ("all of the above") are detected and not shuffled.
- `id`: `<book short name>-q<4-digit number>`. `source`: book, page, question number.
- Each sentence is one `segment`. Keep sentences short; they are also the on-screen captions.

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
- The owner has pictures made (by ChatGPT) from a description you write for each lesson; give them
  the descriptions as one list, with the file name each picture must have (`<lesson id>.webp`).
  They upload the pictures into the book's folder.
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
