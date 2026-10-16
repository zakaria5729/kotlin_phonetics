package io.github.zakaria5729.phonetics

import kotlin.time.TimeSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

internal const val HEAVY_QUERY = "আসাদুজ্জামান শরীফ রহিম"

private val nameParts = listOf("আসাদ", "শরীফ", "রহিম", "করিম", "নাজনীন", "জসীম", "সৈয়দ", "কৃষক")

internal class Workload(val names: List<String>, val directRunMs: Long)

internal fun buildNames(count: Int): List<String> = List(count) { i ->
    "${nameParts[i % 8]} ${nameParts[(i * 3) % 8]} ${nameParts[(i * 5) % 8]} $i"
}

internal fun buildWorkload(engine: PhoneticEngine, targetMs: Long = 400): Workload {
    var count = 20_000
    while (true) {
        val names = buildNames(count)
        val start = TimeSource.Monotonic.markNow()
        engine.rank(HEAVY_QUERY, names)
        val elapsed = start.elapsedNow().inWholeMilliseconds
        if (elapsed >= targetMs || count >= 2_000_000) return Workload(names, elapsed)
        count *= 2
    }
}

internal suspend fun waitFor(milliseconds: Long) {
    withContext(Dispatchers.Default) { delay(milliseconds) }
}
