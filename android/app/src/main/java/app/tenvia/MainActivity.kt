package app.tenvia

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import app.tenvia.data.TenviaAi
import app.tenvia.data.Guide
import app.tenvia.ui.ChatScreen
import app.tenvia.ui.ChatUiState
import app.tenvia.data.MatchResult
import app.tenvia.data.Matcher
import app.tenvia.data.parseGuideLibrary
import app.tenvia.ui.GuideScreen
import app.tenvia.ui.HomeScreen
import app.tenvia.ui.Night
import app.tenvia.ui.Paper
import app.tenvia.ui.ResultsScreen
import app.tenvia.ui.TenviaTheme

private sealed interface Screen {
    data object Home : Screen
    data class Results(val result: MatchResult) : Screen
    data class GuideView(val guide: Guide, val back: Screen) : Screen
    data object Chat : Screen
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // The whole guide library ships inside the app; nothing is ever downloaded.
        TenviaAi.init(this)
        val library = TenviaAi.library
        val matcher = Matcher(library)
        val chatEngine = TenviaAi.engine
        // TEST BUILD: load and warm up the on-phone AI in the background right away.
        AiService.start(this)
        if (android.os.Build.VERSION.SDK_INT >= 33 && checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED)
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1)
        val chatState = ChatUiState()

        setContent {
            TenviaTheme {
                var screen by remember { mutableStateOf<Screen>(Screen.Home) }
                BackHandler(enabled = screen != Screen.Home) {
                    screen = when (val s = screen) {
                        is Screen.GuideView -> s.back
                        else -> Screen.Home
                    }
                }
                // Surface sets the default text color to light Paper for everything inside it.
                Surface(Modifier.fillMaxSize(), color = Night, contentColor = Paper) {
                Box(Modifier.fillMaxSize().safeDrawingPadding().imePadding()) {
                    when (val s = screen) {
                        Screen.Home -> HomeScreen(
                            guides = library.guides,
                            onSearch = { screen = Screen.Results(matcher.match(it)) },
                            onOpenGuide = { screen = Screen.GuideView(it, Screen.Home) },
                            onChat = { chatEngine.reset(); chatState.clear(); screen = Screen.Chat },
                        )
                        Screen.Chat -> ChatScreen(
                            engine = chatEngine,
                            state = chatState,
                            onOpenGuide = { screen = Screen.GuideView(it, Screen.Chat) },
                            onBack = { screen = Screen.Home },
                        )
                        is Screen.Results -> ResultsScreen(
                            result = s.result,
                            allGuides = library.guides,
                            onOpenGuide = { screen = Screen.GuideView(it, s) },
                            onBack = { screen = Screen.Home },
                        )
                        is Screen.GuideView -> GuideScreen(s.guide, onDone = { screen = s.back })
                    }
                }
                }
            }
        }
    }
}
