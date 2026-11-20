---
name: youtop-lessons
description: Make or fix YouTop lessons from the owner's MCQ books (write stories, explanations, tags and image prompts for a range of questions; add the owner's pictures and spotlight spots; publish). Use whenever the owner asks for new lessons, "questions 41-50", pictures that arrived, or a correction to a published lesson.
---

# Making YouTop lessons, cheaply and well

Everything mechanical is done by `tools/content/batch.py` and `tools/content/voice.py`. You write
only what needs a mind, in plain text files. Do not write per-batch Python scripts, do not retype
questions, do not read the published chapter file (it is large): ask the tool.

Read first, and only these: `docs/status.md` (what is done, what must not change) and the two
sections "The story is a story" and "Tags" of `docs/content-guide.md`. The rest of the guide is
reference for when something unusual comes up (a new book).

## The picture prompt is the most important step (the owner's rules, 2026-11-19)

The image prompt is the most important step of all the work; the story is the second. Every prompt
is written to these five rules. They come from memory research, and they apply to every book from
MedCosmos Cardiac (lessons 1-20 are the first set written to them) onwards.

1. **One interacting scene.** The elements act on each other inside one meaningful event (one
   pushes, squeezes, cuts or feeds another), never stand side by side. An interacting image binds
   the items into one memory unit, so recalling one part brings back the others. This is the most
   replicated finding about mnemonics (Bower 1970; Wollen et al. 1972).
2. **Abstract terms become concrete sound-alike objects (VERY IMPORTANT).** Every abstract term the
   answer depends on (a drug, a disease, a structure, a procedure, an eponym, a trial) gets a
   concrete *keyword*: an everyday thing whose English name sounds like the term (atropine →
   a gold TROPHY; secundum → a clock's SECOND hand; asystole → "a SISTER", a nun lying still),
   drawn big and doing something in the scene. Concrete things are far easier to picture and keep
   than abstract words (Paivio, dual coding; Atkinson & Raugh 1975, keyword method). Sound-alikes are
   English words. When no sound-alike exists, take the thing the term literally names (a duct, a
   bundle of branches, a crown for "corona"). Write each one in the batch file's `keywords:` line
   (`term → object (why it sounds alike)`): the prompt repeats them, and they feed the marks.
3. **Signal the answer, strip everything else.** The element that carries the answer is the
   dominant one: centre, largest, brightest light, the thing a gesture or a path points to. Every
   element that does not encode a fact is removed: no decorative people, crowds, props or
   background details. Decorative detail competes for attention and lowers recall of the main point
   (Harp & Mayer 1998; meta-analyses of signalling). A wrong option may have an element only if the
   picture shows *why* it is wrong.
4. **A memory palace for ordered information.** Steps, sequences, pathways, types and
   classifications go on fixed places along one route that the eye follows (landings of a
   stairway numbered 1-2-3, floors of a building, left to right along a street, top to bottom of a
   wall), and the order of the route is the order of the facts. Fixed locations give a ready
   retrieval path that keeps the sequence (Twomey & Kroneisen 2021, meta-analysis).
5. **Recurring symbols, the same in every book.** A concept that comes back (the heart's chambers,
   the aorta, CPR, stroke, a drug) always has the same symbol, from `content/symbols.json` (readable
   list: `content/symbols.md`). A bad recurring symbol spoils all later work, so a new one is added
   only with great care: write it into the json with its terms, the anchor words a prompt must
   contain, a reason, and a score 0-2 for link (sound-alike), concrete, unmistakable, drawable and
   sticky; then run `python3 tools/content/symbols.py audit`. The owner asked this audit to decide
   for them (2026-11-19): it approves a symbol only with every score at least 1, a total of at least
   8, and anchors no other concept uses; it keeps rejected ones so nobody proposes them again. Score
   honestly: the audit is only as good as the scores. The model the owner liked (2026-11-20): endocarditis =
   playing cards sprouting broccoli, a sound-alike in the name (endo-CARD-itis) joined to the meaning
   (vegetations), placed on the valve it affects. **Required since 2026-11-20 (the owner):** every new
   symbol joins at least two layers in one object, three when the place matters: `"layers": {"sound": ...,
   "meaning": ..., "place": ...}` in symbols.json. The audit rejects a new symbol with fewer than two.
   Older symbols are marked `legacy`; when one of them is next used, give it a second layer if it can take one.
   The same goes for one-off keywords in a scene: join the sound-alike to the meaning wherever possible. `batch.py prompts` and `build` refuse a batch
   whose `keywords:` draw a listed concept any other way (`symbols.py check batch.txt` shows why).
   Never invent a second symbol for a concept that already has one.

Before handing over a prompt, read it against the five rules: does every element interact? Has every
abstract term of the answer a concrete keyword? Is the answer the brightest, largest thing? Is
anything decorative left? Is ordered information on a route? Are recurring concepts drawn with
their audited symbol (`symbols.py check`)?

## The standard order (the owner's decision, 2026-11-16; lessons 41-45 are the worked set)

The owner makes the pictures by hand, and that is the slow part. So **the image prompts go to the
owner first**, and the rest is written while they make the pictures:

1. Questions (step 1 below).
2. For each question: the **table of elements** (step 6, "Table first"), then `title`, `tags`,
   `palette`, `words`, `scene` in `batch.txt`. No story yet.
3. `python3 tools/content/batch.py prompts batch.txt` and **hand the owner the prompts at once**:
   the json file, and each prompt as text in the chat in its own code block. Say they can start.
4. While they work: the story in `voice.txt` (step 6, "The voice"), the quotation, the explanation;
   `voice.py fill`, `batch.py build`, `voice.py make`, publish. The lessons work without pictures.
5. When the owner says the pictures are there: look at each, spots, marks (steps 4 and 5), publish.
   A picture the connector does not list yet is usually still uploading: look again, do not guess.

Work in batches of five or ten. Never change a scene after its prompt was handed over: the story is
written to the scene, and to the picture once it exists.

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
- `palette`, `scene`, `keywords`, optional `words`: the picture, written to the five rules above.
  One dominant focal point that carries the answer; few, large elements, each in its own area,
  each standing for one fact and acting on another; a different colour scheme every time; the
  book's theme (`theme` in its json, e.g. New York City); metaphors, no anatomy. `keywords` = the
  sound-alike objects (rule 2) and recurring symbols (rule 5) the scene uses. `words` = at most
  three short labels or numbers painted in it.
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
  it and say so in `note:`. Only a clear, undisputed medical error in the key is corrected (the owner,
  2026-11-19; no complex medical review): `key: C`, the tag `x:key-corrected`, and a `note:` with the book's key and why. A clear misprint in an option's
  text (for example a comparison printed the wrong way round) is corrected the same way with `option: C = the
  corrected text` (the tag and a `note:` are required too). A question the app cannot ask as printed (a matching
  key, "A-1. B-1,2") is asked as several questions in one lesson: an `asks:` block, one line per question,
  `question | option | option ... = A,C`, and a `note:` saying how they come from the book's key.

Quality bar: would the picture alone let the owner recall the answer a month later? If the
scene does not encode the *answer*, redo the scene before writing the story.

## 3. Build, publish, hand over

    python3 tools/content/batch.py build <scratch>/batch.txt --master <file>
    python3 tools/content/publish.py .content-work -m "Shahzad 2015: lessons 41-50"

**Only complete lessons are published (the owner, 2026-11-20).** A lesson without its picture is held
back: publish with `--complete-only`, and from a folder that holds only the book being published, so other
books are not touched, e.g. `mkdir -p F/books && ln -s $PWD/.content-work/books/<book> F/books/<book> &&
ln -s $PWD/.content-work/work F/work`, then `publish.py F --complete-only -m "..."`. Held-back lessons come
in by themselves at the first publish after their picture is set; the phone keeps progress meanwhile.

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

## 5. Marks: the numbered circles of "All circles" and of the question

Once a lesson has its picture and spots, it needs its marks (the app shows them in the question
and in the "All circles" table):

    python3 tools/content/batch.py marks shahzad-2015 41 50 --draft <scratch>/marks.txt

writes one line per distinct spot with the sentences spoken over it, and `<n>-draft.jpg` with those
circles. Per lesson, **looking at the picture**: keep 4 to 8 circles, each on one character, object
or event (merge overlapping ones, drop empty ones), and fill `what` (what is seen, at most 48
characters) and `means` (the medical fact, at most 110, from the explanation), English only. Then
`question:` = the circle(s) that show what the question is about without giving the answer away,
`answer:` = the circle(s) of the correct option. Apply, look at each `<n>-marks.jpg`, publish:

    python3 tools/content/batch.py marks <scratch>/marks.txt

It never touches lesson text. Keep the marks file under `work/<book>/marks/`.

## 6. Table first, and the recorded voice (since 2026-11-16, lessons 41-45 are the worked set)

**Table first.** Before the scene and the story, write for each question the 5 to 7 elements of
the picture and the fact each stands for, as `#` lines under `== N` in `batch.txt`
(`# element → fact (keyword: term → object)`); mark the one that is the ANSWER and make it the
picture's dominant element. A question with several correct options has one element per correct
option, all of them lit together. Scene, story and explanation are
then written from that table, in its order, and it becomes the lesson's marks (step 5) when the
picture arrives. A fact with no element, or an element with no fact, is a sign to redo the table.

**The voice.** The owner chose machine voice over recording ChatGPT: a woman narrates (a different
one from lesson to lesson), each character speaks with a voice that fits it, and each sentence's
start time is exact. Write the story **once**, in `voice.txt` (format at the top of
`tools/content/voice.py`): one line per sentence, parts `role: text` joined by ` || `, the Arabic
**fully vowelled** with a sukun where the voice should stop. Keep a line to one speaker where the
story allows it, so the circle can sit on whoever speaks. Then:

    python3 tools/content/voice.py fill voice.txt batch.txt     # story lines and quotation into batch.txt
    python3 tools/content/batch.py build batch.txt --master <file>
    <venv>/bin/python tools/content/voice.py make voice.txt      # needs: pip install edge-tts (in a virtualenv)

`make` writes `media/<id>-story-ar.m4a` and `-explanation-en.m4a`, sets each segment's `t`, and
refuses when a lesson's sentences differ from the voice file. Voices and pauses are in
`content/voices.json`. It takes about three minutes a lesson; parts are cached in
`.content-work/voice-cache`, so a rerun only makes what changed. In a cloud session the package's
certificate store needs the session's proxy certificate first:
`cat /root/.ccr/ca-bundle.crt >> $(<venv>/bin/python -c "import certifi;print(certifi.where())")`.
Nobody can hear the result but the owner: ask them how it sounds and fix what they name.

## 7. Keeping errors out (the owner asked for this on 2026-11-16; it applies to every new batch)

Nobody but the owner can see the app or hear the voice, so each kind of mistake has its own check.
None of them is optional, and none of them is a reason to touch lessons already published.

**Before the prompts are handed over** (a mistake here costs the owner a picture):
- Every element of the table is in the scene, with its place in the frame, and the ANSWER element
  is the dominant one. Read the scene once as the image generator would: is anything ambiguous?
- Do not hang the story on what generators get wrong: how many fingers, left against right, exact
  counts above three, small text. A number that matters is a painted label (`words:`, at most
  three); a detail the story cannot do without goes in `must:` (the prompt repeats it at its end).
- Painted labels are digits or single letters, never words.

**While writing** (the tools check what they can; read every warning):
- `batch.py build` refuses a repeated quotation, a changed recorded text, a missing field, and
  warns when the explanation never names the correct answer.
- A quotation is used only if you are sure of the line and of the work it is from. If in doubt,
  take another: a famous genuine line beats a perfect doubtful one. Say plainly to the owner that
  quotations are from memory until a verified bank exists.
- Medicine: only what the book's own explanation and mainstream teaching both support; no number
  you are not sure of; when the book's key is debatable keep it and say so in `note:`.

**After `voice.py make`** (the machine's ear):

    <venv>/bin/python tools/content/voice.py check voice.txt       # pip install faster-whisper

A speech recogniser on this machine writes down what it hears and each sentence is compared with
its text, inside its own time window. A sentence marked LISTEN TO THIS ONE was misread, cut, or
starts at the wrong moment: fix its vowelled line (or the role's voice) and make it again. A clean
run proves the words and the timing, not every vowel: still ask the owner how it sounds.
Give every spoken part at least three words: a one-word shout was the only part the recogniser
could not hear in lessons 41-45.

**When the pictures arrive, before the final publish** (two pairs of eyes):
- Go through the story sentence by sentence against the picture itself, not against the prompt.
  Where the picture differs, the picture wins: change that line in `voice.txt`, `fill`, `build`,
  `make` (only that sentence is spoken again). Only when the ANSWER element is missing or a painted
  number is wrong, ask the owner for that one picture again, and say exactly what was wrong.
- Read every painted number and letter in the picture and compare it with `words:`.
- Draw the circles (`<n>-spots.jpg`, `<n>-marks.jpg`) and look at every one.
- Then have a **second agent, who has not seen your work, check it**: give it the pictures, the
  sentences and the marks table, and ask it only to find errors (picture against story, circles,
  table, medicine, Arabic, quotation), not to fix them. Do not tell it what you think is right.
  Fix what it finds that is real, and tell the owner what was found and what you left.

**In the hand-over message** say what was checked by whom, and what nobody could check.

## Rules that are never broken

- Nothing is uploaded to Drive (every write prompts the owner). Publishing is `git push` only.
- Question files and PDFs never go into the repository; `batch.txt` (your own writing) does, under `work/`.
- Published ids never change. A published media file never changes under the same name
  (`pictures --replace b` publishes a corrected picture under a new name).
- Recorded text stays as recorded (`docs/status.md` lists which lessons).
