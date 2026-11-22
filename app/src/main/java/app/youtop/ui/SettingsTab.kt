package app.youtop.ui

import android.text.format.DateUtils
import android.text.format.Formatter
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.youtop.BuildConfig
import app.youtop.MainViewModel
import app.youtop.R
import app.youtop.data.SyncState
import app.youtop.study.Calibration
import app.youtop.study.Memory
import app.youtop.study.Scheduler
import app.youtop.study.StudySettings
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import kotlin.math.roundToInt

/**
 * Everything that can be tuned, grouped: the daily pace, how a lesson plays, where lessons come
 * from, a backup of the learning history, and the scheduler's timings for those who want them.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsTab(vm: MainViewModel) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val s = settings.study
    val set: (StudySettings) -> Unit = { vm.setStudy(it) }
    var advanced by rememberSaveable { mutableStateOf(false) }
    var confirmReset by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding()
            .padding(horizontal = 16.dp).padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Card(title = stringResource(R.string.section_pace)) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // A preset changes the pace but keeps the feed behaviour and book choices.
                fun paced(p: StudySettings) = p.copy(
                    questionFirst = s.questionFirst, autoContinue = s.autoContinue, autoAdvance = s.autoAdvance, shuffleOptions = s.shuffleOptions,
                    speakQuiz = s.speakQuiz, excludedBooks = s.excludedBooks, focus = s.focus, weakFirst = s.weakFirst, afterGoal = s.afterGoal, randomNewOrder = s.randomNewOrder,
                    engine = s.engine, examDay = s.examDay, adaptive = s.adaptive, holdNew = s.holdNew,
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
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("more" to R.string.after_more, "ahead" to R.string.after_ahead, "stop" to R.string.after_stop).forEach { (value, label) ->
                    Chip(stringResource(label), s.afterGoal == value) { set(s.copy(afterGoal = value)) }
                }
            }
        }

        MemoryCard(vm, s, set)

        Card(title = stringResource(R.string.section_lesson), spacing = 14.dp) {
            Toggle(stringResource(R.string.question_first), s.questionFirst, stringResource(R.string.question_first_hint)) { set(s.copy(questionFirst = it)) }
            Toggle(stringResource(R.string.auto_continue), s.autoContinue) { set(s.copy(autoContinue = it)) }
            Toggle(stringResource(R.string.auto_advance), s.autoAdvance) { set(s.copy(autoAdvance = it)) }
            Toggle(stringResource(R.string.shuffle_options), s.shuffleOptions) { set(s.copy(shuffleOptions = it)) }
            Toggle(stringResource(R.string.speak_quiz), s.speakQuiz) { set(s.copy(speakQuiz = it)) }
        }

        Card(title = stringResource(R.string.section_order), spacing = 14.dp) {
            Toggle(stringResource(R.string.weak_first), s.weakFirst, stringResource(R.string.weak_first_hint)) { set(s.copy(weakFirst = it)) }
            Toggle(stringResource(R.string.random_new), s.randomNewOrder) { set(s.copy(randomNewOrder = it)) }
        }

        ContentCard(vm)
        BackupCard(vm)

        Card {
            Row(Modifier.fillMaxWidth().clickable { advanced = !advanced }, verticalAlignment = Alignment.CenterVertically) {
                SectionTitle(stringResource(R.string.advanced), Modifier.weight(1f))
                Icon(if (advanced) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, null, tint = Accent)
            }
            if (advanced) {
                Text(stringResource(R.string.advanced_hint), color = Faint, fontSize = 12.sp, lineHeight = 17.sp)
                Steps(stringResource(R.string.learning_steps), s.learningSteps) { set(s.copy(learningSteps = it)) }
                Steps(stringResource(R.string.relearning_steps), s.relearningSteps) { set(s.copy(relearningSteps = it)) }
                if (!s.smart) {
                    // The classic scheduler's multipliers; the smart one works these out for each lesson.
                    Stepper(stringResource(R.string.graduating_days), s.graduatingDays, 1f, 1f..30f) { set(s.copy(graduatingDays = it)) }
                    Stepper(stringResource(R.string.easy_days), s.easyDays, 1f, 1f..60f) { set(s.copy(easyDays = it)) }
                    Stepper(stringResource(R.string.starting_ease), s.startingEase, 0.1f, 1.3f..4f, percent = true) { set(s.copy(startingEase = it)) }
                    Stepper(stringResource(R.string.easy_bonus), s.easyBonus, 0.05f, 1f..2f, percent = true) { set(s.copy(easyBonus = it)) }
                    Stepper(stringResource(R.string.hard_factor), s.hardFactor, 0.05f, 1f..1.5f, percent = true) { set(s.copy(hardFactor = it)) }
                    Stepper(stringResource(R.string.interval_modifier), s.intervalModifier, 0.05f, 0.3f..2f, percent = true) { set(s.copy(intervalModifier = it)) }
                    Stepper(stringResource(R.string.lapse_factor), s.lapseFactor, 0.1f, 0f..1f, percent = true) { set(s.copy(lapseFactor = it)) }
                }
                Stepper(stringResource(R.string.max_days), s.maxDays, 5f, 1f..3650f) { set(s.copy(maxDays = it)) }
                TextButton(onClick = { confirmReset = true }) { Text(stringResource(R.string.reset_progress), color = Bad) }
            }
        }

        Text(
            stringResource(R.string.about, BuildConfig.VERSION_NAME), color = Faint, fontSize = 12.sp,
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp, start = 4.dp),
        )
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            text = { Text(stringResource(R.string.reset_confirm)) },
            confirmButton = {
                TextButton(onClick = { vm.resetProgress(); confirmReset = false }) { Text(stringResource(R.string.reset), color = Bad) }
            },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

/**
 * How the intervals between reviews are decided. The smart scheduler needs one number from the
 * learner (how sure they want to be of remembering) and shows at once what it leads to.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MemoryCard(vm: MainViewModel, s: StudySettings, set: (StudySettings) -> Unit) {
    val progress by vm.progress.collectAsStateWithLifecycle()
    val today = progress.day
    val personal = if (s.adaptive) Memory.personalFactor(progress.calibration) ?: 1f else 1f
    val toExam = (s.examDay - today).toInt()

    Card(title = stringResource(R.string.section_memory), spacing = 14.dp) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip(stringResource(R.string.engine_smart), s.smart) { set(s.copy(engine = "smart")) }
            Chip(stringResource(R.string.engine_classic), !s.smart) { set(s.copy(engine = "classic")) }
        }
        Text(stringResource(if (s.smart) R.string.engine_smart_hint else R.string.engine_classic_hint), color = Faint, fontSize = 12.sp, lineHeight = 17.sp)
        if (!s.smart) return@Card

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Stepper(stringResource(R.string.retention), s.retention, 0.01f, 0.80f..0.97f, percent = true) { set(s.copy(retention = it)) }
            Text(stringResource(R.string.retention_hint), color = Faint, fontSize = 12.sp, lineHeight = 17.sp)
        }
        // What that means: the gaps a lesson answered right every time will get.
        val gaps = remember(s, personal, today) { Scheduler.preview(s, personal, today) }
        if (gaps.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.preview_title), color = Color.White, fontSize = 13.sp)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    gaps.forEach { days ->
                        Text(
                            span(days), color = Accent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clip(CircleShape).background(Accent.copy(alpha = 0.12f)).padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.exam), color = Color.White, fontSize = 15.sp)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip(stringResource(R.string.exam_none), toExam < 1) { set(s.copy(examDay = 0)) }
                listOf(14 to R.string.exam_2w, 30 to R.string.exam_1m, 90 to R.string.exam_3m, 180 to R.string.exam_6m).forEach { (days, label) ->
                    Chip(stringResource(label), toExam == days) { set(s.copy(examDay = today + days)) }
                }
            }
            if (toExam >= 1) {
                Stepper(stringResource(R.string.exam_days), toExam.toFloat(), 1f, 1f..730f) { set(s.copy(examDay = today + it.roundToInt())) }
                Text(stringResource(R.string.exam_hint), color = Faint, fontSize = 12.sp, lineHeight = 17.sp)
            }
        }

        Toggle(
            stringResource(R.string.adaptive), s.adaptive,
            progress.calibration.measured?.let {
                stringResource(R.string.adaptive_measured, num((it * 100).roundToInt()), num((s.retention * 100).roundToInt()), num((personal * 100).roundToInt()))
            } ?: stringResource(R.string.adaptive_hint, num(progress.calibration.n.roundToInt().coerceAtMost(Calibration.ENOUGH.toInt())), num(Calibration.ENOUGH.toInt())),
        ) { set(s.copy(adaptive = it)) }
        Toggle(stringResource(R.string.hold_new), s.holdNew, stringResource(R.string.hold_new_hint)) { set(s.copy(holdNew = it)) }
    }
}

/** A gap between reviews in words: days, then months, then years. */
@Composable
fun span(days: Float): String = when {
    days < 30.5f -> pluralStringResource(R.plurals.span_days, days.roundToInt().coerceAtLeast(1), days.roundToInt().coerceAtLeast(1))
    days < 365f -> pluralStringResource(R.plurals.span_months, (days / 30.44f).roundToInt(), (days / 30.44f).roundToInt())
    else -> stringResource(R.string.span_years, "%.1f".format(days / 365.25f))
}

/** Where lessons come from and how much of them is kept on the phone. */
@Composable
private fun ContentCard(vm: MainViewModel) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val sync by vm.sync.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val focus = LocalFocusManager.current
    var link by remember(settings.driveFolder) { mutableStateOf(settings.driveFolder) }
    var spare by rememberSaveable { mutableStateOf(false) }
    val running = sync is SyncState.Running
    // Counted off the main thread, and not on every step of a long download.
    var cleared by remember { mutableIntStateOf(0) }
    val mediaSize by produceState(0L, running, cleared) { value = withContext(Dispatchers.IO) { vm.media.sizeBytes() } }

    Card(title = stringResource(R.string.section_content), spacing = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).padding(end = 12.dp)) {
                Text(
                    when (val st = sync) {
                        is SyncState.Running -> stringResource(R.string.sync_running) + if (st.total > 0) "  ${st.done}/${st.total}" else ""
                        is SyncState.Done ->
                            pluralStringResource(R.plurals.sync_done, st.books, st.books) +
                                if (st.failed > 0) "\n" + pluralStringResource(R.plurals.sync_some_failed, st.failed, st.failed) else ""
                        is SyncState.Failed -> stringResource(if (st.offline) R.string.sync_offline else R.string.sync_failed)
                        SyncState.Idle -> when {
                            settings.lastSync == 0L -> stringResource(R.string.sync_never)
                            else -> stringResource(R.string.sync_last, DateUtils.getRelativeTimeSpanString(settings.lastSync).toString())
                        }
                    },
                    color = if (sync is SyncState.Failed || (sync as? SyncState.Done)?.failed ?: 0 > 0) Color(0xFFFCA5A5) else Color.White,
                    fontSize = 14.sp, lineHeight = 20.sp,
                )
                (sync as? SyncState.Failed)?.message?.takeIf { it.isNotBlank() }?.let {
                    Text(it, color = Faint, fontSize = 12.sp, lineHeight = 16.sp, maxLines = 3)
                }
            }
            if (running) {
                CircularProgressIndicator(Modifier.size(24.dp), color = Accent, strokeWidth = 2.5.dp)
            } else {
                PrimaryButton(stringResource(R.string.sync_now), Icons.Rounded.Sync) {
                    focus.clearFocus()
                    vm.setDriveFolder(link).invokeOnCompletion { vm.syncNow() }
                }
            }
        }
        Toggle(stringResource(R.string.auto_sync), settings.autoSync) { vm.setAutoSync(it) }
        Toggle(stringResource(R.string.offline_download), settings.offlineDownload, stringResource(R.string.offline_hint)) { vm.setOfflineDownload(it) }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).padding(end = 12.dp)) {
                Text(stringResource(R.string.downloaded_media), color = Color.White, fontSize = 15.sp)
                Text(Formatter.formatShortFileSize(context, mediaSize), color = Faint, fontSize = 12.sp)
            }
            SecondaryButton(stringResource(R.string.clear_cache)) { vm.clearMedia().invokeOnCompletion { cleared++ } }
        }
        Row(Modifier.fillMaxWidth().clickable { spare = !spare }, verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.drive_folder), color = Faint, fontSize = 13.sp, modifier = Modifier.weight(1f))
            Icon(if (spare) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, null, tint = Faint)
        }
        if (spare) {
            OutlinedTextField(
                value = link,
                onValueChange = { link = it },
                supportingText = { Text(stringResource(R.string.drive_hint)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { vm.setDriveFolder(link); focus.clearFocus() }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                    focusedBorderColor = Accent, unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                    focusedSupportingTextColor = Faint, unfocusedSupportingTextColor = Faint,
                    cursorColor = Accent,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** The learning history as a file the learner keeps somewhere safe, and the way back. */
@Composable
private fun BackupCard(vm: MainViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    fun say(text: Int) = Toast.makeText(context, text, Toast.LENGTH_LONG).show()

    val save = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val text = vm.backupText()
        scope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching { context.contentResolver.openOutputStream(uri)!!.use { it.write(text.toByteArray()) } }.isSuccess
            }
            say(if (ok) R.string.backup_saved else R.string.backup_failed)
        }
    }
    val open = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val text = withContext(Dispatchers.IO) {
                // A backup is a few hundred kilobytes; anything huge is not one.
                runCatching { context.contentResolver.openInputStream(uri)!!.use { it.readNBytesCompat(20_000_000) }.toString(Charsets.UTF_8) }.getOrNull()
            }
            say(if (text != null && vm.restoreBackup(text)) R.string.backup_restored else R.string.backup_not_valid)
        }
    }
    var confirmRestore by remember { mutableStateOf(false) }

    Card(title = stringResource(R.string.section_backup)) {
        Text(stringResource(R.string.backup_hint), color = Faint, fontSize = 13.sp, lineHeight = 19.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SecondaryButton(stringResource(R.string.backup_save), Icons.Rounded.CloudUpload, Modifier.weight(1f)) {
                runCatching { save.launch("youtop-backup-${LocalDate.now()}.json") }.onFailure { say(R.string.backup_failed) }
            }
            SecondaryButton(stringResource(R.string.backup_restore), Icons.Rounded.CloudDownload, Modifier.weight(1f)) { confirmRestore = true }
        }
    }

    if (confirmRestore) {
        AlertDialog(
            onDismissRequest = { confirmRestore = false },
            text = { Text(stringResource(R.string.backup_restore_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmRestore = false
                    runCatching { open.launch(arrayOf("application/json", "text/*", "application/octet-stream")) }.onFailure { say(R.string.backup_failed) }
                }) { Text(stringResource(R.string.backup_choose)) }
            },
            dismissButton = { TextButton(onClick = { confirmRestore = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

private fun java.io.InputStream.readNBytesCompat(limit: Int): ByteArray {
    val out = java.io.ByteArrayOutputStream()
    val buffer = ByteArray(16 * 1024)
    while (out.size() < limit) {
        val n = read(buffer)
        if (n < 0) break
        out.write(buffer, 0, n)
    }
    return out.toByteArray()
}
