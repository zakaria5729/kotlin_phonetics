package io.github.zakaria5729.phonetics

internal enum class InherentVowelFate {
    REALIZED_AT_WORD_START,
    REALIZED_INSIDE_WORD,
    REALIZED_AT_WORD_END,
    DELETED_INSIDE_WORD,
    DELETED_AT_WORD_END,
}

internal fun resolveInherentVowelFates(wordTokens: List<WordToken>): Map<Int, InherentVowelFate> {
    val nucleusIndices = wordTokens.indices.filter { isSyllableNucleus(wordTokens[it]) }
    val fates = HashMap<Int, InherentVowelFate>()
    val realizedByNucleusPosition = BooleanArray(nucleusIndices.size) { true }

    for (position in nucleusIndices.indices.reversed()) {
        val tokenIndex = nucleusIndices[position]
        val group = wordTokens[tokenIndex] as? WordToken.ConsonantGroup
        if (group == null || !group.hasInherentVowel) continue

        val isFirstNucleus = position == 0
        val isLastNucleus = position == nucleusIndices.lastIndex
        val needsVowelBeforeNasalMark = wordTokens.getOrNull(tokenIndex + 1) is WordToken.NasalMark

        val fate = when {
            isFirstNucleus -> InherentVowelFate.REALIZED_AT_WORD_START
            isLastNucleus && needsVowelBeforeNasalMark -> InherentVowelFate.REALIZED_INSIDE_WORD
            isLastNucleus && group.isConjunct -> InherentVowelFate.REALIZED_AT_WORD_END
            isLastNucleus -> InherentVowelFate.DELETED_AT_WORD_END
            needsVowelBeforeNasalMark || group.isConjunct -> InherentVowelFate.REALIZED_INSIDE_WORD
            realizedByNucleusPosition[position + 1] -> InherentVowelFate.DELETED_INSIDE_WORD
            else -> InherentVowelFate.REALIZED_INSIDE_WORD
        }

        fates[tokenIndex] = fate
        realizedByNucleusPosition[position] = !fate.isDeleted
    }
    return fates
}

internal val InherentVowelFate.isDeleted: Boolean
    get() = this == InherentVowelFate.DELETED_INSIDE_WORD || this == InherentVowelFate.DELETED_AT_WORD_END

private fun isSyllableNucleus(token: WordToken): Boolean = when (token) {
    is WordToken.IndependentVowel -> true
    is WordToken.ConsonantGroup -> !token.endsWithHasanta
    else -> false
}
