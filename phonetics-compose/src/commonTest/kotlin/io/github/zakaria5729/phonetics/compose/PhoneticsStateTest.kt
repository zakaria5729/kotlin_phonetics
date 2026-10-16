package io.github.zakaria5729.phonetics.compose

import androidx.compose.runtime.AbstractApplier
import androidx.compose.runtime.BroadcastFrameClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.Snapshot
import io.github.zakaria5729.phonetics.PhoneticMatchesResult
import io.github.zakaria5729.phonetics.PhoneticSimilarityResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

private class NoopApplier : AbstractApplier<Unit>(Unit) {
    override fun insertTopDown(index: Int, instance: Unit) {}
    override fun insertBottomUp(index: Int, instance: Unit) {}
    override fun remove(index: Int, count: Int) {}
    override fun move(from: Int, to: Int, count: Int) {}
    override fun onClear() {}
}

class PhoneticsStateTest {
    private suspend fun <T> awaitValue(read: () -> T?): T = withContext(Dispatchers.Default) {
        withTimeout(20_000) {
            var current = Snapshot.withoutReadObservation { read() }
            while (current == null) {
                delay(10)
                current = Snapshot.withoutReadObservation { read() }
            }
            current
        }
    }

    private fun runComposition(
        scope: kotlinx.coroutines.CoroutineScope,
        content: @Composable () -> Unit,
    ): Composition {
        val clock = BroadcastFrameClock()
        val recomposer = Recomposer(scope.coroutineContext + clock)
        scope.launch(clock, start = CoroutineStart.UNDISPATCHED) { recomposer.runRecomposeAndApplyChanges() }
        scope.launch {
            while (true) {
                Snapshot.sendApplyNotifications()
                clock.sendFrame(0L)
                withContext(Dispatchers.Default) { delay(5) }
            }
        }
        val composition = Composition(NoopApplier(), recomposer)
        composition.setContent(content)
        return composition
    }

    @Test
    fun matchesAreComputedAndUpdatedWhenTheInputChanges() = runTest {
        val input = mutableStateOf("আসাদ")
        var state: State<PhoneticMatchesResult?>? = null
        val composition = runComposition(this) {
            val phonetics = rememberBanglaPhonetics()
            state = rememberPhoneticMatches(phonetics, input.value, minScore = 90.0)
        }
        try {
            val first = awaitValue { state?.value }
            assertNull(first.error)
            assertEquals(listOf("আসাদ", "আশাদ", "আষাদ"), first.matches.map { it.word })

            input.value = "রহিম"
            val second = awaitValue { state?.value?.takeIf { result -> result.matches.firstOrNull()?.word == "রহিম" } }
            assertEquals("রহিম", second.matches.first().word)
        } finally {
            composition.dispose()
            coroutineContext[kotlinx.coroutines.Job]?.children?.forEach { it.cancel() }
        }
    }

    @Test
    fun similarityIsComputed() = runTest {
        var state: State<PhoneticSimilarityResult?>? = null
        val composition = runComposition(this) {
            val phonetics = rememberBanglaPhonetics()
            state = rememberPhoneticSimilarity(phonetics, "আসাদ", "আছাদ")
        }
        try {
            val result = awaitValue { state?.value }
            assertEquals(85.0, result.score)
            assertNull(result.error)
        } finally {
            composition.dispose()
            coroutineContext[kotlinx.coroutines.Job]?.children?.forEach { it.cancel() }
        }
    }

    @Test
    fun rankingIsComputed() = runTest {
        var state: State<PhoneticMatchesResult?>? = null
        val composition = runComposition(this) {
            val phonetics = rememberPhonetics(io.github.zakaria5729.phonetics.PhoneticLanguages.bangla)
            state = rememberPhoneticRanking(phonetics, "আসাদ", listOf("রহিম", "আছাদ", "আশাদ"), limit = 2)
        }
        try {
            val result = awaitValue { state?.value }
            assertNull(result.error)
            assertEquals(2, result.matches.size)
            assertTrue(result.matches.all { it.word != "রহিম" })
            assertNotNull(result.matches.firstOrNull())
        } finally {
            composition.dispose()
            coroutineContext[kotlinx.coroutines.Job]?.children?.forEach { it.cancel() }
        }
    }
}
