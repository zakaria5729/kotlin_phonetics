package io.github.zakaria5729.phonetics

import kotlin.coroutines.EmptyCoroutineContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

internal fun mainOrDefaultDispatcher(): CoroutineDispatcher = try {
    Dispatchers.Main.also { it.isDispatchNeeded(EmptyCoroutineContext) }
} catch (_: Throwable) {
    Dispatchers.Default
}
