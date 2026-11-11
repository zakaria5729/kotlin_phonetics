package io.github.zakaria5729.phonetics

public class CrossLanguageEngine(
    banglaRules: PhoneticRuleSet? = null,
    gapSimilarity: Double = 0.5,
    markGapSimilarity: Double = 0.85,
    unrelatedSimilarity: Double = 0.4,
) {
    private val banglaEngine = PhoneticEngine(
        banglaLanguage,
        banglaRules,
        gapSimilarity,
        markGapSimilarity,
        unrelatedSimilarity,
    )
    private val englishEngine = PhoneticEngine(englishLanguage)

    public val error: PhoneticArgumentException?
        get() = banglaEngine.error

    public fun generate(
        input: String,
        languages: Set<PhoneticLanguage>,
        banglaOptions: PhoneticSearchOptions,
        englishOptions: PhoneticSearchOptions,
    ): CrossLanguageResult {
        crossLanguageArgumentError(languages, banglaOptions, englishOptions)?.let {
            return failedCrossLanguageResult(it)
        }
        return try {
            createJob(input, languages, banglaOptions, englishOptions).run()
        } catch (failure: Throwable) {
            failedCrossLanguageResult(PhoneticWorkerException("$failure"))
        }
    }

    public fun generate(
        input: String,
        languages: Set<PhoneticLanguage> = bothSupportedLanguages,
        minScore: Double = 70.0,
        maxResults: Int? = 50,
        includeOriginal: Boolean = true,
        maxChanges: Int? = null,
    ): CrossLanguageResult {
        val options = PhoneticSearchOptions(minScore, maxResults, includeOriginal, maxChanges)
        return generate(input, languages, options, options)
    }

    internal fun createJob(
        input: String,
        languages: Set<PhoneticLanguage>,
        banglaOptions: PhoneticSearchOptions,
        englishOptions: PhoneticSearchOptions,
    ): PhoneticJob<CrossLanguageResult> {
        val wantsBangla = languages.any { it.code == banglaLanguage.code }
        val wantsEnglish = languages.any { it.code == englishLanguage.code }
        val notRequested = SideOfResult.notRequested()

        return when (detectInputScript(input)) {
            InputScript.EMPTY -> CrossLanguageJob(
                if (wantsBangla) SideOfResult.finished() else notRequested,
                if (wantsEnglish) SideOfResult.finished() else notRequested,
            )

            InputScript.UNSUPPORTED -> {
                val rejection = SideOfResult.failed(
                    PhoneticArgumentException("the input must be a Bangla or English name"),
                )
                CrossLanguageJob(
                    if (wantsBangla) rejection else notRequested,
                    if (wantsEnglish) rejection else notRequested,
                )
            }

            InputScript.BANGLA -> CrossLanguageJob(
                if (wantsBangla) createBanglaVariantsSide(input, banglaOptions) else notRequested,
                if (wantsEnglish) createEnglishFromBanglaSide(input, englishOptions) else notRequested,
            )

            InputScript.LATIN -> CrossLanguageJob(
                if (wantsBangla) createBanglaFromEnglishSide(input, banglaOptions) else notRequested,
                if (wantsEnglish) createEnglishVariantsSide(input, englishOptions) else notRequested,
            )
        }
    }

    private fun createBanglaVariantsSide(input: String, options: PhoneticSearchOptions): SideOfResult {
        banglaEngine.error?.let { return SideOfResult.failed(it) }
        return guardedSide {
            banglaEngine.createGenerateJob(
                input,
                options.minScore,
                options.maxResults,
                options.includeOriginal,
                options.maxChanges,
            )
        }
    }

    private fun createEnglishVariantsSide(input: String, options: PhoneticSearchOptions): SideOfResult =
        guardedSide {
            englishEngine.createGenerateJob(
                input,
                options.minScore,
                options.maxResults,
                options.includeOriginal,
                options.maxChanges,
            )
        }

    private fun createEnglishFromBanglaSide(input: String, options: PhoneticSearchOptions): SideOfResult =
        sideFromPlan(
            plan = { planEnglishSpellings(banglaLanguage.normalize(input)) },
            rejectionPrefix = "cannot read the input as a Bangla name",
            options = options,
            render = ::capitalizeEachWord,
        )

    private fun createBanglaFromEnglishSide(input: String, options: PhoneticSearchOptions): SideOfResult =
        sideFromPlan(
            plan = { planBanglaSpellings(englishLanguage.normalize(input)) },
            rejectionPrefix = "cannot read the input as an English name",
            options = options,
            render = { it },
        )

    private fun sideFromPlan(
        plan: () -> SpellingPlan,
        rejectionPrefix: String,
        options: PhoneticSearchOptions,
        render: (String) -> String,
    ): SideOfResult = try {
        when (val spellingPlan = plan()) {
            is SpellingPlan.Rejected -> SideOfResult.failed(
                PhoneticArgumentException("$rejectionPrefix: ${spellingPlan.reason}"),
            )

            is SpellingPlan.Ready -> SideOfResult.running(
                GenerateJob(
                    spellingPlan.referenceSpelling,
                    spellingPlan.slots,
                    render,
                    options.minScore,
                    options.maxResults,
                    spellingPlan.referenceSpelling.length,
                    includeOriginal = true,
                    options.maxChanges,
                ),
            )
        }
    } catch (failure: Throwable) {
        SideOfResult.failed(PhoneticWorkerException("$failure"))
    }

    private fun guardedSide(createJob: () -> PhoneticJob<List<PhoneticMatch>>): SideOfResult = try {
        SideOfResult.running(createJob())
    } catch (failure: Throwable) {
        SideOfResult.failed(PhoneticWorkerException("$failure"))
    }
}

internal class SideOfResult private constructor(
    val job: PhoneticJob<List<PhoneticMatch>>?,
    val error: PhoneticException?,
) {
    val isFinished: Boolean
        get() = job == null || job.isDone

    fun toMatchesResult(): PhoneticMatchesResult =
        PhoneticMatchesResult(if (error == null) job?.result.orEmpty() else emptyList(), error)

    companion object {
        fun running(job: PhoneticJob<List<PhoneticMatch>>) = SideOfResult(job, null)

        fun failed(error: PhoneticException) = SideOfResult(null, error)

        fun notRequested() = SideOfResult(null, null)

        fun finished() = SideOfResult(ReadyJob(emptyList()), null)
    }
}

internal class CrossLanguageJob(
    private val banglaSide: SideOfResult,
    private val englishSide: SideOfResult,
) : PhoneticJob<CrossLanguageResult>() {
    override val isDone: Boolean
        get() = banglaSide.isFinished && englishSide.isFinished

    override val result: CrossLanguageResult
        get() = CrossLanguageResult(banglaSide.toMatchesResult(), englishSide.toMatchesResult())

    override fun advance(workUnits: Int) {
        val side = if (!banglaSide.isFinished) banglaSide else englishSide
        side.job?.advance(workUnits)
    }
}

internal class ReadyJob<T>(override val result: T) : PhoneticJob<T>() {
    override val isDone: Boolean
        get() = true

    override fun advance(workUnits: Int) = Unit
}
