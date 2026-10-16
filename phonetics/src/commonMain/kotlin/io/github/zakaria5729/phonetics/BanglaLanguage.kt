package io.github.zakaria5729.phonetics

private const val O_KAR = "\u09CB"

private val consonantsThatTakeFinalO: List<String> =
    "কখগঘঙচছজঝঞটঠডঢণতথদধনপফবভমযরলশষসহ".map { it.toString() } +
        listOf("\u09DC", "\u09DD", "\u09DF")

private fun finalOEndings(): List<PhoneticRule> = consonantsThatTakeFinalO.map { consonant ->
    PhoneticRule(listOf(consonant, "$consonant$O_KAR"), 0.85, position = RulePosition.WordEnd)
}

/** The default Bangla sound-alike rules. */
public val banglaRules: List<PhoneticRule> = baseRules() + finalOEndings() + scoreOnlyRules()

private fun baseRules(): List<PhoneticRule> = listOf(
    PhoneticRule(listOf("স", "শ", "ষ"), 0.95),
    PhoneticRule(listOf("ছ", "স", "শ"), 0.85),
    PhoneticRule(listOf("ছ", "ষ"), 0.80),
    PhoneticRule(listOf("চ", "ছ"), 0.85),
    PhoneticRule(listOf("চ", "স", "শ", "ষ"), 0.75),
    PhoneticRule(listOf("ন", "ণ"), 0.95),
    PhoneticRule(listOf("ঞ", "ন"), 0.80),
    PhoneticRule(listOf("ং", "ঙ"), 0.95),
    PhoneticRule(listOf("জ", "য"), 0.95),
    PhoneticRule(listOf("জ", "ঝ"), 0.80),
    PhoneticRule(listOf("য", "ঝ"), 0.75),
    PhoneticRule(listOf("\u09DC", "\u09DD"), 0.90),
    PhoneticRule(listOf("র", "\u09DC"), 0.85),
    PhoneticRule(listOf("র", "\u09DD"), 0.80),
    PhoneticRule(listOf("ৎ", "ত"), 0.95),
    PhoneticRule(listOf("ত", "ট"), 0.75),
    PhoneticRule(listOf("থ", "ঠ"), 0.75),
    PhoneticRule(listOf("দ", "ড"), 0.75),
    PhoneticRule(listOf("ধ", "ঢ"), 0.75),
    PhoneticRule(listOf("ক", "খ"), 0.75),
    PhoneticRule(listOf("গ", "ঘ"), 0.75),
    PhoneticRule(listOf("ট", "ঠ"), 0.75),
    PhoneticRule(listOf("ড", "ঢ"), 0.75),
    PhoneticRule(listOf("ত", "থ"), 0.75),
    PhoneticRule(listOf("দ", "ধ"), 0.75),
    PhoneticRule(listOf("প", "ফ"), 0.75),
    PhoneticRule(listOf("ব", "ভ"), 0.75),
    PhoneticRule(listOf("ক্ষ", "খ"), 0.90),
    PhoneticRule(listOf("জ্ঞ", "গ্গ"), 0.85),
    PhoneticRule(listOf("ই", "ঈ"), 0.97),
    PhoneticRule(listOf("উ", "ঊ"), 0.97),
    PhoneticRule(listOf("ঋ", "রি"), 0.95),
    PhoneticRule(listOf("ঐ", "অই", "ওই"), 0.90),
    PhoneticRule(listOf("ঔ", "অউ", "ওউ"), 0.90),
    PhoneticRule(listOf("অ", "ও"), 0.80),
    PhoneticRule(listOf("ি", "ী"), 0.97),
    PhoneticRule(listOf("ু", "ূ"), 0.97),
    PhoneticRule(listOf("ৃ", "্রি"), 0.95),
    PhoneticRule(listOf("ৈ", "োই"), 0.90),
    PhoneticRule(listOf("ৌ", "োউ"), 0.90),
    PhoneticRule(listOf("ো", "ু"), 0.80),
    PhoneticRule(listOf("ঞ্জ", "নজ"), 0.90),
    PhoneticRule(listOf("ঞ্চ", "নচ"), 0.90),
)

private fun scoreOnlyRules(): List<PhoneticRule> = listOf(
    PhoneticRule(listOf("ই", "ি"), 0.90, scoreOnly = true),
    PhoneticRule(listOf("ই", "ী"), 0.90, scoreOnly = true),
    PhoneticRule(listOf("উ", "ু"), 0.90, scoreOnly = true),
    PhoneticRule(listOf("উ", "ূ"), 0.90, scoreOnly = true),
    PhoneticRule(listOf("ও", "ো"), 0.90, scoreOnly = true),
    PhoneticRule(listOf("এ", "ে"), 0.90, scoreOnly = true),
    PhoneticRule(listOf("আ", "া"), 0.90, scoreOnly = true),
    PhoneticRule(listOf("ঐ", "ৈ"), 0.90, scoreOnly = true),
    PhoneticRule(listOf("ঔ", "ৌ"), 0.90, scoreOnly = true),
    PhoneticRule(listOf("ঋ", "ৃ"), 0.90, scoreOnly = true),
)

private val banglaCanonicalSequences: Map<String, String> = mapOf(
    "\u09A1\u09BC" to "\u09DC",
    "\u09A2\u09BC" to "\u09DD",
    "\u09AF\u09BC" to "\u09DF",
    "\u09A4\u09CD\u200D" to "\u09CE",
    "\u09C7\u09BE" to "\u09CB",
    "\u09C7\u09D7" to "\u09CC",
)

private val banglaMarks: Set<Int> = setOf(0x09CD, 0x0981, 0x09BC, 0x0983, 0x200C, 0x200D)

internal val banglaLanguage: PhoneticLanguage = PhoneticLanguage(
    code = "bn",
    name = "Bangla",
    rules = banglaRules,
    canonicalSequences = banglaCanonicalSequences,
    marks = banglaMarks,
)
