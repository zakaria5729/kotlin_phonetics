package io.github.zakaria5729.phonetics

import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min

private const val NO_LIMIT = 0x3FFFFFFF
private const val RESULT_CEILING = 100_000
private const val OUTPUT_CHARACTER_BUDGET = 20_000_000
private const val ROUNDING_TOLERANCE = 0.005

private fun resultLimit(requested: Int?, inputLength: Int): Int =
    min(min(requested ?: RESULT_CEILING, RESULT_CEILING), OUTPUT_CHARACTER_BUDGET / max(inputLength, 1))

/**
 * The synchronous engine behind [Phonetics].
 *
 * It runs on the calling thread, so use it for short work or from a
 * background dispatcher. Like [Phonetics], it returns errors in result
 * objects instead of throwing.
 */
public class PhoneticEngine(
    /** The language the engine works with. */
    public val language: PhoneticLanguage,
    rules: PhoneticRuleSet? = null,
    /** Score of a letter that is missing from the other spelling, above 0 and below 1. */
    public val gapSimilarity: Double = 0.5,
    /** Score of a missing mark such as a vowel sign, above 0 and below 1. */
    public val markGapSimilarity: Double = 0.85,
    /** Score of two letters with no rule between them, above 0 and below 1. */
    public val unrelatedSimilarity: Double = 0.4,
) {
    /** The rules used for generating and scoring. */
    public val rules: PhoneticRuleSet = rules ?: language.defaultRuleSet

    /** The problem with the settings, or `null` when they are valid. */
    public val error: PhoneticArgumentException? =
        settingsError(language, this.rules, gapSimilarity, markGapSimilarity, unrelatedSimilarity)

    internal val logGap: Double = ln(gapSimilarity)
    internal val logMarkGap: Double = ln(markGapSimilarity)
    internal val logUnrelated: Double = ln(unrelatedSimilarity)

    /**
     * Generates spelling variants of [input] that sound alike, best match first.
     *
     * Only variants scoring at least [minScore] (0 to 100) are returned, up to
     * [maxResults]; pass `null` for no limit. [includeOriginal] keeps the input
     * in the list and [maxChanges] limits how many letters may differ.
     */
    public fun generate(
        input: String,
        minScore: Double = 70.0,
        maxResults: Int? = 50,
        includeOriginal: Boolean = true,
        maxChanges: Int? = null,
    ): PhoneticMatchesResult {
        val problem = error ?: generateArgumentError(minScore, maxResults, maxChanges)
        if (problem != null) return PhoneticMatchesResult(emptyList(), problem)
        return try {
            val job = createGenerateJob(input, minScore, maxResults, includeOriginal, maxChanges)
            PhoneticMatchesResult(job.run(), null)
        } catch (failure: Throwable) {
            PhoneticMatchesResult(emptyList(), PhoneticWorkerException("$failure"))
        }
    }

    /** Like [generate], but returns only the spellings. */
    public fun generateWords(
        input: String,
        minScore: Double = 70.0,
        maxResults: Int? = 50,
        includeOriginal: Boolean = true,
        maxChanges: Int? = null,
    ): PhoneticWordsResult {
        val result = generate(input, minScore, maxResults, includeOriginal, maxChanges)
        return PhoneticWordsResult(result.matches.map { it.word }, result.error)
    }

    /** Scores how alike [a] and [b] sound, from 0 to 100. */
    public fun similarity(a: String, b: String): PhoneticSimilarityResult {
        error?.let { return PhoneticSimilarityResult(0.0, it) }
        return try {
            val first = language.normalize(a)
            val second = language.normalize(b)
            when {
                first == second -> PhoneticSimilarityResult(100.0, null)
                first.isEmpty() || second.isEmpty() -> PhoneticSimilarityResult(0.0, null)
                else -> {
                    val logScore = SimilarityScorer(this, first)
                        .logScoreAgainst(second, Double.NEGATIVE_INFINITY)
                    PhoneticSimilarityResult(toPercent(logScore), null)
                }
            }
        } catch (failure: Throwable) {
            PhoneticSimilarityResult(0.0, PhoneticWorkerException("$failure"))
        }
    }

    /** Tells whether [a] and [b] sound alike, meaning their similarity is at least [minScore]. */
    public fun matches(a: String, b: String, minScore: Double = 80.0): PhoneticMatchCheckResult {
        minScoreError(minScore)?.let { return PhoneticMatchCheckResult(false, it) }
        val result = similarity(a, b)
        return PhoneticMatchCheckResult(result.score >= minScore, result.error)
    }

    /** Orders [candidates] by how closely they sound like [query]. */
    public fun rank(
        query: String,
        candidates: Iterable<String>,
        minScore: Double = 0.0,
        limit: Int? = null,
    ): PhoneticMatchesResult {
        val problem = error ?: rankArgumentError(minScore, limit)
        if (problem != null) return PhoneticMatchesResult(emptyList(), problem)
        val candidateList = try {
            candidates.toList()
        } catch (failure: Throwable) {
            return PhoneticMatchesResult(
                emptyList(),
                PhoneticArgumentException("could not read the candidates: $failure"),
            )
        }
        return try {
            val job = createRankJob(query, candidateList, minScore, limit)
            PhoneticMatchesResult(job.run(), null)
        } catch (failure: Throwable) {
            PhoneticMatchesResult(emptyList(), PhoneticWorkerException("$failure"))
        }
    }

    internal fun createGenerateJob(
        input: String,
        minScore: Double,
        maxResults: Int?,
        includeOriginal: Boolean,
        maxChanges: Int?,
    ): PhoneticJob<List<PhoneticMatch>> =
        GenerateJob(this, input, minScore, maxResults, includeOriginal, maxChanges)

    internal fun createRankJob(
        query: String,
        candidates: List<String>,
        minScore: Double,
        limit: Int?,
    ): PhoneticJob<List<PhoneticMatch>> = RankJob.create(this, query, candidates, minScore, limit)

    private companion object {
        fun settingsError(
            language: PhoneticLanguage,
            rules: PhoneticRuleSet,
            gap: Double,
            markGap: Double,
            unrelated: Double,
        ): PhoneticArgumentException? {
            val problems = buildList {
                language.error?.let { add(it.message) }
                if (rules.languageCode != language.code) {
                    add(
                        "the rules are for language \"${rules.languageCode}\" but the engine " +
                            "uses \"${language.code}\"",
                    )
                }
                if (!isFraction(gap)) add("gapSimilarity must be between 0 and 1 (got $gap)")
                if (!isFraction(markGap)) add("markGapSimilarity must be between 0 and 1 (got $markGap)")
                if (!isFraction(unrelated)) add("unrelatedSimilarity must be between 0 and 1 (got $unrelated)")
                rules.error?.let { add(it.message) }
            }
            return if (problems.isEmpty()) null else PhoneticArgumentException(problems.joinToString("; "))
        }

        fun isFraction(value: Double): Boolean = !value.isNaN() && value > 0 && value < 1
    }
}

private fun logCutoffFor(minScore: Double): Double {
    val lowest = minScore - ROUNDING_TOLERANCE
    return if (lowest <= 0) Double.NEGATIVE_INFINITY else ln(lowest / 100)
}

private fun roundHalfAwayFromZero(value: Double): Double {
    val whole = floor(value)
    return if (value - whole >= 0.5) whole + 1 else whole
}

private fun toPercent(logScore: Double): Double =
    if (logScore == Double.NEGATIVE_INFINITY) 0.0 else roundHalfAwayFromZero(exp(logScore) * 10000) / 100

internal abstract class PhoneticJob<T> {
    abstract val isDone: Boolean

    abstract val result: T

    abstract fun advance(workUnits: Int)

    fun run(): T {
        while (!isDone) advance(NO_LIMIT)
        return result
    }
}

private class SearchState(
    val parent: SearchState?,
    val slot: Int,
    val alternativeNumber: Int,
    val logScore: Double,
    val changeCount: Int,
    val order: Int,
)

private class Slot(
    val start: Int,
    val spelling: String,
    val alternatives: List<PhoneticAlternative>,
)

private class GenerateJob(
    engine: PhoneticEngine,
    input: String,
    private val minScore: Double,
    maxResults: Int?,
    private val includeOriginal: Boolean,
    maxChanges: Int?,
) : PhoneticJob<List<PhoneticMatch>>() {
    private val normalizer = engine.language.normalizer
    private val normalizedInput = engine.language.normalize(input)
    private val letterCase = engine.language.normalizer.detectCase(input)
    private val logCutoff = logCutoffFor(minScore)
    private val maxResults = resultLimit(maxResults, input.length)
    private val maxChanges = maxChanges ?: NO_LIMIT
    private val maxExpansions = this.maxResults * 16 + 1024

    private val slots = mutableListOf<Slot>()
    private val queue = Heap<SearchState> { a, b ->
        a.logScore > b.logScore || (a.logScore == b.logScore && a.order < b.order)
    }
    private val seenWords = HashSet<String>()
    private val results = mutableListOf<PhoneticMatch>()
    private val changedStates = mutableListOf<SearchState>()
    private var createdCount = 0
    private var expansions = 0
    private var finished = false

    init {
        if (normalizedInput.isEmpty()) {
            finished = true
        } else {
            findSlots(engine.rules)
            queue.push(SearchState(null, -1, 0, 0.0, 0, createdCount++))
        }
    }

    override val isDone: Boolean
        get() = finished

    override val result: List<PhoneticMatch>
        get() = results

    private fun findSlots(rules: PhoneticRuleSet) {
        val found = mutableListOf<Slot>()
        var index = 0
        while (index < normalizedInput.length) {
            var length = 1
            var alternatives: List<PhoneticAlternative>? = null
            var candidateLength = min(rules.longestSpellingLength, normalizedInput.length - index)
            while (candidateLength >= 1) {
                val options = rules.alternativesFor(
                    normalizedInput.substring(index, index + candidateLength),
                    atWordStart = startsWord(normalizedInput, index),
                    atWordEnd = endsWord(normalizedInput, index + candidateLength),
                    generating = true,
                )
                if (options.isNotEmpty()) {
                    length = candidateLength
                    alternatives = options
                    break
                }
                candidateLength--
            }
            if (alternatives != null) {
                found.add(Slot(index, normalizedInput.substring(index, index + length), alternatives))
            }
            index += length
        }
        found.sortWith { a, b ->
            val bySimilarity = b.alternatives.first().similarity.compareTo(a.alternatives.first().similarity)
            if (bySimilarity != 0) bySimilarity else a.start.compareTo(b.start)
        }
        slots.addAll(found)
    }

    override fun advance(workUnits: Int) {
        var done = 0
        while (!finished && done < workUnits) {
            if (queue.isEmpty) {
                finished = true
                break
            }
            val state = queue.pop()
            done++
            expansions++
            val score = toPercent(state.logScore)
            if (score < minScore) {
                finished = true
                break
            }
            addResult(state, score)
            if (results.size >= maxResults || expansions >= maxExpansions) {
                finished = true
                break
            }
            pushChildren(state)
        }
    }

    private fun addResult(state: SearchState, score: Double) {
        if (state.slot < 0) {
            seenWords.add(normalizedInput)
            if (includeOriginal) {
                results.add(PhoneticMatch(normalizer.applyCase(normalizedInput, letterCase), 100.0))
            }
            return
        }

        changedStates.clear()
        var current: SearchState? = state
        while (current != null) {
            changedStates.add(current)
            current = current.parent
        }
        changedStates.sortBy { slots[it.slot].start }

        val buffer = StringBuilder()
        var cursor = 0
        for (changed in changedStates) {
            val slot = slots[changed.slot]
            buffer.append(normalizedInput, cursor, slot.start)
                .append(slot.alternatives[changed.alternativeNumber - 1].text)
            cursor = slot.start + slot.spelling.length
        }
        buffer.append(normalizedInput, cursor, normalizedInput.length)

        val word = buffer.toString()
        if (!seenWords.add(word)) return

        val changes = changedStates.map { changed ->
            val slot = slots[changed.slot]
            val alternative = slot.alternatives[changed.alternativeNumber - 1]
            PhoneticChange(slot.start, slot.spelling, alternative.text, alternative.similarity)
        }
        results.add(PhoneticMatch(normalizer.applyCase(word, letterCase), score, changes))
    }

    private fun pushChildren(state: SearchState) {
        if (slots.isEmpty()) return

        if (state.slot < 0) {
            if (maxChanges > 0) pushState(null, 0, 1, slots[0].alternatives[0].logSimilarity, 1)
            return
        }

        val slotIndex = state.slot
        val alternativeNumber = state.alternativeNumber
        val alternatives = slots[slotIndex].alternatives

        if (alternativeNumber < alternatives.size) {
            pushState(
                state.parent,
                slotIndex,
                alternativeNumber + 1,
                state.logScore - alternatives[alternativeNumber - 1].logSimilarity +
                    alternatives[alternativeNumber].logSimilarity,
                state.changeCount,
            )
        }
        if (slotIndex + 1 < slots.size) {
            val nextSlotLogSimilarity = slots[slotIndex + 1].alternatives[0].logSimilarity
            if (state.changeCount < maxChanges) {
                pushState(
                    state,
                    slotIndex + 1,
                    1,
                    state.logScore + nextSlotLogSimilarity,
                    state.changeCount + 1,
                )
            }
            if (alternativeNumber == 1) {
                pushState(
                    state.parent,
                    slotIndex + 1,
                    1,
                    state.logScore - alternatives[0].logSimilarity + nextSlotLogSimilarity,
                    state.changeCount,
                )
            }
        }
    }

    private fun pushState(
        parent: SearchState?,
        slot: Int,
        alternativeNumber: Int,
        logScore: Double,
        changeCount: Int,
    ) {
        if (logScore < logCutoff) return
        queue.push(SearchState(parent, slot, alternativeNumber, logScore, changeCount, createdCount++))
    }
}

private class RankJob private constructor(
    private val normalizer: TextNormalizer,
    private val query: String,
    private val scorer: SimilarityScorer?,
    private val candidates: List<String>,
    private val minScore: Double,
    private val limit: Int?,
) : PhoneticJob<List<PhoneticMatch>>() {
    private val minLogCutoff = logCutoffFor(minScore)
    private var logCutoff = logCutoffFor(minScore)

    private val seenCandidates = HashSet<String>()
    private val allMatches = mutableListOf<PhoneticMatch>()
    private val topMatches = Heap<PhoneticMatch> { a, b ->
        a.score < b.score || (a.score == b.score && a.word > b.word)
    }
    private var nextCandidate = 0
    private var finished = false
    private var ranked: List<PhoneticMatch> = emptyList()

    override val isDone: Boolean
        get() = finished

    override val result: List<PhoneticMatch>
        get() = ranked

    override fun advance(workUnits: Int) {
        var done = 0
        while (!finished && done < workUnits) {
            if (nextCandidate >= candidates.size) {
                finish()
                return
            }
            val candidate = candidates[nextCandidate++]
            if (!seenCandidates.add(candidate)) continue
            done++

            val normalized = normalizer.normalize(candidate)
            val activeScorer = scorer
            val score = when {
                normalized == query -> 100.0
                normalized.isEmpty() || activeScorer == null -> 0.0
                else -> toPercent(activeScorer.logScoreAgainst(normalized, logCutoff))
            }
            if (score < minScore) continue

            val match = PhoneticMatch(candidate, score)
            val maxKept = limit
            if (maxKept == null) {
                allMatches.add(match)
            } else if (topMatches.length < maxKept) {
                topMatches.push(match)
                if (topMatches.length == maxKept) raiseCutoff()
            } else if (topMatches.hasHigherPriority(topMatches.peek, match)) {
                topMatches.replaceTop(match)
                raiseCutoff()
            }
        }
    }

    private fun raiseCutoff() {
        logCutoff = max(minLogCutoff, logCutoffFor(topMatches.peek.score))
    }

    private fun finish() {
        val matches = if (limit == null) allMatches else topMatches.drain()
        matches.sortWith { a, b ->
            val byScore = b.score.compareTo(a.score)
            if (byScore != 0) byScore else a.word.compareTo(b.word)
        }
        ranked = matches
        finished = true
    }

    companion object {
        fun create(
            engine: PhoneticEngine,
            query: String,
            candidates: List<String>,
            minScore: Double,
            limit: Int?,
        ): RankJob {
            val normalizer = engine.language.normalizer
            val normalizedQuery = normalizer.normalize(query)
            return RankJob(
                normalizer,
                normalizedQuery,
                if (normalizedQuery.isEmpty()) null else SimilarityScorer(engine, normalizedQuery),
                candidates,
                minScore,
                limit,
            )
        }
    }
}

private class RuleMove(val fromLength: Int, val to: String, val logSimilarity: Double)

internal class SimilarityScorer(private val engine: PhoneticEngine, private val query: String) {
    private val normalizer = engine.language.normalizer
    private val dropCosts = DoubleArray(query.length)
    private val ruleMoves = arrayOfNulls<MutableList<RuleMove>>(query.length)
    private val rowCount = engine.rules.longestSpellingLength + 1
    private val rowBestScores = DoubleArray(rowCount)
    private var rows: Array<DoubleArray> = emptyArray()

    init {
        val longestSpelling = engine.rules.longestSpellingLength
        for (start in query.indices) {
            dropCosts[start] = if (normalizer.isMark(query[start])) engine.logMarkGap else engine.logGap
            var moves: MutableList<RuleMove>? = null
            var length = 1
            while (length <= longestSpelling && start + length <= query.length) {
                val spelling = query.substring(start, start + length)
                val alternatives = engine.rules.alternativesFor(
                    spelling,
                    atWordStart = startsWord(query, start),
                    atWordEnd = endsWord(query, start + length),
                )
                for (alternative in alternatives) {
                    val list = moves ?: mutableListOf<RuleMove>().also { moves = it }
                    list.add(RuleMove(length, alternative.text, alternative.logSimilarity))
                }
                length++
            }
            ruleMoves[start] = moves
        }
    }

    fun logScoreAgainst(other: String, logCutoff: Double): Double {
        val unreachable = Double.NEGATIVE_INFINITY
        val queryLength = query.length
        val otherLength = other.length
        val lookAhead = rowCount - 1
        val logGap = engine.logGap
        val logMarkGap = engine.logMarkGap
        val logUnrelated = engine.logUnrelated
        val checkCutoff = logCutoff != unreachable

        if (rows.isEmpty() || rows[0].size < otherLength + 1) {
            rows = Array(rowCount) { DoubleArray(otherLength + 1) }
        }
        val grid = rows
        for (row in 0 until rowCount) {
            grid[row].fill(unreachable, 0, otherLength + 1)
            rowBestScores[row] = unreachable
        }
        grid[0][0] = 0.0

        var queryIndex = 0
        while (true) {
            val currentRow = grid[queryIndex % rowCount]
            val hasQueryUnit = queryIndex < queryLength
            val nextRowIndex = (queryIndex + 1) % rowCount
            val nextRow = grid[nextRowIndex]
            val queryUnit = if (hasQueryUnit) query[queryIndex].code else -1
            val dropCost = if (hasQueryUnit) dropCosts[queryIndex] else 0.0
            val moves = if (hasQueryUnit) ruleMoves[queryIndex] else null

            for (otherIndex in 0..otherLength) {
                val score = currentRow[otherIndex]
                if (score == unreachable) continue
                val otherUnit = if (otherIndex < otherLength) other[otherIndex].code else -1

                if (otherIndex < otherLength) {
                    val inserted = score + if (normalizer.isMark(other[otherIndex])) logMarkGap else logGap
                    if (inserted > currentRow[otherIndex + 1]) currentRow[otherIndex + 1] = inserted
                }
                if (!hasQueryUnit) continue

                var candidate = score + dropCost
                if (candidate > nextRow[otherIndex]) {
                    nextRow[otherIndex] = candidate
                    if (candidate > rowBestScores[nextRowIndex]) rowBestScores[nextRowIndex] = candidate
                }
                if (otherIndex < otherLength) {
                    candidate = score + if (queryUnit == otherUnit) 0.0 else logUnrelated
                    if (candidate > nextRow[otherIndex + 1]) {
                        nextRow[otherIndex + 1] = candidate
                        if (candidate > rowBestScores[nextRowIndex]) rowBestScores[nextRowIndex] = candidate
                    }
                }
                if (moves != null) {
                    for (move in moves) {
                        val endIndex = otherIndex + move.to.length
                        if (endIndex > otherLength || !matchesAt(other, move.to, otherIndex)) continue
                        val targetRowIndex = (queryIndex + move.fromLength) % rowCount
                        val targetRow = grid[targetRowIndex]
                        candidate = score + move.logSimilarity
                        if (candidate > targetRow[endIndex]) {
                            targetRow[endIndex] = candidate
                            if (candidate > rowBestScores[targetRowIndex]) rowBestScores[targetRowIndex] = candidate
                        }
                    }
                }
            }

            if (!hasQueryUnit) return currentRow[otherLength]

            if (checkCutoff) {
                var frontier = unreachable
                var ahead = 1
                while (ahead <= lookAhead && queryIndex + ahead <= queryLength) {
                    val best = rowBestScores[(queryIndex + ahead) % rowCount]
                    if (best > frontier) frontier = best
                    ahead++
                }
                if (frontier < logCutoff) return unreachable
            }

            currentRow.fill(unreachable, 0, otherLength + 1)
            rowBestScores[queryIndex % rowCount] = unreachable
            queryIndex++
        }
    }

    private fun matchesAt(text: String, piece: String, start: Int): Boolean {
        for (i in piece.indices) {
            if (text[start + i] != piece[i]) return false
        }
        return true
    }
}

private class Heap<T>(val hasHigherPriority: (T, T) -> Boolean) {
    private val items = ArrayList<T>()

    val length: Int
        get() = items.size
    val isEmpty: Boolean
        get() = items.isEmpty()
    val peek: T
        get() = items.first()

    fun push(item: T) {
        items.add(item)
        siftUp(items.size - 1)
    }

    fun pop(): T {
        val top = items.first()
        val last = items.removeAt(items.size - 1)
        if (items.isNotEmpty()) {
            items[0] = last
            siftDown(0)
        }
        return top
    }

    fun replaceTop(item: T) {
        items[0] = item
        siftDown(0)
    }

    fun drain(): MutableList<T> {
        val drained = ArrayList(items)
        items.clear()
        return drained
    }

    private fun siftUp(start: Int) {
        var index = start
        val item = items[index]
        while (index > 0) {
            val parent = (index - 1) shr 1
            if (!hasHigherPriority(item, items[parent])) break
            items[index] = items[parent]
            index = parent
        }
        items[index] = item
    }

    private fun siftDown(start: Int) {
        var index = start
        val count = items.size
        val item = items[index]
        while (true) {
            val left = 2 * index + 1
            if (left >= count) break
            val right = left + 1
            val child = if (right < count && hasHigherPriority(items[right], items[left])) right else left
            if (!hasHigherPriority(items[child], item)) break
            items[index] = items[child]
            index = child
        }
        items[index] = item
    }
}
