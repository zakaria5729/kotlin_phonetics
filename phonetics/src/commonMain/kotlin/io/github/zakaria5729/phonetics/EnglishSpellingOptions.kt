package io.github.zakaria5729.phonetics

internal class SpellingOptions private constructor(
    val reference: String,
    val alternatives: List<PhoneticAlternative>,
) {
    val hasAlternatives: Boolean
        get() = alternatives.isNotEmpty()

    companion object {
        fun of(reference: String, vararg alternatives: Pair<String, Double>): SpellingOptions =
            fromPairs(reference, alternatives.asList())

        fun fromPairs(reference: String, alternatives: List<Pair<String, Double>>): SpellingOptions {
            val bestSimilarityByText = LinkedHashMap<String, Double>()
            for ((text, similarity) in alternatives) {
                if (text == reference) continue
                val known = bestSimilarityByText[text]
                if (known == null || similarity > known) bestSimilarityByText[text] = similarity
            }
            val sorted = bestSimilarityByText
                .map { (text, similarity) -> PhoneticAlternative(text, similarity) }
                .sortedWith { a, b ->
                    val bySimilarity = b.similarity.compareTo(a.similarity)
                    if (bySimilarity != 0) bySimilarity else a.text.compareTo(b.text)
                }
            return SpellingOptions(reference, sorted)
        }
    }
}

internal enum class VowelSound { OPEN_O, A, I, U, RI, E, OI, O, OU }

private const val GEMINATE_REDUCED_SIMILARITY = 0.9

internal object EnglishSpellingOptions {
    private val consonantOptions: Map<Char, SpellingOptions> = mapOf(
        'ক' to SpellingOptions.of("k", "q" to 0.85),
        'খ' to SpellingOptions.of("kh", "k" to 0.88),
        'গ' to SpellingOptions.of("g"),
        'ঘ' to SpellingOptions.of("gh", "g" to 0.9),
        'ঙ' to SpellingOptions.of("ng", "n" to 0.85),
        'চ' to SpellingOptions.of("ch"),
        'ছ' to SpellingOptions.of("ch", "chh" to 0.95, "s" to 0.8, "sh" to 0.8),
        'জ' to SpellingOptions.of("j", "z" to 0.95),
        'ঝ' to SpellingOptions.of("jh", "j" to 0.9, "z" to 0.85),
        'ঞ' to SpellingOptions.of("n", "ny" to 0.85),
        'ট' to SpellingOptions.of("t"),
        'ঠ' to SpellingOptions.of("th", "t" to 0.9),
        'ড' to SpellingOptions.of("d"),
        'ঢ' to SpellingOptions.of("dh", "d" to 0.9),
        'ণ' to SpellingOptions.of("n"),
        'ত' to SpellingOptions.of("t", "th" to 0.8),
        'থ' to SpellingOptions.of("th", "t" to 0.9),
        'দ' to SpellingOptions.of("d"),
        'ধ' to SpellingOptions.of("dh", "d" to 0.9),
        'ন' to SpellingOptions.of("n"),
        'প' to SpellingOptions.of("p"),
        'ফ' to SpellingOptions.of("f", "ph" to 0.85),
        'ব' to SpellingOptions.of("b", "v" to 0.78),
        'ভ' to SpellingOptions.of("bh", "v" to 0.93, "b" to 0.9),
        'ম' to SpellingOptions.of("m"),
        'য' to SpellingOptions.of("j", "z" to 0.95, "y" to 0.8),
        BanglaCharacters.YA_WITH_DOT to SpellingOptions.of("y"),
        'র' to SpellingOptions.of("r"),
        'ল' to SpellingOptions.of("l"),
        'শ' to SpellingOptions.of("sh", "s" to 0.9),
        'ষ' to SpellingOptions.of("sh", "s" to 0.9),
        'স' to SpellingOptions.of("s", "sh" to 0.9),
        'হ' to SpellingOptions.of("h"),
        'ড়' to SpellingOptions.of("r", "rh" to 0.85),
        'ঢ়' to SpellingOptions.of("r", "rh" to 0.9),
        BanglaCharacters.KHANDA_TA to SpellingOptions.of("t", "th" to 0.85),
    )

    private val vowelOptions: Map<VowelSound, SpellingOptions> = mapOf(
        VowelSound.OPEN_O to SpellingOptions.of("o", "a" to 0.95),
        VowelSound.A to SpellingOptions.of("a", "aa" to 0.88),
        VowelSound.I to SpellingOptions.of("i", "ee" to 0.93, "e" to 0.85),
        VowelSound.U to SpellingOptions.of("u", "oo" to 0.95, "ou" to 0.9, "o" to 0.8),
        VowelSound.RI to SpellingOptions.of("ri", "ru" to 0.85, "rri" to 0.85),
        VowelSound.E to SpellingOptions.of("e", "ay" to 0.85, "a" to 0.8),
        VowelSound.OI to SpellingOptions.of("oi", "ai" to 0.95, "oy" to 0.92, "ay" to 0.88),
        VowelSound.O to SpellingOptions.of("o", "u" to 0.9, "oo" to 0.8),
        VowelSound.OU to SpellingOptions.of("ou", "ow" to 0.95, "au" to 0.92, "aw" to 0.88),
    )

    private val wordEndVowelOptions: Map<VowelSound, SpellingOptions> = mapOf(
        VowelSound.I to SpellingOptions.of("i", "ee" to 0.93, "e" to 0.85, "y" to 0.85),
    )

    private val clusterOptions: Map<String, SpellingOptions> = mapOf(
        "ক্ষ" to SpellingOptions.of("ksh", "kkh" to 0.95, "x" to 0.85, "kh" to 0.85),
        "জ্ঞ" to SpellingOptions.of("gg", "gy" to 0.95, "jn" to 0.88, "gn" to 0.88),
        "হ্ম" to SpellingOptions.of("hm", "mm" to 0.85),
    )

    private val wordStartClusterOptions: Map<String, SpellingOptions> = mapOf(
        "ক্ষ" to SpellingOptions.of("kh", "ksh" to 0.95, "kkh" to 0.9),
    )

    private val velarNasalOptions = SpellingOptions.of("n", "ng" to 0.88)
    private val rephaOptions = SpellingOptions.of("r")
    private val yaPhalaOptions = SpellingOptions.of("y", "" to 0.88)
    private val baPhalaOptions = SpellingOptions.of("w", "" to 0.88, "b" to 0.8)
    private val yaAfterVowelOptions = SpellingOptions.of("y", "i" to 0.93)
    private val wordStartWaOptions = SpellingOptions.of("w", "oy" to 0.75)
    private val yuVowelPairOptions = SpellingOptions.of("yu", "you" to 0.9, "iu" to 0.9, "yoo" to 0.85)

    private val inherentVowelOptions: Map<InherentVowelFate, SpellingOptions> = mapOf(
        InherentVowelFate.REALIZED_AT_WORD_START to SpellingOptions.of("o", "a" to 0.95),
        InherentVowelFate.REALIZED_INSIDE_WORD to SpellingOptions.of("o", "a" to 0.95, "e" to 0.85),
        InherentVowelFate.REALIZED_AT_WORD_END to SpellingOptions.of("o", "a" to 0.9, "" to 0.85),
        InherentVowelFate.DELETED_INSIDE_WORD to SpellingOptions.of("", "a" to 0.8),
    )

    private val nasalMarkOptions: Map<Char, SpellingOptions> = mapOf(
        'ং' to SpellingOptions.of("ng", "n" to 0.85),
        'ঃ' to SpellingOptions.of("h", "" to 0.9),
        'ঁ' to SpellingOptions.of("n", "" to 0.85),
    )

    private val velarConsonants = setOf('ক', 'খ', 'গ', 'ঘ')

    fun forConsonant(consonant: Char): SpellingOptions =
        consonantOptions[consonant] ?: SpellingOptions.of("")

    fun forVowel(sound: VowelSound, atWordEnd: Boolean): SpellingOptions =
        (if (atWordEnd) wordEndVowelOptions[sound] else null) ?: vowelOptions.getValue(sound)

    fun forCluster(first: Char, second: Char, atWordStart: Boolean): SpellingOptions? {
        val key = "$first${BanglaCharacters.HASANTA}$second"
        return (if (atWordStart) wordStartClusterOptions[key] else null) ?: clusterOptions[key]
    }

    fun forNasalConsonantBeforeVelar(): SpellingOptions = velarNasalOptions

    fun forPhala(consonant: Char): SpellingOptions? = when (consonant) {
        'র' -> rephaOptions
        'য' -> yaPhalaOptions
        'ব' -> baPhalaOptions
        else -> null
    }

    fun forYaAfterVowel(): SpellingOptions = yaAfterVowelOptions

    fun forWordStartWa(): SpellingOptions = wordStartWaOptions

    fun forYuVowelPair(): SpellingOptions = yuVowelPairOptions

    fun forInherentVowel(fate: InherentVowelFate): SpellingOptions? = inherentVowelOptions[fate]

    fun forNasalMark(mark: Char): SpellingOptions = nasalMarkOptions.getValue(mark)

    fun isVelar(consonant: Char): Boolean = consonant in velarConsonants

    fun geminated(single: SpellingOptions): SpellingOptions {
        val doubledReference = doubleLetters(single.reference)
        val alternatives = buildList {
            add(single.reference to GEMINATE_REDUCED_SIMILARITY)
            for (alternative in single.alternatives) {
                add(doubleLetters(alternative.text) to alternative.similarity)
            }
        }
        return SpellingOptions.fromPairs(doubledReference, alternatives)
    }

    private fun doubleLetters(text: String): String =
        if (text.length == 2 && text[1] == 'h') "${text[0]}$text" else text + text
}
