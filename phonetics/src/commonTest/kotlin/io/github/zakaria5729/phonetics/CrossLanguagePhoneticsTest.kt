package io.github.zakaria5729.phonetics

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.time.TimeSource
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest

class CrossLanguagePhoneticsTest {
    private val engine = CrossLanguageEngine()
    private val banglaEngine = PhoneticEngine(PhoneticLanguages.bangla)
    private val banglaOnly = setOf(PhoneticLanguages.bangla)
    private val englishOnly = setOf(PhoneticLanguages.english)
    private val bothLanguages = setOf(PhoneticLanguages.bangla, PhoneticLanguages.english)

    private val heavyName: String by lazy { buildHeavyName() }

    private fun buildHeavyName(): String {
        var wordCount = 4
        while (true) {
            val name = List(wordCount) { "আসাদুজ্জামান" }.joinToString(" ")
            val start = TimeSource.Monotonic.markNow()
            engine.generate(name, bothLanguages, minScore = 0.0, maxResults = null)
            if (start.elapsedNow().inWholeMilliseconds >= 250 || wordCount >= 64) return name
            wordCount *= 2
        }
    }

    private val realisticNames = listOf(
        "মাসুদ", "সাদ্দাম", "মোজাফফর", "মোহাম্মদ", "আব্দুল", "রহিম", "করিম", "ফারজানা", "নাসির", "শাকিব",
    )

    @Test
    fun banglaOnlyGivesTheSameListAsBanglaPhonetics() {
        for (name in realisticNames) {
            val result = engine.generate(name, banglaOnly, minScore = 90.0)
            assertNull(result.error)
            assertEquals(banglaEngine.generate(name, minScore = 90.0).matches, result.bangla.matches, name)
            assertEquals(banglaEngine.generate(name, minScore = 90.0).matches.map { it.changes }, result.bangla.matches.map { it.changes })
            assertTrue(result.english.matches.isEmpty())
            assertNull(result.english.error)
        }
    }

    @Test
    fun englishOnlyLeavesTheBanglaListEmpty() {
        val result = engine.generate("মাসুদ", englishOnly, minScore = 90.0)
        assertNull(result.error)
        assertTrue(result.bangla.matches.isEmpty())
        assertNull(result.bangla.error)
        assertEquals("Masud", result.english.matches.first().word)
    }

    @Test
    fun bothLanguagesAreReturnedIndependently() {
        val result = engine.generate("মাসুদ", bothLanguages, minScore = 90.0)
        assertNull(result.error)
        assertEquals(
            listOf("মাসুদ", "মাসূদ", "মাশুদ", "মাষুদ", "মাশূদ", "মাষূদ"),
            result.bangla.matches.map { it.word },
        )
        assertEquals(
            listOf("Masud", "Masood", "Masoud", "Mashud"),
            result.english.matches.map { it.word }.take(4),
        )
        assertTrue(result.bangla.matches.all { it.word.all { character -> character.code > 0x7F } })
        assertTrue(result.english.matches.all { it.word.all { character -> character.code < 0x80 } })
    }

    @Test
    fun resultsCanBeLookedUpByLanguage() {
        val result = engine.generate("রহিম", bothLanguages)
        assertSame(result.bangla, result[PhoneticLanguages.bangla])
        assertSame(result.english, result[PhoneticLanguages.english])
        assertNull(result[PhoneticLanguage("xx", "Other", emptyList())])
    }

    @Test
    fun theDefaultLanguagesAreBanglaAndEnglish() {
        val result = engine.generate("করিম")
        assertTrue(result.bangla.matches.isNotEmpty())
        assertTrue(result.english.matches.isNotEmpty())
    }

    @Test
    fun scoresOfBothListsStartAtOneHundredAndStayInRange() {
        for (name in realisticNames) {
            val result = engine.generate(name, bothLanguages, minScore = 60.0, maxResults = 200)
            for (matches in listOf(result.bangla.matches, result.english.matches)) {
                assertEquals(100.0, matches.first().score, name)
                assertEquals(matches.sortedByDescending { it.score }.map { it.score }, matches.map { it.score }, name)
                assertTrue(matches.all { it.score >= 60.0 && it.score <= 100.0 }, name)
                assertEquals(matches.size, matches.map { it.word }.toSet().size, name)
            }
        }
    }

    @Test
    fun minScoreAndMaxResultsApplyToEachLanguageSeparately() {
        val result = engine.generate(
            "মাসুদ",
            bothLanguages,
            banglaOptions = PhoneticSearchOptions(minScore = 97.0, maxResults = 5),
            englishOptions = PhoneticSearchOptions(minScore = 85.0, maxResults = 3),
        )
        assertNull(result.error)
        assertEquals(listOf("মাসুদ", "মাসূদ"), result.bangla.matches.map { it.word })
        assertEquals(3, result.english.matches.size)
        assertEquals(listOf("Masud", "Masood", "Masoud"), result.english.matches.map { it.word })

        val swapped = engine.generate(
            "মাসুদ",
            bothLanguages,
            banglaOptions = PhoneticSearchOptions(minScore = 90.0, maxResults = 2),
            englishOptions = PhoneticSearchOptions(minScore = 95.0, maxResults = 50),
        )
        assertEquals(2, swapped.bangla.matches.size)
        assertEquals(listOf("Masud", "Masood"), swapped.english.matches.map { it.word })
    }

    @Test
    fun includeOriginalAffectsOnlyTheBanglaList() {
        val result = engine.generate("মাসুদ", bothLanguages, minScore = 90.0, includeOriginal = false)
        assertTrue(result.bangla.matches.none { it.word == "মাসুদ" })
        assertEquals("Masud", result.english.matches.first().word)
    }

    @Test
    fun maxChangesAppliesToBothLanguages() {
        val result = engine.generate("মাসুদ", bothLanguages, minScore = 0.0, maxChanges = 0)
        assertEquals(listOf("মাসুদ"), result.bangla.matches.map { it.word })
        assertEquals(listOf("Masud"), result.english.matches.map { it.word })
    }

    @Test
    fun emptyInputGivesEmptyListsWithoutAnError() {
        for (blank in listOf("", "  ")) {
            val result = engine.generate(blank, bothLanguages)
            assertNull(result.error)
            assertTrue(result.bangla.matches.isEmpty())
            assertTrue(result.english.matches.isEmpty())
        }
    }

    @Test
    fun unreadableInputFailsOnlyTheEnglishList() {
        val result = engine.generate("মাসুদ😀", bothLanguages)
        assertIs<PhoneticArgumentException>(result.english.error)
        assertTrue(result.english.matches.isEmpty())
        assertNull(result.bangla.error)
        assertEquals("মাসুদ😀", result.bangla.matches.first().word)
        assertSame(result.english.error, result.error)
    }

    @Test
    fun badArgumentsFailTheWholeRequest() {
        val badScore = engine.generate("মাসুদ", bothLanguages, minScore = 120.0)
        assertIs<PhoneticArgumentException>(badScore.bangla.error)
        assertIs<PhoneticArgumentException>(badScore.english.error)

        assertIs<PhoneticArgumentException>(engine.generate("মাসুদ", bothLanguages, maxResults = 0).error)
        assertIs<PhoneticArgumentException>(engine.generate("মাসুদ", bothLanguages, maxChanges = -1).error)
        assertIs<PhoneticArgumentException>(engine.generate("মাসুদ", emptySet()).error)

        val unsupported = setOf(PhoneticLanguage("xx", "Other", emptyList()))
        assertIs<PhoneticArgumentException>(engine.generate("মাসুদ", unsupported).error)
    }

    @Test
    fun invalidOptionsOfALanguageThatWasNotRequestedAreIgnored() {
        val result = engine.generate(
            "মাসুদ",
            englishOnly,
            banglaOptions = PhoneticSearchOptions(minScore = 500.0),
            englishOptions = PhoneticSearchOptions(minScore = 90.0),
        )
        assertNull(result.error)
        assertEquals("Masud", result.english.matches.first().word)
    }

    @Test
    fun aBrokenBanglaRuleSetFailsOnlyTheBanglaList() {
        val brokenEngine = CrossLanguageEngine(gapSimilarity = 5.0)
        val result = brokenEngine.generate("মাসুদ", bothLanguages, minScore = 90.0)
        assertIs<PhoneticArgumentException>(result.bangla.error)
        assertNull(result.english.error)
        assertEquals("Masud", result.english.matches.first().word)
        assertIs<PhoneticArgumentException>(brokenEngine.error)
    }

    @Test
    fun suspendApiMatchesTheSynchronousEngine() = runTest {
        val phonetics = CrossLanguagePhonetics()
        try {
            for (name in realisticNames) {
                val expected = engine.generate(name, bothLanguages, minScore = 80.0)
                val actual = phonetics.generate(name, bothLanguages, minScore = 80.0)
                assertNull(actual.error)
                assertEquals(expected, actual, name)
            }
            val options = PhoneticSearchOptions(minScore = 95.0, maxResults = 2)
            val withOptions = phonetics.generate("মাসুদ", bothLanguages, options, options)
            assertEquals(engine.generate("মাসুদ", bothLanguages, options, options), withOptions)
            assertEquals(engine.generate("মাসুদ"), phonetics.generate("মাসুদ"))
        } finally {
            phonetics.dispose()
        }
    }

    @Test
    fun sharedInstanceUsesCancelPreviousByDefault() {
        assertEquals(PhoneticExecutionMode.CancelPrevious, CrossLanguagePhonetics.instance.executionMode)
        assertTrue(CrossLanguagePhonetics.instance.workerName.isNotEmpty())
    }

    @Test
    fun argumentErrorsComeBackAsResults() = runTest {
        val phonetics = CrossLanguagePhonetics()
        try {
            assertIs<PhoneticArgumentException>(phonetics.generate("মাসুদ", minScore = -1.0).error)
            assertIs<PhoneticArgumentException>(phonetics.generate("মাসুদ", emptySet()).error)
            assertIs<PhoneticArgumentException>(phonetics.generate("12345", englishOnly).error)
        } finally {
            phonetics.dispose()
        }
    }

    @Test
    fun aCancelledTokenReturnsACancelledErrorForBothLists() = runTest {
        val phonetics = CrossLanguagePhonetics()
        try {
            val token = PhoneticCancelToken().also { it.cancel() }
            val result = phonetics.generate("মাসুদ", cancelToken = token)
            assertIs<PhoneticCancelledException>(result.bangla.error)
            assertIs<PhoneticCancelledException>(result.english.error)
            assertTrue(result.bangla.matches.isEmpty() && result.english.matches.isEmpty())
            assertNull(phonetics.generate("মাসুদ").error)
        } finally {
            phonetics.dispose()
        }
    }

    @Test
    fun cancelStopsARunningRequest() = runTest {
        val phonetics = CrossLanguagePhonetics(executionMode = PhoneticExecutionMode.Queue)
        try {
            val token = PhoneticCancelToken()
            val longName = heavyName
            val call = async {
                phonetics.generate(longName, bothLanguages, minScore = 0.0, maxResults = null, cancelToken = token)
            }
            waitFor(30)
            token.cancel()
            val result = call.await()
            assertIs<PhoneticCancelledException>(result.error)
            assertNull(phonetics.generate("মাসুদ").error)
        } finally {
            phonetics.dispose()
        }
    }

    @Test
    fun cancelAllStopsEveryPendingRequest() = runTest {
        val phonetics = CrossLanguagePhonetics(executionMode = PhoneticExecutionMode.Queue)
        try {
            val longName = heavyName
            val first = async { phonetics.generate(longName, bothLanguages, minScore = 0.0, maxResults = null) }
            val second = async { phonetics.generate(longName, bothLanguages, minScore = 0.0, maxResults = null) }
            waitFor(30)
            phonetics.cancelAll()
            assertIs<PhoneticCancelledException>(first.await().error)
            assertIs<PhoneticCancelledException>(second.await().error)
            assertNull(phonetics.generate("মাসুদ").error)
        } finally {
            phonetics.dispose()
        }
    }

    @Test
    fun cancelPreviousReplacesTheRunningRequest() = runTest {
        val phonetics = CrossLanguagePhonetics(executionMode = PhoneticExecutionMode.CancelPrevious)
        try {
            val longName = heavyName
            val first = async { phonetics.generate(longName, bothLanguages, minScore = 0.0, maxResults = null) }
            waitFor(30)
            val second = phonetics.generate("মাসুদ", bothLanguages, minScore = 90.0)
            assertIs<PhoneticCancelledException>(first.await().error)
            assertNull(second.error)
            assertEquals("Masud", second.english.matches.first().word)
        } finally {
            phonetics.dispose()
        }
    }

    @Test
    fun aDisposedInstanceReturnsDisposedErrors() = runTest {
        val phonetics = CrossLanguagePhonetics()
        assertNull(phonetics.generate("মাসুদ").error)
        phonetics.dispose()
        phonetics.dispose()
        val result = phonetics.generate("মাসুদ")
        assertIs<PhoneticDisposedException>(result.bangla.error)
        assertIs<PhoneticDisposedException>(result.english.error)
    }

    @Test
    fun queuedRequestsAllFinishWithTheirOwnResults() = runTest {
        val phonetics = CrossLanguagePhonetics(executionMode = PhoneticExecutionMode.Queue)
        try {
            val results = realisticNames.map { name -> async { phonetics.generate(name, minScore = 85.0) } }.awaitAll()
            results.forEachIndexed { index, result ->
                assertNull(result.error)
                assertEquals(engine.generate(realisticNames[index], minScore = 85.0), result)
            }
        } finally {
            phonetics.dispose()
        }
    }

    @Test
    fun callbacksReceiveTheSameResults() = runTest {
        val phonetics = CrossLanguagePhonetics(callbackDispatcher = Dispatchers.Default)
        try {
            val delivered = CompletableDeferred<CrossLanguageResult>()
            phonetics.generateAsync("মাসুদ", bothLanguages, minScore = 90.0) { delivered.complete(it) }
            assertEquals(engine.generate("মাসুদ", bothLanguages, minScore = 90.0), delivered.await())

            val withDefaults = CompletableDeferred<CrossLanguageResult>()
            phonetics.generateAsync("মাসুদ") { withDefaults.complete(it) }
            assertEquals(engine.generate("মাসুদ"), withDefaults.await())

            val cancelledToken = PhoneticCancelToken().also { it.cancel() }
            val cancelled = CompletableDeferred<CrossLanguageResult>()
            phonetics.generateAsync("মাসুদ", cancelToken = cancelledToken) { cancelled.complete(it) }
            assertIs<PhoneticCancelledException>(cancelled.await().error)
        } finally {
            phonetics.dispose()
        }
    }
}
