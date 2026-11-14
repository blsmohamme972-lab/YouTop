package app.youtop.study

/**
 * Tags are strings. Topic tags are paths ("cardiac/aortic-valve"); the other groups
 * carry a prefix ("d:indications", "q:vignette", "l:2", "x:high-yield").
 */
object Tags {
    /** Group a tag belongs to: the prefix before ":", or "topic" for plain paths. */
    fun facet(tag: String): String = if (':' in tag) tag.substringBefore(':') else "topic"

    /** True when [tag] is [selected] itself or lies below it in the tree. */
    fun under(tag: String, selected: String) = tag == selected || tag.startsWith("$selected/")

    /**
     * A lesson is in focus when, for every group that has something selected, it carries at least
     * one tag at or below a selected one. An empty focus matches everything.
     */
    fun matches(tags: List<String>, focus: Set<String>): Boolean {
        if (focus.isEmpty()) return true
        return focus.groupBy(::facet).all { (_, wanted) -> tags.any { t -> wanted.any { under(t, it) } } }
    }

    /** The tag and all its ancestors: "a/b/c" gives "a", "a/b", "a/b/c". */
    fun withAncestors(tag: String): List<String> {
        val prefix = if (':' in tag) tag.substringBefore(':') + ":" else ""
        val parts = tag.removePrefix(prefix).split('/')
        return parts.indices.map { i -> prefix + parts.subList(0, i + 1).joinToString("/") }
    }

    /** The most specific topic tag of a lesson, if it has one. */
    fun mainTopic(tags: List<String>): String? = tags.filter { ':' !in it }.maxByOrNull { it.count { c -> c == '/' } }

    data class Stat(var lessons: Int = 0, var due: Int = 0, var seen: Int = 0, var reps: Int = 0, var lapses: Int = 0) {
        /** Share of reviews answered wrongly; null until there is enough to go on. */
        val weakness: Float? get() = if (reps >= 3) lapses.toFloat() / reps else null
    }

    /** Totals for every tag (each lesson also counts towards its tags' ancestors). */
    fun stats(lessons: List<Pair<String, List<String>>>, cards: Map<String, CardState>, now: Long): Map<String, Stat> {
        val out = HashMap<String, Stat>()
        for ((key, tags) in lessons) {
            val card = cards[key]
            val touched = tags.flatMapTo(HashSet()) { withAncestors(it) }
            for (tag in touched) {
                val s = out.getOrPut(tag) { Stat() }
                s.lessons++
                if (card != null && card.phase != Phase.NEW) {
                    s.seen++
                    s.reps += card.reps
                    s.lapses += card.lapses
                    if (card.due <= now) s.due++
                }
            }
        }
        return out
    }
}
