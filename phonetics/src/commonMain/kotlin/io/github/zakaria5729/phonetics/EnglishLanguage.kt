package io.github.zakaria5729.phonetics

private val doubledLetterRules: List<PhoneticRule> = "bcdfglmnprstz".map { letter ->
    PhoneticRule(listOf("$letter$letter", "$letter"), 0.95, position = RulePosition.NotWordStart)
}

/** The default English sound-alike rules. */
public val englishRules: List<PhoneticRule> = doubledLetterRules + listOf(
    PhoneticRule(listOf("ck", "k"), 0.95, position = RulePosition.NotWordStart),
    PhoneticRule(listOf("c", "k"), 0.85),
    PhoneticRule(listOf("q", "k"), 0.85),
    PhoneticRule(listOf("qu", "kw"), 0.90),
    PhoneticRule(listOf("x", "ks"), 0.95),
    PhoneticRule(listOf("x", "cks"), 0.90, position = RulePosition.NotWordStart),
    PhoneticRule(listOf("ph", "f"), 0.95),
    PhoneticRule(listOf("v", "w"), 0.75),
    PhoneticRule(listOf("s", "z"), 0.85),
    PhoneticRule(listOf("s", "c"), 0.70),
    PhoneticRule(listOf("sh", "s"), 0.70),
    PhoneticRule(listOf("sh", "ch"), 0.65),
    PhoneticRule(listOf("sh", "sch"), 0.85),
    PhoneticRule(listOf("ch", "tch"), 0.95, position = RulePosition.NotWordStart),
    PhoneticRule(listOf("ch", "k"), 0.70),
    PhoneticRule(listOf("j", "g"), 0.75),
    PhoneticRule(listOf("j", "dj"), 0.90, position = RulePosition.NotWordStart),
    PhoneticRule(listOf("j", "dg"), 0.85, position = RulePosition.NotWordStart),
    PhoneticRule(listOf("j", "y"), 0.70, position = RulePosition.WordStart),
    PhoneticRule(listOf("ge", "je"), 0.80, position = RulePosition.WordStart),
    PhoneticRule(listOf("je", "geo"), 0.80, position = RulePosition.WordStart),
    PhoneticRule(listOf("th", "t"), 0.80),
    PhoneticRule(listOf("dh", "d"), 0.85),
    PhoneticRule(listOf("bh", "b"), 0.85),
    PhoneticRule(listOf("gh", "g"), 0.85),
    PhoneticRule(listOf("kh", "k"), 0.85),
    PhoneticRule(listOf("rh", "r"), 0.90, position = RulePosition.WordStart),
    PhoneticRule(listOf("wh", "w"), 0.90, position = RulePosition.WordStart),
    PhoneticRule(listOf("wr", "r"), 0.90, position = RulePosition.WordStart),
    PhoneticRule(listOf("kn", "n"), 0.90, position = RulePosition.WordStart),
    PhoneticRule(listOf("gn", "n"), 0.90, position = RulePosition.WordStart),
    PhoneticRule(listOf("mb", "m"), 0.85, position = RulePosition.WordEnd),
    PhoneticRule(listOf("ah", "a"), 0.90, position = RulePosition.WordEnd),
    PhoneticRule(listOf("eh", "e"), 0.90, position = RulePosition.WordEnd),
    PhoneticRule(listOf("ih", "i"), 0.90, position = RulePosition.WordEnd),
    PhoneticRule(listOf("oh", "o"), 0.90, position = RulePosition.WordEnd),
    PhoneticRule(listOf("uh", "u"), 0.90, position = RulePosition.WordEnd),
    PhoneticRule(listOf("a", "aa"), 0.90),
    PhoneticRule(listOf("o", "oo"), 0.80),
    PhoneticRule(listOf("a", "e"), 0.70),
    PhoneticRule(listOf("i", "e"), 0.70),
    PhoneticRule(listOf("u", "o"), 0.70),
    PhoneticRule(listOf("i", "ee"), 0.80),
    PhoneticRule(listOf("ee", "ea"), 0.90),
    PhoneticRule(listOf("ee", "ie"), 0.85),
    PhoneticRule(listOf("i", "y"), 0.85),
    PhoneticRule(listOf("i", "ie"), 0.85),
    PhoneticRule(listOf("y", "ie"), 0.90, position = RulePosition.WordEnd),
    PhoneticRule(listOf("ey", "y"), 0.90, position = RulePosition.WordEnd),
    PhoneticRule(listOf("ey", "ie"), 0.85, position = RulePosition.WordEnd),
    PhoneticRule(listOf("ai", "ay"), 0.90),
    PhoneticRule(listOf("ai", "ei"), 0.85),
    PhoneticRule(listOf("ay", "ey"), 0.85),
    PhoneticRule(listOf("ai", "ae"), 0.80),
    PhoneticRule(listOf("ai", "ey"), 0.80),
    PhoneticRule(listOf("ei", "ie"), 0.80),
    PhoneticRule(listOf("ay", "aye"), 0.90),
    PhoneticRule(listOf("ai", "aye"), 0.80),
    PhoneticRule(listOf("ai", "a"), 0.70),
    PhoneticRule(listOf("oo", "u"), 0.90),
    PhoneticRule(listOf("oo", "ou"), 0.80),
    PhoneticRule(listOf("ou", "ow"), 0.85),
    PhoneticRule(listOf("ow", "o"), 0.80, position = RulePosition.WordEnd),
    PhoneticRule(listOf("oe", "o"), 0.85, position = RulePosition.WordEnd),
    PhoneticRule(listOf("au", "aw"), 0.90),
    PhoneticRule(listOf("ew", "ue"), 0.90),
    PhoneticRule(listOf("ia", "iya"), 0.90),
    PhoneticRule(listOf("ya", "ia"), 0.80),
    PhoneticRule(listOf("er", "ir", "ur"), 0.70),
)

private val accentedLetters: Map<String, String> = buildMap {
    val accentsByLetter = mapOf(
        'a' to "àáâãäåāăą",
        'c' to "çćĉċč",
        'd' to "ď",
        'e' to "èéêëēĕėęě",
        'g' to "ĝğġģ",
        'i' to "ìíîïĩīĭįı",
        'n' to "ñńņňŉ",
        'o' to "òóôõöōŏő",
        's' to "śŝşš",
        't' to "ţťŧ",
        'u' to "ùúûüũūŭůűų",
        'y' to "ýÿŷ",
        'z' to "źżž",
    )
    for ((letter, accents) in accentsByLetter) {
        for (accented in accents) put(accented.toString(), letter.toString())
    }
}

private val englishCanonicalSequences: Map<String, String> = buildMap {
    for (unit in 0x0300..0x036F) put(unit.toChar().toString(), "")
    put("’", "'")
    put("‘", "'")
    put("ß", "ss")
    put("æ", "ae")
    put("œ", "oe")
    put("ø", "o")
    put("ł", "l")
    put("đ", "d")
    put("ð", "d")
    put("þ", "th")
    putAll(accentedLetters)
}

internal val englishLanguage: PhoneticLanguage = PhoneticLanguage(
    code = "en",
    name = "English",
    foldCase = true,
    rules = englishRules,
    canonicalSequences = englishCanonicalSequences,
    marks = setOf(0x27, 0x2D, 0x2E),
)
