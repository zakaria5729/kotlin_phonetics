package io.github.zakaria5729.phonetics

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest

class PhoneticsTest {
    private val engine = PhoneticEngine(PhoneticLanguages.bangla)

    @Test
    fun resultsMatchTheSynchronousEngine() = runTest {
        val phonetics = BanglaPhonetics()
        try {
            val generated = phonetics.generate("মাসুদ", minScore = 60.0)
            assertNull(generated.error)
            assertEquals(engine.generate("মাসুদ", minScore = 60.0).matches, generated.matches)

            assertEquals(85.0, phonetics.similarity("আসাদ", "আছাদ").score)
            assertTrue(phonetics.matches("আসাদ", "আশাদ").isMatch)
            assertEquals(listOf("আসাদ", "আশাদ", "আষাদ"), phonetics.generateWords("আসাদ", minScore = 90.0).words)

            val candidates = listOf("আসাদ", "আছাদ", "রহিম", "আশাদ")
            val ranked = phonetics.rank("আসাদ", candidates, minScore = 80.0)
            assertEquals(engine.rank("আসাদ", candidates, minScore = 80.0).matches, ranked.matches)
        } finally {
            phonetics.dispose()
        }
    }

    @Test
    fun defaultsAreCancelPreviousAndTheWorkerNameIsSet() {
        val phonetics = BanglaPhonetics()
        try {
            assertEquals(PhoneticExecutionMode.CancelPrevious, phonetics.executionMode)
            assertEquals(PhoneticExecutionMode.CancelPrevious, BanglaPhonetics.instance.executionMode)
            assertEquals(PhoneticExecutionMode.CancelPrevious, EnglishPhonetics.instance.executionMode)
            assertTrue(phonetics.workerName.isNotEmpty())
            assertEquals("bn", phonetics.language.code)
        } finally {
            phonetics.dispose()
        }
    }

    @Test
    fun englishInstanceWorks() = runTest {
        val phonetics = EnglishPhonetics()
        try {
            val words = phonetics.generateWords("Asad", minScore = 95.0).words
            assertTrue("Asad" in words)
            assertTrue("Assad" in words)
        } finally {
            phonetics.dispose()
        }
    }

    @Test
    fun badArgumentsComeBackAsErrors() = runTest {
        val phonetics = BanglaPhonetics()
        try {
            assertIs<PhoneticArgumentException>(phonetics.generate("আ", minScore = 200.0).error)
            assertIs<PhoneticArgumentException>(phonetics.generate("আ", maxResults = 0).error)
            assertIs<PhoneticArgumentException>(phonetics.matches("আ", "আ", minScore = -1.0).error)
            assertIs<PhoneticArgumentException>(phonetics.rank("আ", listOf("আ"), limit = 0).error)
        } finally {
            phonetics.dispose()
        }
    }

    @Test
    fun aBrokenSetupReturnsItsErrorEveryTime() = runTest {
        val phonetics = Phonetics(PhoneticLanguages.english, gapSimilarity = 5.0)
        try {
            assertIs<PhoneticArgumentException>(phonetics.generate("a").error)
            assertIs<PhoneticArgumentException>(phonetics.similarity("a", "b").error)
            assertIs<PhoneticArgumentException>(phonetics.rank("a", listOf("b")).error)
        } finally {
            phonetics.dispose()
        }
    }

    @Test
    fun aDisposedInstanceReturnsDisposedErrors() = runTest {
        val phonetics = BanglaPhonetics()
        assertNull(phonetics.similarity("আ", "আ").error)
        phonetics.dispose()
        phonetics.dispose()
        assertIs<PhoneticDisposedException>(phonetics.generate("আসাদ").error)
        assertIs<PhoneticDisposedException>(phonetics.similarity("আ", "আ").error)
        assertIs<PhoneticDisposedException>(phonetics.rank("আ", listOf("আ")).error)
    }

    @Test
    fun manyRequestsAtOnceAllFinish() = runTest {
        val phonetics = BanglaPhonetics(executionMode = PhoneticExecutionMode.Queue)
        try {
            val words = List(60) { "আসাদ$it" }
            val results = words.map { async { phonetics.generate(it) } }.awaitAll()
            assertTrue(results.all { it.error == null })
            results.forEachIndexed { index, result ->
                assertEquals(engine.generate(words[index]).matches, result.matches)
            }
        } finally {
            phonetics.dispose()
        }
    }

    @Test
    fun callbacksReceiveTheSameResults() = runTest {
        val phonetics = BanglaPhonetics(callbackDispatcher = Dispatchers.Default)
        try {
            val generated = CompletableDeferred<PhoneticMatchesResult>()
            phonetics.generateAsync("আসাদ", minScore = 90.0) { generated.complete(it) }
            assertEquals(listOf("আসাদ", "আশাদ", "আষাদ"), generated.await().matches.map { it.word })

            val similarity = CompletableDeferred<PhoneticSimilarityResult>()
            phonetics.similarityAsync("আসাদ", "আছাদ") { similarity.complete(it) }
            assertEquals(85.0, similarity.await().score)

            val words = CompletableDeferred<PhoneticWordsResult>()
            phonetics.generateWordsAsync("আসাদ", minScore = 90.0) { words.complete(it) }
            assertEquals(3, words.await().words.size)

            val check = CompletableDeferred<PhoneticMatchCheckResult>()
            phonetics.matchesAsync("আসাদ", "আশাদ") { check.complete(it) }
            assertTrue(check.await().isMatch)

            val ranked = CompletableDeferred<PhoneticMatchesResult>()
            phonetics.rankAsync("আসাদ", listOf("আছাদ", "রহিম")) { ranked.complete(it) }
            assertEquals("আছাদ", ranked.await().matches.first().word)
        } finally {
            phonetics.dispose()
        }
    }

    @Test
    fun aCallbackRequestCanBeCancelledThroughItsToken() = runTest {
        val phonetics = BanglaPhonetics(callbackDispatcher = Dispatchers.Default)
        try {
            val cancelled = PhoneticCancelToken().also { it.cancel() }
            val delivered = CompletableDeferred<PhoneticMatchesResult>()
            phonetics.generateAsync("আসাদ", cancelToken = cancelled) { delivered.complete(it) }
            assertIs<PhoneticCancelledException>(delivered.await().error)
        } finally {
            phonetics.dispose()
        }
    }

    @Test
    fun overloadsWithoutArgumentsUseTheDefaults() = runTest {
        val phonetics = BanglaPhonetics(callbackDispatcher = Dispatchers.Default)
        try {
            assertEquals(engine.generate("আসাদ").matches, phonetics.generate("আসাদ").matches)
            assertEquals(engine.generateWords("আসাদ").words, phonetics.generateWords("আসাদ").words)
            assertEquals(85.0, phonetics.similarity("আসাদ", "আছাদ").score)
            assertTrue(phonetics.matches("আসাদ", "আশাদ").isMatch)
            assertEquals(
                engine.rank("আসাদ", listOf("আছাদ", "রহিম")).matches,
                phonetics.rank("আসাদ", listOf("আছাদ", "রহিম")).matches,
            )

            val generated = CompletableDeferred<PhoneticMatchesResult>()
            phonetics.generateAsync("আসাদ") { generated.complete(it) }
            assertEquals(engine.generate("আসাদ").matches, generated.await().matches)

            val words = CompletableDeferred<PhoneticWordsResult>()
            phonetics.generateWordsAsync("আসাদ") { words.complete(it) }
            assertEquals(engine.generateWords("আসাদ").words, words.await().words)

            val similarity = CompletableDeferred<PhoneticSimilarityResult>()
            phonetics.similarityAsync("আসাদ", "আছাদ") { similarity.complete(it) }
            assertEquals(85.0, similarity.await().score)

            val check = CompletableDeferred<PhoneticMatchCheckResult>()
            phonetics.matchesAsync("আসাদ", "আশাদ") { check.complete(it) }
            assertTrue(check.await().isMatch)

            val ranked = CompletableDeferred<PhoneticMatchesResult>()
            phonetics.rankAsync("আসাদ", listOf("আছাদ", "রহিম")) { ranked.complete(it) }
            assertEquals("আছাদ", ranked.await().matches.first().word)
        } finally {
            phonetics.dispose()
        }
    }

    @Test
    fun cancelTokenListenersRunOnceAndCanBeRemoved() {
        val token = PhoneticCancelToken()
        var firstCalls = 0
        var secondCalls = 0
        val first = { firstCalls++; Unit }
        token.addListener(first)
        token.addListener { secondCalls++ }
        token.removeListener(first)
        assertEquals(false, token.isCancelled)
        token.cancel()
        token.cancel()
        assertEquals(true, token.isCancelled)
        assertEquals(0, firstCalls)
        assertEquals(1, secondCalls)
        token.addListener(first)
        assertEquals(1, firstCalls)
    }
}
