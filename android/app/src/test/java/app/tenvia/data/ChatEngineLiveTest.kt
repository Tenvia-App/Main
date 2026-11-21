package app.tenvia.data

import org.junit.Assume.assumeTrue
import org.junit.Test

/**
 * Live check of the small-model mode against a real llama-server (skipped when none is running).
 * Start one with the small model on port 8083, then:
 *   gradlew testDebugUnitTest --tests app.tenvia.data.ChatEngineLiveTest -Dtenvia.live=1
 * Prints each reply and whether the model wrote it (true) or the app fell back to guide text (false).
 */
class ChatEngineLiveTest {
    @Test
    fun smallModelConversation() {
        val url = System.getenv("TENVIA_LIVE_URL") ?: "http://127.0.0.1:8083"
        val lib = parseGuideLibrary(java.io.File("src/main/assets/guides.json").readText())
        val engine = ChatEngine(lib, url, short = true)
        assumeTrue("no AI server at $url", engine.isAvailable())
        val turns = listOf(
            "lost the trail near a creek, getting dark, phone at 30%",
            "should i just walk downhill until i hit a road?",
            "i can hear a helicopter now",
        )
        var generated = 0
        for (t in turns) {
            val r = engine.respond(t)
            if (r.generated) generated++
            println("PERSON: $t\nAPP (${r.generated}): ${r.text}\n")
        }
        println("generated $generated of ${turns.size}")
    }

    /** Runs a conversation file (same format as ai-experiments/chat-sealed*.json) and writes the replies as
     *  JSON, to compare the app with the test pipeline: set TENVIA_LIVE_CONVS and TENVIA_LIVE_OUT. */
    @Test
    fun conversationFile() {
        val convs = System.getenv("TENVIA_LIVE_CONVS") ?: return
        val url = System.getenv("TENVIA_LIVE_URL") ?: "http://127.0.0.1:8083"
        val lib = parseGuideLibrary(java.io.File("src/main/assets/guides.json").readText())
        val engine = ChatEngine(lib, url, short = true)
        assumeTrue("no AI server at $url", engine.isAvailable())
        val list = org.json.JSONObject(java.io.File(convs).readText(Charsets.UTF_8)).getJSONArray("conversations")
        val out = org.json.JSONArray()
        for (i in 0 until list.length()) {
            val c = list.getJSONObject(i)
            engine.reset()
            val turns = org.json.JSONArray()
            val ts = c.getJSONArray("turns")
            for (k in 0 until ts.length()) {
                val r = engine.respond(ts.getString(k))
                turns.put(org.json.JSONObject().put("user", ts.getString(k)).put("reply", r.text).put("generated", r.generated)
                    .put("notice", r.notice ?: "").put("guides", org.json.JSONArray(r.guides.map { it.id })))
            }
            out.put(org.json.JSONObject().put("id", c.get("id")).put("turns", turns))
        }
        java.io.File(System.getenv("TENVIA_LIVE_OUT") ?: "build/live-out.json").writeText(out.toString(1), Charsets.UTF_8)
    }
}
