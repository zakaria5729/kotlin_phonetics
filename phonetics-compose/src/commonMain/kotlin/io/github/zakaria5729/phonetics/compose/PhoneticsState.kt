package io.github.zakaria5729.phonetics.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import io.github.zakaria5729.phonetics.BanglaPhonetics
import io.github.zakaria5729.phonetics.EnglishPhonetics
import io.github.zakaria5729.phonetics.PhoneticExecutionMode
import io.github.zakaria5729.phonetics.PhoneticLanguage
import io.github.zakaria5729.phonetics.PhoneticMatchesResult
import io.github.zakaria5729.phonetics.PhoneticRuleSet
import io.github.zakaria5729.phonetics.PhoneticSimilarityResult
import io.github.zakaria5729.phonetics.Phonetics
import kotlinx.coroutines.delay

/**
 * Remembers a [Phonetics] for [language] and disposes it when it leaves the composition.
 *
 * A new instance is created when [language], [rules] or [executionMode] change.
 */
@Composable
public fun rememberPhonetics(
    language: PhoneticLanguage,
    rules: PhoneticRuleSet? = null,
    executionMode: PhoneticExecutionMode = PhoneticExecutionMode.CancelPrevious,
): Phonetics {
    val phonetics = remember(language, rules, executionMode) {
        Phonetics(language, rules, executionMode = executionMode)
    }
    DisposableEffect(phonetics) {
        onDispose { phonetics.dispose() }
    }
    return phonetics
}

/** Remembers a [BanglaPhonetics] and disposes it when it leaves the composition. */
@Composable
public fun rememberBanglaPhonetics(
    rules: PhoneticRuleSet? = null,
    executionMode: PhoneticExecutionMode = PhoneticExecutionMode.CancelPrevious,
): BanglaPhonetics {
    val phonetics = remember(rules, executionMode) { BanglaPhonetics(rules, executionMode = executionMode) }
    DisposableEffect(phonetics) {
        onDispose { phonetics.dispose() }
    }
    return phonetics
}

/** Remembers an [EnglishPhonetics] and disposes it when it leaves the composition. */
@Composable
public fun rememberEnglishPhonetics(
    rules: PhoneticRuleSet? = null,
    executionMode: PhoneticExecutionMode = PhoneticExecutionMode.CancelPrevious,
): EnglishPhonetics {
    val phonetics = remember(rules, executionMode) { EnglishPhonetics(rules, executionMode = executionMode) }
    DisposableEffect(phonetics) {
        onDispose { phonetics.dispose() }
    }
    return phonetics
}

/**
 * The sound-alike spellings of [input], recomputed when the arguments change.
 *
 * The state holds `null` until the first result arrives and keeps the previous
 * result while a new one is computed. A running request is cancelled when the
 * arguments change, and [debounceMillis] waits for typing to pause first.
 */
@Composable
public fun rememberPhoneticMatches(
    phonetics: Phonetics,
    input: String,
    minScore: Double = 70.0,
    maxResults: Int? = 50,
    includeOriginal: Boolean = true,
    maxChanges: Int? = null,
    debounceMillis: Long = 0,
): State<PhoneticMatchesResult?> = produceState<PhoneticMatchesResult?>(
    initialValue = null,
    phonetics, input, minScore, maxResults, includeOriginal, maxChanges, debounceMillis,
) {
    if (debounceMillis > 0) delay(debounceMillis)
    value = phonetics.generate(input, minScore, maxResults, includeOriginal, maxChanges)
}

/**
 * How alike [a] and [b] sound, recomputed when the arguments change.
 *
 * See [rememberPhoneticMatches] for how the state behaves.
 */
@Composable
public fun rememberPhoneticSimilarity(
    phonetics: Phonetics,
    a: String,
    b: String,
    debounceMillis: Long = 0,
): State<PhoneticSimilarityResult?> = produceState<PhoneticSimilarityResult?>(
    initialValue = null,
    phonetics, a, b, debounceMillis,
) {
    if (debounceMillis > 0) delay(debounceMillis)
    value = phonetics.similarity(a, b)
}

/**
 * [candidates] ordered by how closely they sound like [query], recomputed when the arguments change.
 *
 * See [rememberPhoneticMatches] for how the state behaves.
 */
@Composable
public fun rememberPhoneticRanking(
    phonetics: Phonetics,
    query: String,
    candidates: List<String>,
    minScore: Double = 0.0,
    limit: Int? = null,
    debounceMillis: Long = 0,
): State<PhoneticMatchesResult?> = produceState<PhoneticMatchesResult?>(
    initialValue = null,
    phonetics, query, candidates, minScore, limit, debounceMillis,
) {
    if (debounceMillis > 0) delay(debounceMillis)
    value = phonetics.rank(query, candidates, minScore, limit)
}
