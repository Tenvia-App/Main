package app.tenvia.data

/**
 * TESTER FEATURE: turns a chat into plain text that a tester can choose to send to the Tenvia team
 * through the phone's own share menu (email, messages...). The app itself never sends anything.
 * The format ("Person:" / "Tenvia:" lines) is easy to turn into a test or teaching conversation.
 */
object ChatExport {
    data class Turn(val fromPerson: Boolean, val text: String)

    fun text(turns: List<Turn>, appVersion: String, model: String, notice: String?, date: String): String {
        val sb = StringBuilder()
        sb.append("Tenvia tester chat, shared on purpose by the tester.\n")
        sb.append("App ").append(appVersion).append(", model ").append(model).append(", ").append(date).append("\n")
        if (!notice.isNullOrBlank()) sb.append("Notice on screen: ").append(notice.trim()).append("\n")
        sb.append("\n")
        for (t in turns) {
            sb.append(if (t.fromPerson) "Person: " else "Tenvia: ").append(t.text.trim()).append("\n\n")
        }
        return sb.toString().trimEnd() + "\n"
    }
}
