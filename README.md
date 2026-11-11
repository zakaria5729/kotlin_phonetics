# kotlin-phonetics

[![Maven Central](https://img.shields.io/badge/Maven%20Central-0.2.0-blue)](https://central.sonatype.com/artifact/io.github.zakaria5729/phonetics)
[![JitPack](https://img.shields.io/badge/JitPack-0.2.0-green)](https://jitpack.io/#zakaria5729/kotlin_phonetics)
[![klibs.io](https://img.shields.io/badge/klibs.io-0.2.0-7F52FF)](https://klibs.io/project/zakaria5729/kotlin_phonetics)
[![Web demo](https://img.shields.io/badge/demo-live-orange)](https://zakaria5729.github.io/kotlin_phonetics/)

Finds sound-alike spellings of Bangla and English words, with a match percentage.
One Kotlin Multiplatform library for Android (Views and Compose), the JVM and
desktop, iOS and the other Apple platforms, the web, Linux and Windows. The
search runs off the calling thread, so the UI never waits for it.

Try it live: [zakaria5729.github.io/kotlin_phonetics](https://zakaria5729.github.io/kotlin_phonetics/)
— the `sample-compose` app, built as Kotlin/Wasm and deployed to GitHub Pages.
Switch between Bangla and English, and try the search box, pairwise similarity,
stress test, execution-mode and cross-language sections.

```kotlin
// Bangla phonetics
val bangla = BanglaPhonetics.instance
val result = bangla.generate("মাসুদ", minScore = 90.0)
// মাসুদ 100%, মাসূদ 97%, মাশুদ 95%, মাষুদ 95%, মাশূদ 92.15%, মাষূদ 92.15%

// English phonetics
val english = EnglishPhonetics.instance
val names = english.generate("Philip", minScore = 90.0)
// Philip 100%, Filip 95%, Phillip 95%, Philipp 95%, Fillip 90.25%, ...
```

A name in Bangla or English can also give spellings in the other script, see
[Cross-language phonetics](#cross-language-phonetics).

`generate` is a `suspend` function. Every call returns a result object with the
value and an `error`; nothing is thrown.

| Target | Runs on |
| --- | --- |
| Android, JVM and desktop (macOS, Linux, Windows) | a background thread (`Dispatchers.Default`) |
| iOS, macOS, tvOS, watchOS, Linux and Windows native | a background thread |
| Kotlin/JS and Kotlin/Wasm | the event loop, in 8 ms slices, so the page stays responsive and requests can still be cancelled |

## Modules

| Module | What it is |
| --- | --- |
| `phonetics` | The library: engine, languages, background execution, cancellation. |
| `phonetics-compose` | Compose helpers: remembered instances and state that follows the input. |
| `sample-android-xml` | An Android app built with XML layouts and the callback API. |
| `sample-compose` and `sample-compose-android` | A Compose Multiplatform app for Android, desktop, iOS and the web. |

## Install

Add the dependency to the module that needs it. In a Kotlin Multiplatform
project that is `commonMain`:

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("io.github.zakaria5729:phonetics:0.2.0")
            // for Compose
            implementation("io.github.zakaria5729:phonetics-compose:0.2.0")
        }
    }
}
```

An Android or JVM only project uses the same coordinates:

```kotlin
dependencies {
    implementation("io.github.zakaria5729:phonetics:0.2.0")
}
```

Groovy DSL (`build.gradle`) and Maven (`pom.xml`) for a JVM project:

```groovy
implementation 'io.github.zakaria5729:phonetics-jvm:0.2.0'
```

```xml
<dependency>
    <groupId>io.github.zakaria5729</groupId>
    <artifactId>phonetics-jvm</artifactId>
    <version>0.2.0</version>
</dependency>
```

Download from Maven Central:

| Artifact | Maven Central |
| --- | --- |
| `phonetics` | [central.sonatype.com/artifact/io.github.zakaria5729/phonetics](https://central.sonatype.com/artifact/io.github.zakaria5729/phonetics) |
| `phonetics-compose` | [central.sonatype.com/artifact/io.github.zakaria5729/phonetics-compose](https://central.sonatype.com/artifact/io.github.zakaria5729/phonetics-compose) |

The jar, sources and Javadoc files for every target are also in the
[repository folder](https://repo1.maven.org/maven2/io/github/zakaria5729/).
The project is listed on [klibs.io](https://klibs.io/project/zakaria5729/kotlin_phonetics),
the Kotlin Multiplatform library catalog.

The Android library needs `minSdk` 21. `phonetics-compose` needs a Compose
Multiplatform version that supports your targets (it has no Intel iOS or macOS
build, because Compose has none).

The artifacts are on Maven Central, so `mavenCentral()` is all you need in your
repositories. To try local changes, run `./gradlew publishToMavenLocal` in this
repository and add `mavenLocal()` as well.

### From JitPack

Every release tag is also built by [JitPack](https://jitpack.io/#zakaria5729/kotlin_phonetics).
Add the repository, then use the `com.github` coordinates with the tag as the
version:

```kotlin
repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    implementation("com.github.zakaria5729.kotlin_phonetics:phonetics:v0.2.0")
    // for Compose
    implementation("com.github.zakaria5729.kotlin_phonetics:phonetics-compose:v0.2.0")
}
```

Maven Central is the main source; use one or the other, not both, in the same
project.

## Usage

```kotlin
val phonetics = BanglaPhonetics.instance

val matches = phonetics.generate("আসাদ", minScore = 90.0, maxResults = 20)
matches.error?.let { println(it.message) }
matches.matches.forEach { println("${it.word} ${it.score}%") }

val score = phonetics.similarity("আসাদ", "আছাদ").score          // 85.0
val same = phonetics.matches("আসাদ", "আশাদ", minScore = 90.0).isMatch
val ranked = phonetics.rank("আসাদ", listOf("আছাদ", "রহিম", "আশাদ"), limit = 2)
```

Defaults are `minScore` 70 and `maxResults` 50. Each match has the `word`, the
`score` from 0 to 100 and the `changes` that turn the input into the word.

### Android with XML layouts, Java and Swift

Callbacks work without coroutines. They run on the main thread on Android and
Apple platforms, and the call returns a token that cancels the request:

```kotlin
val bangla = BanglaPhonetics()

input.addTextChangedListener { text ->
    bangla.generateAsync(text.toString(), maxResults = 20) { result ->
        results.text = result.matches.joinToString("\n") { "${it.word} ${it.score}%" }
    }
}

override fun onDestroy() {
    bangla.dispose()
    super.onDestroy()
}
```

`sample-android-xml` is a full example. In a view model, call the `suspend`
functions from `viewModelScope` or `lifecycleScope` instead.

For Java, use the `…Async` functions with a lambda that returns `Unit.INSTANCE`.
For Swift, the `suspend` functions become `async` functions and the callbacks
become closures:

```swift
let result = try await BanglaPhonetics.companion.instance.generate(input: "আসাদ")
print(result.matches.map { $0.word })

BanglaPhonetics.companion.instance.generateAsync(input: "আসাদ") { result in
    print(result.matches.count)
}
```

Build the XCFramework on a Mac with Xcode: `./gradlew :phonetics:assemblePhoneticsXCFramework`.
The Swift snippets above have not been compiled yet, because this was written on a machine without Xcode.

### Compose

```kotlin
@Composable
fun Names() {
    val phonetics = rememberBanglaPhonetics()
    var input by remember { mutableStateOf("আসাদ") }
    val result by rememberPhoneticMatches(phonetics, input, maxResults = 30, debounceMillis = 120)

    Column {
        OutlinedTextField(value = input, onValueChange = { input = it })
        LazyColumn {
            items(result?.matches.orEmpty()) { match -> Text("${match.word} ${match.score}%") }
        }
    }
}
```

`rememberPhonetics`, `rememberBanglaPhonetics` and `rememberEnglishPhonetics`
dispose the instance when it leaves the composition. `rememberPhoneticMatches`,
`rememberPhoneticSimilarity` and `rememberPhoneticRanking` return `null` until
the first result, keep the previous result while a new one is computed, and
cancel a running request when the input changes. It is the same code on
Android, desktop, iOS and the web; `sample-compose` shows it.

## Cross-language phonetics

`CrossLanguagePhonetics` takes a **Bangla or an English name** and returns two
separate lists: Bangla spelling variants and English spelling variants. The
spellings in the other script are generated straight from how the name sounds.
The script of the input decides the direction.

```kotlin
val result = CrossLanguagePhonetics.instance.generate(
    input = "মাসুদ",
    languages = setOf(PhoneticLanguages.bangla, PhoneticLanguages.english),
    minScore = 90.0,
)

result.bangla.matches   // মাসুদ 100%, মাসূদ 97%, মাশুদ 95%, মাষুদ 95%, মাশূদ 92.15%, মাষূদ 92.15%
result.english.matches  // Masud 100%, Masood 95%, Masoud 90%, Mashud 90%
result.error            // the first error of the two lists, or null
```

An English name works the same way:

```kotlin
val result = CrossLanguagePhonetics.instance.generate("Masud", minScore = 90.0)

result.bangla.matches   // মাসুদ 100%, মাসূদ 95%, মাসুড 95%, মসুদ 93%, মাশুদ 93%, ...
result.english.matches  // Masud 100%, Massud 95%, Masudd 95%, ...
```

Both lists are independent. Ask for one language with
`languages = setOf(PhoneticLanguages.english)`, or leave `languages` out to get
both.

| Input | Bangla list | English list |
| --- | --- | --- |
| Bangla name | what `BanglaPhonetics.generate` returns for the same arguments | English spellings generated from the Bangla pronunciation |
| English name | Bangla spellings generated from the English spelling | what `EnglishPhonetics.generate` returns for the same arguments |

### Why this is not transliteration

There is no Bangla → Roman step that feeds a list of candidates into
`EnglishPhonetics`, and no Roman transliteration is offered as a feature.
The name is analysed as sound: for a Bangla name syllables, inherent vowels,
conjuncts, geminates and nasal signs, for an English name its letter groups
(`sh`, `th`, `ng`, `ee`, `ay`, doubled letters). Every sound carries its own
spellings in the other script, each with a confidence. The search then combines them best first, like `generate` does
for swaps. Different Bangla spellings of one sound (স, শ, ষ; ই, ঈ, ি, ী) lead to
the same English candidates, which is what a name search needs.

### Scores and limits

- Scores are 0 to 100. The best reading of the name is 100%, and every other
  spelling is the product of the confidences of the sounds that differ from it.
- The English score is relative to the best English reading of the name, and the
  Bangla score to the input. They come from different models, so **do not compare
  a Bangla score with an English score**.
- `minScore` and `maxResults` are applied to each list on its own. Use
  `PhoneticSearchOptions` to set them per language:

```kotlin
val result = CrossLanguagePhonetics.instance.generate(
    input = "মোহাম্মদ",
    languages = setOf(PhoneticLanguages.bangla, PhoneticLanguages.english),
    banglaOptions = PhoneticSearchOptions(minScore = 95.0, maxResults = 5),
    englishOptions = PhoneticSearchOptions(minScore = 80.0, maxResults = 20),
)
```

- `includeOriginal` only concerns the list in the script of the input, because
  the other list has no original. The best reading in the other script is always
  included.
- `maxChanges` limits how many sounds may differ from the best reading, in both lists.
- Results are deduplicated and ordered the same way on every run.
- Empty or blank input gives empty lists. Input in another script, or only digits,
  gives a `PhoneticArgumentException` in both lists. A stray character such as an
  emoji inside a name fails only the list that has to read it. Digits, spaces,
  hyphens and dots are carried over, and English output has each word capitalised.

### Example outputs

| Input | Some of the English spellings, best first |
| --- | --- |
| মাসুদ | Masud, Masood, Masoud, Mashud |
| রহিম | Rohim, Rahim, Roheem, Raheem |
| মোহাম্মদ | Mohammod, Mohammad, Muhammod, Mohamod, Muhammad, Mohammed |
| সাদ্দাম | Saddam, Shaddam, Sadam |
| ফারজানা | Farjana, Farzana, Faarjana |
| শাকিব | Shakib, Shakeeb, Sakib |
| জাহাঙ্গীর | Jahangir, Zahangir, Jahangeer |

The table shows examples, not the complete list. The first spelling is the
reference reading, which follows the Bangla pronunciation (Rohim, Korim,
Mohammod). Common Arabic-origin spellings such as Rahim, Karim, Muhammad and
Mohammed are offered as alternatives and score a little lower, so a high
`minScore` such as 90 can leave some of them out. Names have many accepted English spellings, so use a lower
`minScore` to see more of them.

| English input | Bangla spellings (best first) |
| --- | --- |
| Masud | মাসুদ, মাসূদ, মাসুড, মসুদ, মাশুদ |
| Rahim | রাহিম, রাহীম, রহিম |
| Farzana | ফারজানা, ফরজানা, ফারযানা |
| Mostafa | মোস্তাফা, মোস্টাফা, মোস্তফা |
| Stephen | স্টেফেন, স্তেফেন, শ্টেফেন |

### Rules in short

- Inherent vowels: kept in the first syllable, dropped at the end of a word after
  a single consonant (মাসুদ → Masud), kept after a conjunct (শান্ত → Shanto,
  Shanta, Shant), and dropped before a pronounced syllable (ফারজানা → Farjana).
- Same sound, several English spellings: উ → u, oo, ou; ই → i, ee, e; স → s, sh;
  জ → j, z; ফ → f, ph; ভ → bh, v, b; ো → o, u; ৈ → oi, ai, oy.
- Conjuncts: doubled sounds are kept or reduced (সাদ্দাম → Saddam, Sadam),
  ক্ষ, জ্ঞ and হ্ম are handled as units, ঙ before ক খ গ ঘ becomes n or ng, and
  ্য, ্র, ্ব follow their own rules.
- Nasal signs ং ঁ ঃ, য় after a vowel (রায়হান → Rayhan, Raihan), word-initial ওয়
  (ওয়াসিম → Wasim) and ইউ (ইউসুফ → Yusuf).

For English input, an `a` after a consonant becomes `া` or the silent inherent
vowel (রাহিম, রহিম), `o` before `ng` is the inherent vowel (রংপুর), `s`, `t`,
`d` and `n` offer their Bangla variants (স শ ষ, ত ট, দ ড, ন ণ), a word-initial
consonant cluster becomes a conjunct (স্মিথ, স্টেফেন), and other clusters offer
both the conjunct and the plain form (আব্দুল, ইসলাম). Doubled letters are kept or
reduced (সাদ্দাম, সাদাম).

The rules are tuned for Bangladeshi names and are a model of pronunciation, not
a dictionary. Names with irregular pronunciation can need other spellings than
the ones offered.

Everything runs offline in common code on every target, with the same execution
as the other classes: a background thread, or 8 ms slices on JS and Wasm.
`generateAsync` is the callback form, and `cancelAll()`, `dispose()`,
`setExecutionMode(...)` and `PhoneticCancelToken` work as described below.
`CrossLanguageEngine` is the synchronous version.

## Languages

`PhoneticLanguages.bangla` and `PhoneticLanguages.english` ship with the library.
A language is data: a code, a name, the rules, and how its text is cleaned up
before use.

```kotlin
val language = PhoneticLanguage(
    code = "xx",
    name = "Test",
    foldCase = true,
    canonicalSequences = mapOf("ß" to "ss"),
    marks = setOf(0x2D),
    rules = listOf(
        PhoneticRule(listOf("q", "x"), 0.9),
        PhoneticRule(listOf("ee", "i"), 0.8, position = RulePosition.WordEnd),
    ),
)
val phonetics = Phonetics(language)
```

Bangla rules treat ড় and ঢ় written with a nukta the same as the single letters.
English folds case, removes accents and gives the answer back in the case of the
input (`JOHN` gives `JOHN`, `John Smith` gives `John Smith`).

A `PhoneticRule` lists spellings that sound alike and how alike (above 0 and
below 1). `position` limits it to the start, the end or the inside of a word.
`scoreOnly` rules count when comparing words but are never used to build new
spellings. Extend the defaults with
`PhoneticLanguages.bangla.defaultRuleSet.extend(listOf(...))` and pass the result
as `rules`.

## Errors

Nothing is thrown. Bad arguments, a broken language or rule set, a cancelled
request and a disposed instance all come back in `error`:

| Error | Meaning |
| --- | --- |
| `PhoneticArgumentException` | an argument, rule or language definition was not valid |
| `PhoneticCancelledException` | the request was cancelled before it finished |
| `PhoneticDisposedException` | the instance was disposed |
| `PhoneticWorkerException` | something unexpected failed while working |

Constructors keep their problems in an `error` property too, so a bad setting is
reported on the first call instead of crashing at construction.

One thing is not turned into a result: if the coroutine that called a `suspend`
function is cancelled, Kotlin's `CancellationException` propagates as usual,
and the work stops.

## Cancelling and execution mode

```kotlin
val token = PhoneticCancelToken()
val call = scope.async { phonetics.rank(query, candidates, cancelToken = token) }
token.cancel()
call.await().error   // PhoneticCancelledException
```

`phonetics.cancelAll()` cancels everything that is pending, and `dispose()`
cancels it and stops the instance.

`PhoneticExecutionMode` decides what a new request does while one is running:

| Mode | Behaviour |
| --- | --- |
| `CancelPrevious` (default) | the running request is cancelled, which suits search-as-you-type |
| `Queue` | requests run one after another |

Pass it to the constructor or change it later with
`phonetics.setExecutionMode(...)`, also on the shared `BanglaPhonetics.instance`.

## Synchronous engine

`PhoneticEngine` is the same code without the background execution. Use it for
short work, in tests, or from your own dispatcher:

```kotlin
val engine = PhoneticEngine(PhoneticLanguages.english)
val result = engine.generate("Catherine")
```

## Memory and limits

The number of variants grows quickly as `minScore` falls, so raising it is the
strongest way to save memory. A call returns at most 100,000 results and about
20 million output characters, whatever you ask for.

| Device | `minScore` | `maxResults` |
| --- | --- | --- |
| Low-end phones and mobile browsers | 60 or more | 500 or less |
| Recommended (the defaults) | 70 | 50 |
| Desktop or batch work | 40 or more | 10,000 or less |

`rank` keeps only `limit` matches, so use a `limit` with big candidate lists.

## Complexity

n is the length of the input, R the number of results, m the length of the other
word and C the number of candidates.

| | Time | Extra space |
| --- | --- | --- |
| `generate` | O(n + R (n + log R)) | O(R n) |
| `similarity` | O(n m) | O(m) |
| `rank` | O(C n m), usually much less | O(n + m + limit) |

`generate` is a best-first search over letter swaps backed by a heap. It always
expands the highest-scoring swaps first and stops as soon as `maxResults` are
collected or the scores fall below `minScore`. `rank` stops scoring a candidate
as soon as it cannot reach the cut-off.

## Scores

Every swap has a similarity, for example স→শ is 0.95 and স→ছ is 0.85. A variant's
score is the product of its swaps, so two স→শ swaps give 90.25%. The default
rules are `banglaRules` and `englishRules`.

Scores are rounded to two decimals. When a score falls exactly between two
roundings, the last digit can differ by 0.01 from the Dart package, because the
platforms' `exp` and `ln` differ in the last bit.

## Benchmark

> 90% accuracy gain, measured by testing against public datasets.

Datasets:

- **Bangla**
  - <https://github.com/google/language-resources/tree/master/bn/data>
  - <https://github.com/google-research-datasets/dakshina>
  - <https://huggingface.co/datasets/ai4bharat/Aksharantar>
- **English**
  - <https://github.com/cmusphinx/cmudict>
  - <https://llds.phon.ox.ac.uk/llds/xmlui/handle/20.500.14106/0643>
  - <https://github.com/mhagiwara/github-typo-corpus>

## Flutter

This package is also available for Flutter:
[pub.dev](https://pub.dev/packages/flutter_phonetics) |
[GitHub](https://github.com/zakaria5729/flutter_phonetics)

## Development

```bash
./gradlew :phonetics:jvmTest
./gradlew :phonetics:jsNodeTest :phonetics:wasmJsNodeTest
./gradlew :phonetics:jsBrowserTest :phonetics:wasmJsBrowserTest   # needs Chrome
./gradlew :phonetics:testAndroidHostTest
./gradlew :phonetics-compose:jvmTest
./gradlew :sample-compose:run                                    # desktop sample
./gradlew :sample-compose:wasmJsBrowserDevelopmentRun            # web sample
./gradlew :sample-android-xml:installDebug
```

`phonetics/src/commonTest/.../GoldenData.kt` holds results from the Dart package.
To refresh it, run `dart run bin/generate.dart <path>` in `tools/golden`.

## License

MIT, see [LICENSE](LICENSE).

## Connect with Developer

Built and maintained by Zakaria Hossain, known online as zakaria5729.

[LinkedIn](https://www.linkedin.com/in/zakaria5729) |
[GitHub](https://github.com/zakaria5729) |
[Stack Overflow](https://stackoverflow.com/users/8641776/zakaria-hossain?tab=profile) |
[Medium](https://medium.com/@zakaria5729) |
[Substack](https://substack.com/@zakaria5729) |
[Twitter](https://twitter.com/zakaria5729) |
[Facebook](https://www.facebook.com/zakaria5729)

## Connect with Developer

[LinkedIn](https://www.linkedin.com/in/zakaria5729) |
[GitHub](https://github.com/zakaria5729) |
[Stack Overflow](https://stackoverflow.com/users/8641776/zakaria-hossain?tab=profile) |
[Medium](https://medium.com/@zakaria5729) |
[Substack](https://substack.com/@zakaria5729) |
[Twitter](https://twitter.com/zakaria5729) |
[Facebook](https://www.facebook.com/zakaria5729)
