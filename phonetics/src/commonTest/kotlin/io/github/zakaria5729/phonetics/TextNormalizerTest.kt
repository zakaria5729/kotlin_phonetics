package io.github.zakaria5729.phonetics

import kotlin.test.Test
import kotlin.test.assertEquals

class TextNormalizerTest {
    private val english = PhoneticLanguages.english
    private val bangla = PhoneticLanguages.bangla

    @Test
    fun trimsAndCollapsesWhitespace() {
        assertEquals("a b", english.normalize("  a \t\n  b  "))
        assertEquals("a b", english.normalize("a  b"))
        assertEquals("", english.normalize(" 　﻿ "))
    }

    @Test
    fun foldsCaseAndAccentsForEnglishOnly() {
        assertEquals("cafe", english.normalize("CAFÉ"))
        assertEquals("strasse", english.normalize("Straße"))
        assertEquals("o'brien", english.normalize("O’Brien"))
        assertEquals("ABC", bangla.normalize("ABC"))
    }

    @Test
    fun banglaCombinesNuktaSequences() {
        assertEquals("ড়", bangla.normalize("ড়"))
        assertEquals("ঢ়", bangla.normalize("ঢ়"))
        assertEquals("য়", bangla.normalize("য়"))
        assertEquals("ো", bangla.normalize("ো"))
    }

    @Test
    fun detectsTheCaseToRestore() {
        val normalizer = english.normalizer
        assertEquals(LetterCase.Upper, normalizer.detectCase("JOHN"))
        assertEquals(LetterCase.Title, normalizer.detectCase("John Smith"))
        assertEquals(LetterCase.Lower, normalizer.detectCase("john smith"))
        assertEquals(LetterCase.Title, normalizer.detectCase("McDonald"))
        assertEquals(LetterCase.Lower, normalizer.detectCase("mcDonald"))
        assertEquals(LetterCase.Unchanged, normalizer.detectCase("123"))
        assertEquals(LetterCase.Unchanged, bangla.normalizer.detectCase("ABC"))
        assertEquals("John Smith", normalizer.applyCase("john smith", LetterCase.Title))
        assertEquals("JOHN", normalizer.applyCase("john", LetterCase.Upper))
    }
}
