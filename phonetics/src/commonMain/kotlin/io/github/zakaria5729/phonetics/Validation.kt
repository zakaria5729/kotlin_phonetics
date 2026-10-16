package io.github.zakaria5729.phonetics

internal fun minScoreError(minScore: Double): PhoneticArgumentException? =
    if (minScore.isNaN() || minScore < 0 || minScore > 100) {
        PhoneticArgumentException("minScore must be between 0 and 100 (got $minScore)")
    } else {
        null
    }

internal fun generateArgumentError(
    minScore: Double,
    maxResults: Int?,
    maxChanges: Int?,
): PhoneticArgumentException? {
    minScoreError(minScore)?.let { return it }
    if (maxResults != null && maxResults < 1) {
        return PhoneticArgumentException("maxResults must be at least 1 (got $maxResults)")
    }
    if (maxChanges != null && maxChanges < 0) {
        return PhoneticArgumentException("maxChanges must not be negative (got $maxChanges)")
    }
    return null
}

internal fun rankArgumentError(minScore: Double, limit: Int?): PhoneticArgumentException? {
    minScoreError(minScore)?.let { return it }
    if (limit != null && limit < 1) {
        return PhoneticArgumentException("limit must be at least 1 (got $limit)")
    }
    return null
}
