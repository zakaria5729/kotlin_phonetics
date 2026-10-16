package io.github.zakaria5729.phonetics

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.TimeSource
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest

class CancelTest {
    private val engine = PhoneticEngine(PhoneticLanguages.bangla)
    private val workload = buildWorkload(engine)

    private suspend fun smallRequestMs(phonetics: Phonetics): Long {
        val start = TimeSource.Monotonic.markNow()
        assertEquals(85.0, phonetics.similarity("আসাদ", "আছাদ").score)
        return start.elapsedNow().inWholeMilliseconds
    }

    @Test
    fun anUnusedTokenChangesNothing() = runTest {
        val phonetics = BanglaPhonetics(executionMode = PhoneticExecutionMode.Queue)
        try {
            val token = PhoneticCancelToken()
            val result = phonetics.generateWords("আসাদ", minScore = 90.0, cancelToken = token)
            assertNull(result.error)
            assertEquals(listOf("আসাদ", "আশাদ", "আষাদ"), result.words)
        } finally {
            phonetics.dispose()
        }
    }

    @Test
    fun aCancelledTokenReturnsAtOnce() = runTest {
        val phonetics = BanglaPhonetics(executionMode = PhoneticExecutionMode.Queue)
        try {
            val token = PhoneticCancelToken().also { it.cancel() }
            val result = phonetics.rank(HEAVY_QUERY, workload.names, cancelToken = token)
            assertIs<PhoneticCancelledException>(result.error)
            assertTrue(result.matches.isEmpty())
            assertTrue(smallRequestMs(phonetics) < workload.directRunMs / 2)
        } finally {
            phonetics.dispose()
        }
    }

    @Test
    fun cancelStopsAJobThatIsAlreadyRunning() = runTest {
        val phonetics = BanglaPhonetics(executionMode = PhoneticExecutionMode.Queue)
        try {
            val token = PhoneticCancelToken()
            val call = async { phonetics.rank(HEAVY_QUERY, workload.names, cancelToken = token) }
            waitFor(60)
            token.cancel()
            val result = call.await()
            assertIs<PhoneticCancelledException>(result.error)
            assertTrue(result.matches.isEmpty())
            assertTrue(smallRequestMs(phonetics) < workload.directRunMs / 2)
        } finally {
            phonetics.dispose()
        }
    }

    @Test
    fun cancelAllStopsEverythingPending() = runTest {
        val phonetics = BanglaPhonetics(executionMode = PhoneticExecutionMode.Queue)
        try {
            val first = async { phonetics.rank(HEAVY_QUERY, workload.names) }
            val second = async { phonetics.rank(HEAVY_QUERY, workload.names) }
            waitFor(60)
            phonetics.cancelAll()
            assertIs<PhoneticCancelledException>(first.await().error)
            assertIs<PhoneticCancelledException>(second.await().error)
            assertTrue(smallRequestMs(phonetics) < workload.directRunMs / 2)
        } finally {
            phonetics.dispose()
        }
    }

    @Test
    fun disposeCancelsRunningRequests() = runTest {
        val phonetics = BanglaPhonetics(executionMode = PhoneticExecutionMode.Queue)
        val call = async { phonetics.rank(HEAVY_QUERY, workload.names) }
        waitFor(60)
        phonetics.dispose()
        val result = call.await()
        assertIs<PhoneticCancelledException>(result.error)
        assertEquals("Phonetics was disposed", result.error?.message)
    }

    @Test
    fun cancelPreviousReplacesTheRunningRequest() = runTest {
        val phonetics = BanglaPhonetics(executionMode = PhoneticExecutionMode.CancelPrevious)
        try {
            val replaced = async { phonetics.rank(HEAVY_QUERY, workload.names) }
            waitFor(60)
            val replacement = phonetics.similarity("আসাদ", "আছাদ")
            assertEquals(85.0, replacement.score)
            assertNull(replacement.error)
            assertIs<PhoneticCancelledException>(replaced.await().error)
        } finally {
            phonetics.dispose()
        }
    }

    @Test
    fun queueFinishesEveryRequestInOrder() = runTest {
        val phonetics = BanglaPhonetics(executionMode = PhoneticExecutionMode.Queue)
        try {
            val first = async { phonetics.rank(HEAVY_QUERY, workload.names, limit = 3) }
            waitFor(30)
            val second = async { phonetics.similarity("আসাদ", "আছাদ") }
            val firstResult = first.await()
            val secondResult = second.await()
            assertNull(firstResult.error)
            assertEquals(3, firstResult.matches.size)
            assertNull(secondResult.error)
            assertEquals(85.0, secondResult.score)
        } finally {
            phonetics.dispose()
        }
    }

    @Test
    fun theModeCanBeChangedOnARunningInstance() = runTest {
        val phonetics = BanglaPhonetics(executionMode = PhoneticExecutionMode.Queue)
        try {
            phonetics.similarity("আ", "আ")
            phonetics.setExecutionMode(PhoneticExecutionMode.CancelPrevious)
            assertEquals(PhoneticExecutionMode.CancelPrevious, phonetics.executionMode)

            val replaced = async { phonetics.rank(HEAVY_QUERY, workload.names) }
            waitFor(60)
            assertEquals(85.0, phonetics.similarity("আসাদ", "আছাদ").score)
            assertIs<PhoneticCancelledException>(replaced.await().error)

            phonetics.setExecutionMode(PhoneticExecutionMode.Queue)
            val first = async { phonetics.rank(HEAVY_QUERY, workload.names, limit = 2) }
            waitFor(30)
            val second = async { phonetics.similarity("আসাদ", "আছাদ") }
            assertNull(first.await().error)
            assertNull(second.await().error)
        } finally {
            phonetics.dispose()
        }
    }
}
