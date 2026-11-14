package app.youtop.study

import kotlinx.serialization.Serializable
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/** Everything the learner can tune. Times are minutes for steps and days for intervals. */
@Serializable
data class StudySettings(
    // Daily load
    val newPerDay: Int = 20,
    val dailyGoal: Int = 30,
    val reviewsPerNew: Int = 3,
    val randomNewOrder: Boolean = false,
    /** What the feed shows once nothing is due: "more" new lessons, review "ahead", or "stop". */
    val afterGoal: String = "more",
    // Learning
    val learningSteps: List<Int> = listOf(1, 10),
    val relearningSteps: List<Int> = listOf(10),
    val graduatingDays: Float = 1f,
    val easyDays: Float = 4f,
    // Reviews
    val startingEase: Float = 2.5f,
    val easyBonus: Float = 1.3f,
    val hardFactor: Float = 1.2f,
    val intervalModifier: Float = 1f,
    /** Share of the old interval kept after a wrong answer (0 = start again from one day). */
    val lapseFactor: Float = 0f,
    val maxDays: Float = 365f,
    // Feed behaviour
    val autoContinue: Boolean = true,
    val autoAdvance: Boolean = true,
    val shuffleOptions: Boolean = true,
    val speakQuiz: Boolean = false,
    val excludedBooks: Set<String> = emptySet(),
) {
    companion object {
        val Relaxed = StudySettings(newPerDay = 10, dailyGoal = 15)
        val Standard = StudySettings()
        /** Short intervals and a heavy daily load, for the weeks before an exam. */
        val Intensive = StudySettings(
            newPerDay = 60, dailyGoal = 100, reviewsPerNew = 2, learningSteps = listOf(1, 5, 20),
            relearningSteps = listOf(5, 20), easyDays = 3f, intervalModifier = 0.7f, maxDays = 30f,
        )
    }
}

enum class Phase { NEW, LEARNING, REVIEW, RELEARNING }

enum class Rating { AGAIN, HARD, GOOD, EASY }

/** Memory state of one lesson. [interval] is in days, [due] and [seen] are epoch milliseconds. */
@Serializable
data class CardState(
    val phase: Phase = Phase.NEW,
    val step: Int = 0,
    val interval: Float = 0f,
    val ease: Float = 2.5f,
    val due: Long = 0L,
    val reps: Int = 0,
    val lapses: Int = 0,
    val seen: Long = 0L,
)

/** Spaced repetition in the style of Anki's SM-2 scheduler. Pure functions, no Android. */
object Scheduler {
    private const val MINUTE = 60_000L
    private const val DAY = 86_400_000L
    private const val MIN_EASE = 1.3f

    fun review(card: CardState, rating: Rating, now: Long, s: StudySettings, fuzz: Float = 1f): CardState {
        val base = card.copy(reps = card.reps + 1, seen = now)
        return when (card.phase) {
            Phase.NEW, Phase.LEARNING -> learn(
                base.copy(ease = if (card.phase == Phase.NEW) s.startingEase else card.ease),
                rating, now, s, s.learningSteps, Phase.LEARNING,
            )
            Phase.RELEARNING -> learn(base, rating, now, s, s.relearningSteps, Phase.RELEARNING)
            Phase.REVIEW -> reviewDue(base, rating, now, s, fuzz)
        }
    }

    private fun learn(card: CardState, rating: Rating, now: Long, s: StudySettings, steps: List<Int>, phase: Phase): CardState {
        fun graduate(days: Float): CardState {
            // A relearned lesson keeps the interval it was given when it lapsed.
            val interval = if (phase == Phase.RELEARNING) max(1f, card.interval) else days
            return card.copy(phase = Phase.REVIEW, step = 0, interval = cap(interval, s), due = now + days(cap(interval, s)))
        }
        fun at(step: Int) = card.copy(phase = phase, step = step, due = now + steps[step] * MINUTE)
        if (steps.isEmpty()) return graduate(if (rating == Rating.EASY) s.easyDays else s.graduatingDays)
        val step = card.step.coerceIn(0, steps.lastIndex)
        return when (rating) {
            Rating.AGAIN -> at(0)
            Rating.HARD -> at(step)
            Rating.GOOD -> if (step + 1 >= steps.size) graduate(s.graduatingDays) else at(step + 1)
            Rating.EASY -> graduate(s.easyDays)
        }
    }

    private fun reviewDue(card: CardState, rating: Rating, now: Long, s: StudySettings, fuzz: Float): CardState {
        // Days actually waited: more than planned when overdue, fewer when reviewed ahead of time.
        val waited = max(0f, card.interval + (now - card.due).toFloat() / DAY)
        val late = max(0f, waited - card.interval)
        if (rating == Rating.AGAIN) {
            val interval = cap(max(1f, card.interval * s.lapseFactor), s)
            val lapsed = card.copy(ease = max(MIN_EASE, card.ease - 0.2f), lapses = card.lapses + 1, interval = interval)
            return if (s.relearningSteps.isEmpty()) {
                lapsed.copy(due = now + days(interval))
            } else {
                lapsed.copy(phase = Phase.RELEARNING, step = 0, due = now + s.relearningSteps[0] * MINUTE)
            }
        }
        val (ease, grown) = when (rating) {
            Rating.HARD -> max(MIN_EASE, card.ease - 0.15f) to min(waited, card.interval) * s.hardFactor
            Rating.EASY -> (card.ease + 0.15f) to (card.interval + late) * card.ease * s.easyBonus
            else -> card.ease to (card.interval + late / 2f) * card.ease
        }
        // Reviewed early: credit only the time that really passed, and never shrink the interval.
        val early = waited < card.interval
        val target = if (early) max(card.interval, waited * ease) else max(card.interval + 1f, grown * s.intervalModifier * fuzz)
        val interval = cap(target, s)
        return card.copy(ease = ease, interval = interval, due = now + days(interval))
    }

    private fun cap(days: Float, s: StudySettings) = days.coerceIn(1f, max(1f, s.maxDays))
    private fun days(d: Float) = (d * DAY).toLong()

    /**
     * Chooses what the feed shows next, or null when the day is finished and the learner chose to stop.
     *
     * @param keys      every lesson that may appear, in book order
     * @param recent    lessons shown most recently (newest last) or already queued; not repeated straight away
     * @param newToday  new lessons already started today
     * @param sinceNew  reviews shown since the last new lesson
     */
    fun next(
        keys: List<String>,
        cards: Map<String, CardState>,
        recent: List<String>,
        newToday: Int,
        sinceNew: Int,
        now: Long,
        s: StudySettings,
        random: Random = Random.Default,
    ): String? {
        if (keys.isEmpty()) return null
        val blocked = recent.takeLast(min(3, keys.size - 1)).toSet()
        val open = keys.filter { it !in blocked }
        fun phase(k: String) = cards[k]?.phase ?: Phase.NEW
        fun due(k: String) = cards[k]?.due ?: 0L

        val learning = open.filter { phase(it) == Phase.LEARNING || phase(it) == Phase.RELEARNING }
        learning.filter { due(it) <= now }.minByOrNull(::due)?.let { return it }

        val reviews = open.filter { phase(it) == Phase.REVIEW && due(it) <= now }.sortedBy(::due)
        val fresh = open.filter { phase(it) == Phase.NEW }
        fun pickNew() = if (s.randomNewOrder) fresh.random(random) else fresh.first()
        val mayStartNew = fresh.isNotEmpty() && newToday < s.newPerDay
        if (mayStartNew && (reviews.isEmpty() || sinceNew >= s.reviewsPerNew)) return pickNew()
        reviews.firstOrNull()?.let { return it }

        // Nothing is due. Lessons still being learned come back a little early rather than leaving a gap.
        learning.minByOrNull(::due)?.let { return it }
        if (s.afterGoal == "stop") return null
        return when (s.afterGoal) {
            "ahead" -> open.filter { phase(it) == Phase.REVIEW }.minByOrNull(::due) ?: fresh.firstOrNull()?.let { pickNew() }
            else -> fresh.firstOrNull()?.let { pickNew() } ?: open.filter { phase(it) == Phase.REVIEW }.minByOrNull(::due)
        } ?: open.firstOrNull()
    }

    /**
     * Turns what the learner did with a lesson into a rating, so no "how well did you know this?"
     * buttons are needed. Returns null when the lesson was skipped and should simply come back later.
     */
    fun rate(answered: Int, wrong: Int, questions: Int, avgAnswerMs: Long, finishedNarration: Boolean, dwellMs: Long): Rating? = when {
        answered > 0 && wrong == 0 && answered >= questions -> if (avgAnswerMs in 1..4_000) Rating.EASY else Rating.GOOD
        answered > 0 && wrong == 0 -> Rating.HARD
        answered > 0 -> if (answered > 1 && wrong * 2 < answered) Rating.HARD else Rating.AGAIN
        dwellMs < 4_000 -> null
        finishedNarration -> if (questions == 0) Rating.GOOD else Rating.HARD
        else -> null
    }

    /** Day number that rolls over at 04:00 local time, like Anki, so late-night study counts for the same day. */
    fun dayOf(now: Long, zoneOffsetMs: Int): Long = Math.floorDiv(now + zoneOffsetMs - 4 * 3_600_000L, DAY)
}
