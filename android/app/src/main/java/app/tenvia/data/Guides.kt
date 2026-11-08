package app.tenvia.data

import org.json.JSONArray
import org.json.JSONObject

/** One step-by-step guide from the vetted guide library. */
data class Guide(
    val id: String,
    val title: String,
    val summary: String,
    val keywords: List<String>,
    val steps: List<String>,
    /** True until the content has been sourced and reviewed. */
    val draft: Boolean,
)

/** A kind of situation Tenvia must NOT guide yet (for example medical problems). */
data class OutOfScope(
    val id: String,
    val keywords: List<String>,
    val message: String,
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
                steps = g.getJSONArray("steps").strings(),
                draft = g.optBoolean("draft", true),
            )
        },
        outOfScope = root.getJSONArray("outOfScope").objects().map { o ->
            OutOfScope(
                id = o.getString("id"),
                keywords = o.getJSONArray("keywords").strings(),
                message = o.getString("message"),
            )
        },
    )
}

private fun JSONArray.objects(): List<JSONObject> = (0 until length()).map { getJSONObject(it) }
private fun JSONArray.strings(): List<String> = (0 until length()).map { getString(it) }
