package io.github.zakaria5729.phonetics

import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

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
    private val scheduler = PhoneticScheduler(executionMode, workerDispatcher, callbackDispatcher)

    /** The kind of worker running the requests. */
    public val workerName: String
        get() = platformWorkerName

    /** How overlapping requests are handled. */
    public val executionMode: PhoneticExecutionMode
        get() = scheduler.executionMode

    /** Changes how overlapping requests are handled, including on shared instances. */
    public fun setExecutionMode(mode: PhoneticExecutionMode) {
        scheduler.executionMode = mode
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
        scheduler.cancelAll()
    }

    /** Stops the background work. Later calls return a disposed error. */
    public fun dispose() {
        scheduler.dispose("Phonetics was disposed")
    }

    private fun <R> deliver(
        cancelToken: PhoneticCancelToken,
        onResult: (R) -> Unit,
        compute: suspend () -> R,
    ): PhoneticCancelToken = scheduler.deliver(cancelToken, onResult, compute)

    private suspend fun <T> execute(
        cancelToken: PhoneticCancelToken?,
        buildJob: () -> PhoneticJob<T>,
    ): Outcome<T> = scheduler.execute(cancelToken, engine.error, buildJob)
}
