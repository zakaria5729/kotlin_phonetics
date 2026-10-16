package io.github.zakaria5729.phonetics

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EngineTest {
    private val bangla = PhoneticEngine(PhoneticLanguages.bangla)
    private val english = PhoneticEngine(PhoneticLanguages.english)

    @Test
    fun defaultsAreMinScore70AndMaxResults50() {
        val result = english.generate("Catherine")
        assertNull(result.error)
        assertTrue(result.matches.size <= 50)
        assertTrue(result.matches.all { it.score >= 70.0 })
        assertEquals(100.0, result.matches.first().score)
        assertEquals("Catherine", result.matches.first().word)
    }

    @Test
    fun banglaGroupsOfSoundAlikeLetters() {
        val words = bangla.generateWords("আসাদ", minScore = 90.0).words
        assertEquals(listOf("আসাদ", "আশাদ", "আষাদ"), words)
        assertEquals(85.0, bangla.similarity("আসাদ", "আছাদ").score)
    }

    @Test
    fun englishKeepsTheShapeOfTheInput() {
        assertEquals("JOHN", english.generate("JOHN").matches.first().word)
        assertEquals("John Smith", english.generate("John Smith").matches.first().word)
        assertEquals("john smith", english.generate("john smith").matches.first().word)
        assertContains(english.generateWords("Phone", minScore = 90.0).words, "Fone")
    }

    @Test
    fun includeOriginalAndMaxChangesAreHonoured() {
        val withoutOriginal = english.generate("Phone", includeOriginal = false)
        assertFalse(withoutOriginal.matches.any { it.word == "Phone" })
        val oneChange = english.generate("Mary Ann", minScore = 0.0, maxChanges = 1)
        assertTrue(oneChange.matches.all { it.changes.size <= 1 })
        val noChange = english.generate("Mary Ann", minScore = 0.0, maxChanges = 0)
        assertEquals(listOf("Mary Ann"), noChange.matches.map { it.word })
    }

    @Test
    fun changesDescribeHowAVariantWasBuilt() {
        val variant = english.generate("Phone", minScore = 90.0).matches.first { it.word == "Fone" }
        val change = variant.changes.single()
        assertEquals(0, change.position)
        assertEquals("ph", change.from.lowercase())
        assertEquals("f", change.to)
        assertEquals(0.95, change.similarity)
        assertEquals("ph→f", change.toString())
    }

    @Test
    fun matchEqualityAndText() {
        assertEquals(PhoneticMatch("a", 90.0), PhoneticMatch("a", 90.0, listOf(PhoneticChange(0, "x", "y", 0.9))))
        assertEquals("a (90.00%)", PhoneticMatch("a", 90.0).toString())
        assertEquals("a (87.55%)", PhoneticMatch("a", 87.55).toString())
        assertEquals("a (0.00%)", PhoneticMatch("a", 0.0).toString())
    }

    @Test
    fun badArgumentsComeBackAsErrors() {
        for (minScore in listOf(-1.0, 100.5, Double.NaN)) {
            val result = english.generate("a", minScore = minScore)
            assertIs<PhoneticArgumentException>(result.error)
            assertTrue(result.matches.isEmpty())
        }
        assertIs<PhoneticArgumentException>(english.generate("a", maxResults = 0).error)
        assertIs<PhoneticArgumentException>(english.generate("a", maxChanges = -1).error)
        assertIs<PhoneticArgumentException>(english.rank("a", listOf("b"), limit = 0).error)
        assertIs<PhoneticArgumentException>(english.rank("a", listOf("b"), minScore = 101.0).error)
        assertIs<PhoneticArgumentException>(english.matches("a", "b", minScore = -5.0).error)
    }

    @Test
    fun badSettingsAreKeptInTheErrorProperty() {
        for (bad in listOf(0.0, 1.0, -0.5, 2.0, Double.NaN)) {
            val engine = PhoneticEngine(PhoneticLanguages.english, gapSimilarity = bad)
            assertIs<PhoneticArgumentException>(engine.error)
            assertEquals(engine.error, engine.generate("a").error)
            assertEquals(engine.error, engine.similarity("a", "b").error)
            assertEquals(engine.error, engine.rank("a", listOf("b")).error)
        }
        assertNotNull(PhoneticEngine(PhoneticLanguages.english, markGapSimilarity = 1.5).error)
        assertNotNull(PhoneticEngine(PhoneticLanguages.english, unrelatedSimilarity = 0.0).error)
        assertNull(PhoneticEngine(PhoneticLanguages.english).error)
    }

    @Test
    fun rulesForAnotherLanguageAreRejected() {
        val engine = PhoneticEngine(PhoneticLanguages.english, rules = PhoneticLanguages.bangla.defaultRuleSet)
        assertNotNull(engine.error)
        assertTrue(engine.error!!.message.contains("\"bn\""))
    }

    @Test
    fun aBrokenLanguageNeverThrows() {
        val language = PhoneticLanguage(code = "", name = "Empty", rules = emptyList(), canonicalSequences = mapOf("" to "x"))
        assertNotNull(language.error)
        val engine = PhoneticEngine(language)
        assertNotNull(engine.error)
        assertNotNull(engine.generate("abc").error)
    }

    @Test
    fun badRulesAreReportedNotThrown() {
        val language = PhoneticLanguage("xx", "Test", emptyList())
        val rules = PhoneticRuleSet(
            listOf(
                PhoneticRule(listOf("a", "b"), 1.0),
                PhoneticRule(listOf("a", "b"), Double.NaN),
                PhoneticRule(listOf("a"), 0.5),
                PhoneticRule(listOf("", "b"), 0.5),
                PhoneticRule(listOf("c", "d"), 0.5),
            ),
            language,
        )
        val message = rules.error!!.message
        assertTrue(message.contains("similarity must be greater than 0 and less than 1"))
        assertTrue(message.contains("at least two different spellings"))
        assertTrue(message.contains("must not be empty"))
        assertEquals(1, rules.rules.size)
        assertTrue(rules.hasRuleFor("c"))
    }

    @Test
    fun ruleSetExtendKeepsTheOldRules() {
        val language = PhoneticLanguage("xx", "Test", emptyList())
        val rules = PhoneticRuleSet(listOf(PhoneticRule(listOf("a", "b"), 0.9)), language)
        val extended = rules.extend(listOf(PhoneticRule(listOf("c", "d"), 0.8)))
        assertTrue(extended.hasRuleFor("a"))
        assertTrue(extended.hasRuleFor("d"))
        assertFalse(rules.hasRuleFor("c"))
        assertEquals(listOf("b"), extended.alternativesFor("a").map { it.text })
    }

    @Test
    fun scoreOnlyRulesDoNotBuildNewSpellings() {
        val language = PhoneticLanguage(
            "xx",
            "Test",
            listOf(
                PhoneticRule(listOf("a", "b"), 0.9),
                PhoneticRule(listOf("c", "d"), 0.9, scoreOnly = true),
            ),
        )
        val engine = PhoneticEngine(language)
        assertEquals(listOf("ac", "bc"), engine.generateWords("ac").words)
        assertEquals(90.0, engine.similarity("c", "d").score)
    }

    @Test
    fun rulePositionsLimitWhereARuleApplies() {
        val language = PhoneticLanguage(
            "xx",
            "Test",
            listOf(
                PhoneticRule(listOf("a", "b"), 0.9, position = RulePosition.WordStart),
                PhoneticRule(listOf("x", "y"), 0.9, position = RulePosition.WordEnd),
                PhoneticRule(listOf("p", "q"), 0.9, position = RulePosition.NotWordStart),
            ),
        )
        val engine = PhoneticEngine(language)
        assertEquals(listOf("aa", "ba"), engine.generateWords("aa").words)
        assertEquals(listOf("xx", "xy"), engine.generateWords("xx").words)
        assertEquals(listOf("pp", "pq"), engine.generateWords("pp").words)
        assertEquals(listOf("p"), engine.generateWords("p").words)
    }

    @Test
    fun rankSortsByScoreThenWordAndDropsDuplicates() {
        val ranked = english.rank("Smith", listOf("Smyth", "Smith", "Smith", "Brown", "Smithe"), minScore = 50.0)
        assertEquals("Smith", ranked.matches.first().word)
        assertEquals(100.0, ranked.matches.first().score)
        assertEquals(ranked.matches.map { it.word }.distinct(), ranked.matches.map { it.word })
        assertFalse(ranked.matches.any { it.word == "Brown" })
        val limited = english.rank("Smith", listOf("Smyth", "Smith", "Smithe"), limit = 2)
        assertEquals(2, limited.matches.size)
    }

    @Test
    fun emptyAndOddInputsNeverThrow() {
        for (text in listOf("", " ", "   ", "\u0000", "\uD83D", "😀", "-", "'")) {
            assertNull(english.generate(text).error)
            assertNull(bangla.generate(text).error)
            assertNull(english.similarity(text, "abc").error)
            assertNull(english.rank(text, listOf("abc", text)).error)
        }
        assertTrue(english.generate("").matches.isEmpty())
        assertEquals(0.0, english.similarity("", "abc").score)
        assertEquals(100.0, english.similarity("", "  ").score)
    }

    @Test
    fun hugeLimitsAreCapped() {
        val result = english.generate("Mary Ann Elizabeth Catherine", minScore = 0.0, maxResults = null)
        assertNull(result.error)
        assertTrue(result.matches.size <= 100_000)
    }

    @Test
    fun customLanguageWorks() {
        val language = PhoneticLanguage(
            code = "xx",
            name = "Test",
            foldCase = true,
            canonicalSequences = mapOf("ß" to "ss"),
            marks = setOf(0x2D),
            rules = listOf(
                PhoneticRule(listOf("q", "x"), 0.9),
                PhoneticRule(listOf("ee", "i"), 0.8, position = RulePosition.WordEnd),
            ),
        )
        val engine = PhoneticEngine(language)
        assertNull(engine.error)
        assertEquals(listOf("Qa" to 100.0, "Xa" to 90.0), engine.generate("Qa").matches.map { it.word to it.score })
        assertContentEquals(listOf("bee", "bi"), engine.generate("bee").matches.map { it.word })
        assertEquals(listOf("beer"), engine.generate("beer").matches.map { it.word })
        assertEquals("STRASSE", engine.generate("STRAßE").matches.first().word)
        assertEquals(90.0, engine.similarity("Qa", "xA").score)
    }
}
