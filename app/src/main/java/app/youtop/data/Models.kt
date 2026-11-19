package app.youtop.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// ---- Published files (GitHub `content` branch, or Google Drive before 1.7) ----

/**
 * Index of all books: `catalog.json` on GitHub, or `catalog-<number>.json` in the root of the
 * Drive folder (highest number wins).
 */
@Serializable
data class Catalog(
    val books: List<BookRef> = emptyList(),
    /** `gh:<path>` or Drive file id of the tag system (taxonomy.json); the copy built into the app is used without it. */
    val taxonomy: String? = null,
)

/**
 * [file] is `gh:<path>` (the path carries a hash of the content) or the Drive file id of the
 * book's JSON. A changed book has a new path or id, which is how updates are detected.
 */
@Serializable
data class BookRef(val id: String, val file: String)

@Serializable
data class Book(
    val id: String,
    val title: String,
    val titleAr: String? = null,
    val description: String? = null,
    /** Cover picture (the book's own cover), same reference format as lesson images. */
    val cover: String? = null,
    val chapters: List<Chapter> = emptyList(),
)

/**
 * A chapter either carries its lessons inline, or (for big books) points with [file] at a
 * separate file (`gh:<path>` or a Drive file id) holding a [ChapterFile], so one changed chapter does not re-send the book.
 */
@Serializable
data class Chapter(
    val id: String,
    val title: String,
    val titleAr: String? = null,
    val lessons: List<Lesson> = emptyList(),
    val file: String? = null,
)

@Serializable
data class ChapterFile(val lessons: List<Lesson> = emptyList())

/**
 * One fullscreen "short".
 * Media references ([image], [Track.audio]) are `gh:<path>`, `drive:<fileId>`,
 * `asset:<path>` or an https URL.
 */
@Serializable
data class Lesson(
    val id: String,
    val title: String,
    val titleAr: String? = null,
    val image: String? = null,
    /** Tags from the tag system (content/taxonomy.txt), e.g. "cardiac/aortic-valve", "d:indications". */
    val tags: List<String> = emptyList(),
    val tracks: List<Track> = emptyList(),
    val quiz: List<Question> = emptyList(),
    /**
     * The picture's numbered circles, for "all circles" and for the question: a circle's number is
     * its place in this list, starting at 1. Not every [Segment.spot] is one, and several sentences
     * may share one.
     */
    val marks: List<Mark> = emptyList(),
)

/**
 * One numbered circle on the picture: [what] is the character, object or event inside it,
 * [means] the medical fact it stands for. Both are English.
 */
@Serializable
data class Mark(val spot: Spot? = null, val what: String = "", val means: String = "")

/** A narration: `kind` is "story" or "explanation", `lang` a language code such as "ar" or "en". */
@Serializable
data class Track(
    val kind: String = "story",
    val lang: String = "ar",
    val audio: String? = null,
    val segments: List<Segment> = emptyList(),
)

/**
 * One sentence of narration. [t] is where it starts in the audio file (milliseconds);
 * [spot] is what the camera points at while it is spoken.
 */
@Serializable
data class Segment(
    val text: String,
    val t: Long? = null,
    val spot: Spot? = null,
    /** Set when [text] is a quotation: where it is from, e.g. "دوستويفسكي، الجريمة والعقاب". Shown under it. */
    val quote: String? = null,
)

/** Region of the image: centre [x],[y] as fractions of image width/height, radius [r] as a fraction of image width. */
@Serializable
data class Spot(val x: Float, val y: Float, val r: Float = 0.15f)

@Serializable
data class Question(
    val q: String,
    val options: List<String>,
    val answer: Int,
    val explain: String = "",
    val lang: String = "en",
    /** Where the question comes from, e.g. "p. 42, Q17". Shown small under the question. */
    val source: String? = null,
    /** A figure the stem refers to ("the ECG shown below"), same reference format as lesson images. */
    val image: String? = null,
    /** Numbers of the lesson's [Lesson.marks] shown while this question is asked: what it is about, never the answer. */
    val marks: List<Int> = emptyList(),
    /** Numbers of the marks that stand for the correct answer; they appear once the question is answered. */
    val answerMarks: List<Int> = emptyList(),
    /**
     * Every correct option when more than one is right ("choose all that apply"); [answer] is then the
     * first of them, so content and apps from before this field still read a valid single answer.
     */
    val answers: List<Int> = emptyList(),
)

/** The options that are right: several for a "choose all that apply" question, otherwise just [Question.answer]. */
val Question.correct: Set<Int> get() = if (answers.size > 1) answers.toSet() else setOf(answer)

/** True when the learner must tick every right option and then check. */
val Question.multi: Boolean get() = correct.size > 1

// ---- What the app keeps in memory ------------------------------------------
// With thousands of lessons the full text stays on disk. Only this light index is loaded;
// a lesson's narration and questions are read from its chapter when it is about to be shown.

@Serializable
data class LessonInfo(
    val id: String,
    val title: String,
    val titleAr: String? = null,
    val image: String? = null,
    val tags: List<String> = emptyList(),
    val questions: Int = 0,
    val audio: List<String> = emptyList(),
    /** Figures of the lesson's questions, so they are downloaded and kept with the lesson. */
    val figures: List<String> = emptyList(),
)

/** [source] says where the full lessons are: `asset:<path>`, `book:<fileId>` or `chapter:<fileId>`. */
@Serializable
data class ChapterInfo(
    val id: String,
    val title: String,
    val titleAr: String? = null,
    val source: String,
    val lessons: List<LessonInfo> = emptyList(),
)

@Serializable
data class BookInfo(
    val id: String,
    val title: String,
    val titleAr: String? = null,
    val description: String? = null,
    val cover: String? = null,
    val chapters: List<ChapterInfo> = emptyList(),
)

@Serializable
data class LibraryIndex(val books: List<BookInfo> = emptyList())

/** A question the quiz can show: at least two options and an answer (or answers) among them. */
fun Question.isUsable() = options.size >= 2 && answer in options.indices && q.isNotBlank() &&
    answers.all { it in options.indices } && (answers.isEmpty() || answer in answers)

/**
 * A lesson as the app shows it: questions that cannot be asked are dropped rather than crash the
 * quiz, and a narration without sentences is dropped rather than play nothing.
 */
fun Lesson.clean(): Lesson {
    val okQuiz = quiz.filter { it.isUsable() }
    val okTracks = tracks.filter { it.segments.isNotEmpty() || it.audio != null }
    return if (okQuiz.size == quiz.size && okTracks.size == tracks.size) this else copy(quiz = okQuiz, tracks = okTracks)
}

/** Books, chapters and lessons are found by id: of two with the same id only the first is kept. */
fun BookInfo.withoutDuplicates(): BookInfo {
    val seen = HashSet<String>()
    return copy(chapters = chapters.distinctBy { it.id }.map { c ->
        // A lesson id must be unique in its chapter (it is the key of the learner's progress).
        seen.clear()
        if (c.lessons.all { seen.add(it.id) }) c else c.copy(lessons = c.lessons.distinctBy { it.id })
    })
}

fun Lesson.info() = clean().run { infoOf() }

private fun Lesson.infoOf() = LessonInfo(id, title, titleAr, image, tags, quiz.size, tracks.mapNotNull { it.audio }, quiz.mapNotNull { it.image })

data class FeedItem(val book: BookInfo, val chapter: ChapterInfo, val lesson: LessonInfo) {
    val key: String = "${book.id}/${chapter.id}/${lesson.id}"
}

fun List<BookInfo>.flatten(): List<FeedItem> =
    flatMap { b -> b.chapters.flatMap { c -> c.lessons.map { FeedItem(b, c, it) } } }

fun BookInfo.mediaRefs(): List<String> =
    listOfNotNull(cover) + chapters.flatMap { c -> c.lessons.flatMap { listOfNotNull(it.image) + it.audio + it.figures } }

// ---- Tag system --------------------------------------------------------------

@Serializable
data class Taxonomy(val version: Int = 1, val facets: List<Facet> = emptyList()) {
    /** Title of every tag, by tag id. */
    val titles: Map<String, String> by lazy {
        HashMap<String, String>().also { map ->
            fun walk(nodes: List<TagNode>) { for (n in nodes) { map[n.id] = n.title; walk(n.children) } }
            facets.forEach { walk(it.nodes) }
        }
    }

    /** Arabic title of every tag that has one. */
    val titlesAr: Map<String, String> by lazy {
        HashMap<String, String>().also { map ->
            fun walk(nodes: List<TagNode>) { for (n in nodes) { n.titleAr?.let { map[n.id] = it }; walk(n.children) } }
            facets.forEach { walk(it.nodes) }
        }
    }

    /** What a tag is called on a phone set to Arabic ([arabic]) or to anything else; null for an unknown tag. */
    fun label(tag: String, arabic: Boolean): String? = (if (arabic) titlesAr[tag] else null) ?: titles[tag]
}

@Serializable
data class Facet(
    val id: String,
    val prefix: String = "",
    val title: String,
    val titleAr: String? = null,
    val nodes: List<TagNode> = emptyList(),
) {
    fun label(arabic: Boolean) = (if (arabic) titleAr else null) ?: title
}

@Serializable
data class TagNode(
    val id: String,
    val title: String,
    val titleAr: String? = null,
    val children: List<TagNode> = emptyList(),
    /** A cover picture (same reference format as lesson images); without it the one built into the app is used. */
    val cover: String? = null,
) {
    fun label(arabic: Boolean) = (if (arabic) titleAr else null) ?: title
}

val ContentJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
    coerceInputValues = true
}
