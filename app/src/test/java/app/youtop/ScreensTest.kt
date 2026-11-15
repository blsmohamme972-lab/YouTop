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

    private fun openFirstLesson() {
        rule.waitUntil(10_000) { rule.onAllNodesWithText("كيف يسافر الضوء").fetchSemanticsNodes().isNotEmpty() }
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
        button("Quiz").performClick()
        shot("5-quiz")
        text("In circles").performClick()
        shot("5b-quiz-answered")
        // Moving on is what feeds the scheduler: a wrong answer puts the lesson in its first learning step.
        rule.onAllNodes(isRoot())[0].performTouchInput { swipeUp() }
        rule.waitForIdle()
        rule.mainClock.advanceTimeBy(1500)
        rule.waitForIdle()
        val vm = rule.activity.viewModel()
        val card = vm.progress.value.cards.getValue("starter/examples/how-light-travels")
        assertEquals(Phase.LEARNING, card.phase)
        assertEquals(1, vm.progress.value.doneToday)
    }

    @Test
    fun secondLessonAndLibrary() {
        openFirstLesson()
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
        text("Cardiac surgery").performClick() // focus the feed on cardiac surgery
        settle { rule.activity.viewModel().settings.value.study.focus == setOf("cardiac") }
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
                        keys[0] to CardState(Phase.REVIEW, interval = 6f, due = now - day, reps = 5, lapses = 1),
                        keys[1] to CardState(Phase.REVIEW, interval = 3f, due = now + 2 * day, reps = 4),
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
        openFirstLesson()
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
        text("الإعدادات").performClick()
        shot("22-ar-settings")
        rule.onNode(hasText("النسخة الاحتياطية")).performScrollTo()
        shot("23-ar-settings-backup")
    }
}
