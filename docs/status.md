# Where the work stands

Updated 2026-11-19 (evening). Whoever works on YouTop next: read `CLAUDE.md`, then this, then
`docs/content-guide.md`. Rewrite this file whenever the state changes (it replaces the
`status-<n>.txt` files in the Drive folder "YouTop private", which stop at 7).

## START HERE (2026-11-19, night): automatic rounds every hour until the whole MedCosmos book (156) is done

- **Answer keys (the owner, 2026-11-19):** correct only a clear medical error in the book's key on which there is no
  disagreement; no complex medical review. In the batch: `key: C` (letters), the tag `x:key-corrected`, and a `note:`
  saying what the book's key was and why; `batch.py build` refuses a corrected key without both.
- **The owner's plan:** finish the whole MedCosmos Cardiac book (questions 1-156; first 1-100) with the highest accuracy, in automatic rounds, by
  this one agent **alone (no subagents, no other sessions; the owner said so twice)**. A scheduled task "YouTop:
  MedCosmos whole book (156), every hour" (trig_01CgSmtZZ33N4gFXAr9mtxZy) fires into session
  session_01TX4PEBKfzepCPexupsMsgY at minute 14 of every hour; its prompt lists the round. It also carries the
  work over a usage-limit pause: a firing during the pause fails, the next one resumes. Disable it with
  update_trigger (enabled=false) when all 156 lessons are published, or when the owner says stop.
- **One chapter holds lessons 1-100** (`q0001-0100`), another 101-156 (`q0101-0200`). `voice.py make` reads it when it starts and rewrites it when it
  ends: while it runs, never build, set pictures, spots or marks, or publish. Start make as a round's last step.
- **Unpublished work** lives in `.content-work/work/medcosmos-cardiac/batch-*/` (batch, voice, spots, marks) and is
  backed up on the work branch under `drafts/medcosmos-cardiac/` until it is published to `content`.
- **No automatic work (the owner, 2026-11-20):** do NOT create scheduled tasks or hourly rounds unless the owner
  asks for it in their own words. Work only when the owner asks.
- **Handover (2026-11-20, 13:30 UTC): the owner is closing this conversation.** Everything is on GitHub: main holds
  the tools, symbols and this file; `content` holds the 96 published lessons and every batch's work files under
  `work/medcosmos-cardiac/`; `drafts/medcosmos-cardiac/` on main holds the same files plus
  `prompts-missing.txt` / `image-prompts-missing.json` (the 41 pictures still wanted, prompts unchanged).
  To restart: `python3 tools/content/publish.py --export .content-work` rebuilds the scratch folder (media are on
  `content`); download the master `.jsonl` from Drive (id in `content/books/medcosmos-cardiac.json`); make a venv with
  edge-tts and faster-whisper for `voice.py`. Pictures still wanted: 1, 10, 16, 21, 25, 31, 38, 45, 60, 65, 76,
  81-110 (86-96, 99, 102, 103 never had one). The hourly scheduled task trig_01CgSmtZZ33N4gFXAr9mtxZy fires into
  THIS session; a new session should disable it (update_trigger enabled=false) and create its own if wanted.
- **2026-11-20, evening (a new session, alone): 108 lessons PUBLISHED.** 37 of the 41 wanted pictures arrived
  (14:08-14:11 UTC) and are published with new spots and marks, every grid and every circle drawing looked at:
  redrawn 10, 16, 21, 25, 31, 38, 45, 60, 65, 76, 81-85, 97, 98, 100, 101, 105-110 (`--replace b`), and the first
  pictures of 86-90, 92, 94-96, 99, 102, 103. Those 14 lessons were never published, so their audio was not on
  `content`: they were rebuilt from their batches and their voices made again (`voice.py make --only ...`;
  `voice.py check`: every sentence heard). Work files: `work/medcosmos-cardiac/pictures-2026-11-20/`
  (spots, marks, notes). **Pictures still wanted: 1, 91, 93, 104** (`drafts/medcosmos-cardiac/prompts-missing.txt`);
  91 and 93 are written and recorded but held back until their picture comes.
  - Found when looking (left as published; the owner decides): 25 lacks the "brick wall with a hole" block (VSD), the
    big 4 fills that block; 105 lacks the snapped harp post (papillary muscle rupture, one of the four answers);
    106 shows a woman (the story says a man) and the frozen leg is not the one the tube enters; 31's third hole is not
    visible; 84's thermometer looks like a cigarette.
  - Restart notes: the old `.content` worktree from an earlier session refused to update ("unrelated histories");
    delete `.content` and `git worktree prune`, then export. `publish.py --export` writes books only: copy
    `.content/work/medcosmos-cardiac` into `.content-work/work/` by hand.
  - Questions 111-120: tables, scenes and prompts written (no key corrected; 120 C "restrictive" at half-systemic RV
    pressure is the book's key, keep it with a note); prompts sent to the owner with those of 1, 91, 93, 104.
    Drafts in `drafts/medcosmos-cardiac/batch-0111-0120/`. Stories, Rumi lines (from memory: Book V guest house, Book I mirror hadith,
    lion that breaks itself, ten lamps, wine drunk on us, patience is the key, Book II names quarrel and hearing vs
    seeing, Book III 'not a single you', Book I mother of idols), explanations: written and built; recording.
  - Questions 121-130: tables, scenes and prompts written and sent (drafts `batch-0121-0130/`). Key of 130 kept
    on purpose: the book says gram-negative and fungal organisms; Staphylococcus aureus is in truth the commonest
    (a golden grape cluster in the picture, and a note); correcting it would leave no right option.
    Stories, Rumi lines (Book I opening: raw and ripe, burst your chains, lip of my confidant, days gone,
    every night You free the spirits, reed companion, the pen split, tongue like stone and iron, reason like an ass
    in mud, parted from one who speaks your tongue) and explanations written (`batch-0121-0130/`), not yet built.
  - Questions 131-140: tables, scenes and prompts written and sent (`batch-0131-0140/`). 136's key is kept with a
    note (the book says tPA is no better than streptokinase; GUSTO found it slightly better).
    Stories of 131-140 written (Rumi: endure the lancet, lion of God, lion without tail, bird without wings,
    the Beloved is all, listen to this tale, the Greeks polished their breasts, imitation, the fool's love is the
    bear's, love makes the sea boil).
  - Questions 141-156: tables, scenes and prompts written and sent (`batch-0141-0150/`, `batch-0151-0156/`). 156's
    option D corrected (`option: D = ...`, x:key-corrected): the book says class 3 drugs "inhibit potassium influx";
    they block the outward potassium current. ALL 156 PROMPTS ARE NOW WITH THE OWNER.
    NEXT: stories of 141-156; build and record 121-156 batch by batch (one chapter file: never two writers at once).
- **Where things stand (2026-11-20, 10:30 UTC):** texts and voices for 1-110 done. **96 lessons PUBLISHED** (complete
  only), published with `publish.py <scratchpad>/pub-mc --complete-only` (pub-mc links only `books/medcosmos-cardiac`
  and `work`).
  - **The owner is redrawing all pictures (2026-11-20)** with the same prompts and deleted the old ones from the Drive
    folder. New pictures are set with `batch.py pictures medcosmos-cardiac <folder> --replace b` (a published media name
    never changes), then NEW spots and marks go in each batch's `spots-v2.txt`/`marks-v2.txt` (round 2) or
    `spots-v3.txt`/`marks-v3.txt` (round 3). Redrawn: 2-9, 11-15, 17-20, 22-24, 26-30, 32-37, 39-44, 46-59, 61-64,
    66-75, 77-80. Still on the old pictures: 1, 10, 16, 21, 25, 31, 38, 45, 60, 65, 76, 81-85, 97, 98, 100, 101,
    104-110. 38 and 45 were uploaded as copies of 49; two files named 52 came, the clearer one was used.
  - Never had a picture: 86-96, 99, 102, 103.
  - NEXT writing: 111-156 (when fewer than 20 prompts wait).
  - New symbols must join two or three layers (sound, meaning, place); `symbols.py audit` rejects fewer.

## NEW BOOK (2026-11-19): MedCosmos Cardiac, 156 questions — New York City, quotations of Rumi

- **The owner's five picture rules (2026-11-19)** are now in the `youtop-lessons` skill ("The picture prompt is
  the most important step") and in `tools/content/batch.py` (`keywords:` field, prompt frame): interacting scene;
  abstract terms as concrete English sound-alike objects; answer signalled, decoration removed; memory palace for
  order; recurring symbols from `content/symbols.json`. The owner asked for an audit to approve symbols instead of
  them: `tools/content/symbols.py audit` (36 approved, 3 rejected on 2026-11-19: the Wall Street crash, the echo in
  the Whispering Gallery, Shahzad's split red weight); `batch.py` refuses prompts that draw a listed concept otherwise.
- Book facts: `content/books/medcosmos-cardiac.json` (id `medcosmos-cardiac`, lessons `medcos-q0001`…). The question
  file is a .jsonl in Drive (`03 MedCosmos Cardiac 156 Q`, file `1vymXJ2XpnuyoU5_twkwYxlQu0H2SOng4`); download it and
  pass it to `batch.py` as `--master` under its own name (`medcosmos-cardiac-156-q.jsonl`). Pictures go to "YouTop books/MedCosmos Cardiac/images" (`14D6AQoOofKzp8Z1jwMzGEqaOI5_GaGKm`, made 2026-11-19).
- **Questions 1-20 are written** (titles, tags, table, keywords, scene, story, Rumi quotation, explanation) in
  `work/medcosmos-cardiac/batch-0001-0020/batch.txt` on `content`, with `image-prompts-1-20.json`. The owner asked for
  these three things only: **no voice, not built, not published as lessons yet.** Quotations are from the Masnavi,
  translated from the Persian from memory; check them before recording.
- **Several right answers: done in app 2.5 (versionCode 16, 2026-11-19).** 83 of the 156 questions have several
  correct options (7 of the first 20: 7, 9, 10, 12, 18, 19, 20). The owner left the choice to the agent: the app now
  takes `answers` (tick every right option, then "Check"; right only when exactly those are ticked). `batch.py` reads
  the extraction `.jsonl` directly as `--master` and turns letter keys into `answers`. Question 82 has a matching
  key ("A-1. B-1,2 ...") and cannot be asked yet: `build` refuses it; decide later (rewrite as several questions?).

## START HERE (2026-11-17, night): automatic rounds of 20 lessons every two hours

- **The owner asked (2026-11-17, ~20:40 UTC):** every two hours make 20 new lessons with the highest accuracy
  and creativity, save the progress at the end of each round, and **do not stop until they say stop**. A
  scheduled task "YouTop: 20 new lessons every 2 hours" (trig_01TXiqB1Lo3S8bobTNg4WmKU) fires into session
  session_012MyNmqRZ5VoyLaDGc4zwec at minute 42 of every even hour UTC; its prompt lists the steps
  (catch up, pictures, 20 new lessons the standard way, save, message the owner). If the owner says stop,
  disable it with update_trigger (enabled=false).
- **Round 1 (2026-11-17, 20:40-23:00 UTC): lessons 106-125 published** with text and voice (work files
  `work/shahzad-2015/batch-0106-0115/`, `batch-0116-0125/`). Image prompts handed to the owner first.
  Two writer agents wrote the stories and explanations from my tables and scenes; a checker agent per ten
  found 0 errors and 11 warnings; fixed: tags of 119, story wording in 117, 120, 121, 123, explanation wording
  in 109, 111, 125, a word in 110. Left on purpose: characters who speak but are not painted (106, 107, 114,
  116). `voice.py check`: every sentence heard; the formulas of 110-112 were rewritten in words because the
  voice read "open parenthesis".
- **Pictures 21 and 22 arrived and are published** with spots and marks (`work/shahzad-2015/batch03/`).
- **Lessons 76-105 are complete** (pictures, spots, marks); see the git log of `content` and the work folders.
- **Round 2 (2026-11-17, 22:42 to about 01:30 UTC): lessons 126-145 published** with text and voice (work files
  `work/shahzad-2015/batch-0126-0135/`, `batch-0136-0145/`); figures of 138-140 published. Checkers found 3 errors,
  all fixed before the voice (verb vowels in 126 and 141, "ابنة الستّ والثلاثين" in 132) and 11 warnings, fixed:
  127, 132 (unnamed speakers now introduced), 133 explanation (the book's "enucleation" means resection; plexus
  invasion "uncommon"), 137 (a whole paw, not a claw), 141, 143, 145 (feeding, not drinking; the duct may heal,
  not "often"), names that began with a sukun. Left on purpose: 143's story is set in a hospital because its scene
  is. `voice.py check`: every sentence heard (two flags were digits and a clipped word).
- **Round 3 (2026-11-18, 00:43 to about 04:30 UTC, interrupted by the usage limit at about 01:10 and resumed at
  02:43): lessons 146-165 published** with text and voice (work files `batch-0146-0155/`, `batch-0156-0165/`);
  figures of 150, 154, 156 published. The tables and scenes were drafted by a designer agent this time and
  checked against the book's keys before the prompts were sent. Checkers: 1 error (164, Crimea mis-vowelled)
  and 8 warnings, all fixed before the voice: quotation lead-ins of 148 (cut before "I killed") and 149 ("come
  forth to me"), unattributed speech in 160, 161, 165, explanation of 154, 158 (the left main bronchus crosses in
  front) and 159 (why lymph node metastases is tempting). `voice.py check`: every sentence heard.
- **Round 4 (2026-11-18, 04:43 to about 07:15 UTC): lessons 166-185 published** with text and voice (work files
  `batch-0166-0175/`, `batch-0176-0185/`); figure of 173 published. Checkers: 1 error (172: "statins lower LDL more
  than any other class" removed) and 13 warnings; fixed: 170's unit, 172's candle that is not painted, 177 vowel,
  180 "2,3-DPG" read aloud, 182, 184, 185 Arabic. Left on purpose: quotations of 170, 172, 173 come from passages
  near earlier quotations of the same chapters (different sentences); 180's cynical line in a kind mouth; a few
  stories mention a voice that is not painted (166 Prokhor through the speaking tube). `voice.py check`: all heard.
- **Round 5 (2026-11-18, 06:42 to about 10:00 UTC): lessons 186-200 published, the last fifteen. All 200
  questions of the Shahzad book are now lessons with text and voice.** Work files `batch-0186-0195/`,
  `batch-0196-0200/` (no figures needed). Checkers: 0 errors, 6 warnings, all fixed before the voice: 196 (Masha
  now died last night and lies at home, so the quotation about a body carried out tomorrow fits), 197 (DOORS
  mortality "no significant difference", not which arm was lower), 190 and 195 (quotation lead-ins), 191 (an
  invented "stapled closure" sentence removed; Lavrenty now says "fruit of one field" before the quotation
  recalls it), 192 (salt "settled", not "peeled"). `voice.py check`: all heard; 196's whispered line was
  missed by the whole-sentence check but heard when checked alone.
- **Every lesson now has a recorded voice (2026-11-18, ~11:00 UTC); the phone voice is never used.** The owner
  asked "جميع الاصوات انت تصنعها احذف الالي" (all voices, you make them; remove the machine/phone voice). So
  lessons 1-40 got a story voice and an English explanation voice made by `voice.py`, **replacing the owner's own
  recordings of 1-10 and 31-40** (their files stay on `content` under the old names; the new ones are `-v2`).
  The story text was not rewritten: two agents only vowelled it (`work/shahzad-2015/voice-0001-0040/`), two
  checkers found 0 errors; the published text only gained the voice file's marks (shadda, tanwin fath) and, in
  ~55 sentences, a full stop or comma moved inside the closing ». Three speeches that the narrator read (2.10,
  13.9, 15.9) now have a character's voice and « ». `voice.py` now says symbols in English explanations as
  words (5-10, 4 x 3, =, /, +, parentheses). `voice.py check`: all heard (40.3 "A و B و C" is read letter by
  letter, poorly recognised). `batch.py status`: no lesson without story or explanation voice. The app's
  phone-voice fallback was NOT removed from the code (it now only plays if an audio file cannot be downloaded).
- **Next rounds: no new lessons (the book is finished; do not invent a new book).** Each round only checks the
  pictures folder and, for pictures that arrived, does pictures, spots and marks and publishes them.
- **2026-11-19: every picture that had arrived is published with spots and marks**, done alone in batches of
  about ten, each grid, spots drawing and marks drawing looked at before publishing (the owner asked: follow the
  quality rules, no extra agents). Work folders `work/shahzad-2015/pictures-0023-0030/` ... `pictures-0199-0200/`.
  Picture 60 is published as `shahzad-q0060-b.webp` (an interrupted run had left an empty `shahzad-q0060.webp`,
  which stays unchanged). For 198 the newest of three uploads was used.
- **Later on 2026-11-19: 69, 141, 147, 148, 150, 158, 168, 169, 179, 181, 193, 194, 197 arrived and are published**
  (`pictures-0069-0168/`, `pictures-0169-0197/`). **Lesson 40 has a new picture** (`shahzad-q0040-b.webp`,
  `pictures-0040-b/`): the owner found the three identical towers easy to forget, so a new prompt made them
  different and numbered (1 broad, road splitting in two = brachiocephalic; 2 tallest, road straight up = left
  carotid; 3 low and round, road sideways = left subclavian); the story was not changed.
- **Pictures still missing: 133, 154 only.** (Superseded list below kept for history.)
- Earlier list, pictures missing at midday 2026-11-19: 69, 133, 141, 147, 148, 150, 154, 158, 168, 169, 179, 181, 193, 194, 197.
  Those lessons show their book figure (if any) or no picture. When one arrives: `batch.py pictures`, spots,
  marks, look, publish (skill steps 4, 5, 7).
- Lesson learned this round: never run `voice.py make` and `batch.py build` on the same chapter at the same
  time; the second one to write the chapter file overwrites the other's changes.

## Lessons 46-75 (night of 2026-11-16 to 17) — done except pictures

The owner asked for "the next 30 lessons", with symbols and stories "creative to a very great degree",
then: finish their voices and everything they need, and **five hours after that message (it came at
about 21:40 UTC on 2026-11-16) start the following thirty (76-105)**, automatically, without mistakes.
A scheduled message fires in the session that did this work at 02:40 UTC on 2026-11-17.

- **Published, the standard way** (table first, prompts first): lessons 46-75, text complete (story,
  quotation, explanation, tags, question, figure of question 48). Work files on `content`:
  `work/shahzad-2015/batch-0046-0055/`, `batch-0056-0065/`, `batch-0066-0075/` (`batch.txt` with the
  tables, `voice.txt`, `image-prompts-*.json`).
- **Image prompts 46-75 were handed to the owner** as files (json and plain text) before the stories
  were written. **No picture 46-75 exists yet**; when they arrive: skill steps 4, 5 and 7 (look at each,
  spots, marks, second agent).
- **Voice: all of 46-75 is made, checked and published** (2026-11-17, about 03:30 UTC). `voice.py check`
  heard every sentence; its only flags were false alarms (the short quotation of 46, re-listened by
  itself and spoken in full; "mmHg" and "Ivor-Lewis lymphadenectomy" spelled differently by the
  recogniser). Nobody has heard the files: ask the owner how they sound.
- **A second agent checked each batch** (medicine, consistency, Arabic, quotations; it recognised all
  thirty quotations as genuine, from memory). Fixed from its findings, then voice remade: a wrong verb
  vowel (63 "نخرها", 48 "اعقد", 52 "ليسند"), story lines that contradicted the picture's scene or the
  numbers (52, 57, 61, 67, 69, 70, 74, 75), the translation of 53's quotation, explanation wording
  (46, 61, 65, 67, 68) and notes where the book differs from current teaching (55 Ghent criteria, 60
  seminoma treatment, 65 platelets in HIT, 68 option C). Left as they are, on purpose: characters who
  speak in a story but are not in the scene (a narrator's device; when the pictures come, the story
  follows the picture); 59's scene has the middle carter going down the back gully though the vein
  runs up (the prompt was already handed over; decide when the picture arrives); wrong options that
  the book itself does not discuss are not all explained.
- Because of the remake, the audio of 46-55 is published as `...-v2.m4a` (the first files stay on
  `content`, unused).
- Decisions taken without the owner (they were asleep): quotations are well-known lines from memory,
  as before (never checked against a printed Arabic translation); key notes were added where the
  book's key is debatable (47, 51, 65, 66, 68, 72) and 74 uses the corrected key D; question 38's
  kind of problem did not recur.

## Last task: circles in the question (app 2.2), done 2026-11-16, NOT YET SEEN BY THE OWNER ON THEIR PHONE

The owner asked for the picture's circles to be used in the question. Built as **2.2 (versionCode 13)**,
APK handed over on 2026-11-16, signed with the Drive key. Nobody has run it on a phone; wait for what
the owner says and for screenshots.

- **Question over the picture** (`QuizOverlay.kt`): the stem in a small translucent panel at the top
  (with "All circles", listen, close), the options small at the bottom, the picture free in between.
  After an answer the long explanation stays, small, in its own scrolling window (about six lines),
  with "Watch", "Next review" and "Next". Nothing moves on by itself after an answer any more
  (`autoAdvance` now only concerns lessons without a question).
- **Marks** (new content, optional): `Lesson.marks` = numbered circles `{spot, what, means}` (English);
  `Question.marks` = numbers shown while the question is asked, `Question.answerMarks` = numbers that
  appear once it is answered. Colours by number: `MarkColors` in `ui/Marks.kt`, same list in `batch.py`.
- **"All circles"**: button on the lesson's rail ("Circles") and in the question. Shows every numbered
  circle and an English table (# | in the picture | stands for; since 2.3 fixed under the picture,
  see below); tapping a row singles out its circle. It pauses the narration.
- **No lesson left unanswered** (`FeedScreen.kt`, `MainViewModel.reachable/holds/pass/heldBack`): the
  pager ends at the first lesson whose question is unanswered; a swipe there pulls the page a little,
  it springs back and the question opens. A lesson with several questions holds until the last is
  answered. Going back to earlier lessons, the library, and a lesson picked in the library stay free.
- Defaults used because the owner did not answer: a wrong answer comes back the same day as before;
  the question's circles show before the answer, the answer's after; "All circles" at any time.
- **Content**: lessons 1-20 and 31-40 have marks (5 to 8 each), written after looking at each picture;
  no story or explanation text was touched. Work files: `work/shahzad-2015/marks/` on `content`.
  Things a later builder may want to know: lesson 5's explanation says an extralobar sequestration
  "more often drains to systemic veins" while the book marks "venous return is usually systemic" as
  wrong (both are true: the usual, intralobar, type drains to pulmonary veins; text left as recorded);
  lesson 20's answer circle is the lodger at the window (nothing in the picture shows "distant
  metastases are rare"); lesson 36's answer circle is the cellar owner (normal coagulation is really
  all five cards); lesson 12's and 14's answers rest on the story more than on something visible.
- Seen only in the test pictures (`5-quiz`, `5b`, `5c`, `5d`, `5e`, `9a`, `9b`), plus lessons 5 and 36
  seeded for a moment. Not checked: the table on a small or a very tall phone, the feel of the
  spring-back, Arabic phone layout of the question (the table itself is always left-to-right English).
- New lessons need marks too: `batch.py marks` (see the skill, step 5). Lessons 21-30 have none (no pictures).

### 2.3 and 2.4, same day: what the owner found on their phone in 2.2, fixed

The owner sent two screenshots of 2.2: the floating table covered half the picture while the top of
the screen stood empty; after an answer a long stem, five options and the explanation covered the
picture and its circles. They asked for: on "Circles" the picture goes up, the texts above it go, the
table sits **under** the picture, **fixed**, its rows scrolling. Built, APK handed over 2026-11-16:

- **Docked picture** (`SpotlightImage(docked = …)`, `DockShare` = 43 % of the height): with the
  table open, or once a question is answered, the picture moves up under the status bar and
  shrinks, whole, to sit above a fixed panel (`DockPanel` in `Marks.kt`). Nothing lies over it.
- The table is that panel (no longer dragged); its rows scroll. Opened from a question it replaces
  the question until closed.
- After an answer the panel holds: the stem as one line (tap = all of it), the right option, the
  wrong pick if any, the whole explanation (scrolls), and "Watch / Next review / Next" fixed at the
  bottom. The other options are not shown again.
- **2.4 (versionCode 15), same day:** the owner then said the question itself hides the picture
  when it appears. So the picture is docked for the whole question (`docked = showAll || showQuiz`,
  `DockShare` now 46 %): stem (three lines, "More" / "Less") and options are in the panel under it,
  then the answer. When the panel holds more than fits (a fifth long option, the rest of the
  explanation) a fade and a small arrow at its bottom say so. This replaces the owner's first
  wording "question at the top, options at the bottom, over the picture": do not go back to it.
- Lesson learned: the owner's phone shows long stems and bigger type than the starter book; look at
  real long lessons (1, 36) seeded for a moment before handing an APK over. Done for 2.3; still not
  run on a real phone.

## Standard way of making lessons — the owner said "make this the standard" (2026-11-16, night)

After seeing lessons 41-45 complete, the owner made their method the standard and added one rule:
**the agent gives the image prompts first** (`batch.py prompts`), so the owner makes the pictures
while the agent writes stories, explanations and voice. It is written in the skill ("The standard
order") and in `CLAUDE.md`. Next batch: questions 46 onwards. The owner has still not said how
the voices sound; ask when they have listened.

The same night the owner asked to **lower the risk of mistakes in future work — and to correct
nothing in what is published** (lesson 44's "two fingers" stays as it is). Added for new batches
only: `must:` in `batch.txt` (details the prompt repeats as exact), `voice.py check` (a local
speech recogniser listens to the audio and compares each sentence with its text and its time
window), and the skill's step 7 with every check, including a second agent that only looks for
errors. `voice.py check` was run, read-only, on lessons 41-45 (2026-11-16): every story and explanation
sentence was heard as written and inside its own time window (so the four narrators, the French
and Portuguese ones included, do read the Arabic, and the timings hold), **except one**: lesson 44,
story sentence 8, where the one-word shout "واحدة!" was not heard. It may be swallowed or garbled
in the audio; **left as it is** on the owner's word. If they complain about that sentence, make it
again with a longer shout. Lesson drawn (in the skill): a spoken part needs at least three words.

## Lessons 41-45 and the machine voice (2026-11-16, evening) — WAITING FOR THE OWNER'S EAR AND PICTURES

- The owner gave up recording ChatGPT's voice and asked for the best Arabic voice a session can
  make. Tried with their permission: **edge-tts** (Microsoft neural voices, free, no key, unofficial;
  it sends the text to Microsoft). They liked the multilingual voices ("like Brian") and asked for:
  **women narrators only**, varied; **characters' voices that fit the picture's characters**; and
  the voice **in step with the circles**, with no audible joins. Built as `tools/content/voice.py`
  with `content/voices.json` (see the skill, step 6): one audio file per track, parts made one by
  one, joined with chosen pauses, each sentence's start written as `t`.
- **Lessons 41-45 are published** this way (story: narrator + characters; explanation: one English
  voice), written "table first" (elements = facts as `#` lines in `batch.txt`). The owner made the pictures from `image-prompts-41-45.json` (see below). Work files: `work/shahzad-2015/batch-0041-0045/`.
- **Nobody has heard these files.** A session cannot listen. Not known: whether the vowelling is
  right everywhere, whether "old" voices (slower, lower pitch; the service has no aged voices)
  convince, whether the pauses feel natural, whether Vivienne and Thalita (French and Portuguese
  voices) read Arabic and are acceptable narrators. Ask the owner; change `voices.json` or the
  vowelled line and run `make` again (a remade file gets a `-v2` name).
- The five quotations are from memory of well-known lines (Notes from Underground twice, Crime and
  Punishment twice, The Idiot); **not checked against a printed Arabic translation**. The owner was
  told that no quotation in the library has been checked that way and that a verified quotation
  bank would fix it; they have not asked for it yet.
- Not done: the automatic check of the audio by speech recognition (planned, not built); voice for
  lessons 11-30 (phone voice) and the English explanations of 1-40; whether to replace the owner's
  own recordings of 1-10 and 31-40 (they did not say).
- **Pictures 41-45 arrived and are published** (2026-11-16) with spots and marks (6 to 8 each;
  `spots.txt`, `marks.txt` in the batch's work folder). All four match their stories; in 44 the
  clerk points one finger at the tinsmith where the story says he raises two fingers (left as is).
  Picture 41 arrived a little later and is published the same way (6 marks).
- Ideas discussed and not built, for quality at low cost: a fixed cast of characters per concept, a
  symbol dictionary (same fact, same symbol), a style sheet from sentences the owner likes, a cheap
  reviewing agent, a verified quotation bank. The owner listened and moved on to the voice.

## App

- **2.4 (versionCode 15)** is on `main` (see above); before it 2.3, 2.2 and 2.1 (versionCode 12), handed to the
  owner on 2026-11-16 (the third APK that day, after 1.9 and 2.0). A screenshot they sent later that day shows 2.0 or 2.1 running on
  their phone with a real lesson (buttons with names, "Home" at the top right).
- New in 2.1, asked for by the owner: **progress by topic across books.** On the Topics page every
  specialty and subtopic is a cover (a picture from the web, built into the app) with the same
  six-colour bar and numbers as a book (new, studied 1×, 2×, 3×, 4×, 5×+); the other tag groups are
  rows with the bar. The 34 covers are public domain or CC0 (sources in
  `app/src/main/assets/licenses/topic-covers.txt`), tinted per specialty by `tools/covers/process.py`.
  Wikimedia's search API answers "too many requests" from cloud sessions; `Special:FilePath/<name>?width=640`
  and the Openverse API (200 searches a day) work. The page was looked at with the three example
  lessons tagged for a moment; with the owner's real 40 lessons it was not seen.
  **The owner has not yet said what they think of 2.0 or 2.1.**
- New in 2.0, after the owner said of 1.9 "I find it hard to use" and asked for books studied one
  at a time in order "like Anki", and for each book's cover to show how many lessons were studied
  once, twice, three times…:
  - The app **opens on Home** with one "Start studying" button; closing the library plays the lesson.
  - **Each book is a card**: cover, a bar in six colours (new, studied 1×, 2×, 3×, 4×, 5×+) with the
    numbers under it, "Study this book" and "Lessons". The same bar is on the covers of the Books
    page and on a book's own page. With more than one book there is also "Study all books together".
  - Studying a book goes **in book order** ("weakest topics first" is now off unless switched on).
    A new choice of book or topics starts the feed on it at once (it used to finish the old lesson first).
  - Every button on a lesson has its name under it; the top-right button is "Home".
  - Fixed from a second review of 1.9: a review done on its due day but before the hour was
    scored as early (shorter gaps than deserved); a quick first answer on a lesson that was
    mid-learning before 1.9 could send it away for two weeks; a quiz shortened by a sync could
    not finish; the press effect moved only the label.
  With only one book published, "Study all
  books together" and switching between books were tested by code reading and unit tests only.
- New in 1.9, after the owner asked to find and fix errors, make spaced repetition "advanced and
  easy", make tags easy and beautiful, and make lesson-making cheaper:
  - **Smart scheduling** (default): the FSRS-4.5 memory model with one dial, "How well to remember"
    (85 / 90 / 94 %). Old progress is taken over at each lesson's next review. The classic rules are
    still there (Settings → Advanced timing → Smart scheduling off).
  - Learned lessons are due **by the day** (04:00 to 04:00), not at the minute they were studied;
    of the due ones the most overdue for its interval comes first; reviewing early earns
    proportionally less (a hard answer given early used to earn more than one given on time).
  - After the last answer the question shows **when the lesson comes back**. Home has a **Memory**
    card (chance of recall now) and lists lessons missed five times or more.
  - **Topics page rebuilt**: three specialty cards with their subtopics, the other tag groups as
    chips, the chosen tags on top with "Study N lessons". Every tag has an Arabic name (shown on
    a phone set to Arabic). Buttons and cards restyled.
  - Fixed: a recording could start by itself behind "question first" or the library, or restart
    behind a paused screen after dragging the seek bar; a sync that started before the phone's
    lessons were read could fall back to the old Drive copy; a question open during a sync could
    crash; a spot with radius 0 could crash; lessons are left out of Android's own backup so the
    learning history always fits in it.
  - `tools/content/batch.py`, `content/books/<book id>.json` and the `youtop-lessons` skill (see below).
- No builder has ever run the app on a real phone. The owner sends screenshots when something
  looks wrong.
- Not done on purpose, because they need a real phone to test: audio focus for the phone's voice,
  switching voice engine when the default one has no Arabic, and a daily reminder notification
  (offered to the owner on 2026-11-15; not asked for yet). The backup's file picker could not be
  tried either (no picker under Robolectric); what it writes and reads is tested.
- In lists, a lesson's small picture is the full picture when it is not yet stored on the phone
  (GitHub has no thumbnails). Fine for now; with thousands of lessons and "download for offline"
  switched off it would cost data. A fix would be small thumbnails written by `publish.py`.

## Content

- Book `shahzad-2015` (200 questions): lessons 41-45 see above. **Lessons 1-20 are published and complete** (Arabic story
  ending in a real Dostoevsky quotation, English explanation, question, tags, picture, spots).
  Chapter id `q0001-0100`, lesson ids `shahzad-q0001` … Never change them.
- **Lessons 31-40 are complete, with recorded story voice** (published 2026-11-15): pictures (the
  owner's, looked at one by one), spots (`work/shahzad-2015/batch04/spots-0031-0040.py`) and the
  owner's ChatGPT recording `story 31 to 40.m4a` (Drive id `1Hg1_2DkvFuTovD0qb9QCA4fjePBn2DR4`),
  cut into `media/shahzad-q0031-story-ar.m4a` … `q0040` with a start time `t` per segment. Scripts
  and alignment: `work/shahzad-2015/voice/` on `content` (`align.py`: character-level difflib
  match of faster-whisper words; `cut_and_set.py`; `edges.py` re-transcribes each file's first and
  last seconds as a check). Only one sentence start was not recognised (story 37, third
  sentence: it starts right after the previous one). **The timings were not checked by ear; the
  owner has not yet said whether they sound right.** The story text of 31-40 must still not
  change (it is what was recorded); if a picture ever disagrees, ask for a new picture.
- **Explanation voice for 31-40 requested** (2026-11-15): the owner asked for the English
  explanations of 31-40 as a reading file for ChatGPT. Sent them
  `work/shahzad-2015/voice/YouTop-explanations-31-40-for-voice.txt` (on `content`; titles
  "Explanation thirty-one" …, one sentence per line). From now on **the explanation text of 31-40
  must not change either.** When the recording arrives: align and cut as for the stories (language
  `en`), name the files `shahzad-q00NN-explanation-en.m4a`, set the explanation track's `audio` and
  each segment's `t` (the app plays `audio` on any track: `MainViewModel` → `Narrator.load`).
- **Lessons 21-30 are published without pictures and without spots.** Image prompts were handed
  to the owner (`work/shahzad-2015/batch03/image-prompts-21-30.json`); no pictures 21-30 had
  arrived by 2026-11-15. The owner chose to do 31-40 first.
- On 2026-11-15 the owner said of lessons 1-20: the stories are not eloquent Arabic and lack
  forceful language, the quotations should be stronger, the pictures should be better. Lessons
  21-30 were written to answer that (see "Style the owner asked for" in `docs/content-guide.md`).
  **They have not said in words whether 21-40 are what they wanted**, but they went on to make the pictures and the recording for 31-40, and they did not ask for 1-20
  to be rewritten. Rewriting the stories of 1-10 would also break their recorded narration.
- Published on `content`: `catalog.json` → `books/shahzad-2015/book-<hash>.json` →
  `chapters/q0001-0100-<hash>.json`, with 20 pictures, the figure of question 10 and twenty story
  recordings in `media/`. It was moved there from Drive unchanged except for the references.
- Recorded voice: the **story** of lessons 1-10 and 31-40 plays a recording the owner made from ChatGPT's
  voice (`media/shahzad-qNNNN-story-ar.m4a`, each segment has a start time `t`). The timings came
  from speech recognition and were never checked by ear by a builder; **the owner has not yet said
  whether they sound right.** Explanations and the stories of 11-30 use the phone's voice.
- Master questions (all 200, word for word), style and earlier work files are in Drive,
  "YouTop private/books/shahzad-2015" (`mcqs-*.json`, `work/README-work.txt`, `work/batch02-v2.py`,
  `work/spots-0011-0020.py`, `work/stories-v3.py`, `work/make_batch.py`). Read them with the Drive
  connector's download tool. New work files go under `work/` on `content`.
- The quotations already used are the last story segment of each published lesson (also listed
  in `status-5.txt` in "YouTop private"). Do not repeat them.
- The owner's original pictures: Drive, "YouTop books/Shahzad 2015/images"
  (`1Rh8ghw3oe5_5yYzaTvH5iFcpJQTnrRxZ`), named by question number, some without ".png".

## What a cloud session can and cannot reach (found 2026-11-15, second session)

- In some sessions the network policy **blocks `drive.google.com`, `drive.usercontent.google.com`
  and `huggingface.co`** (curl answers "CONNECT tunnel failed, 403"). Then the public-link `curl`
  recipes for Drive do not work at all. Only the owner can change that (the cloud environment's
  settings → Network access → allow those two Drive domains, or a broader level).
- The Drive connector still works there: `search_files` with `parentId = '<folder id>'` lists a
  folder, and `download_file_content` **saves a binary file (picture, audio) to disk** and prints
  its path, for private files too. So pictures (about 3 MB) never need the public link. Its limit
  is 10 MB per file: `story 31 to 40.m4a` (10,621,986 bytes) is refused. Recordings should be
  **under 10 MB each** (five stories per recording is about 5 MB) and can then sit in any Drive folder.
- Without Hugging Face, faster-whisper cannot fetch its model. A Whisper "small" model is
  reachable as a GitHub release: `pip install sherpa-onnx` and
  `https://github.com/k2-fsa/sherpa-onnx/releases/download/asr-models/sherpa-onnx-whisper-small.tar.bz2`
  (download checked; recognition with it not yet tried).
- 2026-11-15: after the owner changed the environment's network access, `curl` on
  `drive.usercontent.google.com` and `huggingface.co` worked (faster-whisper fetched "small"
  itself). `pip install` into the system Python was refused by the session's permission check;
  a virtualenv in the scratchpad worked. If Drive is blocked again, recordings under 10 MB can
  still be read with the connector.
- Still no pictures 21-30 in the images folder.

## How lessons are made since 2026-11-16

`tools/content/batch.py` with the `youtop-lessons` skill (`.claude/skills/youtop-lessons/SKILL.md`).
A session fills in one `batch.txt` and the tool does the rest. It was checked by rebuilding
lessons 31-40 from their published text: identical output, recordings and spots kept, and a changed
recorded story refused. `pictures` and `spots` were tried on a made-up picture only. **No real batch
has been made with it yet**: the first one (questions 41-50) is also its first real test.

## What the owner may ask next

- **The pictures of 21-30** (see above), then **questions 41-50** the same way. Per question: an
  image prompt (one JSON file `{"items":[{"info":"41. …"}]}` handed to the owner), an Arabic
  story, a real Dostoevsky quotation with its work, an English explanation, tags. Publish the
  lessons without pictures first; when the pictures arrive, look at each one, correct the story
  where the picture differs, set a spot per sentence and verify by drawing the circles.
- **Rewriting the stories and quotations of 1-20** in the new style, if they like 21-40.
- **More recorded voice.** What worked: give the owner a text file with reading instructions
  followed by the stories, one sentence per line; they have ChatGPT read it and screen-record with
  "media sounds" (about 170 MB for 12 minutes; they put it in Drive and send the link). Then:
  ffmpeg extracts the audio; faster-whisper ("small", language `ar`, `word_timestamps`; pass the
  audio as a 16 kHz float32 numpy array, its own decoder fails in cloud sessions) gives word times;
  match the recognised words to the story words (normalise Arabic: strip diacritics, unify
  alef/ya/ta marbuta; `difflib.SequenceMatcher`) to get each sentence's start; a sentence whose
  first words were not recognised starts right after the previous one ends; cut one
  loudness-normalised mono AAC file per lesson (drop ChatGPT's preamble and the spoken "story one"
  titles, keep the quotation's source) and write each sentence's start in milliseconds as the
  segment's `t`. ChatGPT ignores requests for pauses, so cutting on silence does not work. An
  audio-only recorder inside the app was discussed and not built.
- **Housekeeping, only if asked:** old catalogs, `book.json` and chapter versions, the webp and
  m4a copies and `permission-test*.txt` are still in Drive. Trashing them is a Drive write, so it
  shows the owner a prompt for each file. The Drive copy of the lessons is also what a phone still
  on 1.6 reads, so leave it until the owner has 1.7.

## The owner's rules, learned the hard way

- The picture is never zoomed or cropped; only a spotlight moves.
- The Arabic story is a real story (people, an event, a turn) with no medical explanation in it;
  all the science goes in the English explanation.
- Keep the answer keys the book prints, but note a debatable one in the explanation.
- No "Allow once" prompts: nothing is written to Drive.
