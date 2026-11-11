package io.github.zakaria5729.phonetics

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CrossLanguageBanglaFromEnglishTest {
    private val engine = CrossLanguageEngine()
    private val englishEngine = PhoneticEngine(PhoneticLanguages.english)
    private val banglaOnly = setOf(PhoneticLanguages.bangla)
    private val bothLanguages = setOf(PhoneticLanguages.bangla, PhoneticLanguages.english)

    private fun banglaMatches(
        name: String,
        minScore: Double = 70.0,
        maxResults: Int? = 300,
        maxChanges: Int? = null,
    ): List<PhoneticMatch> {
        val result = engine.generate(name, banglaOnly, minScore, maxResults, maxChanges = maxChanges)
        assertNull(result.error, "unexpected error for $name")
        return result.bangla.matches
    }

    private fun banglaWords(name: String, minScore: Double = 70.0): List<String> =
        banglaMatches(name, minScore).map { it.word }

    private fun bangla(text: String): String = PhoneticLanguages.bangla.normalize(text)

    private val plausibleBanglaByEnglishName: Map<String, List<String>> = mapOf(
        "Masud" to listOf("মাসুদ", "মাসূদ", "মাশুদ", "মাষুদ"),
        "Saddam" to listOf("সাদ্দাম", "সাদাম"),
        "Mozaffar" to listOf("মোজাফ্ফার", "মোজাফ্ফর"),
        "Mohammad" to listOf("মোহাম্মদ", "মোহাম্মাদ"),
        "Abdul" to listOf("আব্দুল", "আব্দূল"),
        "Rahim" to listOf("রহিম", "রাহিম", "রহীম"),
        "Karim" to listOf("করিম", "কারিম"),
        "Farzana" to listOf("ফারজানা", "ফরজানা"),
        "Nasir" to listOf("নাসির", "নাসীর"),
        "Shakib" to listOf("শাকিব", "সাকিব"),
        "Mostafa" to listOf("মোস্তাফা", "মোস্তফা"),
        "Hossain" to listOf("হোসাইন"),
        "Wasim" to listOf("ওয়াসিম"),
        "Yusuf" to listOf("ইউসুফ"),
        "Ayesha" to listOf("আয়েশা"),
        "Jahangir" to listOf("জাহাঙ্গীর", "জাহাঙ্গির"),
        "Stephen" to listOf("স্টেফেন"),
        "Smith" to listOf("স্মিথ"),
        "Philip" to listOf("ফিলিপ"),
    )

    @Test
    fun englishNamesProduceTheirPlausibleBanglaSpellings() {
        for ((name, expected) in plausibleBanglaByEnglishName) {
            val words = banglaWords(name)
            for (spelling in expected) {
                assertTrue(bangla(spelling) in words, "$name should produce $spelling but produced ${words.take(15)}")
            }
        }
    }

    @Test
    fun theBestBanglaReadingIsTheOnlyHundredPercentMatch() {
        for (name in plausibleBanglaByEnglishName.keys) {
            val matches = banglaMatches(name)
            assertEquals(100.0, matches.first().score, name)
            assertTrue(matches.drop(1).all { it.score < 100.0 }, name)
            assertTrue(matches.first().changes.isEmpty(), name)
        }
    }

    @Test
    fun masudMatchesTheDocumentedExample() {
        val matches = banglaMatches("Masud", minScore = 90.0)
        assertEquals("মাসুদ", matches.first().word)
        assertEquals(100.0, matches.first().score)
        assertTrue(listOf("মাসূদ", "মাশুদ", "মাষুদ").all { it in matches.map { match -> match.word } })
    }

    @Test
    fun upperAndLowerCaseGiveTheSameBanglaSpellings() {
        assertEquals(banglaMatches("Masud"), banglaMatches("MASUD"))
        assertEquals(banglaMatches("Masud"), banglaMatches("masud"))
        assertEquals(banglaMatches("Jose"), banglaMatches("José"))
    }

    @Test
    fun differentEnglishSpellingsOfOneSoundReachTheSameBanglaName() {
        assertTrue("মাসুদ" in banglaWords("Masood"))
        assertTrue("মাসুদ" in banglaWords("Masoud"))
        assertTrue("মাহমুদ" in banglaWords("Mahmood"))
        assertTrue("মাহমুদ" in banglaWords("Mahmoud"))
        assertTrue("রহিম" in banglaWords("Raheem"))
        assertTrue("ফিলিপ" in banglaWords("Phillip"))
        assertTrue("ফিলিপ" in banglaWords("Filip"))
    }

    @Test
    fun consonantsThatHaveSeveralBanglaLettersOfferThemAll() {
        val sWords = banglaWords("Masud")
        assertTrue("মাসুদ" in sWords && "মাশুদ" in sWords && "মাষুদ" in sWords)
        val tWords = banglaWords("Tipu")
        assertTrue("তিপু" in tWords && "টিপু" in tWords)
        assertTrue("জাকির" in banglaWords("Zakir") && "যাকির" in banglaWords("Zakir"))
    }

    @Test
    fun conjunctsAreFormedWhereBanglaSpellingUsesThem() {
        assertEquals("আব্দুল", banglaWords("Abdul").first())
        assertEquals("মোস্তাফা", banglaWords("Mostafa").first())
        assertEquals("স্মিথ", banglaWords("Smith").first())
        assertEquals("সাদ্দাম", banglaWords("Saddam").first())
        assertEquals("ইসলাম", banglaWords("Islam").first())
        assertEquals("সালমান", banglaWords("Salman").first())
        assertEquals("আহমেদ", banglaWords("Ahmed").first())
    }

    @Test
    fun doubledEnglishLettersAreKeptOrReducedInBangla() {
        val words = banglaWords("Saddam")
        assertTrue("সাদ্দাম" in words && "সাদাম" in words)
        assertTrue("হোসাইন" in banglaWords("Hossain"))
    }

    @Test
    fun nasalNgBecomesTheRightBanglaForm() {
        assertEquals("বাংলা", banglaWords("Bangla").first())
        assertEquals("রংপুর", banglaWords("Rongpur").first())
        assertEquals("জাহাঙ্গির", banglaWords("Jahangir").first())
    }

    @Test
    fun theVowelSignOrTheStandaloneLetterIsChosenFromTheContext() {
        assertEquals(bangla("আয়েশা"), banglaWords("Ayesha").first())
        assertEquals(bangla("ওয়াসিম"), banglaWords("Wasim").first())
        assertEquals("ইউসুফ", banglaWords("Yusuf").first())
        assertEquals(bangla("রায়হান"), banglaWords("Rayhan").first())
        assertEquals("রাইসা", banglaWords("Raisa").first())
        assertEquals("চৌধুরি", banglaWords("Chowdhury").first())
    }

    @Test
    fun severalWordsKeepTheirSeparators() {
        assertEquals("আব্দুল কারিম", banglaWords("Abdul Karim").first())
        assertEquals("আব্দুল-কারিম", banglaWords("Abdul-Karim").first())
        assertEquals("মাসুদ123", banglaWords("Masud123").first())
    }

    @Test
    fun everyGeneratedBanglaSpellingIsStructurallyValid() {
        for (name in plausibleBanglaByEnglishName.keys + listOf("Abdul Karim", "Chowdhury", "Rahman", "Tanvir")) {
            for (match in banglaMatches(name, minScore = 40.0, maxResults = 500)) {
                assertValidBangla(match.word, name)
            }
        }
    }

    private fun assertValidBangla(word: String, source: String) {
        fun isConsonant(character: Char) = character in 'ক'..'হ' || character in "ড়ঢ়য়"
        for ((index, character) in word.withIndex()) {
            val previous = word.getOrNull(index - 1)
            val next = word.getOrNull(index + 1)
            val message = "invalid Bangla \"$word\" for $source at $index"
            when (character) {
                in "ািীুূৃেৈোৌ" ->
                    assertTrue(previous != null && (isConsonant(previous) || previous in "াে"), message)
                '্' -> assertTrue(previous != null && isConsonant(previous) && next != null && isConsonant(next), message)
                'ং' -> assertTrue(previous != null && !isVowelSignOnly(next), message)
                else -> Unit
            }
            if (index == 0) assertTrue(character != 'ণ' && !isVowelSignOnly(character), message)
        }
    }

    private fun isVowelSignOnly(character: Char?): Boolean =
        character != null && character in "ািীুূৃেৈোৌ"

    @Test
    fun theOrderIsTheSameOnEveryRunAndHasNoDuplicates() {
        for (name in plausibleBanglaByEnglishName.keys) {
            val first = banglaMatches(name, minScore = 50.0)
            assertEquals(first, banglaMatches(name, minScore = 50.0))
            assertEquals(first.size, first.map { it.word }.toSet().size, name)
            assertEquals(first.sortedByDescending { it.score }.map { it.score }, first.map { it.score }, name)
        }
    }

    @Test
    fun englishInputGivesTheExistingEnglishVariantsInTheEnglishList() {
        for (name in listOf("Philip", "Stephen", "Catherine", "Jackson")) {
            val result = engine.generate(name, bothLanguages, minScore = 80.0)
            assertNull(result.error)
            assertEquals(englishEngine.generate(name, minScore = 80.0).matches, result.english.matches, name)
        }
        assertEquals(
            listOf("Philip", "Filip", "Phillip", "Philipp"),
            engine.generate("Philip", bothLanguages, minScore = 90.0).english.matches.map { it.word }.take(4),
        )
    }

    @Test
    fun englishInputGivesBothListsIndependently() {
        val result = engine.generate("Masud", bothLanguages, minScore = 90.0)
        assertNull(result.error)
        assertEquals("মাসুদ", result.bangla.matches.first().word)
        assertEquals("Masud", result.english.matches.first().word)
        assertTrue(result.bangla.matches.all { match -> match.word.any { it.code > 0x7F } })
        assertTrue(result.english.matches.all { match -> match.word.all { it.code < 0x80 } })
    }

    @Test
    fun englishInputLimitsApplyToEachListSeparately() {
        val result = engine.generate(
            "Masud",
            bothLanguages,
            banglaOptions = PhoneticSearchOptions(minScore = 95.0, maxResults = 5),
            englishOptions = PhoneticSearchOptions(minScore = 90.0, maxResults = 2),
        )
        assertTrue(result.bangla.matches.all { it.score >= 95.0 })
        assertEquals(2, result.english.matches.size)
    }

    @Test
    fun includeOriginalDecidesWhetherTheEnglishInputStaysInTheEnglishList() {
        val kept = engine.generate("Philip", bothLanguages, minScore = 90.0).english.matches.map { it.word }
        val dropped = engine.generate("Philip", bothLanguages, minScore = 90.0, includeOriginal = false)
        assertTrue("Philip" in kept)
        assertFalse("Philip" in dropped.english.matches.map { it.word })
        assertEquals("ফিলিপ", dropped.bangla.matches.first().word)
    }

    @Test
    fun maxChangesAppliesToBothListsForEnglishInput() {
        val result = engine.generate("Masud", bothLanguages, minScore = 0.0, maxChanges = 0)
        assertEquals(listOf("মাসুদ"), result.bangla.matches.map { it.word })
        assertEquals(listOf("Masud"), result.english.matches.map { it.word })
    }

    @Test
    fun englishOnlyAndBanglaOnlyRequestsLeaveTheOtherListEmpty() {
        val banglaRequest = engine.generate("Masud", banglaOnly)
        assertTrue(banglaRequest.english.matches.isEmpty())
        assertTrue(banglaRequest.bangla.matches.isNotEmpty())
        val englishRequest = engine.generate("Masud", setOf(PhoneticLanguages.english))
        assertTrue(englishRequest.bangla.matches.isEmpty())
        assertTrue(englishRequest.english.matches.isNotEmpty())
    }

    @Test
    fun unsupportedInputFailsBothRequestedLists() {
        for (invalid in listOf("12345", "مسعود", "मसूद", "😀")) {
            val result = engine.generate(invalid, bothLanguages)
            assertIs<PhoneticArgumentException>(result.bangla.error, invalid)
            assertIs<PhoneticArgumentException>(result.english.error, invalid)
        }
    }

    @Test
    fun anEmojiInsideAnEnglishNameFailsOnlyTheBanglaList() {
        val result = engine.generate("Masud😀", bothLanguages)
        assertIs<PhoneticArgumentException>(result.bangla.error)
        assertNull(result.english.error)
        assertTrue(result.english.matches.isNotEmpty())
    }

    @Test
    fun theScriptOfTheInputDecidesTheDirection() {
        assertEquals(InputScript.BANGLA, detectInputScript("মাসুদ"))
        assertEquals(InputScript.BANGLA, detectInputScript("Masud মাসুদ"))
        assertEquals(InputScript.LATIN, detectInputScript("Masud"))
        assertEquals(InputScript.LATIN, detectInputScript("José"))
        assertEquals(InputScript.EMPTY, detectInputScript("   "))
        assertEquals(InputScript.UNSUPPORTED, detectInputScript("12345"))
        assertEquals(InputScript.UNSUPPORTED, detectInputScript("مسعود"))
    }

    @Test
    fun englishNamesAreSplitIntoSounds() {
        fun unitsOf(word: String): List<String> =
            (parseEnglishText(word) as EnglishParseOutcome.Parsed).units.map {
                when (it) {
                    is EnglishUnit.Consonant -> if (it.isDoubled) "${it.letters}${it.letters}" else it.letters
                    is EnglishUnit.Vowel -> it.letters
                    is EnglishUnit.Literal -> it.text
                    is EnglishUnit.WordBreak -> it.text
                }
            }
        assertEquals(listOf("a", "y", "e", "sh", "a"), unitsOf("ayesha"))
        assertEquals(listOf("r", "ay", "h", "a", "n"), unitsOf("rayhan"))
        assertEquals(listOf("s", "a", "dd", "a", "m"), unitsOf("saddam"))
        assertEquals(listOf("yu", "s", "u", "f"), unitsOf("yusuf"))
        assertEquals(listOf("j", "a", "h", "a", "ng", "i", "r"), unitsOf("jahangir"))
        assertEquals(listOf("r", "a", "f", "y"), unitsOf("rafy").dropLast(1) + "y")
    }

    @Test
    fun everySpellingOptionOfTheBanglaTablesHasAValidSimilarity() {
        val englishUnits = listOf(
            "b", "bh", "ch", "tch", "d", "dh", "f", "gh", "h", "j", "jh", "k", "ck", "kh", "l", "m", "n", "p", "ph", "q",
            "r", "s", "sh", "t", "th", "v", "w", "x", "y", "z", "c", "g", "ng",
        )
        val allOptions = buildList {
            englishUnits.forEach { letters ->
                for (doubled in listOf(false, true)) {
                    for (atWordStart in listOf(false, true)) {
                        add(BanglaSpellingOptions.forConsonant(letters, doubled, atWordStart, true, "e"))
                        add(BanglaSpellingOptions.forConsonant(letters, doubled, atWordStart, false, null))
                    }
                }
            }
            listOf("a", "aa", "e", "i", "y", "ee", "ea", "ie", "o", "oo", "u", "ou", "ow", "au", "aw", "ai", "ay", "oi", "ei", "ey", "yu")
                .forEach { letters ->
                    add(BanglaSpellingOptions.forVowel(letters, afterConsonant = true))
                    add(BanglaSpellingOptions.forVowel(letters, afterConsonant = false))
                    add(BanglaSpellingOptions.forVowel(letters, afterConsonant = true, atWordEnd = true))
                }
        }
        for (options in allOptions) {
            assertTrue(options.alternatives.all { it.similarity > 0.0 && it.similarity < 1.0 })
            assertTrue(options.alternatives.none { it.text == options.reference })
        }
    }
}
