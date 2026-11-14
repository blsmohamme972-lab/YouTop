# YouTop

Android app for short, fullscreen lessons: a picture, a narrated Arabic story, an English
explanation and a quiz. A "camera" moves over the picture and highlights what the narration
is talking about. Lessons live in a Google Drive folder, so new content appears in the app
without rebuilding it.

## Build

    ./gradlew :app:assembleRelease      # app/build/outputs/apk/release/app-release.apk
    ./gradlew :app:testDebugUnitTest    # tests; also writes pictures of each screen to app/build/screens

Needs JDK 17+ and the Android SDK (platform 36). The APK is signed with the prototype key in
`keystore/`, so every build can be installed over the previous one. Replace it before a store release.

## How content gets into the app

The app reads one Drive folder, **YouTop books**, shared as "Anyone with the link – Viewer".
The folder id is set in `gradle.properties` (`youtop.driveFolder`) and can be changed in the app
under Library → Sync.

    YouTop books/
      catalog-7.json            index of all books; the highest number is the one in use
      <Book name>/
        book.json               chapters, lessons, narration text, spotlight cues, quizzes
        *.webp / *.jpg          lesson pictures (portrait, 9:16 works best)
        *.mp3 / *.m4a           narration audio (optional)

On sync the app lists the folder, opens the newest catalog, downloads any book file it has not
seen and then the pictures and audio, and keeps everything on the device for offline use.

Drive cannot change a file's content in place through the tools used here, so an update is always
a **new file**: upload the new `book.json`, then upload `catalog-<n+1>.json` pointing at it. Old
files can be deleted afterwards.

### catalog-N.json

    { "books": [ { "id": "starter", "file": "<Drive file id of book.json>" } ] }

### book.json

    {
      "id": "starter", "title": "YouTop Starter", "titleAr": "…",
      "chapters": [ { "id": "examples", "title": "Examples", "lessons": [ {
        "id": "how-light-travels", "title": "How Light Travels", "titleAr": "كيف يسافر الضوء",
        "image": "drive:<file id>",
        "tracks": [
          { "kind": "story", "lang": "ar", "audio": "drive:<file id>",
            "segments": [
              { "text": "…", "t": 0 },
              { "text": "…", "t": 5200, "spot": { "x": 0.21, "y": 0.15, "r": 0.16 } } ] },
          { "kind": "explanation", "lang": "en", "segments": [ … ] } ],
        "quiz": [ { "q": "…", "options": ["…", "…"], "answer": 0, "explain": "…", "lang": "en" } ]
      } ] } ]
    }

- `segments` are the sentences of the narration, in order. The current one is shown as the caption.
- `spot` is the region to highlight while that sentence is spoken: centre `x`,`y` as fractions of
  the picture's width and height (0–1, from the top-left), radius `r` as a fraction of its width.
  Leave it out to show the whole picture.
- `t` is where the sentence starts in the audio file, in milliseconds. Without `audio` the device's
  own voice reads each sentence and `t` is not needed.
- `answer` is the index of the correct option, counting from 0.

## Starter book

`tools/starter/build_book.py` holds the text of the three example lessons and writes
`tools/starter/book.json`. `tools/starter/art.mjs <dir> --check` draws their placeholder pictures
and, with `--check`, a copy with every spotlight region outlined, to confirm the cues point at the
right things. The same book is bundled in the APK (`app/src/main/assets/seed`) so the app has
content before its first sync.
