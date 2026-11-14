package app.youtop

import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import app.youtop.ui.FeedScreen
import app.youtop.ui.LibraryScreen

class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels()

    @androidx.annotation.VisibleForTesting
    fun viewModel(): MainViewModel = vm

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(primary = Color.White, onPrimary = Color.Black)) {
                var libraryOpen by rememberSaveable { mutableStateOf(false) }

                LifecycleEventEffect(Lifecycle.Event.ON_START) { vm.startAutoSync() }
                LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
                    vm.stopAutoSync()
                    vm.narrator.pause()
                }

                Box(Modifier.fillMaxSize()) {
                    FeedScreen(vm, onOpenLibrary = { vm.narrator.pause(); libraryOpen = true })
                    AnimatedVisibility(
                        visible = libraryOpen,
                        enter = slideInVertically(tween(280)) { it / 6 } + fadeIn(tween(200)),
                        exit = slideOutVertically(tween(220)) { it / 6 } + fadeOut(tween(180)),
                    ) {
                        LibraryScreen(vm, onClose = { libraryOpen = false })
                    }
                }
                BackHandler(enabled = libraryOpen) { libraryOpen = false }
            }
        }
    }
}
