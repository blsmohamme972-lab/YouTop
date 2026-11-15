# Where the work stands

Updated 2026-11-15. Whoever works on YouTop next: read `CLAUDE.md`, then this, then
`docs/content-guide.md`. Rewrite this file whenever the state changes (it replaces the
`status-<n>.txt` files in the Drive folder "YouTop private", which stop at 7).

## App

- **1.8 (versionCode 9)** is on `main`; the APK was handed to the owner on 2026-11-15. Whether they
  have installed it (or 1.7 before it) is not known; 1.6 (versionCode 7), reading Drive, was the
  last version known to be on their phone. New in 1.8, after the owner asked for "the best app":
  a library with five pages and a bar at the bottom (Home dashboard, Books with search and folding
  chapters, Topics, Saved, Settings), backup and restore of the learning history as a file, a daily
  history for the dashboard's charts, messages instead of a blank feed (no lessons / nothing
  matches the focus / done for today), a one-time "swipe up" hint, and content that cannot be read
  no longer stops a sync or crashes a page (see "Content is never trusted" in `CLAUDE.md`).
  **The owner has not yet said what they think of it.**
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

- Book `shahzad-2015` (200 questions): **lessons 1-20 are published and complete** (Arabic story
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
