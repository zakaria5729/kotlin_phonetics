package io.github.zakaria5729.phonetics

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class BanglaSoundAnalysisTest {
    private fun tokensOf(word: String): List<WordToken> =
        (parseBanglaText(banglaLanguage.normalize(word)) as BanglaParseOutcome.Parsed).tokens

    private fun fatesOf(word: String): Map<Int, InherentVowelFate> = resolveInherentVowelFates(tokensOf(word))

    @Test
    fun consonantsVowelSignsAndHasantaAreGroupedIntoSyllables() {
        val tokens = tokensOf("সাদ্দাম")
        assertEquals(3, tokens.size)
        val middle = tokens[1] as WordToken.ConsonantGroup
        assertEquals(listOf('দ', 'দ'), middle.consonants)
        assertEquals('া', middle.vowelSign)
        assertTrue(middle.isConjunct)
        assertTrue((tokens[2] as WordToken.ConsonantGroup).hasInherentVowel)
    }

    @Test
    fun aTrailingHasantaMarksAConsonantWithoutAVowel() {
        val group = tokensOf("ক্").single() as WordToken.ConsonantGroup
        assertTrue(group.endsWithHasanta)
        assertEquals(false, group.hasInherentVowel)
    }

    @Test
    fun khandaTaNeverTakesAVowel() {
        val group = tokensOf("জগৎ")[2] as WordToken.ConsonantGroup
        assertTrue(group.endsWithHasanta)
    }

    @Test
    fun theFirstInherentVowelIsAlwaysSpoken() {
        assertEquals(InherentVowelFate.REALIZED_AT_WORD_START, fatesOf("রহিম").getValue(0))
        assertEquals(InherentVowelFate.REALIZED_AT_WORD_START, fatesOf("করিম").getValue(0))
    }

    @Test
    fun theFinalInherentVowelIsDroppedAfterASingleConsonant() {
        assertEquals(InherentVowelFate.DELETED_AT_WORD_END, fatesOf("রহিম").getValue(2))
        assertEquals(InherentVowelFate.DELETED_AT_WORD_END, fatesOf("মাসুদ").getValue(2))
    }

    @Test
    fun theFinalInherentVowelIsSpokenAfterAConjunct() {
        assertEquals(InherentVowelFate.REALIZED_AT_WORD_END, fatesOf("শান্ত").getValue(1))
    }

    @Test
    fun aMiddleInherentVowelBeforeASpokenSyllableIsDropped() {
        assertEquals(InherentVowelFate.DELETED_INSIDE_WORD, fatesOf("ফারজানা").getValue(1))
        assertEquals(InherentVowelFate.DELETED_INSIDE_WORD, fatesOf("সালমা").getValue(1))
    }

    @Test
    fun aMiddleInherentVowelBeforeADroppedFinalVowelIsSpoken() {
        assertEquals(InherentVowelFate.REALIZED_INSIDE_WORD, fatesOf("কমল").getValue(1))
        assertEquals(InherentVowelFate.REALIZED_INSIDE_WORD, fatesOf("বিজয়").getValue(1))
    }

    @Test
    fun aMiddleInherentVowelOfAConjunctIsKept() {
        assertEquals(InherentVowelFate.REALIZED_INSIDE_WORD, fatesOf("মোস্তফা").getValue(1))
    }

    @Test
    fun anInherentVowelBeforeANasalMarkIsKept() {
        assertEquals(InherentVowelFate.REALIZED_INSIDE_WORD, fatesOf("আরং").getValue(1))
    }

    @Test
    fun fatesAreDecidedForEachWordSeparately() {
        val plan = planEnglishSpellings(banglaLanguage.normalize("রহিম করিম"))
        assertIs<SpellingPlan.Ready>(plan)
        assertEquals("Rohim Korim", capitalizeEachWord(plan.referenceSpelling))
    }

    @Test
    fun unsupportedCharactersAreRejectedWithTheirPosition() {
        val outcome = parseBanglaText("মাসুদ😀")
        assertIs<BanglaParseOutcome.Rejected>(outcome)
        assertTrue(outcome.reason.contains("position"))
    }

    @Test
    fun textWithoutBanglaLettersIsRejected() {
        assertIs<BanglaParseOutcome.Rejected>(parseBanglaText("hello"))
        assertIs<BanglaParseOutcome.Rejected>(parseBanglaText("123"))
        assertIs<BanglaParseOutcome.Parsed>(parseBanglaText("   "))
    }

    @Test
    fun everySpellingOptionHasAValidSimilarity() {
        val consonants = "কখগঘঙচছজঝঞটঠডঢণতথদধনপফবভমযরলশষসহড়ঢ়য়ৎ"
        val allOptions = buildList {
            consonants.forEach { add(EnglishSpellingOptions.forConsonant(it)) }
            VowelSound.entries.forEach {
                add(EnglishSpellingOptions.forVowel(it, atWordEnd = false))
                add(EnglishSpellingOptions.forVowel(it, atWordEnd = true))
            }
            InherentVowelFate.entries.forEach { EnglishSpellingOptions.forInherentVowel(it)?.let(::add) }
            listOf('ং', 'ঃ', 'ঁ').forEach { add(EnglishSpellingOptions.forNasalMark(it)) }
            consonants.forEach { add(EnglishSpellingOptions.geminated(EnglishSpellingOptions.forConsonant(it))) }
        }
        for (options in allOptions) {
            assertTrue(options.alternatives.all { it.similarity > 0.0 && it.similarity < 1.0 })
            assertTrue(options.alternatives.none { it.text == options.reference })
            assertEquals(
                options.alternatives.sortedByDescending { it.similarity }.map { it.similarity },
                options.alternatives.map { it.similarity },
            )
        }
    }
}
