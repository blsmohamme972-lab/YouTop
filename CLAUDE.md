# YouTop — guide for whoever works on this next

Read this first. It is written for an agent (or person) with no prior knowledge of the project.
`README.md` describes the app and the content format in more detail.

## What this is

A personal Android app owned by one person, used on their own phone. It shows fullscreen lessons
you swipe through like short videos: a picture, a narrated story (Arabic), an explanation
(English), then a multiple-choice question over the picture. A spaced-repetition scheduler decides
what comes next. Lessons are made from MCQ books and live in Google Drive, so content changes
never need a new APK. The library is planned to pass 5,000 questions in cardiac, thoracic and
vascular surgery, each tagged from a small tag system (about 55 tags).

The owner is not a programmer and uses a phone. They keep nothing themselves: no key, no
password, no notes. **Everything you need is in this repository or in their Google Drive, as
listed below. Do not ask them for keys, secrets or files; find them there.** Explain things
plainly, send them the APK file itself, and never ask them to run commands.

## Where everything is

| What | Where |
|---|---|
| Code | GitHub `blsmohamme972-lab/YouTop` (public), branch `main`. Do new work on a branch, then merge it into `main` (pull request) so `main` always holds what the owner's phone runs. |
| Lesson content | Google Drive account `gpmo592@gmail.com`, folder **YouTop books**, id `1r5301GkHo5hZItw4gSwTUzrBD6HFcHUI`. Shared as "anyone with the link – viewer" on purpose: the app reads it without signing in. |
| Signing key | Same Drive account, folder **YouTop private**, id `1QQ4CyHwy6VjuYfYRobermLpT5BqLQVqb`, file `youtop-signing.txt` (id `16CYev5pmNzRKGAaLNilaSkfXNq1bFvpL`). Private. Read it with the Google Drive connector. |
| Source PDFs, work status | **YouTop private/source books** and the highest-numbered `status-<n>.txt` in YouTop private. |
| Tag system | `content/taxonomy.txt` in this repository. |
| How to make lessons | `docs/content-guide.md`. |

If the Google Drive connector is missing in your session, or is signed in to a different account,
that is the one thing only the owner can fix: ask them to connect Google Drive as
`gpmo592@gmail.com` in Claude's connector settings. Everything else you can do yourself.

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
- `ScaleTest` — syncs a made-up 5,500-lesson book and fails if anything done per swipe gets slow.
- `ContentTest` — the bundled starter book and the Drive folder parser.
- `ScreensTest` — starts the real activity under Robolectric, taps through it and saves a picture
  of each screen to `app/build/screens/`. **Look at those pictures** after any UI change; it is the
  only way to see the app. Add a step there for any new screen.

The bundled starter book has no quotation and no question figure. To see real lessons, put a
book file and its pictures under `app/src/main/assets/seed/` for a moment (pictures as
`asset:seed/...`), drive it from a throw-away test, look, and delete both again. **Never commit
book text**: the repository is public.

Say plainly to the owner that the app was not run on a real phone, and ask for a screenshot if
something looks wrong on theirs.

### Map of the code (`app/src/main/java/app/youtop/`)

- `data/Models.kt` — the content format (Book → Chapter → Lesson → Track/Segment/Spot, Question).
- `data/DriveClient.kt`, `ContentRepository.kt`, `MediaCache.kt` — sync from Drive, offline cache.
  Only a light index of all lessons is kept in memory; a lesson's text is read from its chapter
  file when it is about to be shown. Keep it that way: the library will hold thousands.
- `study/Tags.kt` — tag matching ("focus") and per-tag statistics.
- `study/Scheduler.kt` — spaced repetition and "what to show next" (pure Kotlin, well tested).
  `study/Progress.kt` — stores each lesson's memory state on the phone.
- `player/Narrator.kt` — plays a narration from an audio file, or the phone's voice, or silently.
- `MainViewModel.kt` — the feed queue and everything the screens call.
- `ui/Theme.kt` (colours and the two typefaces: IBM Plex Sans Arabic for the interface, Amiri for
  stories and quotations; both under the Open Font License, texts in `assets/licenses`).
- `ui/FeedScreen.kt` (the feed and one lesson), `Spotlight.kt` (spotlight on the whole picture, never zoomed),
  `QuizOverlay.kt` (question over the picture), `BooksTab.kt`, `TopicsTab.kt` (tag tree and focus),
  `StudyTab.kt`, `LibraryScreen.kt`.

Keep new fields in `Models.kt` optional with defaults, so older content keeps loading, and keep
`StudySettings` / `CardState` backwards compatible, so stored progress keeps loading.

## Updating content (no APK needed)

Follow `docs/content-guide.md`. In short: lessons are JSON files in "YouTop books"; the app reads
the highest-numbered `catalog-<n>.json`, which lists each book's `book.json`, which lists chapter
files of about 100 lessons. Drive files cannot be edited, only replaced, so every change is
"upload the new files, then upload the next catalog". Validate with
`python3 tools/content/validate.py` before publishing, and check the public links afterwards.

Never change a published book, chapter or lesson id: the owner's learning progress is stored
under `book/chapter/lesson` on their phone.

## Decisions already made (do not reopen without the owner asking)

- Kotlin + Jetpack Compose, single activity, no login, no backend other than the Drive folder.
- Ratings are inferred, never asked: right answer = good (fast = easy), wrong = again, watched
  without answering = hard, quick swipe = nothing.
- The picture is never zoomed or cropped; only a spotlight moves over it, in step with the voice.
- The Arabic story is a real story with no medical explanation in it; the science is in the
  English explanation (see `docs/content-guide.md`, "The story is a story").
- Each lesson plays story → explanation → question → next lesson automatically. "Question first"
  in Library → Study skips straight to the question.
- The app is for the owner's personal use and is meant to be as engaging as possible.
