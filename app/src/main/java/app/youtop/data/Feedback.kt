package app.youtop.data

import kotlinx.serialization.Serializable
import java.net.URLEncoder

/**
 * The owner's word on one lesson, for the agent who makes the lessons: "good job" (keep doing this)
 * or "redo" (what is wrong, and what should change). Kept on the phone until it is sent; it is sent
 * as a GitHub issue on the app's repository, which the agent reads (see CLAUDE.md, "Lesson feedback").
 */
@Serializable
data class Note(
    /** [GOOD] or [REDO]. */
    val kind: String,
    /** `book/chapter/lesson`, the id the agent finds the lesson by. */
    val key: String,
    val title: String = "",
    /** For a redo: which parts are wrong, as the codes in [Parts]. */
    val parts: List<String> = emptyList(),
    /** For a redo: what should change, in the owner's words. */
    val text: String = "",
    /** Where in the lesson the owner was, e.g. "story, sentence 7 of 12". */
    val where: String = "",
    val at: Long = 0L,
)

object Feedback {
    const val GOOD = "good"
    const val REDO = "redo"

    /** The parts of a lesson the owner can mark wrong. The codes are what the agent reads; keep them stable. */
    val Parts = listOf("picture", "spotlight", "circles", "story", "voice", "explanation", "question", "answer", "tags", "symbols", "english", "other")

    const val REPO = "https://github.com/blsmohamme972-lab/YouTop"
    const val TITLE_PREFIX = "Lesson feedback"

    /** Long enough for many notes, short enough for every phone browser and for GitHub. */
    private const val MAX_URL = 7000

    /** Adds [note]; a newer note of the same kind on the same lesson replaces the older one. */
    fun add(pending: List<Note>, note: Note): List<Note> =
        pending.filterNot { it.key == note.key && it.kind == note.kind } + note

    fun body(notes: List<Note>, version: String): String = buildString {
        append("Sent from the YouTop app ").append(version).append(". For the agent: CLAUDE.md, \"Lesson feedback from the app\".\n")
        notes.sortedBy { if (it.kind == REDO) 0 else 1 }.forEach { n ->
            append("\n### ").append(if (n.kind == REDO) "REDO" else "GOOD").append(' ').append(n.key).append('\n')
            if (n.title.isNotBlank()) append("Title: ").append(n.title.oneLine()).append('\n')
            if (n.parts.isNotEmpty()) append("Wrong: ").append(n.parts.joinToString(", ")).append('\n')
            if (n.where.isNotBlank()) append("Where: ").append(n.where.oneLine()).append('\n')
            if (n.text.isNotBlank()) append("Note: ").append(n.text.trim().replace("\n", "\n> ").let { "\n> $it" }).append('\n')
        }
    }

    fun title(notes: List<Note>): String {
        val redo = notes.count { it.kind == REDO }
        val good = notes.count { it.kind == GOOD }
        return "$TITLE_PREFIX: " + listOfNotNull(
            if (redo > 0) "$redo redo" else null,
            if (good > 0) "$good good job" else null,
        ).joinToString(", ")
    }

    /**
     * The address of a new GitHub issue holding as many of [pending] as fit (oldest first), and the
     * notes it holds; the rest wait for the next send. Null when nothing is waiting.
     */
    fun issue(pending: List<Note>, version: String): Pair<String, List<Note>>? {
        if (pending.isEmpty()) return null
        var take = pending.size
        while (take > 1 && url(pending.take(take), version).length > MAX_URL) take--
        val sent = pending.take(take)
        return url(sent, version) to sent
    }

    private fun url(notes: List<Note>, version: String) =
        "$REPO/issues/new?title=${enc(title(notes))}&body=${enc(body(notes, version))}"

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8").replace("+", "%20")

    private fun String.oneLine() = replace(Regex("\\s+"), " ").trim()
}
