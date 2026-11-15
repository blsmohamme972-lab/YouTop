# YouTop content

This branch holds the lessons the YouTop app reads. It has no app code: that is on `main`.

Do not edit files here by hand. Publish with `tools/content/publish.py` from `main`
(see `docs/content-guide.md` there): book and chapter files carry a hash of their content in
their name, and `catalog.json` must point at the current ones.

    catalog.json                              the books in use
    books/<book id>/book-<hash>.json          a book and its chapters
    books/<book id>/chapters/<name>-<hash>.json   about 100 lessons each
    books/<book id>/media/                    pictures, question figures, recorded narration
    work/                                     work files of each batch (stories, spots, scripts)
