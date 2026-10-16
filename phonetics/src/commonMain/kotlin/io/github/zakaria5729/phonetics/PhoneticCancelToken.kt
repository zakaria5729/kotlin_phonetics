package io.github.zakaria5729.phonetics

import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi

/** Lets the caller stop a running request, from any thread. */
@OptIn(ExperimentalAtomicApi::class)
public class PhoneticCancelToken {
    private val state = AtomicReference<TokenState>(TokenState.Waiting(emptyList()))

    /** Whether [cancel] has been called. */
    public val isCancelled: Boolean
        get() = state.load() === TokenState.Cancelled

    /** Marks the token as cancelled and notifies the listeners once. */
    public fun cancel() {
        while (true) {
            val current = state.load()
            if (current !is TokenState.Waiting) return
            if (state.compareAndSet(current, TokenState.Cancelled)) {
                current.listeners.forEach(::callQuietly)
                return
            }
        }
    }

    /** Calls [listener] when the token is cancelled, or right away if it already is. */
    public fun addListener(listener: () -> Unit) {
        while (true) {
            val current = state.load()
            if (current !is TokenState.Waiting) {
                callQuietly(listener)
                return
            }
            if (state.compareAndSet(current, TokenState.Waiting(current.listeners + listener))) return
        }
    }

    /** Stops [listener] from being called. */
    public fun removeListener(listener: () -> Unit) {
        while (true) {
            val current = state.load()
            if (current !is TokenState.Waiting) return
            if (state.compareAndSet(current, TokenState.Waiting(current.listeners - listener))) return
        }
    }

    private fun callQuietly(listener: () -> Unit) {
        try {
            listener()
        } catch (_: Throwable) {
        }
    }

    private sealed interface TokenState {
        class Waiting(val listeners: List<() -> Unit>) : TokenState

        data object Cancelled : TokenState
    }
}
