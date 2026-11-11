package io.github.zakaria5729.phonetics

internal object BanglaCharacters {
    const val HASANTA = '্'
    const val KHANDA_TA = 'ৎ'
    const val YA_WITH_DOT = 'য়'

    private const val CONSONANTS = "কখগঘঙচছজঝঞটঠডঢণতথদধনপফবভমযরলশষসহড়ঢ়য়ৎ"
    private const val INDEPENDENT_VOWELS = "অআইঈউঊঋএঐওঔ"
    private const val VOWEL_SIGNS = "ািীুূৃেৈোৌ"
    private const val NASAL_MARKS = "ংঃঁ"
    private const val IGNORED_MARKS = "‌‍়ঽৗ"
    private const val BENGALI_DIGITS = "০১২৩৪৫৬৭৮৯"
    private const val WORD_SEPARATORS = " -.'"

    fun isConsonant(character: Char): Boolean = character in CONSONANTS

    fun isIndependentVowel(character: Char): Boolean = character in INDEPENDENT_VOWELS

    fun isVowelSign(character: Char): Boolean = character in VOWEL_SIGNS

    fun isNasalMark(character: Char): Boolean = character in NASAL_MARKS

    fun isIgnoredMark(character: Char): Boolean = character in IGNORED_MARKS

    fun isWordSeparator(character: Char): Boolean = character in WORD_SEPARATORS

    fun asciiDigitFor(character: Char): Char? =
        BENGALI_DIGITS.indexOf(character).takeIf { it >= 0 }?.let { '0' + it }
}

internal sealed interface WordToken {
    class IndependentVowel(val letter: Char) : WordToken

    class ConsonantGroup(
        val consonants: List<Char>,
        val vowelSign: Char?,
        val endsWithHasanta: Boolean,
    ) : WordToken {
        val hasInherentVowel: Boolean
            get() = vowelSign == null && !endsWithHasanta

        val isConjunct: Boolean
            get() = consonants.size > 1
    }

    class NasalMark(val mark: Char) : WordToken

    class LiteralText(val text: String) : WordToken

    class WordBreak(val text: String) : WordToken
}

internal sealed interface BanglaParseOutcome {
    class Parsed(val tokens: List<WordToken>) : BanglaParseOutcome

    class Rejected(val reason: String) : BanglaParseOutcome
}

internal fun parseBanglaText(rawText: String): BanglaParseOutcome {
    val text = rawText.filterNot(BanglaCharacters::isIgnoredMark)
    val tokens = mutableListOf<WordToken>()
    var index = 0
    var foundBanglaLetter = false

    while (index < text.length) {
        val character = text[index]
        when {
            BanglaCharacters.isConsonant(character) -> {
                foundBanglaLetter = true
                index = readConsonantGroup(text, index, tokens)
            }

            BanglaCharacters.isIndependentVowel(character) -> {
                foundBanglaLetter = true
                tokens.add(WordToken.IndependentVowel(character))
                index++
            }

            BanglaCharacters.isNasalMark(character) -> {
                tokens.add(WordToken.NasalMark(character))
                index++
            }

            BanglaCharacters.isVowelSign(character) || character == BanglaCharacters.HASANTA -> index++

            BanglaCharacters.isWordSeparator(character) -> {
                tokens.add(WordToken.WordBreak(character.toString()))
                index++
            }

            BanglaCharacters.asciiDigitFor(character) != null -> {
                tokens.add(WordToken.LiteralText(BanglaCharacters.asciiDigitFor(character).toString()))
                index++
            }

            character in '0'..'9' -> {
                tokens.add(WordToken.LiteralText(character.toString()))
                index++
            }

            character in 'a'..'z' || character in 'A'..'Z' -> {
                tokens.add(WordToken.LiteralText(character.lowercaseChar().toString()))
                index++
            }

            else -> return BanglaParseOutcome.Rejected(
                "unsupported character U+${character.code.toString(16).uppercase().padStart(4, '0')} " +
                    "at position $index",
            )
        }
    }

    if (!foundBanglaLetter && tokens.any { it !is WordToken.WordBreak }) {
        return BanglaParseOutcome.Rejected("the input has no Bangla letters")
    }
    return BanglaParseOutcome.Parsed(tokens)
}

private fun readConsonantGroup(text: String, start: Int, tokens: MutableList<WordToken>): Int {
    val consonants = mutableListOf(text[start])
    var index = start + 1
    var endsWithHasanta = text[start] == BanglaCharacters.KHANDA_TA

    while (!endsWithHasanta && index < text.length && text[index] == BanglaCharacters.HASANTA) {
        index++
        val following = text.getOrNull(index)
        if (following != null && BanglaCharacters.isConsonant(following) &&
            following != BanglaCharacters.KHANDA_TA
        ) {
            consonants.add(following)
            index++
        } else {
            endsWithHasanta = true
        }
    }

    var vowelSign: Char? = null
    if (!endsWithHasanta && index < text.length && BanglaCharacters.isVowelSign(text[index])) {
        vowelSign = text[index]
        index++
    }

    tokens.add(WordToken.ConsonantGroup(consonants, vowelSign, endsWithHasanta))
    return index
}
