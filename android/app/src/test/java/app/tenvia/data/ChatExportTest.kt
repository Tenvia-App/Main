package app.tenvia.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatExportTest {
    private val turns = listOf(
        ChatExport.Turn(true, "I'm lost near the ridge. "),
        ChatExport.Turn(false, "Stop and take a moment to calmly look at your situation."),
    )

    @Test fun formatsTurnsAndHeader() {
        val s = ChatExport.text(turns, "0.8.0-ai-test", "tenvia-1.7b-v6", null, "2026-11-22")
        assertTrue(s.startsWith("Tenvia tester chat, shared on purpose by the tester.\n"))
        assertTrue(s.contains("App 0.8.0-ai-test, model tenvia-1.7b-v6, 2026-11-22\n"))
        assertTrue(s.contains("Person: I'm lost near the ridge.\n\nTenvia: Stop and take"))
        assertFalse(s.contains("Notice on screen"))
        assertTrue(s.endsWith("situation.\n"))
    }

    @Test fun includesNoticeWhenShown() {
        val s = ChatExport.text(turns, "v", "m", "  Call 911 if you can.  ", "d")
        assertTrue(s.contains("Notice on screen: Call 911 if you can.\n"))
    }

    @Test fun emptyChatHasOnlyHeader() {
        val s = ChatExport.text(emptyList(), "v", "m", null, "d")
        assertEquals(2, s.trim().lines().size)
    }
}
