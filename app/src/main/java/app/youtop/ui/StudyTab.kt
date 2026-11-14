package app.youtop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.youtop.MainViewModel
import app.youtop.R
import app.youtop.data.flatten
import app.youtop.study.Phase
import app.youtop.study.StudySettings
import app.youtop.study.Tags
import kotlin.math.roundToInt


/** How the feed paces new lessons and reviews. Presets cover most needs; every number can also be set by hand. */
@Composable
fun StudyTab(vm: MainViewModel) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val progress by vm.progress.collectAsStateWithLifecycle()
    val books by vm.books.collectAsStateWithLifecycle()
    val s = settings.study
    val set: (StudySettings) -> Unit = { vm.setStudy(it) }
    var advanced by rememberSaveable { mutableStateOf(false) }
    var confirmReset by remember { mutableStateOf(false) }

    val now = remember(progress) { System.currentTimeMillis() }
    val keys = remember(books, s.excludedBooks, s.focus) { books.filter { it.id !in s.excludedBooks }.flatten().filter { Tags.matches(it.lesson.tags, s.focus) }.map { it.key } }
    val dueNow = keys.count { k -> progress.cards[k]?.let { it.phase != Phase.NEW && it.due <= now } == true }
    val fresh = keys.count { k -> (progress.cards[k]?.phase ?: Phase.NEW) == Phase.NEW }
    val learned = keys.count { k -> progress.cards[k]?.phase == Phase.REVIEW }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding().navigationBarsPadding()
            .padding(horizontal = 20.dp).padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column {
            Text(
                stringResource(R.string.today_progress, progress.doneToday, s.dailyGoal, progress.liveStreak(progress.day)),
                color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
            )
            Text(stringResource(R.string.queue_status, dueNow, fresh, learned), color = Faint, fontSize = 13.sp)
        }

        Label(stringResource(R.string.preset))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // A preset changes the pace but keeps the feed behaviour and book choices.
            fun paced(p: StudySettings) = p.copy(
                questionFirst = s.questionFirst, autoContinue = s.autoContinue, autoAdvance = s.autoAdvance, shuffleOptions = s.shuffleOptions,
                speakQuiz = s.speakQuiz, excludedBooks = s.excludedBooks, focus = s.focus, weakFirst = s.weakFirst, afterGoal = s.afterGoal, randomNewOrder = s.randomNewOrder,
            )
            listOf(
                R.string.preset_relaxed to StudySettings.Relaxed,
                R.string.preset_standard to StudySettings.Standard,
                R.string.preset_intensive to StudySettings.Intensive,
            ).forEach { (label, preset) -> Chip(stringResource(label), s == paced(preset)) { set(paced(preset)) } }
        }

        Stepper(stringResource(R.string.new_per_day), s.newPerDay.toFloat(), 5f, 0f..500f) { set(s.copy(newPerDay = it.roundToInt())) }
        Stepper(stringResource(R.string.daily_goal), s.dailyGoal.toFloat(), 5f, 5f..1000f) { set(s.copy(dailyGoal = it.roundToInt())) }
        Stepper(stringResource(R.string.reviews_per_new), s.reviewsPerNew.toFloat(), 1f, 0f..20f) { set(s.copy(reviewsPerNew = it.roundToInt())) }

        Label(stringResource(R.string.after_goal))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("more" to R.string.after_more, "ahead" to R.string.after_ahead, "stop" to R.string.after_stop).forEach { (value, label) ->
                Chip(stringResource(label), s.afterGoal == value) { set(s.copy(afterGoal = value)) }
            }
        }

        Toggle(stringResource(R.string.question_first), s.questionFirst) { set(s.copy(questionFirst = it)) }
        Toggle(stringResource(R.string.auto_continue), s.autoContinue) { set(s.copy(autoContinue = it)) }
        Toggle(stringResource(R.string.auto_advance), s.autoAdvance) { set(s.copy(autoAdvance = it)) }
        Toggle(stringResource(R.string.shuffle_options), s.shuffleOptions) { set(s.copy(shuffleOptions = it)) }
        Toggle(stringResource(R.string.speak_quiz), s.speakQuiz) { set(s.copy(speakQuiz = it)) }
        Toggle(stringResource(R.string.weak_first), s.weakFirst) { set(s.copy(weakFirst = it)) }
        Toggle(stringResource(R.string.random_new), s.randomNewOrder) { set(s.copy(randomNewOrder = it)) }

        Row(
            Modifier.fillMaxWidth().clickable { advanced = !advanced }.padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.advanced), color = Accent, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Icon(if (advanced) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, null, tint = Accent)
        }
        if (advanced) {
            Steps(stringResource(R.string.learning_steps), s.learningSteps) { set(s.copy(learningSteps = it)) }
            Steps(stringResource(R.string.relearning_steps), s.relearningSteps) { set(s.copy(relearningSteps = it)) }
            Stepper(stringResource(R.string.graduating_days), s.graduatingDays, 1f, 1f..30f) { set(s.copy(graduatingDays = it)) }
            Stepper(stringResource(R.string.easy_days), s.easyDays, 1f, 1f..60f) { set(s.copy(easyDays = it)) }
            Stepper(stringResource(R.string.starting_ease), s.startingEase, 0.1f, 1.3f..4f, percent = true) { set(s.copy(startingEase = it)) }
            Stepper(stringResource(R.string.easy_bonus), s.easyBonus, 0.05f, 1f..2f, percent = true) { set(s.copy(easyBonus = it)) }
            Stepper(stringResource(R.string.hard_factor), s.hardFactor, 0.05f, 1f..1.5f, percent = true) { set(s.copy(hardFactor = it)) }
            Stepper(stringResource(R.string.interval_modifier), s.intervalModifier, 0.05f, 0.3f..2f, percent = true) { set(s.copy(intervalModifier = it)) }
            Stepper(stringResource(R.string.lapse_factor), s.lapseFactor, 0.1f, 0f..1f, percent = true) { set(s.copy(lapseFactor = it)) }
            Stepper(stringResource(R.string.max_days), s.maxDays, 5f, 1f..3650f) { set(s.copy(maxDays = it)) }
        }

        TextButton(onClick = { confirmReset = true }) {
            Text(stringResource(R.string.reset_progress), color = Color(0xFFFCA5A5))
        }
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            text = { Text(stringResource(R.string.reset_confirm)) },
            confirmButton = {
                TextButton(onClick = { vm.resetProgress(); confirmReset = false }) { Text(stringResource(R.string.reset), color = Color(0xFFFCA5A5)) }
            },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@Composable
private fun Label(text: String) = Text(text, color = Faint, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))

@Composable
private fun Chip(text: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text,
        color = if (selected) Color.Black else Color.White,
        fontSize = 14.sp, fontWeight = FontWeight.Medium,
        modifier = Modifier.clip(CircleShape)
            .background(if (selected) Color.White else Color.White.copy(alpha = 0.10f))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
    )
}

@Composable
private fun Toggle(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Color.White, fontSize = 15.sp, modifier = Modifier.weight(1f).padding(end = 12.dp))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun Stepper(
    label: String,
    value: Float,
    step: Float,
    range: ClosedFloatingPointRange<Float>,
    percent: Boolean = false,
    onChange: (Float) -> Unit,
) {
    fun snap(v: Float) = ((v / step).roundToInt() * step).coerceIn(range.start, range.endInclusive)
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Color.White, fontSize = 15.sp, modifier = Modifier.weight(1f).padding(end = 12.dp))
        RoundIcon(Icons.Rounded.Remove) { onChange(snap(value - step)) }
        Text(
            if (percent) "${(value * 100).roundToInt()}%" else if (value % 1f == 0f) "${value.toInt()}" else "%.1f".format(value),
            color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center,
            modifier = Modifier.width(64.dp),
        )
        RoundIcon(Icons.Rounded.Add) { onChange(snap(value + step)) }
    }
}

@Composable
private fun RoundIcon(icon: ImageVector, onClick: () -> Unit) {
    Box(
        Modifier.size(36.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.10f)).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = Color.White, modifier = Modifier.size(18.dp)) }
}

/** A list of minutes typed as "1 10 60". */
@Composable
private fun Steps(label: String, steps: List<Int>, onChange: (List<Int>) -> Unit) {
    var text by remember(steps) { mutableStateOf(steps.joinToString(" ")) }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Color.White, fontSize = 15.sp, modifier = Modifier.weight(1f).padding(end = 12.dp))
        BasicTextField(
            value = text,
            onValueChange = { typed ->
                text = typed.filter { it.isDigit() || it == ' ' }
                val parsed = text.split(' ').mapNotNull { it.toIntOrNull() }.filter { it in 1..10_080 }
                if (parsed != steps && !text.endsWith(' ')) onChange(parsed)
            },
            singleLine = true,
            textStyle = TextStyle(color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center),
            cursorBrush = SolidColor(Color.White),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.width(136.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.10f)).padding(horizontal = 14.dp, vertical = 9.dp),
        )
    }
}
