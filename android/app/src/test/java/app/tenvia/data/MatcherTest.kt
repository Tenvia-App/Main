package app.tenvia.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MatcherTest {

    private fun guide(id: String, vararg keywords: String) =
        Guide(id, id, "", keywords.toList(), listOf(Step("step", "https://www.nps.gov/x")), draft = true)

    @Test fun showsShortSourceName() {
        assertEquals("nps.gov", Step("t", "https://www.nps.gov/articles/x.htm").sourceName)
        assertEquals("911.gov", Step("t", "https://www.911.gov/calling-911/").sourceName)
    }

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

    @Test fun flagsUnconsciousDescribedInOwnWords() {
        // Found by the milestone 2 AI test: "not waking up" wasn't caught before.
        val m = Matcher(library.copy(outOfScope = listOf(OutOfScope("medical", listOf("not waking", "fell"), "medical"))))
        assertEquals(listOf("medical"), m.match("my friend fell down the rocks and he's not waking up").outOfScope.map { it.id })
    }

    @Test fun negatedInjuryIsNotFlagged() {
        val m = Matcher(library.copy(outOfScope = listOf(OutOfScope("medical", listOf("hurt"), "medical", negations = listOf("not hurt")))))
        assertTrue(m.match("I know I'm not hurt but I'm so overwhelmed").outOfScope.isEmpty())
        assertEquals(listOf("medical"), m.match("my leg is hurt").outOfScope.map { it.id })
    }

    @Test fun flagsAllergyWarningSigns() {
        // Missed in milestone 2 round 3: swelling + wheezing (sourced from CDC warning signs).
        val m = Matcher(library.copy(outOfScope = listOf(OutOfScope("medical", listOf("swell", "wheez"), "medical"))))
        assertEquals(listOf("medical"), m.match("my lips are swelling and I'm wheezing").outOfScope.map { it.id })
    }

    @Test fun flagsHeatStrokeAndCarbonMonoxideSigns() {
        // Missed in AI round 6 (CDC warning signs): heat stroke and CO poisoning.
        val m = Matcher(library.copy(outOfScope = listOf(OutOfScope("medical", listOf("confused", "headache", "generator", "hot and dry"), "medical"))))
        assertEquals(listOf("medical"), m.match("my skin is hot and dry and I'm confused").outOfScope.map { it.id })
        assertEquals(listOf("medical"), m.match("headache and nausea, generator running by the door").outOfScope.map { it.id })
    }

    @Test fun flagsHypothermiaSigns() {
        // Missed in AI round 7: "mumbling, stumbling" (CDC + CT.gov hypothermia signs).
        val m = Matcher(library.copy(outOfScope = listOf(OutOfScope("medical", listOf("mumbl", "stumbl", "fumbl", "drowsy"), "medical"))))
        assertEquals(listOf("medical"), m.match("he's mumbling, stumbling, and can't stop shaking").outOfScope.map { it.id })
    }

    @Test fun crisisPhraseFlagsButFearOfDyingDoesNot() {
        // Round 8: "keep myself safe" is a crisis phrase; "I don't want to die out here" is fear, not self-harm.
        val crisis = OutOfScope("crisis", listOf("want to die", "keep myself safe"), "crisis", negations = listOf("dont want to die"))
        val m = Matcher(library.copy(outOfScope = listOf(crisis)))
        assertEquals(listOf("crisis"), m.match("I don't know how much longer I can keep myself safe tonight").outOfScope.map { it.id })
        assertTrue(m.match("I don't want to die out here, it's so cold").outOfScope.isEmpty())
        assertEquals(listOf("crisis"), m.match("I just want to die").outOfScope.map { it.id })
    }

    @Test fun realGuideFileParsesWithNegations() {
        val json = java.io.File("src/main/assets/guides.json").readText()
        val lib = parseGuideLibrary(json)
        val m = Matcher(lib)
        assertTrue(m.match("I don't want to die out here, I'm lost").outOfScope.none { it.id == "crisis" })
        assertTrue(m.match("I can't keep myself safe tonight").outOfScope.any { it.id == "crisis" })
        assertTrue(m.match("my friend is mumbling and stumbling after the freezing rain").outOfScope.any { it.id == "medical" })
    }

    @Test fun emptyOrUnknownTextMatchesNothing() {
        assertTrue(matcher.match("   ").guides.isEmpty())
        assertTrue(matcher.match("purple elephant").guides.isEmpty())
    }
}
