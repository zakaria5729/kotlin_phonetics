package io.github.zakaria5729.phonetics

internal enum class InputScript { EMPTY, BANGLA, LATIN, UNSUPPORTED }

internal fun detectInputScript(input: String): InputScript {
    var hasLatinLetter = false
    var hasContent = false
    for (character in input) {
        when {
            character.code in 0x0980..0x09FF -> return InputScript.BANGLA
            character in 'a'..'z' || character in 'A'..'Z' || character.code in 0x00C0..0x024F -> {
                hasLatinLetter = true
                hasContent = true
            }
            !character.isWhitespace() -> hasContent = true
        }
    }
    return when {
        hasLatinLetter -> InputScript.LATIN
        hasContent -> InputScript.UNSUPPORTED
        else -> InputScript.EMPTY
    }
}

internal sealed interface EnglishUnit {
    class Consonant(val letters: String, val isDoubled: Boolean = false) : EnglishUnit

    class Vowel(val letters: String) : EnglishUnit

    class Literal(val text: String) : EnglishUnit

    class WordBreak(val text: String) : EnglishUnit
}

internal sealed interface EnglishParseOutcome {
    class Parsed(val units: List<EnglishUnit>) : EnglishParseOutcome

    class Rejected(val reason: String) : EnglishParseOutcome
}

private val consonantDigraphs = listOf("tch", "sh", "ch", "kh", "gh", "ph", "bh", "dh", "th", "jh", "ng", "ck")
private val vowelDigraphs = listOf("aa", "ee", "ea", "ie", "oo", "ou", "ow", "ai", "ay", "oi", "ei", "ey", "au", "aw")
private const val PLAIN_VOWELS = "aeiou"
private const val WORD_SEPARATORS = " -.'"

internal fun parseEnglishText(lowercaseText: String): EnglishParseOutcome {
    val units = mutableListOf<EnglishUnit>()
    var index = 0
    var wordStartIndex = 0
    var foundLetter = false

    while (index < lowercaseText.length) {
        val character = lowercaseText[index]
        when {
            character in WORD_SEPARATORS -> {
                units.add(EnglishUnit.WordBreak(character.toString()))
                index++
                wordStartIndex = index
            }

            character in '0'..'9' -> {
                units.add(EnglishUnit.Literal(character.toString()))
                index++
            }

            character in 'a'..'z' -> {
                foundLetter = true
                index = readEnglishUnit(lowercaseText, index, wordStartIndex, units)
            }

            else -> return EnglishParseOutcome.Rejected(
                "unsupported character U+${character.code.toString(16).uppercase().padStart(4, '0')} " +
                    "at position $index",
            )
        }
    }

    if (!foundLetter && units.any { it !is EnglishUnit.WordBreak }) {
        return EnglishParseOutcome.Rejected("the input has no letters")
    }
    return EnglishParseOutcome.Parsed(units)
}

private fun readEnglishUnit(text: String, index: Int, wordStartIndex: Int, units: MutableList<EnglishUnit>): Int {
    if (index == wordStartIndex && text.startsWith("yu", index)) {
        units.add(EnglishUnit.Vowel("yu"))
        return index + 2
    }

    val vowelDigraph = vowelDigraphs.firstOrNull { text.startsWith(it, index) }
    if (vowelDigraph != null) {
        val endsWithGlide = vowelDigraph.last() == 'y' || vowelDigraph.last() == 'w'
        val followedByVowel = text.getOrNull(index + vowelDigraph.length)?.let { it in PLAIN_VOWELS } == true
        if (!(endsWithGlide && followedByVowel)) {
            units.add(EnglishUnit.Vowel(vowelDigraph))
            return index + vowelDigraph.length
        }
    }

    val character = text[index]
    if (character in PLAIN_VOWELS) {
        units.add(EnglishUnit.Vowel(character.toString()))
        return index + 1
    }

    val consonantDigraph = consonantDigraphs.firstOrNull { text.startsWith(it, index) }
    if (consonantDigraph != null) {
        units.add(EnglishUnit.Consonant(consonantDigraph))
        return index + consonantDigraph.length
    }

    val atWordEnd = index + 1 >= text.length || text[index + 1] in WORD_SEPARATORS
    if (character == 'y' && atWordEnd && units.lastOrNull() is EnglishUnit.Consonant) {
        units.add(EnglishUnit.Vowel("y"))
        return index + 1
    }

    val previous = units.lastOrNull()
    if (previous is EnglishUnit.Consonant && !previous.isDoubled && previous.letters == character.toString()) {
        units[units.lastIndex] = EnglishUnit.Consonant(previous.letters, isDoubled = true)
    } else {
        units.add(EnglishUnit.Consonant(character.toString()))
    }
    return index + 1
}
