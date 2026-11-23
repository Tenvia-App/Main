package app.tenvia.data

import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import java.net.URL
import javax.net.ssl.HttpsURLConnection

/**
 * TEST BUILD ONLY: tells testers when a newer test version is out.
 *
 * Once each time the app starts, if the phone happens to be online, it asks GitHub for the list of
 * Tenvia releases (a public page anyone can read). Nothing about the person or their chats is sent:
 * GitHub only sees an ordinary web request, like opening the releases page in a browser. With no
 * internet it fails silently and the app works exactly the same.
 *
 * It never downloads or installs anything. It only shows a note with a button that opens the
 * release page, and the tester decides. The finished app (no internet permission) won't have this.
 */
object UpdateCheck {
    private const val RELEASES = "https://api.github.com/repos/Tenvia-App/Main/releases?per_page=10"

    data class Update(val version: String, val pageUrl: String)

    /** The newer version, once found. Read by the home screen. */
    var available by mutableStateOf<Update?>(null)
        private set

    /** "Not now" hides the note until the app is started again. */
    var dismissed by mutableStateOf(false)

    @Volatile private var started = false

    fun start(installedVersion: String) {
        if (started) return
        started = true
        Thread {
            val found = runCatching { newestNewer(fetch(), installedVersion) }.getOrNull()
            if (found != null) Handler(Looper.getMainLooper()).post { available = found }
        }.apply { isDaemon = true; name = "tenvia-update-check" }.start()
    }

    private fun fetch(): String {
        val c = URL(RELEASES).openConnection() as HttpsURLConnection
        try {
            c.connectTimeout = 8000
            c.readTimeout = 8000
            c.setRequestProperty("Accept", "application/vnd.github+json")
            c.setRequestProperty("User-Agent", "Tenvia-test-app")
            if (c.responseCode != 200) error("HTTP ${c.responseCode}")
            return c.inputStream.bufferedReader().use { it.readText() }
        } finally {
            c.disconnect()
        }
    }

    /** The highest published (not draft) release that is newer than [installed], or null. */
    fun newestNewer(json: String, installed: String): Update? {
        val mine = numbers(installed) ?: return null
        val arr = JSONArray(json)
        var best: Update? = null
        var bestNums: List<Int>? = null
        for (i in 0 until arr.length()) {
            val r = arr.getJSONObject(i)
            if (r.optBoolean("draft")) continue
            val tag = r.optString("tag_name")
            val nums = numbers(tag) ?: continue
            if (compare(nums, mine) <= 0) continue
            if (bestNums == null || compare(nums, bestNums) > 0) {
                bestNums = nums
                best = Update(tag.removePrefix("v"), r.optString("html_url"))
            }
        }
        return best?.takeIf { it.pageUrl.startsWith("https://github.com/Tenvia-App/Main/") }
    }

    /** "v0.8.3-ai-test" -> [0, 8, 3]. Null if it doesn't start with a version number. */
    fun numbers(version: String): List<Int>? {
        val m = Regex("""^v?(\d+(?:\.\d+)*)""").find(version.trim()) ?: return null
        return m.groupValues[1].split('.').map { it.toInt() }
    }

    fun compare(a: List<Int>, b: List<Int>): Int {
        for (i in 0 until maxOf(a.size, b.size)) {
            val d = a.getOrElse(i) { 0 } - b.getOrElse(i) { 0 }
            if (d != 0) return d
        }
        return 0
    }
}
