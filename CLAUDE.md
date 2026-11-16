# YouTop — guide for whoever works on this next

Read this first. It is written for an agent (or person) with no prior knowledge of the project.
`README.md` describes the app and the content format in more detail.

## What this is

A personal Android app owned by one person, used on their own phone. It shows fullscreen lessons
you swipe through like short videos: a picture, a narrated story (Arabic), an explanation
(English), then a multiple-choice question over the picture. A spaced-repetition scheduler decides
what comes next. Lessons are made from MCQ books and live on the `content` branch of this
repository, so content changes never need a new APK. The library is planned to pass 5,000 questions in cardiac, thoracic and
vascular surgery, each tagged from a small tag system (about 55 tags).

The owner is not a programmer and uses a phone. They keep nothing themselves: no key, no
password, no notes. **Everything you need is in this repository or in their Google Drive, as
listed below. Start with `docs/status.md`: it says where the work stands. Do not ask them for keys, secrets or files; find them there.** Explain things
plainly, send them the APK file itself, and never ask them to run commands.

## Where everything is

| What | Where |
|---|---|
| Code | GitHub `blsmohamme972-lab/YouTop` (public), branch `main`. Do new work on a branch, then merge it into `main` (pull request) so `main` always holds what the owner's phone runs. |
| Lesson content | Branch **`content`** of this repository (no app code on it). The app reads it from `https://raw.githubusercontent.com/blsmohamme972-lab/YouTop/content/`. Written only by `tools/content/publish.py`. The owner has given permission to push to `content`. |
| The owner's pictures | Google Drive account `gpmo592@gmail.com`, folder **YouTop books**, id `1r5301GkHo5hZItw4gSwTUzrBD6HFcHUI`, `<Book name>/images`. Shared as "anyone with the link – viewer", so it is read with plain `curl`. It also still holds the lessons as published before 1.7; the app reads them only if GitHub cannot be reached. |
| Signing key | Same Drive account, folder **YouTop private**, id `1QQ4CyHwy6VjuYfYRobermLpT5BqLQVqb`, file `youtop-signing.txt` (id `16CYev5pmNzRKGAaLNilaSkfXNq1bFvpL`). Private. Read it with the Google Drive connector. |
| Work status | `docs/status.md` (the old `status-<n>.txt` files in YouTop private stop at 7). |
| Source PDFs, question files | **YouTop private/source books** and **YouTop private/books/`<book id>`**. Read-only now. |
| Tag system | `content/taxonomy.txt` in this repository. |
| Topic cover pictures | `app/src/main/assets/topics/<tag, with "-" for "/">.webp`, one per topic tag. Public domain or CC0 only (mostly Gray's Anatomy 1918 plates); sources in `assets/licenses/topic-covers.txt`. Made by `tools/covers/` (`fetch.py` finds candidates, `choice.json` says which, `process.py` crops and tints). A new topic without a file shows its colour and sign instead. |
| How to make lessons | The `youtop-lessons` skill and `tools/content/batch.py`; reference: `docs/content-guide.md`. Each book's facts: `content/books/<book id>.json`. |

If the Google Drive connector is missing in your session, or is signed in to a different account,
that is the one thing only the owner can fix: ask them to connect Google Drive as
`gpmo592@gmail.com` in Claude's connector settings. Everything else you can do yourself.

Rules that must not be broken:

- **Do not upload, move or trash anything in Google Drive.** Every Drive write shows the owner an
  "Allow once" prompt on their phone (about thirty for ten lessons), and it cannot be switched
  off: Auto mode, "Always allow" on every connector tool and a new session were all tried.
  Reading and downloading from Drive, `curl` on public links and `git push` do not prompt. That
  is why lessons are published by `git push`. Write to Drive only if the owner asks for it.
- **Publishing through this public repository, without encryption, is the owner's decision.** They
  were told the book's questions become openly readable and that the publisher could have GitHub
  take the repository down, and accepted. Do not reopen it. Source PDFs still never go on GitHub.
- **Never commit the signing key or its passwords.** The repository is public. `keystore/` is git-ignored.
- **Never put anything private in "YouTop books"** and never share "YouTop private". 
- **Use only the Drive account `gpmo592@gmail.com`.** If the Drive connector is signed in to another
  account (check the `owner` field of what you read), stop and tell the owner.
- Old files elsewhere in the owner's Drive are unrelated. Ignore them.

## Updating the app (code change → APK for the owner)

    tools/setup-env.sh                 # once per machine: Android SDK, local.properties, Maven mirror
    # Read youtop-signing.txt from Drive with the Drive connector, save it outside the repo, then:
    tools/setup-signing.sh /path/to/youtop-signing.txt
    tools/build.sh                     # tests + app/build/outputs/apk/release/app-release.apk

Then send the owner the APK file, and commit and push the code.

- Raise `versionCode` (and `versionName`) in `app/build.gradle.kts` for every APK you hand over.
- **Always sign with the key from Drive.** Without it the build still succeeds but is signed with a
  throwaway debug key, and Android refuses to install it over the owner's app; they would have to
  uninstall first and would lose their learning progress. The build prints a warning in that case.
  Check with: `$ANDROID_HOME/build-tools/36.0.0/apksigner verify --print-certs <apk>` → `CN=YouTop`.
- Needs JDK 17 or newer. No emulator is needed or available in cloud sessions.
- If Gradle fails with "429 Too Many Requests" or "Could not find …jar" from Maven Central, run
  `tools/setup-env.sh` (it installs a mirror) and `rm -rf ~/.gradle/caches/modules-2`, then retry.
- GitHub Actions (`.github/workflows/build.yml`) also tests and builds every push; its APK is signed
  with the real key only if the repository secret `YOUTOP_SIGNING` is set.

### Checking your work

There is no device. `tools/build.sh` runs:

- `SchedulerTest` — the spaced-repetition rules (smart scheduling and the classic rules).
- `ScaleTest` — syncs a made-up 5,500-lesson book and fails if anything done per swipe gets slow.
- `ContentTest` — the bundled starter book and the Drive folder parser.
- `GitHubSyncTest` — sync from a pretend GitHub, the fall-back to Drive, `gh:` media, and a damaged
  book that must not stop the other books or lose the copy on the phone.
- `DashboardTest` — the dashboard's numbers, the lesson search and the backup file.
- `ScreensTest` — starts the real activity under Robolectric, taps through it and saves a picture
  of each screen (the question, "All circles", the swipe that comes back) to `app/build/screens/` (English, and the main pages again in Arabic). **Look at those pictures** after any UI change; it is the
  only way to see the app. Add a step there for any new screen.

The bundled starter book has no quotation and no question figure. To see real lessons, put a
book file and its pictures under `app/src/main/assets/seed/` for a moment (pictures as
`asset:seed/...`), drive it from a throw-away test, look, and delete both again. **Never commit
book text**: the repository is public.

Say plainly to the owner that the app was not run on a real phone, and ask for a screenshot if
something looks wrong on theirs.

### Map of the code (`app/src/main/java/app/youtop/`)

- `data/Models.kt` — the content format (Book → Chapter → Lesson → Track/Segment/Spot, Mark, Question).
- `data/DriveClient.kt`, `ContentRepository.kt`, `MediaCache.kt` — sync and offline cache. A file
  is referred to as `gh:<path on the content branch>` or, in content from before 1.7,
  `drive:<file id>`; both must keep working. Sync reads `catalog.json` from GitHub and uses the
  Drive folder only when that fails and the phone holds no GitHub content yet.
  Only a light index of all lessons is kept in memory; a lesson's text is read from its chapter
  file when it is about to be shown. Keep it that way: the library will hold thousands.
- `study/Tags.kt` — tag matching ("focus") and per-tag statistics.
- `study/Scheduler.kt` — spaced repetition and "what to show next" (pure Kotlin, well tested).
  "Smart scheduling" (default) is the FSRS-4.5 memory model (`Memory`): each lesson has a stability and
  a difficulty, and comes back when the chance of recall falls to `StudySettings.retention`. The
  classic SM-2 rules remain behind `smart = false`. Learned lessons are due by the day
  (`Scheduler.dayEnd`), lessons in their learning steps by the minute.
  `study/Progress.kt` — stores each lesson's memory state on the phone.
- `player/Narrator.kt` — plays a narration from an audio file, or the phone's voice, or silently.
- `MainViewModel.kt` — the feed queue and everything the screens call. The feed draws from one
  book when `StudySettings.book` is set (`studyBook`), otherwise from every book not left out; a
  new choice of book or topics restarts the feed on it (`replan(restart = true)`, `generation`).
- `ui/Theme.kt` (colours and the two typefaces: IBM Plex Sans Arabic for the interface, Amiri for
  stories and quotations; both under the Open Font License, texts in `assets/licenses`).
- `ui/FeedScreen.kt` (the feed and one lesson, the hold on an unanswered lesson, and what the feed
  says when it is empty), `Spotlight.kt` (spotlight on the whole picture, never zoomed, and the
  numbered circles), `QuizOverlay.kt` (question over the picture), `Marks.kt` (circle colours and
  the "All circles" table).
- `ui/LibraryScreen.kt` — the library: five pages behind a bar at the bottom.
  `HomeTab.kt` (the page the app opens on: today, then each book as a card with its colour bar of
  "times studied" and "Study this book"; its numbers come from `study/Dashboard.kt`), `BooksTab.kt` (shelf,
  search over all lessons, a book's chapters), `TopicsTab.kt` (progress by topic across all books: each specialty and subtopic as a
  cover with the same colour bar and numbers as a book, the other tag groups as rows; tapping one
  studies only it), saved lessons,
  `SettingsTab.kt` (pace, lesson behaviour, sync, backup, advanced timing). `Parts.kt` holds the
  shared pieces (cards, switches, buttons); use them for anything new so pages keep one look.
- `data/Backup.kt` — the learning history as a file the owner saves and restores from Settings.

Keep new fields in `Models.kt` optional with defaults, so older content keeps loading, and keep
`StudySettings` / `CardState` / `Progress` backwards compatible, so stored progress and backup files
keep loading. Content is never trusted: a book that cannot be read is skipped (the copy on the phone
stays), repeated ids are dropped, and a question without a valid answer is not asked
(`Lesson.clean()`); keep it that way when the format grows. Every text the owner sees is in
`res/values/strings.xml` and `res/values-ar/strings.xml`; add both.

## Updating content (no APK needed)

**Making lessons: use the `youtop-lessons` skill** (`.claude/skills/youtop-lessons/SKILL.md`). It is
the short path: `tools/content/batch.py` does every mechanical step (copies the questions, builds
lessons and image prompts, converts pictures, draws spots, checks the owner's rules), so a session
writes only the stories, explanations, tags and scenes. `docs/content-guide.md` is the full
reference. In short: lessons are JSON files on the `content` branch; the app
reads `catalog.json`, which lists each book's file, which lists chapter files of about 100
lessons; pictures and audio sit beside them in `media/`. To change anything:

    python3 tools/content/publish.py --export <scratch folder>     # what is published now
    # edit the chapter files there, add new pictures/audio under books/<id>/media/
    python3 tools/content/publish.py <scratch folder> -m "what changed"

It validates, commits, pushes and reads the result back from the public address (which lags
about five minutes). Then update `docs/status.md`.

Never change a published book, chapter or lesson id: the owner's learning progress is stored
under `book/chapter/lesson` on their phone.

## Decisions already made (do not reopen without the owner asking)

- Kotlin + Jetpack Compose, single activity, no login, no backend other than the public files
  on the `content` branch (and the old Drive folder as a spare).
- Content is published through this public GitHub repository, unencrypted, and never by
  uploading to Drive (see the rules above).
- Since 2.0 the app opens on the library's Home page (as Anki opens on its decks), not on the feed,
  and books are studied one at a time in book order ("Study this book"); "all books together" is the
  other choice. The owner asked for this after finding the app hard to use. "Weakest topics first"
  is off by default for the same reason.
- A book's progress is always drawn as the six colours of `RepsColors` (`Parts.kt`): lessons never
  studied, studied once, twice, three, four, five times or more. The owner asked for exactly this,
  and then for the same on every topic (tag) with a cover picture, counted across books.
- Smart scheduling is the default since 1.9; lessons studied before it are taken over from their
  interval and ease at their next review (nothing is reset).
- Ratings are inferred, never asked: right answer = good (fast = easy), wrong = again.
- Since 2.2 a lesson with a question cannot be swiped away unanswered: the page springs back with
  the question open; once answered the owner swipes on when they want (nothing moves by itself).
  Only leaving through the library skips a question (watched to the end = hard, otherwise nothing).
- Since 2.2 the question lies over the picture without hiding it (stem small at the top, options
  small at the bottom), shows only the circles that belong to it (the answer's after answering), and
  "All circles" shows every numbered, coloured circle with a draggable English table.
- The picture is never zoomed or cropped; only a spotlight moves over it, in step with the voice.
- The Arabic story is a real story with no medical explanation in it; the science is in the
  English explanation (see `docs/content-guide.md`, "The story is a story").
- Each lesson plays story → explanation → question automatically, and waits there for the answer. "Question first"
  in Library → Study skips straight to the question.
- The app is for the owner's personal use and is meant to be as engaging as possible.
