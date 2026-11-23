package app.tenvia.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UpdateCheckTest {
    private fun rel(tag: String, draft: Boolean = false, url: String = "https://github.com/Tenvia-App/Main/releases/tag/$tag") =
        """{"tag_name":"$tag","draft":$draft,"html_url":"$url"}"""

    private fun list(vararg r: String) = "[" + r.joinToString(",") + "]"

    @Test fun findsNewerVersion() {
        val u = UpdateCheck.newestNewer(list(rel("v0.8.3-ai-test"), rel("v0.8.2-ai-test")), "0.8.2-ai-test")
        assertEquals("0.8.3-ai-test", u?.version)
        assertEquals("https://github.com/Tenvia-App/Main/releases/tag/v0.8.3-ai-test", u?.pageUrl)
    }

    @Test fun nothingWhenUpToDate() {
        assertNull(UpdateCheck.newestNewer(list(rel("v0.8.2-ai-test"), rel("v0.8.1-ai-test")), "0.8.2-ai-test"))
    }

    @Test fun picksHighestEvenIfListedOutOfOrder() {
        val u = UpdateCheck.newestNewer(list(rel("v0.8.3-ai-test"), rel("v0.10.0-ai-test"), rel("v0.9.1-ai-test")), "0.8.2-ai-test")
        assertEquals("0.10.0-ai-test", u?.version)
    }

    @Test fun skipsDraftsOddTagsAndForeignLinks() {
        assertNull(UpdateCheck.newestNewer(list(rel("v0.9.0-ai-test", draft = true)), "0.8.2-ai-test"))
        assertNull(UpdateCheck.newestNewer(list(rel("latest")), "0.8.2-ai-test"))
        assertNull(UpdateCheck.newestNewer(list(rel("v0.9.0", url = "https://evil.example/x")), "0.8.2-ai-test"))
    }

    @Test fun comparesNumbersNotText() {
        assertEquals(listOf(0, 10, 0), UpdateCheck.numbers("v0.10.0-ai-test"))
        assert(UpdateCheck.compare(listOf(0, 10, 0), listOf(0, 9, 9)) > 0)
        assertEquals(0, UpdateCheck.compare(listOf(1, 0), listOf(1, 0, 0)))
    }
}
