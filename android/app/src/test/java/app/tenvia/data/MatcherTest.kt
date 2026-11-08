package app.tenvia.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MatcherTest {

    private fun guide(id: String, vararg keywords: String) =
        Guide(id, id, "", keywords.toList(), listOf("step"), draft = true)

    private val library = GuideLibrary(
        version = 1,
        guides = listOf(
            guide("stay-or-move", "lost", "trail", "which way"),
            guide("cold-weather", "cold", "freez", "snow"),
            guide("stranded-vehicle", "car", "stuck", "broke down"),
        ),
        outOfScope = listOf(
            OutOfScope("medical", listOf("bleed", "broken", "not breathing"), "medical"),
        ),
    )
    private val matcher = Matcher(library)

    @Test fun findsGuideFromOwnWords() {
        val r = matcher.match("I'm lost and can't find the trail")
        assertEquals("stay-or-move", r.guides.first().id)
        assertTrue(r.outOfScope.isEmpty())
    }

    @Test fun matchesWordEndings() {
        val r = matcher.match("it's freezing out here")
        assertEquals("cold-weather", r.guides.first().id)
    }

    @Test fun matchesPhrasesOnlyAsWholePhrases() {
        assertEquals("stranded-vehicle", matcher.match("my truck broke down").guides.first().id)
        assertTrue(matcher.match("the down jacket broke").guides.isEmpty())
    }

    @Test fun ranksBestGuideFirst() {
        // cold-weather scores higher (cold + freezing + snow) than stranded-vehicle (stuck).
        val r = matcher.match("stuck in the snow, so cold and freezing")
        assertEquals(listOf("cold-weather", "stranded-vehicle"), r.guides.map { it.id })
    }

    @Test fun flagsMedicalAsOutOfScopeButStillMatchesGuides() {
        val r = matcher.match("lost on the trail and my leg is bleeding")
        assertEquals(listOf("medical"), r.outOfScope.map { it.id })
        assertEquals("stay-or-move", r.guides.first().id)
    }

    @Test fun emptyOrUnknownTextMatchesNothing() {
        assertTrue(matcher.match("   ").guides.isEmpty())
        assertTrue(matcher.match("purple elephant").guides.isEmpty())
    }
}
