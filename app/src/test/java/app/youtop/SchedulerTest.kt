package app.youtop

import app.youtop.study.CardState
import app.youtop.study.Phase
import app.youtop.study.Rating
import app.youtop.study.Scheduler
import app.youtop.study.StudySettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SchedulerTest {
    private val s = StudySettings()
    private val minute = 60_000L
    private val day = 86_400_000L
    private val t0 = 1_000 * day

    @Test
    fun newLessonWalksThroughLearningStepsThenGraduates() {
        var c = Scheduler.review(CardState(), Rating.GOOD, t0, s)
        assertEquals(Phase.LEARNING, c.phase)
        assertEquals(t0 + 10 * minute, c.due)
        c = Scheduler.review(c, Rating.GOOD, c.due, s)
        assertEquals(Phase.REVIEW, c.phase)
        assertEquals(1f, c.interval, 0.001f)
    }

    @Test
    fun wrongAnswerBringsANewLessonBackInAMinute() {
        val c = Scheduler.review(CardState(), Rating.AGAIN, t0, s)
        assertEquals(Phase.LEARNING, c.phase)
        assertEquals(t0 + minute, c.due)
    }

    @Test
    fun easyAnswerSkipsLearning() {
        val c = Scheduler.review(CardState(), Rating.EASY, t0, s)
        assertEquals(Phase.REVIEW, c.phase)
        assertEquals(4f, c.interval, 0.001f)
    }

    @Test
    fun intervalsGrowWithEachCorrectReview() {
        var c = CardState(phase = Phase.REVIEW, interval = 1f, due = t0)
        val seen = mutableListOf<Float>()
        repeat(4) {
            c = Scheduler.review(c, Rating.GOOD, c.due, s)
            seen += c.interval
        }
        assertEquals(listOf(2.5f, 6.25f, 15.625f, 39.0625f), seen)
    }

    @Test
    fun lapseResetsIntervalLowersEaseAndRelearns() {
        val c = Scheduler.review(CardState(phase = Phase.REVIEW, interval = 20f, due = t0), Rating.AGAIN, t0, s)
        assertEquals(Phase.RELEARNING, c.phase)
        assertEquals(1f, c.interval, 0.001f)
        assertEquals(2.3f, c.ease, 0.001f)
        assertEquals(1, c.lapses)
        assertEquals(t0 + 10 * minute, c.due)
        val back = Scheduler.review(c, Rating.GOOD, c.due, s)
        assertEquals(Phase.REVIEW, back.phase)
    }

    @Test
    fun intervalNeverExceedsTheMaximum() {
        val c = Scheduler.review(CardState(phase = Phase.REVIEW, interval = 300f, due = t0), Rating.EASY, t0, s)
        assertEquals(365f, c.interval, 0.001f)
    }

    @Test
    fun reviewingAheadOfTimeDoesNotInflateTheInterval() {
        val card = CardState(phase = Phase.REVIEW, interval = 10f, due = t0 + 9 * day)
        val c = Scheduler.review(card, Rating.GOOD, t0, s) // only one day after the last review
        assertEquals(10f, c.interval, 0.001f)
    }

    @Test
    fun feedPrefersDueLearningThenMixesNewIntoReviews() {
        val keys = listOf("a", "b", "c", "d")
        val cards = mapOf(
            "a" to CardState(phase = Phase.REVIEW, interval = 3f, due = t0 - day),
            "b" to CardState(phase = Phase.LEARNING, due = t0 - minute),
        )
        assertEquals("b", Scheduler.next(keys, cards, emptyList(), 0, 0, t0, s))
        // "b" just shown: a review is due and no review has been shown since the last new lesson.
        assertEquals("a", Scheduler.next(keys, cards, listOf("b"), 0, 0, t0, s))
        // After enough reviews a new lesson is slipped in, in book order.
        assertEquals("c", Scheduler.next(keys, cards, listOf("b"), 0, 3, t0, s))
    }

    @Test
    fun feedNeverRepeatsALessonStraightAwayAndNeverRunsDryByDefault() {
        val keys = listOf("a", "b")
        val cards = keys.associateWith { CardState(phase = Phase.REVIEW, interval = 5f, due = t0 + 5 * day) }
        assertEquals("b", Scheduler.next(keys, cards, listOf("a"), 0, 0, t0, s))
        assertNull(Scheduler.next(keys, cards, listOf("a"), 0, 0, t0, s.copy(afterGoal = "stop")))
        assertEquals("a", Scheduler.next(listOf("a"), cards, listOf("a"), 0, 0, t0, s))
    }

    @Test
    fun dailyLimitHoldsBackNewLessonsWhileReviewsAreDue() {
        val keys = listOf("a", "b")
        val cards = mapOf("a" to CardState(phase = Phase.REVIEW, interval = 3f, due = t0 - day))
        assertEquals("a", Scheduler.next(keys, cards, emptyList(), newToday = 20, sinceNew = 9, now = t0, s = s))
    }

    @Test
    fun behaviourBecomesARating() {
        assertEquals(Rating.GOOD, Scheduler.rate(1, 0, 1, 9_000, true, 60_000))
        assertEquals(Rating.EASY, Scheduler.rate(1, 0, 1, 2_000, false, 5_000))
        assertEquals(Rating.AGAIN, Scheduler.rate(1, 1, 1, 2_000, true, 60_000))
        assertEquals(Rating.HARD, Scheduler.rate(3, 1, 3, 5_000, true, 60_000))
        assertEquals(Rating.HARD, Scheduler.rate(0, 0, 1, 0, true, 60_000))
        assertEquals(Rating.GOOD, Scheduler.rate(0, 0, 0, 0, true, 60_000))
        assertNull(Scheduler.rate(0, 0, 1, 0, false, 2_000))
        assertNull(Scheduler.rate(0, 0, 1, 0, false, 30_000))
    }

    @Test
    fun studyDayRollsOverAtFourInTheMorning() {
        val threeAm = t0 + 3 * 3_600_000L
        val fiveAm = t0 + 5 * 3_600_000L
        assertTrue(Scheduler.dayOf(fiveAm, 0) == Scheduler.dayOf(threeAm, 0) + 1)
    }
}
