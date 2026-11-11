package io.github.zakaria5729.phonetics

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

public class CrossLanguagePhonetics(
    banglaRules: PhoneticRuleSet? = null,
    gapSimilarity: Double = 0.5,
    markGapSimilarity: Double = 0.85,
    unrelatedSimilarity: Double = 0.4,
    executionMode: PhoneticExecutionMode = PhoneticExecutionMode.CancelPrevious,
    workerDispatcher: CoroutineDispatcher = Dispatchers.Default,
    callbackDispatcher: CoroutineDispatcher? = null,
) {
    private val engine = CrossLanguageEngine(banglaRules, gapSimilarity, markGapSimilarity, unrelatedSimilarity)
    private val scheduler = PhoneticScheduler(executionMode, workerDispatcher, callbackDispatcher)

    public val workerName: String
        get() = platformWorkerName

    public val executionMode: PhoneticExecutionMode
        get() = scheduler.executionMode

    public fun setExecutionMode(mode: PhoneticExecutionMode) {
        scheduler.executionMode = mode
    }

    public suspend fun generate(
        input: String,
        languages: Set<PhoneticLanguage> = bothSupportedLanguages,
        minScore: Double = 70.0,
        maxResults: Int? = 50,
        includeOriginal: Boolean = true,
        maxChanges: Int? = null,
        cancelToken: PhoneticCancelToken? = null,
    ): CrossLanguageResult {
        val options = PhoneticSearchOptions(minScore, maxResults, includeOriginal, maxChanges)
        return generate(input, languages, options, options, cancelToken)
    }

    public suspend fun generate(
        input: String,
        languages: Set<PhoneticLanguage>,
        banglaOptions: PhoneticSearchOptions,
        englishOptions: PhoneticSearchOptions,
        cancelToken: PhoneticCancelToken? = null,
    ): CrossLanguageResult {
        crossLanguageArgumentError(languages, banglaOptions, englishOptions)?.let {
            return failedCrossLanguageResult(it)
        }
        val outcome = scheduler.execute(cancelToken, setupError = null) {
            engine.createJob(input, languages, banglaOptions, englishOptions)
        }
        return outcome.value ?: failedCrossLanguageResult(outcome.error ?: PhoneticWorkerException("no result"))
    }

    public suspend fun generate(input: String): CrossLanguageResult = generate(input, bothSupportedLanguages, 70.0)

    public fun generateAsync(
        input: String,
        languages: Set<PhoneticLanguage> = bothSupportedLanguages,
        minScore: Double = 70.0,
        maxResults: Int? = 50,
        includeOriginal: Boolean = true,
        maxChanges: Int? = null,
        cancelToken: PhoneticCancelToken = PhoneticCancelToken(),
        onResult: (CrossLanguageResult) -> Unit,
    ): PhoneticCancelToken = scheduler.deliver(cancelToken, onResult) {
        generate(input, languages, minScore, maxResults, includeOriginal, maxChanges, cancelToken)
    }

    public fun generateAsync(input: String, onResult: (CrossLanguageResult) -> Unit): PhoneticCancelToken =
        generateAsync(input, bothSupportedLanguages, 70.0, onResult = onResult)

    public fun cancelAll() {
        scheduler.cancelAll()
    }

    public fun dispose() {
        scheduler.dispose("CrossLanguagePhonetics was disposed")
    }

    public companion object {
        public val instance: CrossLanguagePhonetics by lazy { CrossLanguagePhonetics() }
    }
}
