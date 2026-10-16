package io.github.zakaria5729.phonetics

/** One letter or group of letters that was swapped to reach a variant. */
public class PhoneticChange(
    /** Where in the original word the swap happened. */
    public val position: Int,
    /** The spelling in the original word. */
    public val from: String,
    /** The spelling used in the variant. */
    public val to: String,
    /** How alike the two spellings sound, from 0 to 1. */
    public val similarity: Double,
) {
    override fun toString(): String = "$from→$to"

    override fun equals(other: Any?): Boolean =
        other is PhoneticChange &&
            other.position == position &&
            other.from == from &&
            other.to == to &&
            other.similarity == similarity

    override fun hashCode(): Int {
        var result = position
        result = 31 * result + from.hashCode()
        result = 31 * result + to.hashCode()
        result = 31 * result + similarity.hashCode()
        return result
    }
}

/** A spelling variant with its match percentage. */
public class PhoneticMatch(
    /** The variant spelling. */
    public val word: String,
    /** How closely the variant sounds like the input, from 0 to 100. */
    public val score: Double,
    /** The swaps that turn the input into [word]. */
    public val changes: List<PhoneticChange> = emptyList(),
) {
    override fun toString(): String = "$word (${formatTwoDecimals(score)}%)"

    override fun equals(other: Any?): Boolean =
        other is PhoneticMatch && other.word == word && other.score == score

    override fun hashCode(): Int = 31 * word.hashCode() + score.hashCode()
}

internal fun formatTwoDecimals(value: Double): String {
    val hundredths = kotlin.math.round(kotlin.math.abs(value) * 100).toLong()
    val sign = if (value < 0 && hundredths != 0L) "-" else ""
    val cents = (hundredths % 100).toString().padStart(2, '0')
    return "$sign${hundredths / 100}.$cents"
}
