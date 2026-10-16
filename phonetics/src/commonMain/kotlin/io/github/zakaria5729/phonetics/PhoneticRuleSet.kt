package io.github.zakaria5729.phonetics

import kotlin.math.ln
import kotlin.math.max

/** A spelling that can replace another, with how alike they sound. */
public class PhoneticAlternative(
    public val text: String,
    public val similarity: Double,
) {
    internal val logSimilarity: Double = ln(similarity)

    override fun toString(): String = "$text($similarity)"
}

/**
 * The rules of one language, compiled for fast lookup.
 *
 * Problems are reported through [error].
 */
public class PhoneticRuleSet private constructor(
    rules: Iterable<PhoneticRule>,
    public val language: PhoneticLanguage,
    earlierProblems: List<String>,
) {
    /** Builds a rule set from [rules] for [language]. */
    public constructor(rules: Iterable<PhoneticRule>, language: PhoneticLanguage) :
        this(rules, language, emptyList())

    private val compiled = compile(rules, language, earlierProblems)

    /** The rules this set was built from. */
    public val rules: List<PhoneticRule> = compiled.validRules

    internal val longestSpellingLength: Int = compiled.longestSpelling

    /** The problem with the rules, or `null` when they are valid. */
    public val error: PhoneticArgumentException? =
        if (compiled.problems.isEmpty()) {
            null
        } else {
            PhoneticArgumentException("invalid phonetic rules: ${compiled.problems.joinToString("; ")}")
        }

    /** The code of the language these rules are for. */
    public val languageCode: String
        get() = language.code

    /** Returns a new set with [extraRules] added. */
    public fun extend(extraRules: Iterable<PhoneticRule>): PhoneticRuleSet =
        PhoneticRuleSet(rules + extraRules, language, compiled.problems)

    /** Whether any rule covers [spelling]. */
    public fun hasRuleFor(spelling: String): Boolean = compiled.scoring.containsKey(spelling)

    /**
     * The spellings that can replace [spelling] at the given place in a word.
     *
     * With [generating] set, rules marked score-only are left out.
     */
    public fun alternativesFor(
        spelling: String,
        atWordStart: Boolean = false,
        atWordEnd: Boolean = false,
        generating: Boolean = false,
    ): List<PhoneticAlternative> {
        val byContext = (if (generating) compiled.generating else compiled.scoring)[spelling]
            ?: return emptyList()
        return byContext[(if (atWordStart) 1 else 0) or (if (atWordEnd) 2 else 0)]
    }

    private data class Target(val text: String, val position: RulePosition)

    private class Compiled(
        val scoring: Map<String, List<List<PhoneticAlternative>>>,
        val generating: Map<String, List<List<PhoneticAlternative>>>,
        val validRules: List<PhoneticRule>,
        val longestSpelling: Int,
        val problems: List<String>,
    )

    private companion object {
        fun compile(
            rules: Iterable<PhoneticRule>,
            language: PhoneticLanguage,
            earlierProblems: List<String>,
        ): Compiled {
            val problems = earlierProblems.toMutableList()
            val scoringTargets = LinkedHashMap<String, MutableMap<Target, Double>>()
            val generatingTargets = LinkedHashMap<String, MutableMap<Target, Double>>()
            val validRules = mutableListOf<PhoneticRule>()
            var longestSpelling = 1

            for (rule in rules) {
                val similarity = rule.similarity
                if (similarity.isNaN() || similarity <= 0 || similarity >= 1) {
                    problems.add(
                        "similarity must be greater than 0 and less than 1 " +
                            "(got $similarity for ${rule.group})",
                    )
                    continue
                }
                val group = mutableListOf<String>()
                var hasEmptySpelling = false
                for (raw in rule.group) {
                    val spelling = language.normalize(raw)
                    if (spelling.isEmpty()) {
                        hasEmptySpelling = true
                    } else if (spelling !in group) {
                        group.add(spelling)
                    }
                }
                if (hasEmptySpelling) {
                    problems.add("spellings must not be empty (in ${rule.group})")
                    continue
                }
                if (group.size < 2) {
                    problems.add("a rule needs at least two different spellings (got ${rule.group})")
                    continue
                }
                validRules.add(PhoneticRule(group.toList(), similarity, rule.position, rule.scoreOnly))

                for (from in group) {
                    longestSpelling = max(longestSpelling, from.length)
                    val tables = if (rule.scoreOnly) listOf(scoringTargets) else listOf(scoringTargets, generatingTargets)
                    for (table in tables) {
                        val targets = table.getOrPut(from) { LinkedHashMap() }
                        for (to in group) {
                            if (from == to) continue
                            val key = Target(to, rule.position)
                            val existing = targets[key]
                            if (existing == null || similarity > existing) targets[key] = similarity
                        }
                    }
                }
            }

            return Compiled(
                compileTargets(scoringTargets),
                compileTargets(generatingTargets),
                validRules.toList(),
                longestSpelling,
                problems.toList(),
            )
        }

        fun compileTargets(
            targetsByFrom: Map<String, Map<Target, Double>>,
        ): Map<String, List<List<PhoneticAlternative>>> {
            val compiled = HashMap<String, List<List<PhoneticAlternative>>>()
            for ((from, targets) in targetsByFrom) {
                compiled[from] = List(4) { context ->
                    alternativesForContext(
                        targets,
                        atWordStart = context and 1 != 0,
                        atWordEnd = context and 2 != 0,
                    )
                }
            }
            return compiled
        }

        fun alternativesForContext(
            targets: Map<Target, Double>,
            atWordStart: Boolean,
            atWordEnd: Boolean,
        ): List<PhoneticAlternative> {
            val best = LinkedHashMap<String, Double>()
            for ((key, similarity) in targets) {
                if (!key.position.appliesTo(atWordStart, atWordEnd)) continue
                val existing = best[key.text]
                if (existing == null || similarity > existing) best[key.text] = similarity
            }
            return best.map { (text, similarity) -> PhoneticAlternative(text, similarity) }
                .sortedWith { a, b ->
                    val bySimilarity = b.similarity.compareTo(a.similarity)
                    if (bySimilarity != 0) bySimilarity else a.text.compareTo(b.text)
                }
        }
    }
}
