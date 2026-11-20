package app.tenvia.data

import android.content.Context
import androidx.compose.runtime.mutableStateOf

/**
 * TEST BUILD: the one on-phone AI shared by the background service and the chat screen.
 * prepare() starts the AI server and has it read its fixed instructions (warm-up), so a reply
 * doesn't wait for them. Started at phone start-up, after an app update, and when the app opens.
 */
object TenviaAi {
    enum class Status { Off, Starting, Warming, Ready, Failed }

    /** Read by the chat screen (Compose state, safe to write from a background thread). */
    val status = mutableStateOf(Status.Off)

    lateinit var library: GuideLibrary
        private set
    lateinit var server: AiServer
        private set
    lateinit var engine: ChatEngine
        private set

    @Synchronized
    fun init(context: Context) {
        if (::server.isInitialized) return
        val app = context.applicationContext
        library = app.assets.open("guides.json").bufferedReader().use { parseGuideLibrary(it.readText()) }
        server = AiServer(app)
        engine = ChatEngine(library, short = AiServer.SMALL)
    }

    /** Blocking (call off the main thread). Safe to call again: does nothing when the AI is ready. */
    @Synchronized
    fun prepare(context: Context): Boolean {
        init(context)
        if (status.value == Status.Ready && server.isUp()) return true
        status.value = Status.Starting
        if (!server.ensureRunning()) { status.value = Status.Failed; return false }
        status.value = Status.Warming
        runCatching { engine.warmUp() }
        status.value = Status.Ready
        return true
    }
}
