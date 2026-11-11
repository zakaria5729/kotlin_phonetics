package io.github.zakaria5729.phonetics

internal fun planEnglishSpellings(normalizedBanglaText: String): SpellingPlan {
    if (normalizedBanglaText.isEmpty()) return SpellingPlan.Ready("", emptyList())

    val tokens = when (val outcome = parseBanglaText(normalizedBanglaText)) {
        is BanglaParseOutcome.Rejected -> return SpellingPlan.Rejected(outcome.reason)
        is BanglaParseOutcome.Parsed -> outcome.tokens
    }

    val planBuilder = SpellingPlanBuilder()
    val wordPlanner = BanglaToEnglishWordPlanner(planBuilder)
    val currentWord = mutableListOf<WordToken>()
    for (token in tokens) {
        if (token is WordToken.WordBreak) {
            wordPlanner.appendWord(currentWord)
            currentWord.clear()
            planBuilder.appendFixed(token.text)
        } else {
            currentWord.add(token)
        }
    }
    wordPlanner.appendWord(currentWord)
    return planBuilder.build()
}

internal fun capitalizeEachWord(text: String): String {
    val builder = StringBuilder(text.length)
    var atWordStart = true
    for (character in text) {
        builder.append(if (atWordStart) character.uppercaseChar() else character)
        atWordStart = character == ' ' || character == '-'
    }
    return builder.toString()
}

private class BanglaToEnglishWordPlanner(private val planBuilder: SpellingPlanBuilder) {
    private fun appendFixed(text: String) = planBuilder.appendFixed(text)

    private fun appendChoice(options: SpellingOptions) = planBuilder.appendChoice(options)

    fun appendWord(wordTokens: List<WordToken>) {
        if (wordTokens.isEmpty()) return
        val inherentVowelFates = resolveInherentVowelFates(wordTokens)
        var consonantsAlreadySpokenInNextGroup = 0
        var previousTokenEndedWithVowel = false
        var indexOfVowelAlreadySpoken = -1

        for ((index, token) in wordTokens.withIndex()) {
            if (index == indexOfVowelAlreadySpoken) continue
            val atWordStart = index == 0
            val atWordEnd = index == wordTokens.lastIndex
            val followingToken = wordTokens.getOrNull(index + 1)

            when (token) {
                is WordToken.LiteralText -> {
                    appendFixed(token.text)
                    previousTokenEndedWithVowel = false
                }

                is WordToken.NasalMark -> {
                    appendChoice(EnglishSpellingOptions.forNasalMark(token.mark))
                    previousTokenEndedWithVowel = false
                }

                is WordToken.IndependentVowel -> {
                    if (startsWithWa(token, atWordStart, followingToken)) {
                        appendChoice(EnglishSpellingOptions.forWordStartWa())
                        consonantsAlreadySpokenInNextGroup = 1
                    } else if (isIUVowelPair(token, followingToken)) {
                        appendChoice(EnglishSpellingOptions.forYuVowelPair())
                        indexOfVowelAlreadySpoken = index + 1
                    } else {
                        appendChoice(EnglishSpellingOptions.forVowel(soundOfIndependentVowel(token.letter), atWordEnd))
                    }
                    previousTokenEndedWithVowel = true
                }

                is WordToken.ConsonantGroup -> {
                    val consonants = token.consonants.drop(consonantsAlreadySpokenInNextGroup)
                    val startsAtWordStart = atWordStart && consonantsAlreadySpokenInNextGroup == 0
                    consonantsAlreadySpokenInNextGroup = 0
                    val inherentVowelFate = inherentVowelFates[index]

                    val mergesWithNextGroup = inherentVowelFate == InherentVowelFate.DELETED_INSIDE_WORD &&
                        consonants.size == 1 &&
                        (followingToken as? WordToken.ConsonantGroup)?.consonants?.firstOrNull() == consonants[0]

                    if (mergesWithNextGroup) {
                        appendChoice(
                            EnglishSpellingOptions.geminated(
                                EnglishSpellingOptions.forConsonant(consonants[0]),
                            ),
                        )
                        consonantsAlreadySpokenInNextGroup = 1
                    } else {
                        appendConsonants(consonants, startsAtWordStart, previousTokenEndedWithVowel)
                        appendVowelOfGroup(token, inherentVowelFate, atWordEnd)
                    }
                    previousTokenEndedWithVowel = token.vowelSign != null
                }

                is WordToken.WordBreak -> Unit
            }
        }
    }

    private fun startsWithWa(
        vowel: WordToken.IndependentVowel,
        atWordStart: Boolean,
        followingToken: WordToken?,
    ): Boolean = vowel.letter == 'ও' &&
        atWordStart &&
        followingToken is WordToken.ConsonantGroup &&
        followingToken.vowelSign != null &&
        followingToken.consonants.first() == BanglaCharacters.YA_WITH_DOT

    private fun isIUVowelPair(vowel: WordToken.IndependentVowel, followingToken: WordToken?): Boolean =
        (vowel.letter == 'ই' || vowel.letter == 'ঈ') &&
            followingToken is WordToken.IndependentVowel &&
            (followingToken.letter == 'উ' || followingToken.letter == 'ঊ')

    private fun appendConsonants(
        consonants: List<Char>,
        startsAtWordStart: Boolean,
        previousTokenEndedWithVowel: Boolean,
    ) {
        var position = 0
        while (position < consonants.size) {
            val current = consonants[position]
            val following = consonants.getOrNull(position + 1)
            val atWordStart = startsAtWordStart && position == 0
            val clusterOptions = following?.let { EnglishSpellingOptions.forCluster(current, it, atWordStart) }
            val phalaOptions = if (position > 0) EnglishSpellingOptions.forPhala(current) else null

            when {
                following != null && following == current -> {
                    appendChoice(EnglishSpellingOptions.geminated(EnglishSpellingOptions.forConsonant(current)))
                    position += 2
                }

                clusterOptions != null -> {
                    appendChoice(clusterOptions)
                    position += 2
                }

                phalaOptions != null -> {
                    appendChoice(phalaOptions)
                    position++
                }

                current == 'ঙ' && following != null && EnglishSpellingOptions.isVelar(following) -> {
                    appendChoice(EnglishSpellingOptions.forNasalConsonantBeforeVelar())
                    position++
                }

                current == BanglaCharacters.YA_WITH_DOT && position == 0 && previousTokenEndedWithVowel -> {
                    appendChoice(EnglishSpellingOptions.forYaAfterVowel())
                    position++
                }

                else -> {
                    appendChoice(EnglishSpellingOptions.forConsonant(current))
                    position++
                }
            }
        }
    }

    private fun appendVowelOfGroup(
        group: WordToken.ConsonantGroup,
        inherentVowelFate: InherentVowelFate?,
        atWordEnd: Boolean,
    ) {
        val sign = group.vowelSign
        if (sign != null) {
            appendChoice(EnglishSpellingOptions.forVowel(soundOfVowelSign(sign), atWordEnd))
            return
        }
        if (inherentVowelFate != null) {
            EnglishSpellingOptions.forInherentVowel(inherentVowelFate)?.let(::appendChoice)
        }
    }

    private fun soundOfIndependentVowel(letter: Char): VowelSound = when (letter) {
        'আ' -> VowelSound.A
        'ই', 'ঈ' -> VowelSound.I
        'উ', 'ঊ' -> VowelSound.U
        'ঋ' -> VowelSound.RI
        'এ' -> VowelSound.E
        'ঐ' -> VowelSound.OI
        'ও' -> VowelSound.O
        'ঔ' -> VowelSound.OU
        else -> VowelSound.OPEN_O
    }

    private fun soundOfVowelSign(sign: Char): VowelSound = when (sign) {
        'া' -> VowelSound.A
        'ি', 'ী' -> VowelSound.I
        'ু', 'ূ' -> VowelSound.U
        'ৃ' -> VowelSound.RI
        'ে' -> VowelSound.E
        'ৈ' -> VowelSound.OI
        'ো' -> VowelSound.O
        else -> VowelSound.OU
    }
}
