package app.tenvia.data

import org.json.JSONArray
import org.json.JSONObject

/** One instruction, with the official page it was paraphrased from. */
data class Step(
    val text: String,
    val sourceUrl: String,
) {
    /** Short source name to show people, for example "nps.gov". */
    val sourceName: String
        get() = runCatching { java.net.URI(sourceUrl).host.removePrefix("www.") }.getOrDefault("")
}

/** One step-by-step guide from the vetted guide library. */
data class Guide(
    val id: String,
    val title: String,
    val summary: String,
    val keywords: List<String>,
    val steps: List<Step>,
    /** True until the content has been sourced and reviewed. */
    val draft: Boolean,
)

/** A kind of situation Tenvia must NOT guide yet (for example medical problems). */
data class OutOfScope(
    val id: String,
    val keywords: List<String>,
    val message: String,
    /** Phrases removed before matching, so "I'm not hurt" doesn't count as "hurt". */
    val negations: List<String> = emptyList(),
)

data class GuideLibrary(
    val version: Int,
    val guides: List<Guide>,
    val outOfScope: List<OutOfScope>,
)

/** Reads the guide library JSON that ships inside the app (assets/guides.json). */
fun parseGuideLibrary(json: String): GuideLibrary {
    val root = JSONObject(json)
    return GuideLibrary(
        version = root.getInt("version"),
        guides = root.getJSONArray("guides").objects().map { g ->
            Guide(
                id = g.getString("id"),
                title = g.getString("title"),
                summary = g.getString("summary"),
                keywords = g.getJSONArray("keywords").strings(),
                steps = g.getJSONArray("steps").objects().map { s ->
                    Step(text = s.getString("text"), sourceUrl = s.optString("source_url", ""))
                },
                draft = g.optBoolean("draft", true),
            )
        },
        outOfScope = root.getJSONArray("outOfScope").objects().map { o ->
            OutOfScope(
                id = o.getString("id"),
                keywords = o.getJSONArray("keywords").strings(),
                message = o.getString("message"),
                negations = o.optJSONArray("negations")?.strings() ?: emptyList(),
            )
        },
    )
}

private fun JSONArray.objects(): List<JSONObject> = (0 until length()).map { getJSONObject(it) }
private fun JSONArray.strings(): List<String> = (0 until length()).map { getString(it) }
