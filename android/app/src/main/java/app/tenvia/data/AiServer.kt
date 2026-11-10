package app.tenvia.data

import android.content.Context
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * TEST BUILD: starts the on-phone AI (llama.cpp server, bundled in the app as libllamaserver.so) as a
 * child process of the app, reachable at 127.0.0.1:8080 only. Started by the app itself, so it works
 * with the phone unplugged and after a restart (a server started over USB died with the cable).
 *
 * The model file is not in the app. It is looked for in the app's own folder
 * (Android/data/app.tenvia/files/models) and then in /data/local/tmp/tenvia-ai/models.
 */
class AiServer(private val context: Context, private val port: Int = 8080) {

    private var process: Process? = null

    var lastProblem: String = ""
        private set

    fun isUp(): Boolean = runCatching {
        val c = URL("http://127.0.0.1:$port/health").openConnection() as HttpURLConnection
        c.connectTimeout = 1500; c.readTimeout = 1500
        c.responseCode == 200
    }.getOrDefault(false)

    private fun findModel(): File? {
        val dirs = listOfNotNull(
            context.getExternalFilesDir(null)?.let { File(it, "models") },
            File("/data/local/tmp/tenvia-ai/models"),
        )
        return dirs.map { File(it, MODEL) }.firstOrNull { f ->
            f.isFile && runCatching { f.inputStream().use { it.read() } >= 0 }.getOrDefault(false)
        }
    }

    /** Blocking (call off the main thread). Returns true once the server answers. */
    @Synchronized
    fun ensureRunning(timeoutMs: Long = 120_000): Boolean {
        if (isUp()) return true
        val running = process?.let { runCatching { it.exitValue(); false }.getOrDefault(true) } ?: false
        if (!running) {
            val model = findModel() ?: run { lastProblem = "The AI model file isn't on this phone."; return false }
            val libDir = context.applicationInfo.nativeLibraryDir
            val exe = File(libDir, "libllamaserver.so")
            if (!exe.exists()) { lastProblem = "The AI program isn't included in this build."; return false }
            val log = File(context.filesDir, "ai-server.log")
            process = runCatching {
                ProcessBuilder(
                    exe.path, "-m", model.path, "--host", "127.0.0.1", "--port", "$port",
                    "-c", "4096", "--parallel", "1", "-t", "4",
                    // No repacked copy of the weights (2.4 GB of extra memory): with it, Android's
                    // memory killer closed the app while the model loaded. The model is read from the
                    // file instead (~0.7 GB extra). Prompt reading is about 2x slower (14 vs 27 tok/s).
                    "--no-repack",
                ).apply {
                    environment()["LD_LIBRARY_PATH"] = libDir
                    directory(context.filesDir)
                    redirectErrorStream(true)
                    redirectOutput(log)
                }.start()
            }.getOrElse { lastProblem = "The AI couldn't start: ${it.message}"; return false }
        }
        val end = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < end) {
            if (isUp()) { lastProblem = ""; return true }
            val p = process
            if (p != null && runCatching { p.exitValue(); true }.getOrDefault(false)) {
                lastProblem = "The AI stopped while starting (see ai-server.log)."
                process = null
                return false
            }
            Thread.sleep(1000)
        }
        lastProblem = "The AI took too long to start."
        return false
    }

    fun stop() {
        process?.destroy()
        process = null
    }

    companion object {
        const val MODEL = "Qwen3-4B-Instruct-2507-Q4_K_M.gguf"
    }
}
