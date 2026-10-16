package io.github.zakaria5729.phonetics

internal enum class LetterCase { Unchanged, Lower, Title, Upper }

internal class TextNormalizer(
    val foldCase: Boolean,
    canonicalSequences: Map<String, String>,
    marks: Set<Int>,
) {
    private val sequencesByFirstUnit: Map<Char, List<Pair<String, String>>> =
        groupByFirstUnit(canonicalSequences)
    private val sequenceStarts = BooleanArray(CODE_UNIT_COUNT)
    private val markTable = BooleanArray(CODE_UNIT_COUNT)

    init {
        for (unit in sequencesByFirstUnit.keys) sequenceStarts[unit.code] = true
        for (mark in marks) {
            if (mark in 0 until CODE_UNIT_COUNT) markTable[mark] = true
        }
    }

    fun isMark(unit: Char): Boolean = markTable[unit.code]

    fun normalize(input: String): String {
        var text = trimLikeDart(input)
        if (text.isEmpty()) return text
        if (foldCase && hasUppercaseCandidate(text)) text = lowercaseWithoutFinalSigma(text)
        text = replaceSequences(text)
        return if (hasIrregularWhitespace(text)) collapseWhitespace(text) else text
    }

    fun detectCase(original: String): LetterCase {
        if (!foldCase) return LetterCase.Unchanged
        var hasUpper = false
        var hasLower = false
        var everyWordStartsUpper = true
        var atWordStart = true
        var index = 0
        while (index < original.length) {
            val length = if (original[index].isHighSurrogate() &&
                index + 1 < original.length &&
                original[index + 1].isLowSurrogate()
            ) {
                2
            } else {
                1
            }
            val letter = original.substring(index, index + length)
            index += length
            if (trimLikeDart(letter).isEmpty()) {
                atWordStart = true
                continue
            }
            val isUpper = isUpperLetter(letter)
            if (isUpper) hasUpper = true
            if (isLowerLetter(letter)) hasLower = true
            if (atWordStart && !isUpper) everyWordStartsUpper = false
            atWordStart = false
        }
        return when {
            !hasUpper && !hasLower -> LetterCase.Unchanged
            !hasLower -> LetterCase.Upper
            !hasUpper -> LetterCase.Lower
            everyWordStartsUpper -> LetterCase.Title
            else -> LetterCase.Lower
        }
    }

    fun applyCase(text: String, letterCase: LetterCase): String = when (letterCase) {
        LetterCase.Unchanged, LetterCase.Lower -> text
        LetterCase.Upper -> text.uppercase()
        LetterCase.Title -> text.split(' ').joinToString(" ") { word ->
            if (word.isEmpty()) word else firstLetterUppercase(word)
        }
    }

    private fun replaceSequences(text: String): String {
        if (sequencesByFirstUnit.isEmpty()) return text
        var output: StringBuilder? = null
        var copiedUpTo = 0
        var index = 0
        while (index < text.length) {
            val unit = text[index]
            if (!sequenceStarts[unit.code]) {
                index++
                continue
            }
            var replaced = false
            for ((from, to) in sequencesByFirstUnit.getValue(unit)) {
                if (text.startsWith(from, index)) {
                    val builder = output ?: StringBuilder().also { output = it }
                    builder.append(text, copiedUpTo, index).append(to)
                    index += from.length
                    copiedUpTo = index
                    replaced = true
                    break
                }
            }
            if (!replaced) index++
        }
        val builder = output ?: return text
        return builder.append(text, copiedUpTo, text.length).toString()
    }

    private companion object {
        const val CODE_UNIT_COUNT = 0x10000

        fun groupByFirstUnit(sequences: Map<String, String>): Map<Char, List<Pair<String, String>>> {
            val grouped = LinkedHashMap<Char, MutableList<Pair<String, String>>>()
            for ((from, to) in sequences) {
                if (from.isEmpty()) continue
                grouped.getOrPut(from[0]) { mutableListOf() }.add(from to to)
            }
            for (entries in grouped.values) {
                entries.sortByDescending { it.first.length }
            }
            return grouped
        }
    }
}

private fun lowercaseWithoutFinalSigma(text: String): String =
    text.replace('\u03A3', '\u03C3').lowercase()

private fun firstLetterUppercase(word: String): String =
    word[0].toString().uppercase() + word.substring(1)

private fun isTrimmedSpace(unit: Char): Boolean = when (unit.code) {
    in 0x09..0x0D, 0x20, 0x85, 0xA0, 0x1680, in 0x2000..0x200A,
    0x2028, 0x2029, 0x202F, 0x205F, 0x3000, 0xFEFF -> true
    else -> false
}

private fun isRegexSpace(unit: Char): Boolean = unit.code != 0x85 && isTrimmedSpace(unit)

internal fun trimLikeDart(text: String): String {
    var start = 0
    var end = text.length
    while (start < end && isTrimmedSpace(text[start])) start++
    while (end > start && isTrimmedSpace(text[end - 1])) end--
    return text.substring(start, end)
}

private fun collapseWhitespace(text: String): String {
    val output = StringBuilder(text.length)
    var inRun = false
    for (unit in text) {
        if (isRegexSpace(unit)) {
            if (!inRun) output.append(' ')
            inRun = true
        } else {
            output.append(unit)
            inRun = false
        }
    }
    return output.toString()
}

private fun codePointCount(text: String): Int {
    var count = 0
    var index = 0
    while (index < text.length) {
        index += if (text[index].isHighSurrogate() && index + 1 < text.length && text[index + 1].isLowSurrogate()) 2 else 1
        count++
    }
    return count
}

private fun isUpperLetter(letter: String): Boolean {
    val lower = letter.lowercase()
    return lower != letter && codePointCount(lower) == 1 && letter.uppercase() == letter
}

private fun isLowerLetter(letter: String): Boolean {
    val upper = letter.uppercase()
    return upper != letter && codePointCount(upper) == 1 && letter.lowercase() == letter
}

private fun hasUppercaseCandidate(text: String): Boolean {
    for (unit in text) {
        val code = unit.code
        if ((code in 0x41..0x5A) || code >= 0x80) return true
    }
    return false
}

private fun hasIrregularWhitespace(text: String): Boolean {
    for (i in text.indices) {
        val code = text[i].code
        if (code == 0x20) {
            if (i + 1 < text.length && text[i + 1].code == 0x20) return true
        } else if (code < 0x20 || code == 0xA0 || code == 0x1680 || code in 0x2000..0x200A ||
            code == 0x2028 || code == 0x2029 || code == 0x202F || code == 0x205F ||
            code == 0x3000 || code == 0xFEFF
        ) {
            return true
        }
    }
    return false
}
