package app.youtop.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// ---- Files in Google Drive ---------------------------------------------------

/** Index file in the root of the Drive folder: `catalog-<number>.json`. Highest number wins. */
@Serializable
data class Catalog(
    val books: List<BookRef> = emptyList(),
    /** Drive file id of the tag system (taxonomy.json); the copy built into the app is used without it. */
    val taxonomy: String? = null,
)

/** [file] is the Drive file id of the book's JSON. A new upload has a new id, which is how updates are detected. */
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
 * separate Drive file holding a [ChapterFile], so one changed chapter does not re-send the book.
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
 * Media references ([image], [Track.audio]) are `drive:<fileId>`, `asset:<path>` or an https URL.
 */
@Serializable
data class Lesson(
    val id: String,
    val title: String,
    val titleAr: String? = null,
    val image: String? = null,
    /** Tags from the tag system (content/taxonomy.txt), e.g. "cardiac/valve/aortic/stenosis", "d:indications". */
    val tags: List<String> = emptyList(),
    val tracks: List<Track> = emptyList(),
    val quiz: List<Question> = emptyList(),
)

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
data class Segment(val text: String, val t: Long? = null, val spot: Spot? = null)

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
)

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

fun Lesson.info() = LessonInfo(id, title, titleAr, image, tags, quiz.size, tracks.mapNotNull { it.audio })

data class FeedItem(val book: BookInfo, val chapter: ChapterInfo, val lesson: LessonInfo) {
    val key: String = "${book.id}/${chapter.id}/${lesson.id}"
}

fun List<BookInfo>.flatten(): List<FeedItem> =
    flatMap { b -> b.chapters.flatMap { c -> c.lessons.map { FeedItem(b, c, it) } } }

fun BookInfo.mediaRefs(): List<String> =
    listOfNotNull(cover) + chapters.flatMap { c -> c.lessons.flatMap { listOfNotNull(it.image) + it.audio } }

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
}

@Serializable
data class Facet(
    val id: String,
    val prefix: String = "",
    val title: String,
    val titleAr: String? = null,
    val nodes: List<TagNode> = emptyList(),
)

@Serializable
data class TagNode(val id: String, val title: String, val titleAr: String? = null, val children: List<TagNode> = emptyList())

val ContentJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
    coerceInputValues = true
}
