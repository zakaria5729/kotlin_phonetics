package io.github.zakaria5729.phonetics

internal sealed interface SpellingPlan {
    class Ready(val referenceSpelling: String, val slots: List<Slot>) : SpellingPlan

    class Rejected(val reason: String) : SpellingPlan
}

internal class SpellingPlanBuilder {
    private val referenceBuilder = StringBuilder()
    private val slots = mutableListOf<Slot>()
    private var nextSequence = 0

    fun appendFixed(text: String) {
        referenceBuilder.append(text)
    }

    fun appendChoice(options: SpellingOptions) {
        val start = referenceBuilder.length
        referenceBuilder.append(options.reference)
        if (options.hasAlternatives) {
            slots.add(Slot(start, options.reference, options.alternatives, nextSequence++))
        }
    }

    fun build(): SpellingPlan.Ready =
        SpellingPlan.Ready(referenceBuilder.toString(), orderSlotsByConfidence(slots))
}
