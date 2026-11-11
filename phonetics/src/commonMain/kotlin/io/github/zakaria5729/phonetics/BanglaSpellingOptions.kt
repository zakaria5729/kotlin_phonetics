package io.github.zakaria5729.phonetics

internal class BanglaVowelForm(val signAfterConsonant: String, val standaloneLetters: String)

private const val HASANTA_TEXT = "্"
private const val CONJUNCT_ALTERNATIVE_SIMILARITY = 0.88
private const val DOUBLED_REDUCED_SIMILARITY = 0.9
private const val YA_WITH_NUKTA_DECOMPOSED = "য়"
private const val YA_WITH_DOT_COMPOSED = "য়"

private fun canonicalBangla(text: String): String = text.replace(YA_WITH_NUKTA_DECOMPOSED, YA_WITH_DOT_COMPOSED)

private fun bn(reference: String, vararg alternatives: Pair<String, Double>): SpellingOptions =
    SpellingOptions.fromPairs(
        canonicalBangla(reference),
        alternatives.map { (text, similarity) -> canonicalBangla(text) to similarity },
    )

internal object BanglaSpellingOptions {
    private val formAa = BanglaVowelForm("া", "আ")
    private val formInherent = BanglaVowelForm("", "অ")
    private val formShortI = BanglaVowelForm("ি", "ই")
    private val formLongI = BanglaVowelForm("ী", "ঈ")
    private val formShortU = BanglaVowelForm("ু", "উ")
    private val formLongU = BanglaVowelForm("ূ", "ঊ")
    private val formE = BanglaVowelForm("ে", "এ")
    private val formOi = BanglaVowelForm("ৈ", "ঐ")
    private val formO = BanglaVowelForm("ো", "ও")
    private val formOu = BanglaVowelForm("ৌ", "ঔ")
    private val formAaThenI = BanglaVowelForm("াই", "আই")
    private val formAaThenYa = BanglaVowelForm("ায়", "আয়")
    private val formWordStartYu = BanglaVowelForm("ু", "ইউ")
    private val formWordStartYuWithYa = BanglaVowelForm("ু", "ইয়ু")

    private val vowelChoices: Map<String, List<Pair<BanglaVowelForm, Double>>> = mapOf(
        "a" to listOf(formAa to 1.0, formInherent to 0.93),
        "aa" to listOf(formAa to 1.0, formInherent to 0.85),
        "e" to listOf(formE to 1.0, formInherent to 0.88),
        "i" to listOf(formShortI to 1.0, formLongI to 0.95),
        "y" to listOf(formShortI to 1.0, formLongI to 0.95),
        "ee" to listOf(formLongI to 1.0, formShortI to 0.95),
        "ea" to listOf(formLongI to 1.0, formShortI to 0.95),
        "ie" to listOf(formLongI to 1.0, formShortI to 0.95),
        "o" to listOf(formO to 1.0, formInherent to 0.93),
        "oo" to listOf(formLongU to 1.0, formShortU to 0.95),
        "u" to listOf(formShortU to 1.0, formLongU to 0.95, formO to 0.85),
        "ou" to listOf(formOu to 1.0, formShortU to 0.92, formO to 0.85),
        "ow" to listOf(formOu to 1.0, formShortU to 0.92, formO to 0.85),
        "au" to listOf(formOu to 1.0, formO to 0.85),
        "aw" to listOf(formOu to 1.0, formO to 0.85),
        "ai" to listOf(formAaThenI to 1.0, formOi to 0.93),
        "ay" to listOf(formAaThenYa to 1.0, formAaThenI to 0.9),
        "oi" to listOf(formOi to 1.0),
        "ei" to listOf(formOi to 1.0, formE to 0.85),
        "ey" to listOf(formE to 1.0, formAaThenYa to 0.85),
        "yu" to listOf(formWordStartYu to 1.0, formWordStartYuWithYa to 0.92),
    )

    private val wordEndAChoices = listOf(formAa to 1.0, formInherent to 0.7)
    private val beforeNgOChoices = listOf(formInherent to 1.0, formO to 0.9)

    private val consonantOptions: Map<String, SpellingOptions> = mapOf(
        "b" to bn("ব"),
        "bh" to bn("ভ", "ব" to 0.9),
        "ch" to bn("চ", "ছ" to 0.93),
        "tch" to bn("চ", "ছ" to 0.9),
        "d" to bn("দ", "ড" to 0.95),
        "dh" to bn("ধ", "ঢ" to 0.93, "দ" to 0.88),
        "f" to bn("ফ"),
        "gh" to bn("ঘ", "গ" to 0.9),
        "h" to bn("হ"),
        "j" to bn("জ", "য" to 0.95),
        "jh" to bn("ঝ", "জ" to 0.9),
        "k" to bn("ক", "খ" to 0.85),
        "ck" to bn("ক"),
        "kh" to bn("খ", "ক" to 0.88),
        "l" to bn("ল"),
        "m" to bn("ম"),
        "n" to bn("ন", "ণ" to 0.93),
        "p" to bn("প"),
        "ph" to bn("ফ", "প" to 0.88),
        "q" to bn("ক", "খ" to 0.8),
        "r" to bn("র"),
        "s" to bn("স", "শ" to 0.93, "ষ" to 0.9),
        "sh" to bn("শ", "ষ" to 0.93, "স" to 0.9),
        "t" to bn("ত", "ট" to 0.95),
        "th" to bn("থ", "ঠ" to 0.93, "ত" to 0.9),
        "v" to bn("ভ", "ব" to 0.9),
        "w" to bn("ওয়", "ব" to 0.8),
        "x" to bn("ক্স", "ক্ষ" to 0.88),
        "y" to bn("য়"),
        "z" to bn("জ", "য" to 0.93),
    )

    private val softCOptions = bn("স", "ক" to 0.85)
    private val hardCOptions = bn("ক", "স" to 0.8)
    private val softGOptions = bn("জ", "গ" to 0.85)
    private val hardGOptions = bn("গ", "জ" to 0.78)
    private val wordInitialNOptions = bn("ন")
    private val tAfterWordInitialSOptions = bn("ট", "ত" to 0.95)
    private val wordStartYOptions = bn("ইয়", "য়" to 0.8)
    private val ngBeforeVowelOptions = bn("ঙ্গ", "ঙ" to 0.88)
    private val ngElsewhereOptions = bn("ং", "ঙ্গ" to 0.9, "ঙ" to 0.88)

    private val conjunctPairs = setOf(
        "st", "sk", "sp", "sht", "nd", "nt", "nj", "mp", "mb", "bd", "kt", "ft", "pt", "ct", "gn",
    )

    fun forVowel(
        letters: String,
        afterConsonant: Boolean,
        atWordEnd: Boolean = false,
        beforeNasalNg: Boolean = false,
    ): SpellingOptions {
        val choices = when {
            atWordEnd && letters == "a" -> wordEndAChoices
            beforeNasalNg && letters == "o" -> beforeNgOChoices
            else -> vowelChoices.getValue(letters)
        }
        val textOf = { form: BanglaVowelForm ->
            canonicalBangla(if (afterConsonant) form.signAfterConsonant else form.standaloneLetters)
        }
        return SpellingOptions.fromPairs(
            textOf(choices.first().first),
            choices.drop(1).map { (form, similarity) -> textOf(form) to similarity },
        )
    }

    fun forConsonant(
        letters: String,
        isDoubled: Boolean,
        atWordStart: Boolean,
        nextIsVowel: Boolean,
        nextVowelLetters: String?,
        followsWordInitialS: Boolean = false,
    ): SpellingOptions {
        val single = when {
            letters == "t" && followsWordInitialS -> tAfterWordInitialSOptions
            letters == "n" && atWordStart -> wordInitialNOptions
            letters == "ng" -> if (nextIsVowel) ngBeforeVowelOptions else ngElsewhereOptions
            letters == "c" -> if (isSoftVowel(nextVowelLetters)) softCOptions else hardCOptions
            letters == "g" -> if (isSoftVowel(nextVowelLetters)) softGOptions else hardGOptions
            letters == "y" && atWordStart -> wordStartYOptions
            else -> consonantOptions[letters] ?: bn("")
        }
        return if (isDoubled) doubled(single) else single
    }

    fun forConjunctJoin(
        firstLetters: String,
        secondLetters: String,
        firstIsAtWordStart: Boolean,
    ): SpellingOptions {
        val joinsAsConjunct = firstIsAtWordStart ||
            secondLetters == "r" ||
            "$firstLetters$secondLetters" in conjunctPairs
        return if (joinsAsConjunct) {
            bn(HASANTA_TEXT, "" to CONJUNCT_ALTERNATIVE_SIMILARITY)
        } else {
            bn("", HASANTA_TEXT to CONJUNCT_ALTERNATIVE_SIMILARITY)
        }
    }

    fun isNasalGroup(letters: String): Boolean = letters == "ng"

    private fun isSoftVowel(vowelLetters: String?): Boolean =
        vowelLetters != null && (vowelLetters.startsWith("e") || vowelLetters.startsWith("i") || vowelLetters == "y")

    private fun doubled(single: SpellingOptions): SpellingOptions {
        val canDouble = single.reference.length == 1
        if (!canDouble) return SpellingOptions.fromPairs(single.reference, listOf())
        val alternatives = buildList {
            add(single.reference to DOUBLED_REDUCED_SIMILARITY)
            for (alternative in single.alternatives) {
                add(
                    (if (alternative.text.length == 1) conjoin(alternative.text) else alternative.text) to
                        alternative.similarity,
                )
            }
        }
        return SpellingOptions.fromPairs(conjoin(single.reference), alternatives)
    }

    private fun conjoin(letter: String): String = "$letter$HASANTA_TEXT$letter"
}
