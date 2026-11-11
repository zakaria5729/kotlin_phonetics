package io.github.zakaria5729.phonetics

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CrossLanguageEnglishTest {
    private val engine = CrossLanguageEngine()
    private val englishOnly = setOf(PhoneticLanguages.english)

    private fun englishMatches(
        name: String,
        minScore: Double = 70.0,
        maxResults: Int? = 300,
        maxChanges: Int? = null,
    ): List<PhoneticMatch> {
        val result = engine.generate(name, englishOnly, minScore, maxResults, maxChanges = maxChanges)
        assertNull(result.error, "unexpected error for $name")
        return result.english.matches
    }

    private fun englishWords(name: String, minScore: Double = 70.0): List<String> =
        englishMatches(name, minScore).map { it.word }

    private val plausibleSpellingsByName: Map<String, List<String>> = mapOf(
        "মাসুদ" to listOf("Masud", "Masood", "Masoud", "Mashud"),
        "সাদ্দাম" to listOf("Saddam", "Sadam", "Sadaam"),
        "মোজাফফর" to listOf("Mozaffar", "Muzaffar", "Mozafar"),
        "মোহাম্মদ" to listOf("Mohammad", "Muhammad", "Mohammed", "Mohamad"),
        "আব্দুল" to listOf("Abdul", "Abdool"),
        "রহিম" to listOf("Rahim", "Raheem", "Rohim"),
        "করিম" to listOf("Karim", "Kareem"),
        "ফারজানা" to listOf("Farzana", "Farjana"),
        "নাসির" to listOf("Nasir", "Naseer", "Naser"),
        "শাকিব" to listOf("Shakib", "Sakib"),
        "জাহাঙ্গীর" to listOf("Jahangir", "Jahangeer"),
        "ওয়াসিম" to listOf("Wasim", "Waseem"),
        "রায়হান" to listOf("Rayhan", "Raihan"),
        "ইউসুফ" to listOf("Yusuf", "Yousuf"),
        "তানভীর" to listOf("Tanvir", "Tanbir"),
    )

    @Test
    fun commonBangladeshiNamesProduceTheirPlausibleEnglishSpellings() {
        for ((name, expected) in plausibleSpellingsByName) {
            val words = englishWords(name)
            for (spelling in expected) {
                assertTrue(spelling in words, "$name should produce $spelling but produced ${words.take(15)}")
            }
        }
    }

    @Test
    fun masudMatchesTheDocumentedExample() {
        val matches = englishMatches("মাসুদ", minScore = 90.0)
        assertEquals(listOf("Masud", "Masood", "Masoud", "Mashud"), matches.map { it.word }.take(4))
        assertEquals(100.0, matches.first().score)
        assertEquals(95.0, matches[1].score)
    }

    @Test
    fun theBestReadingIsTheOnlyHundredPercentMatch() {
        for (name in plausibleSpellingsByName.keys) {
            val matches = englishMatches(name)
            assertEquals(100.0, matches.first().score, name)
            assertTrue(matches.drop(1).all { it.score < 100.0 }, name)
            assertTrue(matches.first().changes.isEmpty(), name)
        }
    }

    @Test
    fun similarSoundingBanglaLettersGiveTheSameEnglishCandidates() {
        assertEquals(englishWords("মাশুদ"), englishWords("মাষুদ"))
        assertEquals(englishWords("টিপু", minScore = 85.0), englishWords("তিপু", minScore = 85.0))
        assertTrue("Tipu" in englishWords("টিপু") && "Tipu" in englishWords("তিপু"))
        assertTrue("Islam" in englishWords("ইছলাম"))
        assertTrue("Islam" in englishWords("ইশলাম"))
        assertTrue("Masud" in englishWords("মাশুদ", minScore = 90.0))
        assertTrue("Mashud" in englishWords("মাসুদ", minScore = 90.0))
    }

    @Test
    fun sameVowelSoundWrittenDifferentlyInBanglaGivesTheSameEnglish() {
        assertEquals(englishWords("রহিম"), englishWords("রহীম"))
        assertEquals(englishWords("মাসুদ"), englishWords("মাসূদ"))
        assertEquals(englishWords("আলী"), englishWords("আলি"))
    }

    @Test
    fun jaAndYaBothCoverTheJAndZSounds() {
        val withJa = englishWords("জাকির")
        val withYa = englishWords("যাকির")
        assertTrue("Jakir" in withJa && "Zakir" in withJa)
        assertTrue("Jakir" in withYa && "Zakir" in withYa)
    }

    @Test
    fun aspiratedAndUnaspiratedConsonantsAreBothOffered() {
        val words = englishWords("খালেদ")
        assertTrue("Khaled" in words)
        assertTrue("Kaled" in words)
        assertTrue("Dhaka" in englishWords("ঢাকা"))
        assertTrue("Daka" in englishWords("ঢাকা"))
    }

    @Test
    fun nasalSignsBecomeEnglishNasalSpellings() {
        assertTrue("Rongpur" in englishWords("রংপুর"))
        assertTrue("Rangpur" in englishWords("রংপুর"))
        assertTrue("Bangladesh" in englishWords("বাংলাদেশ"))
        assertEquals("Chand", englishWords("চাঁদ").first())
        assertTrue("Chad" in englishWords("চাঁদ"))
    }

    @Test
    fun doubledConsonantsFromConjunctsAreKeptOrReduced() {
        val saddam = englishMatches("সাদ্দাম")
        assertEquals("Saddam", saddam.first().word)
        val sadam = saddam.first { it.word == "Sadam" }
        assertEquals(90.0, sadam.score)
        assertTrue("Jannat" in englishWords("জান্নাত"))
        assertTrue("Janat" in englishWords("জান্নাত"))
        assertTrue("Abdullah" in englishWords("আব্দুল্লাহ"))
        assertTrue("Abdulah" in englishWords("আব্দুল্লাহ"))
    }

    @Test
    fun repeatedLettersWithoutAHasantaAreTreatedAsOneDoubledSound() {
        val words = englishWords("মোজাফফর")
        assertTrue("Mozaffar" in words)
        assertTrue("Mozafar" in words)
        assertTrue(words.none { it.contains("fff") })
    }

    @Test
    fun conjunctsWithSpecialSoundsAreHandledAsUnits() {
        assertTrue("Biggan" in englishWords("বিজ্ঞান"))
        assertTrue("Bigyan" in englishWords("বিজ্ঞান"))
        assertTrue("Raksha" in englishWords("রক্ষা"))
        assertTrue("Khoma" in englishWords("ক্ষমা"))
        assertTrue("Kshoma" in englishWords("ক্ষমা"))
        assertTrue("Ibrahim" in englishWords("ইব্রাহিম"))
        assertTrue("Swapan" in englishWords("স্বপন"))
        assertTrue("Sopon" in englishWords("স্বপন"))
        assertTrue("Mostafa" in englishWords("মোস্তফা"))
        assertTrue("Asaduzzaman" in englishWords("আসাদুজ্জামান"))
    }

    @Test
    fun inherentVowelsAreSpelledOrDroppedAccordingToThePronunciation() {
        assertEquals("Rohim", englishWords("রহিম").first())
        assertEquals("Farjana", englishWords("ফারজানা").first())
        assertEquals("Shanto", englishWords("শান্ত").first())
        assertEquals("Komol", englishWords("কমল").first())
        assertEquals("Mostofa", englishWords("মোস্তফা").first())
        assertEquals("Salma", englishWords("সালমা").first())
        assertEquals("Mohammod", englishWords("মোহাম্মদ").first())
    }

    @Test
    fun wordFinalConjunctsMayKeepOrDropTheirLastVowel() {
        val words = englishWords("শান্ত")
        assertTrue("Shanto" in words && "Shanta" in words && "Shant" in words)
    }

    @Test
    fun severalWordsAreCapitalisedAndKeepTheirSeparators() {
        val matches = englishMatches("মোহাম্মদ আব্দুল")
        assertEquals("Mohammod Abdul", matches.first().word)
        assertTrue("Muhammad Abdul" in matches.map { it.word })
        assertEquals("Abdul-Korim", englishMatches("আব্দুল-করিম").first().word)
        assertEquals("Mo. Hasan", englishMatches("মো. হাসান").first().word)
    }

    @Test
    fun digitsAreCarriedOver() {
        assertEquals("Masud123", englishMatches("মাসুদ১২৩").first().word)
    }

    @Test
    fun differentUnicodeFormsOfTheSameNameGiveTheSameResults() {
        assertEquals(englishWords("বড়"), englishWords("বড়"))
        assertEquals(englishWords("রহিম"), englishWords("রহ‌িম"))
        assertEquals(englishWords("মোহাম্মদ"), englishWords("মোহাম্মদ"))
        assertEquals(englishWords("আসাদ"), englishWords("  আসাদ  "))
    }

    @Test
    fun resultsHaveNoDuplicatesAndAreSortedBestFirst() {
        for (name in plausibleSpellingsByName.keys + listOf("মোহাম্মদ আব্দুল", "আসাদুজ্জামান")) {
            val matches = englishMatches(name, minScore = 50.0, maxResults = 500)
            val words = matches.map { it.word }
            assertEquals(words.size, words.toSet().size, "duplicates for $name")
            assertEquals(matches.sortedByDescending { it.score }.map { it.score }, matches.map { it.score }, name)
            assertTrue(matches.all { it.score in 50.0..100.0 }, name)
        }
    }

    @Test
    fun theOrderIsTheSameOnEveryRun() {
        for (name in plausibleSpellingsByName.keys) {
            assertEquals(englishMatches(name), englishMatches(name))
            assertEquals(
                englishMatches(name).map { it.word to it.score },
                englishMatches(name).map { it.word to it.score },
            )
        }
    }

    @Test
    fun scoresAreTheProductOfTheChosenAlternatives() {
        val masood = englishMatches("মাসুদ").first { it.word == "Masood" }
        assertEquals(95.0, masood.score)
        assertEquals(1, masood.changes.size)
        assertEquals("u", masood.changes.single().from)
        assertEquals("oo", masood.changes.single().to)

        val mashood = englishMatches("মাসুদ").first { it.word == "Mashood" }
        assertEquals(85.5, mashood.score)
        assertEquals(2, mashood.changes.size)
    }

    @Test
    fun maxChangesLimitsHowFarAnEnglishSpellingMayDriftFromTheBestReading() {
        assertEquals(listOf("Masud"), englishMatches("মাসুদ", minScore = 0.0, maxChanges = 0).map { it.word })
        val oneChange = englishMatches("মাসুদ", minScore = 0.0, maxChanges = 1)
        assertTrue(oneChange.all { it.changes.size <= 1 })
        assertTrue(oneChange.size > 1)
    }

    @Test
    fun minScoreFiltersEnglishSpellings() {
        val strict = englishMatches("মাসুদ", minScore = 95.0)
        assertEquals(listOf("Masud", "Masood"), strict.map { it.word })
        assertTrue(englishMatches("মাসুদ", minScore = 0.0).size > englishMatches("মাসুদ", minScore = 90.0).size)
    }

    @Test
    fun maxResultsLimitsEnglishSpellings() {
        assertEquals(3, englishMatches("মাসুদ", minScore = 0.0, maxResults = 3).size)
        assertEquals(1, englishMatches("মাসুদ", minScore = 0.0, maxResults = 1).size)
    }

    @Test
    fun emptyAndBlankInputGiveNoEnglishSpellingsAndNoError() {
        for (blank in listOf("", " ", "   \t\n")) {
            val result = engine.generate(blank, englishOnly, 70.0, 50)
            assertNull(result.error)
            assertTrue(result.english.matches.isEmpty())
        }
    }

    @Test
    fun inputThatIsNotBanglaIsReportedAsAnError() {
        for (invalid in listOf("12345", "مسعود", "मसूद", "মাসুদ😀")) {
            val result = engine.generate(invalid, englishOnly, 70.0, 50)
            assertIs<PhoneticArgumentException>(result.english.error, invalid)
            assertTrue(result.english.matches.isEmpty(), invalid)
        }
    }

    @Test
    fun strayBanglaMarksAreIgnoredInsteadOfFailing() {
        val loneVowelSign = engine.generate("া", englishOnly, 70.0, 50)
        assertNull(loneVowelSign.error)
        assertTrue(loneVowelSign.english.matches.isEmpty())
        assertEquals("Masud", englishMatches("মাসুদ্").first().word)
        assertEquals("K", englishMatches("ক্").first().word)
    }

    @Test
    fun aVeryLongInputStaysBoundedByTheResultLimit() {
        val longName = List(40) { "আসাদুজ্জামান" }.joinToString(" ")
        val matches = englishMatches(longName, minScore = 0.0, maxResults = 200)
        assertTrue(matches.size <= 200)
        assertTrue(matches.isNotEmpty())
        assertEquals(matches.size, matches.map { it.word }.toSet().size)
    }
}
