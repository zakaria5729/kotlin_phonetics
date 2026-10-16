package io.github.zakaria5729.phonetics

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GoldenParityTest {
    private val lines = goldenData.split('\n').filter { it.isNotEmpty() }.map { it.split('\t') }

    private val engines = mapOf(
        "bn" to PhoneticEngine(banglaLanguage),
        "en" to PhoneticEngine(englishLanguage),
    )

    private val languages = mapOf("bn" to banglaLanguage, "en" to englishLanguage)

    private fun describe(matches: List<PhoneticMatch>): String =
        matches.joinToString("|") { "${it.word}:${it.score}" }

    private fun closeEnough(expected: Double, actual: Double): Boolean =
        abs(expected - actual) <= 0.0100001

    private fun parse(text: String): List<Pair<String, Double>> =
        if (text.isEmpty()) {
            emptyList()
        } else {
            text.split('|').map { entry ->
                val cut = entry.lastIndexOf(':')
                entry.substring(0, cut) to entry.substring(cut + 1).toDouble()
            }
        }

    private fun assertSameMatches(expected: String, actual: List<PhoneticMatch>, label: String): String? {
        val wanted = parse(expected)
        val same = wanted.size == actual.size &&
            wanted.indices.all { i ->
                wanted[i].first == actual[i].word && closeEnough(wanted[i].second, actual[i].score)
            }
        return if (same) null else "$label\n  expected: $expected\n  actual:   ${describe(actual)}"
    }

    private fun report(failures: List<String>, total: Int, kind: String) {
        assertTrue(
            failures.isEmpty(),
            "${failures.size} of $total $kind results differ from the Dart package:\n" +
                failures.take(5).joinToString("\n"),
        )
    }

    private fun unhex(text: String): String =
        if (text.isEmpty()) "" else text.split(' ').map { it.toInt(16).toChar() }.joinToString("")

    @Test
    fun generateMatchesTheDartPackage() {
        val rows = lines.filter { it[0] == "G" }
        val failures = rows.mapNotNull { row ->
            val (_, code, setting, input, expected) = row
            val result = when (setting) {
                "a" -> engines.getValue(code).generate(input, 70.0, 50, true, null)
                "b" -> engines.getValue(code).generate(input, 50.0, 20, false, null)
                "c" -> engines.getValue(code).generate(input, 60.0, 30, true, 1)
                else -> engines.getValue(code).generate(input, 0.0, 15, true, 2)
            }
            assertNull(result.error)
            assertSameMatches(expected, result.matches, "generate $code/$setting \"$input\"")
        }
        assertTrue(rows.size > 1000)
        report(failures, rows.size, "generate")
    }

    @Test
    fun similarityMatchesTheDartPackage() {
        val rows = lines.filter { it[0] == "S" }
        val failures = rows.mapNotNull { row ->
            val (_, code, a, b, expected) = row
            val actual = engines.getValue(code).similarity(a, b)
            if (closeEnough(expected.toDouble(), actual.score)) null else "similarity $code \"$a\" \"$b\": expected $expected, got ${actual.score}"
        }
        assertTrue(rows.size > 1000)
        report(failures, rows.size, "similarity")
    }

    @Test
    fun rankMatchesTheDartPackage() {
        val rows = lines.filter { it[0] == "R" }
        val failures = rows.mapNotNull { row ->
            val (_, code, setting, query, expected) = row
            val candidates = lines
                .filter { it[0] == "G" && it[1] == code && it[2] == "a" && it[3].isNotEmpty() }
                .map { it[3] }
            val engine = engines.getValue(code)
            val result = when (setting) {
                "a" -> engine.rank(query, candidates, 0.0, null)
                "b" -> engine.rank(query, candidates, 60.0, 10)
                "c" -> engine.rank(query, candidates, 85.0, null)
                else -> engine.rank(query, candidates, 0.0, 5)
            }
            assertNull(result.error)
            assertSameMatches(expected, result.matches, "rank $code/$setting \"$query\"")
        }
        assertTrue(rows.size > 100)
        report(failures, rows.size, "rank")
    }

    @Test
    fun normalizeMatchesTheDartPackage() {
        val rows = lines.filter { it[0] == "N" }
        val failures = rows.mapNotNull { row ->
            val (_, code, input, expected) = row
            val actual = languages.getValue(code).normalize(unhex(input))
            val actualHex = actual.map { it.code.toString(16).padStart(4, '0') }.joinToString(" ")
            if (actualHex == expected) null else "normalize $code [$input]: expected [$expected], got [$actualHex]"
        }
        assertEquals(58, rows.size)
        report(failures, rows.size, "normalize")
    }
}
