package io.github.zakaria5729.phonetics

public data class PhoneticSearchOptions(
    public val minScore: Double = 70.0,
    public val maxResults: Int? = 50,
    public val includeOriginal: Boolean = true,
    public val maxChanges: Int? = null,
)

public data class CrossLanguageResult(
    public val bangla: PhoneticMatchesResult,
    public val english: PhoneticMatchesResult,
) {
    public val error: PhoneticException?
        get() = bangla.error ?: english.error

    public operator fun get(language: PhoneticLanguage): PhoneticMatchesResult? = when (language.code) {
        banglaLanguage.code -> bangla
        englishLanguage.code -> english
        else -> null
    }
}

internal val bothSupportedLanguages: Set<PhoneticLanguage>
    get() = setOf(banglaLanguage, englishLanguage)

internal fun crossLanguageArgumentError(
    languages: Set<PhoneticLanguage>,
    banglaOptions: PhoneticSearchOptions,
    englishOptions: PhoneticSearchOptions,
): PhoneticArgumentException? {
    if (languages.isEmpty()) return PhoneticArgumentException("languages must not be empty")
    languages.firstOrNull { it.code != banglaLanguage.code && it.code != englishLanguage.code }?.let {
        return PhoneticArgumentException("cross-language generation does not support the language \"${it.code}\"")
    }
    if (languages.any { it.code == banglaLanguage.code }) {
        generateArgumentError(banglaOptions.minScore, banglaOptions.maxResults, banglaOptions.maxChanges)
            ?.let { return PhoneticArgumentException("Bangla options: ${it.message}") }
    }
    if (languages.any { it.code == englishLanguage.code }) {
        generateArgumentError(englishOptions.minScore, englishOptions.maxResults, englishOptions.maxChanges)
            ?.let { return PhoneticArgumentException("English options: ${it.message}") }
    }
    return null
}

internal fun failedCrossLanguageResult(error: PhoneticException): CrossLanguageResult =
    CrossLanguageResult(
        PhoneticMatchesResult(emptyList(), error),
        PhoneticMatchesResult(emptyList(), error),
    )
