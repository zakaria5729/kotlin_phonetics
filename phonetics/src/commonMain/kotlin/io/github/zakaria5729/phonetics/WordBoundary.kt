package io.github.zakaria5729.phonetics

internal fun startsWord(text: String, index: Int): Boolean =
    index <= 0 || isSeparator(text[index - 1])

internal fun endsWord(text: String, end: Int): Boolean =
    end >= text.length || isSeparator(text[end])

private fun isSeparator(unit: Char): Boolean = unit == ' ' || unit == '-'
