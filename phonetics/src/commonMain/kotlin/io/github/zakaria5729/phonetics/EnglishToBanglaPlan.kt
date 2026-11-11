package io.github.zakaria5729.phonetics

internal fun planBanglaSpellings(lowercaseEnglishText: String): SpellingPlan {
    if (lowercaseEnglishText.isEmpty()) return SpellingPlan.Ready("", emptyList())

    val units = when (val outcome = parseEnglishText(lowercaseEnglishText)) {
        is EnglishParseOutcome.Rejected -> return SpellingPlan.Rejected(outcome.reason)
        is EnglishParseOutcome.Parsed -> outcome.units
    }

    val planBuilder = SpellingPlanBuilder()
    val currentWord = mutableListOf<EnglishUnit>()
    for (unit in units) {
        if (unit is EnglishUnit.WordBreak) {
            appendEnglishWord(planBuilder, currentWord)
            currentWord.clear()
            planBuilder.appendFixed(unit.text)
        } else {
            currentWord.add(unit)
        }
    }
    appendEnglishWord(planBuilder, currentWord)
    return planBuilder.build()
}

private fun appendEnglishWord(planBuilder: SpellingPlanBuilder, wordUnits: List<EnglishUnit>) {
    var previousConsonant: EnglishUnit.Consonant? = null
    var previousConsonantIsAtWordStart = false
    var previousWasVowelOrStart = true

    for ((index, unit) in wordUnits.withIndex()) {
        when (unit) {
            is EnglishUnit.Literal -> {
                planBuilder.appendFixed(unit.text)
                previousConsonant = null
                previousWasVowelOrStart = true
            }

            is EnglishUnit.Vowel -> {
                val atWordEnd = index == wordUnits.lastIndex
                val beforeNasalNg = (wordUnits.getOrNull(index + 1) as? EnglishUnit.Consonant)
                    ?.let { BanglaSpellingOptions.isNasalGroup(it.letters) } == true
                planBuilder.appendChoice(
                    BanglaSpellingOptions.forVowel(
                        unit.letters,
                        afterConsonant = previousConsonant != null,
                        atWordEnd = atWordEnd,
                        beforeNasalNg = beforeNasalNg,
                    ),
                )
                previousConsonant = null
                previousWasVowelOrStart = true
            }

            is EnglishUnit.Consonant -> {
                val following = wordUnits.getOrNull(index + 1)
                if (previousConsonant != null &&
                    !BanglaSpellingOptions.isNasalGroup(previousConsonant.letters) &&
                    !BanglaSpellingOptions.isNasalGroup(unit.letters)
                ) {
                    planBuilder.appendChoice(
                        BanglaSpellingOptions.forConjunctJoin(
                            previousConsonant.letters,
                            unit.letters,
                            previousConsonantIsAtWordStart,
                        ),
                    )
                }
                planBuilder.appendChoice(
                    BanglaSpellingOptions.forConsonant(
                        letters = unit.letters,
                        isDoubled = unit.isDoubled,
                        atWordStart = index == 0,
                        nextIsVowel = following is EnglishUnit.Vowel,
                        nextVowelLetters = (following as? EnglishUnit.Vowel)?.letters,
                        followsWordInitialS = previousConsonant?.letters == "s" && previousConsonantIsAtWordStart,
                    ),
                )
                previousConsonantIsAtWordStart = previousConsonant == null && previousWasVowelOrStart && index == 0
                previousConsonant = unit
                previousWasVowelOrStart = false
            }

            is EnglishUnit.WordBreak -> Unit
        }
    }
}
