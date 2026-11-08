package app.tenvia.data

/**
 * Finds the right guides from a person's own words, with no AI.
 * This is the fallback that works on every phone. It scores each guide by how
 * many of its keywords appear in what the person typed.
 */
data class MatchResult(
    val guides: List<Guide>,
    val outOfScope: List<OutOfScope>,
)

class Matcher(private val library: GuideLibrary) {

    fun match(text: String, maxResults: Int = 3): MatchResult {
        val tokens = tokenize(text)
        if (tokens.isEmpty()) return MatchResult(emptyList(), emptyList())
        val phrase = tokens.joinToString(" ")

        val outOfScope = library.outOfScope.filter { oos ->
            // Remove negated phrases first ("I'm not hurt"), then look for warning words.
            var p = " $phrase "
            oos.negations.forEach { p = p.replace(" ${it.lowercase()} ", " ") }
            val t = p.trim().split(' ').filter { it.isNotBlank() }
            score(oos.keywords, t, t.joinToString(" ")) > 0
        }

        val guides = library.guides
            .map { it to score(it.keywords, tokens, phrase) }
            .filter { it.second > 0 }
            .sortedByDescending { it.second }
            .take(maxResults)
            .map { it.first }

        return MatchResult(guides, outOfScope)
    }

    private fun score(keywords: List<String>, tokens: List<String>, phrase: String): Int =
        keywords.sumOf { raw ->
            val kw = raw.lowercase().trim()
            when {
                kw.isEmpty() -> 0
                // Multi-word keywords ("broke down") must appear as a phrase.
                ' ' in kw -> if (" $phrase ".contains(" $kw ")) 3 else 0
                tokens.any { it == kw } -> 2
                // Simple word endings: "bleed" matches "bleeding", "freez" matches "freezing".
                kw.length >= 4 && tokens.any { it.startsWith(kw) } -> 1
                else -> 0
            }
        }

    companion object {
        fun tokenize(text: String): List<String> =
            text.lowercase()
                .replace("'", "")
                .split(Regex("[^\\p{L}\\p{N}]+"))
                .filter { it.isNotBlank() }
    }
}
