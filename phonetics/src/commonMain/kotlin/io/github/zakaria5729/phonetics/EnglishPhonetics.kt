package io.github.zakaria5729.phonetics

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/** Sound-alike spelling search for English. */
public class EnglishPhonetics(
    rules: PhoneticRuleSet? = null,
    gapSimilarity: Double = 0.5,
    markGapSimilarity: Double = 0.85,
    unrelatedSimilarity: Double = 0.4,
    executionMode: PhoneticExecutionMode = PhoneticExecutionMode.CancelPrevious,
    workerDispatcher: CoroutineDispatcher = Dispatchers.Default,
    callbackDispatcher: CoroutineDispatcher? = null,
) : Phonetics(
    englishLanguage,
    rules,
    gapSimilarity,
    markGapSimilarity,
    unrelatedSimilarity,
    executionMode,
    workerDispatcher,
    callbackDispatcher,
) {
    public companion object {
        /** A shared instance with the default settings. */
        public val instance: EnglishPhonetics by lazy { EnglishPhonetics() }
    }
}
