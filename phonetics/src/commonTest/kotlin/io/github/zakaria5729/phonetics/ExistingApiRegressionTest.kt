package io.github.zakaria5729.phonetics

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest

class ExistingApiRegressionTest {
    @Test
    fun banglaInstanceStillProducesTheDocumentedMasudVariants() = runTest {
        val result = BanglaPhonetics.instance.generate("মাসুদ", minScore = 90.0)
        assertNull(result.error)
        assertEquals(
            listOf(
                "মাসুদ" to 100.0,
                "মাসূদ" to 97.0,
                "মাশুদ" to 95.0,
                "মাষুদ" to 95.0,
                "মাশূদ" to 92.15,
                "মাষূদ" to 92.15,
            ),
            result.matches.map { it.word to it.score },
        )
    }

    @Test
    fun englishInstanceStillProducesTheDocumentedPhilipVariants() = runTest {
        val result = EnglishPhonetics.instance.generate("Philip", minScore = 90.0)
        assertNull(result.error)
        assertEquals(
            listOf(
                "Philip" to 100.0,
                "Filip" to 95.0,
                "Phillip" to 95.0,
                "Philipp" to 95.0,
                "Fillip" to 90.25,
            ),
            result.matches.map { it.word to it.score }.take(5),
        )
    }

    @Test
    fun banglaAndEnglishEnginesGiveTheSameResultsThroughEveryEntryPoint() = runTest {
        val banglaEngine = PhoneticEngine(PhoneticLanguages.bangla)
        val englishEngine = PhoneticEngine(PhoneticLanguages.english)
        val bangla = BanglaPhonetics()
        val english = EnglishPhonetics()
        try {
            for (name in listOf("মাসুদ", "সাদ্দাম", "মোজাফফর", "মোহাম্মদ", "আব্দুল", "রহিম")) {
                assertEquals(banglaEngine.generate(name, minScore = 80.0), bangla.generate(name, minScore = 80.0).let {
                    PhoneticMatchesResult(it.matches, it.error)
                })
            }
            for (name in listOf("Philip", "Stephen", "Catherine", "Jackson")) {
                assertEquals(englishEngine.generate(name, minScore = 80.0), english.generate(name, minScore = 80.0).let {
                    PhoneticMatchesResult(it.matches, it.error)
                })
            }
        } finally {
            bangla.dispose()
            english.dispose()
        }
    }

    @Test
    fun similarityAndRankingKeepTheirScores() = runTest {
        val banglaEngine = PhoneticEngine(PhoneticLanguages.bangla)
        val bangla = BanglaPhonetics()
        try {
            assertEquals(85.0, bangla.similarity("আসাদ", "আছাদ").score)
            val candidates = listOf("আছাদ", "রহিম", "আশাদ")
            assertEquals(
                banglaEngine.rank("আসাদ", candidates, minScore = 80.0).matches,
                bangla.rank("আসাদ", candidates, minScore = 80.0).matches,
            )
            assertEquals(listOf("আশাদ", "আছাদ"), bangla.rank("আসাদ", candidates, minScore = 80.0).matches.map { it.word })
        } finally {
            bangla.dispose()
        }
    }

    @Test
    fun customLanguagesAndRuleExtensionsStillWork() = runTest {
        val language = PhoneticLanguage(
            code = "xx",
            name = "Test",
            foldCase = true,
            rules = listOf(PhoneticRule(listOf("q", "x"), 0.9)),
        )
        val phonetics = Phonetics(language)
        try {
            assertEquals(listOf("aqa", "axa"), phonetics.generateWords("aqa").words)
            val extended = PhoneticLanguages.bangla.defaultRuleSet.extend(listOf(PhoneticRule(listOf("ম", "ন"), 0.5)))
            assertEquals(null, PhoneticEngine(PhoneticLanguages.bangla, extended).error)
        } finally {
            phonetics.dispose()
        }
    }
}
