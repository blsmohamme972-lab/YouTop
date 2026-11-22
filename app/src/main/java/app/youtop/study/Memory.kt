package app.youtop.study

import kotlinx.serialization.Serializable
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * The memory model behind the smart scheduler: FSRS-5 (the "Free Spaced Repetition Scheduler",
 * the model Anki now offers), with its published default weights.
 *
 * Every lesson has two numbers:
 *  - stability: how many days until the chance of remembering it drops to 90%;
 *  - difficulty: 1 (easy) to 10 (hard), which decides how fast stability grows.
 * From them the model says how likely the lesson is remembered after any number of days
 * ([retrievability]) and so when it has to come back to be remembered with a wanted
 * probability ([interval]). Pure functions, no Android.
 */
object Memory {
    private val W = floatArrayOf(
        0.40255f, 1.18385f, 3.173f, 15.69105f, // stability after a first answer: again, hard, good, easy
        7.1949f, 0.5345f,                      // first difficulty
        1.4604f, 0.0046f,                      // how difficulty moves, and its pull back to the middle
        1.54575f, 0.1192f, 1.01925f,           // growth of stability after a right answer
        1.9395f, 0.11f, 0.29605f, 2.2698f,     // stability after a forgotten lesson
        0.2315f, 2.9898f,                      // "hard" penalty, "easy" bonus
        0.51655f, 0.6621f,                     // reviews on the same day
    )
    private const val DECAY = -0.5f
    private const val FACTOR = 19f / 81f
    private const val MIN_STABILITY = 0.01f
    private const val MAX_STABILITY = 36_500f

    /** Chance (0..1) of remembering after [days] with the given [stability]. */
    fun retrievability(days: Float, stability: Float): Float =
        if (stability <= 0f) 0f else (1f + FACTOR * max(0f, days) / stability).pow(DECAY)

    /** Days after which the chance of remembering has fallen to [retention]. At 90% this is the stability itself. */
    fun interval(stability: Float, retention: Float): Float =
        stability / FACTOR * (retention.coerceIn(0.5f, 0.99f).pow(1f / DECAY) - 1f)

    private fun grade(rating: Rating) = rating.ordinal + 1 // again 1 … easy 4

    private fun firstDifficulty(g: Int) = W[4] - exp(W[5] * (g - 1)) + 1f

    fun firstStability(rating: Rating): Float = W[grade(rating) - 1]

    fun firstDifficulty(rating: Rating): Float = firstDifficulty(grade(rating)).coerceIn(1f, 10f)

    private fun nextDifficulty(d: Float, g: Int): Float {
        val moved = d - W[6] * (g - 3) * (10f - d) / 9f       // less movement near the hard end
        return (W[7] * firstDifficulty(4) + (1f - W[7]) * moved).coerceIn(1f, 10f)
    }

    /** State of a lesson: [s] stability in days, [d] difficulty. */
    data class State(val s: Float, val d: Float)

    /** First answer to a lesson. */
    fun first(rating: Rating) = State(firstStability(rating), firstDifficulty(rating))

    /**
     * The state after an answer given [days] after the previous one.
     * Within the same day (learning steps) stability only shifts a little; after a real wait it
     * grows on a right answer, the more so the closer the lesson was to being forgotten, and
     * collapses on a wrong one.
     */
    fun next(state: State, rating: Rating, days: Float): State {
        val g = grade(rating)
        val s = state.s.coerceIn(MIN_STABILITY, MAX_STABILITY)
        val d = state.d.coerceIn(1f, 10f)
        val grown = if (days < SAME_DAY) {
            val shifted = s * exp(W[17] * (g - 3 + W[18]))
            if (g >= 3) max(s, shifted) else shifted
        } else {
            val r = retrievability(days, s)
            if (g == 1) {
                min(s, W[11] * d.pow(-W[12]) * ((s + 1f).pow(W[13]) - 1f) * exp(W[14] * (1f - r)))
            } else {
                val bonus = when (g) { 2 -> W[15]; 4 -> W[16]; else -> 1f }
                s * (1f + exp(W[8]) * (11f - d) * s.pow(-W[9]) * (exp(W[10] * (1f - r)) - 1f) * bonus)
            }
        }
        return State(grown.coerceIn(MIN_STABILITY, MAX_STABILITY), nextDifficulty(d, g))
    }

    /** Answers closer together than this count as the same sitting. */
    const val SAME_DAY = 0.5f

    /**
     * A lesson learned under the classic scheduler, carried over: its interval is taken as its
     * stability and its ease becomes a difficulty (the conversion Anki uses).
     */
    fun fromClassic(intervalDays: Float, ease: Float): State {
        if (intervalDays <= 0f) return State(firstStability(Rating.HARD), firstDifficulty(Rating.GOOD))
        val s = intervalDays.coerceIn(MIN_STABILITY, MAX_STABILITY)
        val d = 11f - (ease - 1f) / (exp(W[8]) * s.pow(-W[9]) * (exp(0.1f * W[10]) - 1f))
        return State(s, d.coerceIn(1f, 10f))
    }

    /**
     * How the learner's real memory compares with the model, from their own reviews: 1 when they
     * remember as often as predicted, above 1 when they remember more (intervals can stretch),
     * below 1 when they forget more (intervals shrink). Null until there is enough to go on.
     */
    fun personalFactor(c: Calibration): Float? {
        if (c.n < Calibration.ENOUGH) return null
        val predicted = (c.predicted / c.n).coerceIn(0.5f, 0.99f)
        val actual = (c.right / c.n).coerceIn(0.5f, 0.99f)
        // For the same wait, stability is proportional to 1 / (R^(1/decay) - 1).
        val k = (predicted.pow(1f / DECAY) - 1f) / (actual.pow(1f / DECAY) - 1f)
        return k.coerceIn(0.6f, 1.6f)
    }
}

/**
 * A running tally of reviews made after a real wait: how often the model expected the lesson to be
 * remembered, and how often it was. Old reviews fade out, so it follows the learner as they change.
 */
@Serializable
data class Calibration(val n: Float = 0f, val predicted: Float = 0f, val right: Float = 0f) {
    fun plus(expected: Float, remembered: Boolean) =
        Calibration(n * FADE + 1f, predicted * FADE + expected, right * FADE + if (remembered) 1f else 0f)

    /** Share of reviews actually remembered, or null with too few. */
    val measured: Float? get() = if (n >= ENOUGH) right / n else null

    companion object {
        const val FADE = 0.995f   // about the last 200 reviews
        const val ENOUGH = 40f
    }
}
