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

    /** Set by the chat screen's "Try anyway": start the AI even on a phone with too little memory. */
    @Volatile var tryAnyway = false

    /** Blocking (call off the main thread). Safe to call again: does nothing when the AI is ready. */
    @Synchronized
    fun prepare(context: Context): Boolean {
        init(context)
        if (status.value == Status.Ready && server.isUp()) return true
        val level = PhoneCheck.level(context)
        if (level == PhoneCheck.Level.TooLow && !tryAnyway) {
            server.lastProblem = ""
            status.value = Status.Failed
            return false
        }
        status.value = Status.Starting
        if (!server.ensureRunning()) {
            if (level != PhoneCheck.Level.Ok && server.lastProblem.startsWith("The AI stopped")) {
                server.lastProblem = "The AI stopped while starting, probably because the phone ran out of memory. Close other apps and tap Retry."
            }
            status.value = Status.Failed
            return false
        }
        status.value = Status.Warming
        runCatching { engine.warmUp() }
        status.value = Status.Ready
        return true
    }
}
