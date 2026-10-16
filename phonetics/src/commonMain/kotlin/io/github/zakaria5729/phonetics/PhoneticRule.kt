package io.github.zakaria5729.phonetics

/** Where in a word a rule may be used. */
public enum class RulePosition {
    /** At any place in the word. */
    Anywhere,

    /** Only at the start of the word. */
    WordStart,

    /** Anywhere except the start of the word. */
    NotWordStart,

    /** Only at the end of the word. */
    WordEnd;

    internal fun appliesTo(atWordStart: Boolean, atWordEnd: Boolean): Boolean = when (this) {
        Anywhere -> true
        WordStart -> atWordStart
        NotWordStart -> !atWordStart
        WordEnd -> atWordEnd
    }
}

/**
 * A group of spellings that sound alike.
 *
 * Every spelling in [group] can replace any other, scored by [similarity].
 * A [scoreOnly] rule counts when comparing words but is never used to build
 * new spellings.
 */
public class PhoneticRule(
    /** The spellings that sound alike. */
    public val group: List<String>,
    /** How alike the spellings sound, above 0 and below 1. */
    public val similarity: Double,
    /** Where in a word the rule applies. */
    public val position: RulePosition = RulePosition.Anywhere,
    public val scoreOnly: Boolean = false,
) {
    override fun toString(): String =
        "PhoneticRule($group, $similarity, $position${if (scoreOnly) ", scoreOnly" else ""})"
}
