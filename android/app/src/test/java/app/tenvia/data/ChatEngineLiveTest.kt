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
}
