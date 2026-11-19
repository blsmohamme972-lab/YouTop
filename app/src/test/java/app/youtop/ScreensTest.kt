package app.youtop

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import app.youtop.data.Backup
import app.youtop.study.CardState
import app.youtop.study.DayLog
import app.youtop.study.Phase
import app.youtop.study.Progress
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.performTextInput
import kotlinx.coroutines.Dispatchers
import app.youtop.study.StudySettings
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import com.github.takahirom.roborazzi.captureScreenRoboImage
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Drives the real activity through its main screens and saves a picture of each
 * to app/build/screens, so layout changes can be looked at without a device.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-420dpi")
class ScreensTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    /**
     * Under Robolectric the app keeps one files folder for the whole class, so each test ends by
     * forgetting what it learned, saved and set.
     */
    @After
    fun forgetEverything() {
        runBlocking { rule.activity.viewModel().restoreBackup(Backup.encode(Backup(study = StudySettings()))) }
    }

    private fun shot(name: String) {
        rule.waitForIdle()
        rule.mainClock.advanceTimeBy(1200)
        rule.waitForIdle()
        captureScreenRoboImage("build/screens/$name.png")
    }

    /** The feed keeps neighbouring pages composed, so pick the match that is actually on screen. */
    private fun visible(matcher: SemanticsMatcher): SemanticsNodeInteraction {
        val height = rule.onAllNodes(isRoot())[0].fetchSemanticsNode().size.height
        val all = rule.onAllNodes(matcher)
        val index = all.fetchSemanticsNodes().indexOfFirst { it.boundsInRoot.width > 0 && it.boundsInRoot.top >= 0 && it.boundsInRoot.bottom <= height }
        return all[index]
    }

    /** Waits for work that hops between background threads and the main thread (e.g. saving settings). */
    private fun settle(condition: () -> Boolean) {
        repeat(200) {
            rule.waitForIdle()
            if (condition()) return
            Thread.sleep(25)
        }
        error("Condition not reached")
    }

    private fun button(label: String) = visible(hasContentDescription(label))
    private fun text(text: String) = visible(hasText(text))

    /** The app opens on the library's first page; its main button goes to the first lesson. */
    private fun openFirstLesson(start: String = "Start studying") {
        rule.waitUntil(10_000) { rule.onAllNodesWithText("كيف يسافر الضوء").fetchSemanticsNodes().isNotEmpty() }
        rule.waitUntil(10_000) { rule.onAllNodesWithText(start).fetchSemanticsNodes().isNotEmpty() }
        shot(if (start == "Start studying") "0-opens-on-home" else "0-ar-opens-on-home")
        text(start).performClick()
        rule.waitForIdle()
        rule.mainClock.advanceTimeBy(600)
        rule.waitUntil(10_000) { rule.activity.viewModel().narrator.state.value.playing }
    }

    /** "Study this book" on its card chooses the book, closes the library and the feed is that book's. */
    @Test
    fun studyingByBook() {
        openFirstLesson()
        val vm = rule.activity.viewModel()
        button("Library").performClick()
        rule.onNode(hasText("Study this book")).performScrollTo().performClick()
        settle { vm.settings.value.study.book == "starter" }
        rule.waitUntil(10_000) { vm.queue.isNotEmpty() && vm.queue.all { it.item.book.id == "starter" } }
        rule.mainClock.advanceTimeBy(600)
        rule.waitUntil(10_000) { vm.narrator.state.value.playing } // back on the lesson, and it plays on
        button("Library").performClick()
        text("Books").performClick()
        text("YouTop Starter").performClick()
        shot("7b2-book-studied")
    }

    private fun seekToSentence(index: Int) = rule.runOnUiThread { rule.activity.viewModel().narrator.seekToSegment(index) }

    @Test
    fun feedAndSpotlight() {
        openFirstLesson()
        shot("1-feed")
        seekToSentence(3) // a sentence that points at a region of the picture
        shot("2-spotlight")
        text("Explain").performClick()
        seekToSentence(6)
        shot("3-explanation")
    }

    @Test
    fun transcript() {
        openFirstLesson()
        seekToSentence(4)
        button("Text").performClick()
        shot("4-transcript")
    }

    @Test
    fun quiz() {
        openFirstLesson()
        val vm = rule.activity.viewModel()
        button("Quiz").performClick()
        shot("5-quiz") // the picture whole at the top with the question's circle; question and options in the panel under it
        text("All circles").performClick()
        shot("5c-quiz-all-circles") // the picture moves up, whole; the table sits under it
        // The table is fixed under the picture and its rows scroll; a row singles out its circle.
        rule.onNode(hasText("The straw in the glass")).performScrollTo()
        rule.onNode(hasText("The tilted mirror")).performScrollTo().performClick()
        shot("5d-quiz-table-row-picked")
        rule.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }
        rule.waitForIdle()
        text("In circles").performClick()
        shot("5b-quiz-answered") // the picture moves up; the right answer, the wrong pick and the explanation sit under it

        // The lesson has four more questions: until the last is answered, a swipe does not leave it.
        rule.onAllNodes(isRoot())[0].performTouchInput { swipeUp() }
        rule.waitForIdle()
        rule.mainClock.advanceTimeBy(1500)
        rule.waitForIdle()
        assertEquals("how-light-travels", vm.queue[0].item.lesson.id)
        assertEquals(true, vm.holds(0))
        text("In circles").assertExists() // still on its question
        for (answer in listOf("Reflection", "Light changes speed and direction between water and air", "8 minutes")) {
            text("Next").performClick()
            rule.waitForIdle()
            text(answer).performClick()
            rule.waitForIdle()
        }
        // The last question has several right answers: each tap ticks one, "Check" answers.
        text("Next").performClick()
        rule.waitForIdle()
        text("Choose all that apply").assertExists()
        shot("5f-quiz-choose-all")
        assertEquals(true, vm.holds(0))
        for (option in listOf("The Sun", "A candle flame", "A firefly")) text(option).performClick()
        rule.waitForIdle()
        assertEquals(true, vm.holds(0)) // ticking is not answering
        shot("5g-quiz-choose-all-ticked")
        text("Check").performClick()
        rule.waitForIdle()
        text("The Moon").assertDoesNotExist() // not picked and not right: no longer shown
        shot("5e-quiz-last-answered")
        assertEquals(false, vm.holds(0))
        // Answering is what feeds the scheduler: one wrong of five, and the lesson is done for now.
        assertEquals(1, vm.progress.value.doneToday)
        assertEquals(1, vm.progress.value.cards.getValue("starter/examples/how-light-travels").reps)
        // Now the swipe goes on to the next lesson.
        rule.onAllNodes(isRoot())[0].performTouchInput { swipeUp() }
        rule.waitForIdle()
        rule.mainClock.advanceTimeBy(1500)
        rule.waitForIdle()
        rule.waitUntil(10_000) { vm.queue.size >= 3 }
    }

    /** Swiping away from a lesson whose question is unanswered brings it back with the question open. */
    @Test
    fun unansweredLessonComesBack() {
        openFirstLesson()
        val vm = rule.activity.viewModel()
        button("All circles").performClick() // the button on the lesson itself, at any time
        shot("9a-all-circles")
        assertEquals(false, vm.narrator.state.value.playing)
        button("Close").performClick()
        rule.waitForIdle()
        rule.onAllNodes(isRoot())[0].performTouchInput { swipeUp() }
        rule.waitUntil(10_000) { rule.onAllNodes(hasText("How does light travel", substring = true)).fetchSemanticsNodes().any { it.boundsInRoot.width > 0 } }
        shot("9b-held-back-question-open")
        assertEquals(true, vm.holds(0))
        assertEquals(0, vm.progress.value.doneToday)
        button("Quiz").assertDoesNotExist()
    }

    /** For the tests about other things: lets the feed move on without answering. */
    private fun letPass() = rule.runOnUiThread { rule.activity.viewModel().run { queue.forEach { pass(it.id) } } }

    @Test
    fun secondLessonAndLibrary() {
        openFirstLesson()
        letPass()
        rule.onAllNodes(isRoot())[0].performTouchInput { swipeUp() }
        rule.waitUntil(10_000) { rule.activity.viewModel().queue.size >= 3 }
        button("Save").performClick()
        shot("6-second-lesson")
        button("Library").performClick()
        shot("7-home-first-day")
        text("Books").performClick()
        shot("7a-books")
        text("YouTop Starter").performClick()
        shot("7b-book")
        text("Topics").performClick()
        shot("7c-topics")
        button("Study only Cardiac surgery").performClick() // focus the feed on cardiac surgery
        settle { rule.activity.viewModel().settings.value.study.focus == setOf("cardiac") }
        shot("7c2-topics-focus")
        text("Show all").performClick()
        settle { rule.activity.viewModel().settings.value.study.focus.isEmpty() }
        text("Saved").performClick()
        shot("7d-saved")
        text("Settings").performClick()
        shot("8-settings")
        rule.onNode(hasText("BACKUP")).performScrollTo()
        shot("8a-settings-content-backup")
        rule.onNode(hasText("ADVANCED TIMING")).performScrollTo().performClick()
        rule.onNode(hasText("Longest interval (days)")).performScrollTo()
        shot("8b-settings-advanced")
    }

    /** The dashboard after some weeks of study, the book search, and what the feed says when nothing matches. */
    @Test
    fun dashboardSearchAndEmptyFeed() {
        openFirstLesson()
        val vm = rule.activity.viewModel()
        val now = System.currentTimeMillis()
        val today = vm.progress.value.day
        val day = 86_400_000L
        val keys = listOf("how-light-travels", "why-we-remember-stories", "journey-of-a-water-drop").map { "starter/examples/$it" }
        val history = (0L..9L).associate { back -> (today - back) to DayLog(done = listOf(18, 31, 24, 0, 12, 35, 27, 30, 9, 22)[back.toInt()], again = 3) }
        var restored: Boolean? = null
        val text = (Backup.encode(Backup(
                progress = Progress(
                    cards = mapOf(
                        keys[0] to CardState(Phase.REVIEW, interval = 6f, due = now - day, reps = 9, lapses = 5, seen = now - 7 * day),
                        keys[1] to CardState(Phase.REVIEW, interval = 3f, due = now + 2 * day, reps = 4, seen = now - day),
                    ),
                    day = today, doneToday = 18, newToday = 4, streak = 9, lastStudyDay = today, history = history,
                ),
                saved = setOf(keys[2]),
            )))
        // Restoring waits for the settings to be written on another thread, so it cannot block this one.
        rule.runOnUiThread { GlobalScope.launch(Dispatchers.Main) { restored = vm.restoreBackup(text) } }
        settle { restored != null }
        assertEquals(true, restored)
        settle { vm.settings.value.saved == setOf(keys[2]) }
        assertEquals(9, vm.progress.value.streak)
        button("Library").performClick()
        shot("11-home")
        // Progress by topic, across books: covers, the coloured bar and its numbers.
        text("Topics").performClick()
        shot("11b-topics-progress")
        rule.onAllNodes(androidx.compose.ui.test.hasScrollToNodeAction())[1].performScrollToNode(hasText("KNOWLEDGE TYPE")) // [0] is the feed underneath
        shot("11c-topics-groups")
        text("Books").performClick()
        rule.onNode(hasSetTextAction()).performTextInput("water")
        rule.waitUntil(10_000) { rule.onAllNodesWithText("Journey of a Water Drop").fetchSemanticsNodes().isNotEmpty() }
        shot("12-search")
        assertEquals(0, rule.onAllNodesWithText("How Light Travels").fetchSemanticsNodes().size)
        button("Close").performClick()

        // A focus that no lesson carries empties the feed; it must say so and offer the way out.
        rule.runOnUiThread { vm.setStudy(vm.settings.value.study.copy(focus = setOf("vascular/no-such-topic"))) }
        rule.waitUntil(10_000) { rule.onAllNodesWithText("Show everything").fetchSemanticsNodes().isNotEmpty() }
        shot("13-nothing-matches")
        text("Show everything").performClick()
        settle { vm.settings.value.study.focus.isEmpty() }
        rule.waitUntil(10_000) { vm.queue.isNotEmpty() }
    }

    @Test
    fun questionFirstSkipsTheVideoUntilAsked() {
        openFirstLesson()
        val vm = rule.activity.viewModel()
        rule.runOnUiThread { vm.setStudy(vm.settings.value.study.copy(questionFirst = true)) }
        settle { vm.settings.value.study.questionFirst }
        letPass()
        rule.onAllNodes(isRoot())[0].performTouchInput { swipeUp() }
        rule.waitUntil(10_000) {
            rule.onAllNodes(hasText("Which brain structure", substring = true)).fetchSemanticsNodes().any { it.boundsInRoot.width > 0 }
        }
        shot("10-question-first")
        assertEquals(false, vm.narrator.state.value.playing)

        text("Hippocampus").performClick()
        rule.waitForIdle()
        text("Watch").performClick()
        rule.waitForIdle()
        rule.mainClock.advanceTimeBy(600)
        button("Quiz").assertExists() // back on the lesson itself
        assertEquals(true, vm.narrator.state.value.playing)
    }

    /** The same pages on a phone set to Arabic: everything mirrors and nothing is cut off. */
    @Test
    @Config(sdk = [34], qualifiers = "ar-w411dp-h891dp-420dpi")
    fun arabicInterface() {
        openFirstLesson("ابدأ المذاكرة")
        val vm = rule.activity.viewModel()
        val today = vm.progress.value.day
        val now = System.currentTimeMillis()
        val text = Backup.encode(Backup(progress = Progress(
            cards = mapOf("starter/examples/how-light-travels" to CardState(Phase.REVIEW, interval = 2f, due = now + 86_400_000L, reps = 3)),
            day = today, doneToday = 11, streak = 3, lastStudyDay = today,
            history = (0L..6L).associate { (today - it) to DayLog(done = 8 + 3 * it.toInt(), again = 2) },
        )))
        runBlocking { vm.restoreBackup(text) }
        button("المكتبة").performClick()
        shot("20-ar-home")
        text("الكتب").performClick()
        text("YouTop Starter").performClick()
        shot("21-ar-book")
        text("المواضيع").performClick()
        shot("24-ar-topics")
        text("الإعدادات").performClick()
        shot("22-ar-settings")
        rule.onNode(hasText("النسخة الاحتياطية")).performScrollTo()
        shot("23-ar-settings-backup")
    }
}
