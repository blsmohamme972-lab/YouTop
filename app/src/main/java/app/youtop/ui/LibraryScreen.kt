package app.youtop.ui

import android.text.format.DateUtils
import android.text.format.Formatter
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.youtop.MainViewModel
import app.youtop.R
import app.youtop.data.FeedItem
import app.youtop.data.SyncState
import app.youtop.data.flatten
import coil3.compose.AsyncImage

private val Background = Color(0xFF0E0F13)
private val Faint = Color.White.copy(alpha = 0.6f)

@Composable
fun LibraryScreen(vm: MainViewModel, onClose: () -> Unit) {
    val books by vm.books.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val open: (FeedItem) -> Unit = { vm.jumpTo(it.key); onClose() }

    Column(Modifier.fillMaxSize().background(Background).statusBarsPadding().pointerInput(Unit) { detectTapGestures { } }) {
        Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 10.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("YouTop", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            GlassButton(Icons.Rounded.Close, stringResource(R.string.close), onClose)
        }
        Row(Modifier.padding(horizontal = 20.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(R.string.books, R.string.saved, R.string.study, R.string.sync).forEachIndexed { i, label ->
                val selected = tab == i
                Text(
                    stringResource(label),
                    color = if (selected) Color.Black else Color.White,
                    fontSize = 14.sp, fontWeight = FontWeight.Medium,
                    modifier = Modifier.clip(CircleShape)
                        .background(if (selected) Color.White else Color.White.copy(alpha = 0.10f))
                        .clickable { tab = i }
                        .padding(horizontal = 16.dp, vertical = 7.dp),
                )
            }
        }
        when (tab) {
            0 -> BooksTab(vm, books, settings.saved, open)

            1 -> {
                val saved = remember(books, settings.saved) { books.flatten().filter { it.key in settings.saved } }
                if (saved.isEmpty()) {
                    Text(stringResource(R.string.no_saved), color = Faint, modifier = Modifier.padding(20.dp))
                } else {
                    LazyColumn(contentPadding = PaddingValues(bottom = 32.dp), modifier = Modifier.navigationBarsPadding()) {
                        items(saved, key = { it.key }) { item -> LessonRow(vm, item, true, null, 0L) { open(item) } }
                    }
                }
            }

            2 -> StudyTab(vm)

            else -> SyncTab(vm)
        }
    }
}

@Composable
private fun SyncTab(vm: MainViewModel) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val sync by vm.sync.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val focus = LocalFocusManager.current
    var link by remember(settings.driveFolder) { mutableStateOf(settings.driveFolder) }
    var mediaSize by remember(sync) { mutableStateOf(vm.media.sizeBytes()) }
    val running = sync is SyncState.Running

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding().navigationBarsPadding()
            .padding(horizontal = 20.dp).padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        OutlinedTextField(
            value = link,
            onValueChange = { link = it },
            label = { Text(stringResource(R.string.drive_folder)) },
            supportingText = { Text(stringResource(R.string.drive_hint)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { vm.setDriveFolder(link); focus.clearFocus() }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                focusedBorderColor = Color.White, unfocusedBorderColor = Color.White.copy(alpha = 0.4f),
                focusedLabelColor = Color.White, unfocusedLabelColor = Faint,
                focusedSupportingTextColor = Faint, unfocusedSupportingTextColor = Faint,
                cursorColor = Color.White,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            onClick = {
                focus.clearFocus()
                vm.setDriveFolder(link).invokeOnCompletion { vm.syncNow() }
            },
            enabled = !running && link.isNotBlank(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White, contentColor = Color.Black,
                disabledContainerColor = Color.White.copy(alpha = 0.2f), disabledContentColor = Color.White.copy(alpha = 0.6f),
            ),
            modifier = Modifier.fillMaxWidth().height(48.dp),
        ) {
            if (running) {
                CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
            } else {
                Icon(Icons.Rounded.Sync, null, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.sync_now), fontWeight = FontWeight.SemiBold)
        }
        Text(
            when (val s = sync) {
                is SyncState.Running ->
                    stringResource(R.string.sync_running) + if (s.total > 0) "  ${s.done}/${s.total}" else ""
                is SyncState.Done -> stringResource(R.string.sync_done, s.books)
                is SyncState.Failed ->
                    if (s.offline) stringResource(R.string.sync_offline) + "\n" + s.message else s.message
                SyncState.Idle -> when {
                    link.isBlank() -> stringResource(R.string.sync_no_folder)
                    settings.lastSync == 0L -> stringResource(R.string.sync_never)
                    else -> stringResource(R.string.sync_last, DateUtils.getRelativeTimeSpanString(settings.lastSync).toString())
                }
            },
            color = if (sync is SyncState.Failed) Color(0xFFFCA5A5) else Faint,
            fontSize = 13.sp,
        )
        SwitchRow(stringResource(R.string.auto_sync), settings.autoSync) { vm.setAutoSync(it) }
        SwitchRow(stringResource(R.string.offline_download), settings.offlineDownload) { vm.setOfflineDownload(it) }
        TextButton(onClick = { vm.clearMedia().invokeOnCompletion { mediaSize = vm.media.sizeBytes() } }) {
            Text(
                stringResource(R.string.clear_cache) + " · " + Formatter.formatShortFileSize(context, mediaSize),
                color = Color.White.copy(alpha = 0.85f),
            )
        }
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Color.White, fontSize = 15.sp, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
