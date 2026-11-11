@file:OptIn(ExperimentalAtomicApi::class)

package io.github.zakaria5729.phonetics

import kotlin.concurrent.Volatile
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.yield

private const val WORK_UNITS_PER_SLICE = 16
private val sliceDuration = 8.milliseconds

internal class Outcome<T>(val value: T?, val error: PhoneticException?)

internal class PhoneticScheduler(
    executionMode: PhoneticExecutionMode,
    workerDispatcher: CoroutineDispatcher,
    callbackDispatcher: CoroutineDispatcher?,
) {
    private val workerScope = CoroutineScope(SupervisorJob() + workerDispatcher)
    private val callbackScope = CoroutineScope(SupervisorJob() + (callbackDispatcher ?: mainOrDefaultDispatcher()))
    private val jobLock = Mutex()
    private val pending = AtomicReference<List<Request<*>>>(emptyList())
    private val disposed = AtomicBoolean(false)

    @Volatile
    private var currentMode = executionMode

    var executionMode: PhoneticExecutionMode
        get() = currentMode
        set(mode) {
            currentMode = mode
        }

    fun <R> deliver(
        cancelToken: PhoneticCancelToken,
        onResult: (R) -> Unit,
        compute: suspend () -> R,
    ): PhoneticCancelToken {
        callbackScope.launch {
            onResult(compute())
        }
        return cancelToken
    }

    suspend fun <T> execute(
        cancelToken: PhoneticCancelToken?,
        setupError: PhoneticException?,
        buildJob: () -> PhoneticJob<T>,
    ): Outcome<T> {
        if (disposed.load()) return Outcome(null, PhoneticDisposedException())
        setupError?.let { return Outcome(null, it) }
        if (cancelToken?.isCancelled == true) return Outcome(null, PhoneticCancelledException())

        if (currentMode == PhoneticExecutionMode.CancelPrevious) {
            cancelPending(PhoneticCancelledException())
        }

        val request = Request<T>()
        register(request)
        val onCancel = { finish(request, Outcome(null, PhoneticCancelledException())) }
        cancelToken?.addListener(onCancel)

        try {
            workerScope.launch { runJob(request, buildJob) }
        } catch (failure: Throwable) {
            finish(request, Outcome(null, PhoneticWorkerException("$failure")))
        }

        return try {
            request.outcome.await()
        } catch (cancelled: CancellationException) {
            finish(request, Outcome(null, PhoneticCancelledException()))
            throw cancelled
        } finally {
            cancelToken?.removeListener(onCancel)
        }
    }

    fun cancelAll() {
        cancelPending(PhoneticCancelledException())
    }

    fun dispose(message: String) {
        if (!disposed.compareAndSet(expectedValue = false, newValue = true)) return
        cancelPending(PhoneticCancelledException(message))
        workerScope.cancel()
        callbackScope.cancel()
    }

    private suspend fun <T> runJob(request: Request<T>, buildJob: () -> PhoneticJob<T>) {
        try {
            jobLock.withLock {
                if (request.isFinished) return
                val job = buildJob()
                var sliceStart = TimeSource.Monotonic.markNow()
                while (!job.isDone && !request.isFinished) {
                    job.advance(WORK_UNITS_PER_SLICE)
                    if (!job.isDone && sliceStart.elapsedNow() >= sliceDuration) {
                        yield()
                        sliceStart = TimeSource.Monotonic.markNow()
                    }
                }
                if (!request.isFinished) finish(request, Outcome(job.result, null))
            }
        } catch (cancelled: CancellationException) {
            finish(request, Outcome(null, PhoneticCancelledException("Phonetics was disposed")))
            throw cancelled
        } catch (failure: Throwable) {
            val error = failure as? PhoneticException ?: PhoneticWorkerException("$failure")
            finish(request, Outcome(null, error))
        }
    }

    private fun register(request: Request<*>) {
        while (true) {
            val current = pending.load()
            if (pending.compareAndSet(current, current + request)) return
        }
    }

    private fun finish(request: Request<*>, outcome: Outcome<*>) {
        @Suppress("UNCHECKED_CAST")
        (request as Request<Any?>).complete(outcome as Outcome<Any?>)
        while (true) {
            val current = pending.load()
            if (request !in current) return
            if (pending.compareAndSet(current, current - request)) return
        }
    }

    private fun cancelPending(error: PhoneticException) {
        for (request in pending.load()) finish(request, Outcome(null, error))
    }

    private class Request<T> {
        val outcome = CompletableDeferred<Outcome<T>>()

        val isFinished: Boolean
            get() = outcome.isCompleted

        fun complete(result: Outcome<T>) {
            outcome.complete(result)
        }
    }
}

internal class OneShotJob<T>(private val compute: () -> T) : PhoneticJob<T>() {
    private var finished = false
    private var computed: T? = null

    override val isDone: Boolean
        get() = finished

    @Suppress("UNCHECKED_CAST")
    override val result: T
        get() = computed as T

    override fun advance(workUnits: Int) {
        computed = compute()
        finished = true
    }
}
