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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.yield

private const val WORK_UNITS_PER_SLICE = 16
private val sliceDuration = 8.milliseconds

/**
 * Finds sound-alike spellings of a word and scores how close two spellings are.
 *
 * The work runs off the calling thread: on a background thread for Android, JVM
 * and native targets, and in short time slices on the event loop for Kotlin/JS
 * and Wasm. Every function returns a result object holding the value and an
 * `error`; nothing is thrown, except that a cancelled coroutine keeps the usual
 * [CancellationException] behaviour.
 */
public open class Phonetics(
    /** The language this instance works with. */
    public val language: PhoneticLanguage,
    rules: PhoneticRuleSet? = null,
    gapSimilarity: Double = 0.5,
    markGapSimilarity: Double = 0.85,
    unrelatedSimilarity: Double = 0.4,
    executionMode: PhoneticExecutionMode = PhoneticExecutionMode.CancelPrevious,
    workerDispatcher: CoroutineDispatcher = Dispatchers.Default,
    callbackDispatcher: CoroutineDispatcher? = null,
) {
    private val engine = PhoneticEngine(language, rules, gapSimilarity, markGapSimilarity, unrelatedSimilarity)
    private val workerScope = CoroutineScope(SupervisorJob() + workerDispatcher)
    private val callbackScope = CoroutineScope(SupervisorJob() + (callbackDispatcher ?: mainOrDefaultDispatcher()))
    private val jobLock = Mutex()
    private val pending = AtomicReference<List<Request<*>>>(emptyList())
    private val disposed = AtomicBoolean(false)

    @Volatile
    private var currentMode = executionMode

    /** The kind of worker running the requests. */
    public val workerName: String
        get() = platformWorkerName

    /** How overlapping requests are handled. */
    public val executionMode: PhoneticExecutionMode
        get() = currentMode

    /** Changes how overlapping requests are handled, including on shared instances. */
    public fun setExecutionMode(mode: PhoneticExecutionMode) {
        currentMode = mode
    }

    /**
     * Generates spelling variants of [input] that sound alike, best match first.
     *
     * Only variants scoring at least [minScore] (0 to 100) are returned, up to
     * [maxResults]; pass `null` for no limit. [includeOriginal] keeps the input
     * in the list, [maxChanges] limits how many letters may differ, and
     * [cancelToken] stops the request early.
     */
    public suspend fun generate(
        input: String,
        minScore: Double = 70.0,
        maxResults: Int? = 50,
        includeOriginal: Boolean = true,
        maxChanges: Int? = null,
        cancelToken: PhoneticCancelToken? = null,
    ): PhoneticMatchesResult {
        generateArgumentError(minScore, maxResults, maxChanges)?.let {
            return PhoneticMatchesResult(emptyList(), it)
        }
        val outcome = execute(cancelToken) {
            engine.createGenerateJob(input, minScore, maxResults, includeOriginal, maxChanges)
        }
        return PhoneticMatchesResult(outcome.value ?: emptyList(), outcome.error)
    }

    /** Like [generate], but returns only the spellings without scores. */
    public suspend fun generateWords(
        input: String,
        minScore: Double = 70.0,
        maxResults: Int? = 50,
        includeOriginal: Boolean = true,
        maxChanges: Int? = null,
        cancelToken: PhoneticCancelToken? = null,
    ): PhoneticWordsResult {
        val result = generate(input, minScore, maxResults, includeOriginal, maxChanges, cancelToken)
        return PhoneticWordsResult(result.matches.map { it.word }, result.error)
    }

    /** Scores how alike [a] and [b] sound, from 0 to 100. */
    public suspend fun similarity(
        a: String,
        b: String,
        cancelToken: PhoneticCancelToken? = null,
    ): PhoneticSimilarityResult {
        val outcome = execute(cancelToken) { OneShotJob { engine.similarity(a, b) } }
        val result = outcome.value
        return PhoneticSimilarityResult(result?.score ?: 0.0, outcome.error ?: result?.error)
    }

    /** Tells whether [a] and [b] sound alike, meaning their similarity is at least [minScore]. */
    public suspend fun matches(
        a: String,
        b: String,
        minScore: Double = 80.0,
        cancelToken: PhoneticCancelToken? = null,
    ): PhoneticMatchCheckResult {
        minScoreError(minScore)?.let { return PhoneticMatchCheckResult(false, it) }
        val result = similarity(a, b, cancelToken)
        return PhoneticMatchCheckResult(result.score >= minScore, result.error)
    }

    /**
     * Orders [candidates] by how closely they sound like [query].
     *
     * Candidates scoring below [minScore] are dropped, and [limit] keeps only
     * the best ones.
     */
    public suspend fun rank(
        query: String,
        candidates: Iterable<String>,
        minScore: Double = 0.0,
        limit: Int? = null,
        cancelToken: PhoneticCancelToken? = null,
    ): PhoneticMatchesResult {
        rankArgumentError(minScore, limit)?.let { return PhoneticMatchesResult(emptyList(), it) }
        val candidateList = try {
            candidates.toList()
        } catch (failure: Throwable) {
            return PhoneticMatchesResult(
                emptyList(),
                PhoneticArgumentException("could not read the arguments: $failure"),
            )
        }
        val outcome = execute(cancelToken) { engine.createRankJob(query, candidateList, minScore, limit) }
        return PhoneticMatchesResult(outcome.value ?: emptyList(), outcome.error)
    }

    /** [generate] with the default settings. */
    public suspend fun generate(input: String): PhoneticMatchesResult = generate(input, 70.0)

    /** [generateWords] with the default settings. */
    public suspend fun generateWords(input: String): PhoneticWordsResult = generateWords(input, 70.0)

    /** [similarity] without a cancel token. */
    public suspend fun similarity(a: String, b: String): PhoneticSimilarityResult = similarity(a, b, null)

    /** [matches] with the default threshold. */
    public suspend fun matches(a: String, b: String): PhoneticMatchCheckResult = matches(a, b, 80.0)

    /** [rank] with the default settings. */
    public suspend fun rank(query: String, candidates: Iterable<String>): PhoneticMatchesResult =
        rank(query, candidates, 0.0)

    /** Callback form of [generate]. Returns the token that cancels the request. */
    public fun generateAsync(
        input: String,
        minScore: Double = 70.0,
        maxResults: Int? = 50,
        includeOriginal: Boolean = true,
        maxChanges: Int? = null,
        cancelToken: PhoneticCancelToken = PhoneticCancelToken(),
        onResult: (PhoneticMatchesResult) -> Unit,
    ): PhoneticCancelToken = deliver(cancelToken, onResult) {
        generate(input, minScore, maxResults, includeOriginal, maxChanges, cancelToken)
    }

    /** Callback form of [generateWords]. Returns the token that cancels the request. */
    public fun generateWordsAsync(
        input: String,
        minScore: Double = 70.0,
        maxResults: Int? = 50,
        includeOriginal: Boolean = true,
        maxChanges: Int? = null,
        cancelToken: PhoneticCancelToken = PhoneticCancelToken(),
        onResult: (PhoneticWordsResult) -> Unit,
    ): PhoneticCancelToken = deliver(cancelToken, onResult) {
        generateWords(input, minScore, maxResults, includeOriginal, maxChanges, cancelToken)
    }

    /** Callback form of [similarity]. Returns the token that cancels the request. */
    public fun similarityAsync(
        a: String,
        b: String,
        cancelToken: PhoneticCancelToken = PhoneticCancelToken(),
        onResult: (PhoneticSimilarityResult) -> Unit,
    ): PhoneticCancelToken = deliver(cancelToken, onResult) { similarity(a, b, cancelToken) }

    /** Callback form of [matches]. Returns the token that cancels the request. */
    public fun matchesAsync(
        a: String,
        b: String,
        minScore: Double = 80.0,
        cancelToken: PhoneticCancelToken = PhoneticCancelToken(),
        onResult: (PhoneticMatchCheckResult) -> Unit,
    ): PhoneticCancelToken = deliver(cancelToken, onResult) { matches(a, b, minScore, cancelToken) }

    /** Callback form of [rank]. Returns the token that cancels the request. */
    public fun rankAsync(
        query: String,
        candidates: Iterable<String>,
        minScore: Double = 0.0,
        limit: Int? = null,
        cancelToken: PhoneticCancelToken = PhoneticCancelToken(),
        onResult: (PhoneticMatchesResult) -> Unit,
    ): PhoneticCancelToken = deliver(cancelToken, onResult) {
        rank(query, candidates, minScore, limit, cancelToken)
    }

    /** [generateAsync] with the default settings. */
    public fun generateAsync(input: String, onResult: (PhoneticMatchesResult) -> Unit): PhoneticCancelToken =
        generateAsync(input, 70.0, onResult = onResult)

    /** [generateWordsAsync] with the default settings. */
    public fun generateWordsAsync(input: String, onResult: (PhoneticWordsResult) -> Unit): PhoneticCancelToken =
        generateWordsAsync(input, 70.0, onResult = onResult)

    /** [similarityAsync] with a new cancel token. */
    public fun similarityAsync(a: String, b: String, onResult: (PhoneticSimilarityResult) -> Unit): PhoneticCancelToken =
        similarityAsync(a, b, PhoneticCancelToken(), onResult)

    /** [matchesAsync] with the default threshold. */
    public fun matchesAsync(a: String, b: String, onResult: (PhoneticMatchCheckResult) -> Unit): PhoneticCancelToken =
        matchesAsync(a, b, 80.0, onResult = onResult)

    /** [rankAsync] with the default settings. */
    public fun rankAsync(
        query: String,
        candidates: Iterable<String>,
        onResult: (PhoneticMatchesResult) -> Unit,
    ): PhoneticCancelToken = rankAsync(query, candidates, 0.0, onResult = onResult)

    /** Cancels every request that has not finished yet. */
    public fun cancelAll() {
        cancelPending(PhoneticCancelledException())
    }

    /** Stops the background work. Later calls return a disposed error. */
    public fun dispose() {
        if (!disposed.compareAndSet(expectedValue = false, newValue = true)) return
        cancelPending(PhoneticCancelledException("Phonetics was disposed"))
        workerScope.cancel()
        callbackScope.cancel()
    }

    private fun <R> deliver(
        cancelToken: PhoneticCancelToken,
        onResult: (R) -> Unit,
        compute: suspend () -> R,
    ): PhoneticCancelToken {
        callbackScope.launch {
            onResult(compute())
        }
        return cancelToken
    }

    private suspend fun <T> execute(
        cancelToken: PhoneticCancelToken?,
        buildJob: () -> PhoneticJob<T>,
    ): Outcome<T> {
        if (disposed.load()) return Outcome(null, PhoneticDisposedException())
        engine.error?.let { return Outcome(null, it) }
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

    private class Outcome<T>(val value: T?, val error: PhoneticException?)

    private class Request<T> {
        val outcome = CompletableDeferred<Outcome<T>>()

        val isFinished: Boolean
            get() = outcome.isCompleted

        fun complete(result: Outcome<T>) {
            outcome.complete(result)
        }
    }

    private class OneShotJob<T>(private val compute: () -> T) : PhoneticJob<T>() {
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
}
