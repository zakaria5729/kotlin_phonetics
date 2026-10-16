package io.github.zakaria5729.phonetics

/** Matches with their scores, plus an error if the request failed. */
public data class PhoneticMatchesResult(
    public val matches: List<PhoneticMatch>,
    public val error: PhoneticException?,
)

/** Spellings only, plus an error if the request failed. */
public data class PhoneticWordsResult(
    public val words: List<String>,
    public val error: PhoneticException?,
)

/** A similarity score from 0 to 100, plus an error if the request failed. */
public data class PhoneticSimilarityResult(
    public val score: Double,
    public val error: PhoneticException?,
)

/** Whether two spellings match, plus an error if the request failed. */
public data class PhoneticMatchCheckResult(
    public val isMatch: Boolean,
    public val error: PhoneticException?,
)
