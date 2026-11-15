package app.youtop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Sell
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.youtop.MainViewModel
import app.youtop.R
import app.youtop.data.FeedItem
import app.youtop.data.flatten

/** The library's pages, in the order of the bar at the bottom. */
enum class LibraryTab(val icon: ImageVector, val label: Int) {
    HOME(Icons.Rounded.Home, R.string.home),
    BOOKS(Icons.AutoMirrored.Rounded.MenuBook, R.string.books),
    TOPICS(Icons.Rounded.Sell, R.string.topics),
    SAVED(Icons.Rounded.Bookmark, R.string.saved),
    SETTINGS(Icons.Rounded.Tune, R.string.settings),
}

@Composable
fun LibraryScreen(vm: MainViewModel, tab: LibraryTab, onTab: (LibraryTab) -> Unit, onClose: () -> Unit) {
    val books by vm.books.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    // Which book is open on the Books page; kept here so the dashboard can open one.
    var openBook by rememberSaveable { mutableStateOf<String?>(null) }
    val open: (FeedItem) -> Unit = { vm.jumpTo(it.key); onClose() }

    Column(Modifier.fillMaxSize().background(Ink).statusBarsPadding().pointerInput(Unit) { detectTapGestures { } }) {
        Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 10.dp, top = 8.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (tab == LibraryTab.HOME) "YouTop" else stringResource(tab.label),
                color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.weight(1f))
            GlassButton(Icons.Rounded.Close, stringResource(R.string.close), onClose)
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when (tab) {
                LibraryTab.HOME -> HomeTab(
                    vm, onContinue = onClose,
                    onOpenBook = { openBook = it; onTab(LibraryTab.BOOKS) },
                    onOpenTopics = { onTab(LibraryTab.TOPICS) },
                )

                LibraryTab.BOOKS -> BooksTab(vm, books, settings.saved, openBook, { openBook = it }, open)

                LibraryTab.TOPICS -> TopicsTab(vm)

                LibraryTab.SAVED -> {
                    val saved = remember(books, settings.saved) { books.flatten().filter { it.key in settings.saved } }
                    if (saved.isEmpty()) {
                        EmptyState(Icons.Rounded.BookmarkBorder, stringResource(R.string.no_saved_title), stringResource(R.string.no_saved))
                    } else {
                        LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
                            items(saved, key = { it.key }) { item -> LessonRow(vm, item, true, null, 0L) { open(item) } }
                        }
                    }
                }

                LibraryTab.SETTINGS -> SettingsTab(vm)
            }
        }
        // The pages, always one tap away.
        Row(
            Modifier.fillMaxWidth().background(Surface1).navigationBarsPadding().padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceAround,
        ) {
            LibraryTab.entries.forEach { t ->
                val selected = t == tab
                val tint = if (selected) Accent else Faint
                Column(
                    Modifier.weight(1f).clip(CircleShape)
                        .clickable(remember { MutableInteractionSource() }, indication = null, role = Role.Tab) { onTab(t) }
                        .padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        Modifier.clip(CircleShape).background(if (selected) Accent.copy(alpha = 0.16f) else Color.Transparent)
                            .padding(horizontal = 18.dp, vertical = 4.dp),
                    ) { Icon(t.icon, null, tint = tint, modifier = Modifier.size(22.dp)) }
                    Spacer(Modifier.height(2.dp))
                    Text(stringResource(t.label), color = tint, fontSize = 11.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium, maxLines = 1)
                }
            }
        }
    }
}
