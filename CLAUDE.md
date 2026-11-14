# YouTop — guide for whoever works on this next

Read this first. It is written for an agent (or person) with no prior knowledge of the project.
`README.md` describes the app and the content format in more detail.

## What this is

A personal Android app owned by one person, used on their own phone. It shows fullscreen lessons
you swipe through like short videos: a picture, a narrated story (Arabic), an explanation
(English), then a multiple-choice question over the picture. A spaced-repetition scheduler decides
what comes next. Lessons are made from MCQ books and live in Google Drive, so content changes
never need a new APK.

The owner is not a programmer and uses a phone. Explain things plainly, send them the APK file
itself, and do not ask them to run commands.

## Where everything is

| What | Where |
|---|---|
| Code | GitHub `blsmohamme972-lab/YouTop` (public). Work so far is on branch `ccr-4d386cf7-4se1m2`; check whether it has been merged into `main` before starting. |
| Lesson content | Google Drive account `gpmo592@gmail.com`, folder **YouTop books**, id `1r5301GkHo5hZItw4gSwTUzrBD6HFcHUI`. Shared as "anyone with the link – viewer" on purpose: the app reads it without signing in. |
| Signing key | Same Drive account, folder **YouTop private**, id `1QQ4CyHwy6VjuYfYRobermLpT5BqLQVqb`, file `youtop-signing.txt` (id `16CYev5pmNzRKGAaLNilaSkfXNq1bFvpL`). Private. |

Rules that must not be broken:

- **Never commit the signing key or its passwords.** The repository is public. `keystore/` is git-ignored.
- **Never put anything private in "YouTop books"** and never share "YouTop private". 
- **Use only the Drive account `gpmo592@gmail.com`.** If the Drive connector is signed in to another
  account (check the `owner` field of anything you create), stop and tell the owner; do not create
  YouTop files elsewhere.
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

- `SchedulerTest` — the spaced-repetition rules.
- `ContentTest` — the bundled starter book and the Drive folder parser.
- `ScreensTest` — starts the real activity under Robolectric, taps through it and saves a picture
  of each screen to `app/build/screens/`. **Look at those pictures** after any UI change; it is the
  only way to see the app. Add a step there for any new screen.

Say plainly to the owner that the app was not run on a real phone, and ask for a screenshot if
something looks wrong on theirs.

### Map of the code (`app/src/main/java/app/youtop/`)

- `data/Models.kt` — the content format (Book → Chapter → Lesson → Track/Segment/Spot, Question).
- `data/DriveClient.kt`, `ContentRepository.kt`, `MediaCache.kt` — sync from Drive, offline cache.
- `study/Scheduler.kt` — spaced repetition and "what to show next" (pure Kotlin, well tested).
  `study/Progress.kt` — stores each lesson's memory state on the phone.
- `player/Narrator.kt` — plays a narration from an audio file, or the phone's voice, or silently.
- `MainViewModel.kt` — the feed queue and everything the screens call.
- `ui/FeedScreen.kt` (the feed and one lesson), `Spotlight.kt` (camera over the picture),
  `QuizOverlay.kt` (question over the picture), `BooksTab.kt`, `StudyTab.kt`, `LibraryScreen.kt`.

Keep new fields in `Models.kt` optional with defaults, so older content keeps loading, and keep
`StudySettings` / `CardState` backwards compatible, so stored progress keeps loading.

## Updating content (no APK needed)

The app reads `YouTop books/catalog-<n>.json` with the highest `n`; it lists each book's
`book.json` by Drive file id. Format: see README.

The Drive connector cannot change a file's contents, only create files. So to change anything:

1. Upload the new or changed files (pictures, audio, `book.json`) into the book's folder.
   Use `disableConversionToGoogleType: true`, or JSON turns into a Google Doc.
2. Upload `catalog-<n+1>.json` to "YouTop books" listing **every** book (old and new) with the
   current `book.json` file ids.
3. Check it the way the app does: `https://drive.google.com/embeddedfolderview?id=<folder id>`
   must list the new catalog, and
   `https://drive.usercontent.google.com/download?id=<file id>&export=download&confirm=t` must
   return the JSON / picture (not an HTML page).
4. Trash the superseded `book.json` and catalog files.

Never reuse or change a lesson's `id`, a chapter's `id` or a book's `id` once published: learning
progress on the phone is stored under `book/chapter/lesson`.

### Making lessons from an MCQ book

The owner uploads a PDF of MCQs into "YouTop books". For each question:

- The **stem and the correct answer** are the source: they decide what the picture must show and
  what the lesson teaches. Add from the book's explanation when it has one, otherwise from sound
  general knowledge.
- If the printed answer key is wrong beyond reasonable dispute, use the right answer and say so
  in `explain`. If it is debatable, keep the book's key.
- One lesson per question: `tracks` = an Arabic `story` and an English `explanation`; `quiz` = that
  question as printed, with `source` such as `"p. 42, Q17"`.
- Book `cover` = the book's real cover (first page of the PDF as an image).
- Pictures are portrait 9:16. The owner has them made (by ChatGPT) from a description you write
  per lesson, and uploads them to the book's folder. Then **look at each picture** and set every
  segment's `spot` (x, y, r as fractions of the picture) on the thing that sentence talks about.
  `tools/starter/art.mjs --check` shows how the starter lessons draw their spots on top of the
  picture to verify them; do the same for real pictures before publishing.
- Audio is optional. Without it the phone's own voice reads the text. If audio is added, each
  segment needs `t`, its start time in milliseconds.

## Decisions already made (do not reopen without the owner asking)

- Kotlin + Jetpack Compose, single activity, no login, no backend other than the Drive folder.
- Ratings are inferred, never asked: right answer = good (fast = easy), wrong = again, watched
  without answering = hard, quick swipe = nothing.
- Each lesson plays story → explanation → question → next lesson automatically. "Question first"
  in Library → Study skips straight to the question.
- The app is for the owner's personal use and is meant to be as engaging as possible.
