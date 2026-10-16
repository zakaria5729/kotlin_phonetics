package io.github.zakaria5729.phonetics

/**
 * Describes a language: its rules and how its text is cleaned up before use.
 *
 * The bundled ones are [PhoneticLanguages.bangla] and [PhoneticLanguages.english]; build your own
 * to add another language.
 */
public class PhoneticLanguage(
    /** The language code, for example `bn`. */
    public val code: String,
    /** The language name. */
    public val name: String,
    /** The default sound-alike rules. */
    public val rules: List<PhoneticRule>,
    /** Whether upper and lower case letters are treated as the same. */
    public val foldCase: Boolean = false,
    /** Letter sequences that are rewritten to one standard form before use. */
    public val canonicalSequences: Map<String, String> = emptyMap(),
    /** Code points of marks that attach to a letter instead of standing alone. */
    public val marks: Set<Int> = emptySet(),
) {
    /** The problem with the definition, or `null` when it is valid. */
    public val error: PhoneticArgumentException? = combine(
        buildList {
            if (code.isEmpty()) add("the language code must not be empty")
            if (canonicalSequences.keys.any { it.isEmpty() }) {
                add("canonical sequences must not have an empty key")
            }
        },
    )

    internal val normalizer: TextNormalizer by lazy {
        TextNormalizer(foldCase, canonicalSequences, marks)
    }

    /** The rules this language ships with, compiled for lookup. */
    public val defaultRuleSet: PhoneticRuleSet by lazy { PhoneticRuleSet(rules, this) }

    /** Cleans up [text] the way the engine does before comparing it. */
    public fun normalize(text: String): String = normalizer.normalize(text)

    override fun toString(): String = "PhoneticLanguage($code)"

    private companion object {
        fun combine(problems: List<String>): PhoneticArgumentException? =
            if (problems.isEmpty()) {
                null
            } else {
                PhoneticArgumentException("invalid language: ${problems.joinToString("; ")}")
            }
    }
}
